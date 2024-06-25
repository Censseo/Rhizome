package rhizome.services.network;

import org.jetbrains.annotations.NotNull;

import io.activej.eventloop.Eventloop;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import rhizome.net.p2p.DiscoveryService;
import rhizome.net.protocol.Message;

/**
 * This service execute the service manager process.
 * It pings all peers and marks them as dead or alive accordingly.
 * It also pings all dead peers and marks them as alive if they respond.
 * It also rediscover the peers.
 */
@Slf4j @Getter
public class PeerManagerServiceImpl extends AbstractPeerManagerService {

    /**
     * Private constructor
     * 
     * @param eventloop
     * @param discoveryService
     */
    PeerManagerServiceImpl(Eventloop eventloop, DiscoveryService discoveryService) {
        super(eventloop, discoveryService);
    }

    /**
     * Publishes a message to connected peers following the strategy of the peer
     * system
     * 
     * @param message
     */
    public void broadcast(Message message) {
        log.info("Broadcasting message: {}", message);
        // peerSystem.broadcast(connectedPeersView, message);
    }

    public void sync() {
        // peerSystem.broadcast(connectedPeersView, message);
    }

    @Override
    public @NotNull Eventloop getEventloop() {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'getEventloop'");
    }
}
