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

package bisq.desktop.primary.main.content.social.components;

import bisq.common.data.ByteArray;
import bisq.desktop.components.containers.Spacer;
import bisq.desktop.components.robohash.RoboHash;
import bisq.i18n.Res;
import bisq.social.chat.ChatService;
import bisq.social.user.ChatUserProfile;
import javafx.beans.property.*;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.Pane;
import javafx.scene.layout.VBox;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.fxmisc.easybind.EasyBind;
import org.fxmisc.easybind.Subscription;

import java.util.Optional;
import java.util.function.Consumer;
import java.util.stream.Collectors;

@Slf4j
public class ChatUserDetails implements Comparable<ChatUserDetails> {
    private final Controller controller;

    public ChatUserDetails(ChatService chatService, ChatUserProfile chatUserProfile) {
        controller = new Controller(chatService, chatUserProfile);
    }

    public Pane getRoot() {
        return controller.view.getRoot();
    }

    public void setOnMentionUser(Consumer<ChatUserProfile> handler) {
        controller.model.mentionUserHandler = Optional.ofNullable(handler);
    }

    public void setOnSendPrivateMessage(Consumer<ChatUserProfile> handler) {
        controller.model.sendPrivateMessageHandler = Optional.ofNullable(handler);
    }

    public void setOnIgnoreChatUser(Runnable handler) {
        controller.model.ignoreChatUserHandler = Optional.ofNullable(handler);
    }

    @Override
    public int compareTo(ChatUserDetails o) {
        return controller.model.chatUserProfile.getProfileId().compareTo(o.controller.model.chatUserProfile.getProfileId());
    }

    private static class Controller implements bisq.desktop.common.view.Controller {
        private final Model model;
        @Getter
        private final View view;


        private Controller(ChatService chatService, ChatUserProfile chatUserProfile) {
            model = new Model(chatService, chatUserProfile);
            view = new View(model, this);
        }

        @Override
        public void onActivate() {
            ChatUserProfile chatUserProfile = model.chatUserProfile;
            if (chatUserProfile == null) {
                return;
            }

            model.id.set(Res.get("social.createUserProfile.id", chatUserProfile.getId()));
            model.userName.set(chatUserProfile.getProfileId());
            model.roboHashNode.set(RoboHash.getImage(new ByteArray(chatUserProfile.getPubKeyHash())));
            String entitledRoles = chatUserProfile.getRoles().stream().map(e -> Res.get(e.type().name())).collect(Collectors.joining(", "));
            model.entitlements.set(Res.get("social.createUserProfile.entitledRoles", entitledRoles));
            model.entitlementsVisible.set(!chatUserProfile.getRoles().isEmpty());
        }

        @Override
        public void onDeactivate() {
        }

        public void onSendPrivateMessage() {
            model.sendPrivateMessageHandler.ifPresent(handler -> handler.accept(model.chatUserProfile));
        }

        public void onMentionUser() {
            model.mentionUserHandler.ifPresent(handler -> handler.accept(model.chatUserProfile));
        }

        public void onIgnoreUser() {
            model.chatService.ignoreChatUser(model.chatUserProfile);
            model.ignoreChatUserHandler.ifPresent(Runnable::run);
        }

        public void onReportUser() {
            // todo open popup for editing reason
            model.chatService.reportChatUser(model.chatUserProfile, "");
        }
    }

    private static class Model implements bisq.desktop.common.view.Model {
        private final ChatService chatService;
        private final ChatUserProfile chatUserProfile;
        private Optional<Consumer<ChatUserProfile>> mentionUserHandler = Optional.empty();
        private Optional<Consumer<ChatUserProfile>> sendPrivateMessageHandler = Optional.empty();
        private Optional<Runnable> ignoreChatUserHandler = Optional.empty();
        private final ObjectProperty<Image> roboHashNode = new SimpleObjectProperty<>();
        private final StringProperty userName = new SimpleStringProperty();
        private final StringProperty id = new SimpleStringProperty();
        private final BooleanProperty entitlementsVisible = new SimpleBooleanProperty();
        private final StringProperty entitlements = new SimpleStringProperty();

