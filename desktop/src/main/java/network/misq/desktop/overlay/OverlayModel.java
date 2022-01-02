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

package network.misq.desktop.overlay;

import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.scene.Parent;
import lombok.extern.slf4j.Slf4j;
import network.misq.desktop.common.view.Controller;
import network.misq.desktop.common.view.Model;
import network.misq.desktop.common.view.View;
@Slf4j
public class OverlayModel implements Model {
    final ObjectProperty<View<? extends Parent, ? extends Model, ? extends Controller>> view = new SimpleObjectProperty<>();

    public void selectView(View<? extends Parent, ? extends Model, ? extends Controller> view) {
        this.view.set(view);
    }
}
