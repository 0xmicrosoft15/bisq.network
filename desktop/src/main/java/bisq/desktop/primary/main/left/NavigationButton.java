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

package bisq.desktop.primary.main.left;

import bisq.desktop.common.utils.Transitions;
import bisq.desktop.common.view.NavigationTarget;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.scene.Cursor;
import javafx.scene.control.Label;
import javafx.scene.control.Toggle;
import javafx.scene.control.ToggleGroup;
import javafx.scene.control.Tooltip;
import javafx.scene.image.ImageView;
import javafx.scene.layout.Pane;
import javafx.util.Duration;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

@Slf4j
class NavigationButton extends Pane implements Toggle {
    static final int HEIGHT = 70;
    @Getter
    private final NavigationTarget navigationTarget;
    private final ObjectProperty<ToggleGroup> toggleGroupProperty = new SimpleObjectProperty<>();
    private final BooleanProperty selectedProperty = new SimpleBooleanProperty();
    private final Label label;
    private final Tooltip tooltip;
    private final ImageView icon;

    NavigationButton(String title, ImageView icon, ToggleGroup toggleGroup, NavigationTarget navigationTarget) {
        this.icon = icon;
        this.navigationTarget = navigationTarget;
        setMinHeight(HEIGHT);
        setMaxHeight(HEIGHT);

        setCursor(Cursor.HAND);
        tooltip = new Tooltip(title);
        tooltip.setShowDelay(Duration.millis(200));

        setToggleGroup(toggleGroup);
        toggleGroup.getToggles().add(this);
        selectedProperty().addListener((ov, oldValue, newValue) -> setMouseTransparent(newValue));

        icon.setMouseTransparent(true);
        icon.setLayoutX(40);
        icon.setLayoutY(20);

        label = new Label(title);
        label.setLayoutX(94);
        label.setLayoutY(20);
        label.setMouseTransparent(true);

        setStyle("-fx-background-color: -bisq-grey-1;");
        label.setStyle("-fx-text-fill: -fx-mid-text-color; -fx-font-family: \"IBM Plex Sans Light\"; -fx-font-size: 1.78em;");
        icon.setOpacity(0.6);

        getChildren().addAll(icon, label);
    }


    public final void setOnAction(Runnable handler) {
        setOnMouseClicked(e -> handler.run());
    }

    public void setMenuExpanded(boolean menuExpanded, int width, int duration) {
        //setPrefWidth(width);

        if (menuExpanded) {
            Tooltip.uninstall(this, tooltip);
            label.setVisible(true);
            label.setManaged(true);
            Transitions.fadeIn(label, duration);
        } else {
            Tooltip.install(this, tooltip);
            Transitions.fadeOut(label, duration, () -> {
                label.setVisible(false);
                label.setManaged(false);
            });
        }
    }


    ///////////////////////////////////////////////////////////////////////////////////////////
    // Toggle implementation
    ///////////////////////////////////////////////////////////////////////////////////////////

    @Override
    public ToggleGroup getToggleGroup() {
        return toggleGroupProperty.get();
    }

    @Override
    public void setToggleGroup(ToggleGroup toggleGroup) {
        toggleGroupProperty.set(toggleGroup);
    }

    @Override
    public ObjectProperty<ToggleGroup> toggleGroupProperty() {
        return toggleGroupProperty;
    }

    @Override
    public boolean isSelected() {
        return selectedProperty.get();
    }

    @Override
    public BooleanProperty selectedProperty() {
        return selectedProperty;
    }

    @Override
    public void setSelected(boolean selected) {
        selectedProperty.set(selected);

        if (selected) {
            setStyle("-fx-background-color: -bisq-grey-2;");
            label.setStyle("-fx-text-fill: -fx-light-text-color; -fx-font-family: \"IBM Plex Sans Light\"; -fx-font-size: 1.8em;");
            icon.setOpacity(1);
        } else {
            setStyle("-fx-background-color: -bisq-grey-1;");
            label.setStyle("-fx-text-fill: -fx-mid-text-color; -fx-font-family: \"IBM Plex Sans Light\"; -fx-font-size: 1.8em;");
            icon.setOpacity(0.6);
        }
    }
}