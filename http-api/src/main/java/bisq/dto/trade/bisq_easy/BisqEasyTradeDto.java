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

package bisq.dto.trade.bisq_easy;

import bisq.dto.contract.bisq_easy.BisqEasyContractDto;
import bisq.dto.identity.IdentityDto;
import bisq.dto.trade.TradeRoleDto;

public record BisqEasyTradeDto(
        BisqEasyContractDto contract,
        String id,
        TradeRoleDto tradeRole,
        IdentityDto myIdentity,
        BisqEasyTradePartyDto taker,
        BisqEasyTradePartyDto maker
) {
}