package rhizome.config;

import java.util.Map;
import java.util.HashMap;
import java.net.InetSocketAddress;

import io.activej.config.Config;
import io.activej.config.converter.ConfigConverters;
import io.activej.eventloop.Eventloop;
import io.activej.inject.annotation.Eager;
import io.activej.inject.annotation.Provides;
import io.activej.inject.module.AbstractModule;
import rhizome.net.NetworkUtils;
import rhizome.net.p2p.DiscoveryService;
import rhizome.net.p2p.PeerSystem;
import rhizome.net.p2p.gossip.GossipSystem;
import rhizome.net.p2p.peer.Peer;
import rhizome.net.p2p.peer.PeerInitializer;
import rhizome.services.network.PeerManagerService;

import static io.activej.config.converter.ConfigConverters.ofList;
// import static io.activej.config.converter.ConfigConverters.ofString;

public final class NetworkModule extends AbstractModule {

    public static NetworkModule create() {
        return new NetworkModule();
    }

    @Provides @Eager 
    PeerManagerService peerManagerService(Eventloop eventloop, Map<Object, Peer> seeders, PeerSystem peerSystem) {
        return PeerManagerService.create(eventloop, DiscoveryService.create(seeders, peerSystem));
    }

    @Provides 
    PeerSystem peerSystem(Peer localhostPeer) {
        //config.get(ofString(), "cluster"), 
        return GossipSystem.builder()
                .localhostPeer(localhostPeer)
                .build();
    }
    
    @Provides 
    Map<Object, Peer> seeders(Config config) {
        Map<Object, Peer> peers = new HashMap<>();
        config.get(ofList(ConfigConverters.ofString(), ";"), "seeders").stream()
                .map(ipAddress -> PeerInitializer.fromAddress(new InetSocketAddress(ipAddress, 8080)))
                .forEach(peer -> peers.put(peer.address(), peer));

        config.get(ofList(ConfigConverters.ofString(), ";"), "dns").stream()
                .flatMap(hostname -> NetworkUtils.getIPAddresses(hostname).stream())
                .map(ipAddress -> PeerInitializer.fromAddress(new InetSocketAddress(ipAddress, 8080))) 
                .forEach(peer -> peers.put(peer.address(), peer));
        return peers;
    }

    @Provides
    Peer localhostPeer(Config config) {
        return PeerInitializer.fromAddress(new InetSocketAddress(config.get(ConfigConverters.ofString(), "ip"), 8080));
    }
}
