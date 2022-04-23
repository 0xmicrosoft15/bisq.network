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
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import javax.annotation.Nullable;

@Slf4j
class LeftNavButton extends Pane implements Toggle {
    static final int HEIGHT = 40;
    @Getter
    protected final NavigationTarget navigationTarget;
    protected final ObjectProperty<ToggleGroup> toggleGroupProperty = new SimpleObjectProperty<>();
    protected final BooleanProperty selectedProperty = new SimpleBooleanProperty();
    protected final Label label;
    protected final Tooltip tooltip;

    @Nullable
    protected final ImageView icon;

    LeftNavButton(String title, @Nullable ImageView icon, ToggleGroup toggleGroup, NavigationTarget navigationTarget) {
        this.icon = icon;
        this.navigationTarget = navigationTarget;

        setMinHeight(HEIGHT);
        setMaxHeight(HEIGHT);
        getStyleClass().add("bisq-darkest-bg");
        setCursor(Cursor.HAND);

        setToggleGroup(toggleGroup);
        toggleGroup.getToggles().add(this);
        // selectedProperty().addListener((ov, oldValue, newValue) -> setMouseTransparent(newValue));

        tooltip = new Tooltip(title);
        if (icon != null) {
            icon.setMouseTransparent(true);
            icon.setLayoutX(25);
            icon.setLayoutY(10.5);
            icon.setOpacity(0.5);
            getChildren().add(icon);
        }

        label = new Label(title);
        label.setLayoutX(56);
        label.setLayoutY(9.5);
        label.setMouseTransparent(true);

        getChildren().add(label);

        applyStyle();
    }

    protected void applyStyle() {
        if (selectedProperty.get()) {
            getStyleClass().remove("bisq-darkest-bg");
            getStyleClass().add("bisq-dark-bg");
            label.getStyleClass().remove("bisq-nav-label");
            label.getStyleClass().add("bisq-nav-label-selected");

            if (icon != null) {
                icon.setOpacity(1);
            }
        } else {
            getStyleClass().remove("bisq-dark-bg");
            getStyleClass().add("bisq-darkest-bg");
            label.getStyleClass().remove("bisq-nav-label-selected");
            label.getStyleClass().add("bisq-nav-label");

            if (icon != null) {
                icon.setOpacity(0.6);
            }
        }
    }


    public final void setOnAction(Runnable handler) {
        setOnMouseClicked(e -> handler.run());
    }

    public void setMenuExpanded(boolean menuExpanded, int duration) {
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
        applyStyle();
    }
}