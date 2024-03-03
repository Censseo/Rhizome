package rhizome.net.p2p.peer;

import java.net.InetSocketAddress;
import java.util.Map;
import java.util.List;
import java.util.UUID;

import ch.qos.logback.core.model.PropertyModel;
import io.activej.async.function.AsyncConsumer;
import io.activej.async.function.AsyncFunction;
import io.activej.promise.Promise;
import rhizome.net.p2p.gossip.DiscoveryPeer;

public interface Peer {
    
    /**
     * 
     * @param cluster
     * @param address
     * @return
     */
    public static Peer initLocalPeer(String cluster, InetSocketAddress address) {
        return new DiscoveryPeer(cluster, UUID.randomUUID(), address, PeerState.JOIN, System.currentTimeMillis() / 1000, 0, 0);
    }

    /**
     * 
     * @param cluster
     * @param address
     * @return
     */
    public static Peer fromAddress(String cluster, InetSocketAddress address) {
        return new DiscoveryPeer(cluster, UUID.randomUUID(), address, PeerState.DISCONNECTED, System.currentTimeMillis() / 1000, 0, 0);
    }

    /**
     * 
     * @param startRequestTime
     * @return
     */
    public Peer refresh(long startRequestTime);

    // /**
    //  * 
    //  * @param consumer
    //  * @return
    //  */
    // public default Promise<Void> ping(AsyncConsumer<Peer> consumer) {
	// 	return consumer.accept(this);
	// }

    public default Promise<Void> ping() {
        return Promise.ofCallback(cb -> getPeerChannel().getOutput().ping());
    }

    public PeerChannel getPeerChannel();

    // public default Promise<List<Peer>> discover(AsyncFunction<Peer, List<Peer>> function) {
    //     return function.apply(this);
    // }

    // public default Promise<PeerChannel> connect() {
    //     return PeerChannel.connect(this);
    // }

    InetSocketAddress address();
    String cluster();
    UUID id();
}
