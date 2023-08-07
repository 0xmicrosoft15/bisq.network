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

package bisq.desktop.main.content.academy.security;

import bisq.desktop.main.content.academy.AcademyView;
import javafx.geometry.Insets;
import javafx.scene.control.Hyperlink;
import javafx.scene.control.Label;
import javafx.scene.layout.VBox;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class SecurityAcademyView extends AcademyView<SecurityAcademyModel, SecurityAcademyController> {

    public SecurityAcademyView(SecurityAcademyModel model, SecurityAcademyController controller) {
        super(model, controller);

        Label introContent = addContentLabel("introContent");
        Label securingYourKeysHeadline = addHeadlineLabel("securingYourKeysHeadline");
        Label securingYourKeysContent = addContentLabel("securingYourKeysContent");
        Label avoidScamsHeadline = addHeadlineLabel("avoidScamsHeadline");
        Label avoidScamsContent = addContentLabel("avoidScamsContent");
        Hyperlink learnMore = addLearnMoreHyperlink();

        setHeadlineMargin(introContent);
        setHeadlineMargin(securingYourKeysHeadline);
        setHeadlineMargin(avoidScamsHeadline);
        setLastLabelMargin(avoidScamsContent);
    }

    @Override
    protected String getKey() {
        return "security";
    }

    @Override
    protected String getIconId() {
        return "learn-security";
    }


    @Override
    protected String getUrl() {
        return "https://bitcoin.org/en/secure-your-wallet";
    }
}
