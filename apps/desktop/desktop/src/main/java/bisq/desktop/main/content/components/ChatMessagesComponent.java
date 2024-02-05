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

import bisq.account.AccountService;
import bisq.account.accounts.Account;
import bisq.account.payment_method.PaymentMethod;
import bisq.bisq_easy.NavigationTarget;
import bisq.chat.*;
import bisq.chat.bisqeasy.offerbook.BisqEasyOfferbookChannel;
import bisq.chat.bisqeasy.open_trades.BisqEasyOpenTradeChannel;
import bisq.chat.common.CommonPublicChatChannel;
import bisq.chat.priv.PrivateChatChannel;
import bisq.chat.priv.PrivateChatChannelService;
import bisq.chat.pub.PublicChatChannel;
import bisq.chat.two_party.TwoPartyPrivateChatChannel;
import bisq.common.observable.Pin;
import bisq.common.util.StringUtils;
import bisq.desktop.ServiceProvider;
import bisq.desktop.common.observable.FxBindings;
import bisq.desktop.common.threading.UIThread;
import bisq.desktop.common.utils.ImageUtil;
import bisq.desktop.common.view.Navigation;
import bisq.desktop.components.controls.BisqTextArea;
import bisq.desktop.components.controls.BisqTooltip;
import bisq.desktop.components.overlay.Popup;
import bisq.desktop.main.content.chat.ChatUtil;
import bisq.i18n.Res;
import bisq.offer.Direction;
import bisq.offer.bisq_easy.BisqEasyOffer;
import bisq.settings.SettingsService;
import bisq.user.identity.UserIdentity;
import bisq.user.identity.UserIdentityService;
import bisq.user.profile.UserProfile;
import bisq.user.profile.UserProfileService;
import bisq.wallets.core.WalletService;
import javafx.beans.binding.Bindings;
import javafx.beans.property.*;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.util.StringConverter;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.fxmisc.easybind.EasyBind;
import org.fxmisc.easybind.Subscription;

import javax.annotation.Nullable;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import static com.google.common.base.Preconditions.checkNotNull;

@Slf4j
public class ChatMessagesComponent {
    private final Controller controller;

    public ChatMessagesComponent(ServiceProvider serviceProvider,
                                 ChatChannelDomain chatChannelDomain,
                                 Consumer<UserProfile> openUserProfileSidebarHandler) {
        controller = new Controller(serviceProvider,
                chatChannelDomain,
                openUserProfileSidebarHandler);
    }

    public VBox getRoot() {
        return controller.view.getRoot();
    }

    public void mentionUser(UserProfile userProfile) {
        controller.mentionUserHandler(userProfile);
    }

    public void setSearchPredicate(Predicate<? super ChatMessagesListView.ChatMessageListItem<? extends ChatMessage>> predicate) {
        controller.chatMessagesListView.setSearchPredicate(predicate);
    }

    public void resetSelectedChatMessage() {
        controller.model.selectedChatMessage = null;
    }

    public void createAndSelectTwoPartyPrivateChatChannel(UserProfile peer) {
        controller.createAndSelectTwoPartyPrivateChatChannel(peer);
    }

    public void refreshMessages() {
        controller.chatMessagesListView.refreshMessages();
    }

    public void enableChatDialog(boolean isEnabled) {
        controller.enableChatDialog(isEnabled);
    }

    private static class Controller implements bisq.desktop.common.view.Controller {
        private final Model model;
        @Getter
        private final View view;
        private final Consumer<UserProfile> openUserProfileSidebarHandler;
        private final UserIdentityService userIdentityService;
        private final CitationBlock citationBlock;
        private final ChatMessagesListView chatMessagesListView;
        private final UserProfileService userProfileService;
        private final SettingsService settingsService;
        private final ChatService chatService;
        private final Optional<WalletService> walletService;
        private final AccountService accountService;

        private Pin selectedChannelPin, inMediationPin, chatMessagesPin,
                selectedPaymentAccountPin, paymentAccountsPin, getUserIdentitiesPin;
        private Subscription selectedPaymentAccountSubscription;

