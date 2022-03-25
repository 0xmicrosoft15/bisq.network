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

package bisq.network.p2p.services.data.storage.auth;

import bisq.network.p2p.services.data.storage.DistributedData;
import bisq.network.p2p.services.data.storage.MetaData;
import bisq.network.p2p.services.data.storage.auth.authorized.AuthorizedData;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import java.security.PublicKey;
import java.util.Set;
import java.util.concurrent.TimeUnit;

@Slf4j
@EqualsAndHashCode(callSuper = true)
@Getter
public final class MockAuthorizedData extends AuthorizedData {
    private final MetaData metaData;

    public MockAuthorizedData(DistributedData distributedData, byte[] signature, PublicKey publicKey) {
        super(distributedData, signature, publicKey);

        this.metaData = new MetaData(TimeUnit.DAYS.toMillis(10), 10000, this.getClass().getSimpleName());
    }

    @Override
    public MetaData getMetaData() {
        return metaData;
    }

    @Override
    public Set<String> getAuthorizedPublicKeys() {
        return Set.of("3056301006072a8648ce3d020106052b8104000a03420004170a828efbaa0316b7a59ec5a1e8033ca4c215b5e58b17b16f3e3cbfa5ec085f4bdb660c7b766ec5ba92b432265ba3ed3689c5d87118fbebe19e92b9228aca63");
    }
}
