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
import bisq.common.util.StringUtils;
import bisq.network.p2p.services.data.storage.MetaData;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;

import java.util.Optional;

@Getter
@ToString(callSuper = true)
@EqualsAndHashCode(callSuper = true)
public final class CommonPublicChatMessage extends PublicChatMessage {
    public CommonPublicChatMessage(ChatChannelDomain chatChannelDomain,
                                   String channelName,
                                   String authorId,
                                   String text,
                                   Optional<Citation> citation,
                                   long date,
                                   boolean wasEdited) {
        this(StringUtils.createShortUid(),
                chatChannelDomain,
                channelName,
                authorId,
                Optional.of(text),
                citation,
                date,
                wasEdited,
                ChatMessageType.TEXT,
                new MetaData(ChatMessage.TTL, 100000, CommonPublicChatMessage.class.getSimpleName()));
    }

    private CommonPublicChatMessage(String messageId,
                                    ChatChannelDomain chatChannelDomain,
                                    String channelName,
                                    String authorId,
                                    Optional<String> text,
                                    Optional<Citation> citation,
                                    long date,
                                    boolean wasEdited,
                                    ChatMessageType chatMessageType,
                                    MetaData metaData) {
        super(messageId,
                chatChannelDomain,
                channelName,
                authorId,
                text,
                citation,
                date,
                wasEdited,
                chatMessageType,
                metaData);
    }

    public bisq.chat.protobuf.ChatMessage toProto() {
        return getChatMessageBuilder().setCommonPublicChatMessage(bisq.chat.protobuf.CommonPublicChatMessage.newBuilder()).build();
    }

    public static CommonPublicChatMessage fromProto(bisq.chat.protobuf.ChatMessage baseProto) {
        Optional<Citation> citation = baseProto.hasCitation() ?
                Optional.of(Citation.fromProto(baseProto.getCitation())) :
                Optional.empty();
        return new CommonPublicChatMessage(
                baseProto.getId(),
                ChatChannelDomain.fromProto(baseProto.getChatChannelDomain()),
                baseProto.getChannelName(),
                baseProto.getAuthorId(),
                Optional.of(baseProto.getText()),
                citation,
                baseProto.getDate(),
                baseProto.getWasEdited(),
                ChatMessageType.fromProto(baseProto.getChatMessageType()),
                MetaData.fromProto(baseProto.getMetaData()));
    }
}