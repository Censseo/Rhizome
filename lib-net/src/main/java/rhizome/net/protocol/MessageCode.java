package rhizome.net.protocol;

import java.util.Map;
import static java.util.Map.entry;
import static java.util.Map.of;
import static java.util.Map.ofEntries;
import static java.util.Collections.unmodifiableMap;

import static rhizome.net.protocol.ProtocolVersion.V01;

public enum MessageCode {
    CONNECT(0x00),
    DISCONNECT(0x01),
    ERROR(0x02),
    BLOCKS_REQUEST(0x03),
    BLOCKS_RESPONSE(0x04),
    NEW_BLOCK(0x05),
    SYNC_BLOCK(0x06),
    SYNCBLOCK_REQUEST(0x07),
    TRANSACTION_NEW(0x08),
    PEER_LIST_REQUEST(0x09),
    PEER_LIST_RESPONSE(0x0A),
    CLOSE(0x0B), 
    SYNC(0x0C);

    private static final Map<ProtocolVersion, Map<Integer, MessageCode>> byteToMessageCodeMap = unmodifiableMap(
        of(
            V01, ofEntries(
                entry(0x00, CONNECT),
                entry(0x01, DISCONNECT),
                entry(0x02, ERROR),
                entry(0x03, BLOCKS_REQUEST),
                entry(0x04, BLOCKS_RESPONSE),
                entry(0x05, NEW_BLOCK),
                entry(0x06, SYNC_BLOCK),
                entry(0x07, SYNCBLOCK_REQUEST),
                entry(0x08, TRANSACTION_NEW),
                entry(0x09, PEER_LIST_REQUEST),
                entry(0x0A, PEER_LIST_RESPONSE),
                entry(0x0B, CLOSE),
                entry(0x0C, SYNC)
            )
        )
    );

    private final int commandCode;

    MessageCode(int commandCode) {
        this.commandCode = commandCode;
    }

    public static MessageCode fromByte(byte i, ProtocolVersion v) {
        return byteToMessageCodeMap.get(v).get((int) i);
    }

    public byte asByte() {
        return (byte) (commandCode);
    }
}
