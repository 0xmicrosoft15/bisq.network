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

package bisq.user.role;

import bisq.common.proto.ProtoEnum;
import bisq.common.util.ProtobufUtils;

public enum RoleType implements ProtoEnum {
    MEDIATOR,
    ARBITRATOR,
    MODERATOR,
    SECURITY_MANAGER,
    RELEASE_MANAGER,
    ORACLE_NODE,
    SEED_NODE,
    EXPLORER_NODE,
    MARKET_PRICE_NODE;

    @Override
    public bisq.user.protobuf.RoleType toProto() {
        return bisq.user.protobuf.RoleType.valueOf(name());
    }

    public static RoleType fromProto(bisq.user.protobuf.RoleType proto) {
        return ProtobufUtils.enumFromProto(RoleType.class, proto.name());
    }
}
