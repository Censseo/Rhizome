package rhizome.net.transport;

import java.net.InetSocketAddress;
import java.util.HashMap;
import java.util.Map;
// import java.util.EnumMap;

import lombok.extern.slf4j.Slf4j;
// import rhizome.net.p2p.peer.Protocol;
// import rhizome.net.protocol.Message;
// import rhizome.net.protocol.MessageCode;
// import rhizome.net.protocol.MessageHandler;

@Slf4j
public final class TransportChannels {

    private static final Map<InetSocketAddress, TransportChannel> channels = new HashMap<>();
    // private static final EnumMap<MessageCode, MessageHandler> messageHandlers = new EnumMap<>(MessageCode.class);

    private TransportChannels() {}

    public static TransportChannel create(InetSocketAddress address) {
        log.info("Connecting to addresse: {}", address);

        TransportChannel channel = channels.get(address);
        if (channel == null) {
            // channel = create(address, );
            // channels.put(address, channel);
        }
        return channel;
    }

    // public static void registerMessageHandler(MessageCode code, MessageHandler handler) {
    //     messageHandlers.put(code, handler);
    // }
}
