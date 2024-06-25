package rhizome.core.crypto;

import io.activej.bytebuf.ByteBuf;
import rhizome.core.common.SimpleHashType;

public record RIPEMD160Hash(ByteBuf hash) implements SimpleHashType {
    public static RIPEMD160Hash empty() {
        return new RIPEMD160Hash(SimpleHashType.empty(SIZE));
    }

    public static RIPEMD160Hash random() {
        return new RIPEMD160Hash(SimpleHashType.random(SIZE));
    }

    @Override
    public boolean equals(Object other) {
        if (!(other instanceof RIPEMD160Hash)) {
            return false;
        }
        return hash.isContentEqual(((RIPEMD160Hash) other).hash());
    }

    public static final int SIZE = 20;
    @Override
    public int getSize() {
        return SIZE;
    }

    @Override
    public byte[] toBytes() {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'toBytes'");
    }
}
