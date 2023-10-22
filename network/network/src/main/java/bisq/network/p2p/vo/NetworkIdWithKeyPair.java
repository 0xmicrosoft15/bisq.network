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

package bisq.network.p2p.vo;

import bisq.security.PubKey;
import lombok.Getter;
import lombok.ToString;

import java.security.KeyPair;
import java.util.Arrays;
import java.util.Objects;

@Getter
@ToString
public final class NetworkIdWithKeyPair {
    private final NetworkId networkId;
    private final KeyPair keyPair;

    public NetworkIdWithKeyPair(NetworkId networkId, KeyPair keyPair) {
        this.networkId = networkId;
        this.keyPair = keyPair;
    }

    public String getNodeId() {
        return networkId.getNodeId();
    }

    public PubKey getPubKey() {
        return networkId.getPubKey();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        NetworkIdWithKeyPair that = (NetworkIdWithKeyPair) o;

        if (!Objects.equals(networkId, that.networkId)) return false;
        return Arrays.equals(keyPair.getPublic().getEncoded(), that.keyPair.getPublic().getEncoded()) &&
                Arrays.equals(keyPair.getPrivate().getEncoded(), that.keyPair.getPrivate().getEncoded());
    }

    @Override
    public int hashCode() {
        int result = networkId != null ? networkId.hashCode() : 0;
        result = 31 * result + Arrays.hashCode(keyPair.getPublic().getEncoded());
        result = 31 * result + Arrays.hashCode(keyPair.getPrivate().getEncoded());
        return result;
    }
}
