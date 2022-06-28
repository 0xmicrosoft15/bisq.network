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

package bisq.desktop.primary.onboardingOld.onboardNewbie;

import bisq.common.currency.Market;
import bisq.desktop.common.threading.UIThread;
import bisq.desktop.common.utils.ImageUtil;
import bisq.desktop.common.utils.Layout;
import bisq.desktop.components.controls.AutoCompleteComboBox;
import bisq.i18n.Res;
import bisq.offer.spec.Direction;
import bisq.social.chat.ChatService;
import javafx.beans.property.*;
import javafx.beans.value.ChangeListener;
import javafx.collections.FXCollections;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Cursor;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.image.ImageView;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.layout.VBox;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import java.util.List;

@Slf4j
public class PaymentMethodsSelection {
    private final Controller controller;

    public PaymentMethodsSelection(ChatService chatService) {
        controller = new Controller(chatService);
    }

    public void setSelectedMarket(Market selectedMarket) {
        controller.setSelectedMarket(selectedMarket);
    }

    public Pane getRoot() {
        return controller.view.getRoot();
    }

    public void setDirection(Direction direction) {
        controller.setDirection(direction);
    }

    public ObservableList<String> getSelectedPaymentMethods() {
        return controller.model.selectedPaymentMethods;
    }

    public void setPrefWidth(double prefWidth) {
        controller.model.prefWidth.set(prefWidth);
    }

    private static class Controller implements bisq.desktop.common.view.Controller {
        private final Model model;
        @Getter
        private final View view;
        private final ChatService chatService;

        private Controller(ChatService chatService) {
            this.chatService = chatService;
            model = new Model();
            view = new View(model, this);
        }

        private void setSelectedMarket(Market selectedMarket) {
            if (selectedMarket == null) {
                return;
            }
            model.selectedMarket = selectedMarket;
            updatePaymentMethods();
            updateDescription();
        }

        private void setDirection(Direction direction) {
            model.direction = direction;
            updateDescription();
        }

        @Override
        public void onActivate() {
            updateDescription();
        }

        @Override
        public void onDeactivate() {
        }

        private void onAddPaymentMethod(String selectedPaymentMethod) {
            if (selectedPaymentMethod == null) {
                return;
            }
            if (model.selectedPaymentMethods.size() < 3 && !model.selectedPaymentMethods.contains(selectedPaymentMethod)) {
                model.selectedPaymentMethods.add(selectedPaymentMethod);
            }
            model.selectPaymentMethodsDisabled.set(model.selectedPaymentMethods.size() >= 3);
        }

        private void onRemovePaymentMethod(String paymentMethod) {
            model.selectedPaymentMethods.remove(paymentMethod);
            model.selectPaymentMethodsDisabled.set(model.selectedPaymentMethods.size() >= 3);
        }

        private void updateDescription() {
            if (model.selectedMarket == null) {
                return;
            }
            if (model.direction == Direction.SELL) {
                model.description.set(Res.get("satoshisquareapp.createOffer.paymentMethods.toReceive",
                        model.selectedMarket.getQuoteCurrencyCode()));
            } else {
                model.description.set(Res.get("satoshisquareapp.createOffer.paymentMethods.toSpend",
                        model.selectedMarket.getQuoteCurrencyCode()));
            }
        }

        private void updatePaymentMethods() {
            //todo
            model.paymentMethods.addAll(List.of("SEPA", "BANK-TRANSFER", "ZELLE", "REVOLUT"));
         /*   chatService.findPublicChannelForMarket(model.selectedMarket).ifPresent(publicChannel -> {
                model.paymentMethods.addAll(publicChannel.getPaymentMethodTags().stream()
                        .map(String::toUpperCase)
                        .collect(Collectors.toList()));
            });*/
        }
    }

