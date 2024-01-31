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

import bisq.chat.bisqeasy.offerbook.BisqEasyOfferbookChannel;
import bisq.common.currency.Market;
import bisq.desktop.common.utils.ImageUtil;
import javafx.scene.image.ImageView;
import lombok.EqualsAndHashCode;
import lombok.Getter;

@EqualsAndHashCode
@Getter
public class MarketChannelItem {
    private final Market market;
    private final BisqEasyOfferbookChannel channel;
    private final ImageView icon;

    public MarketChannelItem(BisqEasyOfferbookChannel channel) {
        this.channel = channel;
        market = channel.getMarket();
        icon = ImageUtil.getImageViewById("test");
    }

    public String getMarketString() {
        return market.toString();
    }

    @Override
    public String toString() {
        return market.toString();
    }
}
