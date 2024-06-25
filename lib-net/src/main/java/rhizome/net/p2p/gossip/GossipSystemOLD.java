package rhizome.net.p2p.gossip;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Random;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReentrantLock;

import org.json.JSONArray;

import lombok.extern.slf4j.Slf4j;
import rhizome.core.api.PeerInterface;
import rhizome.core.common.Pair;
import rhizome.core.crypto.SHA256Hash;
import rhizome.net.p2p.peer.PeerOLD;
import rhizome.persistence.BlockPersistence;

@Slf4j
public class GossipSystemOLD {
    private static final int RANDOM_GOOD_HOST_COUNT = 0;
    private static final int TIMEOUT_MS = 0;
    private static final int ADD_PEER_BRANCH_FACTOR = 0;
    private static final int HOST_MIN_FRESHNESS = 0;
    private List<PeerOLD> peers;
    private String networkName;
    private String minHostVersion;
    private List<String> blacklist;
    private List<String> hosts;
    private Map<String, Long> hostPingTimes;
    private Map<String, Long>  peerClockDeltas;
    private List<PeerOLD> currPeers;
    private List<String> whitelist;
    private ReentrantLock lock;
    private List<String> hostSources;
    private String version;

    public GossipSystemOLD() {
        peers = new ArrayList<>();
    }

    public void gossip() {
        if (peers.isEmpty()) {
            throw new IllegalStateException("No peers available for gossiping");
        }

        Random random = new Random();
        int index = random.nextInt(peers.size());
        PeerOLD peer = peers.get(index);

        // Send a message to the selected peer
        sendMessage(peer);
    }

    private void sendMessage(PeerOLD peer) {
        // Implement your logic here for sending a message to a peer
        // For example, you can establish a connection with the peer and send the message over the network
        // You can replace the throw statement with your implementation

        throw new UnsupportedOperationException("Sending messages to peers is not implemented yet");
    }
    
    public void addPeer(PeerOLD peer) {
        // Implement your logic here for adding a peer to the manager
        // For example, you can check if the peer exists in the list and add it if not found
        // You can replace the throw statement with your implementation

        if (!peers.contains(peer)) {
            peers.add(peer);
        }

    }

    public void removePeer(PeerOLD peer) {
        // Implement your logic here for removing a peer from the manager
        // For example, you can check if the peer exists in the list and remove it if found
        // You can replace the throw statement with your implementation

        if (peers.contains(peer)) {
            peers.remove(peer);
        }
    }

    public List<PeerOLD> getPeers() {
        // Implement your logic here for getting the list of peers
        // You can replace the throw statement with your implementation

        return peers;
    }
    
    public void clearPeers() {
        // Implement your logic here for clearing the list of peers
        // You can replace the throw statement with your implementation

        peers.clear();
    }



    public void addPeer(String addr, long time, String version, String network) {
        if (!network.equals(this.networkName)) return;
        if (version.compareTo(this.minHostVersion) < 0) return;

        // check if host is in blacklist
        if (this.blacklist.contains(addr)) return;

        // check if we already have this peer host
        if (this.hosts.contains(addr)) {
            this.hostPingTimes.put(addr, System.currentTimeMillis() / 1000);
            // record how much our system clock differs from theirs:
            this.peerClockDeltas.put(addr, System.currentTimeMillis() / 1000 - time);
            return;
        }

        // check if the host is reachable:
        if (!isJsHost(addr)) {
            Optional<String> peerName = PeerInterface.getName(addr);
            if (peerName.isEmpty())
                return;
        }

        // add to our host list
        if (this.whitelist.isEmpty() || this.whitelist.contains(addr)) {
            log.info("Added new peer: " + addr);
            hosts.add(addr);
        } else {
            return;
        }

        // check if we have less peers than needed, if so add this to our peer list
        if (this.currPeers.size() < RANDOM_GOOD_HOST_COUNT) {
            try {
                lock.lock();
                Map<Long, SHA256Hash> checkpoints = null;
                Map<Long, SHA256Hash> bannedHashes = null;
                this.currPeers.add(PeerOLD.builder()
                                        .host(addr)
                                        .checkPoints(checkpoints)
                                        .bannedHashes(bannedHashes)
                                        .build()
                                    );
            } finally {
                lock.unlock();
            }
        }

        // pick random neighbor hosts and forward the addPeer request to them:
        Set<String> neighbors = sampleFreshHosts(ADD_PEER_BRANCH_FACTOR);
        List<CompletableFuture<Void>> reqs = new ArrayList<>();
        String _version = this.version;
        String networkName = this.networkName;
        for (String neighbor : neighbors) {
            CompletableFuture<Void> future = CompletableFuture.runAsync(() -> {
                if (neighbor.equals(addr)) return;
                try {
                    PeerInterface.pingPeer(neighbor, addr, System.currentTimeMillis() / 1000, _version, networkName);
                } catch (Exception e) {
                    log.info("Could not add peer " + addr + " to " + neighbor);
                }
            });
            reqs.add(future);
        }

        CompletableFuture.allOf(reqs.toArray(new CompletableFuture[0])).join();
    }

