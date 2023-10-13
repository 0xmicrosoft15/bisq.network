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

package bisq.desktop.main.notification;

import bisq.bisq_easy.BisqEasyService;
import bisq.desktop.ServiceProvider;
import bisq.desktop.common.threading.UIThread;
import bisq.desktop.common.view.Controller;
import bisq.desktop.common.view.Navigation;
import bisq.desktop.common.view.NavigationTarget;
import bisq.i18n.Res;
import bisq.presentation.notifications.NotificationsService;
import com.google.common.base.Joiner;
import javafx.beans.property.ReadOnlyBooleanProperty;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import java.util.Set;

@Slf4j
public class NotificationPanelController implements Controller {
    @Getter
    private final NotificationPanelView view;
    private final NotificationPanelModel model;
    private final NotificationsService notificationsService;
    private final BisqEasyService bisqEasyService;
    private boolean notificationSubscriptionDone;

    public NotificationPanelController(ServiceProvider serviceProvider) {
        notificationsService = serviceProvider.getNotificationsService();
        bisqEasyService = serviceProvider.getBisqEasyService();

        model = new NotificationPanelModel();
        view = new NotificationPanelView(model, this);
    }

    public ReadOnlyBooleanProperty getIsNotificationVisible() {
        return model.getIsNotificationVisible();
    }

    public void setNavigationTarget(NavigationTarget navigationTarget) {
        // We subscribe once we get the content target
        if (!notificationSubscriptionDone && navigationTarget == NavigationTarget.CONTENT) {
            notificationSubscriptionDone = true;
            notificationsService.subscribe(e -> {
                UIThread.run(() -> {
                    if (Navigation.getCurrentNavigationTarget().get() != NavigationTarget.BISQ_EASY_OPEN_TRADES) {
                        Set<String> tradeIdsOfNotifications = bisqEasyService.getTradeIdsOfNotifications();
                        if (tradeIdsOfNotifications.size() == 1) {
                            String tradeId = tradeIdsOfNotifications.iterator().next();
                            model.getHeadline().set(Res.get("notificationPanel.headline.single", tradeId));
                            model.getContent().set(Res.get("notificationPanel.content.single", tradeId,
                                    Res.get("notificationPanel.content.part2")));
                        } else if (tradeIdsOfNotifications.size() > 1) {
                            String tradeIds = Joiner.on(", ").join(tradeIdsOfNotifications);
                            model.getHeadline().set(Res.get("notificationPanel.headline.multiple", tradeIds));
                            model.getContent().set(Res.get("notificationPanel.content.multiple", tradeIds,
                                    Res.get("notificationPanel.content.part2")));
                        }
                        model.getIsNotificationVisible().set(!tradeIdsOfNotifications.isEmpty());
                    }
                });
            });
        }
        if (navigationTarget == NavigationTarget.BISQ_EASY_OPEN_TRADES) {
            model.getIsNotificationVisible().set(false);
        }
    }

    @Override
    public void onActivate() {
    }

    @Override
    public void onDeactivate() {
    }

    void onClose() {
        model.getIsNotificationVisible().set(false);
    }

    void onGoToOpenTrades() {
        model.getIsNotificationVisible().set(false);
        Navigation.navigateTo(NavigationTarget.BISQ_EASY_OPEN_TRADES);
    }
}
