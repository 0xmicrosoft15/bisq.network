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

package bisq.network.p2p.services.data.inventory;

import bisq.common.application.DevMode;
import bisq.common.observable.Observable;
import bisq.common.timer.Scheduler;
import bisq.common.util.CompletableFutureUtils;
import bisq.network.identity.NetworkId;
import bisq.network.p2p.message.EnvelopePayloadMessage;
import bisq.network.p2p.node.CloseReason;
import bisq.network.p2p.node.Connection;
import bisq.network.p2p.node.Feature;
import bisq.network.p2p.node.Node;
import bisq.network.p2p.services.data.AddDataRequest;
import bisq.network.p2p.services.data.DataService;
import bisq.network.p2p.services.data.RemoveDataRequest;
import bisq.network.p2p.services.data.inventory.filter.FilterService;
import bisq.network.p2p.services.data.inventory.filter.InventoryFilter;
import bisq.network.p2p.services.data.inventory.filter.InventoryFilterType;
import bisq.network.p2p.services.peer_group.PeerGroupManager;
import bisq.network.p2p.services.peer_group.PeerGroupService;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Slf4j
public class InventoryRequestService implements Node.Listener, PeerGroupManager.Listener {
    private static final long TIMEOUT = TimeUnit.SECONDS.toMillis(120);

    private final Node node;
    private final PeerGroupManager peerGroupManager;
    private final PeerGroupService peerGroupService;
    private final DataService dataService;
    private final Map<InventoryFilterType, FilterService<? extends InventoryFilter>> supportedFilterServices;
    private final List<InventoryFilterType> myPreferredInventoryFilterTypes;
    private final long repeatRequestInterval;
    private final int maxSeedsForRequest;
    private final int maxPeersForRequest;
    private final int maxPendingRequests;
    private final Map<String, InventoryHandler> requestHandlerMap = new ConcurrentHashMap<>();
    private final AtomicBoolean isRepeatedRequest = new AtomicBoolean();
    private Optional<Scheduler> retryScheduler = Optional.empty();
    private Optional<Scheduler> repeatRequestScheduler = Optional.empty();
    private Optional<Scheduler> initialDelayScheduler = Optional.empty();
    @Getter
    private final Observable<Integer> numPendingRequests = new Observable<>(0);
    @Getter
    private final Observable<Boolean> allDataReceived = new Observable<>(false);

    public InventoryRequestService(Node node,
                                   PeerGroupManager peerGroupManager,
                                   DataService dataService,
                                   Map<InventoryFilterType, FilterService<? extends InventoryFilter>> supportedFilterServices,
                                   InventoryService.Config config) {
        this.node = node;
        this.peerGroupManager = peerGroupManager;
        peerGroupService = peerGroupManager.getPeerGroupService();
        this.dataService = dataService;
        this.supportedFilterServices = supportedFilterServices;
        myPreferredInventoryFilterTypes = config.getMyPreferredFilterTypes();
        repeatRequestInterval = config.getRepeatRequestInterval();
        maxSeedsForRequest = config.getMaxSeedsForRequest();
        maxPeersForRequest = config.getMaxPeersForRequest();
        maxPendingRequests = config.getMaxPendingRequests();
        node.addListener(this);
        peerGroupManager.addListener(this);
    }

    public void shutdown() {
        node.removeListener(this);
        peerGroupManager.removeListener(this);
        requestHandlerMap.values().forEach(InventoryHandler::dispose);
        requestHandlerMap.clear();
        numPendingRequests.set(0);
        retryScheduler.ifPresent(Scheduler::stop);
        repeatRequestScheduler.ifPresent(Scheduler::stop);
        initialDelayScheduler.ifPresent(Scheduler::stop);
    }


    ///////////////////////////////////////////////////////////////////////////////////////////////////
    // Node.Listener
    ///////////////////////////////////////////////////////////////////////////////////////////////////

