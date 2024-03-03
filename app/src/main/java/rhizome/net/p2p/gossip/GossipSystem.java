package rhizome.net.p2p.gossip;

import static java.nio.charset.StandardCharsets.UTF_8;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.net.InetSocketAddress;

import com.dslplatform.json.DslJson;

import io.activej.async.function.AsyncConsumer;
import io.activej.http.AsyncHttpClient;
import io.activej.http.HttpRequest;
import io.activej.promise.Promise;
import io.activej.rpc.server.RpcRequestHandler;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import rhizome.net.p2p.PeerSystem;
import rhizome.net.p2p.peer.Peer;
import rhizome.net.p2p.peer.EventListener;
import rhizome.net.p2p.peer.PeerState;
import rhizome.services.network.PeerManager;

import static io.activej.common.Checks.checkState;

@Builder
@Getter
@Setter
@Slf4j
public class GossipSystem implements PeerSystem {

    private PeerManager peerManager;
    private Peer localhostPeer;


    private AsyncHttpClient httpClient;
    private DslJson<Object> dslJson = new DslJson<>();

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
                localhostPeer.cluster(), localhostPeer.address().getHostName(), localhostPeer.address().getPort(),
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
    // public AsyncConsumer<Peer> ping() {
    //     // TODO Auto-generated method stub
    //     throw new UnsupportedOperationException("Unimplemented method 'ping'");
    // }

    @Override
    public Promise<List<InetSocketAddress>> getPeers(Peer peer) {
        return httpClient.request(HttpRequest.get(peer.address() + "/peers"))
                .then(response -> response.loadBody())
                .map(body -> {
                    var peerBytes = body.getString(UTF_8).getBytes();
                    return dslJson.deserializeList(String.class, peerBytes, peerBytes.length).stream()
                            .map(address -> {
                                var ip = address.split(":")[0];
                                int port = Integer.parseInt(address.split(":")[1]);
                                return new InetSocketAddress(ip, port);
                            }).toList();
                });
    }
}
