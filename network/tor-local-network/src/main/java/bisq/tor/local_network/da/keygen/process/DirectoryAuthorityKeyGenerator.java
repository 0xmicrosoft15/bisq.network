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

package bisq.tor.local_network.da.keygen.process;

import bisq.tor.local_network.da.DirectoryAuthority;
import bisq.tor.local_network.da.keygen.RelayKeyGenProcess;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.util.Optional;

@Slf4j
public class DirectoryAuthorityKeyGenerator {
    @Getter
    private Optional<String> relayKeyFingerprint = Optional.empty();

    public void generate(DirectoryAuthority directoryAuthority, String passphrase) throws IOException, InterruptedException {
        var identityKeyGenProcess = new DirectoryIdentityKeyGenProcess(
                directoryAuthority.getKeysPath(),
                "127.0.0.1:" + directoryAuthority.getDirPort()
        );
        identityKeyGenProcess.generateKeys(passphrase);

        String identityKeyFingerprint = directoryAuthority.getIdentityKeyFingerprint().orElseThrow();

        var relayKeyGenProcess = new RelayKeyGenProcess(directoryAuthority);
        String relayKeyFingerprint = relayKeyGenProcess.generateKeys(identityKeyFingerprint);

        this.relayKeyFingerprint = Optional.of(relayKeyFingerprint);
    }
}
