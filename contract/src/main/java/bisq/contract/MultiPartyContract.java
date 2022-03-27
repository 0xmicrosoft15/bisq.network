package bisq.contract;

import bisq.offer.Offer;
import bisq.account.protocol.SwapProtocolType;
import lombok.Getter;

import java.util.Set;

@Getter
public class MultiPartyContract<T extends Offer> extends AContract<T> {
    private final Set<Party> parties;

    public MultiPartyContract(T listing, SwapProtocolType protocolType, Set<Party> parties) {
        super(listing, protocolType);
        this.parties = parties;
    }
}
