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

import bisq.common.util.StringUtils;
import bisq.desktop.common.threading.UIThread;
import javafx.beans.value.ChangeListener;
import javafx.scene.Scene;
import javafx.scene.layout.Region;
import javafx.stage.Window;
import lombok.extern.slf4j.Slf4j;

import static com.google.common.base.Preconditions.checkNotNull;

@Slf4j
public abstract class View<R extends Region, M extends Model, C extends Controller> {
    protected final R root;
    protected final M model;
    protected final C controller;
    private final ChangeListener<Scene> sceneChangeListener;
    private ChangeListener<Window> windowChangeListener;
    private boolean isViewAttached;

    public View(R root, M model, C controller) {
        checkNotNull(root, "Root must not be null");
        this.root = root;
        this.model = model;
        this.controller = controller;

        root.setId(getClass().getSimpleName() + ".root_" + StringUtils.createUid());

        sceneChangeListener = (ov, oldValue, newScene) -> handleSceneChange(oldValue, newScene);
        root.sceneProperty().addListener(sceneChangeListener);
        handleSceneChange(null, root.getScene());
    }

    private void handleSceneChange(Scene oldValue, Scene newScene) {
        if (oldValue == null && newScene != null) {
            if (newScene.getWindow() != null) {
                onViewAttachedPrivate();
            } else {
                // For overlays, we need to wait until window is available
                windowChangeListener = (observable, oldWindow, newWindow) -> {
                    // As we get called with a delay it might be that the root scene has already changed to null.
                    if (newWindow != null && root.getScene() != null) {
                        onViewAttachedPrivate();
                    } else {
                        onViewDetachedPrivate();
                        UIThread.runOnNextRenderFrame(() -> newScene.windowProperty().removeListener(windowChangeListener));
                    }
                };
                newScene.windowProperty().addListener(windowChangeListener);
            }
        } else if (oldValue != null && newScene == null) {
            onViewDetachedPrivate();
            if (!controller.useCaching()) {
                // If we do not use caching we do not expect to get added again to stage without creating a 
                // new instance of the view, so we remove our sceneChangeListener.
                UIThread.runOnNextRenderFrame(() -> root.sceneProperty().removeListener(sceneChangeListener));
                if (oldValue.getWindow() != null && windowChangeListener != null) {
                    UIThread.runOnNextRenderFrame(() -> oldValue.windowProperty().removeListener(windowChangeListener));
                }
            }
        }
    }

    public R getRoot() {
        return root;
    }

    private void onViewDetachedPrivate() {
        // In case we have an overlay we might get called before the onViewAttachedPrivate was called in the windowChangeListener
        // We avoid that call as it could lead to null pointers in the onViewDetached methods.
        if (!isViewAttached) {
            return;
        }
        controller.onDeactivateInternal();
        onViewDetachedInternal();
        isViewAttached = false;
    }

    private void onViewAttachedPrivate() {
        // View is listening on model changes triggered by controller, so we call controller first, so that view has 
        // correct state.
        controller.onActivateInternal();
        onViewAttachedInternal();
        isViewAttached = true;
    }

    // The internal methods should be only used by framework classes (e.g. TabView)
    void onViewAttachedInternal() {
        onViewAttached();
    }

    void onViewDetachedInternal() {
        onViewDetached();
    }

    protected abstract void onViewAttached();

    protected abstract void onViewDetached();
}
