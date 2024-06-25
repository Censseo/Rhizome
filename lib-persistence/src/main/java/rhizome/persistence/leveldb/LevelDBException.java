package rhizome.persistence.leveldb;

public class LevelDBException extends RuntimeException {
    public LevelDBException(String message) {
        super(message);
    }

    public LevelDBException(String message, Throwable cause) {
        super(message, cause);
    }    
}
