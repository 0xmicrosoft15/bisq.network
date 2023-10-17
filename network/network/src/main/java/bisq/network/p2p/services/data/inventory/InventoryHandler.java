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

import bisq.common.encoding.Hex;
import bisq.network.NetworkService;
import bisq.network.p2p.message.NetworkMessage;
import bisq.network.p2p.node.CloseReason;
import bisq.network.p2p.node.Connection;
import bisq.network.p2p.node.Node;
import bisq.network.p2p.services.data.filter.DataFilter;
import bisq.network.p2p.services.data.storage.append.AddAppendOnlyDataRequest;
import bisq.network.p2p.services.data.storage.auth.AddAuthenticatedDataRequest;
import bisq.network.p2p.services.data.storage.auth.RemoveAuthenticatedDataRequest;
import bisq.network.p2p.services.data.storage.mailbox.AddMailboxRequest;
import bisq.network.p2p.services.data.storage.mailbox.RemoveMailboxRequest;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

import static java.util.concurrent.CompletableFuture.supplyAsync;

@Getter
@Slf4j
class InventoryHandler implements Connection.Listener {
    private final Node node;
    private final Connection connection;
    private final CompletableFuture<Inventory> future = new CompletableFuture<>();
    private final int nonce;
    private long ts;

    InventoryHandler(Node node, Connection connection) {
        this.node = node;
        this.connection = connection;

        nonce = new Random().nextInt();
        connection.addListener(this);
    }

    CompletableFuture<Inventory> request(DataFilter dataFilter) {
        // log.debug("Node {} send GetInventoryRequest to {} with dataFilter {} and nonce {}. Connection={}",
        //        node, connection.getPeerAddress(), dataFilter, nonce, connection.getId());
        ts = System.currentTimeMillis();
        supplyAsync(() -> node.send(new InventoryRequest(dataFilter, nonce), connection), NetworkService.NETWORK_IO_POOL)
                .whenComplete((c, throwable) -> {
                    if (throwable != null) {
                        future.completeExceptionally(throwable);
                        dispose();
                    }
                });
        return future;
    }

    @Override
    public void onNetworkMessage(NetworkMessage networkMessage) {
        if (networkMessage instanceof InventoryResponse) {
            InventoryResponse response = (InventoryResponse) networkMessage;
            if (response.getRequestNonce() == nonce) {
                Map<String, List<String>> details = new HashMap<>();
                response.getInventory().getEntries()
                        .forEach(entry -> {
                            String key = entry.getClass().getSimpleName();
                            String data = null;
                            if (entry instanceof AddAuthenticatedDataRequest) {
                                AddAuthenticatedDataRequest addRequest = (AddAuthenticatedDataRequest) entry;
                                data = addRequest.getAuthenticatedSequentialData().getAuthenticatedData().getDistributedData().getClass().getSimpleName();
                            } else if (entry instanceof RemoveAuthenticatedDataRequest) {
                                RemoveAuthenticatedDataRequest removeRequest = (RemoveAuthenticatedDataRequest) entry;
                                data = Hex.encode(removeRequest.getHash());
                                key += ": Hashes";
                            } else if (entry instanceof AddMailboxRequest) {
                                AddMailboxRequest addRequest = (AddMailboxRequest) entry;
                                data = addRequest.getMailboxSequentialData().getMailboxData().getConfidentialMessage().getReceiverKeyId();
                                key += ": ReceiverKeyIds";
                            } else if (entry instanceof RemoveMailboxRequest) {
                                RemoveMailboxRequest removeRequest = (RemoveMailboxRequest) entry;
                                data = Hex.encode(removeRequest.getHash());
                                key += ": Hashes";
                            } else if (entry instanceof AddAppendOnlyDataRequest) {
                                AddAppendOnlyDataRequest addRequest = (AddAppendOnlyDataRequest) entry;
                                data = addRequest.getAppendOnlyData().getClass().getSimpleName();
                            }
                            if (data != null) {
                                details.putIfAbsent(key, new ArrayList<>());
                                List<String> list = details.get(key);
                                list.add(data);
                                details.put(key, list);
                            }
                        });
                log.info("\n##########################################################################################\n" +
                        "## INVENTORY from: " + connection.getPeerAddress() + "\n" +
                        "##########################################################################################\n" +
                        details.entrySet().stream().map(e -> e.getValue().size() + " " + e.getKey() + ": " + e.getValue())
                                .collect(Collectors.joining("\n")) +
                        "\n##########################################################################################");
                removeListeners();
                connection.getMetrics().addRtt(ts = System.currentTimeMillis() - ts);
                future.complete(response.getInventory());
            } else {
                log.warn("Node {} received Pong from {} with invalid nonce {}. Request nonce was {}. Connection={}",
                        node, connection.getPeerAddress(), response.getRequestNonce(), nonce, connection.getId());
            }
        }
    }

    @Override
    public void onConnectionClosed(CloseReason closeReason) {
        dispose();
    }

    void dispose() {
        removeListeners();
        future.cancel(true);
    }

    private void removeListeners() {
        connection.removeListener(this);
    }
}