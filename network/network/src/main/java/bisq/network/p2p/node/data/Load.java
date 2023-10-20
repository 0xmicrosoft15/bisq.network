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

package bisq.network.p2p.node.data;

import bisq.common.proto.Proto;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;

@Getter
@ToString
@EqualsAndHashCode
public final class Load implements Proto {
    public static final Load INITIAL_LOAD = new Load(1);
    private final int numConnections;

    public Load(int numConnections) {
        this.numConnections = numConnections;
    }

    public bisq.network.protobuf.Load toProto() {
        return bisq.network.protobuf.Load.newBuilder().setNumConnections(numConnections).build();
    }

    public static Load fromProto(bisq.network.protobuf.Load proto) {
        return new Load(proto.getNumConnections());
    }

    public int getFactor() {
        //todo
        return 10;
    }
}