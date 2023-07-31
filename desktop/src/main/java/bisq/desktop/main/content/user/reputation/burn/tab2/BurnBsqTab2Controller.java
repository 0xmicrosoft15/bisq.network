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

package bisq.desktop.main.content.user.reputation.burn.tab2;

import bisq.common.util.MathUtils;
import bisq.desktop.ServiceProvider;
import bisq.desktop.common.Browser;
import bisq.desktop.common.view.Controller;
import bisq.desktop.common.view.Navigation;
import bisq.desktop.common.view.NavigationTarget;
import bisq.desktop.main.content.user.reputation.components.Simulation;
import bisq.user.reputation.ProofOfBurnService;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class BurnBsqTab2Controller implements Controller {
    @Getter
    private final BurnBsqTab2View view;
    private final BurnBsqTab2Model model;

    public BurnBsqTab2Controller(ServiceProvider serviceProvider) {
        model = new BurnBsqTab2Model();
        Simulation simulation = new Simulation(BurnBsqTab2Controller::calculateSimScore);
        view = new BurnBsqTab2View(model, this, simulation.getViewRoot());
    }

    @Override
    public void onActivate() {
    }

    private static String calculateSimScore(String amount, Number age) {
        try {
            // amountAsLong is the smallest unit of BSQ (100 = 1 BSQ)
            long amountAsLong = MathUtils.roundDoubleToLong(Double.parseDouble(amount) * 100);
            long ageInDays = age.intValue();
            long totalScore = ProofOfBurnService.doCalculateScore(amountAsLong, ageInDays);
            return String.valueOf(totalScore);
        } catch (Exception e) {
            return "";
        }
    }

    @Override
    public void onDeactivate() {
    }

    void onBack() {
        Navigation.navigateTo(NavigationTarget.BURN_BSQ_TAB_1);
    }

    void onNext() {
        Navigation.navigateTo(NavigationTarget.BURN_BSQ_TAB_3);
    }

    void onLearnMore() {
        Browser.open("https://bisq.wiki/reputation/burnBsq");
    }
}
