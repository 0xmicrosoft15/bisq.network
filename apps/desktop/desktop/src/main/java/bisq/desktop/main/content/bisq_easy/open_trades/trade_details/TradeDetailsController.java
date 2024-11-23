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

package bisq.desktop.main.content.bisq_easy.open_trades.trade_details;

import bisq.account.payment_method.BitcoinPaymentRail;
import bisq.bisq_easy.BisqEasyUtil;
import bisq.bisq_easy.NavigationTarget;
import bisq.chat.bisqeasy.open_trades.BisqEasyOpenTradeChannel;
import bisq.contract.bisq_easy.BisqEasyContract;
import bisq.desktop.ServiceProvider;
import bisq.desktop.common.view.Controller;
import bisq.desktop.common.view.InitWithDataController;
import bisq.desktop.common.view.NavigationController;
import bisq.desktop.overlay.OverlayController;
import bisq.i18n.Res;
import bisq.offer.price.spec.FixPriceSpec;
import bisq.presentation.formatters.DateFormatter;
import bisq.presentation.formatters.PriceFormatter;
import bisq.trade.bisq_easy.BisqEasyTrade;
import bisq.trade.bisq_easy.BisqEasyTradeFormatter;
import bisq.trade.bisq_easy.BisqEasyTradeUtils;
import bisq.user.profile.UserProfile;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;
import lombok.extern.slf4j.Slf4j;

import java.util.Optional;

@Slf4j
public class TradeDetailsController extends NavigationController implements InitWithDataController<TradeDetailsController.InitData> {
    @Getter
    @EqualsAndHashCode
    @ToString
    public static class InitData {
        private final BisqEasyTrade bisqEasyTrade;
        private final BisqEasyOpenTradeChannel channel;

        public InitData(BisqEasyTrade bisqEasyTrade, BisqEasyOpenTradeChannel channel) {
            this.bisqEasyTrade = bisqEasyTrade;
            this.channel = channel;
        }
    }

    @Getter
    private final TradeDetailsModel model;
    @Getter
    private final TradeDetailsView view;
    private BisqEasyTrade trade;
    private BisqEasyOpenTradeChannel channel;
    private BisqEasyContract contract;

    public TradeDetailsController(ServiceProvider serviceProvider) {
        super(NavigationTarget.BISQ_EASY_TRADE_DETAILS);

        model = new TradeDetailsModel();
        view = new TradeDetailsView(model, this);
    }

    @Override
    public void initWithData(InitData initData) {
        trade = initData.bisqEasyTrade;
        channel = initData.channel;
        contract = trade.getContract();
    }

    @Override
    public void onActivate() {
        model.setTradeDate(DateFormatter.formatDateTime(contract.getTakeOfferDate()));
        model.setMe(String.format("%s (%s)", channel.getMyUserIdentity().getNickName(), BisqEasyTradeFormatter.getMakerTakerRole(trade).toLowerCase()));
        model.setPeer(channel.getPeer().getUserName());
        model.setOfferType(trade.getOffer().getDirection().isBuy()
                ? Res.get("bisqEasy.openTrades.tradeDetails.offerTypeAndMarket.buyOffer")
                : Res.get("bisqEasy.openTrades.tradeDetails.offerTypeAndMarket.sellOffer"));
        model.setMarket(Res.get("bisqEasy.openTrades.tradeDetails.offerTypeAndMarket.fiatMarket",
                trade.getOffer().getMarket().getQuoteCurrencyCode()));
        model.setFiatAmount(BisqEasyTradeFormatter.formatQuoteSideAmount(trade));
        model.setFiatCurrency(trade.getOffer().getMarket().getQuoteCurrencyCode());
        model.setBtcAmount(BisqEasyTradeFormatter.formatBaseSideAmount(trade));
        model.setPrice(PriceFormatter.format(BisqEasyTradeUtils.getPriceQuote(contract)));
        model.setPriceCodes(trade.getOffer().getMarket().getMarketCodes());
        model.setPriceSpec(trade.getOffer().getPriceSpec() instanceof FixPriceSpec
                ? ""
                : String.format("(%s)", BisqEasyUtil.getFormattedPriceSpec(trade.getOffer().getPriceSpec(), true)));
        model.setPaymentMethod(contract.getQuoteSidePaymentMethodSpec().getShortDisplayString());
        model.setSettlementMethod(contract.getBaseSidePaymentMethodSpec().getShortDisplayString());
        model.setTradeId(trade.getId());
        model.setPeerNetworkAddress(channel.getPeer().getAddressByTransportDisplayString(50));
        model.setOnChainSettlement(contract.getBaseSidePaymentMethodSpec().getPaymentMethod().getPaymentRail() == BitcoinPaymentRail.MAIN_CHAIN);
        model.setBtcPaymentAddress(trade.getBitcoinPaymentData().get() == null
                ? Res.get("bisqEasy.openTrades.tradeDetails.dataNotYetProvided")
                : trade.getBitcoinPaymentData().get());
        model.setBtcPaymentDataEmpty(trade.getBitcoinPaymentData().get() == null);
        model.setPaymentAccountData(trade.getPaymentAccountData().get() == null
                ? Res.get("bisqEasy.openTrades.tradeDetails.dataNotYetProvided")
                : trade.getPaymentAccountData().get());
        model.setPaymentAccountDataEmpty(trade.getPaymentAccountData().get() == null);
        model.setAssignedMediator(channel.getMediator().map(UserProfile::getUserName).orElse(""));
        model.setHasMediatorBeenAssigned(channel.getMediator().isPresent());
    }

    @Override
    public void onDeactivate() {
    }

    @Override
    protected Optional<? extends Controller> createController(NavigationTarget navigationTarget) {
        return Optional.empty();
    }

    void onClose() {
        OverlayController.hide();
    }
}
