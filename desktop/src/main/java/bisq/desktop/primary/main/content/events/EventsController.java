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

package bisq.desktop.primary.main.content.events;

import bisq.application.DefaultApplicationService;
import bisq.chat.channel.ChatChannelDomain;
import bisq.chat.channel.ChatChannelSelectionService;
import bisq.chat.channel.pub.CommonPublicChatChannelService;
import bisq.desktop.common.view.Controller;
import bisq.desktop.common.view.NavigationTarget;
import bisq.desktop.primary.main.content.chat.ChatController;
import bisq.desktop.primary.main.content.chat.channels.CommonPublicChatChannelSelection;
import bisq.desktop.primary.main.content.chat.channels.PublicChatChannelSelection;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class EventsController extends ChatController<EventsView, EventsModel> implements Controller {
    public EventsController(DefaultApplicationService applicationService) {
        super(applicationService, ChatChannelDomain.EVENTS, NavigationTarget.NONE);
    }

    @Override
    public ChatChannelSelectionService getChannelSelectionService() {
        return chatService.getEventsChatChannelSelectionService();
    }

    @Override
    public CommonPublicChatChannelService getPublicChannelService() {
        return chatService.getEventsPublicChatChannelService();
    }

    @Override
    public PublicChatChannelSelection<?, ?, ?> getPublicChannelSelection() {
        return new CommonPublicChatChannelSelection(applicationService, ChatChannelDomain.EVENTS);
    }


    @Override
    public EventsModel getChatModel(ChatChannelDomain chatChannelDomain) {
        return new EventsModel(chatChannelDomain);
    }

    @Override
    public EventsView getChatView() {
        return new EventsView(model,
                this,
                publicChatChannelSelection.getRoot(),
                twoPartyPrivateChatChannelSelection.getRoot(),
                chatMessagesComponent.getRoot(),
                channelSidebar.getRoot());
    }
}
