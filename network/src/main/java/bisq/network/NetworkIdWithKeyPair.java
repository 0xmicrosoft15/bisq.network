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

package bisq.network;

import bisq.common.proto.Proto;
import bisq.security.PubKey;

import java.security.KeyPair;

public record NetworkIdWithKeyPair(NetworkId networkId, KeyPair keyPair) implements Proto {
    public String nodeId() {
        return networkId.getNodeId();
    }

    public PubKey pubKey() {
        return networkId.getPubKey();
    }
}
