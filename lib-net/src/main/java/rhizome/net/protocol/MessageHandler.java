package rhizome.net.protocol;

import io.activej.rpc.server.RpcRequestHandler;

import java.util.Map;

import static java.util.Map.entry;
import static java.util.Map.ofEntries;
import static rhizome.net.protocol.MessageCode.*;

public class MessageHandler {

    private MessageHandler() {}

    public static final Map<MessageCode, RpcRequestHandler<?,?>> handlers = ofEntries(
        entry(CONNECT, handleConnect()),
        entry(DISCONNECT, handleDisconnect()),
        entry(ERROR, handleError()),
        entry(BLOCKS_REQUEST, handleBlocksRequest()),
        entry(BLOCKS_RESPONSE, handleBlocksResponse()),
        entry(NEW_BLOCK, handleNewBlock()),
        entry(SYNC_BLOCK, handleSyncBlock()),
        entry(SYNCBLOCK_REQUEST, handleSyncBlockRequest()),
        entry(TRANSACTION_NEW, handleTransactionNewRequest()),
        entry(CLOSE, handleClose())
    );

    public static RpcRequestHandler<?,?> getHandler(MessageCode messageType) {
        return handlers.get(messageType);
    }
    
    public static RpcRequestHandler<?,?> handleTransactionNewRequest() {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'handleTransactionNewRequest'");
    }

    public static RpcRequestHandler<?,?> handleConnect() {
        return data -> null;
    }

    public static RpcRequestHandler<?,?> handleDisconnect() {
        return data -> null;
    }

    public static RpcRequestHandler<?,?> handleError() {
        return data -> null;
    }

    public static RpcRequestHandler<?,?> handleBlocksRequest() {
        return data -> null;
    }

    public static RpcRequestHandler<?,?> handleBlocksResponse() {
        return data -> null;
    }

    public static RpcRequestHandler<?,?> handleNewBlock() {
        return data -> null;
    }

    public static RpcRequestHandler<?,?> handleSyncBlock() {
        return data -> null;
    }

    public static RpcRequestHandler<?,?> handleSyncBlockRequest() {
        return data -> null;
    }

    public static RpcRequestHandler<?,?> handleClose() {
        return data -> null;
    }
}
