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

package bisq.chat.priv;

import bisq.chat.*;
import bisq.chat.reactions.Reaction;
import bisq.chat.reactions.TwoPartyPrivateChatMessageReaction;
import bisq.common.util.StringUtils;
import bisq.i18n.Res;
import bisq.network.NetworkService;
import bisq.network.SendMessageResult;
import bisq.network.identity.NetworkId;
import bisq.network.identity.NetworkIdWithKeyPair;
import bisq.network.p2p.services.confidential.ConfidentialMessageService;
import bisq.persistence.PersistableStore;
import bisq.user.UserService;
import bisq.user.identity.UserIdentity;
import bisq.user.profile.UserProfile;
import lombok.extern.slf4j.Slf4j;

import javax.annotation.Nullable;
import java.util.Date;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CompletableFuture;

import static com.google.common.base.Preconditions.checkArgument;

@Slf4j
public abstract class PrivateChatChannelService<
        M extends PrivateChatMessage,
        C extends PrivateChatChannel<M>,
        S extends PersistableStore<S>
        > extends ChatChannelService<M, C, S> implements ConfidentialMessageService.Listener {
    // TODO: Make this generic when enabling reactions in trade messages
    private final Set<TwoPartyPrivateChatMessageReaction> unprocessedReactions = new HashSet<>();

    public PrivateChatChannelService(NetworkService networkService,
                                     UserService userService,
                                     ChatChannelDomain chatChannelDomain) {
        super(networkService, userService, chatChannelDomain);
    }


    ///////////////////////////////////////////////////////////////////////////////////////////////////
    // Service
    ///////////////////////////////////////////////////////////////////////////////////////////////////

    @Override
    public CompletableFuture<Boolean> initialize() {
        networkService.addConfidentialMessageListener(this);
        return CompletableFuture.completedFuture(true);
    }

    @Override
    public CompletableFuture<Boolean> shutdown() {
        networkService.removeConfidentialMessageListener(this);
        return CompletableFuture.completedFuture(true);
    }


    ///////////////////////////////////////////////////////////////////////////////////////////////////
    // API
    ///////////////////////////////////////////////////////////////////////////////////////////////////

    protected CompletableFuture<SendMessageResult> sendMessage(String messageId,
                                                               @Nullable String text,
                                                               Optional<Citation> citation,
                                                               C channel,
                                                               UserProfile receiver,
                                                               ChatMessageType chatMessageType,
                                                               long date) {
        UserIdentity myUserIdentity = channel.getMyUserIdentity();
        if (bannedUserService.isUserProfileBanned(myUserIdentity.getUserProfile())) {
            return CompletableFuture.failedFuture(new RuntimeException());
        }
        if (isPeerBanned(receiver)) {
            return CompletableFuture.failedFuture(new RuntimeException("Peer is banned"));
        }

        M chatMessage = createAndGetNewPrivateChatMessage(messageId,
                channel,
                myUserIdentity.getUserProfile(),
                receiver,
                text,
                citation,
                date,
                false,
                chatMessageType);
        addMessage(chatMessage, channel);
        NetworkId receiverNetworkId = receiver.getNetworkId();
        NetworkIdWithKeyPair senderNetworkIdWithKeyPair = myUserIdentity.getNetworkIdWithKeyPair();
        return networkService.confidentialSend(chatMessage, receiverNetworkId, senderNetworkIdWithKeyPair);
    }

    protected boolean isPeerBanned(UserProfile userProfile) {
        return bannedUserService.isUserProfileBanned(userProfile);
    }

    public void leaveChannel(String id) {
        findChannel(id).ifPresent(this::leaveChannel);
    }

    public void leaveChannel(C channel) {
        synchronized (this) {
            getChannels().remove(channel);
        }
        persist();
    }

    @Override
    public Optional<C> getDefaultChannel() {
        return Optional.empty();
    }

    protected CompletableFuture<SendMessageResult> sendLeaveMessage(C channel, UserProfile receiver, long date) {
        String encoded = Res.encode("chat.privateChannel.message.leave", channel.getMyUserIdentity().getUserProfile().getUserName());
        return sendMessage(StringUtils.createUid(),
                encoded,
                Optional.empty(),
                channel,
                receiver,
                ChatMessageType.LEAVE,
                date);
    }

    @Override
    public String getChannelTitlePostFix(ChatChannel<? extends ChatMessage> chatChannel) {
        checkArgument(chatChannel instanceof PrivateChatChannel,
                "chatChannel at PrivateChatChannelService.getChannelTitlePostFix must be of type PrivateChatChannel");
        return userIdentityService.hasMultipleUserIdentities() ? "" :
                " [" + ((PrivateChatChannel<?>) chatChannel).getMyUserIdentity().getUserName() + "]";
    }

    protected CompletableFuture<SendMessageResult> sendMessageReaction(M message,
                                                                       C chatChannel,
                                                                       UserProfile receiver,
                                                                       Reaction reaction,
                                                                       String messageReactionId,
                                                                       boolean isRemoved) {
        UserIdentity myUserIdentity = chatChannel.getMyUserIdentity();
        if (bannedUserService.isUserProfileBanned(myUserIdentity.getUserProfile())) {
            return CompletableFuture.failedFuture(new RuntimeException());
        }
        if (isPeerBanned(receiver)) {
            return CompletableFuture.failedFuture(new RuntimeException("Peer is banned"));
        }

        TwoPartyPrivateChatMessageReaction chatMessageReaction = createAndGetNewPrivateChatMessageReaction(message,
                myUserIdentity.getUserProfile(), receiver, reaction, messageReactionId, isRemoved);

        addMessageReaction(chatMessageReaction, message);

        NetworkId receiverNetworkId = receiver.getNetworkId();
        NetworkIdWithKeyPair senderNetworkIdWithKeyPair = myUserIdentity.getNetworkIdWithKeyPair();
        return networkService.confidentialSend(chatMessageReaction, receiverNetworkId, senderNetworkIdWithKeyPair);
    }


    ///////////////////////////////////////////////////////////////////////////////////////////////////
    // Protected
    ///////////////////////////////////////////////////////////////////////////////////////////////////

    protected abstract void processMessage(M message);

    // TODO: Make it class generic
    protected void processMessageReaction(TwoPartyPrivateChatMessageReaction messageReaction) {
        findChannel(messageReaction.getChatChannelId())
                .flatMap(channel -> channel.getChatMessages().stream()
                        .filter(message -> message.getId().equals(messageReaction.getChatMessageId()))
                        .findFirst())
                .ifPresentOrElse(
                        message -> addMessageReaction(messageReaction, message),
                        () -> unprocessedReactions.add(messageReaction));
    }

    protected void processQueuedReactions() {
        unprocessedReactions.forEach(reaction -> {
            unprocessedReactions.remove(reaction);
            processMessageReaction(reaction);
        });
    }

    @Override
    protected boolean isValid(M message) {
        if (message.isMyMessage(userIdentityService)) {
            log.warn("Sent a message to myself. This should never happen for private chat messages.");
            return false;
        }
        if (bannedUserService.isNetworkIdBanned(message.getSenderUserProfile().getNetworkId())) {
            log.warn("Message invalid as sender is banned");
            return false;
        }
        return super.isValid(message);
    }

    protected abstract C createAndGetNewPrivateChatChannel(UserProfile peer, UserIdentity myUserIdentity);

    protected abstract M createAndGetNewPrivateChatMessage(String messageId,
                                                           C channel,
                                                           UserProfile senderUserProfile,
                                                           UserProfile receiverUserProfile,
                                                           String text,
                                                           Optional<Citation> citation,
                                                           long time,
                                                           boolean wasEdited,
                                                           ChatMessageType chatMessageType);

    // TODO: Make it class generic.
    private TwoPartyPrivateChatMessageReaction createAndGetNewPrivateChatMessageReaction(M message,
                                                                                         UserProfile senderUserProfile,
                                                                                         UserProfile receiverUserProfile,
                                                                                         Reaction reaction,
                                                                                         String messageReactionId,
                                                                                         boolean isRemoved) {
        return new TwoPartyPrivateChatMessageReaction(
                messageReactionId,
                senderUserProfile,
                receiverUserProfile.getId(),
                receiverUserProfile.getNetworkId(),
                message.getChannelId(),
                message.getChatChannelDomain(),
                message.getId(),
                reaction.ordinal(),
                new Date().getTime(),
                isRemoved
        );
    };
}
