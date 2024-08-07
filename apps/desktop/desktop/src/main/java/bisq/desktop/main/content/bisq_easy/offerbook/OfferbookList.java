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

package bisq.desktop.main.content.bisq_easy.offerbook;

import bisq.account.payment_method.BitcoinPaymentMethod;
import bisq.account.payment_method.FiatPaymentMethod;
import bisq.account.payment_method.PaymentMethod;
import bisq.common.observable.Pin;
import bisq.desktop.ServiceProvider;
import bisq.desktop.common.Layout;
import bisq.desktop.common.Transitions;
import bisq.desktop.common.observable.FxBindings;
import bisq.desktop.common.utils.ImageUtil;
import bisq.desktop.components.controls.BisqTooltip;
import bisq.desktop.components.controls.DropdownMenu;
import bisq.desktop.components.controls.DropdownMenuItem;
import bisq.desktop.components.table.BisqTableColumn;
import bisq.desktop.components.table.BisqTableView;
import bisq.desktop.main.content.bisq_easy.BisqEasyViewUtils;
import bisq.desktop.main.content.chat.BaseChatView;
import bisq.desktop.main.content.chat.message_container.ChatMessageContainerController;
import bisq.desktop.main.content.components.ReputationScoreDisplay;
import bisq.desktop.main.content.components.UserProfileIcon;
import bisq.i18n.Res;
import bisq.settings.SettingsService;
import com.google.common.base.Joiner;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.collections.transformation.FilteredList;
import javafx.collections.transformation.SortedList;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Cursor;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.Tooltip;
import javafx.scene.effect.ColorAdjust;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.util.Callback;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.fxmisc.easybind.EasyBind;
import org.fxmisc.easybind.Subscription;

import java.util.Comparator;

/**
 * Subview for offer list. It shares some model data from the BisqEasyOfferbookModel as
 * those data will effects on the containing view.
 */
@Slf4j
public final class OfferbookList {
    private final OfferbookList.Controller controller;

    public OfferbookList(ServiceProvider serviceProvider,
                         ChatMessageContainerController chatMessageContainerController,
                         FilteredList<OfferMessageItem> filteredOfferMessageItems,
                         SortedList<OfferMessageItem> sortedOfferMessageItems,
                         BooleanProperty showOfferListExpanded) {
        controller = new OfferbookList.Controller(serviceProvider,
                chatMessageContainerController,
                filteredOfferMessageItems,
                sortedOfferMessageItems,
                showOfferListExpanded);
    }

    public Pane getRoot() {
        return controller.view.getRoot();
    }

    private static class Controller implements bisq.desktop.common.view.Controller {
        private final ChatMessageContainerController chatMessageContainerController;
        private final OfferbookList.Model model;
        @Getter
        private final OfferbookList.View view;
        private final SettingsService settingsService;
        private Pin showBuyOffersPin;
        private Subscription showBuyOffersFromModelPin;

        private Controller(ServiceProvider serviceProvider,
                           ChatMessageContainerController chatMessageContainerController,
                           FilteredList<OfferMessageItem> filteredOfferMessageItems,
                           SortedList<OfferMessageItem> sortedOfferMessageItems,
                           BooleanProperty showOfferListExpanded) {
            this.chatMessageContainerController = chatMessageContainerController;
            settingsService = serviceProvider.getSettingsService();
            model = new OfferbookList.Model(filteredOfferMessageItems, sortedOfferMessageItems, showOfferListExpanded);
            view = new OfferbookList.View(model, this);
        }

        @Override
        public void onActivate() {
            showBuyOffersPin = FxBindings.bindBiDir(model.getShowBuyOffers()).to(settingsService.getShowBuyOffers());
            showBuyOffersFromModelPin = EasyBind.subscribe(model.getShowBuyOffers(), showBuyOffers -> {
                model.getFilteredOfferMessageItems().setPredicate(item ->
                        showBuyOffers == item.isBuyOffer()
                );
            });
        }

        @Override
        public void onDeactivate() {
            showBuyOffersPin.unbind();
            showBuyOffersFromModelPin.unsubscribe();
        }

        void toggleOfferList() {
            model.getShowOfferListExpanded().set(!model.getShowOfferListExpanded().get());
        }

        void onSelectOfferMessageItem(OfferMessageItem item) {
            chatMessageContainerController.highlightOfferChatMessage(item == null ? null : item.getBisqEasyOfferbookMessage());
        }

