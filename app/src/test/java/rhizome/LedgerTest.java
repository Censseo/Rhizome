package rhizome;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static rhizome.core.common.Crypto.generateKeyPair;
import static rhizome.core.common.Helpers.PDN;

import java.io.IOException;

import org.bouncycastle.crypto.params.Ed25519PublicKeyParameters;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import rhizome.core.ledger.Ledger;
import rhizome.core.ledger.PublicAddress;

class LedgerTest {

    private static final String TEST_DB_PATH = "./test-data/tmpdb";
    private Ledger ledger;
    private PublicAddress wallet;

    @BeforeEach
    void setUp() throws IOException {
        // Simulate the generateKeyPair and walletAddressFromPublicKey functions
        var pair = generateKeyPair();
        wallet = PublicAddress.of((Ed25519PublicKeyParameters) pair.getPublic());

        ledger = new Ledger(TEST_DB_PATH);
    }

    @AfterEach
    void tearDown() throws IOException {
        ledger.closeDB();
        ledger.deleteDB();
    }

    @Test
    void testLedgerStoresWallets() {
        ledger.createWallet(wallet);
        ledger.deposit(wallet, PDN(50.0));
        assertEquals(PDN(50.0), ledger.getWalletValue(wallet));
    }
}