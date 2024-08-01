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

import bisq.bisq_easy.BisqEasyMarketFilter;
import bisq.bisq_easy.NavigationTarget;
import bisq.bonded_roles.market_price.MarketPriceService;
import bisq.chat.ChatChannel;
import bisq.chat.ChatChannelDomain;
import bisq.chat.ChatMessage;
import bisq.chat.bisqeasy.offerbook.BisqEasyOfferbookChannel;
import bisq.chat.bisqeasy.offerbook.BisqEasyOfferbookChannelService;
import bisq.chat.bisqeasy.offerbook.BisqEasyOfferbookMessage;
import bisq.chat.bisqeasy.open_trades.BisqEasyOpenTradeChannel;
import bisq.chat.notifications.ChatNotificationService;
import bisq.common.currency.Market;
import bisq.common.observable.Pin;
import bisq.common.observable.collection.CollectionObserver;
import bisq.common.observable.collection.ObservableArray;
import bisq.common.util.ProtobufUtils;
import bisq.desktop.ServiceProvider;
import bisq.desktop.common.observable.FxBindings;
import bisq.desktop.common.threading.UIThread;
import bisq.desktop.common.view.Navigation;
import bisq.desktop.main.content.bisq_easy.trade_wizard.TradeWizardController;
import bisq.desktop.main.content.chat.ChatController;
import bisq.desktop.main.content.components.MarketImageComposition;
import bisq.i18n.Res;
import bisq.presentation.formatters.PriceFormatter;
import bisq.settings.CookieKey;
import bisq.settings.FavouriteMarketsService;
import bisq.settings.SettingsService;
import bisq.user.profile.UserProfile;
import bisq.user.reputation.ReputationService;
import javafx.collections.ListChangeListener;
import javafx.scene.layout.StackPane;
import lombok.extern.slf4j.Slf4j;
import org.fxmisc.easybind.EasyBind;
import org.fxmisc.easybind.Subscription;

import java.lang.ref.WeakReference;
import java.util.List;
import java.util.Optional;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import static com.google.common.base.Preconditions.checkArgument;

@Slf4j
public final class BisqEasyOfferbookController extends ChatController<BisqEasyOfferbookView, BisqEasyOfferbookModel> {
    private static final double MARKET_SELECTION_LIST_CELL_HEIGHT = 53;

    private final SettingsService settingsService;
    private final MarketPriceService marketPriceService;
    private final BisqEasyOfferbookChannelService bisqEasyOfferbookChannelService;
    private final ReputationService reputationService;
    private final FavouriteMarketsService favouriteMarketsService;
    private final BisqEasyOfferbookModel bisqEasyOfferbookModel;
    private final ChatNotificationService chatNotificationService;
    private final Predicate<MarketChannelItem> marketChannelItemsPredicate;
    private final Predicate<MarketChannelItem> favouriteMarketChannelItemsPredicate;
    private Pin bisqEasyPrivateTradeChatChannelsPin, selectedChannelPin, marketPriceByCurrencyMapPin,
            favouriteMarketsPin, offerMessagesPin, showBuyOffersPin, showOfferListExpandedSettingsPin,
            showMarketSelectionListCollapsedSettingsPin;
    private Subscription marketSelectorSearchPin, selectedMarketFilterPin, selectedMarketSortTypePin,
            showBuyOffersFromModelPin;

    public BisqEasyOfferbookController(ServiceProvider serviceProvider) {
        super(serviceProvider, ChatChannelDomain.BISQ_EASY_OFFERBOOK, NavigationTarget.BISQ_EASY_OFFERBOOK);

        settingsService = serviceProvider.getSettingsService();
        marketPriceService = serviceProvider.getBondedRolesService().getMarketPriceService();
        bisqEasyOfferbookChannelService = chatService.getBisqEasyOfferbookChannelService();
        reputationService = serviceProvider.getUserService().getReputationService();
        favouriteMarketsService = serviceProvider.getFavouriteMarketsService();
        chatNotificationService = serviceProvider.getChatService().getChatNotificationService();

        bisqEasyOfferbookModel = getModel();
        createMarketChannels();

        marketChannelItemsPredicate = item ->
                model.getMarketFilterPredicate().test(item) &&
                        model.getMarketSearchTextPredicate().test(item) &&
                        model.getMarketPricePredicate().test(item) &&
                        !item.getIsFavourite().get();
        favouriteMarketChannelItemsPredicate = item -> item.getIsFavourite().get();
    }

    @Override
    public BisqEasyOfferbookModel createAndGetModel(ChatChannelDomain chatChannelDomain) {
        return new BisqEasyOfferbookModel(chatChannelDomain);
    }

    @Override
    public BisqEasyOfferbookView createAndGetView() {
        return new BisqEasyOfferbookView(model,
                this,
                chatMessageContainerController.getView().getRoot(),
                channelSidebar.getRoot());
    }

