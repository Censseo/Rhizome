package rhizome.core.block.dto;

import org.jetbrains.annotations.NotNull;

import io.activej.serializer.annotations.Deserialize;
import io.activej.serializer.annotations.Serialize;
import lombok.Getter;
import rhizome.core.crypto.SHA256Hash;
import rhizome.core.serialization.BinarySerializable;

@Getter
public class BlockDto implements BinarySerializable {
    @Serialize(order = 1) public final int id;
    @Serialize(order = 2) public final long timestamp;
    @Serialize(order = 3) public final int difficulty;
    @Serialize(order = 4) public final int numTransactions;
    @Serialize(order = 5) public final SHA256Hash lastBlockHash;
    @Serialize(order = 6) public final SHA256Hash merkleRoot;
    @Serialize(order = 7) public final SHA256Hash nonce;

    public static final int BUFFER_SIZE = 116;

    public BlockDto(
        @Deserialize("id") int id, 
        @Deserialize("timestamp") long timestamp, 
        @Deserialize("difficulty") int difficulty, 
        @Deserialize("numTransactions") int numTransactions, 
        @Deserialize("lastBlockHash") SHA256Hash lastBlockHash, 
        @Deserialize("merkleRoot") SHA256Hash merkleRoot, 
        @Deserialize("nonce") SHA256Hash nonce) {

        this.id = id;
        this.timestamp = timestamp;
        this.difficulty = difficulty;
        this.numTransactions = numTransactions;
        this.lastBlockHash = lastBlockHash;
        this.merkleRoot = merkleRoot;
        this.nonce = nonce;
    }

    @Override
    public @NotNull int getSize() {
        return BUFFER_SIZE;
    }
}
