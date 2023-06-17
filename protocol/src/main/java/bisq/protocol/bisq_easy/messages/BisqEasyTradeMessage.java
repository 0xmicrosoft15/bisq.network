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

package bisq.protocol.bisq_easy.messages;

import bisq.common.proto.ProtoResolver;
import bisq.common.proto.UnresolvableProtobufMessageException;
import bisq.network.NetworkId;
import bisq.network.p2p.message.NetworkMessage;
import bisq.network.p2p.services.data.storage.MetaData;
import bisq.network.p2p.services.data.storage.mailbox.MailboxMessage;
import bisq.network.protobuf.ExternalNetworkMessage;
import com.google.protobuf.Any;
import com.google.protobuf.InvalidProtocolBufferException;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;
import lombok.extern.slf4j.Slf4j;

import java.util.concurrent.TimeUnit;

@Slf4j
@ToString
@Getter
@EqualsAndHashCode
public abstract class BisqEasyTradeMessage implements MailboxMessage {
    public final static long TTL = TimeUnit.DAYS.toMillis(10);

    private final NetworkId sender;
    protected final MetaData metaData;

    protected BisqEasyTradeMessage(NetworkId sender, MetaData metaData) {
        this.sender = sender;
        this.metaData = metaData;
    }

    public bisq.protocol.protobuf.BisqEasyTradeMessage.Builder getBisqEasyTradeMessageBuilder() {
        return bisq.protocol.protobuf.BisqEasyTradeMessage.newBuilder()
                .setSender(sender.toProto())
                .setMetaData(metaData.toProto());
    }

    @Override
    public bisq.network.protobuf.NetworkMessage toProto() {
        return getNetworkMessageBuilder()
                .setExternalNetworkMessage(ExternalNetworkMessage.newBuilder().setAny(Any.pack(toBisqEasyTradeMessageProto())))
                .build();
    }

    protected abstract bisq.protocol.protobuf.BisqEasyTradeMessage toBisqEasyTradeMessageProto();

    public static BisqEasyTradeMessage fromProto(bisq.protocol.protobuf.BisqEasyTradeMessage proto) {
        switch (proto.getMessageCase()) {
            case BISQEASYTAKEOFFERREQUEST: {
                return BisqEasyTakeOfferRequest.fromProto(proto);
            }


            case MESSAGE_NOT_SET: {
                throw new UnresolvableProtobufMessageException(proto);
            }
        }
        throw new UnresolvableProtobufMessageException(proto);
    }

    public static ProtoResolver<NetworkMessage> getNetworkMessageResolver() {
        return any -> {
            try {
                bisq.protocol.protobuf.BisqEasyTradeMessage proto = any.unpack(bisq.protocol.protobuf.BisqEasyTradeMessage.class);
                switch (proto.getMessageCase()) {
                    case BISQEASYTAKEOFFERREQUEST: {
                        return BisqEasyTakeOfferRequest.fromProto(proto);
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

}