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

package bisq.chat.events.pub;

import bisq.chat.channel.ChannelNotificationType;
import bisq.chat.channel.PublicChannel;
import bisq.i18n.Res;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

@Getter
@ToString(callSuper = true)
@EqualsAndHashCode(callSuper = true, onlyExplicitlyIncluded = true)
public final class PublicEventsChannel extends PublicChannel<PublicEventsChatMessage> {
    private final String channelName;
    private final String description;
    private final String channelAdminId;
    private final Set<String> channelModeratorIds;

    public PublicEventsChannel(String id) {
        this(id,
                Res.get("events." + id + ".name").toUpperCase(),
                Res.get("events." + id + ".description").toUpperCase(),
                "",
                new HashSet<>(),
                ChannelNotificationType.MENTION);
    }

    public PublicEventsChannel(String id,
                               String channelName,
                               String description,
                               String channelAdminId,
                               Set<String> channelModeratorIds) {
        this(id,
                channelName,
                description,
                channelAdminId,
                channelModeratorIds,
                ChannelNotificationType.MENTION);
    }

    private PublicEventsChannel(String id,
                                String channelName,
                                String description,
                                String channelAdminId,
                                Set<String> channelModeratorIds,
                                ChannelNotificationType channelNotificationType) {
        super(id, channelNotificationType);

        this.channelName = channelName;
        this.description = description;
        this.channelAdminId = channelAdminId;
        this.channelModeratorIds = channelModeratorIds;
    }

    public bisq.chat.protobuf.Channel toProto() {
        return getChannelBuilder()
                .setPublicEventsChannel(bisq.chat.protobuf.PublicEventsChannel.newBuilder()
                        .setChannelName(channelName)
                        .setDescription(description)
                        .setChannelAdminId(channelAdminId)
                        .addAllChannelModeratorIds(channelModeratorIds))
                .build();
    }

    public static PublicEventsChannel fromProto(bisq.chat.protobuf.Channel baseProto,
                                                bisq.chat.protobuf.PublicEventsChannel proto) {
        return new PublicEventsChannel(
                baseProto.getId(),
                proto.getChannelName(),
                proto.getDescription(),
                proto.getChannelAdminId(),
                new HashSet<>(proto.getChannelModeratorIdsList()),
                ChannelNotificationType.fromProto(baseProto.getChannelNotificationType()));
    }

    @Override
    public void addChatMessage(PublicEventsChatMessage chatMessage) {
        chatMessages.add(chatMessage);
    }

    @Override
    public void removeChatMessage(PublicEventsChatMessage chatMessage) {
        chatMessages.remove(chatMessage);
    }

    @Override
    public void removeChatMessages(Collection<PublicEventsChatMessage> removeMessages) {
        chatMessages.removeAll(removeMessages);
    }

    @Override
    public String getDisplayString() {
        return Res.get("events." + id + ".name").toUpperCase();
    }
}