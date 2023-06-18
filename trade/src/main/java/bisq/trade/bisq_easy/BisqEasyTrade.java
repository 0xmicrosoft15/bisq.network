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

package bisq.trade.bisq_easy;

import bisq.common.util.ProtobufUtils;
import bisq.contract.bisq_easy.BisqEasyContract;
import bisq.network.NetworkId;
import bisq.offer.bisq_easy.BisqEasyOffer;
import bisq.trade.Trade;
import bisq.trade.TradeParty;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@ToString(callSuper = true)
@EqualsAndHashCode(callSuper = true)
@Getter
public final class BisqEasyTrade extends Trade<BisqEasyOffer, BisqEasyContract> {
    public BisqEasyTrade(BisqEasyContract contract, NetworkId takerNetworkId) {
        super(contract, takerNetworkId);

        currentState.set(BisqEasyTradeState.INIT);
    }

    private BisqEasyTrade(String id, BisqEasyContract contract, TradeParty taker, TradeParty maker) {
        super(id, contract, taker, maker);
    }

    @Override
    public bisq.trade.protobuf.Trade toProto() {
        return getTradeBuilder().setBisqEasyTrade(bisq.trade.protobuf.BisqEasyTrade.newBuilder())
                .build();
    }

    public static BisqEasyTrade fromProto(bisq.trade.protobuf.Trade proto) {
        BisqEasyTrade bisqEasyTrade = new BisqEasyTrade(proto.getId(),
                BisqEasyContract.fromProto(proto.getContract()),
                TradeParty.fromProto(proto.getTaker()),
                TradeParty.fromProto(proto.getMaker()));
        bisqEasyTrade.getCurrentState().set(ProtobufUtils.enumFromProto(BisqEasyTradeState.class, proto.getState()));
        return bisqEasyTrade;
    }
}