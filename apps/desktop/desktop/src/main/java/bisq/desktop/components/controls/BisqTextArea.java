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

package bisq.desktop.components.controls;

import bisq.desktop.common.threading.UIThread;
import javafx.beans.InvalidationListener;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.WeakChangeListener;
import javafx.scene.Group;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TextArea;
import javafx.scene.text.Text;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

/**
 * TextArea does not support of adjustment of height based on the text content.
 * A freelance developer has provided an implementation for that feature based on ideas shared at:
 * <a href="https://stackoverflow.com/questions/18588765/how-can-i-make-a-textarea-stretch-to-fill-the-content-expanding-the-parent-in-t/46590540#46590540">...</a>
 */
@Slf4j
public class BisqTextArea extends TextArea {
    private static final String SELECTOR_TEXT = ".viewport .content .text";
    private static final String SELECTOR_SCROLL_PANE = ".scroll-pane";
    private static final double INITIAL_HEIGHT = 19.0;
    private static final double SCROLL_HIDE_THRESHOLD = INITIAL_HEIGHT * 5;

    private double initialHeight = INITIAL_HEIGHT;
    @Setter
    private double scrollHideThreshold = SCROLL_HIDE_THRESHOLD;
    private boolean initialized;
    private ScrollPane selectorScrollPane;
    private Text selectorText;

    private final InvalidationListener textChangeListener = o -> adjustHeight();
    @SuppressWarnings("FieldCanBeLocal") // Need to keep a reference as used in WeakChangeListener
    private final ChangeListener<Scene> sceneListener = (observable, oldValue, newValue) -> {
        if (newValue == null) {
            // When we get removed from the display graph we remove the textChangeListener.
            // We delay that to the next render frame to avoid potential ConcurrentModificationExceptions
            // at the listener collections.
            UIThread.runOnNextRenderFrame(() -> textProperty().removeListener(textChangeListener));
        } else {
            initialized = false;
            layoutChildren();
        }
    };

    public BisqTextArea(String text) {
        this();
        setText(text);
    }

    public BisqTextArea() {
        setContextMenu(new ContextMenu());
        setWrapText(true);
        sceneProperty().addListener(new WeakChangeListener<>(sceneListener));
    }

    public void setInitialHeight(double initialHeight) {
        this.initialHeight = initialHeight;
        if (scrollHideThreshold < initialHeight) {
            scrollHideThreshold = initialHeight;
        }
    }

    @Override
    protected void layoutChildren() {
        Node lookupNode = lookup(SELECTOR_SCROLL_PANE);
        if (lookupNode instanceof ScrollPane scrollPane) {
            if (!scrollPane.getChildrenUnmodifiable().isEmpty()) {
                try {
                    super.layoutChildren();
                } catch (Throwable t) {
                    t.printStackTrace();
                    //TODO why we call super.layoutChildren(); here?
                    super.layoutChildren();
                }
            }

            if (!initialized) {
                this.selectorScrollPane = scrollPane;
                Node lookupTextNode = lookup(SELECTOR_TEXT);
                if (lookupTextNode instanceof Text textNode) {
                    // If we use a promptText the input field is not the textNode we find by the lookup,
                    // but it's inside a region... A pain to work with those closed components... 
                    Parent parent = textNode.getParent();
                    parent.setStyle("-fx-background-color: transparent; -fx-border-color: transparent");
                    if (parent.getChildrenUnmodifiable().size() == 4) {
                        Node thirdNode = parent.getChildrenUnmodifiable().get(2);
                        if (thirdNode instanceof Group group) {
                            if (!group.getChildren().isEmpty()) {
                                Node node = group.getChildren().get(0);
                                if (node instanceof Text) {
                                    this.selectorText = (Text) node;
                                }
                            }
                        }
                    } else {
                        this.selectorText = textNode;
                    }
                    textProperty().addListener(textChangeListener);
                    scrollPane.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
                    adjustHeight();
                    initialized = true;
                }
            }
        }
    }

    private void adjustHeight() {
        double textHeight = selectorText.getBoundsInLocal().getHeight();
        if (textHeight < initialHeight) {
            textHeight = initialHeight;
        }
        if (textHeight > scrollHideThreshold) {
            selectorScrollPane.setVbarPolicy(ScrollPane.ScrollBarPolicy.ALWAYS);
        } else {
            selectorScrollPane.setVbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        }
        double newHeight = textHeight + INITIAL_HEIGHT;
        setMinHeight(newHeight);

        // Set max height dynamically or remove if not needed
        setMaxHeight(Math.min(newHeight, 300)); // FIXME: max width not being applied
    }
}
