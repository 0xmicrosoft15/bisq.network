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

package bisq.desktop.common.utils;

import bisq.common.application.DevMode;
import bisq.desktop.common.view.Navigation;
import bisq.desktop.common.view.NavigationTarget;
import bisq.desktop.overlay.OverlayController;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;

public class KeyHandlerUtil {

    public static void handleShutDownKeyEvent(KeyEvent keyEvent, Runnable handler) {
        if (KeyCodeUtils.isCtrlPressed(KeyCode.W, keyEvent) ||
                KeyCodeUtils.isCtrlPressed(KeyCode.Q, keyEvent)) {
            handler.run();
        }
    }

    public static void handleEscapeKeyEvent(KeyEvent keyEvent, Runnable handler) {
        if (keyEvent.getCode() == KeyCode.ESCAPE) {
            keyEvent.consume();
            handler.run();
        }
    }

    public static void handleEnterKeyEvent(KeyEvent keyEvent, Runnable handler) {
        if (keyEvent.getCode() == KeyCode.ENTER) {
            keyEvent.consume();
            handler.run();
        }
    }

    public static void handleDevModeKeyEvent(KeyEvent keyEvent) {
        if (DevMode.isDevMode()) {
            if (KeyCodeUtils.isCtrlPressed(KeyCode.DIGIT0, keyEvent)) {
                Navigation.navigateTo(NavigationTarget.ONBOARDING_WELCOME);
            } else if (KeyCodeUtils.isCtrlPressed(KeyCode.DIGIT1, keyEvent)) {
                Navigation.navigateTo(NavigationTarget.ONBOARDING_GENERATE_NYM);
            } else if (KeyCodeUtils.isCtrlPressed(KeyCode.DIGIT2, keyEvent)) {
                Navigation.navigateTo(NavigationTarget.CREATE_PROFILE_STEP1);
            } else if (KeyCodeUtils.isCtrlPressed(KeyCode.DIGIT3, keyEvent)) {
                Navigation.navigateTo(NavigationTarget.CREATE_PROFILE_STEP2);
            } else if (KeyCodeUtils.isCtrlPressed(KeyCode.DIGIT4, keyEvent)) {
                Navigation.navigateTo(NavigationTarget.BISQ_EASY_ONBOARDING);
            } else if (KeyCodeUtils.isCtrlPressed(KeyCode.DIGIT5, keyEvent)) {
                Navigation.navigateTo(NavigationTarget.TRADE_WIZARD_DIRECTION);
            } else if (KeyCodeUtils.isCtrlPressed(KeyCode.DIGIT6, keyEvent)) {
                Navigation.navigateTo(NavigationTarget.TRADE_WIZARD_MARKET);
            } else if (KeyCodeUtils.isCtrlPressed(KeyCode.DIGIT7, keyEvent)) {
                Navigation.navigateTo(NavigationTarget.TRADE_WIZARD_AMOUNT);
            } else if (KeyCodeUtils.isCtrlPressed(KeyCode.DIGIT8, keyEvent)) {
                Navigation.navigateTo(NavigationTarget.TRADE_WIZARD_PAYMENT_METHOD);
            } else if (KeyCodeUtils.isCtrlPressed(KeyCode.DIGIT9, keyEvent)) {
                Navigation.navigateTo(NavigationTarget.TRADE_WIZARD_REVIEW_OFFER);
            } else if (KeyCodeUtils.isCtrlPressed(KeyCode.H, keyEvent)) {
                OverlayController.hide();
            }
        }
    }
}