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

package bisq.desktop.common.view;

import bisq.desktop.common.threading.UIThread;
import bisq.desktop.common.utils.Transitions;
import bisq.desktop.components.containers.Spacer;
import javafx.geometry.Insets;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.ToggleGroup;
import javafx.scene.layout.*;
import lombok.extern.slf4j.Slf4j;
import org.fxmisc.easybind.EasyBind;
import org.fxmisc.easybind.Subscription;

@Slf4j
public abstract class TabView<M extends TabModel, C extends TabController<M>> extends NavigationView<VBox, M, C>
        implements TransitionedView {
    protected final Label headlineLabel;
    protected final HBox tabs;
    private final Region selectionMarker, line;
    private final ToggleGroup toggleGroup = new ToggleGroup();
    private final ScrollPane scrollPane;
    private Subscription selectedTabButtonSubscription, rootWidthSubscription, layoutDoneSubscription;
    private boolean transitionStarted;
    private Subscription viewSubscription;

    public TabView(M model, C controller) {
        super(new VBox(), model, controller);

        root.setFillWidth(true);
        root.setPadding(new Insets(0, -67, 0, 0));

        headlineLabel = new Label();
        headlineLabel.getStyleClass().add("bisq-content-headline-label");
        HBox.setMargin(headlineLabel, new Insets(-5, 0, 20, -2));

        tabs = new HBox();
        tabs.setFillHeight(true);
        tabs.setSpacing(46);
        tabs.getChildren().addAll(headlineLabel, Spacer.fillHBox());
        tabs.setPadding(new Insets(0, 67, 2, 0));

        scrollPane = new ScrollPane();
        scrollPane.setFitToHeight(true);
        scrollPane.setFitToWidth(true);
        VBox.setVgrow(scrollPane, Priority.ALWAYS);

        line = new Region();
        line.getStyleClass().add("bisq-darkest-bg");
        double lineHeight = 1.5;
        line.setMinHeight(lineHeight);

        selectionMarker = new Region();
        selectionMarker.getStyleClass().add("bisq-green-line");
        selectionMarker.setMinHeight(lineHeight);

        Pane lineAndMarker = new Pane();
        lineAndMarker.getChildren().addAll(line, selectionMarker);
        lineAndMarker.setMinHeight(lineHeight);
        lineAndMarker.setPadding(new Insets(0, 67, 0, 0));

        root.getChildren().addAll(tabs, lineAndMarker, scrollPane);
    }

    @Override
    void onViewAttachedInternal() {
        selectionMarker.setLayoutX(0);
        selectionMarker.setPrefWidth(0);
        transitionStarted = false;

        line.prefWidthProperty().bind(root.widthProperty());
        model.getTabButtons().forEach(tabButton ->
                tabButton.setOnAction(() -> controller.onTabSelected(tabButton.getNavigationTarget())));

        rootWidthSubscription = EasyBind.subscribe(root.widthProperty(), w -> {
            if (model.getSelectedTabButton().get() != null) {
                selectionMarker.setLayoutX(model.getSelectedTabButton().get().getLayoutX());
            }
        });

        selectedTabButtonSubscription = EasyBind.subscribe(model.getSelectedTabButton(), selectedTabButton -> {
            if (selectedTabButton != null) {
                toggleGroup.selectToggle(selectedTabButton);

                if (transitionStarted) {
                    maybeAnimateMark();
                }
            }
        });

        viewSubscription = EasyBind.subscribe(model.getView(), view -> {
            if (view != null) {
                Region childRoot = view.getRoot();
                if (view instanceof TabViewChild tabViewChild) {
                    tabViewChild.applyPadding(childRoot);
                }
                scrollPane.setContent(childRoot);
            }
        });

        super.onViewAttachedInternal();
    }

    @Override
    void onViewDetachedInternal() {
        line.prefWidthProperty().unbind();
        selectedTabButtonSubscription.unsubscribe();
        rootWidthSubscription.unsubscribe();
        if (layoutDoneSubscription != null) {
            layoutDoneSubscription.unsubscribe();
        }
        model.getTabButtons().forEach(tabButton -> tabButton.setOnAction(null));
        viewSubscription.unsubscribe();

        super.onViewDetachedInternal();
    }

    @Override
    public void onStartTransition() {
        this.transitionStarted = true;
        UIThread.runOnNextRenderFrame(this::maybeAnimateMark);
    }

    @Override
    public void onTransitionCompleted() {
    }

    protected void addTab(String text, NavigationTarget navigationTarget) {
        TabButton tabButton = new TabButton(text, toggleGroup, navigationTarget);
        controller.onTabButtonCreated(tabButton);
        tabs.getChildren().addAll(tabButton);
    }

    //todo 
    protected void removeTab(NavigationTarget navigationTarget) {
        controller.findTabButton(navigationTarget).ifPresent(tabButton -> {
            controller.onTabButtonRemoved(tabButton);
            tabs.getChildren().remove(tabButton);
        });

    }

    private void maybeAnimateMark() {
        TabButton selectedTabButton = model.getSelectedTabButton().get();
        if (selectedTabButton == null) {
            return;
        }
        if (layoutDoneSubscription != null) {
            layoutDoneSubscription.unsubscribe();
        }

        //todo maybe listen to transition animation from outer container to start animation after view is visible
        layoutDoneSubscription = EasyBind.subscribe(model.getSelectedTabButton().get().layoutXProperty(), x -> {
            // At the time when x is available the width is available as well
            if (x.doubleValue() > 0) {
                Transitions.animateTabButtonMarks(selectionMarker, selectedTabButton.getWidth(),
                        selectedTabButton.getBoundsInParent().getMinX());
                UIThread.runOnNextRenderFrame(() -> {
                    if (layoutDoneSubscription != null) {
                        layoutDoneSubscription.unsubscribe();
                        layoutDoneSubscription = null;
                    }
                });
            }
        });
    }
}
