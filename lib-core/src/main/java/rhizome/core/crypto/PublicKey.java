package rhizome.core.crypto;

import org.bouncycastle.crypto.params.AsymmetricKeyParameter;
import org.bouncycastle.crypto.params.Ed25519PublicKeyParameters;
import rhizome.core.common.SimpleHashType;

import static rhizome.core.common.Utils.bytesToHex;
import static rhizome.core.common.Utils.hexStringToByteArray;

import java.util.Optional;

public record PublicKey(Optional<Ed25519PublicKeyParameters> key) implements SimpleHashType {

    public static final int SIZE = 32;

    public static PublicKey empty() {
        return new PublicKey(Optional.empty());
    }
    
    public static PublicKey of(AsymmetricKeyParameter keyParameter) {
        if (keyParameter == null) {
            return empty();
        }
        if (keyParameter instanceof Ed25519PublicKeyParameters) {
            return new PublicKey(Optional.of((Ed25519PublicKeyParameters) keyParameter));
        }
        return empty();
    }

    public static PublicKey of(byte[] bytes) {
        if (bytes == null || bytes.length != SIZE || isZeroFilled(bytes)) {
            return empty();
        }
        return new PublicKey(Optional.of(new Ed25519PublicKeyParameters(bytes, 0)));
    }

    public static PublicKey of(String hexString) {
        if (hexString == null || hexString.isEmpty()) {
            return empty();
        }
        if (hexString.length() != 64) {
            throw new IllegalArgumentException("Invalid public key string length. Expected 64 characters for a 32-byte key.");
        }
        return new PublicKey(Optional.of(new Ed25519PublicKeyParameters(hexStringToByteArray(hexString), 0)));
    }

    public String toHexString() {
        return key.map(pk -> bytesToHex(pk.getEncoded())).orElse("");
    }

    public byte[] toBytes() {
        return key.map(Ed25519PublicKeyParameters::getEncoded).orElseGet(() -> new byte[SIZE]);
    }

    public Ed25519PublicKeyParameters get() {
        return key.orElse(null);
    }

    @Override
    public int getSize() {
        return SIZE;
    }

    private static boolean isZeroFilled(byte[] bytes) {
        for (byte b : bytes) {
            if (b != 0) {
                return false;
            }
        }
        return true;
    }
}