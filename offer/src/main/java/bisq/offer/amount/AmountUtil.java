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

package bisq.offer.amount;

import bisq.common.currency.Market;
import bisq.common.monetary.Monetary;
import bisq.offer.Offer;
import bisq.offer.amount.spec.AmountSpec;
import bisq.offer.amount.spec.AmountSpecUtil;
import bisq.offer.price.PriceUtil;
import bisq.offer.price.spec.PriceSpec;
import bisq.oracle.marketprice.MarketPriceService;

import java.util.Optional;

/**
 * Public APIs for getting different types of amounts:
 * - Base side / quote side
 * - fixPriceAmount, minAmount, maxAmount
 * - Combinations of fallbacks for fixPriceAmount, minAmount, maxAmount
 */
public class AmountUtil {

    ///////////////////////////////////////////////////////////////////////////////////////////////////
    // BaseAmount: If no BaseAmountSpec we calculate it from the QuoteAmountSpec with the PriceSpec
    ///////////////////////////////////////////////////////////////////////////////////////////////////

    // Fixed
    public static Optional<Monetary> findBaseSideFixedAmount(MarketPriceService marketPriceService, Offer offer) {
        return findBaseSideFixedAmount(marketPriceService, offer.getAmountSpec(), offer.getPriceSpec(), offer.getMarket());
    }

    public static Optional<Monetary> findBaseSideFixedAmount(MarketPriceService marketPriceService,
                                                             AmountSpec amountSpec,
                                                             PriceSpec priceSpec,
                                                             Market market) {
        return AmountSpecUtil.findBaseSideFixedAmountFromSpec(amountSpec, market.getBaseCurrencyCode())
                .or(() -> AmountSpecUtil.findQuoteSideFixedAmountFromSpec(amountSpec, market.getQuoteCurrencyCode())
                        .flatMap(quoteAmount -> PriceUtil.findQuote(marketPriceService, priceSpec, market)
                                .map(quote -> quote.toBaseMonetary(quoteAmount))
                        ));
    }

    // Min
    public static Optional<Monetary> findBaseSideMinAmount(MarketPriceService marketPriceService, Offer offer) {
        return findBaseSideMinAmount(marketPriceService, offer.getAmountSpec(), offer.getPriceSpec(), offer.getMarket());
    }

    public static Optional<Monetary> findBaseSideMinAmount(MarketPriceService marketPriceService,
                                                           AmountSpec amountSpec,
                                                           PriceSpec priceSpec,
                                                           Market market) {
        return AmountSpecUtil.findBaseSideMinAmountFromSpec(amountSpec, market.getBaseCurrencyCode())
                .or(() -> AmountSpecUtil.findQuoteSideMinAmountFromSpec(amountSpec, market.getQuoteCurrencyCode())
                        .flatMap(quoteAmount -> PriceUtil.findQuote(marketPriceService, priceSpec, market)
                                .map(quote -> quote.toBaseMonetary(quoteAmount))
                        ));
    }

    // Max
    public static Optional<Monetary> findBaseSideMaxAmount(MarketPriceService marketPriceService, Offer offer) {
        return findBaseSideMaxAmount(marketPriceService, offer.getAmountSpec(), offer.getPriceSpec(), offer.getMarket());
    }

    public static Optional<Monetary> findBaseSideMaxAmount(MarketPriceService marketPriceService,
                                                           AmountSpec amountSpec,
                                                           PriceSpec priceSpec,
                                                           Market market) {
        return AmountSpecUtil.findBaseSideMaxAmountFromSpec(amountSpec, market.getBaseCurrencyCode())
                .or(() -> AmountSpecUtil.findQuoteSideMaxAmountFromSpec(amountSpec, market.getQuoteCurrencyCode())
                        .flatMap(quoteAmount -> PriceUtil.findQuote(marketPriceService, priceSpec, market)
                                .map(quote -> quote.toBaseMonetary(quoteAmount))
                        ));
    }

    // Combinations
    public static Optional<Monetary> findBaseSideMinOrFixedAmount(MarketPriceService marketPriceService, Offer offer) {
        return findBaseSideMinOrFixedAmount(marketPriceService, offer.getAmountSpec(), offer.getPriceSpec(), offer.getMarket());
    }

    public static Optional<Monetary> findBaseSideMinOrFixedAmount(MarketPriceService marketPriceService,
                                                                  AmountSpec amountSpec,
                                                                  PriceSpec priceSpec,
                                                                  Market market) {
        return AmountSpecUtil.findBaseSideMinOrFixedAmountFromSpec(amountSpec, market.getBaseCurrencyCode())
                .or(() -> AmountSpecUtil.findQuoteSideMinOrFixedAmountFromSpec(amountSpec, market.getQuoteCurrencyCode())
                        .flatMap(quoteAmount -> PriceUtil.findQuote(marketPriceService, priceSpec, market)
                                .map(quote -> quote.toBaseMonetary(quoteAmount))
                        ));
    }

    public static Optional<Monetary> findBaseSideMaxOrFixedAmount(MarketPriceService marketPriceService, Offer offer) {
        return findBaseSideMaxOrFixedAmount(marketPriceService, offer.getAmountSpec(), offer.getPriceSpec(), offer.getMarket());
    }

