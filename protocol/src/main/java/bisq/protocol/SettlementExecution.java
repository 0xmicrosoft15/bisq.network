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

package bisq.protocol;

import bisq.contract.Contract;
import com.google.common.util.concurrent.Uninterruptibles;
import lombok.extern.slf4j.Slf4j;

import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;

@Slf4j
public abstract class SettlementExecution {
   /* public static SettlementExecution from(SettlementMethod.Type type) {
        return switch (type) {
            case AUTOMATIC -> new Automatic();
            case MANUAL -> new Manual();
        };
    }*/

    public abstract CompletableFuture<Boolean> sendFunds(Contract contract);


    public static class Automatic extends SettlementExecution {
        @Override
        public CompletableFuture<Boolean> sendFunds(Contract contract) {
            return CompletableFuture.completedFuture(true);
        }
    }

    public static class Manual extends SettlementExecution {
        public interface Listener {
            void onStartManualPayment();
        }

        private final CountDownLatch latch = new CountDownLatch(1);
        private final Set<Listener> listeners = ConcurrentHashMap.newKeySet();

        public void onManualPaymentStarted() {
            latch.countDown();
        }

        public void addListener(Listener listener) {
            listeners.add(listener);
        }

        @Override
        public CompletableFuture<Boolean> sendFunds(Contract contract) {
            listeners.forEach(Listener::onStartManualPayment);
            return CompletableFuture.supplyAsync(() -> {
                Uninterruptibles.awaitUninterruptibly(latch);
                return true;
            });
        }
    }
}
