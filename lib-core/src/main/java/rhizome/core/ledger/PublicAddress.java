package rhizome.core.ledger;

import static rhizome.core.common.Utils.bytesToHex;
import static rhizome.core.common.Utils.hexStringToByteArray;

import java.util.Arrays;

import org.bouncycastle.crypto.digests.RIPEMD160Digest;
import org.bouncycastle.crypto.digests.SHA256Digest;
import io.activej.bytebuf.ByteBuf;
import rhizome.core.common.SimpleHashType;
import rhizome.core.crypto.PublicKey;

public record PublicAddress(ByteBuf address) implements SimpleHashType {
    public PublicAddress {
        checkSize(address);
    }

    public static PublicAddress empty() {
        return new PublicAddress(SimpleHashType.empty(SIZE));
    }

    public static PublicAddress random() {
        return new PublicAddress(SimpleHashType.random(SIZE));
    }

    public static PublicAddress of(PublicKey publicKey){
        if (!publicKey.key().isPresent()) {
            return PublicAddress.empty();
        }

        byte[] publicKeyBytes = publicKey.toBytes();

        SHA256Digest sha256 = new SHA256Digest();
        byte[] hash1 = new byte[32];
        sha256.update(publicKeyBytes, 0, publicKeyBytes.length);
        sha256.doFinal(hash1, 0);

        RIPEMD160Digest ripemd160 = new RIPEMD160Digest();
        byte[] hash2 = new byte[20];
        ripemd160.update(hash1, 0, hash1.length);
        ripemd160.doFinal(hash2, 0);

        byte[] hash3 = new byte[32];
        byte[] hash4 = new byte[32];
        sha256.reset();
        sha256.update(hash2, 0, hash2.length);
        sha256.doFinal(hash3, 0);
        sha256.reset();
        sha256.update(hash3, 0, hash3.length);
        sha256.doFinal(hash4, 0);

        ByteBuf buf = ByteBuf.wrapForWriting(new byte[25]);

        buf.writeByte((byte) 0);
        buf.put(hash2);
        buf.put(Arrays.copyOfRange(hash4, 0, 4));

        return new PublicAddress(buf); 
    }

    public static PublicAddress of(byte[] address) {
        return new PublicAddress(ByteBuf.wrapForReading(address));
    }

    public static PublicAddress of(String hexString) {
        if (hexString.length() != 50) {
            throw new IllegalArgumentException("Invalid wallet address string");
        }
        return PublicAddress.of(hexStringToByteArray(hexString));
    }

    public String toHexString() {
        return bytesToHex(address.getArray());
    }

    public byte[] toBytes() {
        return address.getArray();
    }

    @Override
    public boolean equals(Object other) {
        if (!(other instanceof PublicAddress)) {
            return false;
        }
        return address.isContentEqual(((PublicAddress) other).address());
    }

    public static final int SIZE = 25;
    @Override
    public int getSize() {
        return 25;
    }
}