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

package bisq.desktop.main.content.bisq_easy.trade_wizard.btc_payment_method;

import bisq.account.payment_method.*;
import bisq.common.currency.Market;
import bisq.common.util.StringUtils;
import bisq.desktop.ServiceProvider;
import bisq.desktop.common.view.Controller;
import bisq.desktop.components.overlay.Popup;
import bisq.i18n.Res;
import bisq.offer.Direction;
import bisq.settings.CookieKey;
import bisq.settings.SettingsService;
import com.google.common.base.Joiner;
import javafx.collections.ObservableList;
import javafx.scene.layout.Region;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.fxmisc.easybind.EasyBind;
import org.fxmisc.easybind.Subscription;

import java.util.Comparator;
import java.util.HashSet;
import java.util.List;

@Slf4j
public class TradeWizardBtcPaymentMethodController implements Controller {
    private final TradeWizardBtcPaymentMethodModel model;
    @Getter
    private final TradeWizardBtcPaymentMethodView view;
    private final SettingsService settingsService;
    private final Runnable onNextHandler;
    private final Region owner;
    private Subscription customMethodPin;

    public TradeWizardBtcPaymentMethodController(ServiceProvider serviceProvider, Region owner, Runnable onNextHandler) {
        settingsService = serviceProvider.getSettingsService();
        this.onNextHandler = onNextHandler;
        this.owner = owner;

        model = new TradeWizardBtcPaymentMethodModel();
        view = new TradeWizardBtcPaymentMethodView(model, this);
    }

    public ObservableList<BitcoinPaymentMethod> getBitcoinPaymentMethods() {
        return model.getSelectedBitcoinPaymentMethods();
    }

    public boolean validate() {
        if (getCustomBitcoinPaymentMethodNameNotEmpty()) {
            tryAddCustomPaymentMethodAndNavigateNext();
            return true;
        }
        if (model.getSelectedBitcoinPaymentMethods().isEmpty()) {
            new Popup().invalid(Res.get("bisqEasy.tradeWizard.paymentMethod.warn.noPaymentMethodSelected"))
                    .owner(owner)
                    .show();
            return false;
        } else {
            return true;
        }
    }

    public boolean getCustomBitcoinPaymentMethodNameNotEmpty() {
        return StringUtils.isNotEmpty(model.getCustomBitcoinPaymentMethodName().get());
    }

    public void tryAddCustomPaymentMethodAndNavigateNext() {
        if (doAddCustomMethod()) {
            onNextHandler.run();
        }
    }

    public void setDirection(Direction direction) {
        if (direction != null) {
            model.setDirection(direction);
        }
    }

    public void setMarket(Market market) {
        if (market == null) {
            return;
        }

        model.getMarket().set(market);
        model.getSelectedBitcoinPaymentMethods().clear();
        model.getBitcoinPaymentMethod().setAll(BitcoinPaymentMethodUtil.getAllPaymentMethods());
        model.getBitcoinPaymentMethod().addAll(model.getAddedCustomBitcoinPaymentMethods());
        model.getIsPaymentMethodsEmpty().set(model.getBitcoinPaymentMethod().isEmpty());
    }

    public void reset() {
        model.reset();
    }

    @Override
    public void onActivate() {
        model.setHeadline(model.getDirection().isBuy() ?
                Res.get("bisqEasy.tradeWizard.paymentMethod.headline.buyer", model.getMarket().get().getQuoteCurrencyCode()) :
                Res.get("bisqEasy.tradeWizard.paymentMethod.headline.seller", model.getMarket().get().getQuoteCurrencyCode()));
        model.getCustomBitcoinPaymentMethodName().set("");
        model.getSortedBitcoinPaymentMethods().setComparator(Comparator.comparing(PaymentMethod::getShortDisplayString));
        settingsService.getCookie().asString(CookieKey.CREATE_OFFER_METHODS, getCookieSubKey())
                .ifPresent(names -> {
                    List.of(names.split(",")).forEach(name -> {
                        if (name.isEmpty()) {
                            return;
                        }
                        BitcoinPaymentMethod bitcoinPaymentMethod = BitcoinPaymentMethodUtil.getPaymentMethod(name);
                        boolean isCustomPaymentMethod = bitcoinPaymentMethod.isCustomPaymentMethod();
                        if (!isCustomPaymentMethod && isPredefinedPaymentMethodsContainName(name)) {
                            maybeAddBitcoinPaymentMethod(bitcoinPaymentMethod);
                        } else {
                            maybeAddCustomBitcoinPaymentMethod(bitcoinPaymentMethod);
                        }
                    });
                });
        customMethodPin = EasyBind.subscribe(model.getCustomBitcoinPaymentMethodName(),
                customMethod -> model.getIsAddCustomMethodIconEnabled().set(customMethod != null && !customMethod.isEmpty()));
    }

