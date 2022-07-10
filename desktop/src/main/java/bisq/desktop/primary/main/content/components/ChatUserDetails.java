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

import bisq.desktop.common.utils.Layout;
import bisq.desktop.components.robohash.RoboHash;
import bisq.i18n.Res;
import bisq.chat.ChatService;
import bisq.user.profile.UserProfile;
import javafx.beans.property.*;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.Pane;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.fxmisc.easybind.EasyBind;
import org.fxmisc.easybind.Subscription;

import java.util.Optional;
import java.util.function.Consumer;

@Slf4j
public class ChatUserDetails implements Comparable<ChatUserDetails> {
    private final Controller controller;

    public ChatUserDetails(ChatService chatService, UserProfile userProfile) {
        controller = new Controller(chatService, userProfile);
    }

    public Pane getRoot() {
        return controller.view.getRoot();
    }

    public void setOnMentionUserHandler(Consumer<UserProfile> handler) {
        controller.model.mentionUserHandler = Optional.ofNullable(handler);
    }

    public void setOnSendPrivateMessageHandler(Consumer<UserProfile> handler) {
        controller.model.sendPrivateMessageHandler = Optional.ofNullable(handler);
    }

    public void setIgnoreUserStateHandler(Runnable handler) {
        controller.model.ignoreUserStateHandler = Optional.ofNullable(handler);
    }

    @Override
    public int compareTo(ChatUserDetails o) {
        return controller.model.userProfile.getUserName().compareTo(o.controller.model.userProfile.getUserName());
    }

    private static class Controller implements bisq.desktop.common.view.Controller {
        private final Model model;
        @Getter
        private final View view;


        private Controller(ChatService chatService, UserProfile userProfile) {
            model = new Model(chatService, userProfile);
            view = new View(model, this);
        }

        @Override
        public void onActivate() {
            UserProfile userProfile = model.userProfile;
            if (userProfile == null) {
                return;
            }

            model.ignoreButtonText.set(Res.get("social.ignore"));
            model.id.set(Res.get("social.createUserProfile.id", userProfile.getId()));
            model.bio.set(userProfile.getBio());
            model.terms.set(userProfile.getTerms());
            model.reputationScore.set(userProfile.getBurnScoreAsString());
            model.profileAge.set(userProfile.getAccountAgeAsString());

            model.nym.set(userProfile.getNym());
            model.nickName.set(userProfile.getNickName());
            model.roboHashNode.set(RoboHash.getImage(userProfile.getPubKeyHash()));
        }

        @Override
        public void onDeactivate() {
        }

        public void onSendPrivateMessage() {
            model.sendPrivateMessageHandler.ifPresent(handler -> handler.accept(model.userProfile));
        }

        public void onMentionUser() {
            model.mentionUserHandler.ifPresent(handler -> handler.accept(model.userProfile));
        }

        public void onToggleIgnoreUser() {
            model.ignoreUserSelected.set(!model.ignoreUserSelected.get());
            if (model.ignoreUserSelected.get()) {
                model.chatService.ignoreChatUser(model.userProfile);
                model.ignoreButtonText.set(Res.get("social.undoIgnore"));
            } else {
                model.chatService.undoIgnoreChatUser(model.userProfile);
                model.ignoreButtonText.set(Res.get("social.ignore"));
            }
            model.ignoreUserStateHandler.ifPresent(Runnable::run);
        }

        public void onReportUser() {
            // todo open popup for editing reason
            model.chatService.reportChatUser(model.userProfile, "");
        }
    }

    private static class Model implements bisq.desktop.common.view.Model {
        private final ChatService chatService;
        private final UserProfile userProfile;
        private Optional<Consumer<UserProfile>> mentionUserHandler = Optional.empty();
        private Optional<Consumer<UserProfile>> sendPrivateMessageHandler = Optional.empty();
        private Optional<Runnable> ignoreUserStateHandler = Optional.empty();
        private final ObjectProperty<Image> roboHashNode = new SimpleObjectProperty<>();
        private final StringProperty nym = new SimpleStringProperty();
        private final StringProperty nickName = new SimpleStringProperty();
        private final StringProperty id = new SimpleStringProperty();
        private final StringProperty bio = new SimpleStringProperty();
        private final StringProperty terms = new SimpleStringProperty();
        private final StringProperty reputationScore = new SimpleStringProperty();
        private final StringProperty profileAge = new SimpleStringProperty();
        private final BooleanProperty ignoreUserSelected = new SimpleBooleanProperty();
        private final StringProperty ignoreButtonText = new SimpleStringProperty();

        private Model(ChatService chatService, UserProfile userProfile) {
            this.chatService = chatService;
            this.userProfile = userProfile;
        }
    }

    @Slf4j
    public static class View extends bisq.desktop.common.view.View<VBox, Model, Controller> {
        private final ImageView roboIconImageView;
        private final Label nym;
        private final Label nickName;
        private final Label bio;
        private final Label reputationScore;
        private final Label profileAge;
        private final Button privateMsgButton, mentionButton, ignoreButton, reportButton;
        private final Label terms;
        private Subscription roboHashNodeSubscription;