        private Model(ChatService chatService, ChatUserProfile chatUserProfile) {
            this.chatService = chatService;
            this.chatUserProfile = chatUserProfile;
        }
    }

    @Slf4j
    public static class View extends bisq.desktop.common.view.View<VBox, Model, Controller> {
        private final ImageView roboIconImageView;
        private final Label userName, id, entitlements;
        private final Button openPrivateMessageButton, mentionButton, ignoreButton, reportButton;
        private Subscription roboHashNodeSubscription;

        private View(Model model, Controller controller) {
            super(new VBox(), model, controller);

            root.setSpacing(10);
            root.setAlignment(Pos.TOP_LEFT);

            roboIconImageView = new ImageView();
            VBox.setMargin(roboIconImageView, new Insets(0, 0, 0, 0));

            userName = new Label();
            userName.setStyle("-fx-background-color: -bisq-grey-2;-fx-text-fill: -fx-light-text-color;");
            userName.setMaxWidth(300);
            userName.setMinWidth(300);
            userName.setPadding(new Insets(7, 7, 7, 7));
            VBox.setMargin(userName, new Insets(-10, 0, 0, 0));

            id = new Label();
            id.getStyleClass().add("offer-label-small"); //todo
            id.setPadding(new Insets(-5, 0, 0, 5));

            entitlements = new Label();
            entitlements.getStyleClass().add("offer-label-small"); //todo
            entitlements.setPadding(new Insets(-5, 0, 0, 0));

            //todo add reputation data. need more concept work

            openPrivateMessageButton = new Button(Res.get("social.sendPrivateMessage"));
            mentionButton = new Button(Res.get("social.mention"));
            ignoreButton = new Button(Res.get("social.ignore"));
            reportButton = new Button(Res.get("social.report"));

            root.getChildren().addAll(roboIconImageView, userName, id, entitlements, Spacer.height(10),
                    openPrivateMessageButton, mentionButton, ignoreButton, reportButton);
        }

        @Override
        protected void onViewAttached() {
            userName.textProperty().bind(model.userName);
            id.textProperty().bind(model.id);
            entitlements.textProperty().bind(model.entitlements);
            entitlements.visibleProperty().bind(model.entitlementsVisible);
            entitlements.managedProperty().bind(model.entitlementsVisible);
            mentionButton.minWidthProperty().bind(openPrivateMessageButton.widthProperty());
            ignoreButton.minWidthProperty().bind(openPrivateMessageButton.widthProperty());
            reportButton.minWidthProperty().bind(openPrivateMessageButton.widthProperty());

            roboHashNodeSubscription = EasyBind.subscribe(model.roboHashNode, roboIcon -> {
                if (roboIcon != null) {
                    roboIconImageView.setImage(roboIcon);
                }
            });

            openPrivateMessageButton.setOnAction(e -> controller.onSendPrivateMessage());
            mentionButton.setOnAction(e -> controller.onMentionUser());
            ignoreButton.setOnAction(e -> controller.onIgnoreUser());
            reportButton.setOnAction(e -> controller.onReportUser());
        }

        @Override
        protected void onViewDetached() {
            userName.textProperty().unbind();
            id.textProperty().unbind();
            entitlements.textProperty().unbind();
            entitlements.visibleProperty().unbind();
            entitlements.managedProperty().unbind();
            mentionButton.minWidthProperty().unbind();
            ignoreButton.minWidthProperty().unbind();
            reportButton.minWidthProperty().unbind();

            roboHashNodeSubscription.unsubscribe();

            openPrivateMessageButton.setOnAction(null);
            mentionButton.setOnAction(null);
            ignoreButton.setOnAction(null);
            reportButton.setOnAction(null);
        }
    }
}