        private Controller(ServiceProvider serviceProvider,
                           ChatChannelDomain chatChannelDomain,
                           Consumer<UserProfile> openUserProfileSidebarHandler) {
            this.openUserProfileSidebarHandler = openUserProfileSidebarHandler;

            chatService = serviceProvider.getChatService();
            settingsService = serviceProvider.getSettingsService();
            userIdentityService = serviceProvider.getUserService().getUserIdentityService();
            userProfileService = serviceProvider.getUserService().getUserProfileService();
            accountService = serviceProvider.getAccountService();
            walletService = serviceProvider.getWalletService();

            citationBlock = new CitationBlock(serviceProvider);

            UserProfileSelection userProfileSelection = new UserProfileSelection(serviceProvider);

            chatMessagesListView = new ChatMessagesListView(serviceProvider,
                    this::mentionUserHandler,
                    this::showChatUserDetailsHandler,
                    this::replyHandler,
                    chatChannelDomain);

            model = new Model(chatChannelDomain, chatService);
            view = new View(model, this,
                    chatMessagesListView.getRoot(),
                    citationBlock.getRoot(),
                    userProfileSelection);
        }

        @Override
        public void onActivate() {
            model.mentionableUsers.setAll(userProfileService.getUserProfiles());
            model.getPaymentAccounts().setAll(accountService.getAccounts());
            Optional.ofNullable(model.selectedChatMessage).ifPresent(this::showChatUserDetailsHandler);

            getUserIdentitiesPin = userIdentityService.getUserIdentities().addObserver(() -> UIThread.run(this::applyUserProfileOrChannelChange));

            if (selectedChannelPin != null) {
                selectedChannelPin.unbind();
            }
            ChatChannelSelectionService selectionService = chatService.getChatChannelSelectionServices().get(model.getChatChannelDomain());
            selectedChannelPin = selectionService.getSelectedChannel().addObserver(this::selectedChannelChanged);

            paymentAccountsPin = accountService.getAccounts().addObserver(this::accountsChanged);
            selectedPaymentAccountPin = FxBindings.bind(model.selectedAccountProperty())
                    .to(accountService.selectedAccountAsObservable());
            selectedPaymentAccountSubscription = EasyBind.subscribe(model.selectedAccountProperty(),
                    selectedAccount -> {
                        if (selectedAccount != null) {
                            accountService.setSelectedAccount(selectedAccount);
                        }
                    });
        }

        @Override
        public void onDeactivate() {
            if (selectedChannelPin != null) {
                selectedChannelPin.unbind();
                selectedChannelPin = null;
            }
            selectedPaymentAccountPin.unbind();
            getUserIdentitiesPin.unbind();
            paymentAccountsPin.unbind();
            if (inMediationPin != null) {
                inMediationPin.unbind();
            }
            if (chatMessagesPin != null) {
                chatMessagesPin.unbind();
            }

            selectedPaymentAccountSubscription.unsubscribe();
            model.selectedChannel.set(null);
        }

        protected void selectedChannelChanged(ChatChannel<? extends ChatMessage> chatChannel) {
            UIThread.run(() -> {
                model.selectedChannel.set(chatChannel);
                applyUserProfileOrChannelChange();

                boolean isBisqEasyPrivateTradeChatChannel = chatChannel instanceof BisqEasyOpenTradeChannel;
                boolean isTwoPartyPrivateChatChannel = chatChannel instanceof TwoPartyPrivateChatChannel;
                model.getLeaveChannelButtonVisible().set(false);
                model.getOpenDisputeButtonVisible().set(isBisqEasyPrivateTradeChatChannel);
                model.getSendBtcAddressButtonVisible().set(false);
                model.getSendPaymentAccountButtonVisible().set(false);

                if (chatMessagesPin != null) {
                    chatMessagesPin.unbind();
                }
                if (isBisqEasyPrivateTradeChatChannel) {
                    chatMessagesPin = chatChannel.getChatMessages().addObserver(() -> privateTradeMessagesChanged((BisqEasyOpenTradeChannel) chatChannel));
                    BisqEasyOpenTradeChannel privateChannel = (BisqEasyOpenTradeChannel) chatChannel;
                    if (inMediationPin != null) {
                        inMediationPin.unbind();
                    }
                    inMediationPin = privateChannel.isInMediationObservable().addObserver(isInMediation ->
                            UIThread.run(() -> model.getOpenDisputeButtonVisible().set(!isInMediation && !privateChannel.isMediator())));
                } else if (isTwoPartyPrivateChatChannel) {
                    chatMessagesPin = chatChannel.getChatMessages().addObserver(() -> updateLeaveChannelButtonState((TwoPartyPrivateChatChannel) chatChannel));
                }

                accountsChanged();
            });
        }

