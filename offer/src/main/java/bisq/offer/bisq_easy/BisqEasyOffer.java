package bisq.offer.bisq_easy;


import bisq.account.protocol_type.ProtocolType;
import bisq.common.currency.Market;
import bisq.i18n.Res;
import bisq.network.NetworkId;
import bisq.offer.Direction;
import bisq.offer.Offer;
import bisq.offer.SettlementSpec;
import bisq.offer.amount_spec.AmountSpec;
import bisq.offer.offer_options.OfferOption;
import bisq.offer.price_spec.PriceSpec;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;
import lombok.extern.slf4j.Slf4j;

import java.util.List;
import java.util.stream.Collectors;

@ToString
@EqualsAndHashCode(callSuper = true)
@Slf4j
@Getter
public final class BisqEasyOffer extends Offer {
    private static List<ProtocolType> createSwapProtocolTypes() {
        return List.of(ProtocolType.BISQ_EASY);
    }


    private final transient String chatMessageText;

    public BisqEasyOffer(String id,
                         long date,
                         NetworkId makerNetworkId,
                         Direction direction,
                         Market market,
                         boolean isMinAmountEnabled,
                         long baseSideMinAmount,
                         long baseSideMaxAmount,
                         long quoteSideMinAmount,
                         long quoteSideMaxAmount,
                         List<String> settlementMethodNames,
                         String makersTradeTerms,
                         long requiredTotalReputationScore,
                         double pricePremiumAsPercentage) {
        this(id,
                date,
                makerNetworkId,
                direction,
                market,
                AmountSpec.fromMinMaxAmounts(isMinAmountEnabled,
                        baseSideMinAmount,
                        baseSideMaxAmount,
                        quoteSideMinAmount,
                        quoteSideMaxAmount),
                PriceSpec.fromPremiumAsPercentage(pricePremiumAsPercentage),
                createSwapProtocolTypes(),
                SettlementSpec.createBaseSideSpecsForBitcoinMainChain(),
                SettlementSpec.createQuoteSideSpecsFromMethodNames(settlementMethodNames),
                OfferOption.fromTradeTermsAndReputationScore(makersTradeTerms, requiredTotalReputationScore)
        );
    }

    private BisqEasyOffer(String id,
                          long date,
                          NetworkId makerNetworkId,
                          Direction direction,
                          Market market,
                          AmountSpec amountSpec,
                          PriceSpec priceSpec,
                          List<ProtocolType> protocolTypes,
                          List<SettlementSpec> baseSideSettlementSpecs,
                          List<SettlementSpec> quoteSideSettlementSpecs,
                          List<OfferOption> offerOptions) {
        super(id,
                date,
                makerNetworkId,
                direction,
                market,
                amountSpec,
                priceSpec,
                protocolTypes,
                baseSideSettlementSpecs,
                quoteSideSettlementSpecs,
                offerOptions);

        chatMessageText = Res.get("createOffer.bisqEasyOffer.chatMessage",
                getMakersDirectionAsDisplayString(),
                getQuoteSideAmountAsDisplayString(),
                getQuoteSideSettlementMethodsAsDisplayString());
    }

    @Override
    public bisq.offer.protobuf.Offer toProto() {
        return getSwapOfferBuilder().setBisqEasyOffer(
                        bisq.offer.protobuf.BisqEasyOffer.newBuilder())
                .build();
    }

    public static BisqEasyOffer fromProto(bisq.offer.protobuf.Offer proto) {
        List<ProtocolType> protocolTypes = proto.getProtocolTypesList().stream()
                .map(ProtocolType::fromProto)
                .collect(Collectors.toList());
        List<SettlementSpec> baseSideSettlementSpecs = proto.getBaseSideSettlementSpecsList().stream()
                .map(SettlementSpec::fromProto)
                .collect(Collectors.toList());
        List<SettlementSpec> quoteSideSettlementSpecs = proto.getQuoteSideSettlementSpecsList().stream()
                .map(SettlementSpec::fromProto)
                .collect(Collectors.toList());
        List<OfferOption> offerOptions = proto.getOfferOptionsList().stream()
                .map(OfferOption::fromProto)
                .collect(Collectors.toList());
        return new BisqEasyOffer(proto.getId(),
                proto.getDate(),
                NetworkId.fromProto(proto.getMakerNetworkId()),
                Direction.fromProto(proto.getDirection()),
                Market.fromProto(proto.getMarket()),
                AmountSpec.fromProto(proto.getAmountSpec()),
                PriceSpec.fromProto(proto.getPriceSpec()),
                protocolTypes,
                baseSideSettlementSpecs,
                quoteSideSettlementSpecs,
                offerOptions);
    }
}
