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


import bisq.common.threading.ExecutorFactory;
import bisq.common.util.CompletableFutureUtils;
import bisq.common.util.NetworkUtils;
import bisq.network.http.HttpService;
import bisq.network.http.common.BaseHttpClient;
import bisq.network.p2p.ServiceNode;
import bisq.network.p2p.ServiceNodesByTransport;
import bisq.network.p2p.message.NetworkMessage;
import bisq.network.p2p.node.Address;
import bisq.network.p2p.node.Node;
import bisq.network.p2p.node.transport.Transport;
import bisq.network.p2p.services.confidential.ConfidentialMessageService;
import bisq.network.p2p.services.confidential.MessageListener;
import bisq.network.p2p.services.data.DataService;
import bisq.network.p2p.services.data.storage.DistributedData;
import bisq.network.p2p.services.data.storage.StorageService;
import bisq.network.p2p.services.data.storage.append.AppendOnlyData;
import bisq.network.p2p.services.data.storage.auth.DefaultAuthenticatedData;
import bisq.network.p2p.services.data.storage.auth.authorized.AuthorizedData;
import bisq.network.p2p.services.data.storage.auth.authorized.AuthorizedDistributedData;
import bisq.network.p2p.services.peergroup.PeerGroupService;
import bisq.persistence.Persistence;
import bisq.persistence.PersistenceClient;
import bisq.persistence.PersistenceService;
import bisq.security.KeyPairService;
import bisq.security.PubKey;
import bisq.security.SignatureUtil;
import com.runjva.sourceforge.jsocks.protocol.Socks5Proxy;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import java.security.GeneralSecurityException;
import java.security.KeyPair;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.stream.Collectors;

import static bisq.network.p2p.node.transport.Transport.Type.*;
import static bisq.network.p2p.services.data.DataService.BroadCastDataResult;
import static bisq.network.p2p.services.data.DataService.Listener;
import static com.google.common.base.Preconditions.checkArgument;
import static java.util.concurrent.CompletableFuture.completedFuture;
import static java.util.concurrent.CompletableFuture.supplyAsync;

/**
 * High level API for network access to p2p network as well to http services (over Tor). If user has only I2P selected
 * for p2p network a tor instance will be still bootstrapped for usage for the http requests. Only if user has
 * clearNet enabled clearNet is used for https.
 */
@Slf4j
public class NetworkService implements PersistenceClient<NetworkIdStore> {
    public static final ExecutorService NETWORK_IO_POOL = ExecutorFactory.newCachedThreadPool("NetworkService.network-IO-pool");
    public static final ExecutorService DISPATCHER = ExecutorFactory.newSingleThreadExecutor("NetworkService.dispatcher");
    private final Map<Transport.Type, Integer> defaultNodePortByTransportType;

    public static class InitializeServerResult extends HashMap<Transport.Type, CompletableFuture<Boolean>> {
        public InitializeServerResult(Map<Transport.Type, CompletableFuture<Boolean>> map) {
            super(map);
        }
    }

    public static class SendMessageResult extends HashMap<Transport.Type, ConfidentialMessageService.Result> {
        public SendMessageResult(Map<Transport.Type, ConfidentialMessageService.Result> map) {
            super(map);
        }

        public SendMessageResult() {
            super();
        }
    }

    public static final class Config {
        private final String baseDir;
        private final Transport.Config transportConfig;
        private final Set<Transport.Type> supportedTransportTypes;
        private final ServiceNode.Config serviceNodeConfig;
        private final Map<Transport.Type, PeerGroupService.Config> peerGroupServiceConfigByTransport;
        private final Map<Transport.Type, Integer> defaultNodePortByTransportType;
        private final Map<Transport.Type, Set<Address>> seedAddressesByTransport;
        private final Optional<String> socks5ProxyAddress;

