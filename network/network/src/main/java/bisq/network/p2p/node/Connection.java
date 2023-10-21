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

import bisq.common.util.StringUtils;
import bisq.network.NetworkService;
import bisq.network.p2p.message.NetworkEnvelope;
import bisq.network.p2p.message.NetworkMessage;
import bisq.network.p2p.node.authorization.AuthorizationToken;
import bisq.network.p2p.node.data.ConnectionMetrics;
import bisq.network.p2p.node.data.NetworkLoad;
import bisq.network.p2p.node.envelope.NetworkEnvelopeSocket;
import bisq.network.p2p.vo.Address;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import javax.annotation.Nullable;
import java.io.EOFException;
import java.io.IOException;
import java.net.Socket;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.BiConsumer;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Represents an inbound or outbound connection to a peer node.
 * Listens for messages from the peer.
 * Send messages to the peer.
 * Notifies messageListeners on new received messages.
 * Notifies errorHandler on exceptions from the inputHandlerService executor.
 */
@Slf4j
public abstract class Connection {
    protected interface Handler {
        void handleNetworkMessage(NetworkMessage networkMessage, AuthorizationToken authorizationToken, Connection connection);

        void handleConnectionClosed(Connection connection, CloseReason closeReason);
    }

    public interface Listener {
        void onNetworkMessage(NetworkMessage networkMessage);

        void onConnectionClosed(CloseReason closeReason);
    }

    @Getter
    protected final String id = StringUtils.createUid();
    @Getter
    private final Capability peersCapability;
    @Getter
    private final NetworkLoad peersNetworkLoad;
    @Getter
    private final ConnectionMetrics connectionMetrics;

    private NetworkEnvelopeSocket networkEnvelopeSocket;

    private final Handler handler;
    private final Set<Listener> listeners = new CopyOnWriteArraySet<>();
    @Nullable
    private Future<?> future;

    @Getter
    private volatile boolean isStopped;
    private volatile boolean listeningStopped;
    @Getter
    private final AtomicInteger sentMessageCounter = new AtomicInteger(0);
    private final Object writeLock = new Object();

    protected Connection(Socket socket,
                         Capability peersCapability,
                         NetworkLoad peersNetworkLoad,
                         ConnectionMetrics connectionMetrics,
                         Handler handler,
                         BiConsumer<Connection, Exception> errorHandler) {
        this.peersCapability = peersCapability;
        this.peersNetworkLoad = peersNetworkLoad;
        this.handler = handler;
        this.connectionMetrics = connectionMetrics;

        try {
            this.networkEnvelopeSocket = new NetworkEnvelopeSocket(socket);
        } catch (IOException exception) {
            log.error("Could not create objectOutputStream/objectInputStream for socket " + socket, exception);
            errorHandler.accept(this, exception);
            close(CloseReason.EXCEPTION.exception(exception));
            return;
        }

        future = NetworkService.NETWORK_IO_POOL.submit(() -> {
            Thread.currentThread().setName("Connection.read-" + getThreadNameId());
            try {
                while (isInputStreamActive()) {
                    var proto = networkEnvelopeSocket.receiveNextEnvelope();
                    // parsing might need some time wo we check again if connection is still active
                    if (isInputStreamActive()) {
                        checkNotNull(proto, "Proto from NetworkEnvelope.parseDelimitedFrom(inputStream) must not be null");
                        NetworkEnvelope networkEnvelope = NetworkEnvelope.fromProto(proto);
                        if (networkEnvelope.getVersion() != NetworkEnvelope.VERSION) {
                            throw new ConnectionException("Invalid network version. " +
                                    networkEnvelope.getClass().getSimpleName());
                        }
                        NetworkMessage networkMessage = networkEnvelope.getNetworkMessage();
                        log.debug("Received message: {} at: {}",
                                StringUtils.truncate(networkMessage.toString(), 200), this);
                        connectionMetrics.onReceived(networkEnvelope);
                        NetworkService.DISPATCHER.submit(() -> handler.handleNetworkMessage(networkMessage,
                                networkEnvelope.getAuthorizationToken(),
                                this));
                    }
                }
            } catch (Exception exception) {
                //todo StreamCorruptedException from i2p at shutdown. prob it send some text data at shut down
                if (isInputStreamActive()) {
                    log.debug("Call shutdown from startListen read handler {} due exception={}", this, exception.toString());
                    close(CloseReason.EXCEPTION.exception(exception));
                    // EOFException expected if connection got closed
                    if (!(exception instanceof EOFException)) {
                        errorHandler.accept(this, exception);
                    }
                }
            }
        });
    }

