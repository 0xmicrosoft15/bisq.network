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

package bisq.trade.bisq_easy.protocol;

import bisq.trade.ServiceProvider;
import bisq.trade.TradeProtocolException;
import bisq.trade.bisq_easy.BisqEasyTrade;
import bisq.trade.bisq_easy.protocol.events.*;
import bisq.trade.bisq_easy.protocol.messages.*;

import static bisq.trade.bisq_easy.protocol.BisqEasyTradeState.*;

public class BisqEasySellerAsTakerProtocol extends BisqEasyProtocol {

    public BisqEasySellerAsTakerProtocol(ServiceProvider serviceProvider, BisqEasyTrade model) {
        super(serviceProvider, model);
    }

    @Override
    protected void configErrorHandling() {
        fromAny()
                .on(TradeProtocolException.class)
                .run(BisqEasyProtocolExceptionHandler.class)
                .to(FAILED);

        fromAny()
                .on(BisqEasyReportErrorMessage.class)
                .run(BisqEasyReportErrorMessageHandler.class)
                .to(FAILED_AT_PEER);
    }

    public void configTransitions() {
        from(INIT)
                .on(BisqEasyTakeOfferEvent.class)
                .run(BisqEasyTakeOfferEventHandler.class)
                .to(TAKER_SENT_TAKE_OFFER_REQUEST)
                .then()
                .from(TAKER_SENT_TAKE_OFFER_REQUEST)
                .on(BisqEasyTakeOfferResponse.class)
                .run(BisqEasyTakeOfferResponseHandler.class)
                .to(TAKER_RECEIVED_TAKE_OFFER_RESPONSE)
                .branch(path("Option 1: Buyer sends first btc address, then seller sends account data")
                                .from(TAKER_RECEIVED_TAKE_OFFER_RESPONSE)
                                .on(BisqEasyBtcAddressMessage.class)
                                .run(BisqEasyBtcAddressMessageHandler.class)
                                .to(SELLER_DID_NOT_SEND_ACCOUNT_DATA_AND_RECEIVED_BTC_ADDRESS)
                                .then()
                                .from(SELLER_DID_NOT_SEND_ACCOUNT_DATA_AND_RECEIVED_BTC_ADDRESS)
                                .on(BisqEasyAccountDataEvent.class)
                                .run(BisqEasyAccountDataEventHandler.class)
                                .to(SELLER_SENT_ACCOUNT_DATA_AND_RECEIVED_BTC_ADDRESS),
                        path("Option 2: Seller sends first account data, then buyer sends btc address")
                                .from(TAKER_RECEIVED_TAKE_OFFER_RESPONSE)
                                .on(BisqEasyAccountDataEvent.class)
                                .run(BisqEasyAccountDataEventHandler.class)
                                .to(SELLER_SENT_ACCOUNT_DATA_AND_WAITING_FOR_BTC_ADDRESS)
                                .then()
                                .from(SELLER_SENT_ACCOUNT_DATA_AND_WAITING_FOR_BTC_ADDRESS)
                                .on(BisqEasyBtcAddressMessage.class)
                                .run(BisqEasyBtcAddressMessageHandler.class)
                                .to(SELLER_SENT_ACCOUNT_DATA_AND_RECEIVED_BTC_ADDRESS))
                .then()
                .from(SELLER_SENT_ACCOUNT_DATA_AND_RECEIVED_BTC_ADDRESS)
                .on(BisqEasyConfirmFiatSentMessage.class)
                .run(BisqEasyConfirmFiatSentMessageHandler.class)
                .to(SELLER_RECEIVED_FIAT_SENT_CONFIRMATION)
                .then()
                .from(SELLER_RECEIVED_FIAT_SENT_CONFIRMATION)
                .on(BisqEasyConfirmFiatReceiptEvent.class)
                .run(BisqEasyConfirmFiatReceivedEventHandler.class)
                .to(SELLER_CONFIRMED_FIAT_RECEIPT)
                .then()
                .from(SELLER_CONFIRMED_FIAT_RECEIPT)
                .on(BisqEasyConfirmBtcSentEvent.class)
                .run(BisqEasyConfirmBtcSentEventHandler.class)
                .to(SELLER_SENT_BTC_SENT_CONFIRMATION)
                .then()
                .from(SELLER_SENT_BTC_SENT_CONFIRMATION)
                .on(BisqEasyBtcConfirmedEvent.class)
                .run(BisqEasyBtcConfirmedEventHandler.class)
                .to(BTC_CONFIRMED);


        // Reject trade
        fromStates(TAKER_SENT_TAKE_OFFER_REQUEST, TAKER_RECEIVED_TAKE_OFFER_RESPONSE)
                .on(BisqEasyRejectTradeEvent.class)
                .run(BisqEasyRejectTradeEventHandler.class)
                .to(REJECTED);

        // Peer rejected trade
        fromStates(TAKER_SENT_TAKE_OFFER_REQUEST, TAKER_RECEIVED_TAKE_OFFER_RESPONSE)
                .on(BisqEasyRejectTradeMessage.class)
                .run(BisqEasyRejectTradeMessageHandler.class)
                .to(PEER_REJECTED);

        // Cancel trade
        fromStates(SELLER_SENT_ACCOUNT_DATA_AND_WAITING_FOR_BTC_ADDRESS,
                SELLER_DID_NOT_SEND_ACCOUNT_DATA_AND_RECEIVED_BTC_ADDRESS,
                SELLER_SENT_ACCOUNT_DATA_AND_RECEIVED_BTC_ADDRESS,
                SELLER_RECEIVED_FIAT_SENT_CONFIRMATION,
                SELLER_CONFIRMED_FIAT_RECEIPT,
                SELLER_SENT_BTC_SENT_CONFIRMATION)
                .on(BisqEasyCancelTradeMessage.class)
                .run(BisqEasyCancelTradeMessageHandler.class)
                .to(CANCELLED);

        // Peer cancelled trade
        fromStates(SELLER_SENT_ACCOUNT_DATA_AND_WAITING_FOR_BTC_ADDRESS,
                SELLER_DID_NOT_SEND_ACCOUNT_DATA_AND_RECEIVED_BTC_ADDRESS,
                SELLER_SENT_ACCOUNT_DATA_AND_RECEIVED_BTC_ADDRESS,
                SELLER_RECEIVED_FIAT_SENT_CONFIRMATION,
                SELLER_CONFIRMED_FIAT_RECEIPT,
                SELLER_SENT_BTC_SENT_CONFIRMATION)
                .on(BisqEasyCancelTradeEvent.class)
                .run(BisqEasyCancelTradeEventHandler.class)
                .to(PEER_CANCELLED);
    }
}
