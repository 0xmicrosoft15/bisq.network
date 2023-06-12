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

package bisq.offer.utils;

import bisq.common.monetary.Quote;
import bisq.offer.price.PriceUtil;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

@Slf4j
public class PriceUtilTest {
    @Test
    void testOffsetOf() {
        Quote marketQuote = Quote.fromFiatPrice(50000, "USD");
        Quote offerQuote = Quote.fromFiatPrice(50000, "USD");

        double offset = PriceUtil.getPercentageToMarketPrice(marketQuote, offerQuote);
        assertEquals(0, offset);

        offerQuote = Quote.fromFiatPrice(55000, "USD");
        offset = PriceUtil.getPercentageToMarketPrice(marketQuote, offerQuote);
        log.error("" + offset);

        assertEquals(0.1d, offset);

        offerQuote = Quote.fromFiatPrice(45000, "USD");
        offset = PriceUtil.getPercentageToMarketPrice(marketQuote, offerQuote);
        assertEquals(-0.1, offset);

        offerQuote = Quote.fromFiatPrice(100000, "USD");
        offset = PriceUtil.getPercentageToMarketPrice(marketQuote, offerQuote);
        assertEquals(1, offset); // 100% of marketQuote

        offerQuote = Quote.fromFiatPrice(150000, "USD");
        offset = PriceUtil.getPercentageToMarketPrice(marketQuote, offerQuote);
        assertEquals(2, offset); // 200% of marketQuote

        offerQuote = Quote.fromFiatPrice(0, "USD");
        offset = PriceUtil.getPercentageToMarketPrice(marketQuote, offerQuote);
        assertEquals(-1, offset);

        assertThrows(IllegalArgumentException.class,
                () -> PriceUtil.getPercentageToMarketPrice(Quote.fromFiatPrice(0, "USD"),
                        Quote.fromFiatPrice(50000, "USD")));
    }

    @Test
    void testFromMarketPriceOffset() {
        Quote marketQuote = Quote.fromFiatPrice(50000, "USD");

        Quote quote = PriceUtil.fromMarketPriceMarkup(marketQuote, 0);
        assertEquals(500000000, quote.getValue());
        assertEquals(4, quote.getPrecision());
        assertEquals("BTC/USD", quote.getMarket().getMarketCodes());

        quote = PriceUtil.fromMarketPriceMarkup(marketQuote, 1);
        assertEquals(1000000000, quote.getValue());

        quote = PriceUtil.fromMarketPriceMarkup(marketQuote, -1);
        assertEquals(0, quote.getValue());

        quote = PriceUtil.fromMarketPriceMarkup(marketQuote, 0.1);
        assertEquals(550000000, quote.getValue());

        quote = PriceUtil.fromMarketPriceMarkup(marketQuote, -0.1);
        assertEquals(450000000, quote.getValue());
    }
}