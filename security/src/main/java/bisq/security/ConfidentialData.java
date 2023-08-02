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

package bisq.security;

import bisq.common.proto.Proto;
import bisq.common.validation.BasicInputValidation;
import com.google.protobuf.ByteString;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;
import lombok.extern.slf4j.Slf4j;

import static com.google.common.base.Preconditions.checkArgument;

@Slf4j
@Getter
@ToString
@EqualsAndHashCode
public final class ConfidentialData implements Proto {
    private final byte[] senderPublicKey;
    private final byte[] iv;
    private final byte[] cipherText;
    private final byte[] signature;

    public ConfidentialData(byte[] senderPublicKey,
                            byte[] iv,
                            byte[] cipherText,
                            byte[] signature) {
        this.senderPublicKey = senderPublicKey;
        this.iv = iv;
        this.cipherText = cipherText;
        this.signature = signature;

        log.error("senderPublicKey {}", senderPublicKey.length);
        log.error("iv {}", iv.length);
        log.error("cipherText {}", cipherText.length);
        log.error("signature {}", signature.length);


        checkArgument(iv.length < 20);
        checkArgument(cipherText.length < 10_000);

        BasicInputValidation.validatePubKey(senderPublicKey);
        BasicInputValidation.validateSignature(signature);
    }

    public bisq.security.protobuf.ConfidentialData toProto() {
        return bisq.security.protobuf.ConfidentialData.newBuilder()
                .setSenderPublicKey(ByteString.copyFrom(senderPublicKey))
                .setIv(ByteString.copyFrom(iv))
                .setCipherText(ByteString.copyFrom(cipherText))
                .setSignature(ByteString.copyFrom(signature))
                .build();
    }

    public static ConfidentialData fromProto(bisq.security.protobuf.ConfidentialData proto) {
        return new ConfidentialData(proto.getSenderPublicKey().toByteArray(),
                proto.getIv().toByteArray(),
                proto.getCipherText().toByteArray(),
                proto.getSignature().toByteArray());
    }
}