        public Config(String baseDir,
                      Transport.Config transportConfig,
                      Set<Transport.Type> supportedTransportTypes,
                      ServiceNode.Config serviceNodeConfig,
                      Map<Transport.Type, PeerGroupService.Config> peerGroupServiceConfigByTransport,
                      Map<Transport.Type, Integer> defaultNodePortByTransportType,
                      Map<Transport.Type, Set<Address>> seedAddressesByTransport,
                      Optional<String> socks5ProxyAddress) {
            this.baseDir = baseDir;
            this.transportConfig = transportConfig;
            this.supportedTransportTypes = supportedTransportTypes;
            this.serviceNodeConfig = serviceNodeConfig;
            this.peerGroupServiceConfigByTransport = peerGroupServiceConfigByTransport;
            this.defaultNodePortByTransportType = defaultNodePortByTransportType;
            this.seedAddressesByTransport = seedAddressesByTransport;
            this.socks5ProxyAddress = socks5ProxyAddress;
        }

        public String baseDir() {
            return baseDir;
        }

        public Transport.Config transportConfig() {
            return transportConfig;
        }

        public Set<Transport.Type> supportedTransportTypes() {
            return supportedTransportTypes;
        }

        public ServiceNode.Config serviceNodeConfig() {
            return serviceNodeConfig;
        }

        public Map<Transport.Type, PeerGroupService.Config> peerGroupServiceConfigByTransport() {
            return peerGroupServiceConfigByTransport;
        }

        public Map<Transport.Type, Integer> defaultNodePortByTransportType() {
            return defaultNodePortByTransportType;
        }

        public Map<Transport.Type, Set<Address>> seedAddressesByTransport() {
            return seedAddressesByTransport;
        }

        public Optional<String> socks5ProxyAddress() {
            return socks5ProxyAddress;
        }

        @Override
        public boolean equals(Object obj) {
            if (obj == this) return true;
            if (obj == null || obj.getClass() != this.getClass()) return false;
            var that = (Config) obj;
            return Objects.equals(this.baseDir, that.baseDir) &&
                    Objects.equals(this.transportConfig, that.transportConfig) &&
                    Objects.equals(this.supportedTransportTypes, that.supportedTransportTypes) &&
                    Objects.equals(this.serviceNodeConfig, that.serviceNodeConfig) &&
                    Objects.equals(this.peerGroupServiceConfigByTransport, that.peerGroupServiceConfigByTransport) &&
                    Objects.equals(this.defaultNodePortByTransportType, that.defaultNodePortByTransportType) &&
                    Objects.equals(this.seedAddressesByTransport, that.seedAddressesByTransport) &&
                    Objects.equals(this.socks5ProxyAddress, that.socks5ProxyAddress);
        }

        @Override
        public int hashCode() {
            return Objects.hash(baseDir, transportConfig, supportedTransportTypes, serviceNodeConfig, peerGroupServiceConfigByTransport, defaultNodePortByTransportType, seedAddressesByTransport, socks5ProxyAddress);
        }

        @Override
        public String toString() {
            return "Config[" +
                    "baseDir=" + baseDir + ", " +
                    "transportConfig=" + transportConfig + ", " +
                    "supportedTransportTypes=" + supportedTransportTypes + ", " +
                    "serviceNodeConfig=" + serviceNodeConfig + ", " +
                    "peerGroupServiceConfigByTransport=" + peerGroupServiceConfigByTransport + ", " +
                    "defaultNodePortByTransportType=" + defaultNodePortByTransportType + ", " +
                    "seedAddressesByTransport=" + seedAddressesByTransport + ", " +
                    "socks5ProxyAddress=" + socks5ProxyAddress + ']';
        }

    }

    @Getter
    private final NetworkIdStore persistableStore = new NetworkIdStore();
    @Getter
    private final Persistence<NetworkIdStore> persistence;
    private final KeyPairService keyPairService;
    @Getter
    private final HttpService httpService;
    @Getter
    private final Optional<String> socks5ProxyAddress; // Optional proxy address of external tor instance 
    @Getter
    private final Set<Transport.Type> supportedTransportTypes;
    @Getter
    private final ServiceNodesByTransport serviceNodesByTransport;
    @Getter
    private final Optional<DataService> dataService;

