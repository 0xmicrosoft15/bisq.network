package bisq.desktop.main.content.bisq_easy.offerbook;

import bisq.common.currency.Market;
import bisq.common.currency.MarketRepository;
import bisq.desktop.components.controls.BisqTooltip;
import bisq.desktop.main.content.components.MarketImageComposition;
import bisq.i18n.Res;
import javafx.beans.binding.Bindings;
import javafx.beans.binding.StringExpression;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Cursor;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.util.Callback;

import java.util.Comparator;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class BisqEasyOfferbookUtil {
    private static final List<Market> majorMarkets = MarketRepository.getMajorMarkets();
    private static final Set<String> marketsWithLogos = Stream.of("aed", "afn", "all", "amd", "aoa", "ars", "aud",
            "awg", "azn", "bam", "bbd", "bdt", "bgn", "bhd", "bif", "bmd", "bnd", "bob", "brl", "bsd", "btn", "bwp",
            "byn", "bzd", "cad", "chf", "clp", "cny", "cop", "crc", "cup", "cve", "czk", "djf", "dkk", "dop", "dzd",
            "egp", "ern", "etb", "eur", "fjd", "fkp", "gbp", "gel", "ghs", "gip", "gmd", "gnf", "gtq", "gyd", "hkd",
            "hnl", "htg", "huf", "idr", "ils", "inr", "iqd", "irr", "isk", "jmd", "jod", "jpy", "kes", "kgs", "khr",
            "kmf", "kpw", "krw", "kwd", "kyd", "kzt", "lak", "lbp", "lkr", "lrd", "lsl", "lyd", "mad", "mdl", "mga",
            "mmk", "mnt", "mop", "mru", "mur", "mvr", "mwk", "mxn", "myr", "mzn", "nad", "ngn", "nio", "nok", "npr",
            "nzd", "omr", "pab", "pen", "pgk", "php", "pkr", "pln", "pyg", "qar", "ron", "rsd", "rub", "rwf", "sar",
            "sbd", "scr", "sdg", "sek", "sgd", "sle", "sos", "srd", "ssp", "stn", "svc", "syp", "szl", "thb", "tjs",
            "tmt", "tnd", "top", "try", "ttd", "twd", "tzs", "uah", "ugx", "usd", "uyu", "uzs", "ves", "vnd", "vuv",
            "wst", "xaf", "yer", "zar", "zmw", "zwl")
            .collect(Collectors.toUnmodifiableSet());

    public static Comparator<MarketChannelItem> SortByNumOffers() {
        return (lhs, rhs) -> Integer.compare(rhs.getNumOffers().get(), lhs.getNumOffers().get());
    }

    public static Comparator<MarketChannelItem> SortByMajorMarkets() {
        return (lhs, rhs) -> {
            int index1 = majorMarkets.indexOf(lhs.getMarket());
            int index2 = majorMarkets.indexOf(rhs.getMarket());
            return Integer.compare(index1, index2);
        };
    }

    public static Comparator<MarketChannelItem> SortByMarketNameAsc() {
        return Comparator.comparing(MarketChannelItem::getMarketString);
    }

    public static Comparator<MarketChannelItem> SortByMarketNameDesc() {
        return Comparator.comparing(MarketChannelItem::getMarketString).reversed();
    }

    public static Comparator<MarketChannelItem> SortByMarketActivity() {
        return (lhs, rhs) -> BisqEasyOfferbookUtil.SortByNumOffers()
                .thenComparing(BisqEasyOfferbookUtil.SortByMajorMarkets())
                .thenComparing(BisqEasyOfferbookUtil.SortByMarketNameAsc())
                .compare(lhs, rhs);
    }

    public static Callback<TableColumn<MarketChannelItem, MarketChannelItem>,
            TableCell<MarketChannelItem, MarketChannelItem>> getMarketLabelCellFactory() {
        return column -> new TableCell<>() {
            private final Label marketName = new Label();
            private final Label marketCode = new Label();
            private final Label numOffers = new Label();
            private final HBox hBox = new HBox(10, marketCode, numOffers);
            private final VBox vBox = new VBox(0, marketName, hBox);
            private final Tooltip tooltip = new BisqTooltip();

            {
                setCursor(Cursor.HAND);
                hBox.setAlignment(Pos.CENTER_LEFT);
                vBox.setAlignment(Pos.CENTER_LEFT);
                Tooltip.install(vBox, tooltip);
            }

            @Override
            protected void updateItem(MarketChannelItem item, boolean empty) {
                super.updateItem(item, empty);
                if (item != null && !empty) {
                    marketName.setText(item.getMarket().getQuoteCurrencyName());
                    marketCode.setText(item.getMarket().getQuoteCurrencyCode());
                    StringExpression formattedNumOffers = Bindings.createStringBinding(() ->
                            BisqEasyOfferbookUtil.getFormattedOfferNumber(item.getNumOffers().get()), item.getNumOffers());
                    numOffers.textProperty().bind(formattedNumOffers);
                    StringExpression formattedTooltip = Bindings.createStringBinding(() ->
                            BisqEasyOfferbookUtil.getFormattedTooltip(item.getNumOffers().get(), item.getMarket().getQuoteCurrencyName()), item.getNumOffers());
                    tooltip.textProperty().bind(formattedTooltip);
                    tooltip.setStyle("-fx-text-fill: -fx-dark-text-color;");

                    setGraphic(vBox);
                } else {
                    numOffers.textProperty().unbind();
                    tooltip.textProperty().unbind();

                    setGraphic(null);
                }
            }
        };
    }

    public static Callback<TableColumn<MarketChannelItem, MarketChannelItem>,
            TableCell<MarketChannelItem, MarketChannelItem>> getMarketLogoCellFactory() {
        return column -> new TableCell<>() {
            {
                setCursor(Cursor.HAND);
            }

            @Override
            protected void updateItem(MarketChannelItem item, boolean empty) {
                super.updateItem(item, empty);
                if (item != null && !empty) {
                    String marketCode = item.getMarket().getQuoteCurrencyCode();
                    setGraphic(marketsWithLogos.contains(marketCode.toLowerCase())
                            ? item.getMarketLogo()
                            : MarketImageComposition.createMarketLogoPlaceholder(marketCode));
                } else {
                    setGraphic(null);
                }
            }
        };
    }

    private static String getFormattedOfferNumber(int numOffers) {
        if (numOffers == 0) {
            return "";
        }
        return String.format("(%s)",
                numOffers > 1
                        ? Res.get("bisqEasy.offerbook.marketListCell.numOffers.many", numOffers)
                        : Res.get("bisqEasy.offerbook.marketListCell.numOffers.one", numOffers)
        );
    }

    private static String getFormattedTooltip(int numOffers, String quoteCurrencyName) {
        if (numOffers == 0) {
            return Res.get("bisqEasy.offerbook.marketListCell.numOffers.tooltip.none", quoteCurrencyName);
        }
        return numOffers > 1
                ? Res.get("bisqEasy.offerbook.marketListCell.numOffers.tooltip.many", numOffers, quoteCurrencyName)
                : Res.get("bisqEasy.offerbook.marketListCell.numOffers.tooltip.one", numOffers, quoteCurrencyName);
    }
}
