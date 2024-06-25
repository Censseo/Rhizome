package rhizome.core.crypto;

import org.bouncycastle.crypto.params.AsymmetricKeyParameter;
import org.bouncycastle.crypto.params.Ed25519PrivateKeyParameters;

import rhizome.core.common.SimpleHashType;

import static rhizome.core.common.Utils.bytesToHex;
import static rhizome.core.common.Utils.hexStringToByteArray;

public record PrivateKey(Ed25519PrivateKeyParameters key) implements SimpleHashType {

    public static PrivateKey of(byte[] bytes) {
        return new PrivateKey(new Ed25519PrivateKeyParameters(bytes, 0));
    }

    public static PrivateKey of(AsymmetricKeyParameter keyParameter) {
        if (keyParameter == null) {
            return null;
        }
        if (keyParameter instanceof Ed25519PrivateKeyParameters) {
            return new PrivateKey((Ed25519PrivateKeyParameters) keyParameter);
        }
        return null;
    }

    public static PrivateKey of(String hexString) {
        if ("".equals(hexString)) {
            return null;
        }
        if (hexString.length() != 64) {
            throw new IllegalArgumentException("Invalid public key string length. Expected 64 characters for a 32-byte key.");
        }
        return new PrivateKey(new Ed25519PrivateKeyParameters(hexStringToByteArray(hexString), 0));    
    }

    public String toHexString() {
        if (key == null) {
            return "";
        }
        return bytesToHex(key.getEncoded() == null ? new byte[0] : key.getEncoded());
    }

    public byte[] toBytes() {
        return key.getEncoded();
    }

    public static final int SIZE = 32;
    @Override
    public int getSize() {
        return SIZE;
    }
}
