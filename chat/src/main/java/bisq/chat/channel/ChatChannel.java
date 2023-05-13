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

package bisq.chat.channel;

import bisq.chat.bisqeasy.channel.priv.BisqEasyPrivateTradeChatChannel;
import bisq.chat.bisqeasy.channel.pub.BisqEasyPublicChatChannel;
import bisq.chat.channel.priv.TwoPartyPrivateChatChannel;
import bisq.chat.channel.pub.CommonPublicChatChannel;
import bisq.chat.message.ChatMessage;
import bisq.common.observable.Observable;
import bisq.common.observable.collection.ObservableSet;
import bisq.common.proto.Proto;
import bisq.common.proto.UnresolvableProtobufMessageException;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

@ToString
@Getter
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public abstract class ChatChannel<M extends ChatMessage> implements Proto {
    @EqualsAndHashCode.Include
    protected final String id;
    protected final ChatChannelDomain chatChannelDomain;
    protected final Observable<ChatChannelNotificationType> chatChannelNotificationType = new Observable<>();
    protected final ObservableSet<String> seenChatMessageIds = new ObservableSet<>();
    @Getter
    protected final transient Set<String> userProfileIdsOfParticipants = new HashSet<>();

    public ChatChannel(String id,
                       ChatChannelDomain chatChannelDomain,
                       ChatChannelNotificationType chatChannelNotificationType) {
        this.id = id;
        this.chatChannelDomain = chatChannelDomain;
        this.chatChannelNotificationType.set(chatChannelNotificationType);
    }

    public bisq.chat.protobuf.ChatChannel.Builder getChannelBuilder() {
        return bisq.chat.protobuf.ChatChannel.newBuilder()
                .setId(id)
                .setChatChannelDomain(chatChannelDomain.toProto())
                .setChatChannelNotificationType(chatChannelNotificationType.get().toProto())
                .addAllSeenChatMessageIds(seenChatMessageIds);
    }

    abstract public bisq.chat.protobuf.ChatChannel toProto();

    public static ChatChannel<? extends ChatMessage> fromProto(bisq.chat.protobuf.ChatChannel proto) {
        switch (proto.getMessageCase()) {
            case TWOPARTYPRIVATECHATCHANNEL: {
                return TwoPartyPrivateChatChannel.fromProto(proto, proto.getTwoPartyPrivateChatChannel());
            }

            case PRIVATEBISQEASYTRADECHATCHANNEL: {
                return BisqEasyPrivateTradeChatChannel.fromProto(proto, proto.getPrivateBisqEasyTradeChatChannel());
            }
            case PUBLICBISQEASYOFFERCHATCHANNEL: {
                return BisqEasyPublicChatChannel.fromProto(proto, proto.getPublicBisqEasyOfferChatChannel());
            }


            case COMMONPUBLICCHATCHANNEL: {
                return CommonPublicChatChannel.fromProto(proto, proto.getCommonPublicChatChannel());
            }

            case MESSAGE_NOT_SET: {
                throw new UnresolvableProtobufMessageException(proto);
            }
        }
        throw new UnresolvableProtobufMessageException(proto);
    }

    public void setAllMessagesSeen() {
        seenChatMessageIds.setAll(getChatMessages().stream()
                .map(ChatMessage::getId)
                .collect(Collectors.toSet()));
    }

    abstract public String getDisplayString();

    abstract public ObservableSet<M> getChatMessages();

    abstract public void addChatMessage(M chatMessage);

    abstract public void removeChatMessage(M chatMessage);

    abstract public void removeChatMessages(Collection<M> messages);
}