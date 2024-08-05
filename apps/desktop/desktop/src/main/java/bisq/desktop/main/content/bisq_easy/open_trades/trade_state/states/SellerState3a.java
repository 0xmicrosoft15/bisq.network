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

package bisq.desktop.main.content.bisq_easy.open_trades.trade_state.states;

import bisq.account.payment_method.BitcoinPaymentRail;
import bisq.chat.bisqeasy.open_trades.BisqEasyOpenTradeChannel;
import bisq.common.data.Pair;
import bisq.common.validation.BitcoinDataValidation;
import bisq.common.util.MathUtils;
import bisq.desktop.CssConfig;
import bisq.desktop.ServiceProvider;
import bisq.desktop.common.Layout;
import bisq.desktop.common.qr.QrCodeDisplay;
import bisq.desktop.common.threading.UIScheduler;
import bisq.desktop.common.utils.ClipboardUtil;
import bisq.desktop.common.utils.KeyHandlerUtil;
import bisq.desktop.components.controls.BisqTooltip;
import bisq.desktop.components.controls.MaterialTextField;
import bisq.desktop.components.controls.WrappingText;
import bisq.desktop.components.controls.validator.BitcoinDataValidator;
import bisq.desktop.components.controls.validator.ValidatorBase;
import bisq.desktop.components.overlay.Popup;
import bisq.desktop.overlay.OverlayController;
import bisq.i18n.Res;
import bisq.settings.SettingsService;
import bisq.trade.bisq_easy.BisqEasyTrade;
import javafx.animation.Interpolator;
import javafx.animation.KeyFrame;
import javafx.animation.KeyValue;
import javafx.animation.Timeline;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.collections.ObservableList;
import javafx.geometry.Bounds;
import javafx.geometry.Insets;
import javafx.geometry.Point2D;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Paint;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.Window;
import javafx.util.Duration;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.fxmisc.easybind.EasyBind;
import org.fxmisc.easybind.Subscription;

import java.util.Optional;

@Slf4j
public class SellerState3a extends BaseState {
    private final Controller controller;

    public SellerState3a(ServiceProvider serviceProvider,
                         BisqEasyTrade bisqEasyTrade,
                         BisqEasyOpenTradeChannel channel) {
        controller = new Controller(serviceProvider, bisqEasyTrade, channel);
    }

    public View getView() {
        return controller.getView();
    }

    private static class Controller extends BaseState.Controller<Model, View> {
        private final SettingsService settingsService;

        private Controller(ServiceProvider serviceProvider,
                           BisqEasyTrade bisqEasyTrade,
                           BisqEasyOpenTradeChannel channel) {
            super(serviceProvider, bisqEasyTrade, channel);

            settingsService = serviceProvider.getSettingsService();
        }

        @Override
        protected Model createModel(BisqEasyTrade bisqEasyTrade, BisqEasyOpenTradeChannel channel) {
            return new Model(bisqEasyTrade, channel);
        }

        @Override
        protected View createView() {
            return new View(model, this);
        }

        @Override
        public void onActivate() {
            super.onActivate();

            model.setUseAnimations(settingsService.getUseAnimations().get());
            model.setApplicationRoot(OverlayController.getInstance().getApplicationRoot());

            BitcoinPaymentRail paymentRail = model.getBisqEasyTrade().getContract().getBaseSidePaymentMethodSpec().getPaymentMethod().getPaymentRail();
            String name = paymentRail.name();
            model.setBitcoinPaymentDescription(Res.get("bisqEasy.tradeState.info.seller.phase3a.bitcoinPayment.description." + name));
            model.setPaymentProofDescription(Res.get("bisqEasy.tradeState.info.seller.phase3a.paymentProof.description." + name));
            model.setPaymentProofPrompt(Res.get("bisqEasy.tradeState.info.seller.phase3a.paymentProof.prompt." + name));

            model.setBitcoinPaymentData(model.getBisqEasyTrade().getBitcoinPaymentData().get());
            double factor = 2.5;
            if (paymentRail == BitcoinPaymentRail.MAIN_CHAIN) {
                // Typical bitcoin address require size of 29 or a multiple of it
                model.setSmallQrCodeSize(116); //233
                model.getBtcSentButtonDisabled().bind(model.getPaymentProof().isEmpty());
            } else {
                // TypicalLN invoice require size of 65 or a multiple of it
                model.setSmallQrCodeSize(130);
            }
            model.setLargeQrCodeSize(MathUtils.roundDoubleToInt(factor * model.getSmallQrCodeSize()));
            model.setSmallQrCodeImage(QrCodeDisplay.toImage(model.getBitcoinPaymentData(), model.getSmallQrCodeSize()));
        }

