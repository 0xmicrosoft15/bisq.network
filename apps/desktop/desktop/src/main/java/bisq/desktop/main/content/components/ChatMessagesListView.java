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

package bisq.desktop.main.content.components;

import bisq.bisq_easy.NavigationTarget;
import bisq.chat.*;
import bisq.chat.bisqeasy.BisqEasyOfferMessage;
import bisq.chat.bisqeasy.offerbook.BisqEasyOfferbookChannel;
import bisq.chat.bisqeasy.offerbook.BisqEasyOfferbookMessage;
import bisq.chat.bisqeasy.open_trades.BisqEasyOpenTradeChannel;
import bisq.chat.common.CommonPublicChatChannel;
import bisq.chat.common.CommonPublicChatChannelService;
import bisq.chat.common.CommonPublicChatMessage;
import bisq.chat.notifications.ChatNotificationService;
import bisq.chat.priv.PrivateChatMessage;
import bisq.chat.pub.PublicChatChannel;
import bisq.chat.pub.PublicChatMessage;
import bisq.chat.two_party.TwoPartyPrivateChatChannel;
import bisq.common.locale.LanguageRepository;
import bisq.common.observable.Observable;
import bisq.common.observable.Pin;
import bisq.common.observable.collection.CollectionObserver;
import bisq.common.observable.map.HashMapObserver;
import bisq.common.util.StringUtils;
import bisq.desktop.ServiceProvider;
import bisq.desktop.common.Icons;
import bisq.desktop.common.Transitions;
import bisq.desktop.common.observable.FxBindings;
import bisq.desktop.common.threading.UIScheduler;
import bisq.desktop.common.threading.UIThread;
import bisq.desktop.common.utils.ClipboardUtil;
import bisq.desktop.common.view.Navigation;
import bisq.desktop.components.containers.Spacer;
import bisq.desktop.components.controls.*;
import bisq.desktop.components.list_view.ListViewUtil;
import bisq.desktop.components.list_view.NoSelectionModel;
import bisq.desktop.components.overlay.Popup;
import bisq.desktop.main.content.bisq_easy.take_offer.TakeOfferController;
import bisq.desktop.main.content.chat.ChatUtil;
import bisq.i18n.Res;
import bisq.network.NetworkService;
import bisq.network.identity.NetworkId;
import bisq.network.p2p.services.confidential.ack.MessageDeliveryStatus;
import bisq.offer.bisq_easy.BisqEasyOffer;
import bisq.presentation.formatters.DateFormatter;
import bisq.settings.SettingsService;
import bisq.trade.Trade;
import bisq.trade.bisq_easy.BisqEasyTradeService;
import bisq.user.banned.BannedUserService;
import bisq.user.identity.UserIdentity;
import bisq.user.identity.UserIdentityService;
import bisq.user.profile.UserProfile;
import bisq.user.profile.UserProfileService;
import bisq.user.reputation.ReputationScore;
import bisq.user.reputation.ReputationService;
import com.google.common.base.Joiner;
import com.sun.javafx.scene.control.VirtualScrollBar;
import de.jensd.fx.fontawesome.AwesomeDude;
import de.jensd.fx.fontawesome.AwesomeIcon;
import javafx.animation.Interpolator;
import javafx.animation.KeyFrame;
import javafx.animation.KeyValue;
import javafx.animation.Timeline;
import javafx.beans.property.*;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.collections.transformation.SortedList;
import javafx.geometry.Insets;
import javafx.geometry.Orientation;
import javafx.geometry.Pos;
import javafx.scene.Cursor;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.image.ImageView;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.*;
import javafx.util.Callback;
import javafx.util.Duration;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.fxmisc.easybind.EasyBind;
import org.fxmisc.easybind.Subscription;

import java.text.DateFormat;
import java.util.*;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import static bisq.desktop.main.content.components.ChatMessagesComponent.View.EDITED_POST_FIX;
import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;

@Slf4j
public class ChatMessagesListView {
    private final Controller controller;

    public ChatMessagesListView(ServiceProvider serviceProvider,
                                Consumer<UserProfile> mentionUserHandler,
                                Consumer<ChatMessage> showChatUserDetailsHandler,
                                Consumer<ChatMessage> replyHandler,
                                ChatChannelDomain chatChannelDomain) {
        controller = new Controller(serviceProvider,
                mentionUserHandler,
                showChatUserDetailsHandler,
                replyHandler,
                chatChannelDomain);
    }

    public Pane getRoot() {
        return controller.view.getRoot();
    }

    public void setSearchPredicate(Predicate<? super ChatMessagesListView.ChatMessageListItem<? extends ChatMessage>> predicate) {
        controller.setSearchPredicate(predicate);
    }

    public void refreshMessages() {
        controller.refreshMessages();
    }

    private static class Controller implements bisq.desktop.common.view.Controller {
        private final ChatService chatService;
        private final UserIdentityService userIdentityService;
        private final UserProfileService userProfileService;
        private final ReputationService reputationService;
        private final SettingsService settingsService;
        private final Consumer<UserProfile> mentionUserHandler;
        private final Consumer<ChatMessage> replyHandler;
        private final Consumer<ChatMessage> showChatUserDetailsHandler;
        private final Model model;
        @Getter
        private final View view;
        private final ChatNotificationService chatNotificationService;
        private final BisqEasyTradeService bisqEasyTradeService;
        private final BannedUserService bannedUserService;
        private final NetworkService networkService;
        private Pin selectedChannelPin, chatMessagesPin, offerOnlySettingsPin;
        private Subscription selectedChannelSubscription, focusSubscription, scrollValuePin, scrollBarVisiblePin;

        private Controller(ServiceProvider serviceProvider,
                           Consumer<UserProfile> mentionUserHandler,
                           Consumer<ChatMessage> showChatUserDetailsHandler,
                           Consumer<ChatMessage> replyHandler,
                           ChatChannelDomain chatChannelDomain) {
            chatService = serviceProvider.getChatService();
            chatNotificationService = chatService.getChatNotificationService();
            userIdentityService = serviceProvider.getUserService().getUserIdentityService();
            userProfileService = serviceProvider.getUserService().getUserProfileService();
            reputationService = serviceProvider.getUserService().getReputationService();
            settingsService = serviceProvider.getSettingsService();
            bisqEasyTradeService = serviceProvider.getTradeService().getBisqEasyTradeService();
            bannedUserService = serviceProvider.getUserService().getBannedUserService();
            networkService = serviceProvider.getNetworkService();
            this.mentionUserHandler = mentionUserHandler;
            this.showChatUserDetailsHandler = showChatUserDetailsHandler;
            this.replyHandler = replyHandler;

            model = new Model(userIdentityService, chatChannelDomain);
            view = new View(model, this);
        }

        @Override
        public void onActivate() {
            model.getSortedChatMessages().setComparator(ChatMessagesListView.ChatMessageListItem::compareTo);

            offerOnlySettingsPin = FxBindings.subscribe(settingsService.getOffersOnly(), offerOnly -> UIThread.run(this::applyPredicate));

            if (selectedChannelPin != null) {
                selectedChannelPin.unbind();
            }

            ChatChannelSelectionService selectionService = chatService.getChatChannelSelectionServices().get(model.getChatChannelDomain());

            selectedChannelPin = selectionService.getSelectedChannel().addObserver(this::selectedChannelChanged);

            scrollValuePin = EasyBind.subscribe(model.getScrollValue(), scrollValue -> {
                if (scrollValue != null) {
                    applyScrollValue(scrollValue.doubleValue());
                }
            });

            scrollBarVisiblePin = EasyBind.subscribe(model.scrollBarVisible, scrollBarVisible -> {
                if (scrollBarVisible != null && scrollBarVisible) {
                    applyScrollValue(1);
                    view.listView.setPadding(View.LISTVIEW_PADDING_WITH_SCROLLBAR);
                } else {
                    view.listView.setPadding(new Insets(0));
                }
            });

            applyScrollValue(1);
        }

        @Override
        public void onDeactivate() {
            if (offerOnlySettingsPin != null) {
                offerOnlySettingsPin.unbind();
            }
            if (selectedChannelPin != null) {
                selectedChannelPin.unbind();
                selectedChannelPin = null;
            }
            if (chatMessagesPin != null) {
                chatMessagesPin.unbind();
                chatMessagesPin = null;
            }
            if (focusSubscription != null) {
                focusSubscription.unsubscribe();
            }
            if (selectedChannelSubscription != null) {
                selectedChannelSubscription.unsubscribe();
            }

            scrollValuePin.unsubscribe();
            scrollBarVisiblePin.unsubscribe();

            model.chatMessages.forEach(ChatMessageListItem::dispose);
            model.chatMessages.clear();
        }

