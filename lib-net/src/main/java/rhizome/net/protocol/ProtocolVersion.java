package rhizome.net.protocol;

import java.util.List;

import com.google.common.collect.Lists;

public enum ProtocolVersion {
    V01((byte) 1);

    public static final byte LOWER = V01.getCode();
    public static final byte UPPER = V01.getCode();

    private final byte code;

        ProtocolVersion(byte code) {
        this.code = code;
    }

    public static ProtocolVersion fromCode(int code) {
        for (ProtocolVersion v : values()) {
            if (v.code == code) {
                return v;
            }
        }

        return null;
    }

    public static boolean isSupported(byte code) {
        return code >= LOWER && code <= UPPER;
    }

    public static List<ProtocolVersion> supported() {
        List<ProtocolVersion> supported = Lists.newArrayList();
        for (ProtocolVersion v : values()) {
            if (isSupported(v.code)) {
                supported.add(v);
            }
        }
        return supported;
    }

    public byte getCode() {
        return code;
    }
}