        @Override
        public void onDeactivate() {
            super.onDeactivate();

            model.getBtcSentButtonDisabled().unbind();

            model.getQrCodeWindow().set(null);
            model.setLargeQrCodeImage(null);
            model.setSmallQrCodeImage(null);
            doCloseQrCodeWindow();
        }

        private void onBtcSent() {
            String name = model.getBisqEasyTrade().getContract().getBaseSidePaymentMethodSpec().getPaymentMethod().getPaymentRail().name();
            String proof = Res.get("bisqEasy.tradeState.info.seller.phase3a.tradeLogMessage.paymentProof." + name);
            String userName = model.getChannel().getMyUserIdentity().getUserName();
            String txId = model.getPaymentProof().get();
            if (txId == null) {
                sellerConfirmedBtcSent(null, userName, proof);
            } else {
                if (BitcoinDataValidation.validateTransactionId(txId)) {
                    sellerConfirmedBtcSent(txId, userName, proof);
                } else {
                    new Popup().warning(Res.get("bisqEasy.tradeState.info.seller.phase3a.txId.warning"))
                            .actionButtonText(Res.get("bisqEasy.tradeState.info.seller.phase3a.txId.warning.proceed"))
                            .onAction(() -> { sellerConfirmedBtcSent(txId, userName, proof); }).show();
                }
            }
        }

        private void sellerConfirmedBtcSent(String txId, String userName, String proof) {
            if (txId == null) {
                sendTradeLogMessage(Res.encode("bisqEasy.tradeState.info.seller.phase3a.tradeLogMessage.noProofProvided", userName));
            } else {
                sendTradeLogMessage(Res.encode("bisqEasy.tradeState.info.seller.phase3a.tradeLogMessage",
                        userName, proof, txId));
            }
            bisqEasyTradeService.sellerConfirmBtcSent(model.getBisqEasyTrade(), Optional.ofNullable(model.getPaymentProof().get()));
        }

        void onShowQrCodeDisplay() {
            if (model.getQrCodeWindow().get() == null) {
                if (model.getLargeQrCodeImage() == null) {
                    model.setLargeQrCodeImage(QrCodeDisplay.toImage(model.getBitcoinPaymentData(), model.getLargeQrCodeSize(), 2));
                }
                model.getQrCodeWindow().set(new Stage());
            }
        }

        void onSceneCreated(Scene scene) {
            scene.addEventHandler(KeyEvent.KEY_PRESSED, this::onKeyEvent);
            model.getApplicationRoot().getScene().addEventHandler(KeyEvent.KEY_PRESSED, this::onKeyEvent);
        }

        void onCloseQrCodeWindow() {
            doCloseQrCodeWindow();
        }

        private void doCloseQrCodeWindow() {
            model.getApplicationRoot().getScene().removeEventHandler(KeyEvent.KEY_PRESSED, this::onKeyEvent);

            Stage qrCodeWindow = model.getQrCodeWindow().get();
            if (qrCodeWindow != null) {
                Scene scene = qrCodeWindow.getScene();
                if (scene != null) {
                    scene.removeEventHandler(KeyEvent.KEY_PRESSED, this::onKeyEvent);
                }
                qrCodeWindow.hide();
                model.getQrCodeWindow().set(null);
            }
        }

        void onKeyEvent(KeyEvent keyEvent) {
            KeyHandlerUtil.handleEscapeKeyEvent(keyEvent, this::doCloseQrCodeWindow);
            KeyHandlerUtil.handleShutDownKeyEvent(keyEvent, this::doCloseQrCodeWindow);
        }
    }

