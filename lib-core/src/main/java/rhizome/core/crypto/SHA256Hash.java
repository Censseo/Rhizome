package rhizome.core.crypto;

import java.util.Arrays;
import io.activej.bytebuf.ByteBuf;
import rhizome.core.common.SimpleHashType;

import static rhizome.core.common.Utils.bytesToHex;
import static rhizome.core.common.Utils.hexStringToByteArray;

public record SHA256Hash (ByteBuf hash) implements SimpleHashType, Comparable<SHA256Hash> {

    public static SHA256Hash empty() {
        return new SHA256Hash(SimpleHashType.empty(SIZE));
    }

    public static SHA256Hash random() {
        return new SHA256Hash(SimpleHashType.random(SIZE));
    }

    public static SHA256Hash of(byte[] bytes) {
        return new SHA256Hash(ByteBuf.wrapForReading(bytes));
    }

    public static SHA256Hash of(String hexString) {
        return SHA256Hash.of(hexStringToByteArray(hexString));
    }

    public String toHexString() {
        return bytesToHex(hash.getArray());
    }

    public byte[] toBytes() {
        return hash.getArray();
    }

    @Override
    public boolean equals(Object other) {
        if (!(other instanceof SHA256Hash)) {
            return false;
        }
        return hash.isContentEqual(((SHA256Hash) other).hash());
    }

    @Override
    public int hashCode() {
        return Arrays.hashCode(hash.getArray());
    }

    @Override
    public int compareTo(SHA256Hash o) {
        for (int i = 0; i < this.hash.getArray().length; i++) {
            int compare = Byte.compare(this.hash.getArray()[i], o.hash.getArray()[i]);
            if (compare != 0) {
                return compare;
            }
        }
        return 0;
    }

    public static final int SIZE = 32;
    @Override
    public int getSize() {
        return SIZE;
    }
}
