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

package bisq.chat;

import bisq.chat.bisqeasy.channel.BisqEasyChatChannelSelectionService;
import bisq.chat.bisqeasy.channel.offerbook.BisqEasyPublicChatChannel;
import bisq.chat.bisqeasy.channel.offerbook.BisqEasyPublicChatChannelService;
import bisq.chat.bisqeasy.channel.open_trades.BisqEasyPrivateTradeChatChannel;
import bisq.chat.bisqeasy.channel.open_trades.BisqEasyPrivateTradeChatChannelService;
import bisq.chat.channel.ChatChannel;
import bisq.chat.channel.ChatChannelDomain;
import bisq.chat.channel.ChatChannelSelectionService;
import bisq.chat.channel.ChatChannelService;
import bisq.chat.channel.priv.PrivateChatChannelService;
import bisq.chat.channel.priv.TwoPartyPrivateChatChannel;
import bisq.chat.channel.priv.TwoPartyPrivateChatChannelService;
import bisq.chat.channel.pub.CommonPublicChatChannel;
import bisq.chat.channel.pub.CommonPublicChatChannelService;
import bisq.chat.notifications.ChatNotificationService;
import bisq.common.application.Service;
import bisq.common.util.CompletableFutureUtils;
import bisq.network.NetworkService;
import bisq.persistence.PersistenceService;
import bisq.presentation.notifications.NotificationsService;
import bisq.security.pow.ProofOfWorkService;
import bisq.settings.SettingsService;
import bisq.user.UserService;
import bisq.user.identity.UserIdentityService;
import bisq.user.profile.UserProfile;
import bisq.user.profile.UserProfileService;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import javax.annotation.Nullable;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

@Slf4j
@Getter
public class ChatService implements Service {
    private final PersistenceService persistenceService;
    private final ProofOfWorkService proofOfWorkService;
    private final NetworkService networkService;
    private final UserService userService;
    private final UserIdentityService userIdentityService;
    private final UserProfileService userProfileService;
    private final ChatNotificationService chatNotificationService;
    private final BisqEasyPublicChatChannelService bisqEasyPublicChatChannelService;
    private final BisqEasyPrivateTradeChatChannelService bisqEasyPrivateTradeChatChannelService;
    private final Map<ChatChannelDomain, CommonPublicChatChannelService> commonPublicChatChannelServices = new HashMap<>();
    private final Map<ChatChannelDomain, TwoPartyPrivateChatChannelService> twoPartyPrivateChatChannelServices = new HashMap<>();
    private final Map<ChatChannelDomain, ChatChannelSelectionService> chatChannelSelectionServices = new HashMap<>();

    public ChatService(PersistenceService persistenceService,
                       ProofOfWorkService proofOfWorkService,
                       NetworkService networkService,
                       UserService userService,
                       SettingsService settingsService,
                       NotificationsService notificationsService) {
        this.persistenceService = persistenceService;
        this.proofOfWorkService = proofOfWorkService;
        this.networkService = networkService;
        this.userService = userService;
        this.userIdentityService = userService.getUserIdentityService();
        this.userProfileService = userService.getUserProfileService();

        chatNotificationService = new ChatNotificationService(this,
                notificationsService,
                settingsService,
                userIdentityService,
                userProfileService);

        //BISQ_EASY
        bisqEasyPublicChatChannelService = new BisqEasyPublicChatChannelService(persistenceService,
                networkService,
                userService);
        bisqEasyPrivateTradeChatChannelService = new BisqEasyPrivateTradeChatChannelService(persistenceService,
                networkService,
                userService,
                proofOfWorkService);
        addToTwoPartyPrivateChatChannelServices(ChatChannelDomain.BISQ_EASY);
        chatChannelSelectionServices.put(ChatChannelDomain.BISQ_EASY, new BisqEasyChatChannelSelectionService(persistenceService,
                twoPartyPrivateChatChannelServices.get(ChatChannelDomain.BISQ_EASY),
                bisqEasyPublicChatChannelService,
                bisqEasyPrivateTradeChatChannelService,
                userIdentityService));

        // DISCUSSION
        addToCommonPublicChatChannelServices(ChatChannelDomain.DISCUSSION,
                List.of(new CommonPublicChatChannel(ChatChannelDomain.DISCUSSION, "bisq"),
                        new CommonPublicChatChannel(ChatChannelDomain.DISCUSSION, "bitcoin"),
                        new CommonPublicChatChannel(ChatChannelDomain.DISCUSSION, "markets"),
                        new CommonPublicChatChannel(ChatChannelDomain.DISCUSSION, "economy"),
                        new CommonPublicChatChannel(ChatChannelDomain.DISCUSSION, "offTopic")));
        addToTwoPartyPrivateChatChannelServices(ChatChannelDomain.DISCUSSION);
        addToChatChannelSelectionServices(ChatChannelDomain.DISCUSSION);

        // EVENTS
        addToCommonPublicChatChannelServices(ChatChannelDomain.EVENTS,
                List.of(new CommonPublicChatChannel(ChatChannelDomain.EVENTS, "conferences"),
                        new CommonPublicChatChannel(ChatChannelDomain.EVENTS, "meetups"),
                        new CommonPublicChatChannel(ChatChannelDomain.EVENTS, "podcasts"),
                        new CommonPublicChatChannel(ChatChannelDomain.EVENTS, "noKyc"),
                        new CommonPublicChatChannel(ChatChannelDomain.EVENTS, "nodes"),
                        new CommonPublicChatChannel(ChatChannelDomain.EVENTS, "tradeEvents")));
        addToTwoPartyPrivateChatChannelServices(ChatChannelDomain.EVENTS);
        addToChatChannelSelectionServices(ChatChannelDomain.EVENTS);

        // SUPPORT
        addToCommonPublicChatChannelServices(ChatChannelDomain.SUPPORT,
                List.of(new CommonPublicChatChannel(ChatChannelDomain.SUPPORT, "support"),
                        new CommonPublicChatChannel(ChatChannelDomain.SUPPORT, "questions"),
                        new CommonPublicChatChannel(ChatChannelDomain.SUPPORT, "reports")));
        addToTwoPartyPrivateChatChannelServices(ChatChannelDomain.SUPPORT);
        addToChatChannelSelectionServices(ChatChannelDomain.SUPPORT);
    }

