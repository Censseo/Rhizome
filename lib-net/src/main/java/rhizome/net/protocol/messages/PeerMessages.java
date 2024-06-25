package rhizome.net.protocol.messages;

import java.util.Map;

import io.activej.rpc.protocol.RpcMandatoryData;
import io.activej.serializer.annotations.Deserialize;
import io.activej.serializer.annotations.Serialize;
import rhizome.net.protocol.MessageCode;

import static java.util.Map.entry;
import static java.util.Map.ofEntries;
import static java.util.Collections.unmodifiableMap;

import static rhizome.net.protocol.MessageCode.*;

// WIP - dummy classes
public class PeerMessages {

    private PeerMessages() {}

    public static final Map<MessageCode, Class<?>> MESSAGE_TYPES = unmodifiableMap(
        ofEntries(
            entry(CONNECT, Connect.class),
            entry(DISCONNECT, Disconnect.class),
            entry(ERROR, Error.class),
            entry(BLOCKS_REQUEST, BlocksRequest.class),
            entry(BLOCKS_RESPONSE, BlocksResponse.class),
            entry(NEW_BLOCK, NewBlock.class),
            entry(SYNC_BLOCK, SyncBlock.class),
            entry(SYNCBLOCK_REQUEST, SyncBlockRequest.class),
            entry(TRANSACTION_NEW, TransactionNew.class),
            entry(PEER_LIST_REQUEST, PeerListRequest.class),
            entry(PEER_LIST_RESPONSE, PeerListResponse.class),
            entry(CLOSE, Close.class)
        )
    );

    public static final class Connect implements RpcMandatoryData {
        @Serialize public final String key;

        public Connect(@Deserialize("key") String key) {
            if (key == null || key.isEmpty()) {
                throw new IllegalArgumentException("Key cannot be null or empty");
            }
            this.key = key;
        }
    }

    public static final class Disconnect implements RpcMandatoryData {
        @Serialize public final String key;

        public Disconnect(@Deserialize("key") String key) {
            if (key == null || key.isEmpty()) {
                throw new IllegalArgumentException("Key cannot be null or empty");
            }
            this.key = key;
        }
    }

    public static final class Error implements RpcMandatoryData {
        @Serialize public final String key;
        @Serialize public final String message;

        public Error(@Deserialize("key") String key, @Deserialize("message") String message) {
            if (key == null || key.isEmpty()) {
                throw new IllegalArgumentException("Key cannot be null or empty");
            }
            if (message == null || message.isEmpty()) {
                throw new IllegalArgumentException("Message cannot be null or empty");
            }
            this.key = key;
            this.message = message;
        }
    }

    public static final class BlocksRequest implements RpcMandatoryData {
        @Serialize public final String key;
        @Serialize public final long start;
        @Serialize public final long end;

        public BlocksRequest(@Deserialize("key") String key, @Deserialize("start") long start, @Deserialize("end") long end) {
            if (key == null || key.isEmpty()) {
                throw new IllegalArgumentException("Key cannot be null or empty");
            }
            if (start < 0) {
                throw new IllegalArgumentException("Start cannot be negative");
            }
            if (end < 0) {
                throw new IllegalArgumentException("End cannot be negative");
            }
            this.key = key;
            this.start = start;
            this.end = end;
        }
    }

    public static final class BlocksResponse implements RpcMandatoryData {
        @Serialize public final String key;
        @Serialize public final String blocks;

        public BlocksResponse(@Deserialize("key") String key, @Deserialize("blocks") String blocks) {
            if (key == null || key.isEmpty()) {
                throw new IllegalArgumentException("Key cannot be null or empty");
            }
            if (blocks == null || blocks.isEmpty()) {
                throw new IllegalArgumentException("Blocks cannot be null or empty");
            }
            this.key = key;
            this.blocks = blocks;
        }
    }

    public static final class NewBlock implements RpcMandatoryData {
        @Serialize public final String key;
        @Serialize public final String block;

        public NewBlock(@Deserialize("key") String key, @Deserialize("block") String block) {
            if (key == null || key.isEmpty()) {
                throw new IllegalArgumentException("Key cannot be null or empty");
            }
            if (block == null || block.isEmpty()) {
                throw new IllegalArgumentException("Block cannot be null or empty");
            }
            this.key = key;
            this.block = block;
        }
    }

    public static final class SyncBlock implements RpcMandatoryData {
        @Serialize public final String key;
        @Serialize public final String block;

        public SyncBlock(@Deserialize("key") String key, @Deserialize("block") String block) {
            if (key == null || key.isEmpty()) {
                throw new IllegalArgumentException("Key cannot be null or empty");
            }
            if (block == null || block.isEmpty()) {
                throw new IllegalArgumentException("Block cannot be null or empty");
            }
            this.key = key;
            this.block = block;
        }
    }

    public static final class SyncBlockRequest implements RpcMandatoryData {
        @Serialize public final String key;
        @Serialize public final long index;

        public SyncBlockRequest(@Deserialize("key") String key, @Deserialize("index") long index) {
            if (key == null || key.isEmpty()) {
                throw new IllegalArgumentException("Key cannot be null or empty");
            }
            if (index < 0) {
                throw new IllegalArgumentException("Index cannot be negative");
            }
            this.key = key;
            this.index = index;
        }
    }

    public static final class TransactionNew implements RpcMandatoryData {
        @Serialize public final String key;
        @Serialize public final String transaction;

        public TransactionNew(@Deserialize("key") String key, @Deserialize("transaction") String transaction) {
            if (key == null || key.isEmpty()) {
                throw new IllegalArgumentException("Key cannot be null or empty");
            }
            if (transaction == null || transaction.isEmpty()) {
                throw new IllegalArgumentException("Transaction cannot be null or empty");
            }
            this.key = key;
            this.transaction = transaction;
        }
    }

    public static final class PeerListRequest implements RpcMandatoryData {
        @Serialize public final String key;

        public PeerListRequest(@Deserialize("key") String key) {
            if (key == null || key.isEmpty()) {
                throw new IllegalArgumentException("Key cannot be null or empty");
            }
            this.key = key;
        }
    }

    public static final class PeerListResponse implements RpcMandatoryData {
        @Serialize public final String key;
        @Serialize public final String peers;

        public PeerListResponse(@Deserialize("key") String key, @Deserialize("peers") String peers) {
            if (key == null || key.isEmpty()) {
                throw new IllegalArgumentException("Key cannot be null or empty");
            }
            if (peers == null || peers.isEmpty()) {
                throw new IllegalArgumentException("Peers cannot be null or empty");
            }
            this.key = key;
            this.peers = peers;
        }
    }

    public static final class Close implements RpcMandatoryData {
        @Serialize public final String key;

        public Close(@Deserialize("key") String key) {
            if (key == null || key.isEmpty()) {
                throw new IllegalArgumentException("Key cannot be null or empty");
            }
            this.key = key;
        }
    }
}
