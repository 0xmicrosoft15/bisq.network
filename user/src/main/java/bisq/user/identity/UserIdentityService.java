/*
 * This file is part of Bisq.
 *
 * Bisq is free software: you can redistribute it and/or modify it
 * under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or (at
 * your option) any later version.
 *
 * Bisq is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE. See the GNU Affero General Public
 * License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with Bisq. If not, see <http://www.gnu.org/licenses/>.
 */

package bisq.user.identity;

import bisq.common.application.Service;
import bisq.common.encoding.Hex;
import bisq.common.observable.Observable;
import bisq.common.observable.collection.ObservableSet;
import bisq.common.threading.ExecutorFactory;
import bisq.common.timer.Scheduler;
import bisq.identity.Identity;
import bisq.identity.IdentityService;
import bisq.network.NetworkService;
import bisq.network.p2p.services.data.BroadcastResult;
import bisq.persistence.DbSubDirectory;
import bisq.persistence.Persistence;
import bisq.persistence.PersistenceClient;
import bisq.persistence.PersistenceService;
import bisq.security.AesSecretKey;
import bisq.security.EncryptedData;
import bisq.security.SecurityService;
import bisq.security.pow.ProofOfWork;
import bisq.security.pow.hashcash.HashCashProofOfWorkService;
import bisq.user.profile.UserProfile;
import lombok.Getter;
import lombok.ToString;
import lombok.extern.slf4j.Slf4j;

import javax.annotation.Nullable;
import java.security.KeyPair;
import java.util.Optional;
import java.util.Random;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import static com.google.common.base.Preconditions.checkArgument;

@Slf4j
public class UserIdentityService implements PersistenceClient<UserIdentityStore>, Service {
    public final static int MINT_NYM_DIFFICULTY = 65536;  // Math.pow(2, 16) = 65536;

    @Getter
    @ToString
    public static final class Config {
        private final long republishUserProfileInterval;

        public Config(long republishUserProfileInterval) {
            this.republishUserProfileInterval = TimeUnit.HOURS.toMillis(republishUserProfileInterval);
        }

        public static Config from(com.typesafe.config.Config typeSafeConfig) {
            return new Config(typeSafeConfig.getLong("republishUserProfileInterval"));
        }
    }


    @Getter
    private final UserIdentityStore persistableStore = new UserIdentityStore();
    @Getter
    private final Persistence<UserIdentityStore> persistence;
    private final HashCashProofOfWorkService hashCashProofOfWorkService;
    private final IdentityService identityService;
    private final NetworkService networkService;

    private final Object lock = new Object();
    private final Config config;
    @Getter
    private final Observable<UserIdentity> newlyCreatedUserIdentity = new Observable<>();
    @Nullable
    private ExecutorService rePublishUserProfilesExecutor;

    public UserIdentityService(Config config,
                               PersistenceService persistenceService,
                               SecurityService securityService,
                               IdentityService identityService,
                               NetworkService networkService) {
        this.config = config;
        persistence = persistenceService.getOrCreatePersistence(this, DbSubDirectory.PRIVATE, persistableStore);
        hashCashProofOfWorkService = securityService.getHashCashProofOfWorkService();
        this.identityService = identityService;
        this.networkService = networkService;
    }

    public CompletableFuture<Boolean> initialize() {
        log.info("initialize");

        // We delay publishing to be better bootstrapped 
        Scheduler.run(this::rePublishAllUserProfiles).after(5, TimeUnit.SECONDS);
        return CompletableFuture.completedFuture(true);
    }

    public CompletableFuture<Boolean> shutdown() {
        return CompletableFuture.supplyAsync(() -> {
            if (rePublishUserProfilesExecutor != null) {
                ExecutorFactory.shutdownAndAwaitTermination(rePublishUserProfilesExecutor, 100);
            }
            return true;
        });
    }


    ///////////////////////////////////////////////////////////////////////////////////////////////////
    // API
    ///////////////////////////////////////////////////////////////////////////////////////////////////

    public ProofOfWork mintNymProofOfWork(byte[] pubKeyHash) {
        return hashCashProofOfWorkService.mint(pubKeyHash, null, MINT_NYM_DIFFICULTY);
    }