        ///////////////////////////////////////////////////////////////////////////////////////////////////
        // From method calls on component
        ///////////////////////////////////////////////////////////////////////////////////////////////////

        private void createAndSelectTwoPartyPrivateChatChannel(UserProfile peer) {
            chatService.createAndSelectTwoPartyPrivateChatChannel(model.getChatChannelDomain(), peer)
                    .ifPresent(channel -> {
                        if (model.getChatChannelDomain() == ChatChannelDomain.BISQ_EASY_OFFERBOOK) {
                            Navigation.navigateTo(NavigationTarget.BISQ_EASY_PRIVATE_CHAT);
                        }
                    });
        }


        ///////////////////////////////////////////////////////////////////////////////////////////////////
        // Handlers passed to list view component
        ///////////////////////////////////////////////////////////////////////////////////////////////////

        private void replyHandler(ChatMessage chatMessage) {
            if (!chatMessage.isMyMessage(userIdentityService)) {
                citationBlock.reply(chatMessage);
            }
        }

        private void showChatUserDetailsHandler(ChatMessage chatMessage) {
            model.selectedChatMessage = chatMessage;
            userProfileService.findUserProfile(chatMessage.getAuthorUserProfileId())
                    .ifPresent(openUserProfileSidebarHandler);
        }

        private void mentionUserHandler(UserProfile userProfile) {
            String existingText = model.getTextInput().get();
            if (!existingText.isEmpty() && !existingText.endsWith(" ")) {
                existingText += " ";
            }
            model.getTextInput().set(existingText + "@" + userProfile.getUserName() + " ");
        }


        private void listUserNamesHandler(UserProfile user) {
            String content = model.getTextInput().get().replaceAll("@[a-zA-Z\\d]*$", "@" + user.getUserName() + " ");
            model.getTextInput().set(content);
            view.inputField.positionCaret(content.length());
        }

        private void listChannelsHandler(ChatChannel<?> chatChannel) {
            String channelTitle = chatService.findChatChannelService(chatChannel)
                    .map(service -> service.getChannelTitle(chatChannel))
                    .orElse("");
            String content = model.getTextInput().get().replaceAll("#[a-zA-Z\\d]*$", "#" + channelTitle + " ");
            model.getTextInput().set(content);
            view.inputField.positionCaret(content.length());
        }


        ///////////////////////////////////////////////////////////////////////////////////////////////////
        // Change handlers from service or model
        ///////////////////////////////////////////////////////////////////////////////////////////////////

        private void privateTradeMessagesChanged(BisqEasyOpenTradeChannel chatChannel) {
            UIThread.run(() -> {
                BisqEasyOffer bisqEasyOffer = chatChannel.getBisqEasyOffer();
                boolean isMaker = bisqEasyOffer.isMyOffer(userIdentityService.getMyUserProfileIds());
                Direction usersDirection = isMaker ?
                        bisqEasyOffer.getMakersDirection() :
                        bisqEasyOffer.getTakersDirection();

                if (usersDirection.isSell()) {
                    model.getSendPaymentAccountButtonVisible().set(true);
                    model.getSendBtcAddressButtonVisible().set(false);
                } else {
                    model.getSendPaymentAccountButtonVisible().set(false);
                    model.getSendBtcAddressButtonVisible().set(walletService.isPresent());
                }
            });
            updateLeaveChannelButtonState(chatChannel);
        }

        private void updateLeaveChannelButtonState(PrivateChatChannel<?> chatChannel) {
            UIThread.run(() -> {
                boolean peerLeft = chatChannel.getChatMessages().stream()
                        .anyMatch(message -> message.getChatMessageType() == ChatMessageType.LEAVE);
                model.getLeaveChannelButtonVisible().set(peerLeft);
            });
        }


        private void accountsChanged() {
            UIThread.run(() ->
                    model.getPaymentAccountSelectionVisible().set(
                            model.getSelectedChannel().get() instanceof BisqEasyOpenTradeChannel &&
                                    accountService.getAccounts().size() > 1));
        }

