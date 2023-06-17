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

import bisq.trade.BuyerProtocol;
import bisq.trade.MakerProtocol;
import bisq.trade.bisq_easy.messages.BisqEasyAccountDataMessage;
import bisq.trade.bisq_easy.messages.BisqEasyAccountDataMessageHandler;
import bisq.trade.bisq_easy.messages.BisqEasyTakeOfferRequest;
import bisq.trade.bisq_easy.messages.BisqEasyTakeOfferRequestHandler;

import static bisq.trade.bisq_easy.BisqEasyTradeState.*;

public class BisqEasyBuyerAsMakerProtocol extends BisqEasyProtocol implements BuyerProtocol, MakerProtocol {

    public BisqEasyBuyerAsMakerProtocol(ServiceProvider serviceProvider, BisqEasyTrade model) {
        super(serviceProvider, model);
    }

    @Override
    public void configTransitions() {
        buildTransition()
                .from(INIT)
                .on(BisqEasyTakeOfferRequest.class)
                .run(BisqEasyTakeOfferRequestHandler.class)
                .to(MAKER_TAKE_OFFER_REQUEST_ACCEPTED);

        buildTransition()
                .from(MAKER_TAKE_OFFER_REQUEST_ACCEPTED)
                .on(BisqEasyAccountDataMessage.class)
                .run(BisqEasyAccountDataMessageHandler.class)
                .to(BUYER_ACCOUNT_DATA_RECEIVED);
    }
}