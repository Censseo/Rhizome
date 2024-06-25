package rhizome.core.common;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.HexFormat;

public class Utils {

    private static final HexFormat hexFormat = HexFormat.of().withUpperCase();

    private Utils() {}
        
    public static byte[] longToBytes(long value) {
        ByteBuffer buffer = ByteBuffer.allocate(Long.BYTES);
        buffer.order(ByteOrder.BIG_ENDIAN);
        buffer.putLong(value);
        return buffer.array();
    }
    
    public static long bytesToLong(byte[] bytes) {
        ByteBuffer buffer = ByteBuffer.wrap(bytes);
        buffer.order(ByteOrder.BIG_ENDIAN);
        return buffer.getLong();
    }

    public static String bytesToHex(byte[] bytes) {
        return hexFormat.formatHex(bytes);
    }

    public static byte[] hexStringToByteArray(String s) {
        return hexFormat.parseHex(s);
    }
}