        void onSelectBuyFromFilter() {
            model.getShowBuyOffers().set(false);
        }

        void onSelectSellToFilter() {
            model.getShowBuyOffers().set(true);
        }
    }

    @Getter
    private static class Model implements bisq.desktop.common.view.Model {
        private final FilteredList<OfferMessageItem> filteredOfferMessageItems;
        private final SortedList<OfferMessageItem> sortedOfferMessageItems;
        private final StringProperty fiatAmountTitle = new SimpleStringProperty();
        private final BooleanProperty showOfferListExpanded;
        private final BooleanProperty showBuyOffers = new SimpleBooleanProperty();

        private Model(FilteredList<OfferMessageItem> filteredOfferMessageItems,
                      SortedList<OfferMessageItem> sortedOfferMessageItems,
                      BooleanProperty showOfferListExpanded) {
            this.filteredOfferMessageItems = filteredOfferMessageItems;
            this.sortedOfferMessageItems = sortedOfferMessageItems;
            this.showOfferListExpanded = showOfferListExpanded;
        }
    }

    @Slf4j
    public static class View extends bisq.desktop.common.view.View<VBox, OfferbookList.Model, OfferbookList.Controller> {
        private static final double EXPANDED_OFFER_LIST_WIDTH = 543;
        private static final double COLLAPSED_LIST_WIDTH = BisqEasyOfferbookView.COLLAPSED_LIST_WIDTH;
        private static final double HEADER_HEIGHT = BaseChatView.HEADER_HEIGHT;
        private static final double LIST_CELL_HEIGHT = BisqEasyOfferbookView.LIST_CELL_HEIGHT;

        private final Label title, offerListByDirectionFilter;
        private final BisqTableView<OfferMessageItem> tableView;
        private final BisqTooltip titleTooltip;
        private final HBox header;
        private final ImageView offerListWhiteIcon, offerListGreyIcon, offerListGreenIcon;
        private final DropdownMenu filterDropdownMenu;
        private final DropdownMenuItem buyFromOffers, sellToOffers;
        private Subscription showOfferListExpandedPin, showBuyFromOffersPin, offerListTableViewSelectionPin;

        private View(OfferbookList.Model model, OfferbookList.Controller controller) {
            super(new VBox(), model, controller);

            root.setFillWidth(true);

            offerListGreenIcon = ImageUtil.getImageViewById("list-view-green");
            offerListGreyIcon = ImageUtil.getImageViewById("list-view-grey");
            offerListWhiteIcon = ImageUtil.getImageViewById("list-view-white");

            title = new Label("", offerListGreenIcon);
            title.setCursor(Cursor.HAND);

            titleTooltip = new BisqTooltip();

            header = new HBox(title);
            header.setMinHeight(HEADER_HEIGHT);
            header.setMaxHeight(HEADER_HEIGHT);
            header.getStyleClass().add("chat-header-title");

            filterDropdownMenu = new DropdownMenu("chevron-drop-menu-grey", "chevron-drop-menu-white", false);
            filterDropdownMenu.getStyleClass().add("dropdown-offer-list-direction-filter-menu");
            offerListByDirectionFilter = new Label();
            filterDropdownMenu.setLabel(offerListByDirectionFilter);
            buyFromOffers = new DropdownMenuItem(Res.get("bisqEasy.offerbook.offerList.table.filters.offerDirection.buyFrom"));
            sellToOffers = new DropdownMenuItem(Res.get("bisqEasy.offerbook.offerList.table.filters.offerDirection.sellTo"));
            filterDropdownMenu.addMenuItems(buyFromOffers, sellToOffers);

            HBox subheader = new HBox();
            subheader.setAlignment(Pos.CENTER_LEFT);
            subheader.getStyleClass().add("offer-list-subheader");
            subheader.getChildren().add(filterDropdownMenu);

            tableView = new BisqTableView<>(model.getSortedOfferMessageItems());
            tableView.getStyleClass().add("offers-list");
            tableView.allowVerticalScrollbar();
            tableView.hideHorizontalScrollbar();
            tableView.setFixedCellSize(LIST_CELL_HEIGHT);
            tableView.setPlaceholder(new Label());
            configOffersTableView();
            VBox.setVgrow(tableView, Priority.ALWAYS);

            root.getChildren().addAll(header, Layout.hLine(), subheader, tableView);
        }

