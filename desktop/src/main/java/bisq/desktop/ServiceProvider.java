/*
 * This file is part of Bisq.
 *
 * Bisq is free software: you can redistribute it and/or modify it
 * under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, 
either version 3 of the License, 
or (at
 * your option) any later version.
 *
 * Bisq is distributed in the hope that it will be useful, 
but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE. See the GNU Affero General Public
 * License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with Bisq. If not, 
see <http://www.gnu.org/licenses/>.
 */

package bisq.desktop;

import bisq.account.AccountService;
import bisq.application.ApplicationService;
import bisq.bonded_roles.BondedRolesService;
import bisq.chat.ChatService;
import bisq.contract.ContractService;
import bisq.desktop.common.application.ShotDownHandler;
import bisq.identity.IdentityService;
import bisq.network.NetworkService;
import bisq.offer.OfferService;
import bisq.presentation.notifications.NotificationsService;
import bisq.security.SecurityService;
import bisq.settings.SettingsService;
import bisq.support.SupportService;
import bisq.trade.TradeService;
import bisq.update.UpdateService;
import bisq.user.UserService;
import bisq.wallets.core.WalletService;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import java.util.Optional;

@Slf4j
@Getter
public class ServiceProvider {

    private final ApplicationService.Config config;
    private final SecurityService securityService;
    private final Optional<WalletService> walletService;
    private final NetworkService networkService;
    private final IdentityService identityService;
    private final BondedRolesService bondedRolesService;
    private final AccountService accountService;
    private final OfferService offerService;
    private final ContractService contractService;
    private final UserService userService;
    private final ChatService chatService;
    private final SettingsService settingsService;
    private final SupportService supportService;
    private final NotificationsService notificationsService;
    private final TradeService tradeService;
    private final UpdateService updateService;
    private final ShotDownHandler shotDownHandler;

    public ServiceProvider(ShotDownHandler shotDownHandler,
                           ApplicationService.Config config,
                           SecurityService securityService,
                           Optional<WalletService> walletService,
                           NetworkService networkService,
                           IdentityService identityService,
                           BondedRolesService bondedRolesService,
                           AccountService accountService,
                           OfferService offerService,
                           ContractService contractService,
                           UserService userService,
                           ChatService chatService,
                           SettingsService settingsService,
                           SupportService supportService,
                           NotificationsService notificationsService,
                           TradeService tradeService,
                           UpdateService updateService) {
        this.shotDownHandler = shotDownHandler;
        this.config = config;
        this.securityService = securityService;
        this.walletService = walletService;
        this.networkService = networkService;
        this.identityService = identityService;
        this.bondedRolesService = bondedRolesService;
        this.accountService = accountService;
        this.offerService = offerService;
        this.contractService = contractService;
        this.userService = userService;
        this.chatService = chatService;
        this.settingsService = settingsService;
        this.supportService = supportService;
        this.notificationsService = notificationsService;
        this.tradeService = tradeService;
        this.updateService = updateService;
    }
}
