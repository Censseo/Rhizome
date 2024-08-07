package rhizome.net.service;

import java.time.Duration;
import java.util.Map;
import java.util.HashMap;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import io.activej.async.function.AsyncRunnable;
import io.activej.async.function.AsyncRunnables;
import io.activej.async.service.ReactiveService;
import io.activej.promise.Promise;
import io.activej.promise.Promises;
import io.activej.reactor.AbstractReactive;
import io.activej.reactor.Reactor;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import rhizome.net.p2p.DiscoveryService;
import rhizome.net.p2p.peer.Peer;
import rhizome.services.network.PeerManagerService;

import static java.util.Collections.unmodifiableMap;
import static java.util.Map.Entry;
import static io.activej.async.util.LogUtils.toLogger;

/**
 * This service execute the service manager process.
 * It pings all peers and marks them as dead or alive accordingly.
 * It also pings all dead peers and marks them as alive if they respond.
 * It also rediscover the peers.
 */
@Slf4j @Getter
abstract class AbstractPeerManagerService extends AbstractReactive implements PeerManagerService, ReactiveService {

    protected final DiscoveryService discoveryService;

    private final AsyncRunnable checkAllPeers = AsyncRunnables.reuse(this::doCheckAllPeers);
    private final AsyncRunnable checkDeadPeers = AsyncRunnables.reuse(this::doCheckDeadPeers);

    private final Map<Object, Peer> peers = new HashMap<>();
    private final Map<Object, Peer> peersView = unmodifiableMap(peers);

    private final Map<Object, Peer> alivePeers = new HashMap<>();
    private final Map<Object, Peer> alivePeersView = unmodifiableMap(alivePeers);

    private final Map<Object, Peer> deadPeers = new HashMap<>();
    private final Map<Object, Peer> deadPeersView = unmodifiableMap(deadPeers);

    private final Map<Object, Peer> connectedPeers = new HashMap<>();
    private final Map<Object, Peer> connectedPeersView = unmodifiableMap(connectedPeers);

    /**
     * Private constructor
     * 
     * @param eventloop
     * @param discoveryService
     */
    protected AbstractPeerManagerService(Reactor eventloop, DiscoveryService discoveryService) {
        super(eventloop);
        this.discoveryService = discoveryService;
    }

    /**
     * Starts the service
     * 
     * @return promise of the start
     */
    @Override
    public @NotNull Promise<?> start() {
        log.info("|PEER MANAGER SERVICE CLIENT STARTING|");

        // Start the discovery process
        return Promise.ofCallback(cb -> discoveryService.discover(null, (result, e) -> {
            if (e == null) {
                peers.putAll(result);
                alivePeers.putAll(result);
                checkAllPeers().call(cb);
            } else {
                cb.setException(e);
            }
        }))
                .whenResult(this::rediscover);
    }

    @Override
    public @NotNull Promise<?> stop() {
        return Promise.complete().whenResult(() -> log.info("|PEER MANAGER SERVICE CLIENT STOPPED|"));
    }


    /**
     * Starts a check process, which pings all peers and marks them as dead or alive
     * accordingly
     *
     * @return promise of the check
     */
    public Promise<Void> checkAllPeers() {
        return checkAllPeers.run().whenComplete(toLogger(log, "checkAllPeers"));
    }

    /**
     * Relanches the discovery process
     */
    private void rediscover() {
        discoveryService.discover(peers, (result, e) -> {
            if (e == null) {
                updatePeers(result);
                checkAllPeers().whenResult(this::rediscover);
            } else {
                log.warn("Could not discover peers", e);
                getReactor().delayBackground(Duration.ofSeconds(PING_INTERVAL), this::rediscover);
            }
        });
    }

    /**
     * Starts a check process, which pings all dead peers and marks them as alive if
     * they respond
     *
     * @return promise of the check
     */
    private void updatePeers(Map<Object, Peer> newPeers) {
        peers.clear();
        peers.putAll(newPeers);

        alivePeers.keySet().retainAll(peers.keySet());
        deadPeers.keySet().retainAll(peers.keySet());

        for (Entry<Object, Peer> entry : peers.entrySet()) {
            Object peerId = entry.getKey();
            Peer peer = entry.getValue();

            Peer deadPeer = deadPeers.get(peerId);
            if (deadPeer != null) {
                if (deadPeer == peer)
                    continue;

                deadPeers.remove(peerId);
            }
            alivePeers.put(peerId, peer);
        }

        alivePeers.clear();
        deadPeers.clear();
    }

    /**
     * Starts a check process, which pings all peers and marks them as alive if they
     * respond
     * 
     * @return
     */
    private Promise<Void> doCheckAllPeers() {
        return Promises.all(
                peers.entrySet().stream()
                        .map(entry -> {
                            Object id = entry.getKey();
                            return entry.getValue()
                                    .ping()
                                    .map((o, e) -> {
                                        if (e == null) {
                                            markAlive(id);
                                        } else {
                                            markDead(id, e);
                                        }
                                        return null;
                                    });
                        }));
    }

    /**
     * Starts a check process, which pings all dead peers and marks them as alive if
     * they respond
     * 
     * @return
     */
    private Promise<Void> doCheckDeadPeers() {
        return Promises.all(
                deadPeers.entrySet().stream()
                        .map(entry -> entry.getValue()
                                .ping()
                                .map((o, e) -> {
                                    if (e == null) {
                                        markAlive(entry.getKey());
                                    }
                                    return null;
                                })));
    }

    /**
     * Mark a peer as dead. It means that no operations will use it, and it would
     * not be given to the server selector.
     * Next call to {@link #checkDeadPeers()} or {@link #checkAllPeers()} will ping
     * this peer and possibly
     * mark it as alive again.
     *
     * @param peerId id of the peer to be marked
     * @param e      optional exception for logging
     * @return <code>true</code> if peer was alive and <code>false</code> otherwise
     */
    @SuppressWarnings("UnusedReturnValue")
    private boolean markDead(Object peerId, @Nullable Exception e) {
        Peer peer = alivePeers.remove(peerId);
        if (peer != null) {
            log.warn("Marking peer {} as dead ", peerId, e);
            deadPeers.put(peerId, peer);
            return true;
        }
        return false;
    }

    private void markAlive(Object peerId) {
        Peer peer = deadPeers.remove(peerId);
        if (peer != null) {
            log.info("Peer {} is alive again!", peerId);
            alivePeers.put(peerId, peer);
        }
    }
}
