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

package bisq.java_se.facades;

import bisq.common.facades.JdkFacade;

import java.lang.management.ManagementFactory;
import java.util.stream.Stream;

public class JavaSeJdkFacade implements JdkFacade {
    @Override
    public String getMyPid() {
        String processName = ManagementFactory.getRuntimeMXBean().getName();
        return processName.split("@")[0];
    }

    @Override
    public Stream<String> getProcessCommandLineStream() {
        return ProcessHandle.allProcesses().map(processHandle -> processHandle.info().commandLine().orElse(""));
    }

    @Override
    public void redirectError(ProcessBuilder processBuilder) {
        processBuilder.redirectError(ProcessBuilder.Redirect.DISCARD);
    }

    @Override
    public void redirectOutput(ProcessBuilder processBuilder) {
        processBuilder.redirectOutput(ProcessBuilder.Redirect.DISCARD);
    }
}