        private void applyUserProfileOrChannelChange() {
            boolean multipleProfiles = userIdentityService.getUserIdentities().size() > 1;
            ChatChannel<?> selectedChatChannel = model.selectedChannel.get();
            model.userProfileSelectionVisible.set(multipleProfiles && selectedChatChannel instanceof PublicChatChannel);

            if (chatMessagesPin != null) {
                chatMessagesPin.unbind();
            }

            if (selectedChatChannel != null) {
                chatMessagesPin = selectedChatChannel.getChatMessages().addObserver(this::maybeSwitchUserProfile);
            }
        }


        ///////////////////////////////////////////////////////////////////////////////////////////////////
        // UI handlers
        ///////////////////////////////////////////////////////////////////////////////////////////////////

        void onLeaveChannel() {
            ChatChannel<?> chatChannel = model.getSelectedChannel().get();
            if (!(chatChannel instanceof PrivateChatChannel))
                return;

            chatService.findChatChannelService(chatChannel)
                    .filter(service -> service instanceof PrivateChatChannelService)
                    .map(service -> (PrivateChatChannelService<?, ?, ?>) service).stream()
                    .findAny()
                    .ifPresent(service -> {
                        service.leaveChannel(chatChannel.getId());
                        chatService.getChatChannelSelectionServices().get(model.getChatChannelDomain()).maybeSelectFirstChannel();
                    });
        }

        private void onSendMessage(String text) {
            if (text == null || text.isEmpty()) {
                return;
            }

            if (model.selectedChannel.get() instanceof PublicChatChannel) {
                List<UserIdentity> myUserProfilesInChannel = getMyUserProfilesInChannel();
                if (myUserProfilesInChannel.size() > 0) {
                    UserIdentity lastUsedUserProfile = myUserProfilesInChannel.get(0);
                    if (!lastUsedUserProfile.equals(userIdentityService.getSelectedUserIdentity())) {
                        new Popup().warning(Res.get("chat.message.send.differentUserProfile.warn"))
                                .closeButtonText(Res.get("confirmation.no"))
                                .actionButtonText(Res.get("confirmation.yes"))
                                .onAction(() -> doSendMessage(text))
                                .show();
                        return;
                    }
                }
            }

            doSendMessage(text);
        }


        ///////////////////////////////////////////////////////////////////////////////////////////////////
        // Private
        ///////////////////////////////////////////////////////////////////////////////////////////////////

        private void doSendMessage(String text) {
            if (text.length() > ChatMessage.MAX_TEXT_LENGTH) {
                new Popup().warning(Res.get("validation.tooLong", ChatMessage.MAX_TEXT_LENGTH)).show();
                return;
            }

            ChatChannel<? extends ChatMessage> chatChannel = model.selectedChannel.get();
            UserIdentity userIdentity = checkNotNull(userIdentityService.getSelectedUserIdentity(), "user identity must not be null");
            Optional<Citation> citation = citationBlock.getCitation();

            if (citation.isPresent() && citation.get().getText().length() > Citation.MAX_TEXT_LENGTH) {
                new Popup().warning(Res.get("validation.tooLong", Citation.MAX_TEXT_LENGTH)).show();
                return;
            }

            if (chatChannel instanceof BisqEasyOfferbookChannel) {
                String dontShowAgainId = "sendMsgOfferOnlyWarn";
                if (settingsService.getOffersOnly().get()) {
                    new Popup().information(Res.get("chat.message.send.offerOnly.warn"))
                            .actionButtonText(Res.get("confirmation.yes"))
                            .onAction(() -> settingsService.setOffersOnly(false))
                            .closeButtonText(Res.get("confirmation.no"))
                            .dontShowAgainId(dontShowAgainId)
                            .show();
                }
                chatService.getBisqEasyOfferbookChannelService().publishChatMessage(text, citation, (BisqEasyOfferbookChannel) chatChannel, userIdentity);
            } else if (chatChannel instanceof BisqEasyOpenTradeChannel) {
                if (settingsService.getTradeRulesConfirmed().get() || ((BisqEasyOpenTradeChannel) chatChannel).isMediator()) {
                    chatService.getBisqEasyOpenTradeChannelService().sendTextMessage(text, citation, (BisqEasyOpenTradeChannel) chatChannel);
                } else {
                    new Popup().information(Res.get("bisqEasy.tradeGuide.notConfirmed.warn"))
                            .actionButtonText(Res.get("bisqEasy.tradeGuide.open"))
                            .onAction(() -> Navigation.navigateTo(NavigationTarget.BISQ_EASY_GUIDE))
                            .show();
                }
            } else if (chatChannel instanceof CommonPublicChatChannel) {
                chatService.getCommonPublicChatChannelServices().get(model.chatChannelDomain).publishChatMessage(text, citation, (CommonPublicChatChannel) chatChannel, userIdentity);
            } else if (chatChannel instanceof TwoPartyPrivateChatChannel) {
                chatService.getTwoPartyPrivateChatChannelServices().get(model.chatChannelDomain).sendTextMessage(text, citation, (TwoPartyPrivateChatChannel) chatChannel);
            }

            citationBlock.close();
        }

