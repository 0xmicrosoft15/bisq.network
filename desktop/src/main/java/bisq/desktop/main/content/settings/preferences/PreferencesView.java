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

package bisq.desktop.main.content.settings.preferences;

import bisq.desktop.common.Layout;
import bisq.desktop.common.view.View;
import bisq.desktop.components.controls.AutoCompleteComboBox;
import bisq.desktop.components.controls.Switch;
import bisq.i18n.Res;
import bisq.settings.ChatNotificationType;
import javafx.beans.value.ChangeListener;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;
import javafx.util.StringConverter;
import lombok.extern.slf4j.Slf4j;
import org.fxmisc.easybind.EasyBind;
import org.fxmisc.easybind.Subscription;

@Slf4j
public class PreferencesView extends View<VBox, PreferencesModel, PreferencesController> {
    private final Button resetDontShowAgain;
    private final Switch useAnimations, closeMyOfferWhenTaken, notifyForPreRelease;
    private final ToggleGroup notificationsToggleGroup = new ToggleGroup();
    private final RadioButton all, mention, off;
    private final ChangeListener<Toggle> notificationsToggleListener;
    private final AutoCompleteComboBox<String> languageSelection;
    private Subscription selectedNotificationTypePin;

    public PreferencesView(PreferencesModel model, PreferencesController controller) {
        super(new VBox(20), model, controller);

        root.setAlignment(Pos.TOP_LEFT);

        // Notifications
        Label languageSelectionHeadline = new Label(Res.get("settings.preferences.language.headline"));
        languageSelectionHeadline.getStyleClass().addAll("bisq-text-headline-2", "wrap-text");

        languageSelection = new AutoCompleteComboBox<>(model.getLanguageCodes(), Res.get("settings.preferences.language.select"));
        languageSelection.setPrefWidth(300);
        languageSelection.setConverter(new StringConverter<>() {
            @Override
            public String toString(String languageCode) {
                return languageCode != null ? controller.getDisplayLanguage(languageCode) : "";
            }

            @Override
            public String fromString(String string) {
                return "";
            }
        });

        // Notifications
        Label notificationsHeadline = new Label(Res.get("settings.preferences.notification.options"));
        notificationsHeadline.getStyleClass().addAll("bisq-text-headline-2", "wrap-text");

        all = new RadioButton(Res.get("settings.preferences.notification.option.all"));
        mention = new RadioButton(Res.get("settings.preferences.notification.option.mention"));
        off = new RadioButton(Res.get("settings.preferences.notification.option.off"));

        all.setToggleGroup(notificationsToggleGroup);
        mention.setToggleGroup(notificationsToggleGroup);
        off.setToggleGroup(notificationsToggleGroup);

        all.setUserData(ChatNotificationType.ALL);
        mention.setUserData(ChatNotificationType.MENTION);
        off.setUserData(ChatNotificationType.OFF);

        VBox notificationsVBox = new VBox(10, all, mention, off);
        notificationsVBox.setPadding(new Insets(10));
        notificationsVBox.getStyleClass().add("bisq-dark-bg");

        notificationsToggleListener = (observable, oldValue, newValue) -> controller.onSetChatNotificationType((ChatNotificationType) newValue.getUserData());

        notifyForPreRelease = new Switch(Res.get("settings.preferences.notification.notifyForPreRelease"));

        // Display
        Label displayHeadline = new Label(Res.get("settings.preferences.display.headline"));
        displayHeadline.getStyleClass().addAll("bisq-text-headline-2", "wrap-text");

        useAnimations = new Switch(Res.get("settings.preferences.display.useAnimations"));
        resetDontShowAgain = new Button(Res.get("settings.preferences.display.resetDontShowAgain"));

        // Trade
        Label tradeHeadline = new Label(Res.get("settings.preferences.trade.headline"));
        tradeHeadline.getStyleClass().addAll("bisq-text-headline-2", "wrap-text");
        closeMyOfferWhenTaken = new Switch(Res.get("settings.preferences.trade.closeMyOfferWhenTaken"));

        VBox.setMargin(languageSelectionHeadline, new Insets(30, 0, 0, 0));
        VBox.setMargin(notificationsHeadline, new Insets(10, 0, 0, 0));
        VBox.setMargin(displayHeadline, new Insets(10, 0, 0, 0));
        VBox.setMargin(tradeHeadline, new Insets(10, 0, 0, 0));
        root.getChildren().addAll(languageSelectionHeadline, languageSelection, Layout.hLine(),
                notificationsHeadline, notificationsVBox, notifyForPreRelease, Layout.hLine(),
                displayHeadline, useAnimations, resetDontShowAgain, Layout.hLine(),
                tradeHeadline, closeMyOfferWhenTaken);
    }

    @Override
    protected void onViewAttached() {
        notificationsToggleGroup.selectedToggleProperty().addListener(notificationsToggleListener);
        selectedNotificationTypePin = EasyBind.subscribe(model.getChatNotificationType(), selected -> applyChatNotificationType());

        notifyForPreRelease.selectedProperty().bindBidirectional(model.getNotifyForPreRelease());
        useAnimations.selectedProperty().bindBidirectional(model.getUseAnimations());
        closeMyOfferWhenTaken.selectedProperty().bindBidirectional(model.getCloseMyOfferWhenTaken());

        languageSelection.getSelectionModel().select(model.getSelectedLanguageCode());
        languageSelection.setOnChangeConfirmed(e -> {
            if (languageSelection.getSelectionModel().getSelectedItem() == null) {
                languageSelection.getSelectionModel().select(model.getSelectedLanguageCode());
                return;
            }
            controller.onSelectLanguage(languageSelection.getSelectionModel().getSelectedItem());
        });

        resetDontShowAgain.setOnAction(e -> controller.onResetDontShowAgain());
    }

    @Override
    protected void onViewDetached() {
        notificationsToggleGroup.selectedToggleProperty().removeListener(notificationsToggleListener);
        selectedNotificationTypePin.unsubscribe();

        notifyForPreRelease.selectedProperty().unbindBidirectional(model.getNotifyForPreRelease());
        useAnimations.selectedProperty().unbindBidirectional(model.getUseAnimations());
        closeMyOfferWhenTaken.selectedProperty().unbindBidirectional(model.getCloseMyOfferWhenTaken());

        languageSelection.setOnChangeConfirmed(null);

        resetDontShowAgain.setOnAction(null);
    }

    private void applyChatNotificationType() {
        switch (model.getChatNotificationType().get()) {
            case ALL: {
                notificationsToggleGroup.selectToggle(all);
                break;
            }
            case MENTION: {
                notificationsToggleGroup.selectToggle(mention);
                break;
            }
            case OFF: {
                notificationsToggleGroup.selectToggle(off);
                break;
            }
        }
    }
}
