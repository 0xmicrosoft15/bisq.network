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

import bisq.trade.bisq_easy.events.*;
import bisq.trade.bisq_easy.messages.BisqEasyConfirmFiatSentMessage;
import bisq.trade.bisq_easy.messages.BisqEasyConfirmFiatSentMessageHandler;

import static bisq.trade.bisq_easy.BisqEasyTradeState.*;

public class BisqEasySellerAsTakerProtocol extends BisqEasyProtocol {

    public BisqEasySellerAsTakerProtocol(ServiceProvider serviceProvider, BisqEasyTrade model) {
        super(serviceProvider, model);
    }

    public void configTransitions() {
        addTransition()
                .from(INIT)
                .on(BisqEasyTakeOfferEvent.class)
                .run(BisqEasyTakeOfferEventHandler.class)
                .to(TAKER_SEND_TAKE_OFFER_REQUEST);

        addTransition()
                .from(TAKER_SEND_TAKE_OFFER_REQUEST)
                .on(BisqEasyAccountDataEvent.class)
                .run(BisqEasyAccountDataEventHandler.class)
                .to(SELLER_SENT_ACCOUNT_DATA);

        addTransition()
                .from(SELLER_SENT_ACCOUNT_DATA)
                .on(BisqEasyConfirmFiatSentMessage.class)
                .run(BisqEasyConfirmFiatSentMessageHandler.class)
                .to(SELLER_RECEIVED_FIAT_SENT_CONFIRMATION);

        addTransition()
                .from(SELLER_RECEIVED_FIAT_SENT_CONFIRMATION)
                .on(BisqEasyConfirmBtcSentEvent.class)
                .run(BisqEasyConfirmBtcSentEventHandler.class)
                .to(SELLER_SENT_BTC_SENT_CONFIRMATION);

        addTransition()
                .from(SELLER_SENT_BTC_SENT_CONFIRMATION)
                .on(BisqEasyBtcConfirmedEvent.class)
                .run(BisqEasyBtcConfirmedEventHandler.class)
                .to(BTC_CONFIRMED);

        addTransition()
                .from(BTC_CONFIRMED)
                .on(BisqEasyTradeCompletedEvent.class)
                .to(COMPLETED);
    }
}