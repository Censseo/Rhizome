package rhizome.core.block;

public class BlockException extends RuntimeException {
    public BlockException(String message) {
        super(message);
    }

    public BlockException(String message, Throwable cause) {
        super(message, cause);
    }
}
