package rhizome.core.blockchain;

import java.util.List;
import java.util.Map;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import org.bouncycastle.util.test.FixedSecureRandom.BigInteger;

import lombok.Getter;
import lombok.Setter;
import rhizome.core.crypto.SHA256Hash;
import rhizome.core.ledger.Ledger;
// import rhizome.persistence.BlockPersistence;
// import rhizome.persistence.TransactionStore;
// import rhizome.services.blockchain.MemPool;

@Getter
@Setter
public abstract class AbstractBlockchain implements Blockchain {

    protected boolean isSyncing;
    protected boolean shutdown;
    // protected MemPool memPool;
    protected int numBlocks;
    protected int retries;
    protected BigInteger totalWork;
    // protected BlockPersistence persistence;
    protected Ledger ledger;
    // protected TransactionStore txdb;
    protected SHA256Hash lastHash;
    protected int difficulty;
    protected int targetBlockCount;
    protected final Lock lock = new ReentrantLock();
    protected List<Thread> syncThread;
    protected Map<Integer, SHA256Hash> checkpoints;
}
 