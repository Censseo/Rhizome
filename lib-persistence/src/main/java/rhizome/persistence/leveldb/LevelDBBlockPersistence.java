package rhizome.persistence.leveldb;

import org.iq80.leveldb.DBException;
import org.iq80.leveldb.DBIterator;
import org.iq80.leveldb.ReadOptions;
import org.iq80.leveldb.WriteOptions;

import io.activej.bytebuf.ByteBuf;
import io.activej.bytebuf.ByteBufPool;
import io.activej.common.MemSize;
import rhizome.core.block.Block;
import rhizome.core.block.dto.BlockDto;
import rhizome.core.crypto.SHA256Hash;
import rhizome.core.ledger.PublicAddress;
import rhizome.core.serialization.BinarySerializable;
import rhizome.core.transaction.Transaction;
import rhizome.core.transaction.dto.TransactionDto;
import rhizome.persistence.BlockPersistence;

import java.io.IOException;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class LevelDBBlockPersistence extends LevelDBDataStore implements BlockPersistence {

    static final String BLOCK_COUNT_KEY = "BLOCK_COUNT";
    static final String TOTAL_WORK_KEY = "TOTAL_WORK";

    public LevelDBBlockPersistence(String path) throws IOException {
        super.init(path);
    }

    public void setBlockCount(long count) {
        set(BLOCK_COUNT_KEY, count);
    }

    public long getBlockCount() {
        return (long) get(BLOCK_COUNT_KEY, Long.class);
    }

    public void setTotalWork(BigInteger count) {
        set(TOTAL_WORK_KEY, count);
    }

    public BigInteger getTotalWork() {
        return (BigInteger) get(TOTAL_WORK_KEY, BigInteger.class);
    }

    public boolean hasBlockCount() {
        return hasKey(BLOCK_COUNT_KEY);        
    }

    public boolean hasBlock(int blockId) {
        return hasKey(blockId);
    }

    public BlockDto  getBlockHeader(int blockId) {       
        return BinarySerializable.fromBuffer((byte[])get(blockId, byte[].class), BlockDto.class);
    }
    public ByteBuf getBlockHeadeAsByteBuf(int blockId) {       
        return (ByteBuf) get(blockId, ByteBuf.class);
    }

    public List<TransactionDto> getBlockTransactions(BlockDto block) {
        var transactions = new ArrayList<TransactionDto>();
        for (int i = 0; i < block.numTransactions(); i++) {            
            var value = (byte[]) get(composeKey(block.id(), i), byte[].class);
            transactions.add(BinarySerializable.fromBuffer(value, TransactionDto.class));
        }
        return transactions;
    }

    public ByteBuf getRawData(int blockId) {
        var blockHeader = getBlockHeader(blockId);
        var bufferSize = MemSize.of((long) BlockDto.BUFFER_SIZE + (TransactionDto.BUFFER_SIZE * blockHeader.numTransactions()));
        var buffer = ByteBufPool.allocateExact(bufferSize);
        buffer.put(blockHeader.toBuffer());

        getBlockTransactions(blockHeader).forEach(transaction -> buffer.put(transaction.toBuffer()));
        return buffer;
    }

    public Block fromRawData(byte[] rawData) {
        var buffer = ByteBuf.wrapForReading(rawData);
    
        var blockHeaderData = new byte[BlockDto.BUFFER_SIZE];
        buffer.read(blockHeaderData);
        var blockHeader = BinarySerializable.fromBuffer(blockHeaderData, BlockDto.class);
    
        var block = Block.of(blockHeader, new ArrayList<>());
    
        for (var i = 0; i < blockHeader.numTransactions(); i++) {
            var transactionData = new byte[TransactionDto.BUFFER_SIZE];
            buffer.read(transactionData);
            TransactionDto transactionDto = BinarySerializable.fromBuffer(transactionData, TransactionDto.class);
            block.addTransaction(Transaction.of(transactionDto));
        }
    
        return block;
    }

    public Block getBlock(int blockId) {
        BlockDto block = getBlockHeader(blockId);
        List<Transaction> transactions = new ArrayList<>();
        getBlockTransactions(block).forEach(transaction -> transactions.add(Transaction.of(transaction)));
        return Block.of(block, transactions);
    }

    // NOTE: seek method looks like bugged as it return everything wi
    public List<SHA256Hash> getTransactionsForWallet(PublicAddress wallet) {
        var address = wallet.toBytes();
        List<SHA256Hash> transactions = new ArrayList<>();

        try (DBIterator iterator = db().iterator(new ReadOptions())) {
            for(iterator.seek(address); iterator.hasNext(); iterator.next()) {
                byte[] key = iterator.peekNext().getKey();
                byte[] addressKey = Arrays.copyOfRange(key, 0, 25);
                if (!Arrays.equals(addressKey, address)) {
                    continue;
                }
                byte[] txidBytes = Arrays.copyOfRange(key, 25, 57);
                SHA256Hash txid = SHA256Hash.of(txidBytes);
                transactions.add(txid);
            }
        } catch (IOException e) {
            throw new LevelDBException("Failed to iterate over the database", e);
        }
        
        return transactions;
    }

    public void removeBlockWalletTransactions(Block block) {
        for(Transaction t : block.transactions()) {
            SHA256Hash txid = t.hashContents();
            
            var w1Key = new WalletTransactionKey(t.from(), txid, false);
            var w2Key = new WalletTransactionKey(t.to(), txid, false);
            
            try {
                deleteTransaction(w1Key);
                deleteTransaction(w2Key);
            } catch (DBException e) {
                throw new LevelDBException("Could not remove transaction from wallet in blockstore db: " + e.getMessage(), e);
            }
        }
    }

    private void deleteTransaction(WalletTransactionKey key) throws DBException {
        WriteOptions writeOptions = new WriteOptions().sync(true);
        db().delete(key.toByteArray(), writeOptions);
    }


    public void addBlock(Block block) throws LevelDBException {
        set(block.id(), block.serialize().toBuffer());

        for (int i = 0; i < block.transactions().size(); i++) {
            var transaction = block.transactions().get(i);
            var transactionDto = transaction.serialize();
            set(composeKey(block.id(), i), transactionDto.toBuffer());
            set(composeKey(PublicAddress.of(transactionDto.signingKey).toBytes(), transaction.hashContents().toBytes()), new byte[0]);
            set(composeKey(transactionDto.to.toBytes(), transaction.hashContents().toBytes()), new byte[0]);
        }
    }

    private static class WalletTransactionKey {
        PublicAddress addr;
        SHA256Hash txId;
        
        public WalletTransactionKey(PublicAddress address, SHA256Hash txId, boolean isStartKey) {
            this.addr = address;
            var buf = new byte[SHA256Hash.SIZE];

            if(isStartKey) {
                Arrays.fill(buf, (byte) -128);
            } else {
                Arrays.fill(buf, (byte) 127);
            }

            if(txId != null) {
                this.txId = txId;
            } else {
                this.txId = SHA256Hash.of(buf);
            }
        }
                
        byte[] toByteArray() {
            return key(addr, txId);
        }

        static byte[] key(PublicAddress address, SHA256Hash sha256) {
            return composeKey(address.address().array(), sha256.toBytes());
        }
    }
}
