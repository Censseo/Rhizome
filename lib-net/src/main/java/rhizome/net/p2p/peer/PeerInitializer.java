package rhizome.net.p2p.peer;

import java.net.InetSocketAddress;
import java.util.UUID;

import rhizome.net.p2p.PeerState;

public class PeerInitializer {
    
    private PeerInitializer() {}


    /**
     * 
     * @param address
     * @return
     */
    public static Peer initLocalPeer(InetSocketAddress address) {
        return Peer.builder()
            .id(UUID.randomUUID())
            .address(address)
            .state(PeerState.JOIN)
            .lastPingTime(System.currentTimeMillis() / 1000)
            .clockDelta(0)
            .version(0)
            .build();
    }

    /**
     * 
     * @param address
     * @return
     */
    public static Peer fromAddress(InetSocketAddress address) {
        return Peer.builder()
            .id(UUID.randomUUID())
            .address(address)
            .state(PeerState.DISCONNECTED)
            .lastPingTime(System.currentTimeMillis() / 1000)
            .clockDelta(0)
            .version(0)
            .build();
    }

}
