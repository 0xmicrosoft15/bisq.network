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

package bisq.network.p2p.services.peergroup.exchange;

import bisq.network.p2p.node.Address;
import bisq.network.p2p.services.peergroup.Peer;
import bisq.network.p2p.services.peergroup.PeerGroup;
import bisq.network.p2p.services.peergroup.PeerGroupStore;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import java.util.*;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Slf4j
public class PeerExchangeStrategy {
    private static final long REPORTED_PEERS_LIMIT = 200;
    //todo for dev testing we set it short
    private static final long MAX_AGE = TimeUnit.HOURS.toMillis(1);

    @Getter
    public static class Config {
        private final int numSeeNodesAtBoostrap;
        private final int numPersistedPeersAtBoostrap;
        private final int numReportedPeersAtBoostrap;

        public Config() {
            this(2, 40, 20);
        }

        public Config(int numSeeNodesAtBoostrap,
                      int numPersistedPeersAtBoostrap,
                      int numReportedPeersAtBoostrap) {
            this.numSeeNodesAtBoostrap = numSeeNodesAtBoostrap;
            this.numPersistedPeersAtBoostrap = numPersistedPeersAtBoostrap;
            this.numReportedPeersAtBoostrap = numReportedPeersAtBoostrap;
        }

        public static Config from(com.typesafe.config.Config typesafeConfig) {
            return new PeerExchangeStrategy.Config(
                    typesafeConfig.getInt("numSeedNodesAtBoostrap"),
                    typesafeConfig.getInt("numPersistedPeersAtBoostrap"),
                    typesafeConfig.getInt("numReportedPeersAtBoostrap"));
        }
    }

    private final PeerGroup peerGroup;
    private final Config config;
    private final PeerGroupStore peerGroupStore;
    private final Set<Address> usedAddresses = new CopyOnWriteArraySet<>();

    public PeerExchangeStrategy(PeerGroup peerGroup, Config config, PeerGroupStore peerGroupStore) {
        this.peerGroup = peerGroup;
        this.config = config;
        this.peerGroupStore = peerGroupStore;
    }

    Set<Address> getAddressesForInitialPeerExchange() {
        Set<Address> candidates = getCandidates(getPriorityListForInitialPeerExchange());
        if (candidates.isEmpty()) {
            // It can be that we don't have peers anymore which we have not already connected in the past.
            // We reset the usedAddresses and try again. It is likely that some peers have different peers to 
            // send now.
            log.debug("We reset the usedAddresses and try again to connect to peers we tried in the past.");
            usedAddresses.clear();
            candidates = getCandidates(getPriorityListForInitialPeerExchange());
        }
        usedAddresses.addAll(candidates);
        return candidates;
    }

    // After bootstrap, we might want to add more connections and use the peer exchange protocol for that.
    // We do not want to use seed nodes or already existing connections in that case.
    Set<Address> getAddressesForFurtherPeerExchange() {
        Set<Address> candidates = getCandidates(getPriorityListForFurtherPeerExchange());
        if (candidates.isEmpty()) {
            // It can be that we don't have peers anymore which we have not already connected in the past.
            // We reset the usedAddresses and try again. It is likely that some peers have different peers to 
            // send now.
            log.debug("We reset the usedAddresses and try again to connect to peers we tried in the past.");
            usedAddresses.clear();
            candidates = getCandidates(getPriorityListForFurtherPeerExchange());
        }
        usedAddresses.addAll(candidates);
        return candidates;
    }


    Set<Peer> getPeers(Address peerAddress) {
        return Stream.concat(peerGroup.getAllConnectedPeers(), peerGroup.getReportedPeers().stream())
                .sorted(Comparator.comparing(Peer::getDate))
                .filter(peer -> isValid(peerAddress, peer))
                .limit(REPORTED_PEERS_LIMIT)
                .collect(Collectors.toSet());
    }

    void addReportedPeers(Set<Peer> peers, Address peerAddress) {
        Set<Peer> filtered = peers.stream()
                .filter(peer -> isValid(peerAddress, peer))
                .limit(REPORTED_PEERS_LIMIT)
                .collect(Collectors.toSet());
        peerGroup.addReportedPeers(filtered);
    }

    boolean shouldRedoInitialPeerExchange(int numSuccess, int numRequests) {
        boolean moreThenHalfFailed = numRequests - numSuccess > numRequests / 2;
        return moreThenHalfFailed ||
                !sufficientConnections() ||
                !sufficientReportedPeers();
    }

