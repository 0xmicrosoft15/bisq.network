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

package bisq.desktop.main.content.academy.wallets;

import bisq.desktop.main.content.academy.AcademyBaseView;
import javafx.scene.control.Label;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class WalletsAcademyView extends AcademyBaseView<WalletsAcademyModel, WalletsAcademyController> {

    public WalletsAcademyView(WalletsAcademyModel model, WalletsAcademyController controller) {
        super(model, controller);

        Label whatIsAWalletHeadline = addHeadlineLabel("whatIsAWalletHeadline");
        addContentLabel("whatIsAWalletContent");
        Label howToPickHeadline = addHeadlineLabel("howToPickHeadline");
        Label howToPickContent = addContentLabel("howToPickContent");
        addLearnMoreHyperlink();

        setHeadlineMargin(whatIsAWalletHeadline);
        setHeadlineMargin(howToPickHeadline);
        setLastLabelMargin(howToPickContent);
    }

    @Override
    protected String getKey() {
        return "wallets";
    }

    @Override
    protected String getIconId() {
        return "learn-wallets";
    }

    @Override
    protected String getUrl() {
        return "https://bitcoin.org/en/choose-your-wallet";
    }
}
