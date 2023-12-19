package rhizome.core.block;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import rhizome.core.block.dto.BlockDto;
import rhizome.core.crypto.SHA256Hash;
import rhizome.net.Serializable;
import rhizome.core.transaction.Transaction;

import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public sealed interface Block permits BlockImpl {

    public static Block empty() {
        return BlockImpl.builder().build();
    }

    public static Block of(JSONObject json){
        return serializer().fromJson(json);
    }

    public static Block of(Block block) {
        var blockImpl = (BlockImpl) block;
        return BlockImpl.builder()
                .id(blockImpl.getId())
                .timestamp(blockImpl.getTimestamp())
                .difficulty(blockImpl.getDifficulty())
                .merkleRoot(blockImpl.getMerkleRoot())
                .lastBlockHash(blockImpl.getLastBlockHash())
                .nonce(blockImpl.getNonce())
                .transactions(blockImpl.getTransactions())
                .build();
    }

    public static Block of(BlockDto blockDto, List<Transaction> transactions) {
        return BlockImpl.builder()
                .id(blockDto.getId())
                .timestamp(blockDto.getTimestamp())
                .difficulty(blockDto.getDifficulty())
                .merkleRoot(blockDto.getMerkleRoot())
                .lastBlockHash(blockDto.getLastBlockHash())
                .nonce(blockDto.getNonce())
                .transactions(transactions)
                .build();
    }

    public BlockDto serialize();
    default BlockDto serialize(Block block) {
        return serializer().serialize(block);
    }

    public JSONObject toJson();
    default JSONObject toJson(Block block) {
        return serializer().toJson(block);
    }

    public int getId();
    public void setId(int id);
    public void addTransaction(Transaction t);
    public List<Transaction> getTransactions();
    public boolean verifyNonce();
    public SHA256Hash getHash();
    public SHA256Hash getLastBlockHash();
    public int getDifficulty();

    /**
     * Get instance of the serializer
     * @return
     */
    static BlockSerializer serializer(){
        return BlockSerializer.instance;
    }

    /**
     * Serializes the block
     */
    static class BlockSerializer implements Serializable<BlockDto, Block> {

        static final String ID = "id";
        static final String HASH = "hash";
        static final String TIMESTAMP = "timestamp";
        static final String DIFFICULTY = "difficulty";
        static final String NONCE = "nonce";
        static final String MERKLE_ROOT = "merkleRoot";
        static final String LAST_BLOCK_HASH = "lastBlockHash";
        static final String TRANSACTIONS = "transactions";        

        static BlockSerializer instance = new BlockSerializer();

        @Override
        public BlockDto serialize(Block block) {
            var blockImpl = (BlockImpl) block;
            return new BlockDto(
                blockImpl.getId(),
                blockImpl.getTimestamp(),
                blockImpl.getDifficulty(),
                blockImpl.getTransactions().size(),
                blockImpl.getLastBlockHash(),
                blockImpl.getMerkleRoot(),
                blockImpl.getNonce()
            );
        }
    
        @Override
        public Block deserialize(BlockDto object) {
            throw new UnsupportedOperationException("Not implemented");
        }
    
        @Override
        public JSONObject toJson(Block block) {
            var blockImpl = (BlockImpl) block;
            JSONObject result = new JSONObject();
            result.put(ID, blockImpl.getId());
            try {
                result.put(HASH, blockImpl.getHash().toHexString());
            } catch (JSONException e) {
                e.printStackTrace();
            }
            result.put(DIFFICULTY, blockImpl.getDifficulty());
            result.put(NONCE, blockImpl.getNonce().toHexString());
            result.put(TIMESTAMP, Long.toString(blockImpl.getTimestamp()));
            result.put(MERKLE_ROOT, blockImpl.getMerkleRoot().toHexString());
            result.put(LAST_BLOCK_HASH, blockImpl.getLastBlockHash().toHexString());
            JSONArray transactionsArray = new JSONArray();
            for (Transaction transaction : blockImpl.getTransactions()) {
                transactionsArray.put(transaction.toJson());
            }
            result.put(TRANSACTIONS, transactionsArray);
            return result;
        }
    
        public Block fromJson(JSONObject json) {
            return BlockImpl.builder()
                .id(json.getInt(ID))
                .timestamp(json.getLong(TIMESTAMP))
                .difficulty(json.getInt(DIFFICULTY))
                .merkleRoot(SHA256Hash.of(json.getString(MERKLE_ROOT)))
                .lastBlockHash(SHA256Hash.of(json.getString(LAST_BLOCK_HASH)))
                .nonce(SHA256Hash.of(json.getString(NONCE)))
                .transactions(
                    IntStream.range(0, json.getJSONArray(TRANSACTIONS).length())
                        .mapToObj(i -> Transaction.of(json.getJSONArray(TRANSACTIONS).getJSONObject(i)))
                        .collect(Collectors.toList())
                )
                .build();
        }
    }
}
