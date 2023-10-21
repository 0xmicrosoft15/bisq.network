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

package bisq.network.p2p.services.monitor;

import bisq.network.p2p.node.Connection;
import bisq.network.p2p.node.Node;
import bisq.network.p2p.services.peergroup.Peer;
import bisq.network.p2p.services.peergroup.PeerGroupService;
import bisq.network.p2p.vo.Address;

import java.text.SimpleDateFormat;
import java.util.Comparator;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

public class MonitorService {
    private final Node node;
    private final PeerGroupService peerGroupService;

    public MonitorService(Node node, PeerGroupService peerGroupService) {
        this.node = node;
        this.peerGroupService = peerGroupService;
    }

    public CompletableFuture<Boolean> shutdown() {
        return CompletableFuture.completedFuture(true);
    }

    public String getPeerGroupInfo() {
        int numSeedConnections = (int) peerGroupService.getAllConnections()
                .filter(connection -> peerGroupService.isSeed(connection.getPeerAddress())).count();
        StringBuilder sb = new StringBuilder();
        sb.append(node.getTransportType().name()).append(": ")
                .append(node.findMyAddress().map(Address::toString).orElse(""))
                .append("\n").append("Num connections: ").append(peerGroupService.getNumConnections())
                .append("\n").append("Num all connections: ").append(peerGroupService.getNumConnections())
                .append("\n").append("Num outbound connections: ").append(peerGroupService.getOutboundConnections().count())
                .append("\n").append("Num inbound connections: ").append(peerGroupService.getInboundConnections().count())
                .append("\n").append("Num seed connections: ").append(numSeedConnections)
                .append("\n").append("Connections: ").append("\n");
        peerGroupService.getOutboundConnections()
                .sorted(peerGroupService.getConnectionAgeComparator())
                .forEach(connection -> appendConnectionInfo(sb, connection, true));
        peerGroupService.getInboundConnections()
                .sorted(peerGroupService.getConnectionAgeComparator())
                .forEach(connection -> appendConnectionInfo(sb, connection, false));
        sb.append("\n").append("Reported peers (").append(peerGroupService.getReportedPeers().size()).append("): ").append(peerGroupService.getReportedPeers().stream()
                .map(Peer::getAddress).sorted(Comparator.comparing(Address::getPort)).collect(Collectors.toList()));
        sb.append("\n").append("Persisted peers: ").append(peerGroupService.getPersistedPeers().stream()
                .map(Peer::getAddress).sorted(Comparator.comparing(Address::getPort)).collect(Collectors.toList()));
        return sb.append("\n").toString();
    }

    private void appendConnectionInfo(StringBuilder sb, Connection connection, boolean isOutbound) {
        String date = " at " + new SimpleDateFormat("HH:mm:ss.SSS").format(connection.getConnectionMetrics().getCreationDate());
        String peerAddressVerified = connection.isPeerAddressVerified() ? " !]" : " ?]";
        String peerAddress = connection.getPeerAddress().toString().replace("]", peerAddressVerified);
        String dir = isOutbound ? " --> " : " <-- ";
        sb.append(node).append(dir).append(peerAddress).append(date).append("\n");
    }
}