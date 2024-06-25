package rhizome;

import org.junit.jupiter.api.Test;

import rhizome.core.crypto.PrivateKey;
import rhizome.core.crypto.PublicKey;
import rhizome.core.crypto.SHA256Hash;

import org.bouncycastle.crypto.params.Ed25519PrivateKeyParameters;
import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static rhizome.core.common.Crypto.SHA256;
import static rhizome.core.common.Crypto.addWork;
import static rhizome.core.common.Crypto.generateKeyPair;
import static rhizome.core.common.Crypto.signWithPrivateKey;
import static rhizome.core.common.Utils.bytesToHex;
import static rhizome.core.common.Utils.hexStringToByteArray;
import static rhizome.core.common.Crypto.checkSignature;

import java.math.BigInteger;
import java.nio.charset.StandardCharsets;

class CryptoTests {

    @Test
    void testKeyStringConversion() {
        var keys = generateKeyPair();
        var publicKey = PublicKey.of(keys.getPublic());
        var publicKeyData = publicKey.toBytes();

        var publicKeyString = publicKey.toHexString();
        var convertedPublicKey = PublicKey.of(publicKeyString);
        var convertedPublicKeyData = convertedPublicKey.toBytes();

        assertArrayEquals(publicKeyData, convertedPublicKeyData);
    }

    @Test
    void testSignatureStringConversion() {
        var keys = generateKeyPair();
        var privateKey = new PrivateKey((Ed25519PrivateKeyParameters) keys.getPrivate());
        var message = "FOOBAR";
        var signature = signWithPrivateKey(message.getBytes(StandardCharsets.UTF_8), privateKey);

        var signatureString = bytesToHex(signature);
        var convertedSignature = hexStringToByteArray(signatureString);

        assertArrayEquals(signature, convertedSignature);
    }

    @Test
    void testSignatureVerifications() {
        var keys = generateKeyPair();
        var privateKey = new PrivateKey((Ed25519PrivateKeyParameters) keys.getPrivate());
        var publicKey = PublicKey.of(keys.getPublic());

        var message = "FOOBAR";
        var signature = signWithPrivateKey(message.getBytes(StandardCharsets.UTF_8), privateKey);
        var status = checkSignature(message.getBytes(StandardCharsets.UTF_8), signature, publicKey);
        assertTrue(status);

        // check with wrong public key
        var wrongKeys = generateKeyPair();
        var wrongPrivateKey = new PrivateKey((Ed25519PrivateKeyParameters) wrongKeys.getPrivate());
        var wrongSignature = signWithPrivateKey(message.getBytes(StandardCharsets.UTF_8), wrongPrivateKey);
        status = checkSignature(message.getBytes(StandardCharsets.UTF_8), wrongSignature, publicKey);
        assertFalse(status);
    }

    @Test
    void total_work() {
        var work = BigInteger.ZERO;
        work = addWork(work, 16);
        work = addWork(work, 16);
        work = addWork(work, 16);
        var base = BigInteger.valueOf(2);
        var mult = BigInteger.valueOf(3);
        var expected = mult.multiply(base.pow(16));
        assertEquals(expected, work);
        assertEquals(new BigInteger("196608"), work);
        work = addWork(work, 32);
        work = addWork(work, 28);
        work = addWork(work, 74);
        work = addWork(work, 174);
        var b = expected;
        b = b.add(base.pow(32));
        b = b.add(base.pow(28));
        b = b.add(base.pow(74));
        b = b.add(base.pow(174));
        
        assertEquals(b, work);
        assertEquals("23945242826029513411849172299242470459974281928572928", work.toString());
    }

    // @Test
    // void mine_hash() {
    //     var hash = SHA256("Hello World".getBytes());
    //     var answer = mineHash(hash, 6);
    //     var newHash = concatHashes(hash, answer);
    //     byte[] data = newHash.data();
        
    //     // check first 6 bits are 0
    //     assertTrue((data[0] & 0b11111100) == 0);
    // }

    @Test
    void sha256ToString() {
        var message = "FOOBAR";
        var hash = SHA256(message.getBytes(StandardCharsets.UTF_8));
        var hashString = hash.toHexString();
        var convertedHash = SHA256Hash.of(hashString);

        assertTrue(hash.hash().isContentEqual(convertedHash.hash()));
    }
}
