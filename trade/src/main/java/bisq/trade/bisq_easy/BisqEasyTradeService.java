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

import bisq.common.application.Service;
import bisq.common.monetary.Monetary;
import bisq.contract.ContractService;
import bisq.contract.bisq_easy.BisqEasyContract;
import bisq.identity.Identity;
import bisq.identity.IdentityService;
import bisq.network.NetworkId;
import bisq.network.NetworkService;
import bisq.network.p2p.message.NetworkMessage;
import bisq.network.p2p.services.confidential.MessageListener;
import bisq.offer.OfferService;
import bisq.offer.bisq_easy.BisqEasyOffer;
import bisq.offer.payment_method.BitcoinPaymentMethodSpec;
import bisq.offer.payment_method.FiatPaymentMethodSpec;
import bisq.persistence.Persistence;
import bisq.persistence.PersistenceClient;
import bisq.persistence.PersistenceService;
import bisq.support.MediationService;
import bisq.support.SupportService;
import bisq.trade.ServiceProvider;
import bisq.trade.TradeException;
import bisq.trade.bisq_easy.protocol.*;
import bisq.trade.bisq_easy.protocol.events.*;
import bisq.trade.bisq_easy.protocol.messages.BisqEasyAccountDataMessage;
import bisq.trade.bisq_easy.protocol.messages.BisqEasyConfirmBtcSentMessage;
import bisq.trade.bisq_easy.protocol.messages.BisqEasyConfirmFiatSentMessage;
import bisq.trade.bisq_easy.protocol.messages.BisqEasyTakeOfferRequest;
import bisq.trade.protocol.Protocol;
import bisq.user.profile.UserProfile;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;

import static com.google.common.base.Preconditions.checkArgument;

@Slf4j
@Getter
public class BisqEasyTradeService implements PersistenceClient<BisqEasyTradeStore>, Service, MessageListener {
    @Getter
    private final BisqEasyTradeStore persistableStore = new BisqEasyTradeStore();
    @Getter
    private final Persistence<BisqEasyTradeStore> persistence;
    private final IdentityService identityService;
    private final OfferService offerService;
    private final ContractService contractService;
    private final MediationService mediationService;
    private final NetworkService networkService;
    private final ServiceProvider serviceProvider;

    // We don't persist the protocol, only the model.
    private final Map<String, BisqEasyProtocol> tradeProtocolById = new ConcurrentHashMap<>();

    public BisqEasyTradeService(NetworkService networkService,
                                IdentityService identityService,
                                PersistenceService persistenceService,
                                OfferService offerService,
                                ContractService contractService,
                                SupportService supportService) {
        this.networkService = networkService;
        this.identityService = identityService;
        this.offerService = offerService;
        this.contractService = contractService;
        this.mediationService = supportService.getMediationService();
        serviceProvider = new ServiceProvider(networkService,
                identityService,
                persistenceService,
                offerService,
                contractService,
                supportService);
        persistence = persistenceService.getOrCreatePersistence(this, persistableStore);
    }


    ///////////////////////////////////////////////////////////////////////////////////////////////////
    // Service
    ///////////////////////////////////////////////////////////////////////////////////////////////////

    public CompletableFuture<Boolean> initialize() {
        networkService.addMessageListener(this);

        persistableStore.getTradeById().values().forEach(this::createAndAddTradeProtocol);

        return CompletableFuture.completedFuture(true);
    }

    public CompletableFuture<Boolean> shutdown() {
        networkService.removeMessageListener(this);
        return CompletableFuture.completedFuture(true);
    }


    ///////////////////////////////////////////////////////////////////////////////////////////////////
    // MessageListener
    ///////////////////////////////////////////////////////////////////////////////////////////////////

    @Override
    public void onMessage(NetworkMessage networkMessage) {
        if (networkMessage instanceof BisqEasyTakeOfferRequest) {
            onBisqEasyTakeOfferMessage((BisqEasyTakeOfferRequest) networkMessage);
        } else if (networkMessage instanceof BisqEasyAccountDataMessage) {
            onBisqEasySendAccountDataMessage((BisqEasyAccountDataMessage) networkMessage);
        } else if (networkMessage instanceof BisqEasyConfirmFiatSentMessage) {
            onBisqEasyConfirmFiatSentMessage((BisqEasyConfirmFiatSentMessage) networkMessage);
        } else if (networkMessage instanceof BisqEasyConfirmBtcSentMessage) {
            onBisqEasyConfirmBtcSentMessage((BisqEasyConfirmBtcSentMessage) networkMessage);
        }
    }


    ///////////////////////////////////////////////////////////////////////////////////////////////////
    // Message event
    ///////////////////////////////////////////////////////////////////////////////////////////////////

    private void onBisqEasyTakeOfferMessage(BisqEasyTakeOfferRequest message) {
        NetworkId sender = message.getSender();
        BisqEasyContract bisqEasyContract = message.getBisqEasyContract();
        boolean isBuyer = bisqEasyContract.getOffer().getMakersDirection().isBuy();
        Identity myIdentity = serviceProvider.getIdentityService().findAnyIdentityByNodeId(bisqEasyContract.getOffer().getMakerNetworkId().getNodeId()).orElseThrow();
        BisqEasyTrade tradeModel = new BisqEasyTrade(isBuyer, false, myIdentity, bisqEasyContract, sender);

        if (findProtocol(tradeModel.getId()).isPresent()) {
            log.error("We received the BisqEasyTakeOfferRequest for an already existing protocol");
            return;
        }
        persistableStore.add(tradeModel);
        persist();

        Protocol<BisqEasyTrade> protocol = createAndAddTradeProtocol(tradeModel);
        try {
            protocol.handle(message);
        } catch (TradeException e) {
            log.error("Error at processing message " + message, e);
        }
    }

