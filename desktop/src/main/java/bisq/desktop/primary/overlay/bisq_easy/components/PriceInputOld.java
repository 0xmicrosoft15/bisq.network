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
import bisq.common.monetary.PriceQuote;
import bisq.common.observable.Pin;
import bisq.desktop.common.threading.UIThread;
import bisq.desktop.common.utils.validation.PriceValidator;
import bisq.desktop.components.controls.MaterialTextField;
import bisq.i18n.Res;
import bisq.oracle.marketprice.MarketPrice;
import bisq.oracle.marketprice.MarketPriceService;
import bisq.presentation.formatters.PriceFormatter;
import bisq.presentation.parser.PriceParser;
import javafx.beans.property.*;
import javafx.beans.value.ChangeListener;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.layout.Pane;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class PriceInputOld {
    private final Controller controller;

    public PriceInputOld(MarketPriceService marketPriceService) {
        controller = new Controller(marketPriceService);
    }

    public void setSelectedMarket(Market selectedMarket) {
        controller.setSelectedMarket(selectedMarket);
    }

    public ReadOnlyObjectProperty<PriceQuote> quoteProperty() {
        return controller.model.priceQuote;
    }

    public void setQuote(PriceQuote priceQuote) {
        controller.model.priceQuote.set(priceQuote);
    }

    public void setIsTakeOffer() {
        controller.model.isCreateOffer = false;
    }

    public void setDescription(String description) {
        controller.model.description.set(description);
    }

    public Pane getRoot() {
        return controller.view.getRoot();
    }

    public void reset() {
        controller.model.reset();
    }

    private static class Controller implements bisq.desktop.common.view.Controller {
        private final Model model;
        @Getter
        private final View view;
        private final PriceValidator validator = new PriceValidator();
        private final MarketPriceService marketPriceService;
        private Pin marketPriceUpdateFlagPin;

        private Controller(MarketPriceService marketPriceService) {
            this.marketPriceService = marketPriceService;
            model = new Model();
            view = new View(model, this, validator);
        }

        public void setSelectedMarket(Market selectedMarket) {
            model.selectedMarket = selectedMarket;
            updateFromMarketPrice();
        }

        private void updateFromMarketPrice() {
            if (model.selectedMarket != null) {
                model.marketString.set(model.selectedMarket.toString());
                model.description.set(Res.get("createOffer.price.fix.description.buy", model.selectedMarket.getBaseCurrencyCode()));
            }
            if (model.isCreateOffer) {
                model.priceQuote.set(null);
                setQuoteFromMarketPrice();
            }
        }

        @Override
        public void onActivate() {
            updateFromMarketPrice();

            marketPriceUpdateFlagPin = marketPriceService.getMarketPriceUpdateFlag().addObserver(__ -> {
                UIThread.run(() -> {
                    // We only set it initially
                    if (model.priceQuote.get() != null) return;
                    setQuoteFromMarketPrice();
                });
            });
        }

        @Override
        public void onDeactivate() {
            marketPriceUpdateFlagPin.unbind();
        }


        // View events
        private void onFocusChange(boolean hasFocus) {
            model.hasFocus = hasFocus;
        }

        private void onQuoteInput(String value) {
            if (value == null) return;
            if (model.hasFocus) return;
            if (value.isEmpty()) {
                model.priceQuote.set(null);
                return;
            }
            if (!validator.validate(value).isValid) {
                model.priceQuote.set(null);
                return;
            }
            if (model.selectedMarket == null) return;
            model.priceQuote.set(PriceParser.parse(value, model.selectedMarket));
        }

        private void setQuoteFromMarketPrice() {
            if (model.selectedMarket == null) return;
            MarketPrice marketPrice = marketPriceService.getMarketPriceByCurrencyMap().get(model.selectedMarket);
            if (marketPrice == null) return;
            model.priceQuote.set(marketPrice.getPriceQuote());
        }
    }

    private static class Model implements bisq.desktop.common.view.Model {
        private final ObjectProperty<PriceQuote> priceQuote = new SimpleObjectProperty<>();
        private Market selectedMarket;
        private boolean hasFocus;
        private final StringProperty marketString = new SimpleStringProperty();
        private final StringProperty description = new SimpleStringProperty();
        private boolean isCreateOffer = true;

        private Model() {
        }

        public void reset() {
            priceQuote.set(null);
            selectedMarket = null;
            hasFocus = false;
            marketString.set(null);
            description.set(null);
            isCreateOffer = true;
        }
    }

    public static class View extends bisq.desktop.common.view.View<Pane, Model, Controller> {
        private final static int WIDTH = 250;
        private final static int CODE_LABEL_WIDTH = 60;
        private final MaterialTextField textField;
        private final ChangeListener<String> textInputListener;
        private final ChangeListener<Boolean> focusListener;
        private final ChangeListener<PriceQuote> quoteListener;
        private final Label rightLabel;

        private View(Model model, Controller controller, PriceValidator validator) {
            super(new Pane(), model, controller);

            textField = new MaterialTextField(model.description.get(), Res.get("createOffer.price.fix.prompt"));
            textField.setPrefWidth(WIDTH);
            textField.setValidator(validator);

            rightLabel = new Label();
            rightLabel.setMinHeight(42);
            rightLabel.setAlignment(Pos.CENTER_RIGHT);
            rightLabel.setMinWidth(CODE_LABEL_WIDTH);
            rightLabel.setMaxWidth(CODE_LABEL_WIDTH);
            rightLabel.setLayoutX(WIDTH - CODE_LABEL_WIDTH - 13);
            rightLabel.setLayoutY(11);
            rightLabel.getStyleClass().add("bisq-amount-input-code-label");

            root.getChildren().addAll(textField, rightLabel);


            //  Listeners on view component events
            focusListener = (o, old, newValue) -> {
                controller.onFocusChange(newValue);
                controller.onQuoteInput(textField.getText());
            };
            textInputListener = (o, old, newValue) -> controller.onQuoteInput(textField.getText());

            // Listeners on model change
            quoteListener = (o, old, newValue) -> textField.setText(newValue == null ? "" : PriceFormatter.format(newValue));
        }

        @Override
        protected void onViewAttached() {
            if (model.isCreateOffer) {
                textField.textProperty().addListener(textInputListener);
                textField.focusedProperty().addListener(focusListener);
            } else {
                // editable/disable changes style. setMouseTransparent is just for prototyping now
                textField.setMouseTransparent(true);
            }
            rightLabel.textProperty().bind(model.marketString);

            textField.descriptionProperty().bind(model.description);
            model.priceQuote.addListener(quoteListener);
            textField.setText(model.priceQuote.get() == null ? "" : PriceFormatter.format(model.priceQuote.get()));
        }

        @Override
        protected void onViewDetached() {
            if (model.isCreateOffer) {
                textField.textProperty().removeListener(textInputListener);
                textField.focusedProperty().removeListener(focusListener);
            }
            rightLabel.textProperty().unbind();
            textField.descriptionProperty().unbind();
            model.priceQuote.removeListener(quoteListener);
        }
    }
}