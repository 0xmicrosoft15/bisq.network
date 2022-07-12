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

package bisq.chat.channels;

import bisq.chat.messages.PrivateChatMessage;
import bisq.chat.messages.Quotation;
import bisq.common.application.Service;
import bisq.common.observable.ObservableSet;
import bisq.network.NetworkId;
import bisq.network.NetworkIdWithKeyPair;
import bisq.network.NetworkService;
import bisq.network.p2p.services.confidential.MessageListener;
import bisq.persistence.PersistableStore;
import bisq.persistence.PersistenceClient;
import bisq.security.pow.ProofOfWorkService;
import bisq.user.identity.UserIdentity;
import bisq.user.identity.UserIdentityService;
import bisq.user.profile.UserProfile;
import lombok.extern.slf4j.Slf4j;

import java.util.Date;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

@Slf4j
public abstract class PrivateChannelService<M extends PrivateChatMessage, C extends PrivateChannel<M>, S extends PersistableStore<S>>
        implements MessageListener, Service, PersistenceClient<S> {
    protected final NetworkService networkService;
    protected final UserIdentityService userIdentityService;
    protected final ProofOfWorkService proofOfWorkService;

    public PrivateChannelService(NetworkService networkService,
                                 UserIdentityService userIdentityService,
                                 ProofOfWorkService proofOfWorkService) {

        this.networkService = networkService;
        this.userIdentityService = userIdentityService;
        this.proofOfWorkService = proofOfWorkService;
    }


    ///////////////////////////////////////////////////////////////////////////////////////////////////
    // Service
    ///////////////////////////////////////////////////////////////////////////////////////////////////

    @Override
    public CompletableFuture<Boolean> initialize() {
        log.info("initialize");
        networkService.addMessageListener(this);
        return CompletableFuture.completedFuture(true);
    }

    @Override
    public CompletableFuture<Boolean> shutdown() {
        log.info("shutdown");
        networkService.removeMessageListener(this);
        return CompletableFuture.completedFuture(true);
    }


    ///////////////////////////////////////////////////////////////////////////////////////////////////
    // API
    ///////////////////////////////////////////////////////////////////////////////////////////////////

    public CompletableFuture<NetworkService.SendMessageResult> sendPrivateChatMessage(String text,
                                                                                      Optional<Quotation> quotedMessage,
                                                                                      C channel) {
        String channelId = channel.getId();
        UserIdentity senderIdentity = channel.getMyProfile();
        UserProfile peer = channel.getPeer();
        M chatMessage = createNewPrivateChatMessage(channelId,
                senderIdentity.getUserProfile(),
                peer.getId(),
                text,
                quotedMessage,
                new Date().getTime(),
                false);
        addMessage(chatMessage, channel);
        NetworkId receiverNetworkId = peer.getNetworkId();
        NetworkIdWithKeyPair senderNetworkIdWithKeyPair = senderIdentity.getNodeIdAndKeyPair();
        return networkService.sendMessage(chatMessage, receiverNetworkId, senderNetworkIdWithKeyPair);
    }

    protected abstract M createNewPrivateChatMessage(String channelId,
                                                     UserProfile sender,
                                                     String receiversId,
                                                     String text,
                                                     Optional<Quotation> quotedMessage,
                                                     long time,
                                                     boolean wasEdited);

    public Optional<C> createAndAddChannel(UserProfile peer) {
        return Optional.ofNullable(userIdentityService.getSelectedUserProfile().get())
                .flatMap(myUserIdentity -> createAndAddChannel(peer, myUserIdentity.getId()));
    }

    public void removeExpiredMessages(C channel) {
        Set<M> toRemove = channel.getChatMessages().stream()
                .filter(PrivateChatMessage::isExpired)
                .collect(Collectors.toSet());
        if (!toRemove.isEmpty()) {
            synchronized (getPersistableStore()) {
                channel.removeChatMessages(toRemove);
            }
            persist();
        }
    }


    ///////////////////////////////////////////////////////////////////////////////////////////////////
    // Protected
    ///////////////////////////////////////////////////////////////////////////////////////////////////

    protected Optional<C> createAndAddChannel(UserProfile peer, String myUserIdentityId) {
        return userIdentityService.findUserIdentity(myUserIdentityId)
                .map(myUserIdentity -> {
                            C channel = createNewChannel(peer, myUserIdentity);
                            getChannels().add(channel);
                            persist();
                            return channel;
                        }
                );
    }

    protected abstract C createNewChannel(UserProfile peer, UserIdentity myUserIdentity);

    protected Optional<C> findChannel(String channelId) {
        return getChannels().stream()
                .filter(channel -> channel.getId().equals(channelId))
                .findAny();
    }

    protected abstract ObservableSet<C> getChannels();

    protected void processMessage(M message) {
        if (!userIdentityService.isUserIdentityPresent(message.getAuthorId()) &&
                proofOfWorkService.verify(message.getSender().getProofOfWork())) {
            findChannel(message.getChannelId())
                    .or(() -> createAndAddChannel(message.getSender(), message.getReceiversId()))
                    .ifPresent(channel -> addMessage(message, channel));
        }
    }

    protected void addMessage(M chatMessage, C channel) {
        synchronized (getPersistableStore()) {
            channel.addChatMessage(chatMessage);
        }
        persist();
    }
}