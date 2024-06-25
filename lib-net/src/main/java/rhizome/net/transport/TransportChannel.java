package rhizome.net.transport;

import io.activej.promise.Promise;
import rhizome.net.p2p.peer.Protocol;
import rhizome.net.protocol.Message;

public interface TransportChannel {
    
    // Send a message to the peer
    public Promise<?> send(Message message);

    // Receive a message from the peer
    public void receive(Message message);

    // Close the connection to the peer
    public void close();

    // Get the output channel
    public ChannelOutput getOutput();

    // Get the input channel
    public ChannelInput getInput();

    // Get the protocol used to communicate with the peer
    public Protocol getProtocol();

    // Get the stats of the current peer connection
    public ChannelStats getStats();
}
