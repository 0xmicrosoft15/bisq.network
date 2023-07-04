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

import bisq.chat.bisqeasy.channel.priv.BisqEasyPrivateTradeChatChannel;
import bisq.desktop.DesktopApplicationService;
import bisq.desktop.components.controls.BisqText;
import bisq.desktop.components.controls.MaterialTextField;
import bisq.desktop.components.overlay.Popup;
import bisq.i18n.Res;
import bisq.trade.TradeException;
import bisq.trade.bisq_easy.BisqEasyTrade;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.geometry.Insets;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.VBox;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class SellerState3 extends BaseState {
    private final Controller controller;

    public SellerState3(DesktopApplicationService applicationService, BisqEasyTrade bisqEasyTrade, BisqEasyPrivateTradeChatChannel channel) {
        controller = new Controller(applicationService, bisqEasyTrade, channel);
    }

    public View getView() {
        return controller.getView();
    }

    private static class Controller extends BaseState.Controller<Model, View> {
        private Controller(DesktopApplicationService applicationService, BisqEasyTrade bisqEasyTrade, BisqEasyPrivateTradeChatChannel channel) {
            super(applicationService, bisqEasyTrade, channel);
        }

        @Override
        protected Model createModel(BisqEasyTrade bisqEasyTrade, BisqEasyPrivateTradeChatChannel channel) {
            return new Model(bisqEasyTrade, channel);
        }

        @Override
        protected View createView() {
            return new View(model, this);
        }

        @Override
        public void onActivate() {
            super.onActivate();
            model.setBtcAddress(model.getBisqEasyTrade().getBtcAddress().get());
            model.getButtonDisabled().bind(model.getTxId().isEmpty());
        }

        @Override
        public void onDeactivate() {
            super.onDeactivate();
            model.getButtonDisabled().unbind();
        }

        private void onBtcSent() {
            String txId = model.getTxId().get();
            sendChatBotMessage(Res.get("bisqEasy.tradeState.info.seller.phase3.chatBotMessage", txId));
            try {
                bisqEasyTradeService.sellerConfirmBtcSent(model.getBisqEasyTrade(), txId);
            } catch (TradeException e) {
                new Popup().error(e).show();
            }
        }
    }

    @Getter
    private static class Model extends BaseState.Model {
        @Setter
        protected String btcAddress;
        private final StringProperty txId = new SimpleStringProperty();
        private final BooleanProperty buttonDisabled = new SimpleBooleanProperty();

        protected Model(BisqEasyTrade bisqEasyTrade, BisqEasyPrivateTradeChatChannel channel) {
            super(bisqEasyTrade, channel);
        }
    }

    public static class View extends BaseState.View<Model, Controller> {
        private final Button button;
        private final MaterialTextField txId;
        private final BisqText infoHeadline;
        private final Label sendBtcLabel;
        private final MaterialTextField baseAmount;
        private final MaterialTextField btcAddress;

        private View(Model model, Controller controller) {
            super(model, controller);

            infoHeadline = new BisqText();
            infoHeadline.getStyleClass().add("bisq-easy-trade-state-info-headline");

            txId = FormUtils.getTextField(Res.get("bisqEasy.tradeState.info.seller.phase3.txId"), "", true);
            txId.textProperty().bindBidirectional(model.getTxId());
            txId.setPromptText(Res.get("bisqEasy.tradeState.info.seller.phase3.txId.prompt"));

            button = new Button(Res.get("bisqEasy.tradeState.info.seller.phase3.buttonText"));
            button.setDefaultButton(true);

            sendBtcLabel = FormUtils.getLabel("");
            baseAmount = FormUtils.getTextField(Res.get("bisqEasy.tradeState.info.seller.phase3.baseAmount"), "", false);
            btcAddress = FormUtils.getTextField(Res.get("bisqEasy.tradeState.info.seller.phase3.btcAddress"), "", false);
            VBox.setMargin(button, new Insets(5, 0, 5, 0));
            root.getChildren().addAll(
                    infoHeadline,
                    sendBtcLabel,
                    baseAmount,
                    btcAddress,
                    txId,
                    button);
        }

        @Override
        protected void onViewAttached() {
            super.onViewAttached();

            infoHeadline.setText(Res.get("bisqEasy.tradeState.info.seller.phase3.headline", model.getFormattedQuoteAmount()));
            sendBtcLabel.setText(Res.get("bisqEasy.tradeState.info.seller.phase3.sendBtc", model.getQuoteCode(), model.getFormattedBaseAmount()));
            baseAmount.setText(model.getFormattedBaseAmount());
            btcAddress.setText(model.getBtcAddress());

            txId.textProperty().bindBidirectional(model.getTxId());
            button.disableProperty().bind(model.getButtonDisabled());
            button.setOnAction(e -> controller.onBtcSent());
        }

        @Override
        protected void onViewDetached() {
            super.onViewDetached();

            txId.textProperty().unbindBidirectional(model.getTxId());
            button.disableProperty().unbind();
            button.setOnAction(null);
        }
    }
}