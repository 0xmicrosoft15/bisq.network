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

package bisq.desktop.primary.main.content.newProfilePopup;

import bisq.desktop.common.view.View;
import bisq.desktop.layout.Layout;
import bisq.i18n.Res;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import lombok.extern.slf4j.Slf4j;
import org.fxmisc.easybind.EasyBind;
import org.fxmisc.easybind.Subscription;

@Slf4j
public class NewProfilePopupView extends View<Pane, NewProfilePopupModel, NewProfilePopupController> {
    private final HBox stepsBox;
    private final Button skipButton;
    private Subscription stepSubscription;

    public NewProfilePopupView(NewProfilePopupModel model,
                               NewProfilePopupController controller) {
        super(new VBox(), model, controller);

        skipButton = new Button(Res.get("skip").toUpperCase());

        StackPane stackPane = new StackPane();
        VBox.setMargin(stackPane, new Insets(0, 0, 12, 0));

        stepsBox = new HBox(
                new Label(Res.get("initNymProfile.step1").toUpperCase()),
                new Label(Res.get("initNymProfile.step2").toUpperCase()),
                new Label(Res.get("initNymProfile.step3").toUpperCase())
        );
        stepsBox.setSpacing(60);
        stepsBox.setAlignment(Pos.BASELINE_CENTER);
        stepsBox.getStyleClass().add("border-bottom-2");
        stepsBox.setPadding(new Insets(32, 0, 32, 0));

        skipButton.getStyleClass().setAll("bisq-transparent-button", "bisq-small-light-label");
        skipButton.setOnAction(e -> controller.onSkip());
        StackPane.setAlignment(skipButton, Pos.TOP_RIGHT);
        StackPane.setMargin(skipButton, new Insets(24, 24, 0, 0));
        stackPane.getChildren().addAll(stepsBox, skipButton);
        root.getChildren().add(stackPane);

        model.getView().addListener((observable, oldValue, newValue) -> {
            if (root.getChildren().size() == 1) {
                root.getChildren().add(newValue.getRoot());
            } else {
                root.getChildren().set(1, newValue.getRoot());
            }
        });
    }

    @Override
    protected void onViewAttached() {
        stepSubscription = EasyBind.subscribe(model.currentStepProperty(), selectedStep -> {
            for (int i = 0; i < stepsBox.getChildren().size(); i++) {
                Label step = (Label) stepsBox.getChildren().get(i);
                Layout.chooseStyleClass(
                        step,
                        "bisq-small-light-label",
                        "bisq-small-light-label-dimmed-2",
                        (int) selectedStep == i
                );
            }
        });

        skipButton.setOnAction(e -> controller.onSkip());
    }

    @Override
    protected void onViewDetached() {
        stepSubscription.unsubscribe();

        skipButton.setOnAction(null);
    }
}
