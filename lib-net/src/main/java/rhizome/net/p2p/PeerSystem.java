package rhizome.net.p2p;
import java.util.List;
import java.util.Map;
import java.net.InetSocketAddress;

import io.activej.promise.Promise;
import rhizome.net.p2p.peer.Peer;
import rhizome.net.protocol.Message;

public interface PeerSystem {
    DiscoveryService getDiscoveryService(Map<Object, Peer> peers, PeerSystem peerSystem);

    Promise<List<InetSocketAddress>> getPeers(Peer peer);

    String cluster();

    void broadcast(Map<Object, Peer> alivePeers, Message message);
}