    @Override
    public CompletableFuture<Boolean> initialize() {
        log.info("initialize");

        List<CompletableFuture<Boolean>> list = new ArrayList<>(List.of(bisqEasyPublicChatChannelService.initialize(),
                bisqEasyPrivateTradeChatChannelService.initialize()));
        list.addAll(commonPublicChatChannelServices.values().stream()
                .map(CommonPublicChatChannelService::initialize)
                .collect(Collectors.toList()));
        list.addAll(twoPartyPrivateChatChannelServices.values().stream()
                .map(PrivateChatChannelService::initialize)
                .collect(Collectors.toList()));
        list.addAll(chatChannelSelectionServices.values().stream()
                .map(ChatChannelSelectionService::initialize)
                .collect(Collectors.toList()));

        list.add(chatNotificationService.initialize());

        return CompletableFutureUtils.allOf(list).thenApply(result -> true);
    }

    @Override
    public CompletableFuture<Boolean> shutdown() {
        log.info("shutdown");
        List<CompletableFuture<Boolean>> list = new ArrayList<>(List.of(bisqEasyPublicChatChannelService.shutdown(),
                bisqEasyPrivateTradeChatChannelService.shutdown()));
        list.addAll(commonPublicChatChannelServices.values().stream()
                .map(CommonPublicChatChannelService::shutdown)
                .collect(Collectors.toList()));
        list.addAll(twoPartyPrivateChatChannelServices.values().stream()
                .map(PrivateChatChannelService::shutdown)
                .collect(Collectors.toList()));
        list.addAll(chatChannelSelectionServices.values().stream()
                .map(ChatChannelSelectionService::shutdown)
                .collect(Collectors.toList()));

        list.add(chatNotificationService.shutdown());

        return CompletableFutureUtils.allOf(list).thenApply(result -> true);
    }

    public Optional<ChatChannelService<?, ?, ?>> findChatChannelService(@Nullable ChatChannel<?> chatChannel) {
        if (chatChannel == null) {
            return Optional.empty();
        }
        if (chatChannel instanceof CommonPublicChatChannel) {
            return Optional.of(commonPublicChatChannelServices.get(chatChannel.getChatChannelDomain()));
        } else if (chatChannel instanceof TwoPartyPrivateChatChannel) {
            return Optional.of(twoPartyPrivateChatChannelServices.get(chatChannel.getChatChannelDomain()));
        } else if (chatChannel instanceof BisqEasyPublicChatChannel) {
            return Optional.of(bisqEasyPublicChatChannelService);
        } else if (chatChannel instanceof BisqEasyPrivateTradeChatChannel) {
            return Optional.of(bisqEasyPrivateTradeChatChannelService);
        } else {
            throw new RuntimeException("Unexpected chatChannel instance. chatChannel=" + chatChannel);
        }
    }

    public void createAndSelectTwoPartyPrivateChatChannel(ChatChannelDomain chatChannelDomain, UserProfile peer) {
        TwoPartyPrivateChatChannelService chatChannelService = twoPartyPrivateChatChannelServices.get(chatChannelDomain);
        chatChannelService.findOrCreateChannel(chatChannelDomain, peer)
                .ifPresent(channel -> getChatChannelSelectionService(chatChannelDomain).selectChannel(channel));
    }

    public ChatChannelSelectionService getChatChannelSelectionService(ChatChannelDomain chatChannelDomain) {
        return chatChannelSelectionServices.get(chatChannelDomain);
    }

    public BisqEasyChatChannelSelectionService getBisqEasyChatChannelSelectionService() {
        return (BisqEasyChatChannelSelectionService) getChatChannelSelectionServices().get(ChatChannelDomain.BISQ_EASY);
    }

    private void addToTwoPartyPrivateChatChannelServices(ChatChannelDomain chatChannelDomain) {
        twoPartyPrivateChatChannelServices.put(chatChannelDomain,
                new TwoPartyPrivateChatChannelService(persistenceService,
                        networkService,
                        userService,
                        proofOfWorkService,
                        chatChannelDomain));
    }

    private void addToCommonPublicChatChannelServices(ChatChannelDomain chatChannelDomain, List<CommonPublicChatChannel> channels) {
        commonPublicChatChannelServices.put(chatChannelDomain,
                new CommonPublicChatChannelService(persistenceService,
                        networkService,
                        userService,
                        chatChannelDomain,
                        channels));
    }

    private void addToChatChannelSelectionServices(ChatChannelDomain chatChannelDomain) {
        chatChannelSelectionServices.put(chatChannelDomain,
                new ChatChannelSelectionService(persistenceService,
                        twoPartyPrivateChatChannelServices.get(chatChannelDomain),
                        commonPublicChatChannelServices.get(chatChannelDomain),
                        chatChannelDomain,
                        userIdentityService));
    }
}