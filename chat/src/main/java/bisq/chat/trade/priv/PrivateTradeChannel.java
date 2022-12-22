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

package bisq.chat.trade.priv;

import bisq.chat.channel.ChannelNotificationType;
import bisq.chat.channel.PrivateChannel;
import bisq.common.observable.Observable;
import bisq.i18n.Res;
import bisq.user.identity.UserIdentity;
import bisq.user.profile.UserProfile;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@ToString(callSuper = true)
@Getter
@EqualsAndHashCode(callSuper = true, onlyExplicitlyIncluded = true)
public final class PrivateTradeChannel extends PrivateChannel<PrivateTradeChatMessage> {
    private final UserProfile trader1;
    private final UserProfile trader2;
    private final UserIdentity myUserIdentity;
    private final Optional<UserProfile> mediator;
    private final Observable<Boolean> inMediation = new Observable<>(false);

    public PrivateTradeChannel(UserIdentity myUserIdentity,
                               UserProfile trader1,
                               UserProfile trader2,
                               Optional<UserProfile> mediator) {
        super(PrivateChannel.createChannelId(trader1.getId(), trader2.getId()),
                trader1,
                myUserIdentity,
                new ArrayList<>(),
                ChannelNotificationType.ALL);
        this.trader1 = trader1;
        this.trader2 = trader2;
        this.mediator = mediator;
        this.myUserIdentity = myUserIdentity;
    }

    private PrivateTradeChannel(String id,
                                UserProfile trader1,
                                UserProfile trader2,
                                UserIdentity myUserIdentity,
                                Optional<UserProfile> mediator,
                                List<PrivateTradeChatMessage> chatMessages,
                                ChannelNotificationType channelNotificationType) {
        super(id, trader1, myUserIdentity, chatMessages, channelNotificationType);

        this.trader1 = trader1;
        this.trader2 = trader2;
        this.myUserIdentity = myUserIdentity;
        this.mediator = mediator;
    }

    @Override
    public bisq.chat.protobuf.Channel toProto() {
        bisq.chat.protobuf.PrivateTradeChannel.Builder builder = bisq.chat.protobuf.PrivateTradeChannel.newBuilder()
                .setTrader1(trader1.toProto())
                .setTrader2(trader2.toProto())
                .setMyUserIdentity(this.myUserIdentity.toProto())
                .addAllChatMessages(chatMessages.stream()
                        .map(PrivateTradeChatMessage::toChatMessageProto)
                        .collect(Collectors.toList()))
                .setInMediation(inMediation.get());
        mediator.ifPresent(mediator -> builder.setMediator(mediator.toProto()));
        return getChannelBuilder().setPrivateTradeChannel(builder).build();
    }

    public static PrivateTradeChannel fromProto(bisq.chat.protobuf.Channel baseProto,
                                                bisq.chat.protobuf.PrivateTradeChannel proto) {
        PrivateTradeChannel privateTradeChannel = new PrivateTradeChannel(
                baseProto.getId(),
                UserProfile.fromProto(proto.getTrader1()),
                UserProfile.fromProto(proto.getTrader2()),
                UserIdentity.fromProto(proto.getMyUserIdentity()),
                proto.hasMediator() ? Optional.of(UserProfile.fromProto(proto.getMediator())) : Optional.empty(),
                proto.getChatMessagesList().stream()
                        .map(PrivateTradeChatMessage::fromProto)
                        .collect(Collectors.toList()),
                ChannelNotificationType.fromProto(baseProto.getChannelNotificationType()));
        privateTradeChannel.getInMediation().set(proto.getInMediation());
        return privateTradeChannel;
    }

    @Override
    public void addChatMessage(PrivateTradeChatMessage chatMessage) {
        chatMessages.add(chatMessage);
    }

    @Override
    public void removeChatMessage(PrivateTradeChatMessage chatMessage) {
        chatMessages.remove(chatMessage);
    }

    @Override
    public void removeChatMessages(Collection<PrivateTradeChatMessage> removeMessages) {
        chatMessages.removeAll(removeMessages);
    }

    public boolean isMediator() {
        return mediator.filter(mediator -> mediator.getId().equals(this.myUserIdentity.getId())).isPresent();
    }

    @Override
    public String getDisplayString() {
        String mediatorLabel = "";
        if (mediator.isPresent() && inMediation.get()) {
            mediatorLabel = " (" + Res.get("mediator") + ": " + mediator.get().getUserName() + ")";
        }
        if (isMediator()) {
            return trader1.getUserName() + " - " + trader2.getUserName() + mediatorLabel;
        } else {
            return peer.getUserName() + " - " + myUserIdentity.getUserName() + mediatorLabel;
        }
    }

    public String getChannelSelectionDisplayString() {
        if (isMediator()) {
            return trader1.getUserName() + ", " + trader2.getUserName();
        } else if (mediator.isPresent() && inMediation.get()) {
            return peer.getUserName() + ", " + mediator.get().getUserName();
        } else {
            return peer.getUserName();
        }
    }
}