    @Getter
    private static class Model extends BaseState.Model {
        @Setter
        protected String bitcoinPaymentData;
        private final StringProperty paymentProof = new SimpleStringProperty();
        private final BooleanProperty btcSentButtonDisabled = new SimpleBooleanProperty();
        private final BooleanProperty showQrCodeWindow = new SimpleBooleanProperty();
        private final ObjectProperty<Stage> qrCodeWindow = new SimpleObjectProperty<>();
        @Setter
        private String bitcoinPaymentDescription;
        @Setter
        private String paymentProofDescription;
        @Setter
        private String paymentProofPrompt;
        @Setter
        private int smallQrCodeSize;
        @Setter
        private int largeQrCodeSize;
        @Setter
        private Image largeQrCodeImage;
        @Setter
        private Image smallQrCodeImage;
        @Setter
        private boolean useAnimations;
        @Setter
        private Region applicationRoot;

        protected Model(BisqEasyTrade bisqEasyTrade, BisqEasyOpenTradeChannel channel) {
            super(bisqEasyTrade, channel);
        }
    }

    public static class View extends BaseState.View<Model, Controller> {
        private static final Interpolator INTERPOLATOR = Interpolator.SPLINE(0.25, 0.1, 0.25, 1);

        private final BitcoinDataValidator txIdValidator;
        private final Button sentButton;
        private final MaterialTextField paymentProof;
        private final WrappingText sendBtcHeadline, fiatReceiptConfirmed;
        private final MaterialTextField baseAmount;
        private final MaterialTextField bitcoinPayment;
        private final Label qrCodeLabel;
        private final ImageView qrCodeImageView, openQrCodeWindowIcon;
        private final VBox qrCodeVBox;
        private Button closeQrCodeWindowButton;
        private Subscription qrCodeWindowPin, bitcoinPaymentDataHeightPin;

        private View(Model model, Controller controller) {
            super(model, controller);

            txIdValidator = new BitcoinDataValidator() {
                @Override
                protected String getData() {
                    return paymentProof.getText();
                }

                @Override
                protected BitcoinDataType getType() {
                    return BitcoinDataType.TX_ID;
                }
            };

            Pair<WrappingText, HBox> confirmPair = FormUtils.getConfirmInfo();
            fiatReceiptConfirmed = confirmPair.getFirst();
            HBox fiatReceiptConfirmedHBox = confirmPair.getSecond();

            sendBtcHeadline = FormUtils.getHeadline();

            baseAmount = FormUtils.getTextField(Res.get("bisqEasy.tradeState.info.seller.phase3a.baseAmount"), "", false);
            bitcoinPayment = FormUtils.getTextField("", "", false);
            paymentProof = FormUtils.getTextField("", "", true);

            VBox textFields = new VBox(10, baseAmount,
                    bitcoinPayment,
                    paymentProof);

            openQrCodeWindowIcon = new ImageView();
            openQrCodeWindowIcon.setId("detach");
            StackPane openQrCodeWindowIconPane = new StackPane(openQrCodeWindowIcon);
            openQrCodeWindowIconPane.setAlignment(Pos.TOP_RIGHT);

            BisqTooltip openWindowTooltip = new BisqTooltip(Res.get("bisqEasy.tradeState.info.seller.phase3a.qrCodeDisplay.openWindow"));
            BisqTooltip.install(openQrCodeWindowIcon, openWindowTooltip);

            qrCodeLabel = new Label();
            qrCodeLabel.setPadding(new Insets(3, 0, 5, 0));
            qrCodeLabel.getStyleClass().add("qr-code-display-text");

            qrCodeImageView = new ImageView();

            VBox.setMargin(openQrCodeWindowIconPane, new Insets(7.5, -2.5, 0, 0));
            VBox.setMargin(qrCodeLabel, new Insets(0, 0, 10, 0));
            VBox.setVgrow(openQrCodeWindowIconPane, Priority.ALWAYS);
            VBox.setVgrow(qrCodeLabel, Priority.ALWAYS);
            qrCodeVBox = new VBox(0, openQrCodeWindowIconPane, qrCodeImageView, qrCodeLabel);
            qrCodeVBox.setAlignment(Pos.CENTER);
            qrCodeVBox.setPadding(new Insets(0, 10, 0, 10));
            qrCodeVBox.getStyleClass().add("qr-code-display-bg");
            qrCodeVBox.setMinHeight(188);
            qrCodeVBox.setMaxHeight(qrCodeVBox.getMinHeight());

            HBox.setHgrow(textFields, Priority.ALWAYS);
            HBox controlsHBox = new HBox(20, textFields, qrCodeVBox);
            sentButton = new Button();
            sentButton.setDefaultButton(true);

            VBox.setMargin(fiatReceiptConfirmedHBox, new Insets(0, 0, 5, 0));
            VBox.setMargin(sentButton, new Insets(5, 0, 5, 0));
            root.getChildren().addAll(
                    fiatReceiptConfirmedHBox,
                    sendBtcHeadline,
                    controlsHBox,
                    sentButton);
        }