    public CompletableFuture<AesSecretKey> deriveKeyFromPassword(CharSequence password) {
        return persistableStore.deriveKeyFromPassword(password)
                .whenComplete((aesKey, throwable) -> {
                    if (throwable == null && aesKey != null) {
                        persist();
                    }
                });
    }

    public CompletableFuture<EncryptedData> encryptDataStore() {
        return persistableStore.encrypt()
                .whenComplete((encryptedData, throwable) -> {
                    if (throwable == null && encryptedData != null) {
                        persist();
                    }
                });
    }

    public CompletableFuture<Void> decryptDataStore(AesSecretKey aesSecretKey) {
        return persistableStore.decrypt(aesSecretKey)
                .whenComplete((nil, throwable) -> {
                    if (throwable == null) {
                        persist();
                    }
                });
    }

    public CompletableFuture<Void> removePassword(CharSequence password) {
        return decryptDataStore(getAESSecretKey().orElseThrow())
                .thenCompose(nil -> persistableStore.removeKey(password)
                        .whenComplete((nil2, throwable) -> {
                            if (throwable == null) {
                                persistableStore.clearEncryptedData();
                                persist();
                            }
                        }));
    }

    public boolean isDataStoreEncrypted() {
        return persistableStore.getEncryptedData().isPresent();
    }

    public Optional<AesSecretKey> getAESSecretKey() {
        return persistableStore.getAESSecretKey();
    }

    public CompletableFuture<UserIdentity> createAndPublishNewUserProfile(String nickName,
                                                                          KeyPair keyPair,
                                                                          byte[] pubKeyHash,
                                                                          ProofOfWork proofOfWork,
                                                                          int avatarVersion,
                                                                          String terms,
                                                                          String statement) {
        String identityTag = nickName + "-" + Hex.encode(pubKeyHash);
        return identityService.createNewActiveIdentity(identityTag, keyPair)
                .thenApply(identity -> createUserIdentity(nickName, proofOfWork, avatarVersion, terms, statement, identity))
                .thenApply(userIdentity -> {
                    publishPublicUserProfile(userIdentity.getUserProfile(), userIdentity.getIdentity().getNetworkIdWithKeyPair().getKeyPair());
                    return userIdentity;
                });
    }

    public void selectChatUserIdentity(UserIdentity userIdentity) {
        persistableStore.setSelectedUserIdentity(userIdentity);
        persist();
    }

    public CompletableFuture<BroadcastResult> editUserProfile(UserIdentity oldUserIdentity, String terms, String statement) {
        Identity oldIdentity = oldUserIdentity.getIdentity();
        UserProfile oldUserProfile = oldUserIdentity.getUserProfile();
        UserProfile newUserProfile = UserProfile.from(oldUserProfile, terms, statement);
        UserIdentity newUserIdentity = new UserIdentity(oldIdentity, newUserProfile);

        synchronized (lock) {
            getUserIdentities().remove(oldUserIdentity);
            getUserIdentities().add(newUserIdentity);
            persistableStore.setSelectedUserIdentity(newUserIdentity);
        }
        persist();

        return networkService.removeAuthenticatedData(oldUserProfile, oldIdentity.getNetworkIdWithKeyPair().getKeyPair())
                .thenCompose(result -> networkService.publishAuthenticatedData(newUserProfile, oldIdentity.getNetworkIdWithKeyPair().getKeyPair()));
    }

    // Unsafe to use if there are open private chats or messages from userIdentity
    public CompletableFuture<BroadcastResult> deleteUserIdentity(UserIdentity userIdentity) {
        if (getUserIdentities().size() <= 1) {
            return CompletableFuture.failedFuture(new RuntimeException("Deleting userProfile is not permitted if we only have one left."));
        }
        synchronized (lock) {
            getUserIdentities().remove(userIdentity);

            getUserIdentities().stream().findAny()
                    .ifPresentOrElse(persistableStore::setSelectedUserIdentity,
                            () -> persistableStore.setSelectedUserIdentity(null));
        }
        persist();
        identityService.retireActiveIdentity(userIdentity.getIdentity().getTag());
        return networkService.removeAuthenticatedData(userIdentity.getUserProfile(),
                userIdentity.getIdentity().getNetworkIdWithKeyPair().getKeyPair());
    }

