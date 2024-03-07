package bisq.network.p2p.services.confidential.resend;

import bisq.common.proto.NetworkProto;
import bisq.network.identity.NetworkId;
import bisq.network.p2p.message.EnvelopePayloadMessage;
import bisq.network.p2p.services.confidential.ack.AckRequestingMessage;
import bisq.network.p2p.services.confidential.ack.MessageDeliveryStatus;
import bisq.security.keys.KeyPairProtoUtil;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;

import java.security.KeyPair;

import static com.google.common.base.Preconditions.checkArgument;

@Getter
@EqualsAndHashCode
@ToString
public class ResendMessageData implements NetworkProto {
    public static ResendMessageData from(ResendMessageData data, MessageDeliveryStatus messageDeliveryStatus) {
        return new ResendMessageData(data.getAckRequestingMessage(),
                data.getReceiverNetworkId(),
                data.getSenderKeyPair(),
                data.getSenderNetworkId(),
                messageDeliveryStatus);
    }

    private final EnvelopePayloadMessage envelopePayloadMessage;
    private final NetworkId receiverNetworkId;
    private final KeyPair senderKeyPair;
    private final NetworkId senderNetworkId;
    private final MessageDeliveryStatus messageDeliveryStatus;

    public ResendMessageData(AckRequestingMessage ackRequestingMessage,
                             NetworkId receiverNetworkId,
                             KeyPair senderKeyPair,
                             NetworkId senderNetworkId,
                             MessageDeliveryStatus messageDeliveryStatus) {

        this((EnvelopePayloadMessage) ackRequestingMessage,
                receiverNetworkId,
                senderKeyPair,
                senderNetworkId,
                messageDeliveryStatus);

    }

    private ResendMessageData(EnvelopePayloadMessage envelopePayloadMessage,
                              NetworkId receiverNetworkId,
                              KeyPair senderKeyPair,
                              NetworkId senderNetworkId,
                              MessageDeliveryStatus messageDeliveryStatus) {
        this.envelopePayloadMessage = envelopePayloadMessage;
        this.receiverNetworkId = receiverNetworkId;
        this.senderKeyPair = senderKeyPair;
        this.senderNetworkId = senderNetworkId;
        this.messageDeliveryStatus = messageDeliveryStatus;

        verify();
    }

    @Override
    public void verify() {
        checkArgument(envelopePayloadMessage instanceof AckRequestingMessage,
                "envelopePayloadMessage must be of type AckRequestingMessage");
    }

    @Override
    public bisq.network.protobuf.ResendMessageData toProto() {
        return bisq.network.protobuf.ResendMessageData.newBuilder()
                .setEnvelopePayloadMessage(envelopePayloadMessage.toProto())
                .setReceiverNetworkId(receiverNetworkId.toProto())
                .setSenderKeyPair(KeyPairProtoUtil.toProto(senderKeyPair))
                .setSenderNetworkId(senderNetworkId.toProto())
                .setMessageDeliveryStatus(messageDeliveryStatus.toProto())
                .build();
    }

    public static ResendMessageData fromProto(bisq.network.protobuf.ResendMessageData proto) {
        return new ResendMessageData(EnvelopePayloadMessage.fromProto(proto.getEnvelopePayloadMessage()),
                NetworkId.fromProto(proto.getReceiverNetworkId()),
                KeyPairProtoUtil.fromProto(proto.getSenderKeyPair()),
                NetworkId.fromProto(proto.getSenderNetworkId()),
                MessageDeliveryStatus.fromProto(proto.getMessageDeliveryStatus())
        );
    }

    public String getId() {
        return getAckRequestingMessage().getId();
    }

    public AckRequestingMessage getAckRequestingMessage() {
        return (AckRequestingMessage) envelopePayloadMessage;
    }
}
