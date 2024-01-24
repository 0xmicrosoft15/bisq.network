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

package bisq.trade.bisq_easy.protocol;

import bisq.common.fsm.State;
import lombok.Getter;
import lombok.ToString;

@ToString
@Getter
public enum BisqEasyTradeState implements State {
    INIT,

    // Take offer
    TAKER_SENT_TAKE_OFFER_REQUEST,
    MAKER_SENT_TAKE_OFFER_RESPONSE,


    TAKER_RECEIVED_TAKE_OFFER_RESPONSE, // TODO: REMOVE

    // BUYER AS TAKER *****************************/
    // Branch 1: Buyer received take offer response first
    TAKER_RECEIVED_TAKE_OFFER_RESPONSE__BUYER_DID_NOT_SENT_BTC_ADDRESS__BUYER_DID_NOT_RECEIVED_ACCOUNT_DATA,

        // Branch 1.1.: Buyer sent Btc address first
        TAKER_RECEIVED_TAKE_OFFER_RESPONSE__BUYER_SENT_BTC_ADDRESS__BUYER_DID_NOT_RECEIVED_ACCOUNT_DATA,

        // Branch 1.2.: Buyer received account data first
        TAKER_RECEIVED_TAKE_OFFER_RESPONSE__BUYER_DID_NOT_SENT_BTC_ADDRESS__BUYER_RECEIVED_ACCOUNT_DATA,

    // Branch 2: Buyer sent Btc address first
    TAKER_DID_NOT_RECEIVED_TAKE_OFFER_RESPONSE__BUYER_SENT_BTC_ADDRESS__BUYER_DID_NOT_RECEIVED_ACCOUNT_DATA,
    //TAKER_RECEIVED_TAKE_OFFER_RESPONSE__BUYER_SENT_BTC_ADDRESS__BUYER_DID_NOT_RECEIVED_ACCOUNT_DATA,

    // Unique final converging step (all three states have been completed)
    TAKER_RECEIVED_TAKE_OFFER_RESPONSE__BUYER_SENT_BTC_ADDRESS__BUYER_RECEIVED_ACCOUNT_DATA,
    // *********************************************/


    // SELLER AS TAKER *****************************/
    // Branch 1: Seller received take offer response first
    TAKER_RECEIVED_TAKE_OFFER_RESPONSE__SELLER_DID_NOT_SENT_ACCOUNT_DATA__SELLER_DID_NOT_RECEIVED_BTC_ADDRESS,

        // Branch 1.1.: Seller sent account data first
        TAKER_RECEIVED_TAKE_OFFER_RESPONSE__SELLER_SENT_ACCOUNT_DATA__SELLER_DID_NOT_RECEIVED_BTC_ADDRESS,

        // Branch 1.2.: Buyer received account data first
        TAKER_RECEIVED_TAKE_OFFER_RESPONSE__SELLER_DID_NOT_SENT_ACCOUNT_DATA__SELLER_RECEIVED_BTC_ADDRESS,

    // Branch 2: Seller sent account data first
    TAKER_DID_NOT_RECEIVED_TAKE_OFFER_RESPONSE__SELLER_SENT_ACCOUNT_DATA__SELLER_DID_NOT_RECEIVED_BTC_ADDRESS,
    //TAKER_RECEIVED_TAKE_OFFER_RESPONSE__SELLER_SENT_ACCOUNT_DATA__SELLER_DID_NOT_RECEIVED_BTC_ADDRESS,

    // Unique final converging step (all three states have been completed)
    TAKER_RECEIVED_TAKE_OFFER_RESPONSE__SELLER_SENT_ACCOUNT_DATA__SELLER_RECEIVED_BTC_ADDRESS,
    // *********************************************/


    // SELLER AS MAKER *****************************/
    // Branch 1: Seller sent account data first
    SELLER_SENT_ACCOUNT_DATA__SELLER_DID_NOT_RECEIVED_BTC_ADDRESS,

    // Branch 2: Seller received btc address first
    SELLER_DID_NOT_SENT_ACCOUNT_DATA__SELLER_RECEIVED_BTC_ADDRESS,

    // Unique final converging step (the two states have been completed)
    SELLER_SENT_ACCOUNT_DATA__SELLER_RECEIVED_BTC_ADDRESS,
    // *********************************************/


    // BUYER AS MAKER *****************************/
    // Branch 1: Buyer sends btc address first
    BUYER_SENT_BTC_ADDRESS__BUYER_DID_NOT_RECEIVED_ACCOUNT_DATA,

    // Branch 2: Buyer receives account data first
    BUYER_DID_NOT_SENT_BTC_ADDRESS__BUYER_RECEIVED_ACCOUNT_DATA,

    // Unique final converging step (the two states have been completed)
    BUYER_SENT_BTC_ADDRESS__BUYER_RECEIVED_ACCOUNT_DATA,
    // *********************************************/


    // TODO: Remove
    // Account details
    // Branch 1: Peer starts sending
    BUYER_DID_NOT_SEND_BTC_ADDRESS_AND_RECEIVED_ACCOUNT_DATA,
    SELLER_DID_NOT_SEND_ACCOUNT_DATA_AND_RECEIVED_BTC_ADDRESS,

    // Branch 2: Self start sending
    BUYER_SENT_BTC_ADDRESS_AND_WAITING_FOR_ACCOUNT_DATA,
    SELLER_SENT_ACCOUNT_DATA_AND_WAITING_FOR_BTC_ADDRESS,

    // Branch completed and payment data and BTC address are received
    BUYER_SENT_BTC_ADDRESS_AND_RECEIVED_ACCOUNT_DATA,
    SELLER_SENT_ACCOUNT_DATA_AND_RECEIVED_BTC_ADDRESS,

    // Fiat settlement
    BUYER_SENT_FIAT_SENT_CONFIRMATION,
    SELLER_RECEIVED_FIAT_SENT_CONFIRMATION,
    SELLER_CONFIRMED_FIAT_RECEIPT,
    BUYER_RECEIVED_SELLERS_FIAT_RECEIPT_CONFIRMATION,

    // BTC transfer
    SELLER_SENT_BTC_SENT_CONFIRMATION,
    BUYER_RECEIVED_BTC_SENT_CONFIRMATION,
    BTC_CONFIRMED(true),

    REJECTED(true),
    PEER_REJECTED(true),

    CANCELLED(true),
    PEER_CANCELLED(true),

    FAILED(true),
    FAILED_AT_PEER(true);

    private final boolean isFinalState;
    private final int ordinal;

    BisqEasyTradeState() {
        this(false);
    }

    BisqEasyTradeState(boolean isFinalState) {
        this.isFinalState = isFinalState;
        ordinal = ordinal();
    }
}
