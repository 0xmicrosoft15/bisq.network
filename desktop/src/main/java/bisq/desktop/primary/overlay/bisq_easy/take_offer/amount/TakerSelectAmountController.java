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

package bisq.desktop.primary.overlay.bisq_easy.take_offer.amount;

import bisq.application.DefaultApplicationService;
import bisq.common.monetary.Monetary;
import bisq.desktop.common.view.Controller;
import bisq.desktop.primary.overlay.bisq_easy.components.AmountComponent;
import bisq.i18n.Res;
import bisq.offer.bisq_easy.BisqEasyOffer;
import javafx.beans.property.ReadOnlyObjectProperty;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

// TODO open bug when opening popup with amount screen the values are not set
@Slf4j
public class TakerSelectAmountController implements Controller {
    private final TakerSelectAmountModel model;
    @Getter
    private final TakerSelectAmountView view;
    private final AmountComponent amountComponent;

    public TakerSelectAmountController(DefaultApplicationService applicationService) {
        model = new TakerSelectAmountModel();

        amountComponent = new AmountComponent(applicationService, true);
        view = new TakerSelectAmountView(model, this, amountComponent.getView().getRoot());
    }

    public void setBisqEasyOffer(BisqEasyOffer bisqEasyOffer) {
        amountComponent.setMinMaxRange(bisqEasyOffer.getQuoteSideMinAmount(), bisqEasyOffer.getQuoteSideMaxAmount());
        amountComponent.setDirection(bisqEasyOffer.getDirection());
        amountComponent.setMarket(bisqEasyOffer.getMarket());

        String direction = bisqEasyOffer.getTakersDirection().isBuy() ?
                Res.get("buy").toUpperCase() :
                Res.get("sell").toUpperCase();
        amountComponent.setDescription(Res.get("bisqEasy.takeOffer.amount.description", bisqEasyOffer.getMarket().getQuoteCurrencyCode(), direction));
    }

    public ReadOnlyObjectProperty<Monetary> getBaseSideAmount() {
        return amountComponent.getBaseSideAmount();
    }

    public ReadOnlyObjectProperty<Monetary> getQuoteSideAmount() {
        return amountComponent.getQuoteSideAmount();
    }

    @Override
    public void onActivate() {
    }

    @Override
    public void onDeactivate() {
    }
}
