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

package bisq.trade.bisq_easy.events;

import bisq.common.fsm.Event;
import bisq.contract.ContractSignatureData;
import bisq.contract.bisq_easy.BisqEasyContract;
import bisq.identity.Identity;
import bisq.trade.bisq_easy.BisqEasyTrade;
import bisq.trade.bisq_easy.ServiceProvider;
import bisq.trade.bisq_easy.messages.BisqEasyTakeOfferRequest;
import bisq.trade.tasks.SendTradeMessageHandler;

import java.security.GeneralSecurityException;

public class BisqEasyTakeOfferEventHandler extends SendTradeMessageHandler<BisqEasyTrade> {

    public BisqEasyTakeOfferEventHandler(ServiceProvider serviceProvider, BisqEasyTrade model) {
        super(serviceProvider, model);
    }

    @Override
    public void handle(Event event) {
        BisqEasyTakeOfferEvent bisqEasyTakeOfferEvent = (BisqEasyTakeOfferEvent) event;
        BisqEasyContract bisqEasyContract = bisqEasyTakeOfferEvent.getBisqEasyContract();
        Identity takerIdentity = bisqEasyTakeOfferEvent.getTakerIdentity();
        try {
            ContractSignatureData contractSignatureData = serviceProvider.getContractService().signContract(bisqEasyContract, takerIdentity.getKeyPair());
            model.getMyself().getContractSignatureData().set(contractSignatureData);

            sendMessage(new BisqEasyTakeOfferRequest(model.getId(), takerIdentity.getNetworkId(), bisqEasyContract, contractSignatureData));
        } catch (GeneralSecurityException e) {
            throw new RuntimeException(e);
        }
    }
}