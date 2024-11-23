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

package bisq.desktop.main.content.bisq_easy;

import bisq.account.payment_method.BitcoinPaymentMethod;
import bisq.account.payment_method.FiatPaymentMethod;
import bisq.bisq_easy.BisqEasyUtil;
import bisq.bonded_roles.market_price.MarketPriceService;
import bisq.chat.bisqeasy.open_trades.BisqEasyOpenTradeChannel;
import bisq.common.currency.Market;
import bisq.common.util.StringUtils;
import bisq.i18n.Res;
import bisq.network.identity.NetworkId;
import bisq.offer.Direction;
import bisq.offer.amount.OfferAmountFormatter;
import bisq.offer.amount.spec.AmountSpec;
import bisq.offer.amount.spec.RangeAmountSpec;
import bisq.offer.bisq_easy.BisqEasyOffer;
import bisq.offer.payment_method.PaymentMethodSpecFormatter;
import bisq.offer.price.spec.PriceSpec;
import bisq.trade.Trade;
import bisq.trade.bisq_easy.BisqEasyTrade;
import bisq.trade.bisq_easy.BisqEasyTradeService;
import bisq.user.identity.UserIdentity;
import bisq.user.identity.UserIdentityService;
import bisq.user.profile.UserProfile;
import lombok.extern.slf4j.Slf4j;

import java.util.List;
import java.util.Optional;

@Slf4j
public class BisqEasyServiceUtil {
    public static boolean isMaker(UserIdentityService userIdentityService, BisqEasyOffer bisqEasyOffer) {
        return bisqEasyOffer.isMyOffer(userIdentityService.getMyUserProfileIds());
    }

    public static Optional<BisqEasyTrade> findTradeFromChannel(UserIdentityService userIdentityService,
                                                               BisqEasyTradeService bisqEasyTradeService,
                                                               BisqEasyOpenTradeChannel channel) {
        UserIdentity myUserIdentity = channel.getMyUserIdentity();
        BisqEasyOffer bisqEasyOffer = channel.getBisqEasyOffer();
        boolean maker = isMaker(userIdentityService, bisqEasyOffer);
        UserProfile peerUserProfile = channel.getPeer();
        NetworkId takerNetworkId = maker ? peerUserProfile.getNetworkId() : myUserIdentity.getUserProfile().getNetworkId();
        String tradeId = Trade.createId(bisqEasyOffer.getId(), takerNetworkId.getId());
        return bisqEasyTradeService.findTrade(tradeId);
    }

    public static String createBasicOfferBookMessage(MarketPriceService marketPriceService,
                                                     Market market,
                                                     String bitcoinPaymentMethodNames,
                                                     String fiatPaymentMethodNames,
                                                     AmountSpec amountSpec,
                                                     PriceSpec priceSpec) {
        String priceInfo = String.format("%s %s", Res.get("bisqEasy.tradeWizard.review.chatMessage.price"), BisqEasyUtil.getFormattedPriceSpec(priceSpec));
        boolean hasAmountRange = amountSpec instanceof RangeAmountSpec;
        String quoteAmountAsString = OfferAmountFormatter.formatQuoteAmount(marketPriceService, amountSpec, priceSpec, market, hasAmountRange, true);
        return Res.get("bisqEasy.tradeWizard.review.chatMessage.offerDetails", quoteAmountAsString, bitcoinPaymentMethodNames, fiatPaymentMethodNames, priceInfo);
    }

    public static String createOfferBookMessageFromPeerPerspective(String messageOwnerNickName,
                                                                   MarketPriceService marketPriceService,
                                                                   Direction direction,
                                                                   Market market,
                                                                   List<BitcoinPaymentMethod> bitcoinPaymentMethods,
                                                                   List<FiatPaymentMethod> fiatPaymentMethods,
                                                                   AmountSpec amountSpec,
                                                                   PriceSpec priceSpec) {
        String bitcoinPaymentMethodNames = PaymentMethodSpecFormatter.fromPaymentMethods(bitcoinPaymentMethods);
        String fiatPaymentMethodNames = PaymentMethodSpecFormatter.fromPaymentMethods(fiatPaymentMethods);
        return createOfferBookMessageFromPeerPerspective(messageOwnerNickName,
                marketPriceService,
                direction,
                market,
                bitcoinPaymentMethodNames,
                fiatPaymentMethodNames,
                amountSpec,
                priceSpec);
    }

    public static String createOfferBookMessageFromPeerPerspective(String messageOwnerNickName,
                                                                   MarketPriceService marketPriceService,
                                                                   Direction direction,
                                                                   Market market,
                                                                   String bitcoinPaymentMethodNames,
                                                                   String fiatPaymentMethodNames,
                                                                   AmountSpec amountSpec,
                                                                   PriceSpec priceSpec) {
        String ownerNickName = StringUtils.truncate(messageOwnerNickName, 28);
        String priceInfo = String.format("%s %s", Res.get("bisqEasy.tradeWizard.review.chatMessage.price"), BisqEasyUtil.getFormattedPriceSpec(priceSpec));
        boolean hasAmountRange = amountSpec instanceof RangeAmountSpec;
        String quoteAmountAsString = OfferAmountFormatter.formatQuoteAmount(marketPriceService, amountSpec, priceSpec, market, hasAmountRange, true);
        return buildOfferBookMessage(ownerNickName, direction, quoteAmountAsString, bitcoinPaymentMethodNames, fiatPaymentMethodNames, priceInfo);
    }

    private static String buildOfferBookMessage(String messageOwnerNickName,
                                                Direction direction,
                                                String quoteAmount,
                                                String bitcoinPaymentMethodNames,
                                                String fiatPaymentMethodNames,
                                                String price) {
        return direction == Direction.BUY
                ? Res.get("bisqEasy.tradeWizard.review.chatMessage.peerMessage.sell",
                messageOwnerNickName, quoteAmount, bitcoinPaymentMethodNames, fiatPaymentMethodNames, price)
                : Res.get("bisqEasy.tradeWizard.review.chatMessage.peerMessage.buy",
                messageOwnerNickName, quoteAmount, bitcoinPaymentMethodNames, fiatPaymentMethodNames, price);
    }
}
