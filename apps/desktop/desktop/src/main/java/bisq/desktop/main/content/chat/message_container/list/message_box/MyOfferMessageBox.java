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

package bisq.desktop.main.content.chat.message_container.list.message_box;

import bisq.chat.ChatChannel;
import bisq.chat.ChatMessage;
import bisq.chat.bisqeasy.offerbook.BisqEasyOfferbookMessage;
import bisq.common.util.StringUtils;
import bisq.desktop.components.containers.Spacer;
import bisq.desktop.components.controls.DropdownMenu;
import bisq.desktop.components.controls.DropdownMenuItem;
import bisq.desktop.main.content.chat.message_container.list.ChatMessageListItem;
import bisq.desktop.main.content.chat.message_container.list.ChatMessagesListController;
import bisq.i18n.Res;
import bisq.offer.Direction;
import de.jensd.fx.fontawesome.AwesomeIcon;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Control;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;

import static com.google.common.base.Preconditions.checkArgument;

public final class MyOfferMessageBox extends BubbleMessageBox {
    private final Label myOfferTitle;
    private DropdownMenuItem removeOffer;
    private Label copyIcon;

    public MyOfferMessageBox(ChatMessageListItem<? extends ChatMessage, ? extends ChatChannel<? extends ChatMessage>> item,
                             ListView<ChatMessageListItem<? extends ChatMessage, ? extends ChatChannel<? extends ChatMessage>>> list,
                             ChatMessagesListController controller) {
        super(item, list, controller);

        // User profile icon
        userProfileIcon.setSize(OFFER_MESSAGE_USER_ICON_SIZE);

        // Dropdown menu
        DropdownMenu dropdownMenu = createAndGetDropdownMenu();

        // My offer title
        myOfferTitle = createAndGetMyOfferTitle();

        // Message
        message.getStyleClass().add("chat-my-offer-message");

        // Offer content
        VBox offerMessage = new VBox(10, myOfferTitle, message);
        HBox offerContent = new HBox(15, offerMessage, userProfileIconVbox);
        offerContent.setAlignment(Pos.CENTER);
        VBox messageContent = new VBox(5, offerContent, dropdownMenu);
        messageContent.setAlignment(Pos.CENTER_RIGHT);

        // Message background
        messageBgHBox.getStyleClass().add("chat-my-offer-message-bg");
        messageBgHBox.getChildren().setAll(messageContent);
        messageBgHBox.setMaxWidth(Control.USE_PREF_SIZE);

        // Reactions
        HBox.setMargin(supportedLanguages, new Insets(5, 0, -5, 0));
        HBox.setMargin(copyIcon, new Insets(4, 10, -4, 0));
        actionsHBox.getChildren().setAll(Spacer.fillHBox(), supportedLanguages, copyIcon);

        contentVBox.setAlignment(Pos.CENTER_RIGHT);
        contentVBox.getChildren().setAll(userNameAndDateHBox, messageBgHBox, actionsHBox);
    }

    @Override
    protected void setUpUserNameAndDateTime() {
        super.setUpUserNameAndDateTime();

        userNameAndDateHBox = new HBox(10, dateTime, userName);
        userNameAndDateHBox.setAlignment(Pos.CENTER_RIGHT);
        VBox.setMargin(userNameAndDateHBox, new Insets(0, 10, 5, 0));
    }

    @Override
    protected void setUpReactions() {
        copyIcon = getIconWithToolTip(AwesomeIcon.COPY, Res.get("action.copyToClipboard"));
        actionsHBox.setVisible(false);
    }

    @Override
    protected void addReactionsHandlers() {
        copyIcon.setOnMouseClicked(e -> onCopyMessage(String.format("%s\n%s", myOfferTitle.getText(), message.getText())));
    }

    private DropdownMenu createAndGetDropdownMenu() {
        removeOffer = new DropdownMenuItem("delete-bin-red-lit-10", "delete-bin-red",
                Res.get("offer.deleteOffer"));
        removeOffer.setOnAction(e -> controller.onDeleteMessage(item.getChatMessage()));
        removeOffer.getStyleClass().add("red-menu-item");

        DropdownMenu dropdownMenu = new DropdownMenu("ellipsis-h-grey", "ellipsis-h-white", true);
        dropdownMenu.setVisible(item.isPublicChannel());
        dropdownMenu.setManaged(item.isPublicChannel());
        dropdownMenu.setTooltip(Res.get("chat.dropdownMenu.tooltip"));
        dropdownMenu.addMenuItems(removeOffer);
        return dropdownMenu;
    }

    private Label createAndGetMyOfferTitle() {
        BisqEasyOfferbookMessage bisqEasyOfferbookMessage = (BisqEasyOfferbookMessage) item.getChatMessage();
        checkArgument(bisqEasyOfferbookMessage.getBisqEasyOffer().isPresent(),
                "Bisq Easy Offerbook message must contain an offer");

        Direction direction = bisqEasyOfferbookMessage.getBisqEasyOffer().get().getDirection();
        String directionString = StringUtils.capitalize(Res.get("offer." + direction.name().toLowerCase()));
        String title = Res.get("bisqEasy.tradeWizard.review.chatMessage.myMessageTitle", directionString);
        Label label = new Label(title);
        label.getStyleClass().addAll("bisq-easy-offer-title", "normal-text", "font-default");
        boolean isBuy = direction == Direction.BUY;
        label.getStyleClass().add(isBuy ? "bisq-easy-offer-buy-btc-title" : "bisq-easy-offer-sell-btc-title");
        return label;
    }

    @Override
    public void cleanup() {
        removeOffer.setOnAction(null);
    }
}