    @Override
    public void onActivate() {
        super.onActivate();

        model.getMarketSelectorSearchText().set("");

        showBuyOffersPin = FxBindings.bindBiDir(model.getShowBuyOffers()).to(settingsService.getShowBuyOffers());
        showOfferListExpandedSettingsPin = FxBindings.bindBiDir(model.getShowOfferListExpanded()).to(settingsService.getShowOfferListExpanded());
        showMarketSelectionListCollapsedSettingsPin = FxBindings.bindBiDir(model.getShowMarketSelectionListCollapsed())
                .to(settingsService.getShowMarketSelectionListCollapsed());

        ObservableArray<BisqEasyOpenTradeChannel> bisqEasyOpenTradeChannels = chatService.getBisqEasyOpenTradeChannelService().getChannels();
        bisqEasyPrivateTradeChatChannelsPin = bisqEasyOpenTradeChannels.addObserver(() ->
                UIThread.run(() -> model.getIsTradeChannelVisible().set(!bisqEasyOpenTradeChannels.isEmpty())));

        selectedChannelPin = FxBindings.subscribe(selectionService.getSelectedChannel(), this::selectedChannelChanged);

        marketSelectorSearchPin = EasyBind.subscribe(model.getMarketSelectorSearchText(), searchText -> {
            if (searchText == null || searchText.trim().isEmpty()) {
                model.setMarketSearchTextPredicate(item -> true);
            } else {
                String search = searchText.trim().toLowerCase();
                model.setMarketSearchTextPredicate(item ->
                        item != null &&
                                (item.getMarket().getQuoteCurrencyCode().toLowerCase().contains(search) ||
                                        item.getMarket().getQuoteCurrencyDisplayName().toLowerCase().contains(search))
                );
            }
            updateFilteredMarketChannelItems();
        });

        BisqEasyMarketFilter persistedMarketsFilter = settingsService.getCookie().asString(CookieKey.MARKETS_FILTER).map(name ->
                ProtobufUtils.enumFromProto(BisqEasyMarketFilter.class, name, BisqEasyMarketFilter.ALL)).orElse(BisqEasyMarketFilter.ALL);
        model.getSelectedMarketsFilter().set(persistedMarketsFilter);

        selectedMarketFilterPin = EasyBind.subscribe(model.getSelectedMarketsFilter(), filter -> {
            if (filter != null) {
                model.setMarketFilterPredicate(MarketFilterPredicate.getPredicate(filter));
                settingsService.setCookie(CookieKey.MARKETS_FILTER, model.getSelectedMarketsFilter().get().name());
                updateFilteredMarketChannelItems();
            }
            model.getShouldShowAppliedFilters().set(filter == BisqEasyMarketFilter.WITH_OFFERS || filter == BisqEasyMarketFilter.FAVOURITES);
        });

        marketPriceByCurrencyMapPin = marketPriceService.getMarketPriceByCurrencyMap().addObserver(() -> {
            UIThread.run(() -> {
                model.setMarketPricePredicate(item -> marketPriceService.getMarketPriceByCurrencyMap().isEmpty() ||
                        marketPriceService.getMarketPriceByCurrencyMap().containsKey(item.getMarket()));
                updateFilteredMarketChannelItems();
            });
        });

        model.getMarketChannelItems().addListener(new WeakReference<>(
                (ListChangeListener<? super MarketChannelItem>) c -> updateFilteredMarketChannelItems()
        ).get());

        showBuyOffersFromModelPin = EasyBind.subscribe(model.getShowBuyOffers(), showBuyOffers -> {
            model.getFilteredOfferMessageItems().setPredicate(item ->
                    showBuyOffers == item.isBuyOffer()
            );
        });

        MarketSortType persistedMarketSortType = settingsService.getCookie().asString(CookieKey.MARKET_SORT_TYPE).map(name ->
                        ProtobufUtils.enumFromProto(MarketSortType.class, name, MarketSortType.NUM_OFFERS))
                .orElse(MarketSortType.NUM_OFFERS);
        model.getSelectedMarketSortType().set(persistedMarketSortType);
        selectedMarketSortTypePin = EasyBind.subscribe(model.getSelectedMarketSortType(), marketSortType -> {
            if (marketSortType != null) {
                settingsService.setCookie(CookieKey.MARKET_SORT_TYPE, marketSortType.name());
            }
        });

        favouriteMarketsPin = settingsService.getFavouriteMarkets().addObserver(new CollectionObserver<>() {
            @Override
            public void add(Market market) {
                UIThread.run(() -> {
                    findMarketChannelItem(market).ifPresent(item -> item.getIsFavourite().set(true));
                    updateFilteredMarketChannelItems();
                    updateFavouriteMarketChannelItems();
                });
            }

            @Override
            public void remove(Object element) {
                if (element instanceof Market) {
                    Market market = (Market) element;
                    UIThread.run(() -> {
                        findMarketChannelItem(market).ifPresent(item -> item.getIsFavourite().set(false));
                        updateFilteredMarketChannelItems();
                        updateFavouriteMarketChannelItems();
                    });
                }
            }

            @Override
            public void clear() {
                UIThread.run(() -> {
                    model.getMarketChannelItems().forEach(item -> item.getIsFavourite().set(false));
                    updateFilteredMarketChannelItems();
                    updateFavouriteMarketChannelItems();
                });
            }
        });

        model.getSortedMarketChannelItems().setComparator(model.getSelectedMarketSortType().get().getComparator());

        updateFilteredMarketChannelItems();
        updateFavouriteMarketChannelItems();
        maybeSelectFirst();

        model.getMarketChannelItems().forEach(MarketChannelItem::onActivate);
    }

