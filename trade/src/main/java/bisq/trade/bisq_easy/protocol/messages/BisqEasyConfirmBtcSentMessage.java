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

package bisq.trade.bisq_easy.protocol.messages;

import bisq.common.validation.NetworkDataValidation;
import bisq.network.p2p.vo.NetworkId;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@ToString(callSuper = true)
@Getter
@EqualsAndHashCode(callSuper = true)
public final class BisqEasyConfirmBtcSentMessage extends BisqEasyTradeMessage {
    private final String txId;

    public BisqEasyConfirmBtcSentMessage(String id,
                                         String tradeId,
                                         NetworkId sender,
                                         NetworkId receiver,
                                         String txId) {
        super(id, tradeId, sender, receiver);

        this.txId = txId;

        // We tolerate non-btc txId data as well 
        NetworkDataValidation.validateText(txId, 200);

        // log.error("{} {}", metaData.getClassName(), toProto().getSerializedSize()); //412
    }

    @Override
    protected bisq.trade.protobuf.TradeMessage toTradeMessageProto() {
        return getTradeMessageBuilder()
                .setBisqEasyTradeMessage(bisq.trade.protobuf.BisqEasyTradeMessage.newBuilder()
                        .setBisqEasyConfirmBtcSentMessage(
                                bisq.trade.protobuf.BisqEasyConfirmBtcSentMessage.newBuilder()
                                        .setTxId(txId)))
                .build();
    }

    public static BisqEasyConfirmBtcSentMessage fromProto(bisq.trade.protobuf.TradeMessage proto) {
        bisq.trade.protobuf.BisqEasyConfirmBtcSentMessage bisqEasyConfirmBtcSentMessage = proto.getBisqEasyTradeMessage().getBisqEasyConfirmBtcSentMessage();
        return new BisqEasyConfirmBtcSentMessage(
                proto.getId(),
                proto.getTradeId(),
                NetworkId.fromProto(proto.getSender()),
                NetworkId.fromProto(proto.getReceiver()),
                bisqEasyConfirmBtcSentMessage.getTxId());
    }
}