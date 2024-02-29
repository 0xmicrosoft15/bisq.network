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

package bisq.user;

import bisq.bonded_roles.BondedRolesService;
import bisq.common.application.Service;
import bisq.identity.IdentityService;
import bisq.network.NetworkService;
import bisq.persistence.PersistenceService;
import bisq.security.SecurityService;
import bisq.user.banned.BannedUserService;
import bisq.user.identity.UserIdentityService;
import bisq.user.profile.UserProfileService;
import bisq.user.reputation.ReputationService;
import lombok.Getter;
import lombok.ToString;
import lombok.extern.slf4j.Slf4j;

import java.util.concurrent.CompletableFuture;

@Slf4j
@Getter
public class UserService implements Service {
    private final BannedUserService bannedUserService;

    @Getter
    @ToString
    public static final class Config {
        private final UserIdentityService.Config userIdentityConfig;

        public Config(UserIdentityService.Config userIdentityConfig) {
            this.userIdentityConfig = userIdentityConfig;
        }

        public static Config from(com.typesafe.config.Config config) {
            return new Config(UserIdentityService.Config.from(config.getConfig("userIdentity")));
        }
    }

    private final UserProfileService userProfileService;
    private final UserIdentityService userIdentityService;
    private final ReputationService reputationService;

    public UserService(Config config,
                       PersistenceService persistenceService,
                       SecurityService securityService,
                       IdentityService identityService,
                       NetworkService networkService,
                       BondedRolesService bondedRolesService) {

        bannedUserService = new BannedUserService(persistenceService, networkService);

        userProfileService = new UserProfileService(persistenceService, securityService, networkService);

        userIdentityService = new UserIdentityService(config.getUserIdentityConfig(),
                persistenceService,
                securityService,
                identityService,
                networkService);

        reputationService = new ReputationService(persistenceService,
                networkService,
                userIdentityService,
                userProfileService,
                bannedUserService,
                bondedRolesService.getAuthorizedBondedRolesService());
    }


    ///////////////////////////////////////////////////////////////////////////////////////////////////
    // Service
    ///////////////////////////////////////////////////////////////////////////////////////////////////

    public CompletableFuture<Boolean> initialize() {
        log.info("initialize");
        return userProfileService.initialize()
                .thenCompose(result -> userIdentityService.initialize())
                .thenCompose(result -> reputationService.initialize())
                .thenCompose(result -> bannedUserService.initialize());
    }

    public CompletableFuture<Boolean> shutdown() {
        log.info("shutdown");
        return userProfileService.shutdown()
                .thenCompose(result -> userIdentityService.shutdown())
                .thenCompose(result -> reputationService.shutdown())
                .thenCompose(result -> bannedUserService.shutdown());
    }
}