package rhizome.net;

import io.activej.async.callback.Callback;
import io.activej.promise.Promise;
import rhizome.net.p2p.PeerSystem;
import rhizome.net.p2p.gossip.FloodDiscovery;
import rhizome.net.p2p.peer.Peer;
import rhizome.net.p2p.peer.PeerInitializer;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.net.InetSocketAddress;
import java.util.*;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.*;

class FloodDiscoveryTest {

    private FloodDiscovery floodDiscovery;
    private PeerSystem peerSystem;

    @BeforeEach
    public void setUp() {
        peerSystem = mock(PeerSystem.class);
        floodDiscovery = FloodDiscovery.builder()
                .peerSystem(peerSystem)
                .build();
    }

    @Test
    void testDiscover_Success() {
        // Prepare test data
        Map<Object, Peer> previous = new HashMap<>();
        previous.put("localhost:8001", PeerInitializer.initLocalPeer(new InetSocketAddress("localhost", 8001)));
        Callback<Map<Object, Peer>> callback = mock(Callback.class);
        List<InetSocketAddress> discoveredAddresses = Arrays.asList(
                new InetSocketAddress("localhost", 8001),
                new InetSocketAddress("localhost", 8002)
        );
        Promise<List<InetSocketAddress>> promise = Promise.of(discoveredAddresses);

        // Mock behavior
        when(peerSystem.getPeers(any(Peer.class))).thenReturn(promise);

        // Call the method under test
        floodDiscovery.discover(previous, callback);

        // Verify the behavior
        verify(peerSystem, times(1)).getPeers(any(Peer.class));
        verify(callback, times(1)).accept(anyMap(), isNull());
    }

    @Test
    void testDiscover_Error() {
        // Prepare test data
        Map<Object, Peer> previous = new HashMap<>();
        previous.put("localhost:8001", PeerInitializer.initLocalPeer(new InetSocketAddress("localhost", 8001)));
        Callback<Map<Object, Peer>> callback = mock(Callback.class);
        Exception error = new Exception("Test error");
        Promise<List<InetSocketAddress>> promise = Promise.ofException(error);

        // Mock behavior
        when(peerSystem.getPeers(any(Peer.class))).thenReturn(promise);

        // Call the method under test
        floodDiscovery.discover(previous, callback);

        // Verify the behavior
        verify(peerSystem, times(1)).getPeers(any(Peer.class));
        verify(callback, times(1)).accept(isNull(), eq(error));
    }
}