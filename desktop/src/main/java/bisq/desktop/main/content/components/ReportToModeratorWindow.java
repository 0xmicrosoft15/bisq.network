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

import bisq.chat.channel.ChatChannelDomain;
import bisq.desktop.ServiceProvider;
import bisq.desktop.common.view.InitWithDataController;
import bisq.desktop.components.controls.MaterialTextArea;
import bisq.desktop.components.controls.MultiLineLabel;
import bisq.desktop.overlay.OverlayController;
import bisq.desktop.overlay.OverlayModel;
import bisq.i18n.Res;
import bisq.support.moderator.ModeratorService;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import lombok.extern.slf4j.Slf4j;

public class ReportToModeratorWindow {
    @Getter
    @EqualsAndHashCode
    @ToString
    public static class InitData {
        private final String reportedUserProfileId;
        private final ChatChannelDomain chatChannelDomain;

        public InitData(String reportedUserProfileId, ChatChannelDomain chatChannelDomain) {
            this.reportedUserProfileId = reportedUserProfileId;
            this.chatChannelDomain = chatChannelDomain;
        }
    }

    @Getter
    private final Controller controller;

    public ReportToModeratorWindow(ServiceProvider serviceProvider) {
        controller = new Controller(serviceProvider);
    }

    @Slf4j
    private static class Controller implements InitWithDataController<InitData> {
        @Getter
        private final View view;
        private final Model model;
        private final ModeratorService moderatorService;

        private Controller(ServiceProvider serviceProvider) {
            moderatorService = serviceProvider.getSupportService().getModeratorService();
            model = new Model();
            view = new View(model, this);
        }

        @Override
        public void initWithData(ReportToModeratorWindow.InitData initData) {
            model.setReportedUserProfileId(initData.getReportedUserProfileId());
            model.setChatChannelDomain(initData.getChatChannelDomain());
        }

        @Override
        public void onActivate() {
            model.getReportButtonDisabled().bind(model.getMessage().isEmpty());
        }

        @Override
        public void onDeactivate() {
            model.getReportButtonDisabled().unbind();
            model.getMessage().set("");
            model.setReportedUserProfileId(null);
        }

        private void onReport() {
            moderatorService.reportUserProfile(model.getReportedUserProfileId(), model.getMessage().get(), model.getChatChannelDomain());
            onCancel();
        }

        public void onCancel() {
            OverlayController.hide();
        }
    }

    @Slf4j
    @Getter
    private static class Model implements bisq.desktop.common.view.Model {
        public StringProperty message = new SimpleStringProperty("");
        public BooleanProperty reportButtonDisabled = new SimpleBooleanProperty();
        @Setter
        private String reportedUserProfileId;
        @Setter
        private ChatChannelDomain chatChannelDomain;

    }

    @Slf4j
    private static class View extends bisq.desktop.common.view.View<VBox, Model, Controller> {
        private final Button reportButton, cancelButton;
        private final MaterialTextArea message;

        private View(Model model, Controller controller) {
            super(new VBox(20), model, controller);

            root.setAlignment(Pos.CENTER);
            root.setPadding(new Insets(50, 30, 50, 30));
            root.setPrefWidth(OverlayModel.WIDTH);

            Label headline = new Label(Res.get("chat.reportToModerator.headline"));
            headline.getStyleClass().addAll("bisq-text-headline-2", "wrap-text");

            MultiLineLabel info = new MultiLineLabel(Res.get("chat.reportToModerator.info"));
            info.getStyleClass().addAll("bisq-text-3");

            message = new MaterialTextArea(Res.get("chat.reportToModerator.message"), Res.get("chat.reportToModerator.message.prompt"));
            reportButton = new Button(Res.get("chat.reportToModerator.report"));
            reportButton.setDefaultButton(true);
            cancelButton = new Button(Res.get("action.cancel"));
            HBox buttons = new HBox(20, reportButton, cancelButton);
            buttons.setAlignment(Pos.CENTER);
            root.getChildren().setAll(headline, info, message, buttons);
        }

        @Override
        protected void onViewAttached() {
            message.textProperty().bindBidirectional(model.getMessage());
            reportButton.disableProperty().bind(model.getReportButtonDisabled());
            reportButton.setOnAction(e -> controller.onReport());
            cancelButton.setOnAction(e -> controller.onCancel());
        }

        @Override
        protected void onViewDetached() {
            message.textProperty().unbindBidirectional(model.getMessage());
            reportButton.disableProperty().unbind();
            reportButton.setOnAction(null);
            cancelButton.setOnAction(null);
        }
    }

}