    @Override
    public void onDeactivate() {
        customMethodPin.unsubscribe();
    }

    boolean onTogglePaymentMethod(BitcoinPaymentMethod bitcoinPaymentMethod, boolean isSelected) {
        if (isSelected) {
            if (model.getSelectedBitcoinPaymentMethods().size() >= 4) {
                new Popup().warning(Res.get("bisqEasy.tradeWizard.paymentMethod.warn.maxMethodsReached")).show();
                return false;
            }
            maybeAddBitcoinPaymentMethod(bitcoinPaymentMethod);
        } else {
            model.getSelectedBitcoinPaymentMethods().remove(bitcoinPaymentMethod);
            setCookie();
        }
        return true;
    }

    void onAddCustomMethod() {
        doAddCustomMethod();
    }

    private boolean doAddCustomMethod() {
        if (model.getSelectedBitcoinPaymentMethods().size() >= 4) {
            new Popup().warning(Res.get("bisqEasy.tradeWizard.paymentMethod.warn.maxMethodsReached")).show();
            return false;
        }
        String customName = model.getCustomBitcoinPaymentMethodName().get();
        if (customName == null || customName.trim().isEmpty()) {
            return false;
        }
        if (customName.length() > 20) {
            new Popup().warning(Res.get("bisqEasy.tradeWizard.paymentMethod.warn.tooLong")).show();
            return false;
        }
        maybeAddCustomBitcoinPaymentMethod(BitcoinPaymentMethod.fromCustomName(customName));
        return true;
    }

    private void maybeAddBitcoinPaymentMethod(BitcoinPaymentMethod bitcoinPaymentMethod) {
        if (!model.getSelectedBitcoinPaymentMethods().contains(bitcoinPaymentMethod)) {
            model.getSelectedBitcoinPaymentMethods().add(bitcoinPaymentMethod);
            setCookie();
        }
        if (!model.getBitcoinPaymentMethod().contains(bitcoinPaymentMethod)) {
            model.getBitcoinPaymentMethod().add(bitcoinPaymentMethod);
        }
    }

    private void maybeAddCustomBitcoinPaymentMethod(BitcoinPaymentMethod bitcoinPaymentMethod) {
        if (bitcoinPaymentMethod != null) {
            if (!model.getAddedCustomBitcoinPaymentMethods().contains(bitcoinPaymentMethod)) {
                String customName = bitcoinPaymentMethod.getName().toUpperCase().strip();
                if (isPredefinedPaymentMethodsContainName(customName)) {
                    new Popup().warning(Res.get("bisqEasy.tradeWizard.paymentMethod.warn.customNameMatchesPredefinedMethod")).show();
                    model.getCustomBitcoinPaymentMethodName().set("");
                    return;
                }
                model.getAddedCustomBitcoinPaymentMethods().add(bitcoinPaymentMethod);
            }
            maybeAddBitcoinPaymentMethod(bitcoinPaymentMethod);
            model.getCustomBitcoinPaymentMethodName().set("");
        }
    }

    private boolean isPredefinedPaymentMethodsContainName(String name) {
        return new HashSet<>(PaymentMethodUtil.getPaymentMethodNames(model.getBitcoinPaymentMethod())).contains(name);
    }

    void onRemoveCustomMethod(BitcoinPaymentMethod bitcoinPaymentMethod) {
        model.getAddedCustomBitcoinPaymentMethods().remove(bitcoinPaymentMethod);
        model.getSelectedBitcoinPaymentMethods().remove(bitcoinPaymentMethod);
        model.getBitcoinPaymentMethod().remove(bitcoinPaymentMethod);
        setCookie();
    }

    private void setCookie() {
        settingsService.setCookie(CookieKey.CREATE_OFFER_METHODS, getCookieSubKey(),
                Joiner.on(",").join(PaymentMethodUtil.getPaymentMethodNames(model.getSelectedBitcoinPaymentMethods())));
    }

    private String getCookieSubKey() {
        return model.getMarket().get().getMarketCodes();
    }
}
