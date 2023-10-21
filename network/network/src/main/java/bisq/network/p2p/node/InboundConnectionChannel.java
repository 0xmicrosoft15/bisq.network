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

package bisq.network.p2p.node;

import bisq.network.p2p.node.data.Metrics;
import bisq.network.p2p.node.data.NetworkLoad;
import bisq.network.p2p.node.envelope.NetworkEnvelopeSocketChannel;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class InboundConnectionChannel extends ConnectionChannel {
    @Setter
    private boolean isPeerAddressVerified;

    public InboundConnectionChannel(Capability peersCapability,
                                    NetworkLoad peersNetworkLoad,
                                    NetworkEnvelopeSocketChannel networkEnvelopeSocketChannel,
                                    Metrics metrics) {
        super(peersCapability, peersNetworkLoad, networkEnvelopeSocketChannel, metrics);
        log.debug("Create inboundConnection");
    }

    @Override
    public boolean isPeerAddressVerified() {
        return isPeerAddressVerified;
    }
}
