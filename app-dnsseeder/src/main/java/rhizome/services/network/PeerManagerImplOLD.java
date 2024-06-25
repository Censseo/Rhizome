package rhizome.services.network;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URL;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

import org.json.JSONArray;
import org.json.JSONObject;

import lombok.extern.slf4j.Slf4j;
import rhizome.core.api.PeerInterface;
import rhizome.core.common.Constants;
import rhizome.core.common.Pair;
import rhizome.core.crypto.SHA256Hash;
import rhizome.net.p2p.peer.PeerOLD;
import rhizome.persistence.BlockPersistence;

@Slf4j
public class PeerManagerImplOLD {

    private static final long HOST_MIN_FRESHNESS = 180l * 60; // 3 hours
    private static final int RANDOM_GOOD_HOST_COUNT = 0;
    private static final int ADD_PEER_BRANCH_FACTOR = 0;
    private static final long TIMEOUT_MS = 0;
    protected List<PeerOLD> currPeers;
    protected BlockPersistence blockStore;
    protected final Lock lock = new ReentrantLock();
    protected boolean disabled;
    protected boolean firewall;
    protected String ip;
    protected int port;
    protected String name;
    protected String address;
    protected String version;
    protected String minHostVersion;
    protected String networkName;
    protected Map<String, Long> hostPingTimes;
    protected Map<String, Long> peerClockDeltas;
    protected Map<Long, SHA256Hash> checkpoints;
    protected Map<Long, SHA256Hash> bannedHashes;
    protected List<String> hostSources;
    protected List<String> hosts;
    protected Set<String> blacklist;
    protected Set<String> whitelist;
    protected List<Thread> syncThread;
    protected List<Thread> headerStatsThread;