    public NetworkService(Config config,
                          PersistenceService persistenceService,
                          KeyPairService keyPairService) {
        this.keyPairService = keyPairService;
        httpService = new HttpService();

        boolean supportsDataService = config.serviceNodeConfig().getServices().contains(ServiceNode.Service.DATA);
        dataService = supportsDataService ? Optional.of(new DataService(new StorageService(persistenceService))) : Optional.empty();

        socks5ProxyAddress = config.socks5ProxyAddress;
        supportedTransportTypes = config.supportedTransportTypes();
        serviceNodesByTransport = new ServiceNodesByTransport(config.transportConfig(),
                supportedTransportTypes,
                config.serviceNodeConfig(),
                config.peerGroupServiceConfigByTransport,
                config.seedAddressesByTransport(),
                dataService,
                keyPairService,
                persistenceService);

        defaultNodePortByTransportType = config.defaultNodePortByTransportType();

        persistence = persistenceService.getOrCreatePersistence(this, persistableStore);
    }

    public CompletableFuture<Boolean> shutdown() {
        return CompletableFutureUtils.allOf(serviceNodesByTransport.shutdown(), httpService.shutdown())
                .thenCompose(list -> dataService.map(DataService::shutdown).orElse(completedFuture(true)));
    }


    ///////////////////////////////////////////////////////////////////////////////////////////////////
    // Initialize server
    ///////////////////////////////////////////////////////////////////////////////////////////////////

    public InitializeServerResult maybeInitializeServer(String nodeId, PubKey pubKey) {
        return maybeInitializeServer(getOrCreatePortByTransport(nodeId), nodeId, pubKey);
    }

    public InitializeServerResult maybeInitializeServer(Map<Transport.Type, Integer> portByTransport,
                                                        String nodeId,
                                                        PubKey pubKey) {
        InitializeServerResult initializeServerResult = serviceNodesByTransport.maybeInitializeServer(portByTransport, nodeId);
        // After server has been started we can be sure the networkId is available. 
        // If it was not already available before we persist it.
        initializeServerResult.values()
                .forEach(future -> future
                        .whenComplete((result, throwable) -> {
                            if (throwable == null) {
                                findOrPersistNewNetworkId(nodeId, pubKey);
                            }
                        }));
        return initializeServerResult;
    }


    ///////////////////////////////////////////////////////////////////////////////////////////////////
    // Get networkId and initialize server
    ///////////////////////////////////////////////////////////////////////////////////////////////////

    public CompletableFuture<NetworkId> getInitializedNetworkId(String nodeId, PubKey pubKey) {
        CompletableFuture<NetworkId> future = new CompletableFuture<>();
        maybeInitializeServer(nodeId, pubKey)
                .forEach((transportType, resultFuture) -> {
                    resultFuture.whenComplete((initializeServerResult, throwable) -> {
                        if (throwable != null) {
                            log.error(throwable.toString()); //todo
                        }

                        findOrPersistNewNetworkId(nodeId, pubKey)
                                .ifPresentOrElse(future::complete,
                                        () -> future.completeExceptionally(new IllegalStateException("NetworkId must be present")));
                    });
                });
        return future;
    }


    ///////////////////////////////////////////////////////////////////////////////////////////////////
    // Bootstrap to gossip network (initialize default node and initialize peerGroupService
    ///////////////////////////////////////////////////////////////////////////////////////////////////

    public CompletableFuture<Boolean> bootstrapToNetwork() {
        log.info("bootstrapToNetwork");
        String nodeId = Node.DEFAULT;
        return serviceNodesByTransport.bootstrapToNetwork(getDefaultPortByTransport(), nodeId)
                .thenCompose(result -> findOrPersistNewNetworkId(nodeId, keyPairService.getDefaultPubKey())
                        .map(networkId -> CompletableFuture.completedFuture(result))
                        .orElse(CompletableFuture.failedFuture(new IllegalStateException("NetworkId not present"))));
    }


    ///////////////////////////////////////////////////////////////////////////////////////////////////
    // Send confidential message
    ///////////////////////////////////////////////////////////////////////////////////////////////////

