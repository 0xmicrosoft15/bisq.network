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

package bisq.offer;

import bisq.common.proto.UnresolvableProtobufMessageException;
import bisq.common.proto.ProtoResolver;
import bisq.network.p2p.services.data.storage.DistributedData;
import com.google.protobuf.Any;
import com.google.protobuf.InvalidProtocolBufferException;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class OfferDistributedDataResolver implements ProtoResolver<DistributedData> {
    public DistributedData resolve(Any any, String protoMessageName) {
        try {
            if (protoMessageName.equals("Offer")) {
                return Offer.fromProto(any.unpack(bisq.offer.protobuf.Offer.class));
            }
        } catch (InvalidProtocolBufferException e) {
            throw new UnresolvableProtobufMessageException(e);
        }

        throw new UnresolvableProtobufMessageException(any);
    }
}