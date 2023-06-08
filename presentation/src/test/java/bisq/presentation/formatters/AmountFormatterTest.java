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

package bisq.presentation.formatters;

import bisq.common.monetary.Coin;
import bisq.common.monetary.Fiat;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.Locale;

import static org.junit.jupiter.api.Assertions.assertEquals;


public class AmountFormatterTest {

    @Test
    void testFormat() {
        Coin btc = Coin.asBtcFromFaceValue(1.0);
        Assertions.assertEquals("1,00000000", AmountFormatter.formatAmount(btc, Locale.GERMAN));
        assertEquals("1.00000000", AmountFormatter.formatAmount(btc, Locale.US));

        btc = Coin.asBtcFromFaceValue(20123456.1234);
        assertEquals("20123456.12340000", AmountFormatter.formatAmount(btc, Locale.US));

        Fiat usd = Fiat.fromFaceValue(51234.1234, "USD");
        assertEquals("51234.1234", AmountFormatter.formatAmount(usd, Locale.US));
        usd = Fiat.fromFaceValue(51234.56, "USD");
        assertEquals("51234.5600", AmountFormatter.formatAmount(usd, Locale.US));
    }
}