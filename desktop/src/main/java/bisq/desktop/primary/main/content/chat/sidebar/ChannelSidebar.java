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

package bisq.desktop.primary.main.content.chat.sidebar;

import bisq.application.DefaultApplicationService;
import bisq.chat.ChatService;
import bisq.chat.bisqeasy.channel.pub.BisqEasyPublicChatChannel;
import bisq.chat.channel.ChatChannel;
import bisq.chat.channel.pub.CommonPublicChatChannel;
import bisq.chat.message.ChatMessage;
import bisq.common.observable.Pin;
import bisq.common.observable.collection.CollectionObserver;
import bisq.desktop.common.threading.UIThread;
import bisq.desktop.components.containers.Spacer;
import bisq.desktop.components.controls.BisqIconButton;
import bisq.desktop.primary.main.content.components.ChatUserOverview;
import bisq.i18n.Res;
import bisq.user.profile.UserProfile;
import bisq.user.profile.UserProfileService;
import javafx.beans.property.*;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.SortedList;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Cursor;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.scene.text.Text;
import javafx.util.Callback;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.fxmisc.easybind.EasyBind;

import javax.annotation.Nullable;
import java.util.Comparator;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

@Slf4j
public class ChannelSidebar {
    private final Controller controller;

    public ChannelSidebar(DefaultApplicationService applicationService, Runnable closeHandler) {
        controller = new Controller(applicationService, closeHandler);
    }

    public Pane getRoot() {
        return controller.view.getRoot();
    }

    public void setChannel(@Nullable ChatChannel<? extends ChatMessage> chatChannel) {
        controller.setChannel(chatChannel);
    }

    public void setOnUndoIgnoreChatUser(Runnable handler) {
        controller.model.undoIgnoreChatUserHandler = Optional.ofNullable(handler);
    }

    private static class Controller implements bisq.desktop.common.view.Controller {
        private final Model model;
        @Getter
        private final View view;
        private final UserProfileService userProfileService;
        private final Runnable closeHandler;
        private final NotificationsSidebar notificationsSidebar;
        private final ChatService chatService;
        @Nullable
        private Pin userProfileIdsOfParticipantsPin;

        private Controller(DefaultApplicationService applicationService, Runnable closeHandler) {
            this.closeHandler = closeHandler;
            userProfileService = applicationService.getUserService().getUserProfileService();
            chatService = applicationService.getChatService();
            notificationsSidebar = new NotificationsSidebar(chatService);
            model = new Model();
            view = new View(model, this, notificationsSidebar.getRoot());
        }

        @Override
        public void onActivate() {
            model.getSortedListParticipantList()
                    .sort(Comparator.comparing(item -> item.getChatUser().getUserName()));
        }

        @Override
        public void onDeactivate() {
            if (userProfileIdsOfParticipantsPin != null) {
                userProfileIdsOfParticipantsPin.unbind();
            }
        }

        void setChannel(@Nullable ChatChannel<? extends ChatMessage> chatChannel) {
            notificationsSidebar.setChannel(chatChannel);

            if (chatChannel == null) {
                model.descriptionVisible.set(false);
                model.description.set(null);
                model.adminProfile = Optional.empty();
                model.moderators.clear();
                model.channel.set(null);
                return;
            }

            Set<String> ignoredChatUserIds = new HashSet<>(userProfileService.getIgnoredUserProfileIds());

            model.channelTitle.set(chatService.findChatChannelService(chatChannel)
                    .map(service -> service.getChannelTitle(chatChannel))
                    .orElse(""));

            model.participantList.clear();
            if (userProfileIdsOfParticipantsPin != null) {
                userProfileIdsOfParticipantsPin.unbind();
            }
            userProfileIdsOfParticipantsPin = chatChannel.getUserProfileIdsOfParticipants().addListener(new CollectionObserver<>() {
                @Override
                public void add(String userProfileId) {
                    UIThread.run(() ->
                            userProfileService.findUserProfile(userProfileId)
                                    .ifPresent(userProfile -> model.participantList.add(new ChatUserOverview(userProfile,
                                            ignoredChatUserIds.contains(userProfileId)))));
                }

                @Override
                public void remove(Object element) {
                    if (element instanceof String) {
                        String userProfileId = (String) element;
                        UIThread.run(() ->
                                model.participantList.stream()
                                        .filter(item -> item.getChatUser().getId().equals(userProfileId))
                                        .findFirst()
                                        .ifPresent(model.participantList::remove));
                    }
                }

                @Override
                public void clear() {
                    UIThread.run(model.participantList::clear);
                }
            });

            if (chatChannel instanceof CommonPublicChatChannel) {
                CommonPublicChatChannel commonPublicChatChannel = (CommonPublicChatChannel) chatChannel;
                model.description.set(commonPublicChatChannel.getDescription());
                model.descriptionVisible.set(true);
                model.adminProfile = commonPublicChatChannel.getChannelAdminId()
                        .flatMap(channelAdmin -> userProfileService.findUserProfile(channelAdmin).map(ChatUserOverview::new))
                        .stream()
                        .findAny();
                model.moderators.setAll(commonPublicChatChannel.getChannelModeratorIds().stream()
                        .flatMap(id -> userProfileService.findUserProfile(id).stream())
                        .map(ChatUserOverview::new)
                        .sorted()
                        .collect(Collectors.toList()));
            } else if (chatChannel instanceof BisqEasyPublicChatChannel) {
                model.description.set(((BisqEasyPublicChatChannel) chatChannel).getDescription());
                model.descriptionVisible.set(true);
                model.adminProfile = Optional.empty();
                model.moderators.clear();
            } else {
                model.descriptionVisible.set(false);
                model.description.set(null);
                model.adminProfile = Optional.empty();
                model.moderators.clear();
            }

            model.channel.set(chatChannel);
        }

