package rhizome.persistence;

import rhizome.core.ledger.PublicAddress;
import rhizome.core.transaction.TransactionAmount;

public interface LedgerPersistence {

    boolean hasWallet(PublicAddress wallet);

    void createWallet(PublicAddress wallet);

    TransactionAmount getWalletValue(PublicAddress wallet);

    void withdraw(PublicAddress wallet, TransactionAmount amt);

    void revertSend(PublicAddress wallet, TransactionAmount amt);

    void deposit(PublicAddress wallet, TransactionAmount amt);

    void revertDeposit(PublicAddress wallet, TransactionAmount amt);

}