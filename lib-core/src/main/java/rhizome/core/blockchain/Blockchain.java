package rhizome.core.blockchain;

public interface Blockchain {


    default void sync() {
        throw new UnsupportedOperationException("Unimplemented method 'sync'");
    }

}
