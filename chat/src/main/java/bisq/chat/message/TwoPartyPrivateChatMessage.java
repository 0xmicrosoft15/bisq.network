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

package bisq.chat.message;

import bisq.chat.channel.ChatChannelDomain;
import bisq.network.p2p.services.data.storage.MetaData;
import bisq.network.protobuf.ExternalNetworkMessage;
import bisq.user.profile.UserProfile;
import com.google.protobuf.Any;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;

import java.util.Optional;
import java.util.concurrent.TimeUnit;

@Getter
@ToString(callSuper = true)
@EqualsAndHashCode(callSuper = true)
public final class TwoPartyPrivateChatMessage extends PrivateChatMessage {
    private final static long TTL = TimeUnit.DAYS.toMillis(30);

    public TwoPartyPrivateChatMessage(String messageId,
                                      ChatChannelDomain chatChannelDomain,
                                      String channelId,
                                      UserProfile sender,
                                      String receiverUserProfileId,
                                      String text,
                                      Optional<Citation> citation,
                                      long date,
                                      boolean wasEdited,
                                      ChatMessageType chatMessageType) {
        super(messageId,
                chatChannelDomain,
                channelId,
                sender,
                receiverUserProfileId,
                text,
                citation,
                date,
                wasEdited,
                chatMessageType,
                new MetaData(TTL, 100_000, TwoPartyPrivateChatMessage.class.getSimpleName()));
    }

    private TwoPartyPrivateChatMessage(String messageId,
                                       ChatChannelDomain chatChannelDomain,
                                       String channelId,
                                       UserProfile sender,
                                       String receiverUserProfileId,
                                       String text,
                                       Optional<Citation> citation,
                                       long date,
                                       boolean wasEdited,
                                       ChatMessageType chatMessageType,
                                       MetaData metaData) {
        super(messageId, chatChannelDomain, channelId, sender, receiverUserProfileId, text, citation, date, wasEdited, chatMessageType, metaData);
    }

    @Override
    public bisq.network.protobuf.NetworkMessage toProto() {
        return getNetworkMessageBuilder()
                .setExternalNetworkMessage(ExternalNetworkMessage.newBuilder().setAny(Any.pack(toChatMessageProto())))
                .build();
    }

    public bisq.chat.protobuf.ChatMessage toChatMessageProto() {
        return getChatMessageBuilder()
                .setTwoPartyPrivateChatMessage(bisq.chat.protobuf.TwoPartyPrivateChatMessage.newBuilder()
                        .setReceiverUserProfileId(receiverUserProfileId)
                        .setSender(sender.toProto()))
                .build();
    }

    public static TwoPartyPrivateChatMessage fromProto(bisq.chat.protobuf.ChatMessage baseProto) {
        Optional<Citation> citation = baseProto.hasCitation() ?
                Optional.of(Citation.fromProto(baseProto.getCitation())) :
                Optional.empty();
        bisq.chat.protobuf.TwoPartyPrivateChatMessage privateChatMessage = baseProto.getTwoPartyPrivateChatMessage();
        return new TwoPartyPrivateChatMessage(
                baseProto.getId(),
                ChatChannelDomain.fromProto(baseProto.getChatChannelDomain()),
                baseProto.getChannelId(),
                UserProfile.fromProto(privateChatMessage.getSender()),
                privateChatMessage.getReceiverUserProfileId(),
                baseProto.getText(),
                citation,
                baseProto.getDate(),
                baseProto.getWasEdited(),
                ChatMessageType.fromProto(baseProto.getChatMessageType()),
                MetaData.fromProto(baseProto.getMetaData()));
    }
}