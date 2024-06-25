package rhizome.persistence;

import org.iq80.leveldb.*;
import rhizome.core.transaction.Transaction;
import rhizome.persistence.leveldb.LevelDBDataStore;
import rhizome.core.crypto.SHA256Hash;
import rhizome.core.ledger.LedgerException;

import java.nio.ByteBuffer;
import java.io.IOException;


public class TransactionStore extends LevelDBDataStore {

    public TransactionStore(String path) throws IOException {
        super.init(path);
    }

    public boolean hasTransaction(Transaction t) {
        SHA256Hash txHash = t.hashContents();
        byte[] key = txHash.toBytes();
        try {
            byte[] value = db().get(key);
            return value != null;
        } catch (DBException e) {
            throw new LedgerException("Failed to check transaction existence", e);
        }
    }

    public int blockForTransaction(Transaction t) {
        SHA256Hash txHash = t.hashContents();
        byte[] key = txHash.toBytes();
        try {
            byte[] value = db().get(key);
            if (value == null) {
                return 0; // Or throw an exception if that's the required logic
            }
            ByteBuffer buffer = ByteBuffer.wrap(value);
            return buffer.getInt();
        } catch (DBException e) {
            throw new LedgerException("Could not find block for specified transaction", e);
        }
    }

    public int blockForTransactionId(SHA256Hash txHash) {
        byte[] key = txHash.toBytes();
        try {
            byte[] value = db().get(key);
            if (value == null) {
                return 0; // Or throw an exception if that's the required logic
            }
            ByteBuffer buffer = ByteBuffer.wrap(value);
            return buffer.getInt();
        } catch (DBException e) {
            throw new LedgerException("Could not find block for specified transaction ID", e);
        }
    }

    public void insertTransaction(Transaction t, int blockId) {
        SHA256Hash txHash = t.hashContents();
        byte[] key = txHash.toBytes();
        ByteBuffer buffer = ByteBuffer.allocate(Integer.BYTES);
        buffer.putInt(blockId);
        try {
            db().put(key, buffer.array());
        } catch (DBException e) {
            throw new LedgerException("Could not write transaction to DB", e);
        }
    }

    public void removeTransaction(Transaction t) {
        SHA256Hash txHash = t.hashContents();
        byte[] key = txHash.toBytes();
        try {
            db().delete(key);
        } catch (DBException e) {
            throw new LedgerException("Could not remove transaction from DB", e);
        }
    }
}