        private boolean isOfferAuthor(String offerAuthorUserProfileId) {
            return userIdentityService.isUserIdentityPresent(offerAuthorUserProfileId);
        }

        private void maybeSwitchUserProfile() {
            if (model.userProfileSelectionVisible.get()) {
                List<UserIdentity> myUserProfilesInChannel = getMyUserProfilesInChannel();
                if (myUserProfilesInChannel.size() > 0) {
                    userIdentityService.selectChatUserIdentity(myUserProfilesInChannel.get(0));
                }
            }
        }

        private List<UserIdentity> getMyUserProfilesInChannel() {
            return model.selectedChannel.get().getChatMessages().stream()
                    .sorted(Comparator.comparing(ChatMessage::getDate).reversed())
                    .map(ChatMessage::getAuthorUserProfileId)
                    .map(userIdentityService::findUserIdentity)
                    .filter(Optional::isPresent)
                    .map(Optional::get)
                    .distinct()
                    .collect(Collectors.toList());
        }

        private void enableChatDialog(boolean isEnabled) {
            model.getChatDialogEnabled().set(isEnabled);
        }
    }

    @Getter
    private static class Model implements bisq.desktop.common.view.Model {
        private final BooleanProperty leaveChannelButtonVisible = new SimpleBooleanProperty();
        private final BooleanProperty openDisputeButtonVisible = new SimpleBooleanProperty();
        private final BooleanProperty sendBtcAddressButtonVisible = new SimpleBooleanProperty();
        private final BooleanProperty sendPaymentAccountButtonVisible = new SimpleBooleanProperty();
        private final BooleanProperty paymentAccountSelectionVisible = new SimpleBooleanProperty();
        private final BooleanProperty chatDialogEnabled = new SimpleBooleanProperty(true);
        private final ObservableList<Account<?, ? extends PaymentMethod<?>>> paymentAccounts = FXCollections.observableArrayList();
        private final ObjectProperty<Account<?, ? extends PaymentMethod<?>>> selectedAccount = new SimpleObjectProperty<>();

        private final ObjectProperty<ChatChannel<? extends ChatMessage>> selectedChannel = new SimpleObjectProperty<>();
        private final StringProperty textInput = new SimpleStringProperty("");
        private final BooleanProperty userProfileSelectionVisible = new SimpleBooleanProperty();
        private final ObjectProperty<ChatMessage> moreOptionsVisibleMessage = new SimpleObjectProperty<>(null);
        private final ObservableList<UserProfile> mentionableUsers = FXCollections.observableArrayList();
        private final ObservableList<ChatChannel<?>> mentionableChatChannels = FXCollections.observableArrayList();
        private final ChatChannelDomain chatChannelDomain;
        private final ChatService chatService;
        @Nullable
        private ChatMessage selectedChatMessage;

        private Model(ChatChannelDomain chatChannelDomain, ChatService chatService) {
            this.chatChannelDomain = chatChannelDomain;
            this.chatService = chatService;
        }

        String getChannelTitle(ChatChannel<?> chatChannel) {
            return chatService.findChatChannelService(chatChannel)
                    .map(service -> service.getChannelTitle(chatChannel))
                    .orElse("");
        }

        @Nullable
        public Account<?, ? extends PaymentMethod<?>> getSelectedAccount() {
            return selectedAccount.get();
        }

        public ObjectProperty<Account<?, ? extends PaymentMethod<?>>> selectedAccountProperty() {
            return selectedAccount;
        }

        public void setSelectedAccount(Account<?, ? extends PaymentMethod<?>> selectedAccount) {
            this.selectedAccount.set(selectedAccount);
        }
    }

    @Slf4j
    public static class View extends bisq.desktop.common.view.View<VBox, Model, Controller> {
        private final static double CHAT_BOX_MAX_WIDTH = 1200;
        public final static String EDITED_POST_FIX = " " + Res.get("chat.message.wasEdited");

