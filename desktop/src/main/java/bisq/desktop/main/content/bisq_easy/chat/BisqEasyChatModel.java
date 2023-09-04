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

package bisq.desktop.main.content.bisq_easy.chat;

import bisq.chat.channel.ChatChannelDomain;
import bisq.desktop.common.view.NavigationTarget;
import bisq.desktop.main.content.chat.ChatModel;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.collections.transformation.SortedList;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Getter
public class BisqEasyChatModel extends ChatModel {
    private final BooleanProperty offerOnly = new SimpleBooleanProperty();
    private final BooleanProperty topSeparatorVisible = new SimpleBooleanProperty();
    private final BooleanProperty createOfferButtonVisible = new SimpleBooleanProperty();
    private final BooleanProperty offerOnlyVisible = new SimpleBooleanProperty();
    private final BooleanProperty isTradeChannelVisible = new SimpleBooleanProperty();
    private final BooleanProperty isBisqEasyPrivateTradeChatChannel = new SimpleBooleanProperty();
    private final BooleanProperty showFilterOverlay = new SimpleBooleanProperty();
    private final ObservableList<MarketChannelItem> marketChannelItems = FXCollections.observableArrayList();
    private final FilteredList<MarketChannelItem> filteredMarketChannelItems = new FilteredList<>(marketChannelItems);
    private final SortedList<MarketChannelItem> sortedMarketChannelItems = new SortedList<>(filteredMarketChannelItems);

    public BisqEasyChatModel(ChatChannelDomain chatChannelDomain) {
        super(chatChannelDomain);
    }

    @Override
    public NavigationTarget getDefaultNavigationTarget() {
        return NavigationTarget.NONE;
    }
}
