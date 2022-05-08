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

package bisq.wallets.rpc;

import bisq.wallets.exceptions.RpcCallFailureException;
import bisq.wallets.rpc.call.DaemonRpcCall;
import bisq.wallets.rpc.call.RpcCall;
import com.googlecode.jsonrpc4j.JsonRpcHttpClient;

public class DaemonRpcClient extends AbstractRpcClient {

    private final JsonRpcHttpClient daemonJsonRpcClient;

    DaemonRpcClient(JsonRpcHttpClient daemonJsonRpcClient) {
        this.daemonJsonRpcClient = daemonJsonRpcClient;
    }

    public <T, R> R invokeAndValidate(DaemonRpcCall<T, R> rpcCall) {
        R response = invoke(rpcCall);
        validateRpcCall(rpcCall, response);
        return response;
    }

    private <T, R> R invoke(DaemonRpcCall<T, R> rpcCall) {
        return invokeAndHandleExceptions(daemonJsonRpcClient, rpcCall);
    }

    private <T, R> void validateRpcCall(RpcCall<T, R> rpcCall, R response) {
        boolean isValid = rpcCall.isResponseValid(response);
        if (!isValid) {
            throw new RpcCallFailureException("RPC Call to '" + rpcCall.getRpcMethodName() + "' failed. " +
                    response.toString());
        }
    }
}
