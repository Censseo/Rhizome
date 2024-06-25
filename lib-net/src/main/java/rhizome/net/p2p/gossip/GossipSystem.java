package rhizome.net.p2p.gossip;

import static java.nio.charset.StandardCharsets.UTF_8;

import java.util.List;
import java.util.Map;
import java.net.InetSocketAddress;

// import com.dslplatform.json.DslJson;

import io.activej.http.AsyncHttpClient;
import io.activej.http.HttpRequest;
import io.activej.promise.Promise;
import lombok.Builder;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import rhizome.net.p2p.DiscoveryService;
import rhizome.net.p2p.PeerState;
import rhizome.net.p2p.PeerSystem;
import rhizome.net.p2p.peer.Peer;
import rhizome.net.p2p.peer.EventListener;
import rhizome.net.protocol.Message;

import static io.activej.common.Checks.checkState;

@Builder @Getter @Slf4j
public class GossipSystem implements PeerSystem {

    private final Peer localhostPeer;
    private final AsyncHttpClient httpClient;
    // @Builder.Default private DslJson<Object> dslJson = new DslJson<>();

    // private RpcServer peerServer;

    // private final Set<Peer> alivePeers = new HashSet<>();
    // private final Set<Peer> deadPeers = new HashSet<>();
    // private final Queue<Peer> candidatePeers = new ConcurrentLinkedQueue<>();
    // private final Queue<Peer> endpointMembers = new ConcurrentLinkedQueue<>();

    // private final Map<Class<?>, RpcRequestHandler<?, ?>> handlers = new HashMap<>();
    private final EventListener listener;

    public void start() {
        // checkState(handlers != null && !handlers.isEmpty(), "Handlers must be set before starting the gossip system");
        checkState(listener != null, "Listener must be set before starting the gossip system");
        log.debug("|Gossip system starting for cluster[%s] ip[%s] port[%d] id[%s]|",
                localhostPeer.address().getHostName(), localhostPeer.address().getPort(),
                localhostPeer.id());

        notify(localhostPeer, PeerState.JOIN, null);
    }

    public void notify(Peer member, PeerState state, Object payload) {
        if (state == PeerState.RECEIVE) {
            listener.fire(member, state, payload)
                .whenComplete((result, e) -> {
                    if (e != null) {
                        log.error("Error notifying listener", e);
                    }
                });
        } else {
            listener.fire(member, state, payload);
        }
    }

    // @Override
    // public Promise<List<InetSocketAddress>> getPeers(Peer peer) {
    //     return httpClient.request(HttpRequest.get(peer.address() + "/peers"))
    //             .then(response -> response.loadBody())
    //             .map(body -> {
    //                 var peerBytes = body.getString(UTF_8).getBytes();
    //                 return dslJson.deserializeList(String.class, peerBytes, peerBytes.length).stream()
    //                         .map(address -> new InetSocketAddress(address.split(":")[0], Integer.parseInt(address.split(":")[1])))
    //                         .toList();
    //             });
    // }

    @Override
    public DiscoveryService getDiscoveryService(Map<Object, Peer> peers, PeerSystem peerSystem) {
        return FloodDiscovery.builder()
                .peerSystem(peerSystem)
                .build();
    }

    @Override
    public String cluster() {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'cluster'");
    }

    @Override
    public void broadcast(Map<Object, Peer> alivePeers, Message message) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'broadcast'");
    }

    @Override
    public Promise<List<InetSocketAddress>> getPeers(Peer peer) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'getPeers'");
    }
}
