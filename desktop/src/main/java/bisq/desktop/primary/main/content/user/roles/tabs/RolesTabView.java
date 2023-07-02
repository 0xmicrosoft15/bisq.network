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

package bisq.desktop.primary.main.content.user.roles.tabs;

import bisq.desktop.common.utils.Styles;
import bisq.desktop.common.view.NavigationTarget;
import bisq.desktop.common.view.TabView;
import bisq.i18n.Res;
import javafx.geometry.Insets;
import javafx.scene.control.Label;
import javafx.scene.layout.VBox;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class RolesTabView extends TabView<RolesTabModel, RolesTabController> {
    public RolesTabView(RolesTabModel model, RolesTabController controller) {
        super(model, controller);

        root.setPadding(new Insets(30));
        root.getStyleClass().add("bisq-box-2");

        VBox.setMargin(contentPane, new Insets(20, 0, 0, 0));

        Styles styles = new Styles("bisq-text-grey-9", "bisq-text-white", "bisq-text-green", "bisq-text-grey-9");
        addTab(Res.get("user.roles.type.MEDIATOR"),
                NavigationTarget.REGISTER_MEDIATOR,
                styles);
     /*   addTab(Res.get("roles.type.ARBITRATOR"),
                NavigationTarget.REGISTER_ARBITRATOR,
                styles);*/
        addTab(Res.get("user.roles.type.MODERATOR"),
                NavigationTarget.REGISTER_MODERATOR,
                styles);
        addTab(Res.get("user.roles.type.SECURITY_MANAGER"),
                NavigationTarget.REGISTER_SECURITY_MANAGER,
                styles);
        addTab(Res.get("user.roles.type.RELEASE_MANAGER"),
                NavigationTarget.REGISTER_RELEASE_MANAGER,
                styles);
        addTab(Res.get("user.roles.type.SEED_NODE"),
                NavigationTarget.REGISTER_SEED_NODE,
                styles);
        addTab(Res.get("user.roles.type.ORACLE_NODE"),
                NavigationTarget.REGISTER_ORACLE_NODE,
                styles);
        addTab(Res.get("user.roles.type.EXPLORER_NODE"),
                NavigationTarget.REGISTER_EXPLORER_NODE,
                styles);
        addTab(Res.get("user.roles.type.MARKET_PRICE_NODE"),
                NavigationTarget.REGISTER_MARKET_PRICE_NODE,
                styles);

    }

    @Override
    protected void onViewAttached() {
        line.prefWidthProperty().unbind();
        line.prefWidthProperty().bind(root.widthProperty().subtract(61));

        onStartTransition();
    }

    @Override
    protected void onViewDetached() {
        line.prefWidthProperty().unbind();
    }

    @Override
    protected void setupTopBox() {
        headLine = new Label(Res.get("user.roles.headline"));
        headLine.getStyleClass().add("bisq-text-headline-5");

        tabs.setFillHeight(true);
        tabs.setSpacing(46);
        tabs.setMinHeight(35);

        topBox = new VBox();
        VBox.setMargin(headLine, new Insets(-10, 0, 17, -2));
        topBox.getChildren().addAll(headLine, tabs);
    }

    @Override
    protected void setupLineAndMarker() {
        super.setupLineAndMarker();

        line.getStyleClass().remove("bisq-dark-bg");
        line.getStyleClass().add("bisq-mid-grey");
    }
}
