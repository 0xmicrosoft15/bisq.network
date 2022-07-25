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

package bisq.user;

import bisq.common.application.Service;
import bisq.identity.IdentityService;
import bisq.network.NetworkService;
import bisq.oracle.daobridge.DaoBridgeService;
import bisq.oracle.ots.OpenTimestampService;
import bisq.persistence.PersistenceService;
import bisq.security.pow.ProofOfWorkService;
import bisq.user.identity.UserIdentityService;
import bisq.user.profile.UserProfileService;
import bisq.user.proof.ProofOfBurnVerificationService;
import bisq.user.reputation.ReputationService;
import lombok.Getter;
import lombok.ToString;
import lombok.extern.slf4j.Slf4j;

import java.util.concurrent.CompletableFuture;

@Slf4j
@Getter
public class UserService implements Service {
    @Getter
    @ToString
    public static final class Config {
        private final UserIdentityService.Config userIdentityConfig;
        private final ProofOfBurnVerificationService.Config proofOfBurnVerificationConfig;

        public Config(UserIdentityService.Config userIdentityConfig,
                      ProofOfBurnVerificationService.Config proofOfBurnVerificationConfig) {
            this.userIdentityConfig = userIdentityConfig;
            this.proofOfBurnVerificationConfig = proofOfBurnVerificationConfig;
        }

        public static Config from(com.typesafe.config.Config config) {
            return new Config(UserIdentityService.Config.from(config.getConfig("userIdentity")),
                    ProofOfBurnVerificationService.Config.from(config.getConfig("proofOfBurnVerification")));
        }
    }

    private final UserProfileService userProfileService;
    private final UserIdentityService userIdentityService;
    private final ReputationService reputationService;

    public UserService(Config config,
                       String baseDir,
                       PersistenceService persistenceService,
                       IdentityService identityService,
                       NetworkService networkService,
                       OpenTimestampService openTimestampService,
                       DaoBridgeService daoBridgeService,
                       ProofOfWorkService proofOfWorkService) {
        userProfileService = new UserProfileService(persistenceService, networkService, proofOfWorkService);
        userIdentityService = new UserIdentityService(config.getUserIdentityConfig(),
                persistenceService,
                identityService,
                openTimestampService,
                networkService);
        reputationService = new ReputationService(baseDir,
                networkService,
                userIdentityService,
                userProfileService,
                daoBridgeService
        );
    }


    ///////////////////////////////////////////////////////////////////////////////////////////////////
    // Service
    ///////////////////////////////////////////////////////////////////////////////////////////////////

    public CompletableFuture<Boolean> initialize() {
        log.info("initialize");
        return userProfileService.initialize()
                .thenCompose(result -> userIdentityService.initialize())
                .thenCompose(result -> reputationService.initialize());
    }

    public CompletableFuture<Boolean> shutdown() {
        log.info("shutdown");
        return userProfileService.shutdown()
                .thenCompose(result -> userIdentityService.shutdown())
                .thenCompose(result -> reputationService.shutdown());
    }
}