        @Override
        protected void onViewAttached() {
            showOfferListExpandedPin = EasyBind.subscribe(model.getShowOfferListExpanded(), showOfferListExpanded -> {
                if (showOfferListExpanded != null) {
                    tableView.setVisible(showOfferListExpanded);
                    tableView.setManaged(showOfferListExpanded);
                    filterDropdownMenu.setVisible(showOfferListExpanded);
                    filterDropdownMenu.setManaged(showOfferListExpanded);
                    title.setGraphic(offerListGreyIcon);
                    if (showOfferListExpanded) {
                        header.setAlignment(Pos.CENTER_LEFT);
                        header.setPadding(new Insets(4, 0, 0, 15));
                        root.setMaxWidth(EXPANDED_OFFER_LIST_WIDTH);
                        root.setPrefWidth(EXPANDED_OFFER_LIST_WIDTH);
                        root.setMinWidth(EXPANDED_OFFER_LIST_WIDTH);
                        HBox.setMargin(root, new Insets(0, 0, 0, 0));
                        root.getStyleClass().remove("collapsed-offer-list-container");
                        root.getStyleClass().add("chat-container");
                        title.setText(Res.get("bisqEasy.offerbook.offerList"));
                        titleTooltip.setText(Res.get("bisqEasy.offerbook.offerList.expandedList.tooltip"));
                        Transitions.expansionAnimation(root, COLLAPSED_LIST_WIDTH + 20, EXPANDED_OFFER_LIST_WIDTH);
                        title.setOnMouseExited(e -> title.setGraphic(offerListGreenIcon));
                    } else {
                        Transitions.expansionAnimation(root, EXPANDED_OFFER_LIST_WIDTH, COLLAPSED_LIST_WIDTH + 20, () -> {
                            header.setAlignment(Pos.CENTER);
                            header.setPadding(new Insets(4, 0, 0, 0));
                            root.setMaxWidth(COLLAPSED_LIST_WIDTH);
                            root.setPrefWidth(COLLAPSED_LIST_WIDTH);
                            root.setMinWidth(COLLAPSED_LIST_WIDTH);
                            HBox.setMargin(root, new Insets(0, 0, 0, -9));
                            root.getStyleClass().remove("chat-container");
                            root.getStyleClass().add("collapsed-offer-list-container");
                            title.setText("");
                            titleTooltip.setText(Res.get("bisqEasy.offerbook.offerList.collapsedList.tooltip"));
                            title.setGraphic(offerListGreyIcon);
                            title.setOnMouseExited(e -> title.setGraphic(offerListGreyIcon));
                        });
                    }
                }
            });

            offerListTableViewSelectionPin = EasyBind.subscribe(tableView.getSelectionModel().selectedItemProperty(),
                    controller::onSelectOfferMessageItem);

            showBuyFromOffersPin = EasyBind.subscribe(model.getShowBuyOffers(), showBuyFromOffers -> {
                if (showBuyFromOffers != null) {
                    offerListByDirectionFilter.getStyleClass().clear();
                    if (showBuyFromOffers) {
                        offerListByDirectionFilter.setText(sellToOffers.getLabelText());
                        offerListByDirectionFilter.getStyleClass().add("sell-to-offers");
                    } else {
                        offerListByDirectionFilter.setText(buyFromOffers.getLabelText());
                        offerListByDirectionFilter.getStyleClass().add("buy-from-offers");
                    }
                }
            });

            title.setOnMouseEntered(e -> title.setGraphic(offerListWhiteIcon));
            title.setOnMouseClicked(e -> controller.toggleOfferList());
            buyFromOffers.setOnAction(e -> controller.onSelectBuyFromFilter());
            sellToOffers.setOnAction(e -> controller.onSelectSellToFilter());

            title.setTooltip(titleTooltip);
        }

        @Override
        protected void onViewDetached() {
            showOfferListExpandedPin.unsubscribe();
            offerListTableViewSelectionPin.unsubscribe();
            showBuyFromOffersPin.unsubscribe();

            title.setOnMouseEntered(null);
            title.setOnMouseExited(null);
            title.setOnMouseClicked(null);
            buyFromOffers.setOnAction(null);
            sellToOffers.setOnAction(null);

            title.setTooltip(null);
        }

