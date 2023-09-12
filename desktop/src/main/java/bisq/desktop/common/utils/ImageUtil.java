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

import bisq.common.util.OsUtils;
import javafx.scene.SnapshotParameters;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.paint.Color;
import javafx.stage.Screen;
import lombok.extern.slf4j.Slf4j;

import java.io.InputStream;
import java.util.Objects;

@Slf4j
public class ImageUtil {

    // Does not resolve the @2x automatically
    public static Image getImageByPath(String path) {
        try (InputStream resourceAsStream = ImageUtil.class.getClassLoader().getResourceAsStream(path)) {
            if (resourceAsStream == null) {
                return null;
            }
            return new Image(Objects.requireNonNull(resourceAsStream));
        } catch (Exception e) {
            log.error("Loading image failed: path={}", path);
            e.printStackTrace();
            throw new RuntimeException(e);
        }
    }

    public static Image getImageByPath(String path, int width, int height) {
        try (InputStream resourceAsStream = ImageUtil.class.getClassLoader().getResourceAsStream(path)) {
            if (resourceAsStream == null) {
                return null;
            }
            return new Image(resourceAsStream, width, height, true, true);
        } catch (Exception e) {
            log.error("Loading image failed: path={}", path);
            e.printStackTrace();
            throw new RuntimeException(e);
        }
    }

    // Does resolve the @2x automatically
    public static ImageView getImageViewById(String id) {
        ImageView imageView = new ImageView();
        imageView.setId(id);
        return imageView;
    }

    // determine if this is a macOS retina display
    // https://stackoverflow.com/questions/20767708/how-do-you-detect-a-retina-display-in-java#20767802
    public static boolean isRetina() {
        return Screen.getPrimary().getOutputScaleX() > 1.5;
    }

    public static Image getWindowTitleIcon() {
        String iconPath;
        if (OsUtils.isMac())
            iconPath = ImageUtil.isRetina() ? "images/window_icon@2x.png" : "images/window_icon.png";
        else if (OsUtils.isWindows())
            iconPath = "images/task_bar_icon_windows.png";
        else
            iconPath = "images/task_bar_icon_linux.png";

        return ImageUtil.getImageByPath(iconPath);
    }

    public static Image composeImage(String[] paths, int width, int height) {
        Canvas canvas = new Canvas();
        canvas.setWidth(width);
        canvas.setHeight(height);
        GraphicsContext graphicsContext2D = canvas.getGraphicsContext2D();

        double radius = Math.min(height, width) / 2d;
        double x = width / 2d;
        double y = height / 2d;
        graphicsContext2D.beginPath();
        graphicsContext2D.moveTo(x - radius, y);
        graphicsContext2D.arc(x, y, radius, radius, 180, 360);
        graphicsContext2D.closePath();
        graphicsContext2D.clip();

        for (String path : paths) {
            graphicsContext2D.drawImage(new Image("images/robohash/" + path), 0, 0, width, height);
        }
        SnapshotParameters snapshotParameters = new SnapshotParameters();
        snapshotParameters.setFill(Color.TRANSPARENT);
        return canvas.snapshot(snapshotParameters, null);
    }
}