    @Override
    public void onMessage(EnvelopePayloadMessage envelopePayloadMessage, Connection connection, NetworkId networkId) {
    }

    @Override
    public void onConnection(Connection connection) {
        if (sufficientConnections()) {
            maybeRequestInventory();
        }
    }

    @Override
    public void onDisconnect(Connection connection, CloseReason closeReason) {
        String key = getRequestHandlerMapKey(connection);
        if (requestHandlerMap.containsKey(key)) {
            requestHandlerMap.get(key).dispose();
            requestHandlerMap.remove(key);
            numPendingRequests.set(requestHandlerMap.size());
        }
    }


    ///////////////////////////////////////////////////////////////////////////////////////////////////
    // PeerGroupManager.Listener
    ///////////////////////////////////////////////////////////////////////////////////////////////////

    @Override
    public void onStateChanged(PeerGroupManager.State state) {
        if (state == PeerGroupManager.State.RUNNING) {
            initialDelayScheduler.ifPresent(Scheduler::stop);
            int delay = DevMode.isDevMode() ? 100 : 1000;
            initialDelayScheduler = Optional.of(Scheduler.run(this::maybeRequestInventory).after(delay));
        }
    }


    ///////////////////////////////////////////////////////////////////////////////////////////////////
    // Request inventory
    ///////////////////////////////////////////////////////////////////////////////////////////////////

    private void maybeRequestInventory() {
        if ((allDataReceived.get() && !isRepeatedRequest.get()) || requestHandlerMap.size() > maxPendingRequests) {
            return;
        }

        if (peerGroupManager.getState().get() != PeerGroupManager.State.RUNNING) {
            // Not ready yet, lets try again later
            log.info("PeerGroupManager.State is not RUNNING. We try again in 5 sec.");
            retryScheduler.ifPresent(Scheduler::stop);
            retryScheduler = Optional.of(Scheduler.run(this::maybeRequestInventory).after(5000));
            return;
        }

        log.info("Start inventory requests");
        CompletableFutureUtils.allOf(requestFromPeers())
                .whenComplete((list, throwable) -> {
                    if (throwable != null) {
                        if (throwable instanceof CompletionException &&
                                throwable.getCause() instanceof CancellationException) {
                            log.debug("requestFromPeers failed", throwable);
                        } else {
                            log.error("requestFromPeers failed", throwable);
                        }
                    } else if (list == null) {
                        log.error("requestFromPeers completed with result list = null");
                    } else {
                        // Repeat requests until we have received all data
                        if (list.isEmpty()) {
                            log.info("No matching peers for request have been found. We try again in 10 sec.");
                            retryScheduler.ifPresent(Scheduler::stop);
                            retryScheduler = Optional.of(Scheduler.run(this::maybeRequestInventory).after(10000));
                        } else if (list.stream().noneMatch(Inventory::noDataMissing)) {
                            log.info("We completed all requests but we still miss data, so we repeat requests again in 1 sec.");
                            retryScheduler.ifPresent(Scheduler::stop);
                            retryScheduler = Optional.of(Scheduler.run(this::maybeRequestInventory).after(1000));
                        } else {
                            // We got all data
                            allDataReceived.set(true);

                            // We request again in 10 minutes to be sure that potentially missed data gets received.
                            log.info("All data have been received. We start a scheduler to repeat request inventory again in {} sec. " +
                                    "to reduce risks that we miss network data.", repeatRequestInterval / 1000);
                            isRepeatedRequest.set(false);
                            repeatRequestScheduler.ifPresent(Scheduler::stop);
                            repeatRequestScheduler = Optional.of(Scheduler.run(() -> {
                                log.info("We repeat request inventory again triggered from our scheduler.");
                                isRepeatedRequest.set(true);
                                maybeRequestInventory();
                            }).after(repeatRequestInterval));
                        }
                    }
                });
    }

