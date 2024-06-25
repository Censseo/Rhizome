package rhizome.core.ledger;

import org.iq80.leveldb.DBException;

import rhizome.core.transaction.TransactionAmount;
import rhizome.persistence.leveldb.LevelDBDataStore;

import static rhizome.core.common.Utils.bytesToLong;
import static rhizome.core.common.Utils.longToBytes;

import java.io.IOException;
import java.util.concurrent.atomic.AtomicLong;

public class Ledger extends LevelDBDataStore {

    private static final long HUNDRED_MILLIONS = 100_000_000L * 100L;

    public Ledger(String path) throws IOException {
        super.init(path);
    }

    public boolean hasWallet(PublicAddress wallet) {
        return getWalletValueInternal(wallet) != null;
    }

    public void createWallet(PublicAddress wallet) {
        if (this.hasWallet(wallet)) {
            throw new LedgerException("Wallet already exists");
        }
        this.setWalletValue(wallet, new TransactionAmount(0L));
    }

    private void setWalletValue(PublicAddress wallet, TransactionAmount amount) {
        try {
            db().put(wallet.address().asArray(), longToBytes(amount.amount()));
        } catch (DBException e) {
            throw new LedgerException("Failed to set wallet value", e);
        }
    }

    public TransactionAmount getWalletValue(PublicAddress wallet) {
        TransactionAmount amount = getWalletValueInternal(wallet);
        if (amount == null) {
            throw new IllegalArgumentException("Tried fetching wallet value for non-existent wallet");
        }
        return amount;
    }

    private TransactionAmount getWalletValueInternal(PublicAddress wallet) {
        byte[] value = db().get(wallet.address().asArray());
        if (value == null) {
            return null;
        }
        long amount = bytesToLong(value);

        // set overflow values to 0
        if (amount > HUNDRED_MILLIONS) {
            return new TransactionAmount(0);
        }
        return new TransactionAmount(amount);
    }

    public void withdraw(PublicAddress wallet, TransactionAmount amt) {
        TransactionAmount currentAmount = getWalletValue(wallet);
        long newValue = currentAmount.amount() - amt.amount();
        if (newValue < 0) {
            throw new LedgerException("Insufficient funds for withdrawal");
        }
        this.setWalletValue(wallet, new TransactionAmount(newValue));
    }

    public void revertSend(PublicAddress wallet, TransactionAmount amt) {
        adjustWalletBalance(wallet, amt.amount(), true);
    }

    public void deposit(PublicAddress wallet, TransactionAmount amt) {
        adjustWalletBalance(wallet, amt.amount(), true);
    }

    public void revertDeposit(PublicAddress wallet, TransactionAmount amt) {
        adjustWalletBalance(wallet, -amt.amount(), false);
    }

    private void adjustWalletBalance(PublicAddress wallet, long amount, boolean isAdding) {
        TransactionAmount currentAmount = getWalletValue(wallet);
        AtomicLong newAmount = new AtomicLong(currentAmount.amount());
        if (isAdding) {
            if (Long.MAX_VALUE - newAmount.get() < amount) {
                // uncomment the line below if overflow should raise an exception
                // throw new ArithmeticException("Overflow detected during balance adjustment");
                newAmount.set(Long.MAX_VALUE); // or handle the overflow as desired
            } else {
                newAmount.addAndGet(amount);
            }
        } else {
            if (newAmount.get() < amount) {
                // uncomment the line below if underflow should raise an exception
                // throw new ArithmeticException("Underflow detected during balance adjustment");
                newAmount.set(0); // or handle the underflow as desired
            } else {
                newAmount.addAndGet(-amount);
            }
        }
        setWalletValue(wallet, new TransactionAmount(newAmount.get()));
    }
}
