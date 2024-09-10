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

package bisq.desktop.main.content.bisq_easy.trade_wizard.amount;

import bisq.account.payment_method.BitcoinPaymentMethod;
import bisq.account.payment_method.FiatPaymentMethod;
import bisq.common.currency.Market;
import bisq.common.currency.MarketRepository;
import bisq.common.monetary.Monetary;
import bisq.common.monetary.PriceQuote;
import bisq.desktop.common.view.Model;
import bisq.offer.Direction;
import bisq.offer.amount.spec.QuoteSideAmountSpec;
import javafx.beans.property.*;
import lombok.Getter;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Getter
public class TradeWizardAmountModel implements Model {
    @Setter
    private Direction direction;
    @Setter
    private Market market = MarketRepository.getDefault();
    @Setter
    private List<BitcoinPaymentMethod> bitcoinPaymentMethods = new ArrayList<>();
    @Setter
    private List<FiatPaymentMethod> fiatPaymentMethods = new ArrayList<>();
    @Setter
    private String headline;
    private final StringProperty amountLimitInfoLeft = new SimpleStringProperty();
    private final StringProperty amountLimitInfoAmount = new SimpleStringProperty();
    private final StringProperty amountLimitInfoRight = new SimpleStringProperty();
    private final StringProperty amountLimitInfoOverlayInfo = new SimpleStringProperty();
    @Setter
    private String amountLimitInfoLink;
    @Setter
    private boolean isCreateOfferMode;
    @Setter
    private Optional<Monetary> baseSideAmount = Optional.empty();
    private final BooleanProperty showRangeAmounts = new SimpleBooleanProperty();
    private final BooleanProperty isMinAmountEnabled = new SimpleBooleanProperty();
    private final BooleanProperty isAmountLimitInfoOverlayVisible = new SimpleBooleanProperty();
    private final BooleanProperty isWarningIconVisible = new SimpleBooleanProperty();
    private final StringProperty toggleButtonText = new SimpleStringProperty();
    private final StringProperty priceTooltip = new SimpleStringProperty();
    private final ObjectProperty<QuoteSideAmountSpec> quoteSideAmountSpec = new SimpleObjectProperty<>();
    private final ObjectProperty<PriceQuote> priceQuote = new SimpleObjectProperty<>();
    private final StringProperty errorMessage = new SimpleStringProperty();

    public void reset() {
        direction = null;
        market = MarketRepository.getDefault();
        bitcoinPaymentMethods = new ArrayList<>();
        fiatPaymentMethods = new ArrayList<>();
        headline = null;
        amountLimitInfoLeft.set(null);
        amountLimitInfoAmount.set(null);
        amountLimitInfoRight.set(null);
        amountLimitInfoOverlayInfo.set(null);
        amountLimitInfoLink = null;
        isCreateOfferMode = false;
        baseSideAmount = Optional.empty();
        showRangeAmounts.set(false);
        isMinAmountEnabled.set(false);
        isAmountLimitInfoOverlayVisible.set(false);
        isWarningIconVisible.set(false);
        toggleButtonText.set(null);
        priceTooltip.set(null);
        quoteSideAmountSpec.set(null);
        priceQuote.set(null);
        errorMessage.set(null);
    }
}