    private List<CompletableFuture<Inventory>> requestFromPeers() {
        return getCandidates().stream()
                .map(connection -> {
                    // We need to handle requests from ourselves and those from our peer separate in case they happen on the same connection
                    // therefor we add the peer address
                    String key = getRequestHandlerMapKey(connection);
                    if (requestHandlerMap.containsKey(key)) {
                        return CompletableFuture.<Inventory>failedFuture(new RuntimeException("There is a pending request for key " + key));
                    }

                    InventoryHandler handler = new InventoryHandler(node, connection);
                    requestHandlerMap.put(key, handler);
                    numPendingRequests.set(requestHandlerMap.size());
                    List<Feature> peersFeatures = connection.getPeersCapability().getFeatures();
                    InventoryFilterType inventoryFilterType = getPreferredFilterType(peersFeatures).orElseThrow(); // we filtered above for presence
                    var filterService = supportedFilterServices.get(inventoryFilterType);
                    return handler.request(filterService.getFilter())
                            .orTimeout(TIMEOUT, TimeUnit.MILLISECONDS)
                            .whenComplete((inventory, throwable) -> {
                                requestHandlerMap.remove(key);
                                numPendingRequests.set(requestHandlerMap.size());
                                if (inventory != null) {
                                    inventory.getEntries().forEach(dataRequest -> {
                                        if (dataRequest instanceof AddDataRequest) {
                                            dataService.processAddDataRequest((AddDataRequest) dataRequest, false);
                                        } else if (dataRequest instanceof RemoveDataRequest) {
                                            dataService.processRemoveDataRequest((RemoveDataRequest) dataRequest, false);
                                        }
                                    });
                                }
                                if (throwable != null) {
                                    if (throwable instanceof CancellationException) {
                                        log.debug("Inventory request failed.", throwable);
                                    } else {
                                        log.info("Inventory request failed.", throwable);
                                    }
                                }
                            });
                })
                .collect(Collectors.toList());
    }

    private List<Connection> getCandidates() {
        Stream<Connection> seeds = peerGroupService.getShuffledSeedConnections(node)
                .filter(connection -> !requestHandlerMap.containsKey(getRequestHandlerMapKey(connection)))
                .limit(maxSeedsForRequest);
        Stream<Connection> peers = peerGroupService.getShuffledNonSeedConnections(node)
                .filter(connection -> !requestHandlerMap.containsKey(getRequestHandlerMapKey(connection)))
                .limit(maxPeersForRequest);
        List<Connection> allConnections = Stream.concat(seeds, peers).collect(Collectors.toList());
        List<Connection> matchingConnections = allConnections.stream()
                .filter(connection -> getPreferredFilterType(connection.getPeersCapability().getFeatures()).isPresent())
                .collect(Collectors.toList());
        if (matchingConnections.isEmpty() && !allConnections.isEmpty()) {
            log.warn("We did not find any peer which matches our inventory filter type settings");
        }

        int limit = maxPendingRequests - requestHandlerMap.size();
        List<Connection> candidates = matchingConnections.stream()
                .limit(limit)
                .collect(Collectors.toList());
        log.info("Candidates for inventory requests={}", candidates);
        return candidates;
    }

    private boolean sufficientConnections() {
        return node.getNumConnections() > peerGroupService.getTargetNumConnectedPeers() / 2;
    }

    // Get first match with peers feature based on order of myPreferredFilterTypes
    private Optional<InventoryFilterType> getPreferredFilterType(List<Feature> peersFeatures) {
        List<InventoryFilterType> peersInventoryFilterTypes = toFilterTypes(peersFeatures);
        return myPreferredInventoryFilterTypes.stream()
                .filter(peersInventoryFilterTypes::contains)
                .findFirst();
    }

    private List<InventoryFilterType> toFilterTypes(List<Feature> features) {
        return features.stream()
                .flatMap(feature -> InventoryFilterType.fromFeature(feature).stream())
                .collect(Collectors.toList());
    }

    private static String getRequestHandlerMapKey(Connection connection) {
        return connection.getPeerAddress().getFullAddress();
    }
}