        private void selectedChannelChanged(ChatChannel<? extends ChatMessage> channel) {
            UIThread.run(() -> {
                model.selectedChannel.set(channel);
                model.isPublicChannel.set(channel instanceof PublicChatChannel);

                if (chatMessagesPin != null) {
                    chatMessagesPin.unbind();
                }

                // Clear and call dispose on the current messages when we change the channel.
                model.chatMessages.forEach(ChatMessageListItem::dispose);
                model.chatMessages.clear();

                if (channel instanceof BisqEasyOfferbookChannel) {
                    chatMessagesPin = bindChatMessages((BisqEasyOfferbookChannel) channel);
                } else if (channel instanceof BisqEasyOpenTradeChannel) {
                    chatMessagesPin = bindChatMessages((BisqEasyOpenTradeChannel) channel);
                } else if (channel instanceof CommonPublicChatChannel) {
                    chatMessagesPin = bindChatMessages((CommonPublicChatChannel) channel);
                } else if (channel instanceof TwoPartyPrivateChatChannel) {
                    chatMessagesPin = bindChatMessages((TwoPartyPrivateChatChannel) channel);
                }

                if (focusSubscription != null) {
                    focusSubscription.unsubscribe();
                }
                if (selectedChannelSubscription != null) {
                    selectedChannelSubscription.unsubscribe();
                }
                if (channel != null) {
                    Scene scene = view.getRoot().getScene();
                    if (scene != null) {
                        focusSubscription = EasyBind.subscribe(scene.getWindow().focusedProperty(),
                                focused -> {
                                    if (focused && model.getSelectedChannel().get() != null) {
                                        chatNotificationService.consume(model.getSelectedChannel().get().getId());
                                    }
                                });
                    }

                    selectedChannelSubscription = EasyBind.subscribe(model.selectedChannel,
                            selectedChannel -> {
                                if (selectedChannel != null) {
                                    chatNotificationService.consume(model.getSelectedChannel().get().getId());
                                }
                            });
                }
            });
        }


        ///////////////////////////////////////////////////////////////////////////////////////////////////
        // API - called from client
        ///////////////////////////////////////////////////////////////////////////////////////////////////

        private void refreshMessages() {
            model.chatMessages.setAll(new ArrayList<>(model.chatMessages));
        }

        private void setSearchPredicate(Predicate<? super ChatMessagesListView.ChatMessageListItem<? extends ChatMessage>> predicate) {
            model.setSearchPredicate(Objects.requireNonNullElseGet(predicate, () -> e -> true));
            applyPredicate();
        }


        ///////////////////////////////////////////////////////////////////////////////////////////////////
        // UI - delegate to client
        ///////////////////////////////////////////////////////////////////////////////////////////////////

        private void onMention(UserProfile userProfile) {
            mentionUserHandler.accept(userProfile);
        }

        private void onShowChatUserDetails(ChatMessage chatMessage) {
            showChatUserDetailsHandler.accept(chatMessage);
        }

        private void onReply(ChatMessage chatMessage) {
            replyHandler.accept(chatMessage);
        }


        ///////////////////////////////////////////////////////////////////////////////////////////////////
        // UI - handler
        ///////////////////////////////////////////////////////////////////////////////////////////////////

        private void onTakeOffer(BisqEasyOfferbookMessage chatMessage, boolean canTakeOffer) {
            if (userIdentityService.getSelectedUserIdentity() == null ||
                    bannedUserService.isUserProfileBanned(chatMessage.getAuthorUserProfileId()) ||
                    bannedUserService.isUserProfileBanned(userIdentityService.getSelectedUserIdentity().getUserProfile())) {
                return;
            }

            if (!canTakeOffer) {
                new Popup().information(Res.get("chat.message.offer.offerAlreadyTaken.warn")).show();
                return;
            }
            checkArgument(!model.isMyMessage(chatMessage), "tradeChatMessage must not be mine");
            checkArgument(chatMessage.getBisqEasyOffer().isPresent(), "message must contain offer");

            BisqEasyOffer bisqEasyOffer = chatMessage.getBisqEasyOffer().get();
            Navigation.navigateTo(NavigationTarget.TAKE_OFFER, new TakeOfferController.InitData(bisqEasyOffer));
        }

        private void onDeleteMessage(ChatMessage chatMessage) {
            String authorUserProfileId = chatMessage.getAuthorUserProfileId();
            userIdentityService.findUserIdentity(authorUserProfileId)
                    .ifPresent(authorUserIdentity -> {
                        if (authorUserIdentity.equals(userIdentityService.getSelectedUserIdentity())) {
                            doDeleteMessage(chatMessage, authorUserIdentity);
                        } else {
                            new Popup().warning(Res.get("chat.message.delete.differentUserProfile.warn"))
                                    .closeButtonText(Res.get("confirmation.no"))
                                    .actionButtonText(Res.get("confirmation.yes"))
                                    .onAction(() -> {
                                        userIdentityService.selectChatUserIdentity(authorUserIdentity);
                                        doDeleteMessage(chatMessage, authorUserIdentity);
                                    })
                                    .show();
                        }
                    });
        }

        private void doDeleteMessage(ChatMessage chatMessage, UserIdentity userIdentity) {
            checkArgument(chatMessage instanceof PublicChatMessage);

            if (chatMessage instanceof BisqEasyOfferbookMessage) {
                BisqEasyOfferbookMessage bisqEasyOfferbookMessage = (BisqEasyOfferbookMessage) chatMessage;
                chatService.getBisqEasyOfferbookChannelService().deleteChatMessage(bisqEasyOfferbookMessage, userIdentity.getNetworkIdWithKeyPair())
                        .whenComplete((result, throwable) -> {
                            if (throwable != null) {
                                log.error("We got an error at doDeleteMessage: " + throwable);
                            }
                        });
            } else if (chatMessage instanceof CommonPublicChatMessage) {
                CommonPublicChatChannelService commonPublicChatChannelService = chatService.getCommonPublicChatChannelServices().get(model.chatChannelDomain);
                CommonPublicChatMessage commonPublicChatMessage = (CommonPublicChatMessage) chatMessage;
                commonPublicChatChannelService.findChannel(chatMessage)
                        .ifPresent(channel -> commonPublicChatChannelService.deleteChatMessage(commonPublicChatMessage, userIdentity.getNetworkIdWithKeyPair()));
            }
        }

        private void onOpenPrivateChannel(ChatMessage chatMessage) {
            checkArgument(!model.isMyMessage(chatMessage));

            userProfileService.findUserProfile(chatMessage.getAuthorUserProfileId())
                    .ifPresent(this::createAndSelectTwoPartyPrivateChatChannel);
        }

        private void onSaveEditedMessage(ChatMessage chatMessage, String editedText) {
            checkArgument(chatMessage instanceof PublicChatMessage);
            checkArgument(model.isMyMessage(chatMessage));

            if (editedText.length() > ChatMessage.MAX_TEXT_LENGTH) {
                new Popup().warning(Res.get("validation.tooLong", ChatMessage.MAX_TEXT_LENGTH)).show();
                return;
            }

            UserIdentity userIdentity = checkNotNull(userIdentityService.getSelectedUserIdentity());
            if (chatMessage instanceof BisqEasyOfferbookMessage) {
                BisqEasyOfferbookMessage bisqEasyOfferbookMessage = (BisqEasyOfferbookMessage) chatMessage;
                chatService.getBisqEasyOfferbookChannelService().publishEditedChatMessage(bisqEasyOfferbookMessage, editedText, userIdentity);
            } else if (chatMessage instanceof CommonPublicChatMessage) {
                CommonPublicChatMessage commonPublicChatMessage = (CommonPublicChatMessage) chatMessage;
                chatService.getCommonPublicChatChannelServices().get(model.chatChannelDomain).publishEditedChatMessage(commonPublicChatMessage, editedText, userIdentity);
            }
        }