        private final BisqTextArea inputField = new BisqTextArea();
        private final Button sendButton = new Button();
        private final Pane messagesListView;
        private final VBox emptyMessageList;
        private ChatMentionPopupMenu<UserProfile> userMentionPopup;
        private ChatMentionPopupMenu<ChatChannel<?>> channelMentionPopup;
        private Pane userProfileSelectionRoot;
        private Button leaveChannelButton;

        private View(Model model,
                     Controller controller,
                     Pane messagesListView,
                     Pane quotedMessageBlock,
                     UserProfileSelection userProfileSelection) {
            super(new VBox(), model, controller);

            this.messagesListView = messagesListView;
            VBox.setVgrow(messagesListView, Priority.ALWAYS);

            emptyMessageList = ChatUtil.createEmptyChatPlaceholder(
                    new Label(Res.get("chat.private.messagebox.noChats.placeholder.title")),
                    new Label(Res.get("chat.private.messagebox.noChats.placeholder.description")));

            VBox bottomBarContainer = createAndGetBottomBar(userProfileSelection);

            quotedMessageBlock.setMaxWidth(CHAT_BOX_MAX_WIDTH);

            root.setAlignment(Pos.CENTER);
            root.getChildren().addAll(messagesListView, emptyMessageList, quotedMessageBlock, bottomBarContainer);
        }

        @Override
        protected void onViewAttached() {
            leaveChannelButton.visibleProperty().bind(model.getLeaveChannelButtonVisible());
            leaveChannelButton.managedProperty().bind(model.getLeaveChannelButtonVisible());
            userProfileSelectionRoot.visibleProperty().bind(model.userProfileSelectionVisible);
            userProfileSelectionRoot.managedProperty().bind(model.userProfileSelectionVisible);
            inputField.textProperty().bindBidirectional(model.getTextInput());
            userMentionPopup.filterProperty().bind(Bindings.createStringBinding(
                    () -> StringUtils.deriveWordStartingWith(inputField.getText(), '@'),
                    inputField.textProperty()
            ));
            channelMentionPopup.filterProperty().bind(Bindings.createStringBinding(
                    () -> StringUtils.deriveWordStartingWith(inputField.getText(), '#'),
                    inputField.textProperty()
            ));

            inputField.setOnKeyPressed(event -> {
                if (event.getCode() == KeyCode.ENTER) {
                    event.consume();
                    if (event.isShiftDown()) {
                        inputField.appendText(System.getProperty("line.separator"));
                    } else if (!inputField.getText().isEmpty()) {
                        controller.onSendMessage(inputField.getText().trim());
                        inputField.clear();
                    }
                }
            });
            sendButton.setOnAction(event -> {
                controller.onSendMessage(inputField.getText().trim());
                inputField.clear();
            });
            leaveChannelButton.setOnAction(e -> controller.onLeaveChannel());

            userMentionPopup.setItems(model.mentionableUsers);
            channelMentionPopup.setItems(model.mentionableChatChannels);

            createChatDialogEnabledSubscription();
        }

        @Override
        protected void onViewDetached() {
            leaveChannelButton.visibleProperty().unbind();
            leaveChannelButton.managedProperty().unbind();
            userProfileSelectionRoot.visibleProperty().unbind();
            userProfileSelectionRoot.managedProperty().unbind();
            inputField.textProperty().unbindBidirectional(model.getTextInput());
            userMentionPopup.filterProperty().unbind();
            channelMentionPopup.filterProperty().unbind();
            removeChatDialogEnabledSubscription();

            inputField.setOnKeyPressed(null);
            sendButton.setOnAction(null);
            leaveChannelButton.setOnAction(null);
        }

        private VBox createAndGetBottomBar(UserProfileSelection userProfileSelection) {
            setUpUserProfileSelection(userProfileSelection);
            HBox sendMessageBox = createAndGetSendMessageBox();

            // TODO: Remove this. We need a better solution for this use-case.
            leaveChannelButton = createAndGetChatButton(Res.get("chat.leave"), 120);

            HBox bottomBar = new HBox(10);
            bottomBar.getChildren().addAll(userProfileSelectionRoot, sendMessageBox, leaveChannelButton);
            bottomBar.setMaxWidth(CHAT_BOX_MAX_WIDTH);
            bottomBar.setPadding(new Insets(14, 20, 14, 20));
            bottomBar.setAlignment(Pos.BOTTOM_CENTER);

            VBox bottomBarContainer = new VBox(bottomBar);
            bottomBarContainer.setAlignment(Pos.CENTER);
            return bottomBarContainer;
        }

