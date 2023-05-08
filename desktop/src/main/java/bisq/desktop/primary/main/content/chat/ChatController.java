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

package bisq.desktop.primary.main.content.chat;

import bisq.application.DefaultApplicationService;
import bisq.chat.channel.ChatChannel;
import bisq.chat.channel.ChatChannelDomain;
import bisq.chat.channel.ChatChannelSelectionService;
import bisq.chat.channel.priv.PrivateChatChannel;
import bisq.chat.channel.priv.TwoPartyPrivateChatChannel;
import bisq.chat.channel.pub.CommonPublicChatChannelService;
import bisq.chat.channel.pub.PublicChatChannel;
import bisq.chat.message.ChatMessage;
import bisq.desktop.common.threading.UIThread;
import bisq.desktop.common.view.Controller;
import bisq.desktop.common.view.NavigationTarget;
import bisq.desktop.primary.main.content.chat.channels.PublicChannelSelection;
import lombok.extern.slf4j.Slf4j;

import java.util.Optional;

@Slf4j
public abstract class ChatController<V extends BaseChatView, M extends BaseChatModel> extends BaseChatController<V, M> implements Controller {
    protected ChatChannelSelectionService chatChannelSelectionService;
    protected CommonPublicChatChannelService commonPublicChatChannelService;
    protected PublicChannelSelection publicChannelSelection;

    public ChatController(DefaultApplicationService applicationService, ChatChannelDomain chatChannelDomain, NavigationTarget host) {
        super(applicationService, chatChannelDomain, host);
    }

    @Override
    public void createDependencies() {
        commonPublicChatChannelService = getPublicChannelService();
        chatChannelSelectionService = getChannelSelectionService();
        publicChannelSelection = getPublicChannelSelection();
    }

    abstract public ChatChannelSelectionService getChannelSelectionService();

    abstract public CommonPublicChatChannelService getPublicChannelService();

    abstract public PublicChannelSelection getPublicChannelSelection();

    @Override
    public void onActivate() {
        super.onActivate();

        selectedChannelPin = chatChannelSelectionService.getSelectedChannel().addObserver(this::handleChannelChange);
    }

    @Override
    protected void handleChannelChange(ChatChannel<? extends ChatMessage> chatChannel) {
        super.handleChannelChange(chatChannel);

        UIThread.run(() -> {
            if (chatChannel == null) {
                return;
            }

            if (chatChannel instanceof TwoPartyPrivateChatChannel) {
                applyPeersIcon((PrivateChatChannel<?>) chatChannel);
                publicChannelSelection.deSelectChannel();
            } else {
                applyDefaultPublicChannelIcon((PublicChatChannel<?>) chatChannel);
                twoPartyPrivateChatChannelSelection.deSelectChannel();
            }
        });
    }

    @Override
    protected Optional<? extends Controller> createController(NavigationTarget navigationTarget) {
        return Optional.empty();
    }
}
