package rhizome;

import org.bouncycastle.crypto.params.Ed25519PrivateKeyParameters;
import org.junit.jupiter.api.Test;

import rhizome.core.crypto.PrivateKey;
import rhizome.core.ledger.PublicAddress;
import rhizome.core.transaction.TransactionSignature;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.junit.jupiter.api.Assertions.assertEquals;

import static rhizome.core.common.Crypto.signWithPrivateKey;
import static rhizome.core.common.Utils.bytesToHex;
import static rhizome.core.common.Utils.hexStringToByteArray;

class UtilsTest {

    static String TEST_PUBLIC_KEY = "471979EB816A71948474F86EED38D26DC1DB94506E776031C91958BEDF2A8862";
    static String TEST_PRIVATE_KEY = "FA0D5D01EB4965A6B19E62AF17215731A4BFD50CBE2881F395917446DE2EC996471979EB816A71948474F86EED38D26DC1DB94506E776031C91958BEDF2A8862";
    static String TEST_MESSAGE = "test";
    static String LEGACY_SIGNATURE = "914D303286115695366F6977B7748AA51CAFFEDFFC0560652D17DDEE809E8120DBE00715204D7D71B96A6350C0DCF6BFE4EB3B018810A9F50052E8934E900107";
    static String ADDRESS = "00A7D087CC62C349B10D8EE7BD923047F881E21F8BFB94E0CA";

    @Test
    void checkAddressSerialisation() {
        var a = PublicAddress.random();
        var b = PublicAddress.of(a.toHexString());
        assertEquals(a,b);
    }

    @Test
    void checkTransactionSignatureSerialisation() {
        var a = TransactionSignature.random();
        var b = TransactionSignature.of(a.toHexString());
        assertEquals(a,b);
    }

    @Test
    void checkSignWithPrivateKey() {
        var messageToSign = TEST_MESSAGE;
        var signtureExpected = LEGACY_SIGNATURE;
        var privateKey = new PrivateKey(new Ed25519PrivateKeyParameters(hexStringToByteArray(TEST_PRIVATE_KEY), 0));
        var signature = signWithPrivateKey(messageToSign.getBytes(UTF_8), privateKey);
        assertEquals(signtureExpected,bytesToHex(signature));
    }
}