        private void onOpenMoreOptions(Node owner, ChatMessage chatMessage, Runnable onClose) {
            if (chatMessage.equals(model.selectedChatMessageForMoreOptionsPopup.get())) {
                return;
            }
            model.selectedChatMessageForMoreOptionsPopup.set(chatMessage);

            List<BisqPopupMenuItem> items = new ArrayList<>();
            items.add(new BisqPopupMenuItem(Res.get("chat.message.contextMenu.copyMessage"),
                    () -> onCopyMessage(chatMessage)));
            if (!model.isMyMessage(chatMessage)) {
                if (chatMessage instanceof PublicChatMessage) {
                    items.add(new BisqPopupMenuItem(Res.get("chat.message.contextMenu.ignoreUser"),
                            () -> onIgnoreUser(chatMessage)));
                }
                items.add(new BisqPopupMenuItem(Res.get("chat.message.contextMenu.reportUser"),
                        () -> onReportUser(chatMessage)));
            }

            BisqPopupMenu menu = new BisqPopupMenu(items, onClose);
            menu.setAlignment(BisqPopup.Alignment.LEFT);
            menu.show(owner);
        }

        private void onReportUser(ChatMessage chatMessage) {
            ChatChannelDomain chatChannelDomain = model.getSelectedChannel().get().getChatChannelDomain();
            if (chatMessage instanceof PrivateChatMessage) {
                PrivateChatMessage privateChatMessage = (PrivateChatMessage) chatMessage;
                Navigation.navigateTo(NavigationTarget.REPORT_TO_MODERATOR,
                        new ReportToModeratorWindow.InitData(privateChatMessage.getSenderUserProfile(), chatChannelDomain));
            } else {
                userProfileService.findUserProfile(chatMessage.getAuthorUserProfileId())
                        .ifPresent(accusedUserProfile -> Navigation.navigateTo(NavigationTarget.REPORT_TO_MODERATOR,
                                new ReportToModeratorWindow.InitData(accusedUserProfile, chatChannelDomain)));
            }
        }

        private void onIgnoreUser(ChatMessage chatMessage) {
            userProfileService.findUserProfile(chatMessage.getAuthorUserProfileId())
                    .ifPresent(userProfileService::ignoreUserProfile);
        }

        private void onCopyMessage(ChatMessage chatMessage) {
            ClipboardUtil.copyToClipboard(chatMessage.getText());
        }


        ///////////////////////////////////////////////////////////////////////////////////////////////////
        // Scrolling
        ///////////////////////////////////////////////////////////////////////////////////////////////////

        private void applyScrollValue(double scrollValue) {
            model.scrollValue.set(scrollValue);
            model.hasUnreadMessages.set(model.numReadMessages < model.getChatMessages().size());
            boolean isAtBottom = scrollValue == 1d;
            model.showScrolledDownButton.set(!isAtBottom && model.scrollBarVisible.get());
            model.autoScrollToBottom = isAtBottom;
            if (isAtBottom) {
                model.numReadMessages = model.getChatMessages().size();
            }

            int numUnReadMessages = model.getChatMessages().size() - model.numReadMessages;
            model.numUnReadMessages.set(numUnReadMessages > 0 ? String.valueOf(numUnReadMessages) : "");
        }

        private void maybeScrollDownOnNewItemAdded() {
            if (model.autoScrollToBottom) {
                // The 100 ms delay is needed as when the item gets added to the listview it updates the scroll property
                // to a value < 1. After the render process is done we set it to 1.
                UIScheduler.run(() -> applyScrollValue(1)).after(100);
            } else {
                applyScrollValue(model.scrollValue.get());
            }
        }

        void onScrollToBottom() {
            applyScrollValue(1);
        }

        ///////////////////////////////////////////////////////////////////////////////////////////////////
        // Private
        ///////////////////////////////////////////////////////////////////////////////////////////////////

        private void createAndSelectTwoPartyPrivateChatChannel(UserProfile peer) {
            chatService.createAndSelectTwoPartyPrivateChatChannel(model.getChatChannelDomain(), peer)
                    .ifPresent(channel -> {
                        if (model.getChatChannelDomain() == ChatChannelDomain.BISQ_EASY_OFFERBOOK) {
                            Navigation.navigateTo(NavigationTarget.BISQ_EASY_PRIVATE_CHAT);
                        }
                        if (model.getChatChannelDomain() == ChatChannelDomain.DISCUSSION) {
                            Navigation.navigateTo(NavigationTarget.DISCUSSION_PRIVATECHATS);
                        }
                        if (model.getChatChannelDomain() == ChatChannelDomain.EVENTS) {
                            Navigation.navigateTo(NavigationTarget.EVENTS_PRIVATECHATS);
                        }
                        if (model.getChatChannelDomain() == ChatChannelDomain.SUPPORT) {
                            Navigation.navigateTo(NavigationTarget.SUPPORT_PRIVATECHATS);
                        }
                    });
        }

        private void applyPredicate() {
            boolean offerOnly = settingsService.getOffersOnly().get();
            Predicate<ChatMessageListItem<? extends ChatMessage>> predicate = item -> {
                Optional<UserProfile> senderUserProfile = item.getSenderUserProfile();
                if (senderUserProfile.isEmpty()) {
                    return false;
                }
                if (bannedUserService.isUserProfileBanned(item.getChatMessage().getAuthorUserProfileId()) ||
                        bannedUserService.isUserProfileBanned(senderUserProfile.get())) {
                    return false;
                }

                boolean offerOnlyPredicate = true;
                if (item.getChatMessage() instanceof BisqEasyOfferbookMessage) {
                    BisqEasyOfferbookMessage bisqEasyOfferbookMessage = (BisqEasyOfferbookMessage) item.getChatMessage();
                    offerOnlyPredicate = !offerOnly || bisqEasyOfferbookMessage.hasBisqEasyOffer();
                }
                // We do not display the take offer message as it has no text and is used only for sending the offer 
                // to the peer and signalling the take offer event.
                if (item.getChatMessage().getChatMessageType() == ChatMessageType.TAKE_BISQ_EASY_OFFER) {
                    return false;
                }

                return offerOnlyPredicate &&
                        !userProfileService.getIgnoredUserProfileIds().contains(senderUserProfile.get().getId()) &&
                        userProfileService.findUserProfile(senderUserProfile.get().getId()).isPresent();
            };
            model.filteredChatMessages.setPredicate(item -> model.getSearchPredicate().test(item) && predicate.test(item));
        }

        private <M extends ChatMessage, C extends ChatChannel<M>> Pin bindChatMessages(C channel) {
            // We clear and fill the list at channel change. The addObserver triggers the add method for each item,
            // but as we have a contains() check there it will not have any effect.
            model.chatMessages.clear();
            model.chatMessages.addAll(channel.getChatMessages().stream().map(chatMessage -> new ChatMessageListItem<>(chatMessage, userProfileService, reputationService,
                            bisqEasyTradeService, userIdentityService, networkService))
                    .collect(Collectors.toSet()));
            maybeScrollDownOnNewItemAdded();

            return channel.getChatMessages().addObserver(new CollectionObserver<>() {
                @Override
                public void add(M chatMessage) {
                    // TODO Delaying to the next render frame can cause duplicated items in case we get the channel
                    //  change called 2 times in short interval (should be avoid as well).
                    // @namloan Could you re-test the performance issues with testing if using UIThread.run makes a difference?
                    // There have been many changes in the meantime, so maybe the performance issue was fixed by other changes.
                    UIThread.runOnNextRenderFrame(() -> {
                        ChatMessageListItem<M> item = new ChatMessageListItem<>(chatMessage, userProfileService, reputationService,
                                bisqEasyTradeService, userIdentityService, networkService);
                        // As long as we use runOnNextRenderFrame we need to check to avoid adding duplicates
                        if (!model.chatMessages.contains(item)) {
                            model.chatMessages.add(item);
                            maybeScrollDownOnNewItemAdded();
                        }
                    });
                }

                @Override
                public void remove(Object element) {
                    if (element instanceof ChatMessage) {
                        UIThread.runOnNextRenderFrame(() -> {
                            ChatMessage chatMessage = (ChatMessage) element;
                            Optional<ChatMessageListItem<? extends ChatMessage>> toRemove = model.chatMessages.stream()
                                    .filter(item -> item.getChatMessage().getId().equals(chatMessage.getId()))
                                    .findAny();
                            toRemove.ifPresent(item -> {
                                item.dispose();
                                model.chatMessages.remove(item);
                            });
                        });
                    }
                }

                @Override
                public void clear() {
                    UIThread.runOnNextRenderFrame(() -> {
                        model.chatMessages.forEach(ChatMessageListItem::dispose);
                        model.chatMessages.clear();
                    });
                }
            });
        }

