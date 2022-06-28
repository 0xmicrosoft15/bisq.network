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

package bisq.desktop.primary.main.content.settings.userProfile.create.step2;

import bisq.application.DefaultApplicationService;
import bisq.desktop.common.view.Controller;
import bisq.desktop.primary.overlay.OverlayController;
import bisq.social.user.ChatUserService;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class GenerateNewProfileStep2Controller implements Controller {
    @Getter
    private final GenerateNewProfileStep2View view;

    public GenerateNewProfileStep2Controller(DefaultApplicationService applicationService) {
        ChatUserService chatUserService = applicationService.getChatUserService();

        GenerateNewProfileStep2Model model = new GenerateNewProfileStep2Model();
        view = new GenerateNewProfileStep2View(model, this);
    }

    @Override
    public void onActivate() {
    }

    @Override
    public void onDeactivate() {
    }

    private void onSave() {
        OverlayController.hide();
    }
}