    Connection send(NetworkMessage networkMessage, AuthorizationToken authorizationToken) {
        if (isStopped) {
            log.warn("Message not sent as connection has been shut down already. Message={}, Connection={}",
                    StringUtils.truncate(networkMessage.toString(), 200), this);
            throw new ConnectionClosedException(this);
        }
        try {
            NetworkEnvelope networkEnvelope = new NetworkEnvelope(NetworkEnvelope.VERSION, authorizationToken, networkMessage);
            boolean sent = false;
            synchronized (writeLock) {
                try {
                    networkEnvelopeSocket.send(networkEnvelope);
                    sent = true;
                } catch (Throwable throwable) {
                    if (!isStopped) {
                        throw throwable;
                    }
                }
            }
            if (sent) {
                connectionMetrics.onSent(networkEnvelope);
                if (networkMessage instanceof CloseConnectionMessage) {
                    log.info("Sent {} from {}",
                            StringUtils.truncate(networkMessage.toString(), 300), this);
                } else {
                    log.debug("Sent {} from {}",
                            StringUtils.truncate(networkMessage.toString(), 300), this);
                }
            }
            return this;
        } catch (IOException exception) {
            if (!isStopped) {
                log.error("Call shutdown from send {} due exception={}", this, exception.toString());
                close(CloseReason.EXCEPTION.exception(exception));
            }
            // We wrap any exception (also expected EOFException in case of connection close), to inform the caller 
            // that the "send proto" intent failed.
            throw new ConnectionException(exception);
        }
    }

    void stopListening() {
        listeningStopped = true;
    }

    void close(CloseReason closeReason) {
        if (isStopped) {
            log.debug("Shut down already in progress {}", this);
            return;
        }
        log.info("Close {}", this);
        isStopped = true;
        if (future != null) {
            future.cancel(true);
        }
        try {
            networkEnvelopeSocket.close();
        } catch (IOException ignore) {
        }
        NetworkService.DISPATCHER.submit(() -> {
            handler.handleConnectionClosed(this, closeReason);
            listeners.forEach(listener -> listener.onConnectionClosed(closeReason));
            listeners.clear();
        });
    }

    void notifyListeners(NetworkMessage networkMessage) {
        listeners.forEach(listener -> listener.onNetworkMessage(networkMessage));
    }

    public void addListener(Listener listener) {
        listeners.add(listener);
    }

    public void removeListener(Listener listener) {
        listeners.remove(listener);
    }

    public Address getPeerAddress() {
        return peersCapability.getAddress();
    }

    // Only at outbound connections we can be sure that the peer address is correct.
    // The announced peer address in capability is not guaranteed to be valid.
    // For most cases that is sufficient as the peer would not gain anything if lying about their address
    // as it would make them unreachable for receiving messages from newly established connections. But there are
    // cases where we need to be sure that it is the real address, like if we might use the peer address for banning a
    // not correctly behaving peer.
    public boolean getPeerAddressVerified() {
        return isOutboundConnection();
    }

    public boolean isOutboundConnection() {
        return this instanceof OutboundConnection;
    }

    public boolean isRunning() {
        return !isStopped();
    }

    @Override
    public String toString() {
        return "'" + getClass().getSimpleName() + " [peerAddress=" + getPeersCapability().getAddress() +
                ", socket=" + networkEnvelopeSocket +
                ", keyId=" + getId() + "]'";
    }

    private String getThreadNameId() {
        return StringUtils.truncate(getPeersCapability().getAddress().toString() + "-" + id.substring(0, 8));
    }

    private boolean isInputStreamActive() {
        return !listeningStopped && !isStopped && !Thread.currentThread().isInterrupted();
    }

    public abstract boolean isPeerAddressVerified();
}
