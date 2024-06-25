package rhizome.persistence;

import java.math.BigInteger;
import java.util.List;

import rhizome.core.block.Block;
import rhizome.core.block.dto.BlockDto;
import rhizome.core.crypto.SHA256Hash;
import rhizome.core.ledger.PublicAddress;
import rhizome.core.transaction.dto.TransactionDto;

public interface BlockPersistence {
    long getBlockCount();

    BigInteger getTotalWork();

    boolean hasBlock(int blockId);

    BlockDto getBlockHeader(int blockId);

    List<TransactionDto> getBlockTransactions(BlockDto block);

    Block getBlock(int blockId);

    List<SHA256Hash> getTransactionsForWallet(PublicAddress wallet);

    void removeBlockWalletTransactions(Block block);
    
    void addBlock(Block block);
}