        @Override
        protected void onViewAttached() {
            super.onViewAttached();

            baseAmount.setText(model.getFormattedBaseAmount());
            bitcoinPayment.setDescription(model.getBitcoinPaymentDescription());
            qrCodeLabel.setText(model.getBitcoinPaymentDescription());
            bitcoinPayment.setText(model.getBitcoinPaymentData());
            paymentProof.setDescription(model.getPaymentProofDescription());
            paymentProof.setPromptText(model.getPaymentProofPrompt());
            sendBtcHeadline.setText(Res.get("bisqEasy.tradeState.info.seller.phase3a.sendBtc", model.getFormattedBaseAmount()));
            fiatReceiptConfirmed.setText(Res.get("bisqEasy.tradeState.info.seller.phase3a.fiatPaymentReceivedCheckBox", model.getFormattedQuoteAmount()));
            sentButton.setText(Res.get("bisqEasy.tradeState.info.seller.phase3a.btcSentButton", model.getFormattedBaseAmount()));
            qrCodeImageView.setImage(model.getSmallQrCodeImage());

            paymentProof.textProperty().bindBidirectional(model.getPaymentProof());
            paymentProof.setValidators(txIdValidator);
            sentButton.disableProperty().bind(model.getBtcSentButtonDisabled());
            openQrCodeWindowIcon.disableProperty().bind(model.getQrCodeWindow().isNotNull());

            qrCodeWindowPin = EasyBind.subscribe(model.getQrCodeWindow(), this::qrCodeWindowChanged);

            baseAmount.getIconButton().setOnAction(e -> ClipboardUtil.copyToClipboard(model.getBaseAmount()));
            qrCodeVBox.setOnMouseClicked(e -> controller.onShowQrCodeDisplay());
            sentButton.setOnAction(e -> controller.onBtcSent());
        }

        @Override
        protected void onViewDetached() {
            super.onViewDetached();

            paymentProof.textProperty().unbindBidirectional(model.getPaymentProof());
            sentButton.disableProperty().unbind();
            openQrCodeWindowIcon.disableProperty().unbind();

            qrCodeWindowPin.unsubscribe();
            if (bitcoinPaymentDataHeightPin != null) {
                bitcoinPaymentDataHeightPin.unsubscribe();
                bitcoinPaymentDataHeightPin = null;
            }

            sentButton.setOnAction(null);
            baseAmount.getIconButton().setOnAction(null);
            qrCodeVBox.setOnMouseClicked(null);
            if (closeQrCodeWindowButton != null) {
                closeQrCodeWindowButton.setOnAction(null);
            }
        }

