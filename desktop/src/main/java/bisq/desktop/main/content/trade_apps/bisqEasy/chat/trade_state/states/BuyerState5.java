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

package bisq.desktop.main.content.trade_apps.bisqEasy.chat.trade_state.states;

import bisq.bonded_roles.service.explorer.ExplorerService;
import bisq.chat.bisqeasy.channel.BisqEasyChatChannelSelectionService;
import bisq.chat.bisqeasy.channel.priv.BisqEasyPrivateTradeChatChannel;
import bisq.chat.bisqeasy.channel.priv.BisqEasyPrivateTradeChatChannelService;
import bisq.common.encoding.Csv;
import bisq.common.util.FileUtils;
import bisq.desktop.ServiceProvider;
import bisq.desktop.common.Browser;
import bisq.desktop.common.utils.FileChooserUtil;
import bisq.desktop.components.controls.BisqText;
import bisq.desktop.components.controls.MaterialTextField;
import bisq.desktop.components.overlay.Popup;
import bisq.i18n.Res;
import bisq.settings.DontShowAgainService;
import bisq.trade.bisq_easy.BisqEasyTrade;
import de.jensd.fx.fontawesome.AwesomeIcon;
import javafx.geometry.Insets;
import javafx.scene.control.Button;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

import java.io.File;
import java.io.IOException;
import java.util.List;

@Slf4j
public class BuyerState5 extends BaseState {
    private final Controller controller;

    public BuyerState5(ServiceProvider serviceProvider, BisqEasyTrade bisqEasyTrade, BisqEasyPrivateTradeChatChannel channel) {
        controller = new Controller(serviceProvider, bisqEasyTrade, channel);
    }

    public View getView() {
        return controller.getView();
    }

    private static class Controller extends BaseState.Controller<Model, View> {
        private final ExplorerService explorerService;
        private final BisqEasyPrivateTradeChatChannelService bisqEasyPrivateTradeChatChannelService;
        private final BisqEasyChatChannelSelectionService bisqEasyChatChannelSelectionService;

        private Controller(ServiceProvider serviceProvider, BisqEasyTrade bisqEasyTrade, BisqEasyPrivateTradeChatChannel channel) {
            super(serviceProvider, bisqEasyTrade, channel);

            explorerService = serviceProvider.getBondedRolesService().getExplorerService();
            bisqEasyPrivateTradeChatChannelService = serviceProvider.getChatService().getBisqEasyPrivateTradeChatChannelService();
            bisqEasyChatChannelSelectionService = serviceProvider.getChatService().getBisqEasyChatChannelSelectionService();

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
            model.setTxId(model.getBisqEasyTrade().getTxId().get());
        }

        @Override
        public void onDeactivate() {
            super.onDeactivate();
        }

        private void onLeaveChannel() {
            String dontShowAgainId = "leaveTradeChannel";
            if (DontShowAgainService.showAgain(dontShowAgainId)) {
                new Popup().warning(Res.get("bisqEasy.channelSelection.private.leave.warn",
                                model.getChannel().getPeer().getUserName()))
                        .dontShowAgainId(dontShowAgainId)
                        .closeButtonText(Res.get("action.cancel"))
                        .actionButtonText(Res.get("bisqEasy.channelSelection.private.leave"))
                        .onAction(this::doLeaveChannel)
                        .show();
            } else {
                doLeaveChannel();
            }
        }

        private void doLeaveChannel() {
            bisqEasyPrivateTradeChatChannelService.leaveChannel(model.getChannel());
            bisqEasyChatChannelSelectionService.maybeSelectFirstChannel();
        }

        private void onExportTrade() {
            List<List<String>> tradeData = List.of(
                    List.of(
                            "Trade ID",
                            "BTC amount",
                            model.getQuoteCode() + " amount",
                            "Transaction ID",
                            "Receiver address",
                            "Payment method"
                    ),
                    List.of(
                            model.getBisqEasyTrade().getId(),
                            model.getFormattedBaseAmount(),
                            model.getFormattedQuoteAmount(),
                            model.getTxId(),
                            model.getBisqEasyTrade().getBtcAddress().get(),
                            model.getBisqEasyTrade().getContract().getQuoteSidePaymentMethodSpec().getDisplayString()
                    )
            );

            String csv = Csv.toCsv(tradeData);
            File file = FileChooserUtil.openFile(getView().getRoot().getScene(), "BisqEasyTrade.csv");
            if (file != null) {
                try {
                    FileUtils.writeToFile(csv, file);
                } catch (IOException e) {
                    new Popup().error(e).show();
                }
            }
        }

        private void openExplorer() {
            ExplorerService.Provider provider = explorerService.getSelectedProvider().get();
            String url = provider.getBaseUrl() + provider.getTxPath() + model.getTxId();
            Browser.open(url);
        }
    }

    @Getter
    private static class Model extends BaseState.Model {
        @Setter
        protected String txId;

        protected Model(BisqEasyTrade bisqEasyTrade, BisqEasyPrivateTradeChatChannel channel) {
            super(bisqEasyTrade, channel);
        }
    }

    public static class View extends BaseState.View<Model, Controller> {
        private final Button leaveButton, exportButton;
        private final MaterialTextField quoteAmount, baseAmount;
        private final MaterialTextField txId;

        private View(Model model, Controller controller) {
            super(model, controller);

            BisqText infoHeadline = new BisqText(Res.get("bisqEasy.tradeState.info.buyer.phase5.headline"));
            infoHeadline.getStyleClass().add("bisq-easy-trade-state-info-headline");

            exportButton = new Button(Res.get("bisqEasy.tradeState.info.phase5.exportTrade"));
            leaveButton = new Button(Res.get("bisqEasy.tradeState.info.phase5.leaveChannel"));
            quoteAmount = FormUtils.getTextField(Res.get("bisqEasy.tradeState.info.buyer.phase5.quoteAmount"), "", false);
            baseAmount = FormUtils.getTextField(Res.get("bisqEasy.tradeState.info.buyer.phase5.baseAmount"), "", false);

            txId = FormUtils.getTextField(Res.get("bisqEasy.tradeState.info.phase4.txId"), "", false);
            txId.setIcon(AwesomeIcon.EXTERNAL_LINK);
            txId.setIconTooltip(Res.get("bisqEasy.tradeState.info.phase4.txId.tooltip"));

            HBox buttons = new HBox(20, exportButton, leaveButton);
            VBox.setMargin(buttons, new Insets(5, 0, 5, 0));
            root.getChildren().addAll(
                    infoHeadline,
                    quoteAmount,
                    baseAmount,
                    txId,
                    buttons);
        }

        @Override
        protected void onViewAttached() {
            super.onViewAttached();

            txId.setText(model.getTxId());

            quoteAmount.setText(model.getFormattedQuoteAmount());
            baseAmount.setText(model.getFormattedBaseAmount());

            leaveButton.setOnAction(e -> controller.onLeaveChannel());
            exportButton.setOnAction(e -> controller.onExportTrade());
            txId.getIconButton().setOnAction(e -> controller.openExplorer());
        }

        @Override
        protected void onViewDetached() {
            super.onViewDetached();

            leaveButton.setOnAction(null);
            exportButton.setOnAction(null);
            txId.getIconButton().setOnAction(null);
        }
    }
}