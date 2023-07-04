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

package bisq.desktop.primary.overlay.bisq_easy.components;

import bisq.common.currency.Market;
import bisq.common.monetary.Monetary;
import bisq.desktop.common.utils.validation.MonetaryValidator;
import bisq.desktop.components.controls.BisqIconButton;
import bisq.desktop.components.controls.BisqTooltip;
import bisq.i18n.Res;
import bisq.presentation.formatters.AmountFormatter;
import bisq.presentation.parser.AmountParser;
import javafx.beans.property.*;
import javafx.beans.value.ChangeListener;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class SmallAmountInput {
    private final Controller controller;

    public SmallAmountInput(boolean isBaseCurrency) {
        controller = new Controller(isBaseCurrency);
    }

    public ReadOnlyObjectProperty<Monetary> amountProperty() {
        return controller.model.amount;
    }

    public void setSelectedMarket(Market selectedMarket) {
        controller.setSelectedMarket(selectedMarket);
    }

    public void setAmount(Monetary value) {
        controller.model.amount.set(value);
    }

    public void setTooltip(String tooltip) {
        controller.model.setTooltip(tooltip);
    }

    public void setShowEstimationPrefix(boolean showEstimationPrefix) {
        controller.model.setShowEstimationPrefix(showEstimationPrefix);
    }

    public void setUseLowPrecision(boolean useLowPrecision) {
        controller.model.setUseLowPrecision(useLowPrecision);
    }

    public Pane getRoot() {
        return controller.view.getRoot();
    }

    public ReadOnlyBooleanProperty focusedProperty() {
        return controller.view.textInput.focusedProperty();
    }

    public void reset() {
        controller.model.reset();
    }

    private static class Controller implements bisq.desktop.common.view.Controller {
        private final Model model;
        @Getter
        private final View view;
        private final MonetaryValidator validator = new MonetaryValidator();

        private Controller(boolean isBaseCurrency) {
            model = new Model(isBaseCurrency);
            view = new View(model, this, validator);
        }

        private void setSelectedMarket(Market selectedMarket) {
            model.selectedMarket = selectedMarket;
            model.amount.set(null);
            updateModel();
        }

        @Override
        public void onActivate() {
            model.amount.set(null);
            updateModel();
        }

        @Override
        public void onDeactivate() {
        }

        // View events
        private void onFocusChange(boolean hasFocus) {
            model.hasFocus = hasFocus;
        }

        private void onAmount(String value) {
            if (value == null) return;
            if (model.hasFocus) return;
            if (value.isEmpty()) {
                model.amount.set(null);
                return;
            }
            if (!validator.validate(value).isValid) {
                model.amount.set(null);
                return;
            }
            if (model.code.get() == null) return;
            model.amount.set(AmountParser.parse(value, model.code.get()));
        }

        private void updateModel() {
            if (model.selectedMarket == null) {
                model.code.set("");
                return;
            }

            model.code.set(model.isBaseCurrency ? model.selectedMarket.getBaseCurrencyCode() : model.selectedMarket.getQuoteCurrencyCode());
        }
    }

    private static class Model implements bisq.desktop.common.view.Model {
        @Setter
        private boolean showEstimationPrefix;
        @Setter
        private boolean useLowPrecision;
        @Setter
        private String tooltip = Res.get("bisqEasy.component.amount.baseSide.buyer.tooltip");
        private final boolean isBaseCurrency;
        private final ObjectProperty<Monetary> amount = new SimpleObjectProperty<>();
        private final StringProperty code = new SimpleStringProperty();
        private Market selectedMarket;
        public boolean hasFocus;

        private Model(boolean isBaseCurrency) {
            this.isBaseCurrency = isBaseCurrency;
        }

        void reset() {
            amount.set(null);
            code.set(null);
            selectedMarket = null;
            hasFocus = false;
        }
    }

    public static class View extends bisq.desktop.common.view.View<HBox, Model, Controller> {
        private static final String ESTIMATION_PREFIX = "~ ";
        private final ChangeListener<String> textInputListener;
        private final ChangeListener<Boolean> focusListener;
        private final ChangeListener<Monetary> amountListener;
        private final TextField textInput;
        private final Label codeLabel;
        private final BisqTooltip tooltip;

        private View(Model model, Controller controller, MonetaryValidator validator) {
            super(new HBox(), model, controller);

            root.setAlignment(Pos.CENTER);
            root.setSpacing(3);

            // textInput would be black without setting a style on root. Not clear why...
            root.setStyle("-fx-fill: -fx-light-text-color;");

            textInput = new TextField();
            textInput.setPrefWidth(100);
            textInput.setId("quote-amount-text-field");
            textInput.setAlignment(Pos.CENTER_RIGHT);
            textInput.setPadding(new Insets(0, 0, 0, 0));

            codeLabel = new Label();
            codeLabel.setAlignment(Pos.CENTER_LEFT);
            codeLabel.setId("quote-amount-text-field");
            codeLabel.setPadding(new Insets(0, 0, 0, 0));

            Button iconButton = BisqIconButton.createIconButton("info");
            iconButton.setScaleX(0.8);
            iconButton.setScaleY(0.8);
            iconButton.setOpacity(0.5);
            tooltip = new BisqTooltip();
            tooltip.getStyleClass().add("dark-tooltip");
            iconButton.setTooltip(tooltip);

            HBox.setMargin(textInput, new Insets(0, 0, 0, -35));
            HBox.setMargin(iconButton, new Insets(-8, 0, 0, 1));
            root.getChildren().addAll(textInput, codeLabel, iconButton);

            //  Listeners on view component events
            focusListener = (o, oldValue, newValue) -> {
                controller.onFocusChange(newValue);
                if (oldValue) {
                    controller.onAmount(getText());
                }
            };
            textInputListener = (o, old, newValue) -> {
                if (textInput.isFocused()) {
                    controller.onAmount(getText());
                }
            };

            // Listeners on model change
            amountListener = (o, old, newValue) -> applyAmount(newValue);
        }

        @Override
        protected void onViewAttached() {
            tooltip.setText(model.tooltip);
            textInput.textProperty().addListener(textInputListener);
            textInput.focusedProperty().addListener(focusListener);
            codeLabel.textProperty().bind(model.code);
            model.amount.addListener(amountListener);
            applyAmount(model.amount.get());
        }

        @Override
        protected void onViewDetached() {
            textInput.textProperty().removeListener(textInputListener);
            textInput.focusedProperty().removeListener(focusListener);

            codeLabel.textProperty().unbind();
            model.amount.removeListener(amountListener);
        }

        private void applyAmount(Monetary newValue) {
            String prefix = model.showEstimationPrefix ? ESTIMATION_PREFIX : "";
            textInput.setText(newValue == null ? "" :
                    prefix + AmountFormatter.formatAmount(newValue, model.useLowPrecision));
        }

        private String getText() {
            return textInput.getText().replace(ESTIMATION_PREFIX, "");
        }
    }
}