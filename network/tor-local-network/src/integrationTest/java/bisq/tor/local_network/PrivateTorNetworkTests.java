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

package bisq.tor.local_network;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Path;

public class PrivateTorNetworkTests {
    private static final String PASSPHRASE = "my_passphrase";

    @Test
    public void createTestNetwork(@TempDir Path tempDir) throws IOException, InterruptedException {
        new TorNetwork(tempDir)
                .addDirAuth(PASSPHRASE)
                .addDirAuth(PASSPHRASE)
                .addDirAuth(PASSPHRASE)

                .addRelay()
                .addRelay()
                .addRelay()
                .addRelay()
                .addRelay()

                .addClient()
                .addClient()

                .start();
    }
}