    @Override
    public void onDeactivate() {
        super.onDeactivate();

        if (offerMessagesPin != null) {
            offerMessagesPin.unbind();
        }
        model.getMarketChannelItems().forEach(MarketChannelItem::onDeactivate);

        showBuyOffersPin.unbind();
        showOfferListExpandedSettingsPin.unbind();
        showMarketSelectionListCollapsedSettingsPin.unbind();
        bisqEasyPrivateTradeChatChannelsPin.unbind();
        selectedChannelPin.unbind();
        marketSelectorSearchPin.unsubscribe();
        selectedMarketFilterPin.unsubscribe();
        marketPriceByCurrencyMapPin.unbind();
        selectedMarketSortTypePin.unsubscribe();
        favouriteMarketsPin.unbind();
        showBuyOffersFromModelPin.unsubscribe();

        resetSelectedChildTarget();
    }

    @Override
    protected void selectedChannelChanged(ChatChannel<? extends ChatMessage> chatChannel) {
        super.selectedChannelChanged(chatChannel);

        UIThread.run(() -> {
            if (chatChannel == null) {
                model.getSelectedMarketChannelItem().set(null);
                maybeSelectFirst();
            }

            if (chatChannel instanceof BisqEasyOfferbookChannel) {
                BisqEasyOfferbookChannel channel = (BisqEasyOfferbookChannel) chatChannel;

                model.getMarketChannelItems().stream()
                        .filter(item -> item.getChannel().equals(channel))
                        .findAny()
                        .ifPresent(item -> model.getSelectedMarketChannelItem().set(item));

                model.getSearchText().set("");
                resetSelectedChildTarget();

                String channelTitle = channel.getShortDescription();
                model.getChannelTitle().set(channelTitle);

                String marketSpecs = channel.getDisplayString();
                model.getChannelDescription().set(marketSpecs);

                Market market = channel.getMarket();
                StackPane marketsImage = MarketImageComposition.imageBoxForMarkets(
                        market.getBaseCurrencyCode().toLowerCase(),
                        market.getQuoteCurrencyCode().toLowerCase());
                model.getChannelIconNode().set(marketsImage);

                model.getFiatAmountTitle().set(Res.get("bisqEasy.offerbook.offerList.table.columns.fiatAmount", channel.getMarket().getQuoteCurrencyCode()).toUpperCase());

                updateMarketPrice();
                bindOfferMessages(channel);
            }
        });
    }

    void onCreateOffer() {
        ChatChannel<? extends ChatMessage> chatChannel = model.getSelectedChannel();
        checkArgument(chatChannel instanceof BisqEasyOfferbookChannel,
                "channel must be instanceof BisqEasyPublicChatChannel at onCreateOfferButtonClicked");
        Navigation.navigateTo(NavigationTarget.TRADE_WIZARD, new TradeWizardController.InitData(true));
    }

    void onSortMarkets(MarketSortType marketSortType) {
        model.getSelectedMarketSortType().set(marketSortType);
        model.getSortedMarketChannelItems().setComparator(marketSortType.getComparator());
    }

    void onSelectMarketChannelItem(MarketChannelItem item) {
        if (item == null) {
            selectionService.selectChannel(null);
        } else if (!item.getChannel().equals(selectionService.getSelectedChannel().get())) {
            selectionService.selectChannel(item.getChannel());
            chatNotificationService.consume(item.getChannel());
        }
    }

    void onSelectOfferMessageItem(OfferMessageItem item) {
        chatMessageContainerController.highlightOfferChatMessage(item == null ? null : item.getBisqEasyOfferbookMessage());
    }

    double getMarketSelectionListCellHeight() {
        return MARKET_SELECTION_LIST_CELL_HEIGHT;
    }

    void toggleOfferList() {
        model.getShowOfferListExpanded().set(!model.getShowOfferListExpanded().get());
    }