    private void onBisqEasySendAccountDataMessage(BisqEasyAccountDataMessage message) {
        findProtocol(message.getTradeId()).ifPresent(protocol -> {
            try {
                protocol.handle(message);
            } catch (TradeException e) {
                log.error("Error at processing message " + message, e);
            }
        });
    }

    private void onBisqEasyConfirmFiatSentMessage(BisqEasyConfirmFiatSentMessage message) {
        findProtocol(message.getTradeId()).ifPresent(protocol -> {
            try {
                protocol.handle(message);
            } catch (TradeException e) {
                log.error("Error at processing message " + message, e);
            }
        });
    }

    private void onBisqEasyConfirmBtcSentMessage(BisqEasyConfirmBtcSentMessage message) {
        findProtocol(message.getTradeId()).ifPresent(protocol -> {
            try {
                protocol.handle(message);
            } catch (TradeException e) {
                log.error("Error at processing message " + message, e);
            }
        });
    }


    ///////////////////////////////////////////////////////////////////////////////////////////////////
    // Events
    ///////////////////////////////////////////////////////////////////////////////////////////////////

    public BisqEasyTrade onTakeOffer(Identity takerIdentity,
                                     BisqEasyOffer bisqEasyOffer,
                                     Monetary baseSideAmount,
                                     Monetary quoteSideAmount,
                                     BitcoinPaymentMethodSpec bitcoinPaymentMethodSpec,
                                     FiatPaymentMethodSpec fiatPaymentMethodSpec) throws TradeException {
        Optional<UserProfile> mediator = serviceProvider.getMediationService().takerSelectMediator(bisqEasyOffer.getMakersUserProfileId());
        NetworkId takerNetworkId = takerIdentity.getNetworkId();
        BisqEasyContract contract = new BisqEasyContract(bisqEasyOffer,
                takerNetworkId,
                baseSideAmount.getValue(),
                quoteSideAmount.getValue(),
                bitcoinPaymentMethodSpec,
                fiatPaymentMethodSpec,
                mediator);
        boolean isBuyer = bisqEasyOffer.getTakersDirection().isBuy();
        BisqEasyTrade tradeModel = new BisqEasyTrade(isBuyer, true, takerIdentity, contract, takerNetworkId);

        checkArgument(findProtocol(tradeModel.getId()).isPresent(),
                "We received the BisqEasyTakeOfferRequest for an already existing protocol");

        persistableStore.add(tradeModel);

        Protocol<BisqEasyTrade> protocol = createAndAddTradeProtocol(tradeModel);
        protocol.handle(new BisqEasyTakeOfferEvent(takerIdentity, contract));
        persist();
        return tradeModel;
    }

    public void sellerSendsPaymentAccount(BisqEasyTrade tradeModel, String paymentAccountData) throws TradeException {
        BisqEasyProtocol protocol = findProtocol(tradeModel.getId()).orElseThrow();
        protocol.handle(new BisqEasyAccountDataEvent(paymentAccountData));
        persist();
    }

    public void buyerConfirmFiatSent(BisqEasyTrade tradeModel, String buyersBtcAddress) throws TradeException {
        BisqEasyProtocol protocol = findProtocol(tradeModel.getId()).orElseThrow();
        protocol.handle(new BisqEasyConfirmFiatSentEvent(buyersBtcAddress));
        persist();
    }

    public void sellerConfirmBtcSent(BisqEasyTrade tradeModel, String txId) throws TradeException {
        BisqEasyProtocol protocol = findProtocol(tradeModel.getId()).orElseThrow();
        protocol.handle(new BisqEasyConfirmFiatSentEvent(txId));
        persist();
    }

    public void btcConfirmed(BisqEasyTrade tradeModel) throws TradeException {
        BisqEasyProtocol protocol = findProtocol(tradeModel.getId()).orElseThrow();
        protocol.handle(new BisqEasyBtcConfirmedEvent());
        persist();
    }

    public void tradeCompleted(BisqEasyTrade tradeModel) throws TradeException {
        BisqEasyProtocol protocol = findProtocol(tradeModel.getId()).orElseThrow();
        protocol.handle(new BisqEasyTradeCompletedEvent());
        persist();
    }


    ///////////////////////////////////////////////////////////////////////////////////////////////////
    // Misc API
    ///////////////////////////////////////////////////////////////////////////////////////////////////

    public Optional<BisqEasyProtocol> findProtocol(String id) {
        return Optional.ofNullable(tradeProtocolById.get(id));
    }

    public Optional<BisqEasyTrade> findTrade(String tradeId) {
        return persistableStore.findTrade(tradeId);
    }


    ///////////////////////////////////////////////////////////////////////////////////////////////////
    // TradeProtocol factory
    ///////////////////////////////////////////////////////////////////////////////////////////////////

    private BisqEasyProtocol createAndAddTradeProtocol(BisqEasyTrade model) {
        String id = model.getId();
        BisqEasyProtocol tradeProtocol;
        boolean isBuyer = model.isBuyer();
        if (model.isTaker()) {
            if (isBuyer) {
                tradeProtocol = new BisqEasyBuyerAsTakerProtocol(serviceProvider, model);
            } else {
                tradeProtocol = new BisqEasySellerAsTakerProtocol(serviceProvider, model);
            }
        } else {
            if (isBuyer) {
                tradeProtocol = new BisqEasyBuyerAsMakerProtocol(serviceProvider, model);
            } else {
                tradeProtocol = new BisqEasySellerAsMakerProtocol(serviceProvider, model);
            }
        }
        tradeProtocolById.put(id, tradeProtocol);
        return tradeProtocol;
    }
}