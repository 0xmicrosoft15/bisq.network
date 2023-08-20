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

package bisq.network.p2p;

import bisq.common.util.OsUtils;
import bisq.network.p2p.node.Node;
import bisq.network.p2p.node.authorization.AuthorizationService;
import bisq.network.p2p.node.transport.Transport;
import bisq.security.pow.EquihashProofOfWorkService;

import java.util.Set;
import java.util.concurrent.TimeUnit;

public abstract class BaseNetworkTest {
    protected Node.Config getConfig(Transport.Type transportType) {
        return getConfig(transportType, Set.of(transportType));
    }

    protected Node.Config getConfig(Transport.Type transportType, Set<Transport.Type> supportedTransportTypes) {
        return new Node.Config(transportType,
                supportedTransportTypes,
                new AuthorizationService(new EquihashProofOfWorkService()),
                getTransportConfig(getBaseDirName()),
                (int) TimeUnit.SECONDS.toMillis(120));
    }


    protected Transport.Config getTransportConfig(String baseDirName) {
        return new Transport.Config() {
            @Override
            public String getBaseDir() {
                return baseDirName;
            }

            @Override
            public int getSocketTimeout() {
                return 600;
            }
        };
    }

    protected String getBaseDirName() {
        return OsUtils.getUserDataDir().toAbsolutePath() + "/Bisq2_" + getClassName();
    }

    protected abstract long getTimeout();

    protected String getClassName() {
        return this.getClass().getSimpleName();
    }
}