package rhizome.net.transport;

public interface ChannelOutput {
    void ping();
    boolean isClosed();
}