        private String getUserName(String userProfileId) {
            return userProfileService.findUserProfile(userProfileId)
                    .map(UserProfile::getUserName)
                    .orElse(Res.get("data.na"));
        }

        private String getSupportedLanguageCodes(BisqEasyOfferbookMessage chatMessage) {
            String result = getSupportedLanguageCodes(chatMessage, ", ", LanguageRepository::getDisplayLanguage);
            return result.isEmpty() ? "" : Res.get("chat.message.supportedLanguages") + " " + StringUtils.truncate(result, 100);
        }

        private String getSupportedLanguageCodesForTooltip(BisqEasyOfferbookMessage chatMessage) {
            String result = getSupportedLanguageCodes(chatMessage, "\n", LanguageRepository::getDisplayString);
            return result.isEmpty() ? "" : Res.get("chat.message.supportedLanguages") + "\n" + result;
        }

        private String getSupportedLanguageCodes(BisqEasyOfferbookMessage chatMessage, String separator, Function<String, String> toStringFunction) {
            return chatMessage.getBisqEasyOffer()
                    .map(BisqEasyOffer::getSupportedLanguageCodes)
                    .map(supportedLanguageCodes -> Joiner.on(separator)
                            .join(supportedLanguageCodes.stream()
                                    .map(toStringFunction)
                                    .collect(Collectors.toList())))
                    .orElse("");
        }
    }

    @Getter
    private static class Model implements bisq.desktop.common.view.Model {
        private final UserIdentityService userIdentityService;
        private final ObjectProperty<ChatChannel<?>> selectedChannel = new SimpleObjectProperty<>();
        private final ObservableList<ChatMessageListItem<? extends ChatMessage>> chatMessages = FXCollections.observableArrayList();
        private final FilteredList<ChatMessageListItem<? extends ChatMessage>> filteredChatMessages = new FilteredList<>(chatMessages);
        private final SortedList<ChatMessageListItem<? extends ChatMessage>> sortedChatMessages = new SortedList<>(filteredChatMessages);
        private final BooleanProperty isPublicChannel = new SimpleBooleanProperty();
        private final ObjectProperty<ChatMessage> selectedChatMessageForMoreOptionsPopup = new SimpleObjectProperty<>(null);
        private final ChatChannelDomain chatChannelDomain;
        @Setter
        private Predicate<? super ChatMessageListItem<? extends ChatMessage>> searchPredicate = e -> true;

        private boolean autoScrollToBottom;
        private int numReadMessages;
        private final BooleanProperty hasUnreadMessages = new SimpleBooleanProperty();
        private final StringProperty numUnReadMessages = new SimpleStringProperty();
        private final BooleanProperty showScrolledDownButton = new SimpleBooleanProperty();
        private final BooleanProperty scrollBarVisible = new SimpleBooleanProperty();
        private final DoubleProperty scrollValue = new SimpleDoubleProperty();

        private Model(UserIdentityService userIdentityService,
                      ChatChannelDomain chatChannelDomain) {
            this.userIdentityService = userIdentityService;
            this.chatChannelDomain = chatChannelDomain;
        }

        boolean isMyMessage(ChatMessage chatMessage) {
            return chatMessage.isMyMessage(userIdentityService);
        }

        boolean hasTradeChatOffer(ChatMessage chatMessage) {
            return chatMessage instanceof BisqEasyOfferMessage &&
                    ((BisqEasyOfferMessage) chatMessage).hasBisqEasyOffer();
        }
    }


    @Slf4j
    private static class View extends bisq.desktop.common.view.View<StackPane, Model, Controller> {
        public static final Insets LISTVIEW_PADDING_WITH_SCROLLBAR = new Insets(0, 0, 0, 15);
        private static final String EDITED_POST_FIX = " " + Res.get("chat.message.wasEdited");

        private final ListView<ChatMessageListItem<? extends ChatMessage>> listView;
        private final ImageView scrollDownImageView;
        private final Badge scrollDownBadge;
        private final BisqTooltip scrollDownTooltip;
        private final Label placeholderTitle = new Label();
        private final Label placeholderDescription = new Label();
        private final VBox placeholder;
        private Optional<VirtualScrollBar> scrollBar = Optional.empty();
        private Subscription hasUnreadMessagesPin, showScrolledDownButtonPin;
        private Timeline fadeInScrollDownBadgeTimeline;

        private View(Model model, Controller controller) {
            super(new StackPane(), model, controller);

            listView = new ListView<>(model.getSortedChatMessages());
            listView.getStyleClass().add("chat-messages-list-view");

            placeholder = ChatUtil.createEmptyChatPlaceholder(placeholderTitle, placeholderDescription);

            listView.setCellFactory(getCellFactory());

            // https://stackoverflow.com/questions/20621752/javafx-make-listview-not-selectable-via-mouse
            listView.setSelectionModel(new NoSelectionModel<>());
            VBox.setVgrow(listView, Priority.ALWAYS);

            scrollDownImageView = new ImageView();
            scrollDownImageView.setCursor(Cursor.HAND);
            scrollDownTooltip = new BisqTooltip(Res.get("chat.listView.scrollDown"));
            Tooltip.install(scrollDownImageView, scrollDownTooltip);

            scrollDownBadge = new Badge(scrollDownImageView);
            scrollDownBadge.setMaxSize(25, 25);
            scrollDownBadge.getStyleClass().add("chat-messages-badge");
            scrollDownBadge.setPosition(Pos.BOTTOM_RIGHT);
            scrollDownBadge.setBadgeInsets(new Insets(0, -5, -8, 0));

            StackPane.setAlignment(scrollDownBadge, Pos.BOTTOM_RIGHT);
            StackPane.setMargin(scrollDownBadge, new Insets(0, 25, 20, 0));
            root.setAlignment(Pos.CENTER);
            root.getChildren().addAll(listView, scrollDownBadge);
        }

        @Override
        protected void onViewAttached() {
            ListViewUtil.findScrollbarAsync(listView, Orientation.VERTICAL, 1000).whenComplete((scrollBar, throwable) -> {
                if (throwable != null) {
                    log.error("Find scrollbar failed", throwable);
                    return;
                }
                this.scrollBar = scrollBar;
                if (scrollBar.isPresent()) {
                    scrollBar.get().valueProperty().bindBidirectional(model.getScrollValue());
                    model.scrollBarVisible.bind(scrollBar.get().visibleProperty());
                    controller.onScrollToBottom();
                } else {
                    log.error("scrollBar is empty");
                }
            });

            scrollDownBadge.textProperty().bind(model.numUnReadMessages);

            scrollDownBadge.setOpacity(0);
            showScrolledDownButtonPin = EasyBind.subscribe(model.showScrolledDownButton, showScrolledDownButton -> {
                if (showScrolledDownButton == null) {
                    return;
                }
                if (fadeInScrollDownBadgeTimeline != null) {
                    fadeInScrollDownBadgeTimeline.stop();
                }
                if (showScrolledDownButton) {
                    fadeInScrollDownBadge();
                } else {
                    scrollDownBadge.setOpacity(0);
                }
            });
            hasUnreadMessagesPin = EasyBind.subscribe(model.hasUnreadMessages, hasUnreadMessages -> {
                if (hasUnreadMessages) {
                    scrollDownImageView.setOpacity(1);
                    scrollDownImageView.setId("scroll-down-green");
                    scrollDownTooltip.setText(Res.get("chat.listView.scrollDown.newMessages"));
                } else {
                    scrollDownImageView.setOpacity(0.5);
                    scrollDownImageView.setId("scroll-down-white");
                    scrollDownTooltip.setText(Res.get("chat.listView.scrollDown"));
                }
            });

            scrollDownImageView.setOnMouseClicked(e -> controller.onScrollToBottom());

            UIThread.runOnNextRenderFrame(this::adjustPadding);

            if (ChatUtil.isCommonChat(model.getChatChannelDomain()) && model.isPublicChannel.get()) {
                placeholderTitle.setText(Res.get("chat.messagebox.noChats.placeholder.title"));
                placeholderDescription.setText(Res.get("chat.messagebox.noChats.placeholder.description",
                        model.getSelectedChannel().get().getDisplayString()));
                listView.setPlaceholder(placeholder);
            }
        }

