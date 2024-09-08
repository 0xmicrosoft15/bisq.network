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

package bisq.desktop.main.content.bisq_easy.take_offer.amount;

import bisq.bonded_roles.market_price.MarketPriceService;
import bisq.common.currency.Market;
import bisq.common.monetary.Monetary;
import bisq.desktop.ServiceProvider;
import bisq.desktop.common.view.Controller;
import bisq.desktop.main.content.bisq_easy.components.AmountComponent;
import bisq.i18n.Res;
import bisq.offer.Direction;
import bisq.offer.amount.OfferAmountFormatter;
import bisq.offer.amount.OfferAmountUtil;
import bisq.offer.bisq_easy.BisqEasyOffer;
import bisq.offer.price.PriceUtil;
import bisq.presentation.formatters.AmountFormatter;
import bisq.presentation.formatters.PriceFormatter;
import bisq.user.identity.UserIdentityService;
import bisq.user.reputation.ReputationService;
import javafx.beans.property.ReadOnlyObjectProperty;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.fxmisc.easybind.EasyBind;
import org.fxmisc.easybind.Subscription;

import java.util.Optional;

@Slf4j
public class TakeOfferAmountController implements Controller {
    private final TakeOfferAmountModel model;
    @Getter
    private final TakeOfferAmountView view;
    private final AmountComponent amountComponent;
    private final MarketPriceService marketPriceService;
    private final UserIdentityService userIdentityService;
    private final ReputationService reputationService;
    private Subscription baseSideAmountPin, quoteSideAmountPin;

    public TakeOfferAmountController(ServiceProvider serviceProvider) {
        model = new TakeOfferAmountModel();
        marketPriceService = serviceProvider.getBondedRolesService().getMarketPriceService();
        userIdentityService = serviceProvider.getUserService().getUserIdentityService();
        reputationService = serviceProvider.getUserService().getReputationService();
        amountComponent = new AmountComponent(serviceProvider, true);
        view = new TakeOfferAmountView(model, this, amountComponent.getView().getRoot());
    }

    public void init(BisqEasyOffer bisqEasyOffer, Optional<Monetary> takerAsSellersMaxAllowedAmount) {
        model.setBisqEasyOffer(bisqEasyOffer);

        Direction takersDirection = bisqEasyOffer.getTakersDirection();
        model.setHeadline(takersDirection.isBuy() ? Res.get("bisqEasy.takeOffer.amount.headline.buyer") : Res.get("bisqEasy.takeOffer.amount.headline.seller"));
        amountComponent.setDirection(takersDirection);
        Market market = bisqEasyOffer.getMarket();
        amountComponent.setMarket(market);

        PriceUtil.findQuote(marketPriceService, bisqEasyOffer.getPriceSpec(), bisqEasyOffer.getMarket())
                .ifPresent(amountComponent::setQuote);

        Optional<Monetary> optionalQuoteSideMinOrFixedAmount = OfferAmountUtil.findQuoteSideMinOrFixedAmount(marketPriceService, bisqEasyOffer);
        Optional<Monetary> optionalQuoteSideMaxOrFixedAmount = OfferAmountUtil.findQuoteSideMaxOrFixedAmount(marketPriceService, bisqEasyOffer);
        if (optionalQuoteSideMinOrFixedAmount.isPresent() && takerAsSellersMaxAllowedAmount.isPresent()) {
            Monetary maxAmount = takerAsSellersMaxAllowedAmount.get();
            amountComponent.setMinMaxRange(optionalQuoteSideMinOrFixedAmount.get(), maxAmount);

            long sellersScore = reputationService.getReputationScore(userIdentityService.getSelectedUserIdentity().getUserProfile()).getTotalScore();
            amountComponent.setDescription(Res.get("bisqEasy.takeOffer.amount.description.limitedByTakersReputation",
                    sellersScore,
                    OfferAmountFormatter.formatQuoteSideMinAmount(marketPriceService, bisqEasyOffer, false),
                    AmountFormatter.formatAmountWithCode(maxAmount)));
        } else if (optionalQuoteSideMinOrFixedAmount.isPresent() && optionalQuoteSideMaxOrFixedAmount.isPresent()) {
            Monetary maxAmount = optionalQuoteSideMaxOrFixedAmount.get();
            amountComponent.setMinMaxRange(optionalQuoteSideMinOrFixedAmount.get(), maxAmount);

            amountComponent.setDescription(Res.get("bisqEasy.takeOffer.amount.description",
                    OfferAmountFormatter.formatQuoteSideMinAmount(marketPriceService, bisqEasyOffer, false),
                    AmountFormatter.formatAmountWithCode(maxAmount)));
        } else {
            log.error("optionalQuoteSideMinOrFixedAmount or optionalQuoteSideMaxOrFixedAmount is not present");
        }

        String btcAmount = takersDirection.isBuy()
                ? Res.get("bisqEasy.component.amount.baseSide.tooltip.buyer.btcAmount")
                : Res.get("bisqEasy.component.amount.baseSide.tooltip.seller.btcAmount");
        Optional<String> priceQuoteOptional = PriceUtil.findQuote(marketPriceService, model.getBisqEasyOffer())
                .map(priceQuote -> "\n" + Res.get("bisqEasy.component.amount.baseSide.tooltip.taker.offerPrice", PriceFormatter.formatWithCode(priceQuote)));
        priceQuoteOptional.ifPresent(priceQuote -> amountComponent.setTooltip(String.format("%s%s", btcAmount, priceQuote)));
    }

    public ReadOnlyObjectProperty<Monetary> getTakersQuoteSideAmount() {
        return model.getTakersQuoteSideAmount();
    }

    public ReadOnlyObjectProperty<Monetary> getTakersBaseSideAmount() {
        return model.getTakersBaseSideAmount();
    }

    @Override
    public void onActivate() {
        baseSideAmountPin = EasyBind.subscribe(amountComponent.getBaseSideAmount(),
                amount -> model.getTakersBaseSideAmount().set(amount));
        quoteSideAmountPin = EasyBind.subscribe(amountComponent.getQuoteSideAmount(),
                amount -> model.getTakersQuoteSideAmount().set(amount));
    }

    @Override
    public void onDeactivate() {
        baseSideAmountPin.unsubscribe();
        quoteSideAmountPin.unsubscribe();
    }
}
