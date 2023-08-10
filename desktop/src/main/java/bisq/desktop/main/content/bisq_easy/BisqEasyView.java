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

package bisq.desktop.main.content.bisq_easy;

import bisq.desktop.common.view.NavigationTarget;
import bisq.desktop.common.view.TabButton;
import bisq.desktop.common.view.TabView;
import bisq.i18n.Res;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class BisqEasyView extends TabView<BisqEasyModel, BisqEasyController> {

    private final TabButton trades, privateChat;

    public BisqEasyView(BisqEasyModel model, BisqEasyController controller) {
        super(model, controller);

        // headLine.setText(Res.get("bisqEasy.headline"));

        addTab(Res.get("bisqEasy.dashboard"), NavigationTarget.BISQ_EASY_ONBOARDING);
        addTab(Res.get("bisqEasy.markets"), NavigationTarget.BISQ_EASY_MARKETS);
        trades = addTab(Res.get("bisqEasy.trades"), NavigationTarget.BISQ_EASY_TRADES);
        privateChat = addTab(Res.get("bisqEasy.privateChat"), NavigationTarget.BISQ_EASY_PRIVATE_CHAT);
    }

    @Override
    protected void onViewAttached() {
        trades.visibleProperty().bind(model.getTradeTabVisible());
        trades.managedProperty().bind(model.getTradeTabVisible());
        privateChat.visibleProperty().bind(model.getPrivateChatTabVisible());
        privateChat.managedProperty().bind(model.getPrivateChatTabVisible());
    }

    @Override
    protected void onViewDetached() {
    }

    @Override
    protected boolean isRightSide() {
        return false;
    }
}
