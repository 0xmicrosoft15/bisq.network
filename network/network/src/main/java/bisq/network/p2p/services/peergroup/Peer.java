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

package bisq.network.p2p.services.peergroup;

import bisq.common.proto.Proto;
import bisq.common.validation.NetworkDataValidation;
import bisq.network.p2p.node.Capability;
import bisq.network.p2p.node.data.Load;
import bisq.network.p2p.vo.Address;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;

import javax.annotation.Nonnull;
import java.util.Date;

@Getter
@ToString
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public final class Peer implements Proto, Comparable<Peer> {
    @EqualsAndHashCode.Include
    private final Capability capability;
    private final Load load;
    private final boolean isOutboundConnection;
    private final long created;

    public Peer(Capability capability, Load load, boolean isOutboundConnection) {
        this(capability, load, isOutboundConnection, System.currentTimeMillis());
    }

    public Peer(Capability capability, Load load, boolean isOutboundConnection, long created) {
        this.capability = capability;
        this.load = load;
        this.isOutboundConnection = isOutboundConnection;
        this.created = created;

        NetworkDataValidation.validateDate(created);
    }

    public bisq.network.protobuf.Peer toProto() {
        return bisq.network.protobuf.Peer.newBuilder()
                .setCapability(capability.toProto())
                .setLoad(load.toProto())
                .setIsOutboundConnection(isOutboundConnection)
                .setCreated(created)
                .build();
    }

    public static Peer fromProto(bisq.network.protobuf.Peer proto) {
        return new Peer(Capability.fromProto(proto.getCapability()),
                Load.fromProto(proto.getLoad()),
                proto.getIsOutboundConnection(),
                proto.getCreated());
    }

    public Date getDate() {
        return new Date(created);
    }

    public Address getAddress() {
        return capability.getAddress();
    }

    public long getAge() {
        return new Date().getTime() - created;
    }

    // Descending order
    @Override
    public int compareTo(@Nonnull Peer o) {
        return Long.compare(o.getCreated(), created);
    }
}