    public PeerManagerImplOLD(JSONObject config) {
        this.name = config.getString("name");
        this.port = config.getInt("port");
        this.ip = config.getString("ip");
        this.firewall = config.getBoolean("firewall");
        this.version = Constants.BUILD_VERSION;
        this.networkName = config.getString("networkName");
        computeAddress();

        // Parse checkpoints
        JSONArray checkpointsArray = config.getJSONArray("checkpoints");
        for (int i = 0; i < checkpointsArray.length(); i++) {
            JSONArray checkpoint = checkpointsArray.getJSONArray(i);
            this.checkpoints.put(checkpoint.getLong(0), SHA256Hash.of(checkpoint.getString(1)));
        }

        // Parse banned hashes
        JSONArray bannedHashesArray = config.getJSONArray("bannedHashes");
        for (int i = 0; i < bannedHashesArray.length(); i++) {
            JSONArray bannedHash = bannedHashesArray.getJSONArray(i);
            this.bannedHashes.put(bannedHash.getLong(0), SHA256Hash.of(bannedHash.getString(1)));
        }

        this.minHostVersion = config.getString("minHostVersion");

        // Check if a blacklist file exists
        try (BufferedReader blacklistReader = new BufferedReader(new FileReader("blacklist.txt"))) {
            String line;
            while ((line = blacklistReader.readLine()) != null) {
                if (line.charAt(0) != '#') {
                    String blocked = line.trim();
                    this.blacklist.add(blocked);
                    log.info("Ignoring host {}", blocked);
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

        // Check if a whitelist file exists
        try (BufferedReader whitelistReader = new BufferedReader(new FileReader("whitelist.txt"))) {
            String line;
            while ((line = whitelistReader.readLine()) != null) {
                if (line.charAt(0) != '#') {
                    String enabled = line.trim();
                    this.whitelist.add(enabled);
                    log.info("Enabling host {}", enabled);
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

        this.disabled = false;
        JSONArray hostSourcesArray = config.getJSONArray("hostSources");
        for (int i = 0; i < hostSourcesArray.length(); i++) {
            this.hostSources.add(hostSourcesArray.getString(i));
        }

        if (this.hostSources.isEmpty()) {
            String localhost = "http://localhost:3000";
            this.hosts.add(localhost);
            this.hostPingTimes.put(localhost, System.currentTimeMillis() / 1000);
            this.peerClockDeltas.put(localhost, 0l);
            syncHeadersWithPeers();
        } else {
            refreshHostList();
        }

        // Start thread to print header chain stats
        boolean showHeaderStats = config.getBoolean("showHeaderStats");
        if (showHeaderStats) {
            this.headerStatsThread.add(new Thread(() -> headerStats()));
        }
    }

    public String computeAddress() {
        if (firewall) {
            return "http://undiscoverable";
        }
        if (ip.isEmpty()) {
            boolean found = false;
            List<String> lookupServices = Arrays.asList(
                "http://checkip.amazonaws.com", 
                "http://icanhazip.com", 
                "http://ifconfig.co", 
                "http://wtfismyip.com/text", 
                "http://ifconfig.io"
            );

            for (String lookupService : lookupServices) {
                try {
                    String rawUrl = fetchUrlContent(lookupService);
                    String ip = rawUrl.trim();
                    if (isValidIPv4(ip)) {
                        address = "http://" + ip + ":" + port;
                        found = true;
                        break;
                    }
                } catch (IOException e) {
                    log.error(null, e);
                }
            }

            if (!found) {
                log.error("Could not determine current IP address");
            }
        } else {
            address = ip + ":" + port;
        }
        return address;
    }

    public void startPeerSync() {
        ExecutorService executor = Executors.newSingleThreadExecutor();
        executor.submit(() -> {
            while (true) {
                try {
                    for (String host : this.hosts) {
                        PeerInterface.pingPeer(host, this.computeAddress(), System.currentTimeMillis() / 1000, this.version, this.networkName);
                    }
                    Thread.sleep(300000);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    break;
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        });
    }

    public void headerStats() {
        Executors.newSingleThreadExecutor().execute(() -> {
            try {
                while (true) {
                    TimeUnit.SECONDS.sleep(30);
                    log.debug("================ Header Sync Status ===============");
                    Map<String, Pair<Long, String>> stats = getHeaderChainStats();
                    for (Map.Entry<String, Pair<Long, String>> entry : stats.entrySet()) {
                        log.debug(String.format("Host: %s blocks: %d, node_ver: %s",
                                entry.getKey(), entry.getValue().getLeft(), entry.getValue().getRight()));
                    }
                    log.debug("===================================================");
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                log.debug("Thread interrupted");
            }
        });
    }

    public synchronized void startPingingPeers() {
        if (!syncThread.isEmpty()) {
            throw new RuntimeException("Peer ping thread exists.");
        }
        syncThread.add(new Thread(() -> startPeerSync()));
    }

    public long getNetworkTimestamp() {
        List<Integer> deltas = new ArrayList<>();
        long currentTime = System.currentTimeMillis() / 1000;
        hostPingTimes.forEach((key, value) -> {
            long lastPingAge = currentTime - value;
            if (lastPingAge < HOST_MIN_FRESHNESS) {
                Integer delta = peerClockDeltas.get(key).intValue();
                if (delta != null) {
                    deltas.add(delta);
                }
            }
        });

        if (deltas.isEmpty()) return currentTime;

        Collections.sort(deltas);

        long medianTime;
        int size = deltas.size();
        if (size % 2 == 0) {
            int avg = (deltas.get(size / 2) + deltas.get(size / 2 - 1)) / 2;
            medianTime = currentTime + avg;
        } else {
            int delta = deltas.get(size / 2);
            medianTime = currentTime + delta;
        }

        return medianTime;
    }

    public String getGoodHost() {
        if (currPeers.isEmpty()) return "";
        BigInteger bestWork = BigInteger.ZERO;
        String bestHost = currPeers.get(0).host();
        lock.lock();
        try {
            for (PeerOLD h : currPeers) {
                if (h.getTotalWork().compareTo(bestWork) > 0) {
                    bestWork = h.getTotalWork();
                    bestHost = h.host();
                }
            }
        } finally {
            lock.unlock();
        }
        return bestHost;
    }

    // Returns number of block headers downloaded by peer host
    public Map<String, Pair<Long, String>> getHeaderChainStats() {
        Map<String, Pair<Long, String>> ret = new HashMap<>();
        for (PeerOLD h : currPeers) {
            ret.put(h.host(), new Pair<>(h.getCurrentDownloaded(), version)); // Remplacez 'version' par la variable/le champ approprié
        }
        return ret;
    }

    // Returns the block count of the highest PoW chain amongst current peers
    public long getBlockCount() {
        if (currPeers.isEmpty()) return 0;
        long bestLength = 0;
        BigInteger bestWork = BigInteger.ZERO;
        lock.lock();
        try {
            for (PeerOLD h : currPeers) {
                if (h.getTotalWork().compareTo(bestWork) > 0) {
                    bestWork = h.getTotalWork();
                    bestLength = h.getChainLength();
                }
            }
        } finally {
            lock.unlock();
        }
        return bestLength;
    }

    public BigInteger getTotalWork() {
        BigInteger bestWork = BigInteger.ZERO;
        lock.lock();
        try {
            if (currPeers.isEmpty()) {
                return bestWork;
            }
            for (PeerOLD h : currPeers) {
                BigInteger peerWork = h.getTotalWork();
                if (peerWork.compareTo(bestWork) > 0) {
                    bestWork = peerWork;
                }
            }
        } finally {
            lock.unlock();
        }
        return bestWork;
    }

    public SHA256Hash getBlockHash(String host, long blockId) {
        SHA256Hash ret = SHA256Hash.empty(); // Assuming NULL_SHA256_HASH is a constant
        lock.lock();
        try {
            for (PeerOLD h : currPeers) {
                if (h.host().equals(host)) {
                    ret = h.getHash(blockId);
                    break;
                }
            }
        } finally {
            lock.unlock();
        }
        return ret;
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

    public Set<String> sampleAllHosts(int count) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'sampleAllHosts'");
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

    public static boolean isValidIPv4(String ip) {
        String ipPattern = "^((25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)\\.){3}(25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)$";
        return ip.matches(ipPattern);
    }

    public static boolean isJsHost(String addr) {
        return addr.contains("peer://");
    }

    private String fetchUrlContent(String urlString) throws IOException {
        URL url = new URL(urlString);
        HttpURLConnection con = (HttpURLConnection) url.openConnection();
        con.setRequestMethod("GET");

        BufferedReader in = new BufferedReader(new InputStreamReader(con.getInputStream()));
        String inputLine;
        StringBuilder content = new StringBuilder();
        while ((inputLine = in.readLine()) != null) {
            content.append(inputLine);
        }
        in.close();
        con.disconnect();

        return content.toString();
    }

    private static Optional<String> extractHostVersion(JSONObject hostInfo) {
        try {
            String version = hostInfo.optString("version");
            return Optional.ofNullable(version.isEmpty() ? null : version);
        } catch (Exception e) {
            return Optional.empty();
        }
    }

    public void setBlockstore(BlockPersistence blockStore) {
        this.blockStore = blockStore;
    }

    public int size() {
        return hosts.size();
    }
    

    public List<String> getHosts(boolean includeSelf) {
        List<String> ret = new ArrayList<>();
        for (Map.Entry<String, Long> pair : hostPingTimes.entrySet()) {
            long lastPingAge = System.currentTimeMillis() - pair.getValue();
            // only return peers that have pinged
            if (lastPingAge < HOST_MIN_FRESHNESS) { 
                ret.add(pair.getKey());
            }
        }
        if (includeSelf) {
            ret.add(address);
        }
        return ret;
    }

    public synchronized void syncHeadersWithPeers() {
        // clear existing peers
        currPeers.clear();
    
        // pick N random peers
        Set<String> hosts = this.sampleFreshHosts(RANDOM_GOOD_HOST_COUNT);
    
        for (String h : hosts) {
            currPeers.add(
                PeerOLD.builder()
                .host(h)
                .checkPoints(checkpoints)
                .bannedHashes(bannedHashes)
                .blockStore(blockStore)
                .build());
        }
    }

    public void onNewPeerDiscovered(Object peer) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'onNewPeerDiscovered'");
    }
    
    
}
