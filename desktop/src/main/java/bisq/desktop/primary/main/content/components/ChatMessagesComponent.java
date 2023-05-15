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

package bisq.desktop.primary.main.content.components;

import bisq.application.DefaultApplicationService;
import bisq.chat.ChatService;
import bisq.chat.bisqeasy.channel.priv.BisqEasyPrivateTradeChatChannel;
import bisq.chat.bisqeasy.channel.pub.BisqEasyPublicChatChannel;
import bisq.chat.channel.ChatChannel;
import bisq.chat.channel.ChatChannelDomain;
import bisq.chat.channel.ChatChannelSelectionService;
import bisq.chat.channel.priv.TwoPartyPrivateChatChannel;
import bisq.chat.channel.pub.CommonPublicChatChannel;
import bisq.chat.channel.pub.PublicChatChannel;
import bisq.chat.message.ChatMessage;
import bisq.chat.message.Citation;
import bisq.common.observable.Pin;
import bisq.common.util.StringUtils;
import bisq.desktop.common.utils.ImageUtil;
import bisq.desktop.components.controls.BisqTextArea;
import bisq.desktop.components.overlay.Popup;
import bisq.i18n.Res;
import bisq.settings.SettingsService;
import bisq.user.identity.UserIdentity;
import bisq.user.identity.UserIdentityService;
import bisq.user.profile.UserProfile;
import bisq.user.profile.UserProfileService;
import javafx.beans.binding.Bindings;
import javafx.beans.property.*;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.*;
import javafx.util.StringConverter;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

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

    public ChatMessagesComponent(DefaultApplicationService applicationService, ChatChannelDomain chatChannelDomain) {
        controller = new Controller(applicationService, chatChannelDomain);
    }

    public Pane getRoot() {
        return controller.view.getRoot();
    }

    public HBox getBottomHBox() {
        return controller.view.getBottomHBox();
    }

    public void mentionUser(UserProfile userProfile) {
        controller.mentionUser(userProfile);
    }

    public FilteredList<ChatMessagesListView.ChatMessageListItem<? extends ChatMessage>> getFilteredChatMessages() {
        return controller.chatMessagesListView.getFilteredChatMessages();
    }

    public void setSearchPredicate(Predicate<? super ChatMessagesListView.ChatMessageListItem<? extends ChatMessage>> predicate) {
        controller.chatMessagesListView.setSearchPredicate(predicate);
    }

    public void setOnShowChatUserDetails(Consumer<UserProfile> handler) {
        controller.model.showChatUserDetailsHandler = Optional.of(handler);
    }

    public void resetSelectedChatMessage() {
        controller.model.selectedChatMessage = null;
    }

    public void openPrivateChannel(UserProfile peer) {
        controller.createAndSelectTwoPartyPrivateChatChannel(peer);
    }

    public void refreshMessages() {
        controller.chatMessagesListView.refreshMessages();
    }

    private static class Controller implements bisq.desktop.common.view.Controller {
        private final Model model;
        @Getter
        private final View view;
        private final UserIdentityService userIdentityService;
        private final QuotedMessageBlock citationBlock;
        private final ChatMessagesListView chatMessagesListView;
        private final UserProfileService userProfileService;
        private final SettingsService settingsService;
        private final ChatService chatService;
        private Pin selectedChannelPin;
        private Pin chatMessagesPin;

        private Controller(DefaultApplicationService applicationService,
                           ChatChannelDomain chatChannelDomain) {
            chatService = applicationService.getChatService();


            settingsService = applicationService.getSettingsService();
            userIdentityService = applicationService.getUserService().getUserIdentityService();
            userProfileService = applicationService.getUserService().getUserProfileService();
            citationBlock = new QuotedMessageBlock(applicationService);

            UserProfileSelection userProfileSelection = new UserProfileSelection(userIdentityService);

            chatMessagesListView = new ChatMessagesListView(applicationService,
                    this::mentionUser,
                    this::showChatUserDetails,
                    this::onReply,
                    chatChannelDomain);

            model = new Model(chatChannelDomain);
            view = new View(model, this,
                    chatMessagesListView.getRoot(),
                    citationBlock.getRoot(),
                    userProfileSelection);
        }

        @Override
        public void onActivate() {
            model.mentionableUsers.setAll(userProfileService.getUserProfiles());

            //todo
            //model.mentionableChatChannels.setAll(publicDiscussionChannelService.getMentionableChannels());

            ChatChannelSelectionService chatChannelSelectionService = chatService.getChatChannelSelectionServices().get(model.getChatChannelDomain());
            selectedChannelPin = chatChannelSelectionService.getSelectedChannel().addObserver(this::applySelectedChannel);

            Optional.ofNullable(model.selectedChatMessage).ifPresent(this::showChatUserDetails);

            userIdentityService.getUserIdentityChangedFlag().addObserver(__ -> applyUserProfileOrChannelChange());
        }

        @Override
        public void onDeactivate() {
            selectedChannelPin.unbind();
            if (chatMessagesPin != null) {
                chatMessagesPin.unbind();
            }
        }


        ///////////////////////////////////////////////////////////////////////////////////////////////////
        // UI
        ///////////////////////////////////////////////////////////////////////////////////////////////////


        private void onSendMessage(String text) {
            if (text == null || text.isEmpty()) {
                return;
            }

            if (model.selectedChannel.get() instanceof PublicChatChannel) {
                List<UserIdentity> myUserProfilesInChannel = getMyUserProfilesInChannel();
                if (myUserProfilesInChannel.size() > 0) {
                    UserIdentity lastUsedUserProfile = myUserProfilesInChannel.get(0);
                    if (!lastUsedUserProfile.equals(userIdentityService.getSelectedUserIdentity())) {
                        new Popup().information(Res.get("chat.sendMessage.differentUserProfile.popup"))
                                .closeButtonText(Res.get("no"))
                                .actionButtonText(Res.get("yes"))
                                .onAction(() -> doSendMessage(text))
                                .show();
                        return;
                    }
                }
            }

            doSendMessage(text);
        }

        private void doSendMessage(String text) {
            ChatChannel<? extends ChatMessage> chatChannel = model.selectedChannel.get();
            UserIdentity userIdentity = checkNotNull(userIdentityService.getSelectedUserIdentity());
            Optional<Citation> citation = citationBlock.getCitation();

            if (chatChannel instanceof BisqEasyPublicChatChannel) {
                String dontShowAgainId = "sendMsgOfferOnlyWarn";
                if (settingsService.getOffersOnly().get()) {
                    new Popup().information(Res.get("social.chat.sendMsg.offerOnly.popup"))
                            .actionButtonText(Res.get("yes"))
                            .onAction(() -> settingsService.setOffersOnly(false))
                            .closeButtonText(Res.get("no"))
                            .dontShowAgainId(dontShowAgainId)
                            .show();
                }
                chatService.getBisqEasyPublicChatChannelService().publishChatMessage(text, citation, (BisqEasyPublicChatChannel) chatChannel, userIdentity);
            } else if (chatChannel instanceof BisqEasyPrivateTradeChatChannel) {
                if (settingsService.getTradeRulesConfirmed().get() || ((BisqEasyPrivateTradeChatChannel) chatChannel).isMediator()) {
                    chatService.getBisqEasyPrivateTradeChatChannelService().sendTextMessage(text, citation, (BisqEasyPrivateTradeChatChannel) chatChannel);
                } else {
                    new Popup().information(Res.get("social.chat.sendMsg.tradeRulesNotConfirmed.popup")).show();
                }
            } else if (chatChannel instanceof CommonPublicChatChannel) {
                chatService.getCommonPublicChatChannelServices().get(model.chatChannelDomain).publishChatMessage(text, citation, (CommonPublicChatChannel) chatChannel, userIdentity);
            } else if (chatChannel instanceof TwoPartyPrivateChatChannel) {
                chatService.getTwoPartyPrivateChatChannelServices().get(model.chatChannelDomain).sendTextMessage(text, citation, (TwoPartyPrivateChatChannel) chatChannel);
            }

            citationBlock.close();
        }

        private void onReply(ChatMessage chatMessage) {
            if (!chatMessage.isMyMessage(userIdentityService)) {
                citationBlock.reply(chatMessage);
            }
        }

        private void fillUserMention(UserProfile user) {
            String content = model.getTextInput().get().replaceAll("@[a-zA-Z\\d]*$", "@" + user.getUserName() + " ");
            model.getTextInput().set(content);
            //todo
            view.inputField.positionCaret(content.length());
        }

        private void fillChannelMention(ChatChannel<?> chatChannel) {
            String channelTitle = chatService.findChatChannelService(chatChannel)
                    .map(service -> service.getChannelTitle(chatChannel))
                    .orElse("");
            String content = model.getTextInput().get().replaceAll("#[a-zA-Z\\d]*$", "#" + channelTitle + " ");
            model.getTextInput().set(content);
            //todo
            view.inputField.positionCaret(content.length());
        }

        private void createAndSelectTwoPartyPrivateChatChannel(UserProfile peer) {
            chatService.createAndSelectTwoPartyPrivateChatChannel(model.getChatChannelDomain(), peer);
        }

        private void showChatUserDetails(ChatMessage chatMessage) {
            model.selectedChatMessage = chatMessage;
            userProfileService.findUserProfile(chatMessage.getAuthorUserProfileId()).ifPresent(author ->
                    model.showChatUserDetailsHandler.ifPresent(handler -> handler.accept(author)));
        }

        private void mentionUser(UserProfile userProfile) {
            String existingText = model.getTextInput().get();
            if (!existingText.isEmpty() && !existingText.endsWith(" ")) {
                existingText += " ";
            }
            model.getTextInput().set(existingText + "@" + userProfile.getUserName() + " ");
        }

        private void applySelectedChannel(ChatChannel<? extends ChatMessage> chatChannel) {
            model.selectedChannel.set(chatChannel);
            applyUserProfileOrChannelChange();
        }

        private void applyUserProfileOrChannelChange() {
            boolean multipleProfiles = userIdentityService.getUserIdentities().size() > 1;
            ChatChannel<?> selectedChatChannel = model.selectedChannel.get();
            model.userProfileSelectionVisible.set(multipleProfiles && selectedChatChannel instanceof PublicChatChannel);

            if (chatMessagesPin != null) {
                chatMessagesPin.unbind();
            }

            if (selectedChatChannel != null) {
                chatMessagesPin = selectedChatChannel.getChatMessages().addListener(this::maybeSwitchUserProfile);
            }
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

        public String getChannelTitle(ChatChannel<?> chatChannel) {
            return chatService.findChatChannelService(chatChannel)
                    .map(service -> service.getChannelTitle(chatChannel))
                    .orElse("");
        }
    }

    @Getter
    private static class Model implements bisq.desktop.common.view.Model {
        private final ObjectProperty<ChatChannel<?>> selectedChannel = new SimpleObjectProperty<>();
        private final StringProperty textInput = new SimpleStringProperty("");
        private final BooleanProperty userProfileSelectionVisible = new SimpleBooleanProperty();
        private final ObjectProperty<ChatMessage> moreOptionsVisibleMessage = new SimpleObjectProperty<>(null);
        private final ObservableList<UserProfile> mentionableUsers = FXCollections.observableArrayList();
        private final ObservableList<ChatChannel<?>> mentionableChatChannels = FXCollections.observableArrayList();
        private final ChatChannelDomain chatChannelDomain;
        @Nullable
        private ChatMessage selectedChatMessage;
        private Optional<Consumer<UserProfile>> showChatUserDetailsHandler = Optional.empty();

        private Model(ChatChannelDomain chatChannelDomain) {
            this.chatChannelDomain = chatChannelDomain;
        }
    }

    @Slf4j
    public static class View extends bisq.desktop.common.view.View<VBox, Model, Controller> {
        public final static String EDITED_POST_FIX = " " + Res.get("social.message.wasEdited");

        private final BisqTextArea inputField;
        private final Button sendButton;
        private final ChatMentionPopupMenu<UserProfile> userMentionPopup;
        private final ChatMentionPopupMenu<ChatChannel<?>> channelMentionPopup;
        private final Pane userProfileSelectionRoot;
        @Getter
        private final HBox bottomHBox = new HBox(10);

        private View(Model model,
                     Controller controller,
                     Pane messagesListView,
                     Pane quotedMessageBlock,
                     UserProfileSelection userProfileSelection) {
            super(new VBox(), model, controller);

            inputField = new BisqTextArea();
            inputField.setId("chat-input-field");
            inputField.setPromptText(Res.get("social.chat.input.prompt"));

            sendButton = new Button("", ImageUtil.getImageViewById("chat-send"));
            sendButton.setId("chat-messages-send-button");
            sendButton.setPadding(new Insets(5));
            sendButton.setMinWidth(31);
            sendButton.setMaxWidth(31);

            StackPane.setAlignment(inputField, Pos.CENTER_LEFT);
            StackPane.setAlignment(sendButton, Pos.CENTER_RIGHT);
            StackPane.setMargin(sendButton, new Insets(0, 10, 0, 0));
            StackPane bottomBoxStackPane = new StackPane(inputField, sendButton);

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

            HBox.setHgrow(bottomBoxStackPane, Priority.ALWAYS);
            HBox.setMargin(userProfileSelectionRoot, new Insets(0, -20, 0, -25));
            bottomHBox.getChildren().addAll(userProfileSelectionRoot, bottomBoxStackPane);
            bottomHBox.getStyleClass().add("bg-grey-5");
            bottomHBox.setAlignment(Pos.CENTER);
            bottomHBox.setPadding(new Insets(14, 25, 14, 25));

            VBox.setVgrow(messagesListView, Priority.ALWAYS);
            root.getChildren().addAll(messagesListView, quotedMessageBlock, bottomHBox);

            userMentionPopup = new ChatMentionPopupMenu<>(inputField);
            userMentionPopup.setItemDisplayConverter(UserProfile::getUserName);
            userMentionPopup.setSelectionHandler(controller::fillUserMention);

            channelMentionPopup = new ChatMentionPopupMenu<>(inputField);
            channelMentionPopup.setItemDisplayConverter(controller::getChannelTitle);
            channelMentionPopup.setSelectionHandler(controller::fillChannelMention);
        }

        @Override
        protected void onViewAttached() {
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

            userMentionPopup.setItems(model.mentionableUsers);
            channelMentionPopup.setItems(model.mentionableChatChannels);
        }

        @Override
        protected void onViewDetached() {
            userProfileSelectionRoot.visibleProperty().unbind();
            userProfileSelectionRoot.managedProperty().unbind();
            inputField.textProperty().unbindBidirectional(model.getTextInput());
            userMentionPopup.filterProperty().unbind();
            channelMentionPopup.filterProperty().unbind();

            inputField.setOnKeyPressed(null);
            sendButton.setOnAction(null);
        }
    }
}