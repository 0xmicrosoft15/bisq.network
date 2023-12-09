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

package bisq.desktop.main.content.common_chat;

import bisq.bisq_easy.NavigationTarget;
import bisq.chat.*;
import bisq.chat.common.CommonPublicChatChannel;
import bisq.chat.common.CommonPublicChatChannelService;
import bisq.chat.notifications.ChatNotification;
import bisq.chat.notifications.ChatNotificationService;
import bisq.common.observable.Pin;
import bisq.desktop.ServiceProvider;
import bisq.desktop.common.observable.FxBindings;
import bisq.desktop.common.threading.UIThread;
import bisq.desktop.common.view.Controller;
import bisq.desktop.main.content.ContentTabController;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import java.util.Objects;
import java.util.Optional;

@Slf4j
public class ChatContainerController extends ContentTabController<ChatContainerModel> {
    protected final ChatService chatService;
    @Getter
    private final ChatContainerView view;
    private final ChatNotificationService chatNotificationService;
    protected final ChatChannelDomain channelDomain;
    private final CommonPublicChatChannelService chatChannelService;
    private final ChatChannelSelectionService chatChannelSelectionService;
    private Pin selectedChannelPin;
    private Pin changedChatNotificationPin;

    public ChatContainerController(ServiceProvider serviceProvider, ChatChannelDomain chatChannelDomain, NavigationTarget navigationTarget) {
        super(new ChatContainerModel(chatChannelDomain), navigationTarget, serviceProvider);

        chatService = serviceProvider.getChatService();
        chatNotificationService = serviceProvider.getChatService().getChatNotificationService();
        channelDomain = chatChannelDomain;
        chatChannelService = chatService.getCommonPublicChatChannelServices().get(chatChannelDomain);
                //chatService.getTwoPartyPrivateChatChannelServices().get(chatChannelDomain);
        chatChannelSelectionService = chatService.getChatChannelSelectionServices().get(chatChannelDomain);

        createChannels();
        view = new ChatContainerView(model, this);
    }

    private void createChannels() {
        chatChannelService.getChannels().forEach(commonPublicChatChannel -> {
            Channel channel = findOrCreateChannelItem(commonPublicChatChannel);
            if (channel != null) {
                model.channels.put(channel.getChannelId(), channel);
            }
        });
    }

    private Channel findOrCreateChannelItem(ChatChannel<? extends ChatMessage> chatChannel) {
        if (chatChannel instanceof CommonPublicChatChannel) {
            CommonPublicChatChannel commonChannel = (CommonPublicChatChannel) chatChannel;
            if (model.channels.containsKey(chatChannel.getId())) {
                return model.channels.get(chatChannel.getId());
            } else {
                String targetName = channelDomain.toString() + "_" + commonChannel.getChannelTitle().toUpperCase();
                try {
                    return new Channel(commonChannel, chatChannelService, NavigationTarget.valueOf(targetName));
                } catch (IllegalArgumentException e) {
                    log.info("Couldn't find navigation target " + targetName + " in channel domain " + channelDomain);
                }
            }
        }
        return null;
    }


    @Override
    public void onActivate() {
        super.onActivate();

        selectedChannelPin = FxBindings.subscribe(chatChannelSelectionService.getSelectedChannel(),
                channel -> UIThread.run(() -> handleSelectedChannelChange(channel)));
        chatNotificationService.getNotConsumedNotifications().forEach(this::handleNotification);
        changedChatNotificationPin = chatNotificationService.getChangedNotification().addObserver(this::handleNotification);
    }

    private void handleNotification(ChatNotification notification) {
        if (notification == null || notification.getChatChannelDomain() != channelDomain
                || !model.channels.containsKey(notification.getChatChannelId())) {
            return;
        }

        UIThread.run(() ->
            updateTabButtonNotifications(
                    notification.getChatChannelId(),
                    chatNotificationService.getNumNotifications(notification.getChatChannelId()))
        );
    }

    private void updateTabButtonNotifications(String channelId, long newCount) {
        if (!model.channels.containsKey(channelId)) {
            return;
        }

        Channel channel = model.channels.get(channelId);
        model.getTabButtons().stream()
                .filter(tabButton -> channel.getNavigationTarget() == tabButton.getNavigationTarget())
                .findAny()
                .ifPresent(tabButton -> tabButton.setNumNotifications(newCount));
    }

    @Override
    public void onDeactivate() {
        super.onDeactivate();

        selectedChannelPin.unbind();
        changedChatNotificationPin.unbind();
    }

    protected void handleSelectedChannelChange(ChatChannel<? extends ChatMessage> chatChannel) {
        Channel channel = findOrCreateChannelItem(chatChannel);
        if (channel != null) {
            model.selectedChannel.set(channel);

            if (model.previousSelectedChannel != null) {
                model.previousSelectedChannel.setSelected(false);
            }
            model.previousSelectedChannel = channel;

            channel.setSelected(true);
        }
    }

    protected void onSelected(NavigationTarget navigationTarget) {
        Optional<Channel> channel = model.channels.values().stream()
                .filter(Objects::nonNull)
                .filter(item -> item.getNavigationTarget().equals(navigationTarget))
                .findFirst();
        chatChannelSelectionService.selectChannel(
                channel.<ChatChannel<? extends ChatMessage>>map(Channel::getChatChannel).orElse(null));
    }

    @Override
    protected Optional<? extends Controller> createController(NavigationTarget navigationTarget) {
        switch (navigationTarget) {
            case DISCUSSION_BISQ:
            case DISCUSSION_BITCOIN:
            case DISCUSSION_MARKETS:
            case DISCUSSION_ECONOMY:
            case DISCUSSION_OFFTOPIC:
            case EVENTS_CONFERENCES:
            case EVENTS_MEETUPS:
            case EVENTS_PODCASTS:
            case EVENTS_NOKYC:
            case EVENTS_NODES:
            case EVENTS_TRADEEVENTS:
            case SUPPORT_SUPPORT:
            case SUPPORT_QUESTIONS:
            case SUPPORT_REPORTS:{
                return Optional.of(new CommonChatController(serviceProvider, channelDomain, navigationTarget));
            }
            case DISCUSSION_PRIVATECHATS:
            case EVENTS_PRIVATECHATS:
            case SUPPORT_PRIVATECHATS: {
                //return Optional.empty(); TODO: Make new class for private chats
            }
            default: {
                return Optional.empty();
            }
        }
    }
}
