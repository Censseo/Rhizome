package rhizome.net.p2p.peer;

import io.activej.promise.Promise;
import rhizome.net.p2p.PeerState;

@FunctionalInterface
public interface EventListener {
    Promise<Void> fire(Peer peer, PeerState state, Object payload);
}
