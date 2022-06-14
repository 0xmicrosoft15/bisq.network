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

package bisq.desktop.primary.overlay.onboarding.nym;

import bisq.desktop.common.view.Model;
import bisq.security.pow.ProofOfWork;
import javafx.beans.property.*;
import javafx.scene.image.Image;
import lombok.Getter;

import java.security.KeyPair;

@Getter
public class GenerateNymModel implements Model {
    final StringProperty nickName = new SimpleStringProperty("");
    final StringProperty bio = new SimpleStringProperty("");
    final StringProperty terms = new SimpleStringProperty("");
    final ObjectProperty<Image> roboHashImage = new SimpleObjectProperty<>();
    final StringProperty nymId = new SimpleStringProperty();
    final BooleanProperty createProfileButtonMouseTransparent = new SimpleBooleanProperty();
    final BooleanProperty regenerateButtonMouseTransparent = new SimpleBooleanProperty();
    final DoubleProperty createProfileProgress = new SimpleDoubleProperty();
    final BooleanProperty roboHashIconVisible = new SimpleBooleanProperty();
    final DoubleProperty powProgress = new SimpleDoubleProperty();
    ProofOfWork proofOfWork;
    KeyPair tempKeyPair = null;
    String tempKeyId;
}