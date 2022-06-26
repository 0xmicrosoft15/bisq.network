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

package bisq.desktop.primary.main.content.settings.reputation;

import bisq.application.DefaultApplicationService;
import bisq.common.observable.Pin;
import bisq.desktop.common.observable.FxBindings;
import bisq.desktop.common.view.Controller;
import bisq.desktop.primary.main.content.components.UserProfileSelection;
import bisq.social.chat.ChatService;
import bisq.social.user.ChatUserService;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class ManageReputationController implements Controller {

    private final ManageReputationModel model;
    @Getter
    private final ManageReputationView view;
    private final ChatUserService chatUserService;
    private final UserProfileSelection userProfileSelection;
    private final ChatService chatService;
    private final DefaultApplicationService applicationService;
    private Pin selectedUserProfilePin;

    public ManageReputationController(DefaultApplicationService applicationService) {
        chatUserService = applicationService.getChatUserService();
        chatService = applicationService.getChatService();
        this.applicationService = applicationService;
        userProfileSelection = new UserProfileSelection(chatUserService);

        model = new ManageReputationModel();
        view = new ManageReputationView(model, this, userProfileSelection);
    }

    @Override
    public void onActivate() {
        selectedUserProfilePin = FxBindings.subscribe(chatUserService.getSelectedChatUserIdentity(),
                chatUserIdentity -> model.getSelectedChatUserIdentity().set(chatUserIdentity)
        );
    }

    @Override
    public void onDeactivate() {
    }
}
