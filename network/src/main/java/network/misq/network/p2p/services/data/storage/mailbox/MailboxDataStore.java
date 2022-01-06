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

package network.misq.network.p2p.services.data.storage.mailbox;

import com.google.common.annotations.VisibleForTesting;
import lombok.extern.slf4j.Slf4j;
import network.misq.common.data.ByteArray;
import network.misq.network.p2p.services.data.filter.ProtectedDataFilter;
import network.misq.network.p2p.services.data.inventory.Inventory;
import network.misq.network.p2p.services.data.inventory.InventoryUtil;
import network.misq.network.p2p.services.data.storage.MetaData;
import network.misq.network.p2p.services.data.storage.Result;
import network.misq.network.p2p.services.data.storage.auth.AuthenticatedDataRequest;
import network.misq.persistence.PersistenceService;
import network.misq.security.DigestUtil;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Slf4j
public class MailboxDataStore extends DataStore<MailboxRequest> {
    private static final long MAX_AGE = TimeUnit.DAYS.toMillis(10);
    private static final int MAX_MAP_SIZE = 10000;

    // Max size of serialized NetworkData or MailboxMessage. Used to limit response map.
    // Depends on data types max. expected size.
    // Does not contain metadata like signatures and keys as well not the overhead from encryption.
    // So this number has to be fine-tuned with real data later...
    private static final int MAX_INVENTORY_MAP_SIZE = 1_000_000;

    public interface Listener {
        void onAdded(MailboxPayload mailboxPayload);

        void onRemoved(MailboxPayload mailboxPayload);
    }

    private final int maxItems;
    private final Set<Listener> listeners = new CopyOnWriteArraySet<>();

    public MailboxDataStore(PersistenceService persistenceService, MetaData metaData) {
        super(persistenceService, metaData);

        maxItems = MAX_INVENTORY_MAP_SIZE / metaData.getMaxSizeInBytes();
    }

    @Override
    protected long getMaxWriteRateInMs() {
        return 1000;
    }

    public Result add(AddMailboxRequest request) {
        MailboxData data = request.getMailboxData();
        MailboxPayload payload = data.getMailboxPayload();
        byte[] hash = DigestUtil.hash(payload.serialize());
        ByteArray byteArray = new ByteArray(hash);
        MailboxRequest requestFromMap;
        synchronized (map) {
            requestFromMap = map.get(byteArray);
            int sequenceNumberFromMap = requestFromMap != null ? requestFromMap.getSequenceNumber() : 0;

            if (request.equals(requestFromMap)) {
                return new Result(false).requestAlreadyReceived();
            }

            if (requestFromMap != null && data.isSequenceNrInvalid(sequenceNumberFromMap)) {
                return new Result(false).sequenceNrInvalid();
            }

            if (data.isExpired()) {
                return new Result(false).expired();
            }

            if (payload.isDataInvalid()) {
                return new Result(false).dataInvalid();
            }

            if (request.isPublicKeyInvalid()) {
                return new Result(false).publicKeyHashInvalid();
            }

            if (request.isSignatureInvalid()) {
                return new Result(false).signatureInvalid();
            }
            map.put(byteArray, request);
        }
        persist();

        // If we had already the data (only updated seq nr) we return false as well and do not notify listeners.
        // This should only happen if client re-publishes mailbox data 
        if (requestFromMap != null) {
            return new Result(false).payloadAlreadyStored();
        }

        listeners.forEach(listener -> listener.onAdded(payload));
        return new Result(true);
    }