        @Override
        protected void onViewDetached() {
            scrollBar.ifPresent(scrollbar -> scrollbar.valueProperty().unbindBidirectional(model.getScrollValue()));
            model.scrollBarVisible.unbind();
            scrollDownBadge.textProperty().unbind();
            hasUnreadMessagesPin.unsubscribe();
            showScrolledDownButtonPin.unsubscribe();

            scrollDownImageView.setOnMouseClicked(null);
            if (fadeInScrollDownBadgeTimeline != null) {
                fadeInScrollDownBadgeTimeline.stop();
                fadeInScrollDownBadgeTimeline = null;
                scrollDownBadge.setOpacity(0);
            }
        }

        private void fadeInScrollDownBadge() {
            if (!Transitions.getUseAnimations()) {
                scrollDownBadge.setOpacity(1);
                return;
            }

            fadeInScrollDownBadgeTimeline = new Timeline();
            scrollDownBadge.setOpacity(0);
            ObservableList<KeyFrame> keyFrames = fadeInScrollDownBadgeTimeline.getKeyFrames();
            keyFrames.add(new KeyFrame(Duration.millis(0),
                    new KeyValue(scrollDownBadge.opacityProperty(), 0, Interpolator.LINEAR)
            ));
            // Add a delay before starting fade-in to deal with a render delay when adding a
            // list item.
            keyFrames.add(new KeyFrame(Duration.millis(100),
                    new KeyValue(scrollDownBadge.opacityProperty(), 0, Interpolator.EASE_OUT)
            ));
            keyFrames.add(new KeyFrame(Duration.millis(400),
                    new KeyValue(scrollDownBadge.opacityProperty(), 1, Interpolator.EASE_OUT)
            ));
            fadeInScrollDownBadgeTimeline.play();
        }

        private void adjustPadding() {
            Optional<VirtualScrollBar> scrollBar = ListViewUtil.findScrollbar(listView, Orientation.VERTICAL);
            scrollBar.ifPresent(bar ->
                listView.setPadding(
                        bar.isVisible() ? LISTVIEW_PADDING_WITH_SCROLLBAR : new Insets(0))
            );
        }