    private static class Model implements bisq.desktop.common.view.Model {
        private final ObservableList<String> paymentMethods = FXCollections.observableArrayList();
        private final ObservableList<String> selectedPaymentMethods = FXCollections.observableArrayList();
        private final BooleanProperty selectPaymentMethodsDisabled = new SimpleBooleanProperty();
        private final StringProperty description = new SimpleStringProperty();
        private final DoubleProperty prefWidth = new SimpleDoubleProperty(Double.MAX_VALUE);
        private Market selectedMarket;
        private Direction direction;

        private Model() {
        }
    }

    @Slf4j
    public static class View extends bisq.desktop.common.view.View<VBox, Model, Controller> {
        private final AutoCompleteComboBox<String> comboBox;
        private final ListChangeListener<String> selectedPaymentMethodsListener;
        private final FlowPane selectedPaymentMethodsBox;
        private final ChangeListener<String> selectedItemListener;
        private final Label maxPaymentMethods;

        private View(Model model, Controller controller) {
            super(new VBox(), model, controller);

            maxPaymentMethods = new Label(Res.get("satoshisquareapp.createOffer.paymentMethods.max"));
            maxPaymentMethods.setPadding(new Insets(3, 0, 0, 0));
            maxPaymentMethods.getStyleClass().add("bisq-small-light-label-dimmed");
            maxPaymentMethods.setAlignment(Pos.CENTER_RIGHT);
            maxPaymentMethods.setPadding(new Insets(3, 5, 0, 0));

            comboBox = new AutoCompleteComboBox<>(model.paymentMethods, model.description.get());

            selectedPaymentMethodsBox = new FlowPane();
            selectedPaymentMethodsBox.setHgap(10);
            selectedPaymentMethodsBox.setVgap(10);
            selectedPaymentMethodsBox.setPadding(new Insets(10, 0, 0, 0));

            root.getChildren().addAll(comboBox, maxPaymentMethods, selectedPaymentMethodsBox);

            selectedPaymentMethodsListener = c -> {
                c.next();
                selectedPaymentMethodsBox.getChildren().clear();
                model.selectedPaymentMethods.forEach(paymentMethod -> {
                    selectedPaymentMethodsBox.getChildren().add(getPaymentMethodItem(paymentMethod));
                });
            };
            selectedItemListener = (observable, oldValue, newValue) -> {
                controller.onAddPaymentMethod(comboBox.getSelectionModel().getSelectedItem());
                UIThread.runOnNextRenderFrame(() -> comboBox.getSelectionModel().clearSelection());
            };
        }

        @Override
        protected void onViewAttached() {
            comboBox.disableProperty().bind(model.selectPaymentMethodsDisabled);
            comboBox.descriptionProperty().bind(model.description);
            maxPaymentMethods.prefWidthProperty().bind(comboBox.widthProperty());

            comboBox.getSelectionModel().selectedItemProperty().addListener(selectedItemListener);
            model.selectedPaymentMethods.addListener(selectedPaymentMethodsListener);

            comboBox.maxWidthProperty().bind(root.maxWidthProperty());
        }

        @Override
        protected void onViewDetached() {
            comboBox.disableProperty().unbind();
            comboBox.descriptionProperty().unbind();
            maxPaymentMethods.prefWidthProperty().unbind();

            comboBox.getSelectionModel().selectedItemProperty().removeListener(selectedItemListener);
            model.selectedPaymentMethods.removeListener(selectedPaymentMethodsListener);

            comboBox.maxWidthProperty().unbind();
        }

        private Node getPaymentMethodItem(String paymentMethod) {
            Label label = new Label(paymentMethod);
            label.getStyleClass().add("payment-methods-label");
            ImageView icon = ImageUtil.getImageViewById("light_close");
            icon.setCursor(Cursor.HAND);
            HBox.setMargin(icon, new Insets(3, 3, 0, 0));
            icon.setOnMousePressed(e -> controller.onRemovePaymentMethod(paymentMethod));
            HBox hBox = Layout.hBoxWith(label, icon);
            hBox.getStyleClass().add("payment-methods-icon-button-bg");
            return hBox;
        }
    }
}