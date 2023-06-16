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

package bisq.protocol.bisq_easy.taker.handlers;

import bisq.common.monetary.Monetary;
import bisq.contract.ContractSignatureData;
import bisq.contract.bisq_easy.BisqEasyContract;
import bisq.identity.Identity;
import bisq.network.p2p.message.NetworkMessage;
import bisq.offer.bisq_easy.BisqEasyOffer;
import bisq.offer.payment_method.BitcoinPaymentMethodSpec;
import bisq.offer.payment_method.FiatPaymentMethodSpec;
import bisq.protocol.bisq_easy.BisqEasyProtocolModel;
import bisq.protocol.bisq_easy.ProtocolParty;
import bisq.protocol.bisq_easy.ServiceProvider;
import bisq.protocol.bisq_easy.taker.messages.BisqEasyTakeOfferRequest;
import bisq.protocol.fsm.EventHandler;
import bisq.user.profile.UserProfile;

import java.security.GeneralSecurityException;
import java.util.Optional;

public class TakeOfferHandler implements EventHandler {
    private final ServiceProvider serviceProvider;
    private final BisqEasyProtocolModel model;
    private final Identity takerIdentity;
    private final BisqEasyOffer bisqEasyOffer;
    private final Monetary baseSideAmount;
    private final Monetary quoteSideAmount;
    private final BitcoinPaymentMethodSpec bitcoinPaymentMethodSpec;
    private final FiatPaymentMethodSpec fiatPaymentMethodSpec;

    public TakeOfferHandler(ServiceProvider serviceProvider,
                            BisqEasyProtocolModel model,
                            Identity takerIdentity,
                            BisqEasyOffer bisqEasyOffer,
                            Monetary baseSideAmount,
                            Monetary quoteSideAmount,
                            BitcoinPaymentMethodSpec bitcoinPaymentMethodSpec,
                            FiatPaymentMethodSpec fiatPaymentMethodSpec) {
        this.serviceProvider = serviceProvider;
        this.model = model;
        this.takerIdentity = takerIdentity;
        this.bisqEasyOffer = bisqEasyOffer;
        this.baseSideAmount = baseSideAmount;
        this.quoteSideAmount = quoteSideAmount;
        this.bitcoinPaymentMethodSpec = bitcoinPaymentMethodSpec;
        this.fiatPaymentMethodSpec = fiatPaymentMethodSpec;
    }

    @Override
    public void handle() {
        Optional<UserProfile> mediator = serviceProvider.getMediationService().takerSelectMediator(bisqEasyOffer.getMakersUserProfileId());
        BisqEasyContract bisqEasyContract = new BisqEasyContract(bisqEasyOffer,
                takerIdentity.getNetworkId(),
                baseSideAmount.getValue(),
                quoteSideAmount.getValue(),
                bitcoinPaymentMethodSpec,
                fiatPaymentMethodSpec,
                mediator);
        try {
            ContractSignatureData contractSignatureData = serviceProvider.getContractService().signContract(bisqEasyContract, takerIdentity.getKeyPair());
            ProtocolParty taker = new ProtocolParty(takerIdentity.getNetworkId());
            taker.setContractSignatureData(contractSignatureData);
            ProtocolParty maker = new ProtocolParty(bisqEasyOffer.getMakerNetworkId());

            NetworkMessage networkMessage = new BisqEasyTakeOfferRequest(bisqEasyContract, contractSignatureData);
            serviceProvider.getNetworkService().confidentialSend(networkMessage, maker.getNetworkId(), takerIdentity.getNodeIdAndKeyPair())
                    .whenComplete((result, throwable) -> {
                        //todo
                    });

            model.setBisqEasyContract(bisqEasyContract);
            model.setTaker(taker);
            model.setMaker(maker);
        } catch (GeneralSecurityException e) {
            throw new RuntimeException(e);
        }
    }
}