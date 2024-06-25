package rhizome.net.transport.rpc;

import io.activej.promise.Promise;
import rhizome.net.p2p.peer.Protocol;
import rhizome.net.protocol.Message;
import rhizome.net.transport.ChannelInput;
import rhizome.net.transport.ChannelOutput;
import rhizome.net.transport.ChannelStats;
import rhizome.net.transport.TransportChannel;
import rhizome.net.transport.rpc.client.RpcClient;
import rhizome.net.transport.rpc.server.RpcServer;

public class RpcTransportChannel implements TransportChannel {

    RpcClient client;
    RpcServer server;

    @Override
    public Promise<?> send(Message message) {
        return client.sendRequest(message);
    }

    @Override
    public void receive(Message message) {
        
    }

    @Override
    public void close() {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'close'");
    }

    @Override
    public ChannelOutput getOutput() {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'getOutput'");
    }

    @Override
    public ChannelInput getInput() {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'getInput'");
    }

    @Override
    public Protocol getProtocol() {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'getProtocol'");
    }

    @Override
    public ChannelStats getStats() {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'getStats'");
    }
    
}