    void toggleMarketSelectionList() {
        model.getShowMarketSelectionListCollapsed().set(!model.getShowMarketSelectionListCollapsed().get());
    }

    void onSelectBuyFromFilter() {
        model.getShowBuyOffers().set(false);
    }

    void onSelectSellToFilter() {
        model.getShowBuyOffers().set(true);
    }

    private void createMarketChannels() {
        List<MarketChannelItem> marketChannelItems = bisqEasyOfferbookChannelService.getChannels().stream()
                .map(channel -> new MarketChannelItem(channel, favouriteMarketsService, chatNotificationService))
                .collect(Collectors.toList());
        model.getMarketChannelItems().setAll(marketChannelItems);
    }

    private void updateMarketPrice() {
        Market selectedMarket = bisqEasyOfferbookModel.getSelectedMarketChannelItem().get().getMarket();
        if (selectedMarket != null) {
            marketPriceService
                    .findMarketPrice(selectedMarket)
                    .ifPresent(marketPrice ->
                            model.getMarketPrice().set(PriceFormatter.format(marketPrice.getPriceQuote(), true)));
        }
    }

    private void updateFilteredMarketChannelItems() {
        model.getFilteredMarketChannelItems().setPredicate(null);
        model.getFilteredMarketChannelItems().setPredicate(marketChannelItemsPredicate);
        model.getMarketChannelItems().forEach(MarketChannelItem::onActivate);
    }

    private void updateFavouriteMarketChannelItems() {
        // FilteredList has no API for refreshing/invalidating so that the tableView gets updated.
        // Calling refresh on the tableView also did not refresh the collection.
        // Thus, we trigger a change of the predicate to force a refresh.
        model.getFavouriteMarketChannelItems().setPredicate(null);
        model.getFavouriteMarketChannelItems().setPredicate(favouriteMarketChannelItemsPredicate);
        double padding = 21;
        double tableViewHeight = (model.getFavouriteMarketChannelItems().size() * MARKET_SELECTION_LIST_CELL_HEIGHT) + padding;
        model.getFavouritesTableViewHeight().set(tableViewHeight);
        model.getMarketChannelItems().forEach(MarketChannelItem::onActivate);
    }

    private void maybeSelectFirst() {
        if (selectionService.getSelectedChannel().get() == null &&
                !bisqEasyOfferbookChannelService.getChannels().isEmpty() &&
                !model.getSortedMarketChannelItems().isEmpty()) {
            selectionService.selectChannel(model.getSortedMarketChannelItems().get(0).getChannel());
        }
    }

    private Optional<MarketChannelItem> findMarketChannelItem(Market market) {
        return model.getMarketChannelItems().stream()
                .filter(e -> e.getMarket().equals(market))
                .findFirst();
    }


    private void bindOfferMessages(BisqEasyOfferbookChannel channel) {
        model.getOfferMessageItems().clear();
        offerMessagesPin = channel.getChatMessages().addObserver(new CollectionObserver<>() {
            @Override
            public void add(BisqEasyOfferbookMessage bisqEasyOfferbookMessage) {
                Optional<UserProfile> userProfile = userProfileService.findUserProfile(bisqEasyOfferbookMessage.getAuthorUserProfileId());
                boolean shouldAddOfferMessage = bisqEasyOfferbookMessage.hasBisqEasyOffer()
                        && bisqEasyOfferbookMessage.getBisqEasyOffer().isPresent()
                        && userProfile.isPresent();
                if (shouldAddOfferMessage) {
                    UIThread.run(() -> {
                        if (model.getOfferMessageItems().stream()
                                .noneMatch(item -> item.getBisqEasyOfferbookMessage().equals(bisqEasyOfferbookMessage))) {
                            OfferMessageItem item = new OfferMessageItem(bisqEasyOfferbookMessage,
                                    userProfile.get(),
                                    reputationService,
                                    marketPriceService,
                                    userProfileService);
                            model.getOfferMessageItems().add(item);
                        }
                    });
                }
            }

            @Override
            public void remove(Object element) {
                if (element instanceof BisqEasyOfferbookMessage && ((BisqEasyOfferbookMessage) element).hasBisqEasyOffer()) {
                    UIThread.run(() -> {
                        BisqEasyOfferbookMessage offerMessage = (BisqEasyOfferbookMessage) element;
                        Optional<OfferMessageItem> toRemove = model.getOfferMessageItems().stream()
                                .filter(item -> item.getBisqEasyOfferbookMessage().getId().equals(offerMessage.getId()))
                                .findAny();
                        toRemove.ifPresent(item -> {
                            item.dispose();
                            model.getOfferMessageItems().remove(item);
                        });
                    });
                }
            }

            @Override
            public void clear() {
                UIThread.run(() -> {
                    model.getOfferMessageItems().forEach(OfferMessageItem::dispose);
                    model.getOfferMessageItems().clear();
                });
            }
        });
    }
}
