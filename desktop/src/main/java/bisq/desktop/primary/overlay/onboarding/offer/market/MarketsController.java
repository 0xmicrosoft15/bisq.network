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

package bisq.desktop.primary.overlay.onboarding.offer.market;

import bisq.application.DefaultApplicationService;
import bisq.desktop.common.view.Controller;
import bisq.desktop.common.view.Navigation;
import bisq.desktop.common.view.NavigationTarget;
import bisq.desktop.primary.overlay.OverlayController;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class MarketsController implements Controller {
    private final MarketsModel model;
    @Getter
    private final MarketsView view;

    public MarketsController(DefaultApplicationService applicationService) {
        model = new MarketsModel();
        view = new MarketsView(model, this);
    }

    @Override
    public void onActivate() {
        onSelect(MarketsModel.Direction.BUY);
    }

    @Override
    public void onDeactivate() {
    }

    public void onSelect(MarketsModel.Direction selectedDirection) {
        model.getDirection().set(selectedDirection);
    }


    public void onNext() {
        Navigation.navigateTo(NavigationTarget.ONBOARDING_AMOUNT);
    }

    public void onBack() {
        Navigation.navigateTo(NavigationTarget.ONBOARDING_DIRECTION);
    }

    public void onSkip() {
        OverlayController.hide();
    }
}
