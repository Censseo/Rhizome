package rhizome;

import org.junit.jupiter.api.*;
import rhizome.persistence.TransactionStore;
import rhizome.core.user.User;

import java.io.IOException;

import static org.junit.jupiter.api.Assertions.*;

class TransactionStoreTests {

    private static final String TEST_DB_PATH = "./test-data/tmpdb";
    private TransactionStore txdb;
    private User miner;
    private User other;

    @BeforeEach
    void setUp() throws IOException {
        txdb = new TransactionStore(TEST_DB_PATH);
        miner = User.create();
        other = User.create();
    }

    @AfterEach
    void tearDown() throws IOException {
        txdb.deleteDB();
    }

    @Test
    void testTransactionStoreHandlesTransactions() {
        // Test for a mined transaction
        var t = miner.mine();
        assertFalse(txdb.hasTransaction(t));
        txdb.insertTransaction(t, 1);
        assertTrue(txdb.hasTransaction(t));
        assertEquals(1, txdb.blockForTransaction(t));
        txdb.removeTransaction(t);
        assertFalse(txdb.hasTransaction(t));

        // Test for a transaction sent to another user
        var t2 = miner.send(other, 333);
        assertFalse(txdb.hasTransaction(t2));
        txdb.insertTransaction(t2, 3);
        assertTrue(txdb.hasTransaction(t2));
        assertEquals(3, txdb.blockForTransaction(t2));
        txdb.removeTransaction(t2);
        assertFalse(txdb.hasTransaction(t2));
    }
}