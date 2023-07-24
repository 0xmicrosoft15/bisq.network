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

package bisq.desktop.main.content.academy.bisq;

import bisq.desktop.main.content.academy.AcademyView;
import javafx.geometry.Insets;
import javafx.scene.layout.VBox;
import javafx.scene.control.Label;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class BisqAcademyView extends AcademyView<BisqAcademyModel, BisqAcademyController> {

    public BisqAcademyView(BisqAcademyModel model, BisqAcademyController controller) {
        super(model, controller);

        Label exchangeDecentralizedHeadline = addHeadlineLabel("exchangeDecentralizedHeadline");
        Label exchangeDecentralizedContent = addContentLabel("exchangeDecentralizedContent");
        Label whyBisqHeadline = addHeadlineLabel("whyBisqHeadline");
        Label whyBisqContent = addContentLabel("whyBisqContent");
        Label tradeSafelyHeadline = addHeadlineLabel("tradeSafelyHeadline");
        Label tradeSafelyContent = addContentLabel("tradeSafelyContent");
        addLearnMoreHyperlink();

        VBox.setMargin(exchangeDecentralizedHeadline, new Insets(25, 0, 0, 0));
        VBox.setMargin(whyBisqHeadline, new Insets(35, 0, 0, 0));
        VBox.setMargin(tradeSafelyHeadline, new Insets(35, 0, 0, 0));
        VBox.setMargin(tradeSafelyContent, new Insets(0, 0, 15, 0));
    }

    @Override
    protected String getKey() {
        return "bisq";
    }

    @Override
    protected String getIconId() {
        return "learn-bisq";
    }


    @Override
    protected String getUrl() {
        return "https://bisq.network/" + getKey();
    }
}