    private boolean isJsHost(String addr) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'isJsHost'");
    }

    public Set<String> sampleFreshHosts(int count) {
        List<String> fixedHosts = List.of(
            "http://94.130.69.234:6002",
            "http://88.119.169.111:3000",
            "http://65.108.201.144:3005"
        );

        List<Pair<String, Long>> freshHostsWithHeight = new ArrayList<>();
        for (Map.Entry<String, Long> pair : hostPingTimes.entrySet()) {
            long lastPingAge = System.currentTimeMillis() / 1000L - pair.getValue();
            if (lastPingAge < HOST_MIN_FRESHNESS && !isJsHost(pair.getKey())) {
                Optional<Long> v = PeerInterface.getCurrentBlockCount(pair.getKey());
                v.ifPresent(value -> freshHostsWithHeight.add(new Pair<>(pair.getKey(), value)));
            }
        }

        if (freshHostsWithHeight.isEmpty()) {
            log.debug("HostManager::sampleFreshHosts No fresh hosts found. Falling back to fixed hosts.");
            return new HashSet<>(fixedHosts);
        }

        freshHostsWithHeight.sort((a, b) -> Long.compare(b.getRight(), a.getRight()));

        log.info("HostManager::sampleFreshHosts Top-synced host: {} with block height: {}", 
                    freshHostsWithHeight.get(0).getLeft(), freshHostsWithHeight.get(0).getRight());

        int numToPick = Math.min(count, freshHostsWithHeight.size());
        Set<String> sampledHosts = new HashSet<>();
        for (int i = 0; i < numToPick; i++) {
            sampledHosts.add(freshHostsWithHeight.get(i).getLeft());
        }

        return sampledHosts;
    }

    public void refreshHostList() {
        if (hostSources.isEmpty()) return;
        
        log.info("Finding peers...");

        Set<String> fullHostList = new HashSet<>();

        // HttpClient
        HttpClient client = HttpClient.newHttpClient();

        // Itérer à travers toutes les sources d'hôtes
        for (String hostUrl : hostSources) {
            try {
                HttpRequest request = HttpRequest.newBuilder()
                        .uri(URI.create(hostUrl))
                        .timeout(Duration.ofMillis(TIMEOUT_MS))
                        .GET()
                        .build();

                HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
                JSONArray hostList = new JSONArray(response.body());
                for (int i = 0; i < hostList.length(); i++) {
                    fullHostList.add(hostList.getString(i));
                }
            } catch (Exception e) {
                continue;
            }
        }

        if (fullHostList.isEmpty()) return;

        ExecutorService executor = Executors.newFixedThreadPool(fullHostList.size());
        for (String host : fullHostList) {
            if (hosts.contains(host) || blacklist.contains(host)) continue;

            executor.execute(() -> {
                try {
                   // TODO: verify if peer is valide
                    boolean condition = true;
                if (condition) {
                        synchronized (this) {
                            hosts.add(host);
                            log.info("[ CONNECTED ] {}", host);
                            hostPingTimes.put(host, System.currentTimeMillis());
                        }
                    } else {
                        log.warn("[ UNREACHABLE ] {}", host);
                    }
                } catch (Exception e) {
                    log.error("Error connecting to host: {}", host, e);
                }
            });
        }
        executor.shutdown();
        try {
            executor.awaitTermination(Long.MAX_VALUE, TimeUnit.NANOSECONDS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    public synchronized void syncHeadersWithPeers() {
        // clear existing peers
        currPeers.clear();
    
        // pick N random peers
        Set<String> hosts = this.sampleFreshHosts(RANDOM_GOOD_HOST_COUNT);
    
        for (String h : hosts) {
            Map<Long, SHA256Hash> checkpoints = null;
            Map<Long, SHA256Hash> bannedHashes = null;
            BlockPersistence blockStore = null;
            currPeers.add(
                PeerOLD.builder()
                .host(h)
                .checkPoints(checkpoints)
                .bannedHashes(bannedHashes)
                .blockStore(blockStore)
                .build());
        }
    }

}
