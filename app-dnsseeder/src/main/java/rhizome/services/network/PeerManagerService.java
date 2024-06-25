package rhizome.services.network;

import io.activej.eventloop.Eventloop;
import rhizome.net.p2p.DiscoveryService;
import rhizome.net.protocol.Message;

/**
 * This service execute the service manager process.
 * It pings all peers and marks them as dead or alive accordingly.
 * It also pings all dead peers and marks them as alive if they respond.
 * It also rediscover the peers.
 */
public interface PeerManagerService {

    static final int PING_INTERVAL = 10;

    /**
     * Factory method
     * 
     * @param eventloop
     * @param discoveryService
     * @return new instance of PeerManagerService
     */
    static PeerManagerService create(Eventloop eventloop, DiscoveryService discoveryService) {
        return new PeerManagerServiceImpl(eventloop, discoveryService);
    }

    /**
     * Publishes a message to connected peers following the strategy of the peer
     * system
     * 
     * @param message
     */
    void broadcast(Message message);


    /*
     * 
     * 
     */
    void sync();
}
