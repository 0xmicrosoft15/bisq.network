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

package bisq.tor.controller;

import bisq.common.threading.ExecutorFactory;
import bisq.security.keys.TorKeyPair;
import bisq.tor.controller.events.events.HsDescEvent;
import bisq.tor.controller.events.listener.FilteredHsDescEventListener;
import bisq.tor.controller.exceptions.HsDescUploadFailedException;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

@Slf4j
public class PublishOnionAddressService extends FilteredHsDescEventListener {
    private final TorControlProtocol torControlProtocol;
    private final int timeout;
    private final TorKeyPair torKeyPair;
    private final CountDownLatch countDownLatch = new CountDownLatch(1);
    @Getter
    private Optional<CompletableFuture<Void>> future = Optional.empty();

    public PublishOnionAddressService(TorControlProtocol torControlProtocol, int timeout, TorKeyPair torKeyPair) {
        super(torKeyPair.getOnionAddress(), Set.of(HsDescEvent.Action.UPLOAD));

        this.torControlProtocol = torControlProtocol;
        this.timeout = timeout;
        this.torKeyPair = torKeyPair;
    }

    public CompletableFuture<Void> publish(int onionServicePort, int localPort) throws InterruptedException {
        future = Optional.of(CompletableFuture.runAsync(() -> {
                    try {
                        torControlProtocol.addHsDescEventListener(this);
                        torControlProtocol.setEvents(List.of("HS_DESC"));

                        torControlProtocol.addOnion(torKeyPair, onionServicePort, localPort);

                        boolean success = countDownLatch.await(timeout, TimeUnit.MILLISECONDS);
                        if (!success) {
                            throw new HsDescUploadFailedException("Could not get onion address upload completed in " + timeout / 1000 + " seconds");
                        }
                    } catch (Exception e) {
                        throw new HsDescUploadFailedException(e);
                    }
                }, ExecutorFactory.newSingleThreadExecutor("PublishOnionAddressService"))
                .whenComplete((nil, throwable) -> {
                    torControlProtocol.removeHsDescEventListener(this);
                    torControlProtocol.setEvents(Collections.emptyList());
                }));
        return future.get();
    }

    @Override
    public void onFilteredEvent(HsDescEvent hsDescEvent) {
        log.info("Publishing of onion address completed. Received UPLOADED event {}", hsDescEvent);
        countDownLatch.countDown();
    }
}
