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

package bisq.desktop.main.content;

import bisq.desktop.common.Transitions;
import bisq.desktop.common.view.NavigationView;
import bisq.desktop.main.content.chat.ChatView;
import javafx.geometry.Insets;
import javafx.scene.layout.StackPane;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class ContentView extends NavigationView<StackPane, ContentModel, ContentController> {
    public ContentView(ContentModel model, ContentController controller) {
        super(new StackPane(), model, controller);

        model.getView().addListener((observable, oldValue, newValue) -> {
            if (!(newValue instanceof ChatView)) {
                StackPane.setMargin(newValue.getRoot(), new Insets(33, 67, 67, 67));
            }
            root.getChildren().add(newValue.getRoot());
            Transitions.transitContentViews(oldValue, newValue);
        });
    }

    @Override
    protected void onViewAttached() {
    }

    @Override
    protected void onViewDetached() {
    }
}
