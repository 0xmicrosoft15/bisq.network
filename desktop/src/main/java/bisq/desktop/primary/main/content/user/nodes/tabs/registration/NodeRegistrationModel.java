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

package bisq.desktop.primary.main.content.user.nodes.tabs.registration;

import bisq.desktop.common.view.Model;
import bisq.network.p2p.node.Address;
import bisq.network.p2p.node.transport.Transport;
import bisq.user.identity.UserIdentity;
import bisq.user.node.NodeType;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

import java.security.KeyPair;
import java.util.Map;

@Slf4j
@Getter
public class NodeRegistrationModel implements Model {
    private final StringProperty selectedProfileUserName = new SimpleStringProperty();
    private final StringProperty privateKey = new SimpleStringProperty();
    private final StringProperty publicKey = new SimpleStringProperty();
    private final StringProperty addressInfo = new SimpleStringProperty();
    private final BooleanProperty registrationDisabled = new SimpleBooleanProperty();
    private final BooleanProperty removeRegistrationVisible = new SimpleBooleanProperty();
    private final NodeType nodeType;
    @Setter
    private UserIdentity userIdentity;
    @Setter
    private KeyPair keyPair;
    @Setter
    private Map<Transport.Type, Address> addressByNetworkType;

    public NodeRegistrationModel(NodeType nodeType) {
        this.nodeType = nodeType;
    }
}