        private void qrCodeWindowChanged(Stage qrCodeWindow) {
            if (qrCodeWindow != null) {
                int qrCodeSize = model.getLargeQrCodeSize();
                String shortTradeId = model.getBisqEasyTrade().getShortId();

                Label headline = new Label(Res.get("bisqEasy.tradeState.info.seller.phase3a.qrCodeDisplay.window.title", shortTradeId));
                headline.getStyleClass().add("qr-code-window-headline");
                ImageView qrCodeImageView = new ImageView(model.getLargeQrCodeImage());

                String data = model.getBitcoinPaymentDescription() + ": " + model.getBitcoinPaymentData();
                Label bitcoinPaymentData = new Label(data);
                bitcoinPaymentData.getStyleClass().add("qr-code-window-data");
                bitcoinPaymentData.setAlignment(Pos.CENTER);
                bitcoinPaymentData.setWrapText(true);

                closeQrCodeWindowButton = new Button(Res.get("action.close"));
                closeQrCodeWindowButton.setDefaultButton(true);
                closeQrCodeWindowButton.setOnAction(e -> controller.onCloseQrCodeWindow());

                VBox.setMargin(headline, new Insets(30, 0, 15, 0));
                VBox.setMargin(bitcoinPaymentData, new Insets(15, 0, 15, 0));
                VBox.setVgrow(bitcoinPaymentData, Priority.ALWAYS);
                VBox vBox = new VBox(10, headline, qrCodeImageView, bitcoinPaymentData, closeQrCodeWindowButton);
                vBox.setAlignment(Pos.TOP_CENTER);

                Layout.pinToAnchorPane(vBox, 0, 0, 0, 0);
                AnchorPane root = new AnchorPane(vBox);
                root.getStyleClass().add("bisq-popup");

                Scene scene = new Scene(root);
                scene.setFill(Paint.valueOf("#1c1c1c")); //  "bisq-popup" use -bisq-dark-grey-20: #1c1c1c;
                CssConfig.addAllCss(scene);
                qrCodeWindow.setScene(scene);
                controller.onSceneCreated(scene);

                qrCodeWindow.setTitle(Res.get("bisqEasy.tradeState.info.seller.phase3a.qrCodeDisplay.window.title", shortTradeId));
                qrCodeWindow.initModality(Modality.NONE);

                int heightAdjustment = 240;
                bitcoinPaymentDataHeightPin = EasyBind.subscribe(bitcoinPaymentData.heightProperty(), height -> {
                    if (height.doubleValue() > 0) {
                        double value = qrCodeSize + heightAdjustment + height.doubleValue();
                        qrCodeWindow.setHeight(value);
                        double width = value / 0.75;
                        bitcoinPaymentData.setMaxWidth(width - 40);
                        qrCodeWindow.setWidth(width);
                        layoutQrCodeWindow(qrCodeWindow);
                    }
                });

                qrCodeWindow.setOnCloseRequest(event -> {
                    event.consume();
                    controller.onCloseQrCodeWindow();
                });

                startShowAnimation(root, qrCodeWindow);
                UIScheduler.run(() -> {
                            qrCodeWindow.show();
                            layoutQrCodeWindow(qrCodeWindow);
                        })
                        .after(150);
            } else {
                if (closeQrCodeWindowButton != null) {
                    closeQrCodeWindowButton.setOnAction(null);
                    closeQrCodeWindowButton = null;
                }
                if (bitcoinPaymentDataHeightPin != null) {
                    bitcoinPaymentDataHeightPin.unsubscribe();
                    bitcoinPaymentDataHeightPin = null;
                }
            }
        }

        private void layoutQrCodeWindow(Stage stage) {
            Region owner = model.getApplicationRoot();
            Scene ownerScene = owner.getScene();
            Window window = ownerScene.getWindow();
            Bounds ownerBoundsInLocal = owner.getBoundsInLocal();
            Point2D ownerInLocalTopLeft = new Point2D(ownerBoundsInLocal.getMinX(), ownerBoundsInLocal.getMinY());
            Point2D ownerToScreenTopLeft = owner.localToScreen(ownerInLocalTopLeft);
            double titleBarHeight = ownerToScreenTopLeft.getY() - ownerScene.getWindow().getY();
            stage.setX(Math.round(window.getX() + (owner.getWidth() - stage.getWidth()) / 2));
            stage.setY(Math.round(window.getY() + titleBarHeight / 2 + (owner.getHeight() - stage.getHeight()) / 2));
        }

        private void startShowAnimation(Node node, Stage qrCodeWindow) {
            double duration = model.isUseAnimations() ? 400 : 0;
            Timeline timeline = new Timeline();
            ObservableList<KeyFrame> keyFrames = timeline.getKeyFrames();

            double startScale = 0.25;
            keyFrames.add(new KeyFrame(Duration.millis(0),
                    new KeyValue(qrCodeWindow.opacityProperty(), 0, INTERPOLATOR),
                    new KeyValue(node.scaleXProperty(), startScale, INTERPOLATOR),
                    new KeyValue(node.scaleYProperty(), startScale, INTERPOLATOR)

            ));
            keyFrames.add(new KeyFrame(Duration.millis(duration),
                    new KeyValue(qrCodeWindow.opacityProperty(), 1, INTERPOLATOR),
                    new KeyValue(node.scaleXProperty(), 1, INTERPOLATOR),
                    new KeyValue(node.scaleYProperty(), 1, INTERPOLATOR)
            ));

            timeline.play();
        }
    }
}
