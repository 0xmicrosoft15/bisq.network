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


import bisq.common.util.CompletableFutureUtils;
import bisq.network.common.Address;
import bisq.network.identity.NetworkId;
import bisq.network.p2p.message.EnvelopePayloadMessage;
import bisq.network.p2p.node.authorization.AuthorizationService;
import bisq.network.p2p.node.network_load.NetworkLoadService;
import bisq.network.p2p.node.transport.TransportService;
import bisq.network.p2p.services.peergroup.BanList;
import bisq.security.keys.KeyBundleService;

import java.util.Collection;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.concurrent.TimeUnit;
import java.util.stream.Stream;

import static com.google.common.base.Preconditions.checkArgument;

/**
 * Maintains a map with nodes by nodeId.
 * Provides delegate methods to node with given nodeId
 */
public class NodesById implements Node.Listener {
    public interface Listener {
        void onNodeAdded(Node node);

        default void onNodeRemoved(Node node) {
        }
    }

    private final BanList banList;
    private final Node.Config nodeConfig;
    private final KeyBundleService keyBundleService;
    private final TransportService transportService;
    private final NetworkLoadService networkLoadService;
    private final AuthorizationService authorizationService;
    private final Map<NetworkId, Node> map = new ConcurrentHashMap<>();
    private final Set<Listener> listeners = new CopyOnWriteArraySet<>();
    private final Set<Node.Listener> nodeListeners = new CopyOnWriteArraySet<>();

    public NodesById(BanList banList,
                     Node.Config nodeConfig,
                     KeyBundleService keyBundleService,
                     TransportService transportService,
                     NetworkLoadService networkLoadService,
                     AuthorizationService authorizationService) {
        this.banList = banList;
        this.nodeConfig = nodeConfig;
        this.keyBundleService = keyBundleService;
        this.transportService = transportService;
        this.networkLoadService = networkLoadService;
        this.authorizationService = authorizationService;
    }

    ///////////////////////////////////////////////////////////////////////////////////////////////////
    // API
    ///////////////////////////////////////////////////////////////////////////////////////////////////

    public Node createAndConfigNode(NetworkId networkId, boolean isDefaultNode) {
        Node node = new Node(networkId, isDefaultNode, nodeConfig, banList, keyBundleService, transportService, networkLoadService, authorizationService);
        map.put(networkId, node);
        node.addListener(this);
        listeners.forEach(listener -> listener.onNodeAdded(node));
        return node;
    }

    public Node initializeNode(NetworkId networkId) {
        Node node = getOrCreateNode(networkId);
        node.initialize();   // blocking
        return node;
    }

    public Connection getConnection(NetworkId networkId, Address address) {
        return getOrCreateNode(networkId).getConnection(address);
    }

    public Connection send(NetworkId senderNetworkId, EnvelopePayloadMessage envelopePayloadMessage, Address address) {
        return getOrCreateNode(senderNetworkId).send(envelopePayloadMessage, address);
    }

    public Connection send(NetworkId senderNetworkId, EnvelopePayloadMessage envelopePayloadMessage, Connection connection) {
        return getOrCreateNode(senderNetworkId).send(envelopePayloadMessage, connection);
    }

    public CompletableFuture<Boolean> shutdown() {
        Stream<CompletableFuture<Boolean>> futures = map.values().stream().map(Node::shutdown);
        return CompletableFutureUtils.allOf(futures)
                .orTimeout(10, TimeUnit.SECONDS)
                .handle((list, throwable) -> {
                    map.clear();
                    listeners.clear();
                    nodeListeners.clear();
                    return throwable == null && list.stream().allMatch(e -> e);
                });
    }

    public boolean isNodeInitialized(NetworkId networkId) {
        return findNode(networkId)
                .map(Node::isInitialized)
                .orElse(false);
    }

    public void assertNodeIsInitialized(NetworkId networkId) {
        checkArgument(isNodeInitialized(networkId), "Node must be present and initialized");
    }

    public Optional<Node> findNode(NetworkId networkId) {
        return Optional.ofNullable(map.get(networkId));
    }

    public Collection<Node> getAllNodes() {
        return map.values();
    }

    public void addNodeListener(Node.Listener listener) {
        nodeListeners.add(listener);
    }

    public void removeNodeListener(Node.Listener listener) {
        nodeListeners.remove(listener);
    }

    public void addListener(Listener listener) {
        listeners.add(listener);
    }

    public void removeListener(Listener listener) {
        listeners.remove(listener);
    }


    ///////////////////////////////////////////////////////////////////////////////////////////////////
    // Node.Listener
    ///////////////////////////////////////////////////////////////////////////////////////////////////

    @Override
    public void onMessage(EnvelopePayloadMessage envelopePayloadMessage, Connection connection, NetworkId networkId) {
        nodeListeners.forEach(listener -> listener.onMessage(envelopePayloadMessage, connection, networkId));
    }

    @Override
    public void onConnection(Connection connection) {
        nodeListeners.forEach(listener -> listener.onConnection(connection));
    }

    @Override
    public void onDisconnect(Connection connection, CloseReason closeReason) {
        nodeListeners.forEach(listener -> listener.onDisconnect(connection, closeReason));
    }

    @Override
    public void onShutdown(Node node) {
        map.remove(node.getNetworkId());
        node.removeListener(this);
        listeners.forEach(listener -> listener.onNodeRemoved(node));
        nodeListeners.forEach(listener -> listener.onShutdown(node));
    }


    ///////////////////////////////////////////////////////////////////////////////////////////////////
    // Private
    ///////////////////////////////////////////////////////////////////////////////////////////////////

    private Node getOrCreateNode(NetworkId networkId) {
        return findNode(networkId)
                .orElseGet(() -> createAndConfigNode(networkId, false));
    }
}