    void shutdown() {
        usedAddresses.clear();
    }

    private boolean sufficientConnections() {
        return peerGroup.getAllConnectedPeers().count() >= peerGroup.getMinNumConnectedPeers();
    }

    private boolean sufficientReportedPeers() {
        return peerGroup.getReportedPeers().size() >= peerGroup.getMinNumReportedPeers();
    }

    private boolean notASeed(Address address) {
        return !peerGroup.isASeed(address);
    }

    private boolean notASeed(Peer peer) {
        return notASeed(peer.getAddress());
    }

    private boolean isDateValid(Peer peer) {
        return peer.getAge() < MAX_AGE;
    }

    private boolean isNotUsed(Address address) {
        return !usedAddresses.contains(address);
    }

    private boolean notTargetPeer(Address peerAddress, Peer peer) {
        return !peer.getAddress().equals(peerAddress);
    }

    private boolean isValid(Address peerAddress, Peer peer) {
        return notTargetPeer(peerAddress, peer) &&
                peerGroup.notMyself(peer) &&
                notASeed(peer) &&
                isDateValid(peer);
    }

    private Set<Address> getPriorityListForInitialPeerExchange() {
        Set<Address> seeds = getSeeds();
        Set<Address> priorityList = new HashSet<>(seeds);
        Set<Address> reported = getReported();
        priorityList.addAll(reported);
        priorityList.addAll(getPersisted());
        Set<Address> connected = getConnected();
        priorityList.addAll(connected);

        // log.error("seeds {}", seeds);
        // log.error("reported {}", reported);
        // log.error("connected {}", connected);
        return priorityList;
    }

    private Set<Address> getPriorityListForFurtherPeerExchange() {
        Set<Address> priorityList = new HashSet<>(getReported());
        priorityList.addAll(getPersisted());
        return priorityList;
    }

    private Set<Address> getSeeds() {
        return getShuffled(peerGroup.getSeedNodeAddresses()).stream()
                .filter(peerGroup::notMyself)
                .filter(this::isNotUsed)
                .limit(config.getNumSeeNodesAtBoostrap())
                .collect(Collectors.toSet());
    }

    private Set<Address> getReported() {
        return peerGroup.getReportedPeers().stream()
                .filter(peerGroup::isNotInQuarantine)
                .sorted(Comparator.comparing(peer -> peer.getLoad().getNumConnections()))
                .sorted(Comparator.comparing(Peer::getDate))
                .map(Peer::getAddress)
                .filter(this::isNotUsed)
                .limit(config.getNumReportedPeersAtBoostrap())
                .collect(Collectors.toSet());
    }

    private Set<Address> getPersisted() {
        return peerGroupStore.getPersistedPeers().stream()
                .filter(peerGroup::isNotInQuarantine)
                .sorted(Comparator.comparing(Peer::getDate))
                .map(Peer::getAddress)
                .filter(this::isNotUsed)
                .limit(config.getNumReportedPeersAtBoostrap())
                .collect(Collectors.toSet());
    }

    private Set<Address> getConnected() {
        return peerGroup.getAllConnectedPeers()
                .filter(peerGroup::isNotInQuarantine)
                .sorted(Comparator.comparing(peer -> peer.getLoad().getNumConnections()))
                .sorted(Comparator.comparing(Peer::getDate))
                .map(Peer::getAddress)
                .filter(this::notASeed)
                .filter(this::isNotUsed)
                .collect(Collectors.toSet());
    }

    private Set<Address> getCandidates(Set<Address> priorityList) {
        return priorityList.stream()
                .limit(getLimit())
                .collect(Collectors.toSet());
    }

    private int getLimit() {
        int minNumConnectedPeers = peerGroup.getMinNumConnectedPeers();
        // We want at least 25% of minNumConnectedPeers
        int minValue = minNumConnectedPeers / 4;
        int limit = Math.max(minValue, peerGroup.getTargetNumConnectedPeers() - peerGroup.getNumConnections());

        // In case we have enough connections but do not have received at least 25% of our numReportedPeersAtBoostrap 
        // target we still try to connect to 50% of minNumConnectedPeers.
        if (limit == minValue && peerGroup.getReportedPeers().size() < config.getNumReportedPeersAtBoostrap() / 4) {
            return minNumConnectedPeers / 2;
        }
        return limit;
    }

    private List<Address> getShuffled(Collection<Address> addresses) {
        List<Address> list = new ArrayList<>(addresses);
        Collections.shuffle(list);
        return list;
    }
}