    public CompletableFuture<SendMessageResult> sendMessage(NetworkMessage networkMessage,
                                                            NetworkId receiverNetworkId,
                                                            KeyPair senderKeyPair,
                                                            String senderNodeId) {
        return supplyAsync(() -> serviceNodesByTransport.confidentialSend(networkMessage, receiverNetworkId, senderKeyPair, senderNodeId), NETWORK_IO_POOL);
    }

    public CompletableFuture<SendMessageResult> sendMessage(NetworkMessage networkMessage,
                                                            NetworkId receiverNetworkId,
                                                            NetworkIdWithKeyPair senderNetworkIdWithKeyPair) {
        return supplyAsync(() -> serviceNodesByTransport.confidentialSend(networkMessage,
                        receiverNetworkId,
                        senderNetworkIdWithKeyPair.getKeyPair(),
                        senderNetworkIdWithKeyPair.getNodeId()),
                NETWORK_IO_POOL);
    }


    ///////////////////////////////////////////////////////////////////////////////////////////////////
    // Add/remove data
    ///////////////////////////////////////////////////////////////////////////////////////////////////

    public CompletableFuture<BroadCastDataResult> publishAuthenticatedData(DistributedData distributedData, NetworkIdWithKeyPair ownerNetworkIdWithKeyPair) {
        checkArgument(dataService.isPresent(), "DataService must be supported when addData is called.");
        String nodeId = ownerNetworkIdWithKeyPair.getNodeId();
        PubKey pubKey = ownerNetworkIdWithKeyPair.getPubKey();
        KeyPair keyPair = ownerNetworkIdWithKeyPair.getKeyPair();
        return CompletableFutureUtils.allOf(maybeInitializeServer(nodeId, pubKey).values()) //todo
                .thenCompose(list -> {
                    DefaultAuthenticatedData authenticatedData = new DefaultAuthenticatedData(distributedData);
                    return dataService.get().addAuthenticatedData(authenticatedData, keyPair)
                            .whenComplete((broadCastResultFutures, throwable) -> {
                                broadCastResultFutures.forEach((key, value) -> value.whenComplete((broadcastResult, throwable2) -> {
                                    //todo apply state
                                }));
                            });
                });
    }

    public CompletableFuture<BroadCastDataResult> removeAuthenticatedData(DistributedData distributedData, NetworkIdWithKeyPair ownerNetworkIdWithKeyPair) {
        checkArgument(dataService.isPresent(), "DataService must be supported when removeData is called.");
        String nodeId = ownerNetworkIdWithKeyPair.getNodeId();
        PubKey pubKey = ownerNetworkIdWithKeyPair.getPubKey();
        KeyPair keyPair = ownerNetworkIdWithKeyPair.getKeyPair();
        return CompletableFutureUtils.allOf(maybeInitializeServer(nodeId, pubKey).values())
                .thenCompose(list -> {
                    DefaultAuthenticatedData authenticatedData = new DefaultAuthenticatedData(distributedData);
                    return dataService.get().removeAuthenticatedData(authenticatedData, keyPair)
                            .whenComplete((broadCastDataResult, throwable) -> {
                                broadCastDataResult.forEach((key, value) -> value.whenComplete((broadcastResult, throwable2) -> {
                                    //todo apply state
                                }));
                            });
                });
    }

    public CompletableFuture<BroadCastDataResult> publishAuthorizedData(AuthorizedDistributedData authorizedDistributedData,
                                                                        NetworkIdWithKeyPair ownerNetworkIdWithKeyPair,
                                                                        PrivateKey authorizedPrivateKey,
                                                                        PublicKey authorizedPublicKey) {
        checkArgument(dataService.isPresent(), "DataService must be supported when addData is called.");
        String nodeId = ownerNetworkIdWithKeyPair.getNodeId();
        PubKey pubKey = ownerNetworkIdWithKeyPair.getPubKey();
        KeyPair keyPair = ownerNetworkIdWithKeyPair.getKeyPair();
        return CompletableFutureUtils.allOf(maybeInitializeServer(nodeId, pubKey).values()) //todo
                .thenCompose(list -> {
                    try {
                        byte[] signature = SignatureUtil.sign(authorizedDistributedData.serialize(), authorizedPrivateKey);
                        AuthorizedData authorizedData = new AuthorizedData(authorizedDistributedData, signature, authorizedPublicKey);
                        return dataService.get().addAuthenticatedData(authorizedData, keyPair)
                                .whenComplete((broadCastResultFutures, throwable) -> {
                                    broadCastResultFutures.forEach((key, value) -> value.whenComplete((broadcastResult, throwable2) -> {
                                        //todo apply state
                                    }));
                                });
                    } catch (GeneralSecurityException e) {
                        e.printStackTrace();
                        return CompletableFuture.failedFuture(e);
                    }
                });
    }