        private void configOffersTableView() {
            tableView.getColumns().add(tableView.getSelectionMarkerColumn());

            BisqTableColumn<OfferMessageItem> userProfileColumn = new BisqTableColumn.Builder<OfferMessageItem>()
                    .title(Res.get("bisqEasy.offerbook.offerList.table.columns.peerProfile"))
                    .left()
                    .fixWidth(150)
                    .setCellFactory(getUserProfileCellFactory())
                    .comparator(Comparator
                            .comparingLong(OfferMessageItem::getTotalScore).reversed()
                            .thenComparing(OfferMessageItem::getUserNickname))
                    .build();
            tableView.getColumns().add(userProfileColumn);
            tableView.getSortOrder().add(userProfileColumn);

            tableView.getColumns().add(new BisqTableColumn.Builder<OfferMessageItem>()
                    .title(Res.get("bisqEasy.offerbook.offerList.table.columns.price"))
                    .right()
                    .fixWidth(70)
                    .setCellFactory(getPriceCellFactory())
                    .comparator(Comparator.comparing(OfferMessageItem::getPriceSpecAsPercent))
                    .build());

            tableView.getColumns().add(new BisqTableColumn.Builder<OfferMessageItem>()
                    .titleProperty(model.getFiatAmountTitle())
                    .right()
                    .fixWidth(120)
                    .setCellFactory(getFiatAmountCellFactory())
                    .comparator(Comparator.comparing(OfferMessageItem::getMinAmount))
                    .build());

            tableView.getColumns().add(new BisqTableColumn.Builder<OfferMessageItem>()
                    .title(Res.get("bisqEasy.offerbook.offerList.table.columns.paymentMethod"))
                    .right()
                    .fixWidth(105)
                    .setCellFactory(getPaymentCellFactory())
                    .comparator(Comparator.comparing(OfferMessageItem::getFiatPaymentMethodsAsString))
                    .build());

            tableView.getColumns().add(new BisqTableColumn.Builder<OfferMessageItem>()
                    .title(Res.get("bisqEasy.offerbook.offerList.table.columns.settlementMethod"))
                    .left()
                    .fixWidth(95)
                    .setCellFactory(getSettlementCellFactory())
                    .comparator(Comparator.comparing(OfferMessageItem::getBitcoinPaymentMethodsAsString))
                    .build());
        }


        private Callback<TableColumn<OfferMessageItem, OfferMessageItem>,
                TableCell<OfferMessageItem, OfferMessageItem>> getUserProfileCellFactory() {
            return column -> new TableCell<>() {
                private final Label userNameLabel = new Label();
                private final ReputationScoreDisplay reputationScoreDisplay = new ReputationScoreDisplay();
                private final VBox nameAndReputationBox = new VBox(userNameLabel, reputationScoreDisplay);
                private final UserProfileIcon userProfileIcon = new UserProfileIcon();
                private final HBox userProfileBox = new HBox(10, userProfileIcon, nameAndReputationBox);

                {
                    userNameLabel.setId("chat-user-name");
                    HBox.setMargin(userProfileIcon, new Insets(0, 0, 0, -1));
                    nameAndReputationBox.setAlignment(Pos.CENTER_LEFT);
                    userProfileBox.setAlignment(Pos.CENTER_LEFT);
                }

                @Override
                protected void updateItem(OfferMessageItem item, boolean empty) {
                    super.updateItem(item, empty);

                    if (item != null && !empty) {
                        userNameLabel.setText(item.getUserNickname());
                        reputationScoreDisplay.setReputationScore(item.getReputationScore().get());
                        userProfileIcon.setUserProfile(item.getUserProfile());
                        setGraphic(userProfileBox);
                    } else {
                        userNameLabel.setText("");
                        reputationScoreDisplay.setReputationScore(null);
                        userProfileIcon.dispose();
                        setGraphic(null);
                    }
                }
            };
        }