        private HBox createAndGetSendMessageBox() {
            inputField.setPromptText(Res.get("chat.message.input.prompt"));
            inputField.getStyleClass().addAll("chat-input-field", "normal-text");
            inputField.setPadding(new Insets(5, 0, 5, 5));
            HBox.setMargin(inputField, new Insets(0, 0, 1.5, 0));
            HBox.setHgrow(inputField, Priority.ALWAYS);
            setUpInputFieldAtMentions();

            sendButton.setGraphic(ImageUtil.getImageViewById("chat-send"));
            sendButton.setId("chat-messages-send-button");
            HBox.setMargin(sendButton, new Insets(0, 0, 5, 0));
            sendButton.setMinWidth(30);
            sendButton.setMaxWidth(30);
            sendButton.setTooltip(new BisqTooltip(Res.get("chat.message.input.send"), true));

            HBox sendMessageBox = new HBox(inputField, sendButton);
            sendMessageBox.getStyleClass().add("chat-send-message-box");
            sendMessageBox.setAlignment(Pos.BOTTOM_CENTER);
            HBox.setHgrow(sendMessageBox, Priority.ALWAYS);
            return sendMessageBox;
        }

        private void setUpInputFieldAtMentions() {
            userMentionPopup = new ChatMentionPopupMenu<>(inputField);
            userMentionPopup.setItemDisplayConverter(UserProfile::getUserName);
            userMentionPopup.setSelectionHandler(controller::listUserNamesHandler);

            channelMentionPopup = new ChatMentionPopupMenu<>(inputField);
            channelMentionPopup.setItemDisplayConverter(model::getChannelTitle);
            channelMentionPopup.setSelectionHandler(controller::listChannelsHandler);
        }

        private void setUpUserProfileSelection(UserProfileSelection userProfileSelection) {
            userProfileSelection.setMaxComboBoxWidth(165);
            userProfileSelection.setConverter(new StringConverter<>() {
                @Override
                public String toString(UserProfileSelection.ListItem item) {
                    return item != null ? StringUtils.truncate(item.getUserIdentity().getUserName(), 10) : "";
                }

                @Override
                public UserProfileSelection.ListItem fromString(String string) {
                    return null;
                }
            });
            userProfileSelectionRoot = userProfileSelection.getRoot();
            userProfileSelectionRoot.setMaxHeight(44);
            userProfileSelectionRoot.setMaxWidth(165);
            userProfileSelectionRoot.setMinWidth(165);
            userProfileSelectionRoot.setId("chat-user-profile-bg");
            HBox.setMargin(userProfileSelectionRoot, new Insets(0, -20, 0, -8));
        }

        private void createChatDialogEnabledSubscription() {
            inputField.disableProperty().bind(model.getChatDialogEnabled().not());
            sendButton.disableProperty().bind(model.getChatDialogEnabled().not());
            emptyMessageList.visibleProperty().bind(model.getChatDialogEnabled().not());
            emptyMessageList.managedProperty().bind(model.getChatDialogEnabled().not());
            messagesListView.visibleProperty().bind(model.getChatDialogEnabled());
            messagesListView.managedProperty().bind(model.getChatDialogEnabled());
            userProfileSelectionRoot.disableProperty().bind(model.getChatDialogEnabled().not());
        }

        private void removeChatDialogEnabledSubscription() {
            inputField.disableProperty().unbind();
            sendButton.disableProperty().unbind();
            emptyMessageList.visibleProperty().unbind();
            emptyMessageList.managedProperty().unbind();
            messagesListView.visibleProperty().unbind();
            messagesListView.managedProperty().unbind();
            userProfileSelectionRoot.disableProperty().unbind();
        }
    }

    private static Button createAndGetChatButton(String title, double width) {
        Button button = new Button(title);
        button.setStyle("-fx-label-padding: 0 -30 0 -30; -fx-background-radius: 8; -fx-border-radius: 8;");
        button.setMinHeight(34);
        button.setMinWidth(width);
        button.getStyleClass().add("outlined-button");
        return button;
    }
}