        private View(Model model, Controller controller) {
            super(new VBox(), model, controller);

            root.setSpacing(10);
            root.setMaxWidth(200);
            root.setPadding(new Insets(0, 25, 0, 35));
            root.setAlignment(Pos.TOP_CENTER);

            nickName = new Label();
            nickName.getStyleClass().addAll("bisq-text-9", "font-semi-bold");
            nickName.setAlignment(Pos.CENTER);
            nickName.setMaxWidth(200);
            nickName.setMinWidth(200);
            VBox.setMargin(nickName, new Insets(-20, 0, 5, 0));

            roboIconImageView = new ImageView();
            roboIconImageView.setFitWidth(100);
            roboIconImageView.setFitHeight(100);

            nym = new Label();
            nym.getStyleClass().addAll("bisq-text-7");
            nym.setAlignment(Pos.CENTER);
            nym.setMaxWidth(200);
            nym.setMinWidth(200);
            VBox.setMargin(nym, new Insets(0, 0, 24, 0));

            privateMsgButton = new Button(Res.get("social.sendPrivateMessage"));
            VBox.setMargin(privateMsgButton, new Insets(0, 0, 13, 0));

            VBox bioBox = getInfoBox(Res.get("social.chatUser.bio"), false);
            bio = (Label) bioBox.getChildren().get(1);

            VBox reputationScoreBox = getInfoBox(Res.get("social.chatUser.reputationScore"), false);
            reputationScore = (Label) reputationScoreBox.getChildren().get(1);

            VBox profileAgeBox = getInfoBox(Res.get("social.chatUser.profileAge"), false);
            profileAge = (Label) profileAgeBox.getChildren().get(1);

            Label optionsLabel = new Label(Res.get("social.chatUser.options").toUpperCase());
            optionsLabel.getStyleClass().addAll("bisq-text-7", "bisq-text-grey-9", "font-semi-bold");

            mentionButton = new Button(Res.get("social.mention"));
            ignoreButton = new Button();
            reportButton = new Button(Res.get("social.report"));
            mentionButton.getStyleClass().add("bisq-text-button");
            ignoreButton.getStyleClass().add("bisq-text-button");
            reportButton.getStyleClass().add("bisq-text-button");
            VBox optionsBox = new VBox(5, optionsLabel, mentionButton, ignoreButton, reportButton);
            optionsBox.setAlignment(Pos.CENTER_LEFT);
            VBox.setMargin(optionsBox, new Insets(8, 0, 0, 0));

            Region separator = Layout.separator();
            VBox.setMargin(separator, new Insets(24, -45, 15, -55));

            VBox chatRulesBox = getInfoBox(Res.get("social.chat.chatRules.headline"), true);
            terms = (Label) chatRulesBox.getChildren().get(1);
            terms.setText(Res.get("social.chat.chatRules.content"));

            root.getChildren().addAll(nickName, roboIconImageView, nym, privateMsgButton,
                    bioBox, reputationScoreBox, profileAgeBox,
                    optionsBox, separator, chatRulesBox);
        }

        @Override
        protected void onViewAttached() {
            nym.textProperty().bind(model.nym);
            nickName.textProperty().bind(model.nickName);
            bio.textProperty().bind(model.bio);
            terms.textProperty().bind(model.terms);
            reputationScore.textProperty().bind(model.reputationScore);
            profileAge.textProperty().bind(model.profileAge);
            ignoreButton.textProperty().bind(model.ignoreButtonText);
            roboHashNodeSubscription = EasyBind.subscribe(model.roboHashNode, roboIcon -> {
                if (roboIcon != null) {
                    roboIconImageView.setImage(roboIcon);
                }
            });

            privateMsgButton.setOnAction(e -> controller.onSendPrivateMessage());
            mentionButton.setOnAction(e -> controller.onMentionUser());
            ignoreButton.setOnAction(e -> controller.onToggleIgnoreUser());
            reportButton.setOnAction(e -> controller.onReportUser());
        }

        @Override
        protected void onViewDetached() {
            nym.textProperty().unbind();
            nickName.textProperty().unbind();
            bio.textProperty().unbind();
            terms.textProperty().unbind();
            reputationScore.textProperty().unbind();
            profileAge.textProperty().unbind();
            ignoreButton.textProperty().unbind();

            roboHashNodeSubscription.unsubscribe();

            privateMsgButton.setOnAction(null);
            mentionButton.setOnAction(null);
            ignoreButton.setOnAction(null);
            reportButton.setOnAction(null);
        }

        private VBox getInfoBox(String title, boolean smaller) {
            Label titleLabel = new Label(title.toUpperCase());
            titleLabel.getStyleClass().addAll("bisq-text-4", "bisq-text-grey-9", "font-semi-bold");
            Label contentLabel = new Label();

            contentLabel.getStyleClass().addAll(smaller ? "bisq-text-7" : "bisq-text-6", "wrap-text");
            VBox box = new VBox(2, titleLabel, contentLabel);
            VBox.setMargin(box, new Insets(2, 0, 0, 0));

            return box;
        }
    }
}