        private Callback<TableColumn<OfferMessageItem, OfferMessageItem>,
                TableCell<OfferMessageItem, OfferMessageItem>> getPriceCellFactory() {
            return column -> new TableCell<>() {
                private final Label percentagePriceLabel = new Label();
                private final BisqTooltip tooltip = new BisqTooltip();

                @Override
                protected void updateItem(OfferMessageItem item, boolean empty) {
                    super.updateItem(item, empty);

                    if (item != null && !empty) {
                        percentagePriceLabel.setText(item.getFormattedPercentagePrice());
                        percentagePriceLabel.setOpacity(item.isFixPrice() ? 0.5 : 1);
                        tooltip.setText(item.getPriceTooltipText());
                        percentagePriceLabel.setTooltip(tooltip);
                        setGraphic(percentagePriceLabel);
                    } else {
                        percentagePriceLabel.setText("");
                        percentagePriceLabel.setTooltip(null);
                        setGraphic(null);
                    }
                }
            };
        }

        private Callback<TableColumn<OfferMessageItem, OfferMessageItem>,
                TableCell<OfferMessageItem, OfferMessageItem>> getFiatAmountCellFactory() {
            return column -> new TableCell<>() {
                private final Label fiatAmountLabel = new Label();
                private final BisqTooltip tooltip = new BisqTooltip();

                @Override
                protected void updateItem(OfferMessageItem item, boolean empty) {
                    super.updateItem(item, empty);

                    if (item != null && !empty) {
                        fiatAmountLabel.setText(item.getFormattedRangeQuoteAmount());
                        tooltip.setText(item.getFormattedRangeQuoteAmount());
                        fiatAmountLabel.setTooltip(tooltip);
                        setGraphic(fiatAmountLabel);
                    } else {
                        fiatAmountLabel.setText("");
                        fiatAmountLabel.setTooltip(null);
                        setGraphic(null);
                    }
                }
            };
        }

        private Callback<TableColumn<OfferMessageItem, OfferMessageItem>,
                TableCell<OfferMessageItem, OfferMessageItem>> getPaymentCellFactory() {
            return column -> new TableCell<>() {
                private final HBox hbox = new HBox(5);
                private final BisqTooltip tooltip = new BisqTooltip();

                {
                    hbox.setAlignment(Pos.CENTER_RIGHT);
                }

                @Override
                protected void updateItem(OfferMessageItem item, boolean empty) {
                    super.updateItem(item, empty);

                    if (item != null && !empty) {

                        for (FiatPaymentMethod fiatPaymentMethod : item.getFiatPaymentMethods()) {
                            Node icon = !fiatPaymentMethod.isCustomPaymentMethod()
                                    ? ImageUtil.getImageViewById(fiatPaymentMethod.getName())
                                    : BisqEasyViewUtils.getCustomPaymentMethodIcon(fiatPaymentMethod.getDisplayString());
                            hbox.getChildren().add(icon);
                        }
                        tooltip.setText(Joiner.on("\n").join(item.getFiatPaymentMethods().stream()
                                .map(PaymentMethod::getDisplayString)
                                .toList()));
                        Tooltip.install(hbox, tooltip);
                        setGraphic(hbox);
                    } else {
                        Tooltip.uninstall(hbox, tooltip);
                        hbox.getChildren().clear();
                        setGraphic(null);
                    }
                }
            };
        }

        private Callback<TableColumn<OfferMessageItem, OfferMessageItem>,
                TableCell<OfferMessageItem, OfferMessageItem>> getSettlementCellFactory() {
            return column -> new TableCell<>() {
                private final HBox hbox = new HBox(5);
                private final BisqTooltip tooltip = new BisqTooltip();

                {
                    hbox.setAlignment(Pos.CENTER_LEFT);
                }

                @Override
                protected void updateItem(OfferMessageItem item, boolean empty) {
                    super.updateItem(item, empty);

                    if (item != null && !empty) {
                        for (BitcoinPaymentMethod bitcoinPaymentMethod : item.getBitcoinPaymentMethods()) {
                            ImageView icon = ImageUtil.getImageViewById(bitcoinPaymentMethod.getName());
                            ColorAdjust colorAdjust = new ColorAdjust();
                            colorAdjust.setBrightness(-0.2);
                            icon.setEffect(colorAdjust);
                            hbox.getChildren().add(icon);
                        }
                        tooltip.setText(Joiner.on("\n").join(item.getBitcoinPaymentMethods().stream()
                                .map(PaymentMethod::getDisplayString)
                                .toList()));
                        Tooltip.install(hbox, tooltip);
                        setGraphic(hbox);
                    } else {
                        Tooltip.uninstall(hbox, tooltip);
                        hbox.getChildren().clear();
                        setGraphic(null);
                    }
                }
            };
        }
    }
}
