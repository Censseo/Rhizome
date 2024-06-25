package rhizome.net.p2p;

import io.activej.async.callback.Callback;
import rhizome.net.p2p.peer.Peer;

import org.jetbrains.annotations.Nullable;

import java.util.Map;

/**
 * This interface defines the discovery service.
 */
public interface DiscoveryService {

	/**
	 * This method discovers the peers.
	 * @param previous
	 * @param cb
	 */
    void discover(@Nullable Map<Object, Peer> previous, Callback<Map<Object, Peer>> cb);

	/**
	 * This method creates a discovery service.
	 * @param peers
	 * @param peerSystem
	 * @return
	 */
	static DiscoveryService create(Map<Object, Peer> peers, PeerSystem peerSystem) {
		return peerSystem.getDiscoveryService(peers, peerSystem);
	}
}
