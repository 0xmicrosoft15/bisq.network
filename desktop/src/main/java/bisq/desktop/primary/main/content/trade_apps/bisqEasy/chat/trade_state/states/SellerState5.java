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

package bisq.desktop.primary.main.content.trade_apps.bisqEasy.chat.trade_state.states;

import bisq.application.DefaultApplicationService;
import bisq.chat.bisqeasy.channel.priv.BisqEasyPrivateTradeChatChannel;
import bisq.desktop.common.utils.Layout;
import bisq.desktop.components.controls.BisqText;
import bisq.desktop.components.controls.MaterialTextField;
import bisq.desktop.components.overlay.Popup;
import bisq.i18n.Res;
import bisq.network.NetworkId;
import bisq.offer.bisq_easy.BisqEasyOffer;
import bisq.trade.TradeException;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.geometry.Insets;
import javafx.scene.control.Button;
import javafx.scene.layout.VBox;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class SellerState5 extends BaseState {
    private final Controller controller;

    public SellerState5(DefaultApplicationService applicationService, BisqEasyOffer bisqEasyOffer, NetworkId takerNetworkId, BisqEasyPrivateTradeChatChannel channel) {
        controller = new Controller(applicationService, bisqEasyOffer, takerNetworkId, channel);
    }

    public View getView() {
        return controller.getView();
    }

    private static class Controller extends BaseState.Controller<Model, View> {
        private Controller(DefaultApplicationService applicationService, BisqEasyOffer bisqEasyOffer, NetworkId takerNetworkId, BisqEasyPrivateTradeChatChannel channel) {
            super(applicationService, bisqEasyOffer, takerNetworkId, channel);
        }

        @Override
        protected Model createModel() {
            return new Model();
        }

        @Override
        protected View createView() {
            return new View(model, this);
        }

        @Override
        public void onActivate() {
            super.onActivate();
        }

        @Override
        public void onDeactivate() {
            super.onDeactivate();
        }

        private void onTradeCompleted() {
            try {
                bisqEasyTradeService.tradeCompleted(model.getBisqEasyTradeModel());
            } catch (TradeException e) {
                new Popup().error(e).show();
            }
        }
    }

    @Getter
    private static class Model extends BaseState.Model {
        private final StringProperty btcBalance = new SimpleStringProperty();
        private final BooleanProperty buttonDisabled = new SimpleBooleanProperty();
    }

    public static class View extends BaseState.View<Model, Controller> {
        private final Button button;
        private final MaterialTextField quoteAmount;
        private final MaterialTextField baseAmount;

        private View(Model model, Controller controller) {
            super(model, controller);

            BisqText infoHeadline = new BisqText(Res.get("bisqEasy.tradeState.info.seller.phase5.headline"));
            infoHeadline.getStyleClass().add("bisq-easy-trade-state-info-headline");

            button = new Button(Res.get("bisqEasy.tradeState.info.phase5.buttonText"));
            button.setDefaultButton(true);
            quoteAmount = FormUtils.getTextField(Res.get("bisqEasy.tradeState.info.seller.phase5.quoteAmount"), "", false);
            baseAmount = FormUtils.getTextField(Res.get("bisqEasy.tradeState.info.seller.phase5.baseAmount"), "", false);
            VBox.setMargin(button, new Insets(5, 0, 0, 0));
            root.getChildren().addAll(Layout.hLine(),
                    infoHeadline,
                    quoteAmount,
                    baseAmount,
                    button);
        }

        @Override
        protected void onViewAttached() {
            super.onViewAttached();

            quoteAmount.setText(model.getFormattedQuoteAmount());
            baseAmount.setText(model.getFormattedBaseAmount());

            button.disableProperty().bind(model.getButtonDisabled());
            button.setOnAction(e -> controller.onTradeCompleted());
        }

        @Override
        protected void onViewDetached() {
            super.onViewDetached();

            button.disableProperty().unbind();
            button.setOnAction(null);
        }
    }
}