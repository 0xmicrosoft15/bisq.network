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

package bisq.chat.channel.pub;

import bisq.chat.channel.ChatChannelDomain;
import bisq.chat.message.Citation;
import bisq.chat.message.CommonPublicChatMessage;
import bisq.common.observable.collection.ObservableArray;
import bisq.common.util.StringUtils;
import bisq.i18n.Res;
import bisq.network.NetworkService;
import bisq.network.p2p.services.data.storage.DistributedData;
import bisq.network.p2p.services.data.storage.auth.AuthenticatedData;
import bisq.persistence.Persistence;
import bisq.persistence.PersistenceService;
import bisq.user.identity.UserIdentityService;
import bisq.user.profile.UserProfile;
import bisq.user.profile.UserProfileService;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import java.util.Date;
import java.util.List;
import java.util.Optional;

@Slf4j
public final class CommonPublicChatChannelService extends PublicChatChannelService<CommonPublicChatMessage, CommonPublicChatChannel, CommonPublicChatChannelStore> {
    @Getter
    private final CommonPublicChatChannelStore persistableStore = new CommonPublicChatChannelStore();
    @Getter
    private final Persistence<CommonPublicChatChannelStore> persistence;
    private final List<CommonPublicChatChannel> defaultChannels;

    public CommonPublicChatChannelService(PersistenceService persistenceService,
                                          NetworkService networkService,
                                          UserIdentityService userIdentityService,
                                          UserProfileService userProfileService,
                                          ChatChannelDomain chatChannelDomain,
                                          List<CommonPublicChatChannel> defaultChannels) {
        super(networkService, userIdentityService, userProfileService, chatChannelDomain);

        this.defaultChannels = defaultChannels;

        persistence = persistenceService.getOrCreatePersistence(this,
                "db",
                "Public" + StringUtils.capitalize(chatChannelDomain.name()) + "ChatChannelStore",
                persistableStore);
    }


    ///////////////////////////////////////////////////////////////////////////////////////////////////
    // DataService.Listener
    ///////////////////////////////////////////////////////////////////////////////////////////////////

    @Override
    public void onAuthenticatedDataAdded(AuthenticatedData authenticatedData) {
        DistributedData distributedData = authenticatedData.getDistributedData();
        if (distributedData instanceof CommonPublicChatMessage) {
            processAddedMessage((CommonPublicChatMessage) distributedData);
        }
    }

    @Override
    public void onAuthenticatedDataRemoved(AuthenticatedData authenticatedData) {
        DistributedData distributedData = authenticatedData.getDistributedData();
        if (distributedData instanceof CommonPublicChatMessage) {
            processRemovedMessage((CommonPublicChatMessage) distributedData);
        }
    }


    ///////////////////////////////////////////////////////////////////////////////////////////////////
    // API 
    ///////////////////////////////////////////////////////////////////////////////////////////////////

    @Override
    public void leaveChannel(CommonPublicChatChannel channel) {
        //todo
    }

    @Override
    public ObservableArray<CommonPublicChatChannel> getChannels() {
        return persistableStore.getChannels();
    }

    @Override
    protected String provideChannelTitle(CommonPublicChatChannel chatChannel) {
        return Res.get(chatChannel.getId() + ".title");
    }


    ///////////////////////////////////////////////////////////////////////////////////////////////////
    // Protected 
    ///////////////////////////////////////////////////////////////////////////////////////////////////

    @Override
    protected CommonPublicChatMessage createChatMessage(String text,
                                                        Optional<Citation> citation,
                                                        CommonPublicChatChannel commonPublicChatChannel,
                                                        UserProfile authorUserProfileId) {
        return new CommonPublicChatMessage(commonPublicChatChannel.getChatChannelDomain(),
                commonPublicChatChannel.getId(),
                authorUserProfileId.getId(),
                text,
                citation,
                new Date().getTime(),
                false);
    }

    @Override
    protected CommonPublicChatMessage createEditedChatMessage(CommonPublicChatMessage originalChatMessage,
                                                              String editedText,
                                                              UserProfile authorUserProfileId) {
        return new CommonPublicChatMessage(originalChatMessage.getChatChannelDomain(),
                originalChatMessage.getChannelId(),
                authorUserProfileId.getId(),
                editedText,
                originalChatMessage.getCitation(),
                originalChatMessage.getDate(),
                true);
    }

    @Override
    protected void maybeAddDefaultChannels() {
        if (!getChannels().isEmpty()) {
            return;
        }

        getChannels().setAll(defaultChannels);
        persist();
    }
}