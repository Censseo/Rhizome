package rhizome.net;

import io.activej.promise.Promise;
import io.activej.promise.Promises;
import java.net.InetSocketAddress;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class GossipProtocolTest {

    public static class Node {
        private final InetSocketAddress address;
        private final Map<InetSocketAddress, Node> peers;
        private final Set<String> receivedMessages;

        public Node(InetSocketAddress address) {
            this.address = address;
            this.peers = new ConcurrentHashMap<>();
            this.receivedMessages = ConcurrentHashMap.newKeySet();
        }

        public void addPeer(Node peer) {
            peers.put(peer.address, peer);
        }

        // public void receiveMessage(GossipMessage message) {
        //     if (receivedMessages.add(message.getContent())) {
        //         System.out.println("Node " + address + " received message: " + message.getContent());
        //         propagateMessage(message);
        //     }
        // }

        // private void propagateMessage(GossipMessage message) {
        //     peers.values().forEach(peer -> {
        //         Promise.ofCallback(cb -> peer.receiveMessageAsync(message, cb));
        //     });
        // }

        // private void receiveMessageAsync(GossipMessage message, Promise.CompleteCallback<Void> cb) {
        //     Promises.delay(1000) // Simulate network delay
        //             .thenRun(() -> {
        //                 receiveMessage(message);
        //                 cb.setComplete();
        //             });
        // }
    }

    public static class GossipMessage {
        private final String content;

        public GossipMessage(String content) {
            this.content = content;
        }

        public String getContent() {
            return content;
        }
    }

    public static class GossipManager {
        private final Set<Node> nodes;

        public GossipManager() {
            this.nodes = new HashSet<>();
        }

        public void addNode(Node node) {
            nodes.add(node);
            nodes.forEach(n -> {
                if (!n.equals(node)) {
                    n.addPeer(node);
                    node.addPeer(n);
                }
            });
        }

        public void spreadGossip(String messageContent) {
            GossipMessage message = new GossipMessage(messageContent);
            // nodes.forEach(node -> node.receiveMessage(message));
        }
    }

    public static void main(String[] args) {
        GossipManager manager = new GossipManager();
        
        // Example of creating nodes and spreading a message
        Node node1 = new Node(new InetSocketAddress("localhost", 8001));
        Node node2 = new Node(new InetSocketAddress("localhost", 8002));
        Node node3 = new Node(new InetSocketAddress("localhost", 8003));

        manager.addNode(node1);
        manager.addNode(node2);
        manager.addNode(node3);

        manager.spreadGossip("Hello Gossip World!");
    }
}