        public Callback<ListView<ChatMessageListItem<? extends ChatMessage>>, ListCell<ChatMessageListItem<? extends ChatMessage>>> getCellFactory() {
            return new Callback<>() {
                @Override
                public ListCell<ChatMessageListItem<? extends ChatMessage>> call(ListView<ChatMessageListItem<? extends ChatMessage>> list) {
                    return new ListCell<>() {
                        private final static double CHAT_BOX_MAX_WIDTH = 1200;
                        private final static double CHAT_MESSAGE_BOX_MAX_WIDTH = 630;

                        private final ReputationScoreDisplay reputationScoreDisplay;
                        private final Button takeOfferButton, removeOfferButton;
                        private final Label message, userName, dateTime, replyIcon, pmIcon, editIcon, deleteIcon, copyIcon,
                                moreOptionsIcon, supportedLanguages;
                        private final Label deliveryState;
                        private final Label quotedMessageField = new Label();
                        private final BisqTextArea editInputField;
                        private final Button saveEditButton, cancelEditButton;
                        private final VBox mainVBox, quotedMessageVBox;
                        private final HBox cellHBox, messageHBox, messageBgHBox, reactionsHBox, editButtonsHBox;
                        private final UserProfileIcon userProfileIcon = new UserProfileIcon(60);
                        private final Set<Subscription> subscriptions = new HashSet<>();

                        {
                            userName = new Label();
                            userName.getStyleClass().addAll("text-fill-white", "font-size-09", "font-default");

                            deliveryState = new Label();
                            deliveryState.setCursor(Cursor.HAND);
                            deliveryState.setTooltip(new BisqTooltip(true));

                            dateTime = new Label();
                            dateTime.getStyleClass().addAll("text-fill-grey-dimmed", "font-size-09", "font-light");

                            reputationScoreDisplay = new ReputationScoreDisplay();
                            takeOfferButton = new Button(Res.get("offer.takeOffer"));

                            removeOfferButton = new Button(Res.get("offer.deleteOffer"));
                            removeOfferButton.getStyleClass().addAll("red-small-button", "no-background");

                            // quoted message
                            quotedMessageField.setWrapText(true);
                            quotedMessageVBox = new VBox(5);
                            quotedMessageVBox.setVisible(false);
                            quotedMessageVBox.setManaged(false);

                            // HBox for message reputation vBox and action button
                            message = new Label();
                            message.setWrapText(true);
                            message.setPadding(new Insets(10));
                            message.getStyleClass().addAll("text-fill-white", "normal-text", "font-default");


                            // edit
                            editInputField = new BisqTextArea();
                            //editInputField.getStyleClass().addAll("text-fill-white", "font-size-13", "font-default");
                            editInputField.setId("chat-messages-edit-text-area");
                            editInputField.setMinWidth(150);
                            editInputField.setVisible(false);
                            editInputField.setManaged(false);

                            // edit buttons
                            saveEditButton = new Button(Res.get("action.save"));
                            saveEditButton.setDefaultButton(true);
                            cancelEditButton = new Button(Res.get("action.cancel"));

                            editButtonsHBox = new HBox(15, Spacer.fillHBox(), cancelEditButton, saveEditButton);
                            editButtonsHBox.setVisible(false);
                            editButtonsHBox.setManaged(false);

                            messageBgHBox = new HBox(15);
                            messageBgHBox.setAlignment(Pos.CENTER_LEFT);
                            messageBgHBox.setMaxWidth(CHAT_MESSAGE_BOX_MAX_WIDTH);

                            // Reactions box
                            replyIcon = getIconWithToolTip(AwesomeIcon.REPLY, Res.get("chat.message.reply"));
                            pmIcon = getIconWithToolTip(AwesomeIcon.COMMENT_ALT, Res.get("chat.message.privateMessage"));
                            editIcon = getIconWithToolTip(AwesomeIcon.EDIT, Res.get("action.edit"));
                            HBox.setMargin(editIcon, new Insets(1, 0, 0, 0));
                            copyIcon = getIconWithToolTip(AwesomeIcon.COPY, Res.get("action.copyToClipboard"));
                            deleteIcon = getIconWithToolTip(AwesomeIcon.REMOVE_SIGN, Res.get("action.delete"));
                            moreOptionsIcon = getIconWithToolTip(AwesomeIcon.ELLIPSIS_HORIZONTAL, Res.get("chat.message.moreOptions"));
                            supportedLanguages = new Label();

                            reactionsHBox = new HBox(20);

                            reactionsHBox.setVisible(false);

                            HBox.setHgrow(messageBgHBox, Priority.SOMETIMES);
                            messageHBox = new HBox();

                            VBox.setMargin(quotedMessageVBox, new Insets(15, 0, 10, 5));
                            VBox.setMargin(messageHBox, new Insets(10, 0, 0, 0));
                            VBox.setMargin(editButtonsHBox, new Insets(10, 25, -15, 0));
                            mainVBox = new VBox();
                            mainVBox.setFillWidth(true);
                            HBox.setHgrow(mainVBox, Priority.ALWAYS);
                            cellHBox = new HBox(15);
                            cellHBox.setPadding(new Insets(0, 20, 0, 20));
                            cellHBox.setMaxWidth(CHAT_BOX_MAX_WIDTH);
                            cellHBox.setAlignment(Pos.CENTER);
                        }


                        private void hideReactionsBox() {
                            reactionsHBox.setVisible(false);
                        }

                        @Override
                        public void updateItem(final ChatMessageListItem<? extends ChatMessage> item, boolean empty) {
                            super.updateItem(item, empty);
                            if (item == null || empty) {
                                cleanup();
                                return;
                            }

                            subscriptions.clear();
                            ChatMessage chatMessage = item.getChatMessage();

                            Node flow = this.getListView().lookup(".virtual-flow");
                            if (flow != null && !flow.isVisible()) {
                                return;
                            }

                            boolean hasTradeChatOffer = model.hasTradeChatOffer(chatMessage);
                            boolean isBisqEasyPublicChatMessageWithOffer = chatMessage instanceof BisqEasyOfferbookMessage && hasTradeChatOffer;
                            boolean isMyMessage = model.isMyMessage(chatMessage);

                            if (isBisqEasyPublicChatMessageWithOffer) {
                                supportedLanguages.setText(controller.getSupportedLanguageCodes(((BisqEasyOfferbookMessage) chatMessage)));
                                supportedLanguages.setTooltip(new BisqTooltip(controller.getSupportedLanguageCodesForTooltip(((BisqEasyOfferbookMessage) chatMessage))));
                            }

                            dateTime.setVisible(false);

                            cellHBox.getChildren().setAll(mainVBox);

                            message.maxWidthProperty().unbind();
                            if (hasTradeChatOffer) {
                                messageBgHBox.setPadding(new Insets(15));
                            } else {
                                messageBgHBox.setPadding(new Insets(5, 15, 5, 15));
                            }
                            messageBgHBox.getStyleClass().removeAll("chat-message-bg-my-message", "chat-message-bg-peer-message");
                            VBox userProfileIconVbox = new VBox(userProfileIcon);
                            if (isMyMessage) {
                                buildMyMessage(isBisqEasyPublicChatMessageWithOffer, userProfileIconVbox, chatMessage);
                            } else {
                                buildPeerMessage(item, isBisqEasyPublicChatMessageWithOffer, userProfileIconVbox, chatMessage);
                            }

                            handleQuoteMessageBox(item);
                            handleReactionsBox(item);
                            handleEditBox(chatMessage);

                            message.setText(item.getMessage());
                            dateTime.setText(item.getDate());

                            item.getSenderUserProfile().ifPresent(author -> {
                                userName.setText(author.getUserName());
                                userName.setOnMouseClicked(e -> controller.onMention(author));

                                userProfileIcon.setUserProfile(author);
                                userProfileIcon.setCursor(Cursor.HAND);
                                Tooltip.install(userProfileIcon, new BisqTooltip(author.getTooltipString()));
                                userProfileIcon.setOnMouseClicked(e -> controller.onShowChatUserDetails(chatMessage));
                            });

                            subscriptions.add(EasyBind.subscribe(item.getMessageDeliveryStatusIcon(), icon -> {
                                        deliveryState.setManaged(icon != null);
                                        deliveryState.setVisible(icon != null);
                                        if (icon != null) {
                                            AwesomeDude.setIcon(deliveryState, icon, AwesomeDude.DEFAULT_ICON_SIZE);
                                            item.messageDeliveryStatusIconColor.ifPresent(color ->
                                                    Icons.setAwesomeIconColor(deliveryState, color));
                                        }
                                    }
                            ));
                            deliveryState.getTooltip().textProperty().bind(item.messageDeliveryStatusTooltip);
                            editInputField.maxWidthProperty().bind(message.widthProperty());
                            setGraphic(cellHBox);
                            setAlignment(Pos.CENTER);
                        }

                        private void buildPeerMessage(ChatMessageListItem<? extends ChatMessage> item, boolean isBisqEasyPublicChatMessageWithOffer, VBox userProfileIconVbox, ChatMessage chatMessage) {
                            // Peer
                            HBox userNameAndDateHBox = new HBox(10, userName, dateTime);
                            message.setAlignment(Pos.CENTER_LEFT);
                            userNameAndDateHBox.setAlignment(Pos.CENTER_LEFT);

                            userProfileIcon.setSize(60);
                            HBox.setMargin(replyIcon, new Insets(4, 0, -4, 10));
                            HBox.setMargin(pmIcon, new Insets(4, 0, -4, 0));
                            HBox.setMargin(moreOptionsIcon, new Insets(6, 0, -6, 0));


                            quotedMessageVBox.setId("chat-message-quote-box-peer-msg");

                            messageBgHBox.getStyleClass().add("chat-message-bg-peer-message");
                            if (isBisqEasyPublicChatMessageWithOffer) {
                                reactionsHBox.getChildren().setAll(replyIcon, pmIcon, editIcon, deleteIcon, moreOptionsIcon, supportedLanguages, Spacer.fillHBox());
                                message.maxWidthProperty().bind(root.widthProperty().subtract(430));
                                userProfileIconVbox.setAlignment(Pos.CENTER_LEFT);

                                Label reputationLabel = new Label(Res.get("chat.message.reputation").toUpperCase());
                                reputationLabel.getStyleClass().add("bisq-text-7");

                                reputationScoreDisplay.setReputationScore(item.getReputationScore());
                                VBox.setMargin(reputationScoreDisplay, new Insets(0, 0, 10, 0));
                                reputationScoreDisplay.setAlignment(Pos.CENTER);
                                VBox reputationTakeOfferVBox = new VBox(4, reputationLabel, reputationScoreDisplay, takeOfferButton);
                                reputationTakeOfferVBox.setAlignment(Pos.CENTER);

                                BisqEasyOfferbookMessage bisqEasyOfferbookMessage = (BisqEasyOfferbookMessage) chatMessage;
                                takeOfferButton.setOnAction(e -> controller.onTakeOffer(bisqEasyOfferbookMessage, item.isCanTakeOffer()));
                                takeOfferButton.setDefaultButton(item.isCanTakeOffer());

                                VBox messageVBox = new VBox(quotedMessageVBox, message);
                                HBox.setMargin(userProfileIconVbox, new Insets(-5, 0, -5, 0));
                                HBox.setMargin(messageVBox, new Insets(0, 0, 0, -10));
                                HBox.setMargin(reputationTakeOfferVBox, new Insets(-5, 10, 0, 0));
                                messageBgHBox.getChildren().setAll(userProfileIconVbox, messageVBox, Spacer.fillHBox(), reputationTakeOfferVBox);

                                VBox.setMargin(userNameAndDateHBox, new Insets(-5, 0, 5, 10));
                                mainVBox.getChildren().setAll(userNameAndDateHBox, messageBgHBox, reactionsHBox);
                            } else {
                                reactionsHBox.getChildren().setAll(replyIcon, pmIcon, editIcon, deleteIcon, moreOptionsIcon, Spacer.fillHBox());
                                message.maxWidthProperty().bind(root.widthProperty().subtract(140));//165
                                userProfileIcon.setSize(30);
                                userProfileIconVbox.setAlignment(Pos.TOP_LEFT);

                                VBox messageVBox = new VBox(quotedMessageVBox, message);
                                HBox.setMargin(userProfileIconVbox, new Insets(7.5, 0, -5, 5));
                                HBox.setMargin(messageVBox, new Insets(0, 0, 0, -10));
                                messageBgHBox.getChildren().setAll(userProfileIconVbox, messageVBox);
                                messageHBox.getChildren().setAll(messageBgHBox, Spacer.fillHBox());

                                VBox.setMargin(userNameAndDateHBox, new Insets(-5, 0, -5, 10));
                                mainVBox.getChildren().setAll(userNameAndDateHBox, messageHBox, reactionsHBox);
                            }
                        }

                        private void buildMyMessage(boolean isBisqEasyPublicChatMessageWithOffer, VBox userProfileIconVbox, ChatMessage chatMessage) {
                            HBox userNameAndDateHBox = new HBox(10, dateTime, userName);
                            userNameAndDateHBox.setAlignment(Pos.CENTER_RIGHT);
                            message.setAlignment(Pos.CENTER_RIGHT);

                            quotedMessageVBox.setId("chat-message-quote-box-my-msg");

                            messageBgHBox.getStyleClass().add("chat-message-bg-my-message");
                            VBox.setMargin(userNameAndDateHBox, new Insets(-5, 10, -5, 0));

                            VBox messageVBox = new VBox(quotedMessageVBox, message, editInputField);
                            if (isBisqEasyPublicChatMessageWithOffer) {
                                message.maxWidthProperty().bind(root.widthProperty().subtract(160));
                                userProfileIcon.setSize(60);
                                userProfileIconVbox.setAlignment(Pos.CENTER_LEFT);
                                HBox.setMargin(userProfileIconVbox, new Insets(-5, 0, -5, 0));
                                HBox.setMargin(editInputField, new Insets(-4, -10, -15, 0));
                                HBox.setMargin(messageVBox, new Insets(0, -10, 0, 0));

                                removeOfferButton.setOnAction(e -> controller.onDeleteMessage(chatMessage));
                                reactionsHBox.getChildren().setAll(Spacer.fillHBox(), replyIcon, pmIcon, editIcon, supportedLanguages, copyIcon);
                                reactionsHBox.setAlignment(Pos.CENTER_RIGHT);

                                HBox.setMargin(userProfileIconVbox, new Insets(0, 0, 10, 0));
                                HBox hBox = new HBox(15, messageVBox, userProfileIconVbox);
                                HBox removeOfferButtonHBox = new HBox(Spacer.fillHBox(), removeOfferButton);
                                VBox vBox = new VBox(hBox, removeOfferButtonHBox);
                                messageBgHBox.getChildren().setAll(vBox);
                            } else {
                                message.maxWidthProperty().bind(root.widthProperty().subtract(140));
                                userProfileIcon.setSize(30);
                                userProfileIconVbox.setAlignment(Pos.TOP_LEFT);
                                HBox.setMargin(deleteIcon, new Insets(0, 10, 0, 0));
                                reactionsHBox.getChildren().setAll(Spacer.fillHBox(), replyIcon, pmIcon, editIcon, copyIcon, deleteIcon);
                                HBox.setMargin(messageVBox, new Insets(0, -15, 0, 0));
                                HBox.setMargin(userProfileIconVbox, new Insets(7.5, 0, -5, 5));
                                HBox.setMargin(editInputField, new Insets(6, -10, -25, 0));
                                messageBgHBox.getChildren().setAll(messageVBox, userProfileIconVbox);
                            }

                            HBox.setMargin(deliveryState, new Insets(0, 10, 0, 0));
                            HBox deliveryStateHBox = new HBox(Spacer.fillHBox(), reactionsHBox);

                            subscriptions.add(EasyBind.subscribe(reactionsHBox.visibleProperty(), v -> {
                                if (v) {
                                    deliveryStateHBox.getChildren().remove(deliveryState);
                                    if (!reactionsHBox.getChildren().contains(deliveryState)) {
                                        reactionsHBox.getChildren().add(deliveryState);
                                    }
                                } else {
                                    reactionsHBox.getChildren().remove(deliveryState);
                                    if (!deliveryStateHBox.getChildren().contains(deliveryState)) {
                                        deliveryStateHBox.getChildren().add(deliveryState);
                                    }
                                }
                            }));

                            VBox.setMargin(deliveryStateHBox, new Insets(4, 0, -3, 0));
                            mainVBox.getChildren().setAll(userNameAndDateHBox, messageHBox, editButtonsHBox, deliveryStateHBox);

                            messageHBox.getChildren().setAll(Spacer.fillHBox(), messageBgHBox);
                        }


                        private void cleanup() {
                            message.maxWidthProperty().unbind();
                            editInputField.maxWidthProperty().unbind();

                            editInputField.maxWidthProperty().unbind();
                            removeOfferButton.setOnAction(null);
                            takeOfferButton.setOnAction(null);

                            saveEditButton.setOnAction(null);
                            cancelEditButton.setOnAction(null);

                            userName.setOnMouseClicked(null);
                            userProfileIcon.setOnMouseClicked(null);
                            replyIcon.setOnMouseClicked(null);
                            pmIcon.setOnMouseClicked(null);
                            editIcon.setOnMouseClicked(null);
                            copyIcon.setOnMouseClicked(null);
                            deleteIcon.setOnMouseClicked(null);
                            moreOptionsIcon.setOnMouseClicked(null);

                            editInputField.setOnKeyPressed(null);

                            cellHBox.setOnMouseEntered(null);
                            cellHBox.setOnMouseExited(null);

                            userProfileIcon.releaseResources();

                            subscriptions.forEach(Subscription::unsubscribe);
                            subscriptions.clear();

                            setGraphic(null);
                        }

                        private void handleEditBox(ChatMessage chatMessage) {
                            saveEditButton.setOnAction(e -> {
                                controller.onSaveEditedMessage(chatMessage, editInputField.getText());
                                onCloseEditMessage();
                            });
                            cancelEditButton.setOnAction(e -> onCloseEditMessage());
                        }

                        private void handleReactionsBox(ChatMessageListItem<? extends ChatMessage> item) {
                            ChatMessage chatMessage = item.getChatMessage();
                            boolean isMyMessage = model.isMyMessage(chatMessage);

                            boolean isPublicChannel = model.isPublicChannel.get();
                            boolean allowEditing = isPublicChannel;
                            if (chatMessage instanceof BisqEasyOfferbookMessage) {
                                BisqEasyOfferbookMessage bisqEasyOfferbookMessage = (BisqEasyOfferbookMessage) chatMessage;
                                allowEditing = allowEditing && bisqEasyOfferbookMessage.getBisqEasyOffer().isEmpty();
                            }
                            if (isMyMessage) {
                                copyIcon.setOnMouseClicked(e -> controller.onCopyMessage(chatMessage));
                                if (allowEditing) {
                                    editIcon.setOnMouseClicked(e -> onEditMessage(item));
                                }
                                if (isPublicChannel) {
                                    deleteIcon.setOnMouseClicked(e -> controller.onDeleteMessage(chatMessage));
                                }
                            } else {
                                moreOptionsIcon.setOnMouseClicked(e -> controller.onOpenMoreOptions(pmIcon, chatMessage, () -> {
                                    hideReactionsBox();
                                    model.selectedChatMessageForMoreOptionsPopup.set(null);
                                }));
                                replyIcon.setOnMouseClicked(e -> controller.onReply(chatMessage));
                                pmIcon.setOnMouseClicked(e -> controller.onOpenPrivateChannel(chatMessage));
                            }

                            replyIcon.setVisible(!isMyMessage);
                            replyIcon.setManaged(!isMyMessage);

                            pmIcon.setVisible(!isMyMessage && chatMessage instanceof PublicChatMessage);
                            pmIcon.setManaged(!isMyMessage && chatMessage instanceof PublicChatMessage);

                            editIcon.setVisible(isMyMessage && allowEditing);
                            editIcon.setManaged(isMyMessage && allowEditing);
                            deleteIcon.setVisible(isMyMessage && isPublicChannel);
                            deleteIcon.setManaged(isMyMessage && isPublicChannel);
                            removeOfferButton.setVisible(isMyMessage && isPublicChannel);
                            removeOfferButton.setManaged(isMyMessage && isPublicChannel);

                            setOnMouseEntered(e -> {
                                if (model.selectedChatMessageForMoreOptionsPopup.get() != null || editInputField.isVisible()) {
                                    return;
                                }
                                dateTime.setVisible(true);
                                reactionsHBox.setVisible(true);
                            });

                            setOnMouseExited(e -> {
                                if (model.selectedChatMessageForMoreOptionsPopup.get() == null) {
                                    hideReactionsBox();
                                    dateTime.setVisible(false);
                                    reactionsHBox.setVisible(false);
                                }
                            });
                        }

                        private void handleQuoteMessageBox(ChatMessageListItem<? extends ChatMessage> item) {
                            Optional<Citation> optionalCitation = item.getCitation();
                            if (optionalCitation.isPresent()) {
                                Citation citation = optionalCitation.get();
                                if (citation.isValid()) {
                                    quotedMessageVBox.setVisible(true);
                                    quotedMessageVBox.setManaged(true);
                                    quotedMessageField.setText(citation.getText());
                                    quotedMessageField.setStyle("-fx-fill: -fx-mid-text-color");
                                    Label userName = new Label(controller.getUserName(citation.getAuthorUserProfileId()));
                                    userName.getStyleClass().add("font-medium");
                                    userName.setStyle("-fx-text-fill: -bisq-mid-grey-40");
                                    quotedMessageVBox.getChildren().setAll(userName, quotedMessageField);
                                }
                            } else {
                                quotedMessageVBox.getChildren().clear();
                                quotedMessageVBox.setVisible(false);
                                quotedMessageVBox.setManaged(false);
                            }
                        }

                        private void onEditMessage(ChatMessageListItem<? extends ChatMessage> item) {
                            reactionsHBox.setVisible(false);
                            editInputField.setVisible(true);
                            editInputField.setManaged(true);
                            editInputField.setInitialHeight(message.getBoundsInLocal().getHeight());
                            editInputField.setText(message.getText().replace(EDITED_POST_FIX, ""));
                            editInputField.requestFocus();
                            editInputField.positionCaret(message.getText().length());
                            editButtonsHBox.setVisible(true);
                            editButtonsHBox.setManaged(true);
                            removeOfferButton.setVisible(false);
                            removeOfferButton.setManaged(false);

                            message.setVisible(false);
                            message.setManaged(false);

                            editInputField.setOnKeyPressed(event -> {
                                if (event.getCode() == KeyCode.ENTER) {
                                    event.consume();
                                    if (event.isShiftDown()) {
                                        editInputField.appendText(System.getProperty("line.separator"));
                                    } else if (!editInputField.getText().isEmpty()) {
                                        controller.onSaveEditedMessage(item.getChatMessage(), editInputField.getText().trim());
                                        onCloseEditMessage();
                                    }
                                }
                            });
                        }

                        private void onCloseEditMessage() {
                            editInputField.setVisible(false);
                            editInputField.setManaged(false);
                            editButtonsHBox.setVisible(false);
                            editButtonsHBox.setManaged(false);
                            removeOfferButton.setVisible(true);
                            removeOfferButton.setManaged(true);

                            message.setVisible(true);
                            message.setManaged(true);
                            editInputField.setOnKeyPressed(null);
                        }
                    };
                }

                private Label getIconWithToolTip(AwesomeIcon icon, String tooltipString) {
                    Label iconLabel = Icons.getIcon(icon);
                    iconLabel.setCursor(Cursor.HAND);
                    iconLabel.setTooltip(new BisqTooltip(tooltipString, true));
                    return iconLabel;
                }
            };
        }
    }

