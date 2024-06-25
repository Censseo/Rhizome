package rhizome.core.transaction;

import io.activej.bytebuf.ByteBuf;
import rhizome.core.common.SimpleHashType;

import static rhizome.core.common.Utils.bytesToHex;
import static rhizome.core.common.Utils.hexStringToByteArray;

public record TransactionSignature(ByteBuf signature) implements SimpleHashType {

    public static TransactionSignature empty() {
        return new TransactionSignature(SimpleHashType.empty(SIZE));
    }

    public static TransactionSignature random() {
        return new TransactionSignature(SimpleHashType.random(SIZE));
    }

    public static TransactionSignature of(byte[] bytes) {
        return new TransactionSignature(ByteBuf.wrapForReading(bytes));
    }

    public static TransactionSignature of(String hexString) {
        return TransactionSignature.of(hexStringToByteArray(hexString));
    }

    public String toHexString() {
        return bytesToHex(signature.getArray());
    }

    public byte[] toBytes() {
        return signature.getArray();
    }

    @Override
    public boolean equals(Object other) {
        if (!(other instanceof TransactionSignature)) {
            return false;
        }
        return signature.isContentEqual(((TransactionSignature) other).signature());
    }

    public static final int SIZE = 64;
    @Override
    public int getSize() {
        return SIZE;
    }
}