        void onUndoIgnoreUser(UserProfile userProfile) {
            userProfileService.undoIgnoreUserProfile(userProfile);
            model.undoIgnoreChatUserHandler.ifPresent(Runnable::run);
        }

        void onClose() {
            closeHandler.run();
        }
    }

    @Getter
    private static class Model implements bisq.desktop.common.view.Model {
        private final ObjectProperty<ChatChannel<? extends ChatMessage>> channel = new SimpleObjectProperty<>();
        private final StringProperty channelTitle = new SimpleStringProperty();
        private final StringProperty description = new SimpleStringProperty();
        private final BooleanProperty descriptionVisible = new SimpleBooleanProperty();
        private final ObservableList<ChatUserOverview> moderators = FXCollections.observableArrayList();
        private Optional<ChatUserOverview> adminProfile = Optional.empty();
        private final ObservableList<ChatUserOverview> participantList = FXCollections.observableArrayList();
        private final SortedList<ChatUserOverview> sortedListParticipantList = new SortedList<>(participantList);
        private Optional<Runnable> undoIgnoreChatUserHandler = Optional.empty();

        private Model() {
        }
    }

    @Slf4j
    public static class View extends bisq.desktop.common.view.View<VBox, Model, Controller> {
        private final ListView<ChatUserOverview> participants;
        private final Label headline;
        private final Text descriptionText;
        private final Button closeButton;

        private View(Model model, Controller controller, Pane notificationsSidebar) {
            super(new VBox(), model, controller);

            root.setSpacing(15);
            root.setMinWidth(260);
            root.setPadding(new Insets(0, 20, 20, 20));

            closeButton = BisqIconButton.createIconButton("close");

            headline = new Label();
            headline.setId("chat-sidebar-headline");

            HBox.setMargin(headline, new Insets(18, 0, 0, 0));
            HBox.setMargin(closeButton, new Insets(10, 10, 0, 0));
            HBox topHBox = new HBox(headline, Spacer.fillHBox(), closeButton);

            descriptionText = new Text();
            descriptionText.setId("chat-sidebar-text");

            Label participantsLabel = new Label(Res.get("social.channel.settings.participants"));
            participantsLabel.setId("chat-sidebar-title");

            participants = new ListView<>(model.getSortedListParticipantList());
            VBox.setVgrow(participants, Priority.ALWAYS);
            participants.setCellFactory(getCellFactory(controller));

            VBox.setMargin(topHBox, new Insets(0, -20, 0, 0));
            VBox.setMargin(notificationsSidebar, new Insets(20, 0, 20, 0));
            root.getChildren().addAll(topHBox, descriptionText, notificationsSidebar, participantsLabel, participants);
        }

        @Override
        protected void onViewAttached() {
            headline.textProperty().bind(model.channelTitle);
            descriptionText.textProperty().bind(model.description);
            descriptionText.visibleProperty().bind(model.descriptionVisible);
            descriptionText.managedProperty().bind(model.descriptionVisible);
            EasyBind.subscribe(root.widthProperty(), w -> {
                double width = w.doubleValue();
                if (width > 0) {
                    descriptionText.setWrappingWidth(width - 20);
                }
            });
            closeButton.setOnAction(e -> controller.onClose());
        }

        @Override
        protected void onViewDetached() {
            headline.textProperty().unbind();
            descriptionText.textProperty().unbind();
            descriptionText.visibleProperty().unbind();
            descriptionText.managedProperty().unbind();
            closeButton.setOnAction(null);
        }

        private Callback<ListView<ChatUserOverview>, ListCell<ChatUserOverview>> getCellFactory(Controller controller) {
            return new Callback<>() {
                @Override
                public ListCell<ChatUserOverview> call(ListView<ChatUserOverview> list) {
                    return new ListCell<>() {
                        final Hyperlink undoIgnoreUserButton = new Hyperlink(Res.get("social.undoIgnore"));
                        final HBox hBox = new HBox();

                        {
                            hBox.setAlignment(Pos.CENTER_LEFT);
                            hBox.setFillHeight(true);
                            hBox.setPadding(new Insets(10, 10, 0, -10));
                            hBox.setCursor(Cursor.HAND);
                        }

                        @Override
                        public void updateItem(final ChatUserOverview chatUserOverview, boolean empty) {
                            super.updateItem(chatUserOverview, empty);
                            if (chatUserOverview != null && !empty) {
                                undoIgnoreUserButton.setOnAction(e -> {
                                    controller.onUndoIgnoreUser(chatUserOverview.getChatUser());
                                    participants.refresh();
                                });
                                undoIgnoreUserButton.setVisible(chatUserOverview.isIgnored());
                                undoIgnoreUserButton.setManaged(chatUserOverview.isIgnored());

                                Pane chatUserOverviewRoot = chatUserOverview.getRoot();
                                chatUserOverviewRoot.setOpacity(chatUserOverview.isIgnored() ? 0.4 : 1);

                                hBox.getChildren().setAll(chatUserOverviewRoot, Spacer.fillHBox(), undoIgnoreUserButton);

                                setGraphic(hBox);
                            } else {
                                if (undoIgnoreUserButton != null) {
                                    undoIgnoreUserButton.setOnAction(null);
                                }
                                setGraphic(null);
                            }
                        }

                    };
                }
            };
        }
    }
}