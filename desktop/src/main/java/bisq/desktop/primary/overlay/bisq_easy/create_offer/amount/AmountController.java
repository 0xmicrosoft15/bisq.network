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

package bisq.desktop.primary.overlay.bisq_easy.create_offer.amount;

import bisq.application.DefaultApplicationService;
import bisq.common.currency.Market;
import bisq.common.monetary.Quote;
import bisq.desktop.common.view.Controller;
import bisq.desktop.primary.overlay.bisq_easy.components.AmountComponent;
import bisq.i18n.Res;
import bisq.offer.Direction;
import bisq.offer.amount.spec.AmountSpec;
import bisq.offer.amount.spec.FixQuoteAmountSpec;
import bisq.offer.amount.spec.MinMaxQuoteAmountSpec;
import bisq.offer.price.PriceUtil;
import bisq.offer.price.spec.FixPriceSpec;
import bisq.offer.price.spec.FloatPriceSpec;
import bisq.offer.price.spec.PriceSpec;
import bisq.oracle.marketprice.MarketPriceService;
import bisq.settings.CookieKey;
import bisq.settings.SettingsService;
import javafx.beans.property.ReadOnlyBooleanProperty;
import javafx.beans.property.ReadOnlyObjectProperty;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.fxmisc.easybind.EasyBind;
import org.fxmisc.easybind.Subscription;

@Slf4j
public class AmountController implements Controller {
    private final AmountModel model;
    @Getter
    private final AmountView view;
    private final AmountComponent minAmountComponent, maxOrFixAmountComponent;
    private final SettingsService settingsService;
    private final MarketPriceService marketPriceService;
    private Subscription isMinAmountEnabledPin, maxOrFixAmountCompBaseSideAmountPin, minAmountCompBaseSideAmountPin,
            maxAmountCompQuoteSideAmountPin, minAmountCompQuoteSideAmountPin;

    public AmountController(DefaultApplicationService applicationService) {
        settingsService = applicationService.getSettingsService();
        marketPriceService = applicationService.getOracleService().getMarketPriceService();
        model = new AmountModel();

        minAmountComponent = new AmountComponent(applicationService, true);
        minAmountComponent.setDescription(Res.get("onboarding.amount.description.minAmount"));
        maxOrFixAmountComponent = new AmountComponent(applicationService, true);

        view = new AmountView(model, this,
                minAmountComponent,
                maxOrFixAmountComponent);
    }

    public void setDirection(Direction direction) {
        if (direction == null) {
            return;
        }
        minAmountComponent.setDirection(direction);
        maxOrFixAmountComponent.setDirection(direction);
    }

    public void setMarket(Market market) {
        if (market == null) {
            return;
        }
        minAmountComponent.setMarket(market);
        maxOrFixAmountComponent.setMarket(market);
        model.setMarket(market);
    }

    public void setPriceSpec(PriceSpec priceSpec) {
        Quote quote;
        if (priceSpec instanceof FixPriceSpec) {
            quote = ((FixPriceSpec) priceSpec).getQuote();
        } else if (priceSpec instanceof FloatPriceSpec) {
            double percentage = ((FloatPriceSpec) priceSpec).getPercentage();
            quote = PriceUtil.fromMarketPriceMarkup(getMarketPriceQuote(), percentage);
        } else {
            quote = getMarketPriceQuote();
        }
        minAmountComponent.setQuote(quote);
        maxOrFixAmountComponent.setQuote(quote);
    }

    public void reset() {
        minAmountComponent.reset();
        maxOrFixAmountComponent.reset();
        model.reset();
    }

    public ReadOnlyObjectProperty<AmountSpec> getAmountSpec() {
        return model.getAmountSpec();
    }

    public ReadOnlyBooleanProperty getIsMinAmountEnabled() {
        return model.getIsMinAmountEnabled();
    }

