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

package bisq.desktop.primary.main.content.social.onboarding.onboardNewbie;

import bisq.common.monetary.Market;
import bisq.desktop.common.utils.ImageUtil;
import bisq.desktop.components.controls.BisqComboBox;
import bisq.desktop.components.controls.BisqLabel;
import bisq.desktop.layout.Layout;
import bisq.i18n.Res;
import bisq.offer.spec.Direction;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.beans.value.ChangeListener;
import javafx.collections.FXCollections;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Cursor;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.layout.VBox;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class PaymentMethodsSelection {
    private final Controller controller;

    public PaymentMethodsSelection() {
        controller = new Controller();
    }

    void setSelectedMarket(Market selectedMarket) {
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

    private static class Controller implements bisq.desktop.common.view.Controller {
        private final Model model;
        @Getter
        private final View view;

        private Controller() {
            model = new Model();
            view = new View(model, this);
        }

        private void setSelectedMarket(Market selectedMarket) {
            model.selectedMarket = selectedMarket;
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
                        model.selectedMarket.quoteCurrencyCode()));
            } else {
                model.description.set(Res.get("satoshisquareapp.createOffer.paymentMethods.toSpend",
                        model.selectedMarket.quoteCurrencyCode()));
            }
        }
    }

    private static class Model implements bisq.desktop.common.view.Model {
        private final ObservableList<String> paymentMethods = FXCollections.observableArrayList();
        private final ObservableList<String> selectedPaymentMethods = FXCollections.observableArrayList();
        private final BooleanProperty selectPaymentMethodsDisabled = new SimpleBooleanProperty();
        private final StringProperty description = new SimpleStringProperty();
        public Market selectedMarket;
        public Direction direction;

        public Model() {
            //todo provide from domain
            paymentMethods.add("SEPA");
            paymentMethods.add("Bank transfer");
            paymentMethods.add("Revolut");
            paymentMethods.add("Zelle");
            paymentMethods.add("Other");
        }
    }

    @Slf4j
    public static class View extends bisq.desktop.common.view.View<VBox, Model, Controller> {
        private final BisqComboBox<String> comboBox;
        private final ChangeListener<String> selectedMarketListener;
        private final ListChangeListener<String> selectedPaymentMethodsListener;
        private final HBox box;
        private final BisqLabel description;
        private final ChangeListener<String> selectedItemListener;
        private final BisqLabel maxPaymentMethods;

        private View(Model model, Controller controller) {
            super(new VBox(), model, controller);

            Label headline = new BisqLabel(Res.get("satoshisquareapp.createOffer.paymentMethods.headline"));
            headline.setPadding(new Insets(0, 0, 10, 0));
            headline.getStyleClass().add("titled-group-bg-label-active");
            description = new BisqLabel();
            description.setPadding(new Insets(0, 0, 2, 0));
            description.setId("small-info-label");
            maxPaymentMethods = new BisqLabel(Res.get("satoshisquareapp.createOffer.paymentMethods.max"));
            maxPaymentMethods.setPadding(new Insets(3, 0, 0, 0));
            maxPaymentMethods.setId("small-info-label");
            maxPaymentMethods.setAlignment(Pos.CENTER_RIGHT);
            maxPaymentMethods.setPadding(new Insets(3, 5, 0, 0));

            comboBox = new BisqComboBox<>();
            comboBox.setItems(model.paymentMethods);
            comboBox.setPrefWidth(310);

            box = new HBox();
            box.setSpacing(10);
            box.setPadding(new Insets(10, 0, 0, 0));
            root.getChildren().addAll(headline, description, comboBox, maxPaymentMethods, box);

            // From model
            selectedMarketListener = (o, old, newValue) -> comboBox.getSelectionModel().select(newValue);
            selectedPaymentMethodsListener = c -> {
                c.next();
                box.getChildren().clear();
                model.selectedPaymentMethods.forEach(paymentMethod -> {
                    box.getChildren().add(getPaymentMethodItem(paymentMethod));
                });
            };
            selectedItemListener = (observable, oldValue, newValue) -> controller.onAddPaymentMethod(comboBox.getSelectionModel().getSelectedItem());
        }

        private Node getPaymentMethodItem(String paymentMethod) {
            ImageView icon = ImageUtil.getImageViewById("light_close");
            Label label = new Label(paymentMethod);
            label.setStyle("-fx-text-fill: #222");
            label.setPadding(new Insets(3, 5, 3, 10));
            Button button = new Button();
            button.setCursor(Cursor.HAND);
            button.setStyle("-fx-background-color: transparent");
            button.setPadding(new Insets(2, 4, 0, 0));
            button.setGraphic(icon);
            button.setOnAction(e -> controller.onRemovePaymentMethod(paymentMethod));
            HBox hBox = Layout.hBoxWith(label, button);
            hBox.setStyle("-fx-background-color: #eee; -fx-background-radius: 7");
            return hBox;
        }

        @Override
        protected void onViewAttached() {
            comboBox.disableProperty().bind(model.selectPaymentMethodsDisabled);
            description.textProperty().bind(model.description);
            comboBox.getSelectionModel().selectedItemProperty().addListener(selectedItemListener);
            model.selectedPaymentMethods.addListener(selectedPaymentMethodsListener);
            maxPaymentMethods.prefWidthProperty().bind(comboBox.widthProperty());
        }

        @Override
        protected void onViewDetached() {
            comboBox.disableProperty().unbind();
            description.textProperty().unbind();
            comboBox.getSelectionModel().selectedItemProperty().removeListener(selectedItemListener);
            model.selectedPaymentMethods.removeListener(selectedPaymentMethodsListener);
            maxPaymentMethods.prefWidthProperty().unbind();
        }
    }
}