    public Result remove(RemoveMailboxRequest request) {
        ByteArray byteArray = new ByteArray(request.getHash());
        MailboxRequest requestFromMap = map.get(byteArray);
        synchronized (map) {
            if (requestFromMap == null) {
                // We don't have any entry but it might be that we would receive later an add request, so we need to keep
                // track of the sequence number
                map.put(byteArray, request);
                persist();
                return new Result(false).noEntry();
            }

            if (requestFromMap instanceof RemoveMailboxRequest) {
                // We have had the entry already removed.
                if (!request.isSequenceNrInvalid(requestFromMap.getSequenceNumber())) {
                    // We update the request, so we have latest sequence number.
                    map.put(byteArray, request);
                    persist();
                }
                return new Result(false).alreadyRemoved();
            }

            // At that point we know requestFromMap is an AddProtectedDataRequest
            AddMailboxRequest addRequest = (AddMailboxRequest) requestFromMap;
            // We have an entry, lets validate if we can remove it
            MailboxData dataFromMap = addRequest.getMailboxData();
            if (request.isSequenceNrInvalid(dataFromMap.getSequenceNumber())) {
                // Sequence number has not increased
                return new Result(false).sequenceNrInvalid();
            }

            if (request.isPublicKeyHashInvalid(dataFromMap)) {
                // Hash of pubKey of data does not match provided one
                return new Result(false).publicKeyHashInvalid();
            }

            if (request.isSignatureInvalid()) {
                return new Result(false).signatureInvalid();
            }

            map.put(byteArray, request);
            listeners.forEach(listener -> listener.onRemoved(dataFromMap.getMailboxPayload()));
        }

        persist();
        return new Result(true);
    }

    public Inventory getInventoryList(ProtectedDataFilter dataFilter) {
        List<MailboxRequest> inventoryList = getInventoryList(dataFilter.getFilterMap());
        int maxItems = getMaxItems();
        int size = inventoryList.size();
        if (size <= maxItems) {
            return new Inventory(inventoryList, 0);
        }

        List<? extends AuthenticatedDataRequest> result = InventoryUtil.getSubList(inventoryList,
                dataFilter.getOffset(),
                dataFilter.getRange(),
                maxItems);
        int numDropped = size - result.size();
        return new Inventory(result, numDropped);
    }

    @Override
    public void shutdown() {

    }

    public void addListener(Listener listener) {
        listeners.add(listener);
    }

    public void removeListener(Listener listener) {
        listeners.remove(listener);
    }


    ///////////////////////////////////////////////////////////////////////////////////////////////////
    // Private
    ///////////////////////////////////////////////////////////////////////////////////////////////////


    @VisibleForTesting
    int getMaxItems() {
        return maxItems;
    }

    @VisibleForTesting
    Map<ByteArray, MailboxRequest> getMap() {
        return map;
    }

    List<MailboxRequest> getInventoryList(Map<ByteArray, Integer> requesterMap) {
        return new HashSet<>(map.entrySet()).stream()
                .filter(entry -> {
                    // Any entry we have but is not included in filter gets added
                    if (!requesterMap.containsKey(entry.getKey())) {
                        return true;
                    }
                    // If there is a match we add entry if sequence number is higher
                    return entry.getValue().getSequenceNumber() > requesterMap.get(entry.getKey());
                })
                .map(Map.Entry::getValue)
                .collect(Collectors.toList());
    }


    int getSequenceNumber(byte[] hash) {
        ByteArray byteArray = new ByteArray(hash);
        int sequenceNumber = 0;
        synchronized (map) {
            if (map.containsKey(byteArray)) {
                sequenceNumber = map.get(byteArray).getSequenceNumber();
            }
        }
        return sequenceNumber;
    }

    boolean canAddMailboxMessage(MailboxPayload mailboxPayload) {
        byte[] hash = DigestUtil.hash(mailboxPayload.serialize());
        return getSequenceNumber(hash) < Integer.MAX_VALUE;
    }

    // todo call by time interval
    private void maybePruneMap(ConcurrentHashMap<ByteArray, MailboxRequest> current) {
        long now = System.currentTimeMillis();
        // Remove entries older than MAX_AGE
        // Remove expired ProtectedEntry in case value is of type AddProtectedDataRequest
        // Sort by created date
        // Limit to MAX_MAP_SIZE
        Map<ByteArray, MailboxRequest> pruned = current.entrySet().stream()
                .filter(entry -> now - entry.getValue().getCreated() < MAX_AGE)
                .filter(entry -> entry.getValue() instanceof RemoveMailboxRequest ||
                        !((AddMailboxRequest) entry.getValue()).getMailboxData().isExpired())
                .sorted((o1, o2) -> Long.compare(o2.getValue().getCreated(), o1.getValue().getCreated()))
                .limit(MAX_MAP_SIZE)
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
        synchronized (map) {
            map.clear();
            map.putAll(pruned);
        }
    }
}