    public CompletableFuture<Void> maybePublishUserProfile(UserProfile userProfile, KeyPair keyPair) {
        if (shouldPublishUserProfile()) {
            return publishPublicUserProfile(userProfile, keyPair)
                    .whenComplete((broadcastResult, throwable) -> {
                        boolean success = throwable == null && !broadcastResult.isEmpty();
                        // Publish all other user profiles as well, or republish if not successful
                        Set<UserIdentity> userIdentities = getUserIdentities().stream()
                                .filter(userIdentity -> !success || !userProfile.equals(userIdentity.getUserProfile()))
                                .collect(Collectors.toSet());
                        rePublishUserProfiles(userIdentities);
                    })
                    .thenApply(broadcastResult -> null);
        } else {
            return CompletableFuture.completedFuture(null);
        }
    }

    public boolean hasUserIdentities() {
        return !getUserIdentities().isEmpty();
    }

    public boolean hasMultipleUserIdentities() {
        return getUserIdentities().size() > 1;
    }

    public Observable<UserIdentity> getSelectedUserIdentityObservable() {
        return persistableStore.getSelectedUserIdentityObservable();
    }

    @Nullable
    public UserIdentity getSelectedUserIdentity() {
        return persistableStore.getSelectedUserIdentity();
    }

    public ObservableSet<UserIdentity> getUserIdentities() {
        return persistableStore.getUserIdentities();
    }

    public boolean isUserIdentityPresent(String profileId) {
        return findUserIdentity(profileId).isPresent();
    }

    public Optional<UserIdentity> findUserIdentity(String id) {
        return getUserIdentities().stream().filter(userIdentity -> userIdentity.getId().equals(id)).findAny();
    }

    public Set<String> getMyUserProfileIds() {
        return getUserIdentities().stream()
                .map(userIdentity -> userIdentity.getUserProfile().getId())
                .collect(Collectors.toSet());
    }

    private boolean shouldPublishUserProfile() {
        return System.currentTimeMillis() - persistableStore.getLastUserProfilePublishingDate() > config.getRepublishUserProfileInterval();
    }

    private void rePublishAllUserProfiles() {
        rePublishUserProfiles(getUserIdentities());
    }

    private void rePublishUserProfiles(Set<UserIdentity> userIdentities) {
        if (rePublishUserProfilesExecutor == null) {
            rePublishUserProfilesExecutor = ExecutorFactory.newSingleThreadExecutor("rePublishUserProfilesExecutor");
            rePublishUserProfilesExecutor.submit(() -> {
                userIdentities.forEach(userIdentity -> {
                    publishPublicUserProfile(userIdentity.getUserProfile(),
                            userIdentity.getNetworkIdWithKeyPair().getKeyPair());
                    try {
                        // Add random delay of 1-31 sec
                        Thread.sleep(1000 + new Random().nextInt(30_000));
                    } catch (InterruptedException ignore) {
                    }
                });
                rePublishUserProfilesExecutor.shutdownNow();
                rePublishUserProfilesExecutor = null;
            });
        } else {
            log.warn("called rePublishUserProfiles while previous call to rePublishUserProfiles has not completed yet. We ignore that call.");
        }
    }

    private CompletableFuture<BroadcastResult> publishPublicUserProfile(UserProfile userProfile, KeyPair keyPair) {
        log.info("publishPublicUserProfile {}", userProfile.getUserName());
        persistableStore.setLastUserProfilePublishingDate(System.currentTimeMillis());
        persist();
        return networkService.publishAuthenticatedData(userProfile, keyPair);
    }

    private UserIdentity createUserIdentity(String nickName,
                                            ProofOfWork proofOfWork,
                                            int avatarVersion,
                                            String terms,
                                            String statement,
                                            Identity identity) {
        checkArgument(nickName.equals(nickName.trim()) && !nickName.isEmpty(),
                "Nickname must not have leading or trailing spaces and must not be empty.");
        UserProfile userProfile = new UserProfile(nickName, proofOfWork, avatarVersion,
                identity.getNetworkIdWithKeyPair().getNetworkId(), terms, statement);
        UserIdentity userIdentity = new UserIdentity(identity, userProfile);

        synchronized (lock) {
            getUserIdentities().add(userIdentity);
            persistableStore.setSelectedUserIdentity(userIdentity);
        }
        newlyCreatedUserIdentity.set(userIdentity);
        persist();
        return userIdentity;
    }
}