    public CompletableFuture<BroadCastDataResult> publishAppendOnlyData(AppendOnlyData appendOnlyData,
                                                                        NetworkIdWithKeyPair ownerNetworkIdWithKeyPair) {
        checkArgument(dataService.isPresent(), "DataService must be supported when addData is called.");
        String nodeId = ownerNetworkIdWithKeyPair.getNodeId();
        PubKey pubKey = ownerNetworkIdWithKeyPair.getPubKey();
        return CompletableFutureUtils.allOf(maybeInitializeServer(nodeId, pubKey).values())
                .thenCompose(list -> dataService.get().addAppendOnlyData(appendOnlyData)
                        .whenComplete((broadCastResultFutures, throwable) -> {
                            broadCastResultFutures.forEach((key, value) -> value.whenComplete((broadcastResult, throwable2) -> {
                                //todo apply state
                            }));
                        }));
    }


    ///////////////////////////////////////////////////////////////////////////////////////////////////
    // Listeners
    ///////////////////////////////////////////////////////////////////////////////////////////////////

    public void addDataServiceListener(Listener listener) {
        dataService.orElseThrow().addListener(listener);
    }

    public void removeDataServiceListener(Listener listener) {
        dataService.orElseThrow().removeListener(listener);
    }

    public void addMessageListener(MessageListener messageListener) {
        serviceNodesByTransport.addMessageListener(messageListener);
    }

    public void removeMessageListener(MessageListener messageListener) {
        serviceNodesByTransport.removeMessageListener(messageListener);
    }

    public void addDefaultNodeListener(Node.Listener nodeListener) {
        serviceNodesByTransport.addDefaultNodeListener(nodeListener);
    }

    public void removeDefaultNodeListener(Node.Listener nodeListener) {
        serviceNodesByTransport.removeDefaultNodeListener(nodeListener);
    }


    ///////////////////////////////////////////////////////////////////////////////////////////////////
    // Getters
    ///////////////////////////////////////////////////////////////////////////////////////////////////

    public BaseHttpClient getHttpClient(String url, String userAgent, Transport.Type transportType) {
        // socksProxy only supported for TOR
        Optional<Socks5Proxy> socksProxy = transportType == TOR ? serviceNodesByTransport.getSocksProxy() : Optional.empty();
        return httpService.getHttpClient(url, userAgent, transportType, socksProxy, socks5ProxyAddress);
    }

    public Map<Transport.Type, Map<String, Address>> getMyAddresses() {
        return serviceNodesByTransport.getAddressesByNodeIdMapByTransportType();
    }

    public Map<Transport.Type, Address> getAddressByNetworkType(String nodeId) {
        return supportedTransportTypes.stream()
                .filter(transportType -> findAddress(transportType, nodeId).isPresent())
                .collect(Collectors.toMap(transportType -> transportType,
                        transportType -> findAddress(transportType, nodeId).orElseThrow()));
    }

    public Map<Transport.Type, ServiceNode.State> getStateByTransportType() {
        return serviceNodesByTransport.getStateByTransportType();
    }

    public boolean isTransportTypeSupported(Transport.Type transportType) {
        return getSupportedTransportTypes().contains(transportType);
    }

    // We return the port by transport type if found from the persisted networkId, otherwise we
    // fill in a random free system port for all supported transport types. 
    private Map<Transport.Type, Integer> getDefaultPortByTransport() {
        return getOrCreatePortByTransport(Node.DEFAULT);
    }

