package rhizome.core.block;

import static rhizome.core.common.Constants.*;
import static rhizome.core.common.Crypto.verifyHash;

import java.nio.ByteBuffer;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.List;
import java.util.ArrayList;
import java.util.Objects;

import org.json.JSONObject;

import lombok.Builder;
import lombok.Data;
import rhizome.core.block.dto.BlockDto;
import rhizome.core.common.Constants;
import rhizome.core.crypto.SHA256Hash;
import rhizome.core.transaction.Transaction;

@Data @Builder
public final class BlockImpl implements Block {

    /**
     * Variable Definitions
     */
    @Builder.Default
    private int id = 1;

    @Builder.Default
    private long timestamp = System.currentTimeMillis();

    @Builder.Default
    private int difficulty = MIN_DIFFICULTY;

    @Builder.Default
    private List<Transaction> transactions = new ArrayList<>();

    @Builder.Default
    private SHA256Hash merkleRoot = SHA256Hash.empty();

    @Builder.Default
    private SHA256Hash lastBlockHash = SHA256Hash.empty();

    @Builder.Default
    private SHA256Hash nonce = SHA256Hash.empty();

    /**
     * Serialization
     */
    public BlockDto serialize() {
       return serialize(this);
    }

    public JSONObject toJson() {
        return toJson(this);
    }

    /**
     * Cryptographic Hashing
     * @return
     * @throws NoSuchAlgorithmException
     */
    public SHA256Hash hash() {
        try {
            MessageDigest sha256 = MessageDigest.getInstance("SHA-256");

            sha256.update(merkleRoot.hash().getArray());
            sha256.update(lastBlockHash.hash().getArray());
            
            // Convert int and long to byte arrays and update hash
            ByteBuffer buffer = ByteBuffer.allocate(Integer.BYTES + Long.BYTES);
            buffer.putInt(difficulty);
            buffer.putLong(timestamp);
            sha256.update(buffer.array());

            return SHA256Hash.of(sha256.digest());
        } catch (NoSuchAlgorithmException e) {
            throw new BlockException("SHA-256 algorithm not found", e);
        }
    }

    public boolean verifyNonce() {
        boolean usePufferfish = id() > Constants.PUFFERFISH_START_BLOCK;
        return verifyHash(hash(), nonce, difficulty, usePufferfish, true);
    }

    /**
     * Utils
     */
    public void addTransaction(Transaction t) {
        transactions.add(t);
    }
    
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        BlockImpl block = (BlockImpl) o;
        return id == block.id && difficulty == block.difficulty && timestamp == block.timestamp &&
            nonce.equals(block.nonce) && merkleRoot.equals(block.merkleRoot) &&
            lastBlockHash.equals(block.lastBlockHash) && transactions.equals(block.transactions);
    }

    @Override
    public int hashCode() {
        return Objects.hash(nonce, id, difficulty, timestamp, merkleRoot, lastBlockHash, transactions);
    }
}
