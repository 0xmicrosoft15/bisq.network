/*
 * This file is part of Bisq.
 *
 * Bisq is free software: you can redistribute it and/or modify it
 * under the terms of the GNU Affero General Private License as published by
 * the Free Software Foundation, either version 3 of the License, or (at
 * your option) any later version.
 *
 * Bisq is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE. See the GNU Affero General Private
 * License for more details.
 *
 * You should have received a copy of the GNU Affero General Private License
 * along with Bisq. If not, see <http://www.gnu.org/licenses/>.
 */

package bisq.desktop.primary.main.content.chat.channels;

import bisq.application.DefaultApplicationService;
import bisq.chat.channel.ChatChannel;
import bisq.chat.channel.ChatChannelDomain;
import bisq.chat.channel.ChatChannelSelectionService;
import bisq.chat.channel.priv.PrivateChatChannel;
import bisq.chat.channel.priv.PrivateChatChannelService;
import bisq.chat.message.ChatMessage;
import bisq.common.observable.Pin;
import bisq.common.observable.collection.CollectionObserver;
import bisq.i18n.Res;

public abstract class PrivateChannelSelectionMenu<
        C extends PrivateChatChannel<?>,
        S extends PrivateChatChannelService<?, C, ?>,
        E extends ChatChannelSelectionService
        > extends ChannelSelectionMenu<C, S, E> {

    public PrivateChannelSelectionMenu() {
        super();
    }

    protected static abstract class Controller<
            V extends PrivateChannelSelectionMenu.View<M, ?>,
            M extends Model,
            C extends PrivateChatChannel<?>,
            S extends PrivateChatChannelService<?, C, ?>,
            E extends ChatChannelSelectionService
            >
            extends ChannelSelectionMenu.Controller<V, M, C, S, E> {

        protected Pin channelCollectionObserverPin;

        public Controller(DefaultApplicationService applicationService, ChatChannelDomain chatChannelDomain) {
            super(applicationService, chatChannelDomain);
        }

        @Override
        public void onActivate() {
            super.onActivate();

            channelCollectionObserverPin = chatChannelService.getChannels().addListener(new CollectionObserver<>() {
                @Override
                public void add(C channel) {
                    addListenersToChannel(channel);
                }

                @Override
                public void remove(Object channel) {
                    if (channel instanceof PrivateChatChannel<?>) {
                        removeListenersToChannel(((PrivateChatChannel<?>) channel).getId());
                    }
                }

                @Override
                public void clear() {
                    unbindAndClearAllChannelListeners();
                }
            });
        }

        @Override
        public void onDeactivate() {
            channelCollectionObserverPin.unbind();
        }

        @Override
        protected boolean isChannelExpectedInstance(ChatChannel<? extends ChatMessage> chatChannel) {
            return chatChannel instanceof PrivateChatChannel;
        }
    }

    protected static abstract class View<
            M extends Model,
            C extends PrivateChannelSelectionMenu.Controller<?, M, ?, ?, ?>
            > extends ChannelSelectionMenu.View<M, C> {
        protected View(M model, C controller) {
            super(model, controller);
        }

        @Override
        protected String getHeadlineText() {
            return Res.get("social.privateChannels");
        }
    }
}