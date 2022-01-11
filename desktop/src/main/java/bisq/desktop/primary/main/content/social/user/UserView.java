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

package bisq.desktop.primary.main.content.social.user;

import bisq.desktop.common.view.View;
import bisq.desktop.components.controls.BisqComboBox;
import bisq.desktop.primary.main.content.social.hangout.ChatUser;
import bisq.i18n.Res;
import javafx.beans.value.ChangeListener;
import javafx.collections.ListChangeListener;
import javafx.geometry.Insets;
import javafx.util.StringConverter;
import lombok.extern.slf4j.Slf4j;

import javax.annotation.Nullable;

@Slf4j
public class UserView extends View<BisqComboBox<ChatUser>, UserModel, UserController> {

    private final BisqComboBox<ChatUser> comboBox;
    private final ListChangeListener<ChatUser> listener;
    private final ChangeListener<ChatUser> selectionListener;
    private final ChangeListener<ChatUser> selectedChatUserListener;

    public UserView(UserModel model, UserController controller) {
        super(new BisqComboBox<>(Res.common.get("selectUser")), model, controller);
        comboBox = root;
        comboBox.setPadding(new Insets(20, 20, 20, 0));
        listener = c -> updateItems();
        comboBox.setConverter(new StringConverter<>() {
            @Override
            public String toString(@Nullable ChatUser chatUser) {
                return chatUser != null ? chatUser.userName() : "";
            }

            @Override
            public ChatUser fromString(String string) {
                return null;
            }
        });
        selectionListener = (observable, oldValue, newValue) -> {
            if (newValue != null) {
                controller.onSelectChatUser(newValue);
            }
        };
        selectedChatUserListener = (observable, oldValue, newValue) -> {
            comboBox.getSelectionModel().select(newValue);
        };
    }

    private void updateItems() {
        comboBox.setItems(model.getChatUsers());

    }

    @Override
    public void onViewAttached() {
        model.getChatUsers().addListener(listener);
        comboBox.getSelectionModel().selectedItemProperty().addListener(selectionListener);
        model.getSelectedChatUser().addListener(selectedChatUserListener);
        updateItems();
        comboBox.getSelectionModel().select(model.getSelectedChatUser().get());
    }

    @Override
    protected void onViewDetached() {
        model.getChatUsers().removeListener(listener);
        comboBox.getSelectionModel().selectedItemProperty().removeListener(selectionListener);
        model.getSelectedChatUser().removeListener(selectedChatUserListener);
    }
}
