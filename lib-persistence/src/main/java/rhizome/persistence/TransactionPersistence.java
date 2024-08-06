package rhizome.persistence;

import rhizome.core.crypto.SHA256Hash;
import rhizome.core.transaction.Transaction;

public interface TransactionPersistence {

    boolean hasTransaction(Transaction t);

    int blockForTransaction(Transaction t);

    int blockForTransactionId(SHA256Hash txHash);

    void insertTransaction(Transaction t, int blockId);

    void removeTransaction(Transaction t);

}