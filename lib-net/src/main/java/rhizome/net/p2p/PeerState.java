package rhizome.net.p2p;

public enum PeerState {
    CONNECTED("connected"),
    DISCONNECTED("disconnected"),
    SUSPENDED("suspended"), 
    RESUMED("resumed"),
    ERROR("error"),
    JOIN("join"),
    RECEIVE("receive");

    private final String state;

    PeerState(String state) {
        this.state = state;
    }
}
