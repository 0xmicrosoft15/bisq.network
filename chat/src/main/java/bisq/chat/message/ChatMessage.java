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

import bisq.chat.bisqeasy.message.BisqEasyPrivateTradeChatMessage;
import bisq.chat.bisqeasy.message.BisqEasyPublicChatMessage;
import bisq.chat.channel.ChatChannelDomain;
import bisq.common.proto.Proto;
import bisq.common.proto.ProtoResolver;
import bisq.common.proto.UnresolvableProtobufMessageException;
import bisq.i18n.Res;
import bisq.network.p2p.message.NetworkMessage;
import bisq.network.p2p.services.data.storage.DistributedData;
import bisq.network.p2p.services.data.storage.MetaData;
import bisq.user.identity.UserIdentity;
import bisq.user.identity.UserIdentityService;
import com.google.protobuf.InvalidProtocolBufferException;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;
import lombok.extern.slf4j.Slf4j;

import javax.annotation.Nonnull;
import java.util.Optional;

@Slf4j
@ToString
@Getter
@EqualsAndHashCode
public abstract class ChatMessage implements Proto, Comparable<ChatMessage> {
    protected final String id;
    private final ChatChannelDomain chatChannelDomain;
    protected final String channelId;
    protected final Optional<String> optionalText;
    protected String authorUserProfileId;
    protected final Optional<Citation> citation;
    protected final long date;
    protected final boolean wasEdited;
    protected final ChatMessageType chatMessageType;
    protected final MetaData metaData;

    protected ChatMessage(String id,
                          ChatChannelDomain chatChannelDomain,
                          String channelId,
                          String authorUserProfileId,
                          Optional<String> text,
                          Optional<Citation> citation,
                          long date,
                          boolean wasEdited,
                          ChatMessageType chatMessageType,
                          MetaData metaData) {
        this.id = id;
        this.chatChannelDomain = chatChannelDomain;
        this.channelId = channelId;
        this.authorUserProfileId = authorUserProfileId;
        this.optionalText = text;
        this.citation = citation;
        this.date = date;
        this.wasEdited = wasEdited;
        this.chatMessageType = chatMessageType;
        this.metaData = metaData;
    }

    public bisq.chat.protobuf.ChatMessage.Builder getChatMessageBuilder() {
        bisq.chat.protobuf.ChatMessage.Builder builder = bisq.chat.protobuf.ChatMessage.newBuilder()
                .setId(id)
                .setChatChannelDomain(chatChannelDomain.toProto())
                .setChannelId(channelId)
                .setAuthorUserProfileId(authorUserProfileId)
                .setDate(date)
                .setWasEdited(wasEdited)
                .setChatMessageType(chatMessageType.toProto())
                .setMetaData(metaData.toProto());
        citation.ifPresent(citation -> builder.setCitation(citation.toProto()));
        optionalText.ifPresent(builder::setText);
        return builder;
    }

    public static ChatMessage fromProto(bisq.chat.protobuf.ChatMessage proto) {
        switch (proto.getMessageCase()) {
            case TWOPARTYPRIVATECHATMESSAGE: {
                return TwoPartyPrivateChatMessage.fromProto(proto);
            }

            case PUBLICBISQEASYOFFERCHATMESSAGE: {
                return BisqEasyPublicChatMessage.fromProto(proto);
            }
            case PRIVATEBISQEASYTRADECHATMESSAGE: {
                return BisqEasyPrivateTradeChatMessage.fromProto(proto);
            }

            case COMMONPUBLICCHATMESSAGE: {
                return CommonPublicChatMessage.fromProto(proto);
            }

            case MESSAGE_NOT_SET: {
                throw new UnresolvableProtobufMessageException(proto);
            }
        }
        throw new UnresolvableProtobufMessageException(proto);
    }

    public static ProtoResolver<DistributedData> getDistributedDataResolver() {
        return any -> {
            try {
                bisq.chat.protobuf.ChatMessage proto = any.unpack(bisq.chat.protobuf.ChatMessage.class);
                switch (proto.getMessageCase()) {
                    case PUBLICBISQEASYOFFERCHATMESSAGE: {
                        return BisqEasyPublicChatMessage.fromProto(proto);
                    }
                    case COMMONPUBLICCHATMESSAGE: {
                        return CommonPublicChatMessage.fromProto(proto);
                    }
                    case MESSAGE_NOT_SET: {
                        throw new UnresolvableProtobufMessageException(proto);
                    }
                }
                throw new UnresolvableProtobufMessageException(proto);
            } catch (InvalidProtocolBufferException e) {
                throw new UnresolvableProtobufMessageException(e);
            }
        };
    }

    public static ProtoResolver<NetworkMessage> getNetworkMessageResolver() {
        return any -> {
            try {
                bisq.chat.protobuf.ChatMessage proto = any.unpack(bisq.chat.protobuf.ChatMessage.class);
                switch (proto.getMessageCase()) {
                    case TWOPARTYPRIVATECHATMESSAGE: {
                        return TwoPartyPrivateChatMessage.fromProto(proto);
                    }

                    case PRIVATEBISQEASYTRADECHATMESSAGE: {
                        return BisqEasyPrivateTradeChatMessage.fromProto(proto);
                    }

                    case MESSAGE_NOT_SET: {
                        throw new UnresolvableProtobufMessageException(proto);
                    }
                }
                throw new UnresolvableProtobufMessageException(proto);
            } catch (InvalidProtocolBufferException e) {
                throw new UnresolvableProtobufMessageException(e);
            }
        };
    }


    ///////////////////////////////////////////////////////////////////////////////////////////
    // API
    ///////////////////////////////////////////////////////////////////////////////////////////

    public String getText() {
        return optionalText.orElse(Res.get("data.na"));
    }

    public boolean wasMentioned(UserIdentity userIdentity) {
        return getText().contains("@" + userIdentity.getUserName());
    }

    public boolean isExpired() {
        return (System.currentTimeMillis() - getDate() > getMetaData().getTtl());
    }

    public boolean isMyMessage(UserIdentityService userIdentityService) {
        return userIdentityService.isUserIdentityPresent(authorUserProfileId);
    }

    @Override
    public int compareTo(@Nonnull ChatMessage o) {
        return id.compareTo(o.getId());
    }
}