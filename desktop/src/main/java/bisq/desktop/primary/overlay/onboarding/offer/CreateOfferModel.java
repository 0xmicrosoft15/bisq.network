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

package bisq.desktop.primary.overlay.onboarding.offer;

import bisq.desktop.common.view.NavigationModel;
import bisq.desktop.common.view.NavigationTarget;
import javafx.beans.property.*;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Getter
public class CreateOfferModel extends NavigationModel {
    private final IntegerProperty navigationProgressIndex = new SimpleIntegerProperty();
    private final StringProperty skipButtonText = new SimpleStringProperty();
    private final BooleanProperty skipButtonVisible = new SimpleBooleanProperty();

    public CreateOfferModel() {
    }

    @Override
    public NavigationTarget getDefaultNavigationTarget() {
        return NavigationTarget.CREATE_OFFER_DIRECTION;
    }
}