    @Slf4j
    @Getter
    @EqualsAndHashCode
    public static class ChatMessageListItem<T extends ChatMessage> implements Comparable<ChatMessageListItem<T>> {
        private final T chatMessage;
        private final String message;
        private final String date;
        private final Optional<Citation> citation;
        private final Optional<UserProfile> senderUserProfile;
        private final String nym;
        private final String nickName;
        @EqualsAndHashCode.Exclude
        private final ReputationScore reputationScore;
        private final boolean canTakeOffer;
        @EqualsAndHashCode.Exclude
        private final StringProperty messageDeliveryStatusTooltip = new SimpleStringProperty();
        @EqualsAndHashCode.Exclude
        private final ObjectProperty<AwesomeIcon> messageDeliveryStatusIcon = new SimpleObjectProperty<>();
        private Optional<String> messageDeliveryStatusIconColor = Optional.empty();
        @EqualsAndHashCode.Exclude
        private final Set<Pin> mapPins = new HashSet<>();
        private final Set<Pin> statusPins = new HashSet<>();

        public ChatMessageListItem(T chatMessage,
                                   UserProfileService userProfileService,
                                   ReputationService reputationService,
                                   BisqEasyTradeService bisqEasyTradeService,
                                   UserIdentityService userIdentityService,
                                   NetworkService networkService) {
            this.chatMessage = chatMessage;

            if (chatMessage instanceof PrivateChatMessage) {
                senderUserProfile = Optional.of(((PrivateChatMessage) chatMessage).getSenderUserProfile());
            } else {
                senderUserProfile = userProfileService.findUserProfile(chatMessage.getAuthorUserProfileId());
            }
            String editPostFix = chatMessage.isWasEdited() ? EDITED_POST_FIX : "";
            message = chatMessage.getText() + editPostFix;
            citation = chatMessage.getCitation();
            date = DateFormatter.formatDateTime(new Date(chatMessage.getDate()), DateFormat.MEDIUM, DateFormat.SHORT, true, " " + Res.get("temporal.at") + " ");

            nym = senderUserProfile.map(UserProfile::getNym).orElse("");
            nickName = senderUserProfile.map(UserProfile::getNickName).orElse("");

            reputationScore = senderUserProfile.flatMap(reputationService::findReputationScore).orElse(ReputationScore.NONE);

            if (chatMessage instanceof BisqEasyOfferbookMessage) {
                BisqEasyOfferbookMessage bisqEasyOfferbookMessage = (BisqEasyOfferbookMessage) chatMessage;
                if (userIdentityService.getSelectedUserIdentity() != null && bisqEasyOfferbookMessage.getBisqEasyOffer().isPresent()) {
                    UserProfile userProfile = userIdentityService.getSelectedUserIdentity().getUserProfile();
                    NetworkId takerNetworkId = userProfile.getNetworkId();
                    BisqEasyOffer bisqEasyOffer = bisqEasyOfferbookMessage.getBisqEasyOffer().get();
                    String tradeId = Trade.createId(bisqEasyOffer.getId(), takerNetworkId.getId());
                    canTakeOffer = !bisqEasyTradeService.tradeExists(tradeId);
                } else {
                    canTakeOffer = false;
                }
            } else {
                canTakeOffer = false;
            }

            mapPins.add(networkService.getMessageDeliveryStatusByMessageId().addObserver(new HashMapObserver<>() {
                @Override
                public void put(String key, Observable<MessageDeliveryStatus> value) {
                    if (key.equals(chatMessage.getId())) {
                        // Delay to avoid ConcurrentModificationException
                        UIThread.runOnNextRenderFrame(() -> {
                            statusPins.add(value.addObserver(status -> {
                                UIThread.run(() -> {
                                    if (status != null) {
                                        messageDeliveryStatusIconColor = Optional.empty();
                                        messageDeliveryStatusTooltip.set(Res.get("chat.message.deliveryState." + status.name()));
                                        switch (status) {
                                            case CONNECTING:
                                                // -bisq-mid-grey-30: #808080;
                                                messageDeliveryStatusIconColor = Optional.of("#808080");
                                                messageDeliveryStatusIcon.set(AwesomeIcon.SPINNER);
                                                break;
                                            case SENT:
                                                // -bisq-light-grey-50: #eaeaea;
                                                messageDeliveryStatusIconColor = Optional.of("#eaeaea");
                                                messageDeliveryStatusIcon.set(AwesomeIcon.CIRCLE_ARROW_RIGHT);
                                                break;
                                            case ACK_RECEIVED:
                                                // -bisq2-green-dim-50: #2b5624;
                                                messageDeliveryStatusIconColor = Optional.of("#2b5624");
                                                messageDeliveryStatusIcon.set(AwesomeIcon.OK_SIGN);
                                                break;
                                            case TRY_ADD_TO_MAILBOX:
                                                // -bisq-yellow: #e5a500;
                                                messageDeliveryStatusIconColor = Optional.of("#e5a500");
                                                messageDeliveryStatusIcon.set(AwesomeIcon.SHARE_SIGN);
                                                break;
                                            case ADDED_TO_MAILBOX:
                                                // -bisq-yellow: #e5a500;
                                                messageDeliveryStatusIconColor = Optional.of("#e5a500");
                                                messageDeliveryStatusIcon.set(AwesomeIcon.CLOUD_UPLOAD);
                                                break;
                                            case MAILBOX_MSG_RECEIVED:
                                                // -bisq2-green-dim-50: #2b5624;
                                                messageDeliveryStatusIconColor = Optional.of("#2b5624");
                                                messageDeliveryStatusIcon.set(AwesomeIcon.CLOUD_DOWNLOAD);
                                                break;
                                            case FAILED:
                                                // -bisq-red: #d02c1f;
                                                messageDeliveryStatusIconColor = Optional.of("#d02c1f");
                                                messageDeliveryStatusIcon.set(AwesomeIcon.EXCLAMATION_SIGN);
                                                break;
                                        }
                                    }
                                });
                            }));
                        });
                    }
                }

                @Override
                public void putAll(Map<? extends String, ? extends Observable<MessageDeliveryStatus>> map) {
                    map.forEach(this::put);
                }

                @Override
                public void remove(Object key) {
                }

                @Override
                public void clear() {
                }
            }));
        }

        @Override
        public int compareTo(ChatMessageListItem o) {
            return Comparator.comparingLong(ChatMessage::getDate).compare(this.getChatMessage(), o.getChatMessage());
        }

        public boolean match(String filterString) {
            return filterString == null || filterString.isEmpty() || StringUtils.containsIgnoreCase(message, filterString) || StringUtils.containsIgnoreCase(nym, filterString) || StringUtils.containsIgnoreCase(nickName, filterString) || StringUtils.containsIgnoreCase(date, filterString);
        }

        public void dispose() {
            mapPins.forEach(Pin::unbind);
            statusPins.forEach(Pin::unbind);
        }
    }
}
