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

package bisq.chat.channel.priv;

import bisq.chat.channel.ChatChannel;
import bisq.chat.channel.ChatChannelDomain;
import bisq.chat.channel.ChatChannelService;
import bisq.chat.message.ChatMessage;
import bisq.chat.message.ChatMessageType;
import bisq.chat.message.Citation;
import bisq.chat.message.PrivateChatMessage;
import bisq.common.util.StringUtils;
import bisq.i18n.Res;
import bisq.network.NetworkId;
import bisq.network.NetworkIdWithKeyPair;
import bisq.network.NetworkService;
import bisq.network.p2p.services.confidential.MessageListener;
import bisq.persistence.PersistableStore;
import bisq.security.pow.ProofOfWorkService;
import bisq.user.identity.UserIdentity;
import bisq.user.identity.UserIdentityService;
import bisq.user.profile.UserProfile;
import bisq.user.profile.UserProfileService;
import lombok.extern.slf4j.Slf4j;

import java.util.Date;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

import static com.google.common.base.Preconditions.checkArgument;

@Slf4j
public abstract class PrivateChatChannelService<
        M extends PrivateChatMessage,
        C extends PrivateChatChannel<M>,
        S extends PersistableStore<S>
        > extends ChatChannelService<M, C, S> implements MessageListener {
    protected final ProofOfWorkService proofOfWorkService;

    public PrivateChatChannelService(NetworkService networkService,
                                     UserIdentityService userIdentityService,
                                     UserProfileService userProfileService,
                                     ProofOfWorkService proofOfWorkService,
                                     ChatChannelDomain chatChannelDomain) {
        super(networkService, userIdentityService, userProfileService, chatChannelDomain);

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

    protected CompletableFuture<NetworkService.SendMessageResult> sendMessage(String messageId,
                                                                              String text,
                                                                              Optional<Citation> citation,
                                                                              C channel,
                                                                              UserProfile receiver,
                                                                              ChatMessageType chatMessageType) {
        UserIdentity myUserIdentity = channel.getMyUserIdentity();
        M chatMessage = createAndGetNewPrivateChatMessage(messageId,
                channel,
                myUserIdentity.getUserProfile(),
                receiver.getId(),
                text,
                citation,
                new Date().getTime(),
                false,
                chatMessageType);
        addMessage(chatMessage, channel);
        NetworkId receiverNetworkId = receiver.getNetworkId();
        NetworkIdWithKeyPair senderNetworkIdWithKeyPair = myUserIdentity.getNodeIdAndKeyPair();
        return networkService.confidentialSend(chatMessage, receiverNetworkId, senderNetworkIdWithKeyPair);
    }

    @Override
    public void leaveChannel(C channel) {
        synchronized (this) {
            getChannels().remove(channel);
        }
        persist();
    }

    protected CompletableFuture<NetworkService.SendMessageResult> sendLeaveMessage(C channel, UserProfile receiver) {
        return sendMessage(StringUtils.createShortUid(),
                Res.get("social.privateChannel.leave.message", channel.getMyUserIdentity().getUserProfile().getUserName()),
                Optional.empty(),
                channel,
                receiver,
                ChatMessageType.LEAVE);
    }

    @Override
    public String getChannelTitlePostFix(ChatChannel<? extends ChatMessage> chatChannel) {
        checkArgument(chatChannel instanceof PrivateChatChannel,
                "chatChannel at PrivateChatChannelService.getChannelTitlePostFix must be of type PrivateChatChannel");
        return userIdentityService.hasMultipleUserIdentities() ? "" :
                " [" + ((PrivateChatChannel<?>) chatChannel).getMyUserIdentity().getUserName() + "]";
    }


    ///////////////////////////////////////////////////////////////////////////////////////////////////
    // Protected
    ///////////////////////////////////////////////////////////////////////////////////////////////////

    protected abstract C createAndGetNewPrivateChatChannel(UserProfile peer, UserIdentity myUserIdentity);

    protected abstract M createAndGetNewPrivateChatMessage(String messageId,
                                                           C channel,
                                                           UserProfile sender,
                                                           String receiversId,
                                                           String text,
                                                           Optional<Citation> citation,
                                                           long time,
                                                           boolean wasEdited,
                                                           ChatMessageType chatMessageType);
}