package rhizome.persistence.rocksdb;

import rhizome.core.transaction.Transaction;
import rhizome.persistence.TransactionPersistence;
import rhizome.core.crypto.SHA256Hash;
import rhizome.core.ledger.LedgerException;

import java.nio.ByteBuffer;

import org.rocksdb.RocksDBException;

public class RocksDBTransactionPersistence extends RocksDBDataStore implements TransactionPersistence {

    public RocksDBTransactionPersistence(String path) {
        super.init(path);
    }

    @Override
    public boolean hasTransaction(Transaction t) {
        SHA256Hash txHash = t.hashContents();
        byte[] key = txHash.toBytes();
        try {
            byte[] value = db().get(key);
            return value != null;
        } catch (RocksDBException e) {
            throw new LedgerException("Failed to check transaction existence", e);
        }
    }

    @Override
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
        } catch (RocksDBException e) {
            throw new LedgerException("Could not find block for specified transaction", e);
        }
    }

    @Override
    public int blockForTransactionId(SHA256Hash txHash) {
        byte[] key = txHash.toBytes();
        try {
            byte[] value = db().get(key);
            if (value == null) {
                return 0; // Or throw an exception if that's the required logic
            }
            ByteBuffer buffer = ByteBuffer.wrap(value);
            return buffer.getInt();
        } catch (RocksDBException e) {
            throw new LedgerException("Could not find block for specified transaction ID", e);
        }
    }

    @Override
    public void insertTransaction(Transaction t, int blockId) {
        SHA256Hash txHash = t.hashContents();
        byte[] key = txHash.toBytes();
        ByteBuffer buffer = ByteBuffer.allocate(Integer.BYTES);
        buffer.putInt(blockId);
        try {
            db().put(key, buffer.array());
        } catch (RocksDBException e) {
            throw new LedgerException("Could not write transaction to DB", e);
        }
    }

    @Override
    public void removeTransaction(Transaction t) {
        SHA256Hash txHash = t.hashContents();
        byte[] key = txHash.toBytes();
        try {
            db().delete(key);
        } catch (RocksDBException e) {
            throw new LedgerException("Could not remove transaction from DB", e);
        }
    }
}