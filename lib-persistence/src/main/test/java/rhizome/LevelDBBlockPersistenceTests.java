package rhizome;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.math.BigInteger;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import rhizome.core.block.Block;
import rhizome.core.block.BlockImpl;
import rhizome.core.common.Constants;
import rhizome.core.transaction.Transaction;
import rhizome.core.transaction.TransactionImpl;
import rhizome.core.user.User;
import rhizome.persistence.leveldb.LevelDBBlockPersistence;

class LevelDBBlockPersistenceTests {

    private static final String TEST_DB_PATH = "./test-data/tmpdb";
    private LevelDBBlockPersistence blocks;
    private User miner;
    private User receiver;

    @BeforeEach
    void setUp() throws IOException {
        blocks = new LevelDBBlockPersistence(TEST_DB_PATH);
        miner = User.create();
        receiver = User.create();
    }

    @AfterEach
    void tearDown() throws IOException {
        blocks.deleteDB();
    }

    @Test
    void testStoresBlock() throws IOException {

        var a = Block.empty();
        ((BlockImpl) a).id(2);
        var t = miner.mine();
        a.addTransaction(t);

        // send tiny shares to receiver:
        for (int i = 0; i < 5; i++) {
            var t2 = miner.send(receiver, 1);
            ((TransactionImpl)t2).timestamp(i);
            a.addTransaction(t2);
        }

        assertFalse(blocks.hasBlock(2));
        blocks.addBlock(a);
        assertTrue(blocks.hasBlock(2));
        var b = blocks.getBlock(2);
        assertEquals(b, a);

        // test we can get transactions for wallets
        var to = receiver.getAddress();
        var txIdsTo = blocks.getTransactionsForWallet(to);
        assertEquals(5, txIdsTo.size());

        var from = miner.getAddress();
        var txIdsFrom = blocks.getTransactionsForWallet(from);
        assertEquals(6, txIdsFrom.size());

        // test transactions are removed
        blocks.removeBlockWalletTransactions(b);
        txIdsTo = blocks.getTransactionsForWallet(to);
        txIdsFrom = blocks.getTransactionsForWallet(from);
        assertEquals(0, txIdsTo.size());
        assertEquals(0, txIdsFrom.size());
    }

    @Test
    void testBlockstoreStoresMultiple() throws IOException {
        for (int i = 0; i < 30; i++) {
            var a = Block.empty();
            ((BlockImpl) a).id(i + 1);
            var t = miner.mine();
            a.addTransaction(t);

            // Send tiny shares to receiver
            for (int j = 0; j < 5; j++) {
                var t2 = miner.send(receiver, 1);
                ((TransactionImpl) t2).timestamp(j);
                a.addTransaction(t2);
            }

            assertFalse(blocks.hasBlock(i + 1));
            blocks.addBlock(a);
            assertTrue(blocks.hasBlock(i + 1));
            var b = blocks.getBlock(i + 1);
            assertEquals(b, a);
        }

        blocks.setBlockCount(30);
        blocks.setTotalWork(BigInteger.valueOf(30 * Constants.MIN_DIFFICULTY));
        assertEquals(30, blocks.getBlockCount());
        assertEquals(BigInteger.valueOf(30 * Constants.MIN_DIFFICULTY), blocks.getTotalWork());
    }

    @Test
    void testBlockstoreReturnsValidRawData() throws IOException {
        var a = Block.empty();
        a.id(1);
        Transaction t = miner.mine();
        a.addTransaction(t);

        for (int i = 0; i < 5; i++) {
            Transaction t2 = miner.send(receiver, 1);
            a.addTransaction(t2);
        }

        blocks.addBlock(a);

        var buffer = blocks.getRawData(a.id()).asArray();
        var b = blocks.fromRawData(buffer);
        assertEquals(a, b);
    }

    @Test
    void testBlockstoreStoresBigint() throws IOException {
        BigInteger a = BigInteger.valueOf(2);
        BigInteger b = a.pow(10);
        blocks.setTotalWork(b);
    
        BigInteger c = blocks.getTotalWork();
    
        assertEquals(b, c);
    }
}