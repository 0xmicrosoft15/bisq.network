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
import bisq.common.observable.Pin;
import bisq.desktop.common.observable.FxBindings;
import bisq.desktop.common.threading.UIThread;
import bisq.desktop.components.controls.AutoCompleteComboBox;
import bisq.desktop.components.robohash.RoboHash;
import bisq.i18n.Res;
import bisq.social.user.profile.UserProfile;
import bisq.social.user.profile.UserProfileService;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.value.ChangeListener;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.scene.Cursor;
import javafx.scene.Node;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.Tooltip;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.fxmisc.easybind.EasyBind;
import org.fxmisc.easybind.Subscription;

import java.lang.ref.WeakReference;

@Slf4j
public class UserProfileSelection {
    private final Controller controller;

    public UserProfileSelection(UserProfileService userProfileService) {
        controller = new Controller(userProfileService);
    }

    public Pane getRoot() {
        return controller.view.getRoot();
    }

    private static class Controller implements bisq.desktop.common.view.Controller {
        private final Model model;
        @Getter
        private final View view;
        private final UserProfileService userProfileService;
        private Pin selectedUserProfilePin;
        private Pin userProfilesPin;

        private Controller(UserProfileService userProfileService) {
            this.userProfileService = userProfileService;

            model = new Model();
            view = new View(model, this);
        }

        @Override
        public void onActivate() {
            selectedUserProfilePin = FxBindings.subscribe(userProfileService.getPersistableStore().getSelectedUserProfile(),
                    userProfile -> model.selectedUserProfile.set(new ListItem(userProfile)));
            userProfilesPin = FxBindings.<UserProfile, ListItem>bind(model.userProfiles)
                    .map(ListItem::new)
                    .to(userProfileService.getPersistableStore().getUserProfiles());
        }

        @Override
        public void onDeactivate() {
            if (selectedUserProfilePin != null) {
                selectedUserProfilePin.unbind();
            }
            userProfilesPin.unbind();
        }

        private void onSelected(ListItem selectedItem) {
            if (selectedItem != null) {
                userProfileService.selectUserProfile(selectedItem.userProfile);
            }
        }
    }

    private static class Model implements bisq.desktop.common.view.Model {
        private final ObjectProperty<ListItem> selectedUserProfile = new SimpleObjectProperty<>();
        private final ObservableList<ListItem> userProfiles = FXCollections.observableArrayList();

        private Model() {
        }
    }


    @Slf4j
    public static class View extends bisq.desktop.common.view.View<Pane, Model, Controller> {
        private final UserProfileComboBox comboBox;
        private Subscription subscription;

        private View(Model model, Controller controller) {
            super(new Pane(), model, controller);

            comboBox = new UserProfileComboBox(model.userProfiles,
                    Res.get("social.userProfile.comboBox.description"));
            root.getChildren().addAll(comboBox);
            // comboBox.autosize();
            // root.autosize();

        }

        @Override
        protected void onViewAttached() {
            comboBox.prefWidthProperty().bind(root.widthProperty());
            comboBox.setOnChangeConfirmed(e -> controller.onSelected(comboBox.getSelectionModel().getSelectedItem()));
            subscription = EasyBind.subscribe(model.selectedUserProfile,
                    selected -> UIThread.runOnNextRenderFrame(() -> comboBox.getSelectionModel().select(selected)));
        }

        @Override
        protected void onViewDetached() {
            comboBox.prefWidthProperty().unbind();
            comboBox.setOnChangeConfirmed(null);
            subscription.unsubscribe();
        }
    }

    @EqualsAndHashCode
    public static class ListItem {
        private final UserProfile userProfile;

        private ListItem(UserProfile userProfile) {
            this.userProfile = userProfile;
        }

        @Override
        public String toString() {
            return userProfile.getNickName();
        }
    }

    private static class UserProfileComboBox extends AutoCompleteComboBox<ListItem> {
        public UserProfileComboBox(ObservableList<ListItem> items, String description) {
            super(items, description);

            setCellFactory(param -> new ListCell<>() {
                private final ImageView imageView;
                private final Label label;
                private final HBox hBox;

                {
                    label = new Label();
                    label.setMouseTransparent(true);
                    label.getStyleClass().add("bisq-input-box-text-input");
                    HBox.setMargin(label, new Insets(14, 0, 0, 10));

                    imageView = new ImageView();
                    imageView.setFitWidth(50);
                    imageView.setFitHeight(50);
                    setStyle("-fx-pref-height: 50; -fx-padding: 0 0 0 0");

                    hBox = new HBox();
                    hBox.getChildren().addAll(imageView, label);
                }

                @Override
                protected void updateItem(ListItem item, boolean empty) {
                    super.updateItem(item, empty);

                    if (item != null && !empty) {
                        imageView.setImage(RoboHash.getImage(new ByteArray(item.userProfile.getPubKeyHash())));
                        label.setText(item.userProfile.getNickName());
                        setGraphic(hBox);
                    } else {
                        setGraphic(null);
                    }
                }
            });
        }

        @Override
        protected Skin<?> createDefaultSkin() {
            if (skin == null) {
                skin = new UserProfileSkin(this, description, prompt);
                editor = skin.getTextInputBox().getInputTextField();
            }
            return skin;
        }
    }

    private static class UserProfileSkin extends AutoCompleteComboBox.Skin<ListItem> {
        public UserProfileSkin(ComboBox<ListItem> control, String description, String prompt) {
            super(control, description, prompt);

            ImageView imageView = new ImageView();
            imageView.setFitWidth(50);
            imageView.setFitHeight(50);

            arrow.setLayoutY(23);
            textInputBox.setLayoutX(50);
            textInputBox.setMouseTransparent(true);
            buttonPane.getChildren().add(imageView);
            buttonPane.setCursor(Cursor.HAND);

            control.getSelectionModel().selectedItemProperty().addListener(new WeakReference<>(
                    (ChangeListener<ListItem>) (observable, oldValue, newValue) -> {
                        if (newValue != null) {
                            UserProfile userProfile = newValue.userProfile;
                            imageView.setImage(RoboHash.getImage(new ByteArray(userProfile.getPubKeyHash())));
                            textInputBox.setText(userProfile.getNickName());
                            Tooltip.install(buttonPane,
                                    new Tooltip(userProfile.getTooltipString()));
                        }
                    }).get());
        }

        @Override
        protected void layoutChildren(final double x, final double y,
                                      final double w, final double h) {
            super.layoutChildren(x, y, w, h);

            textInputBox.setPrefWidth(w - 50);
        }

        @Override
        protected int getRowHeight() {
            return 50;
        }

        @Override
        public Node getDisplayNode() {
            return null;
        }
    }
}