    @Override
    public void onActivate() {
        model.getIsMinAmountEnabled().set(settingsService.getCookie().asBoolean(CookieKey.CREATE_BISQ_EASY_OFFER_IS_MIN_AMOUNT_ENABLED).orElse(false));
        minAmountCompBaseSideAmountPin = EasyBind.subscribe(minAmountComponent.getBaseSideAmount(),
                value -> {
                    if (model.getIsMinAmountEnabled().get()) {
                        if (value != null && maxOrFixAmountComponent.getBaseSideAmount().get() != null &&
                                value.getValue() > maxOrFixAmountComponent.getBaseSideAmount().get().getValue()) {
                            maxOrFixAmountComponent.setBaseSideAmount(value);
                        }
                    }
                });
        maxOrFixAmountCompBaseSideAmountPin = EasyBind.subscribe(maxOrFixAmountComponent.getBaseSideAmount(),
                value -> {
                    if (model.getIsMinAmountEnabled().get() &&
                            value != null && minAmountComponent.getBaseSideAmount().get() != null &&
                            value.getValue() < minAmountComponent.getBaseSideAmount().get().getValue()) {
                        minAmountComponent.setBaseSideAmount(value);
                    }
                });

        minAmountCompQuoteSideAmountPin = EasyBind.subscribe(minAmountComponent.getQuoteSideAmount(),
                value -> {
                    if (model.getIsMinAmountEnabled().get()) {
                        if (value != null && maxOrFixAmountComponent.getQuoteSideAmount().get() != null &&
                                value.getValue() > maxOrFixAmountComponent.getQuoteSideAmount().get().getValue()) {
                            maxOrFixAmountComponent.setQuoteSideAmount(value);

                        }
                    }
                    applyAmountSpec();
                });
        maxAmountCompQuoteSideAmountPin = EasyBind.subscribe(maxOrFixAmountComponent.getQuoteSideAmount(),
                value -> {
                    if (model.getIsMinAmountEnabled().get() &&
                            value != null && minAmountComponent.getQuoteSideAmount().get() != null &&
                            value.getValue() < minAmountComponent.getQuoteSideAmount().get().getValue()) {
                        minAmountComponent.setQuoteSideAmount(value);
                    }
                    applyAmountSpec();
                });

        isMinAmountEnabledPin = EasyBind.subscribe(model.getIsMinAmountEnabled(), isMinAmountEnabled -> {
            model.getToggleButtonText().set(isMinAmountEnabled ?
                    Res.get("onboarding.amount.removeMinAmountOption") :
                    Res.get("onboarding.amount.addMinAmountOption"));

            maxOrFixAmountComponent.setDescription(isMinAmountEnabled ?
                    Res.get("onboarding.amount.description.maxAmount") :
                    Res.get("onboarding.amount.description.fixAmount"));

            applyAmountSpec();
        });

        applyAmountSpec();
    }

    @Override
    public void onDeactivate() {
        isMinAmountEnabledPin.unsubscribe();
        maxOrFixAmountCompBaseSideAmountPin.unsubscribe();
        maxAmountCompQuoteSideAmountPin.unsubscribe();
        minAmountCompBaseSideAmountPin.unsubscribe();
        minAmountCompQuoteSideAmountPin.unsubscribe();
    }

    void onToggleMinAmountVisibility() {
        boolean value = !model.getIsMinAmountEnabled().get();
        model.getIsMinAmountEnabled().set(value);
        settingsService.setCookie(CookieKey.CREATE_BISQ_EASY_OFFER_IS_MIN_AMOUNT_ENABLED, value);
    }

    private void applyAmountSpec() {
        long maxOrFixAmount = maxOrFixAmountComponent.getQuoteSideAmount().get().getValue();

        if (model.getIsMinAmountEnabled().get()) {
            long minAmount = minAmountComponent.getQuoteSideAmount().get().getValue();
            if (minAmount == maxOrFixAmount) {
                model.getAmountSpec().set(new FixQuoteAmountSpec(maxOrFixAmount));
            } else {
                model.getAmountSpec().set(new MinMaxQuoteAmountSpec(minAmount, maxOrFixAmount));
            }
        } else {
            model.getAmountSpec().set(new FixQuoteAmountSpec(maxOrFixAmount));
        }
    }

    private Quote getMarketPriceQuote() {
        return marketPriceService.findMarketPriceQuote(model.getMarket()).orElseThrow();
    }

}