    public static Optional<Monetary> findBaseSideMaxOrFixedAmount(MarketPriceService marketPriceService,
                                                                  AmountSpec amountSpec,
                                                                  PriceSpec priceSpec,
                                                                  Market market) {
        return AmountSpecUtil.findBaseSideMaxOrFixedAmountFromSpec(amountSpec, market.getBaseCurrencyCode())
                .or(() -> AmountSpecUtil.findQuoteSideMaxOrFixedAmountFromSpec(amountSpec, market.getQuoteCurrencyCode())
                        .flatMap(quoteAmount -> PriceUtil.findQuote(marketPriceService, priceSpec, market)
                                .map(quote -> quote.toBaseMonetary(quoteAmount))
                        ));
    }


    ///////////////////////////////////////////////////////////////////////////////////////////////////
    // QuoteAmount: If no QuoteAmountSpec we calculate it from the BaseAmountSpec with the PriceSpec
    ///////////////////////////////////////////////////////////////////////////////////////////////////

    // Fixed
    public static Optional<Monetary> findQuoteSideFixedAmount(MarketPriceService marketPriceService, Offer offer) {
        return findQuoteSideFixedAmount(marketPriceService, offer.getAmountSpec(), offer.getPriceSpec(), offer.getMarket());
    }

    public static Optional<Monetary> findQuoteSideFixedAmount(MarketPriceService marketPriceService,
                                                              AmountSpec amountSpec,
                                                              PriceSpec priceSpec,
                                                              Market market) {
        return AmountSpecUtil.findQuoteSideFixedAmountFromSpec(amountSpec, market.getQuoteCurrencyCode())
                .or(() -> AmountSpecUtil.findBaseSideFixedAmountFromSpec(amountSpec, market.getBaseCurrencyCode())
                        .flatMap(baseAmount -> PriceUtil.findQuote(marketPriceService, priceSpec, market)
                                .map(quote -> quote.toQuoteMonetary(baseAmount))
                        ));
    }

    // Min
    public static Optional<Monetary> findQuoteSideMinAmount(MarketPriceService marketPriceService, Offer offer) {
        return findQuoteSideMinAmount(marketPriceService, offer.getAmountSpec(), offer.getPriceSpec(), offer.getMarket());
    }

    public static Optional<Monetary> findQuoteSideMinAmount(MarketPriceService marketPriceService,
                                                            AmountSpec amountSpec,
                                                            PriceSpec priceSpec,
                                                            Market market) {
        return AmountSpecUtil.findQuoteSideMinAmountFromSpec(amountSpec, market.getQuoteCurrencyCode())
                .or(() -> AmountSpecUtil.findBaseSideMinAmountFromSpec(amountSpec, market.getBaseCurrencyCode())
                        .flatMap(baseAmount -> PriceUtil.findQuote(marketPriceService, priceSpec, market)
                                .map(quote -> quote.toQuoteMonetary(baseAmount))
                        ));
    }

    // Max
    public static Optional<Monetary> findQuoteSideMaxAmount(MarketPriceService marketPriceService, Offer offer) {
        return findQuoteSideMaxAmount(marketPriceService, offer.getAmountSpec(), offer.getPriceSpec(), offer.getMarket());
    }

    public static Optional<Monetary> findQuoteSideMaxAmount(MarketPriceService marketPriceService,
                                                            AmountSpec amountSpec,
                                                            PriceSpec priceSpec,
                                                            Market market) {
        return AmountSpecUtil.findQuoteSideMaxAmountFromSpec(amountSpec, market.getQuoteCurrencyCode())
                .or(() -> AmountSpecUtil.findBaseSideMaxAmountFromSpec(amountSpec, market.getBaseCurrencyCode())
                        .flatMap(baseAmount -> PriceUtil.findQuote(marketPriceService, priceSpec, market)
                                .map(quote -> quote.toQuoteMonetary(baseAmount))
                        ));
    }

    // Combinations
    public static Optional<Monetary> findQuoteSideMinOrFixedAmount(MarketPriceService marketPriceService, Offer offer) {
        return findQuoteSideMinOrFixedAmount(marketPriceService, offer.getAmountSpec(), offer.getPriceSpec(), offer.getMarket());
    }

    public static Optional<Monetary> findQuoteSideMinOrFixedAmount(MarketPriceService marketPriceService,
                                                                   AmountSpec amountSpec,
                                                                   PriceSpec priceSpec,
                                                                   Market market) {
        return AmountSpecUtil.findQuoteSideMinOrFixedAmountFromSpec(amountSpec, market.getQuoteCurrencyCode())
                .or(() -> AmountSpecUtil.findBaseSideMinOrFixedAmountFromSpec(amountSpec, market.getBaseCurrencyCode())
                        .flatMap(baseAmount -> PriceUtil.findQuote(marketPriceService, priceSpec, market)
                                .map(quote -> quote.toQuoteMonetary(baseAmount))
                        ));
    }

    public static Optional<Monetary> findQuoteSideMaxOrFixedAmount(MarketPriceService marketPriceService, Offer offer) {
        return findQuoteSideMaxOrFixedAmount(marketPriceService, offer.getAmountSpec(), offer.getPriceSpec(), offer.getMarket());
    }

    public static Optional<Monetary> findQuoteSideMaxOrFixedAmount(MarketPriceService marketPriceService,
                                                                   AmountSpec amountSpec,
                                                                   PriceSpec priceSpec,
                                                                   Market market) {
        return AmountSpecUtil.findQuoteSideMaxOrFixedAmountFromSpec(amountSpec, market.getQuoteCurrencyCode())
                .or(() -> AmountSpecUtil.findBaseSideMaxOrFixedAmountFromSpec(amountSpec, market.getBaseCurrencyCode())
                        .flatMap(baseAmount -> PriceUtil.findQuote(marketPriceService, priceSpec, market)
                                .map(quote -> quote.toQuoteMonetary(baseAmount))
                        ));
    }
}