    private Map<Transport.Type, Integer> getOrCreatePortByTransport(String nodeId) {
        Optional<NetworkId> networkIdOptional = findNetworkId(nodeId);
        // If we have a persisted networkId we take that port otherwise we take random system port.  
        Map<Transport.Type, Integer> persistedOrRandomPortByTransport = supportedTransportTypes.stream()
                .collect(Collectors.toMap(transportType -> transportType,
                        transportType -> networkIdOptional.stream()
                                .map(NetworkId::getAddressByNetworkType)
                                .flatMap(addressByNetworkType -> Optional.ofNullable(addressByNetworkType.get(transportType)).stream())
                                .map(Address::getPort)
                                .findAny()
                                .orElse(NetworkUtils.findFreeSystemPort())));

        // In case of the default node and if we have defined ports in the config we take those.
        Map<Transport.Type, Integer> portByTransport = new HashMap<>();
        if (nodeId.equals(Node.DEFAULT)) {
            if (defaultNodePortByTransportType.containsKey(CLEAR)) {
                portByTransport.put(CLEAR, defaultNodePortByTransportType.get(CLEAR));
            } else {
                portByTransport.put(CLEAR, persistedOrRandomPortByTransport.get(CLEAR));
            }
            if (defaultNodePortByTransportType.containsKey(TOR)) {
                portByTransport.put(TOR, defaultNodePortByTransportType.get(TOR));
            } else {
                portByTransport.put(TOR, persistedOrRandomPortByTransport.get(TOR));
            }
            if (defaultNodePortByTransportType.containsKey(I2P)) {
                portByTransport.put(I2P, defaultNodePortByTransportType.get(I2P));
            } else {
                portByTransport.put(I2P, persistedOrRandomPortByTransport.get(I2P));
            }
        } else {
            portByTransport = persistedOrRandomPortByTransport;
        }
        return portByTransport;
    }


    ///////////////////////////////////////////////////////////////////////////////////////////////////
    // Get optional 
    ///////////////////////////////////////////////////////////////////////////////////////////////////

    public Optional<ServiceNode> findServiceNode(Transport.Type transport) {
        return serviceNodesByTransport.findServiceNode(transport);
    }

    public Optional<Node> findDefaultNode(Transport.Type transport) {
        return findNode(transport, Node.DEFAULT);
    }

    public Optional<Node> findNode(Transport.Type transport, String nodeId) {
        return serviceNodesByTransport.findNode(transport, nodeId);
    }

    public Optional<Map<String, Address>> findAddressesByNodeId(Transport.Type transport) {
        return serviceNodesByTransport.findAddressesByNodeId(transport);
    }

    public Optional<Address> findAddress(Transport.Type transport, String nodeId) {
        return serviceNodesByTransport.findAddress(transport, nodeId);
    }

    public Optional<NetworkId> findNetworkId(String nodeId) {
        return Optional.ofNullable(persistableStore.getNetworkIdByNodeId().get(nodeId));
    }

    public Optional<NetworkId> createNetworkId(String nodeId, PubKey pubKey) {
        Map<Transport.Type, Address> addressByNetworkType = getAddressByNetworkType(nodeId);
        if (supportedTransportTypes.size() == addressByNetworkType.size()) {
            return Optional.of(new NetworkId(addressByNetworkType, pubKey, nodeId));
        } else {
            //todo not sure if that is a valid case or better treated as exception
            log.error("supportedTransportTypes.size() != addressByNetworkType.size(). " +
                            "supportedTransportTypes={}, addressByNetworkType={}",
                    supportedTransportTypes, addressByNetworkType);
            return Optional.empty();
        }
    }

    private Optional<NetworkId> findOrPersistNewNetworkId(String nodeId, PubKey pubKey) {
        return findNetworkId(nodeId)
                .or(() -> createNetworkId(nodeId, pubKey)
                        .map(networkId -> {
                            persistableStore.getNetworkIdByNodeId().put(nodeId, networkId);
                            persist();
                            return networkId;
                        })
                );
    }
}
