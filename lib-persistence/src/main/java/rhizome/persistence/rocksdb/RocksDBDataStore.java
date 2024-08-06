package rhizome.persistence.rocksdb;

import io.activej.bytebuf.ByteBuf;
import io.activej.bytebuf.ByteBufPool;
import io.activej.common.MemSize;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

import org.apache.commons.io.FileUtils;
import org.rocksdb.Options;
import org.rocksdb.RocksDB;
import org.rocksdb.RocksDBException;
import org.rocksdb.RocksIterator;
import org.rocksdb.WriteOptions;

import java.io.File;
import java.io.IOException;
import java.math.BigInteger;
import java.nio.ByteBuffer;

import static java.nio.charset.StandardCharsets.UTF_8;

@Getter @Setter @Slf4j
public class RocksDBDataStore {
    private RocksDB db;
    private String path;

    public RocksDBDataStore() {
        this.db = null;
        this.path = "";
    }

    public void init(String path) {

        RocksDB.loadLibrary();

        if (this.db != null) {
            this.closeDB();
        }

        this.path = path;
        Options options = new Options();
        options.setCreateIfMissing(true);
        try {
            this.db = RocksDB.open(options, path);
        } catch (RocksDBException e) {
            log.error("Error opening RocksDB", e);
        }
    }

    public void deleteDB() throws IOException, RocksDBException {
        this.closeDB();
        RocksDB.destroyDB(path, new Options());
        
        File directory = new File(path);
        try {
            FileUtils.deleteDirectory(directory);
        } catch (IOException e) {
            throw new RocksDBException("Could not clear path " + path);
        }
    }

    public void closeDB() {
        if (this.db != null) {
            this.db.close();
            this.db = null;
        }
    }

    public void clear() throws RocksDBException {
    RocksIterator iterator = db.newIterator();
        try {
            for (iterator.seekToFirst(); iterator.isValid(); iterator.next()) {
                byte[] key = iterator.key();
                db.delete(key);
            }
        } finally {
            iterator.close();
        }
    }

    public String getPath() {
        return this.path;
    }

    protected void set(String key, String value) {
        set(key.getBytes(UTF_8), value.getBytes(UTF_8));
    }
    protected void set(String key, int value) {
        set(key.getBytes(UTF_8), Integer.toString(value).getBytes(UTF_8));
    }
    protected void set(String key, long value) {
        var keyByte = ByteBufPool.allocate(Long.BYTES);
        keyByte.writeLong(value);
        set(key.getBytes(UTF_8), keyByte.asArray());
    }
    protected void set(String key, BigInteger value) {
        set(key.getBytes(UTF_8), value.toByteArray());
    }
    protected void set(int key, byte[] value) {
        var keyByte = ByteBufPool.allocate(Integer.BYTES);
        keyByte.writeInt(key);
        set(keyByte.asArray(), value);
    }

    protected void set(byte[] key, byte[] value) {
        try (WriteOptions writeOptions = new WriteOptions().setSync(true)) {
            db.put(writeOptions, key, value);
        } catch (RocksDBException e) {
            log.error("Error setting key", e);
        }
    }

    protected void delete(byte[] key) {
        try (WriteOptions writeOptions = new WriteOptions().setSync(true)) {
            db.delete(writeOptions, key);
        } catch (RocksDBException e) {
            log.error("Error deleting transaction", e);
        }
    }

    protected Object get(String key, Class<?> type) {
        try {
            return get(key.getBytes(UTF_8), type);
        } catch (RocksDBException e) {
            log.error(key, e);
        }
        return null;
    }
    protected Object get(int key, Class<?> type) {
        var keyByte = ByteBufPool.allocate(Integer.BYTES);
        keyByte.writeInt(key);
        try {
            return get(keyByte.asArray(), type);
        } catch (RocksDBException e) {
            log.error(keyByte.toString(), e);
        }
        return null;
    }
    protected Object get(byte[] key, Class<?> type) throws RocksDBException {
        byte[] value;
        try {
            value = db.get(key);
        } catch (RocksDBException e) {
            value = null;
        }
        
        if (value == null) {
            throw new RocksDBException("Empty key: " + key);
        }

        if(type == String.class) {
            return new String(value, UTF_8);
        } else if (type == Integer.class) {
            return ByteBuffer.wrap(value).getInt();
        } else if (type == Long.class && value.length == Long.BYTES) {
            return ByteBuffer.wrap(value).getLong();
        } else if (type == BigInteger.class) {
            return new BigInteger(value);
        } else if (type == byte[].class) {
            return value;
        } else if (type == ByteBuf.class) {
            var buff = ByteBufPool.allocate(MemSize.of(value.length));
            buff.put(value);

            if (!buff.canRead()) {
                throw new RocksDBException("Could not read value of record " + key + " from BlockStore db.");
            }

            return buff;
        } else {
            throw new RocksDBException("Unsupported type");
        }
    }

    protected boolean hasKey(String key) {
        try {
            return db.get(key.getBytes(UTF_8)) != null;
        } catch (RocksDBException e) {
            log.error("Error checking key", e);
            return false;
        }
    }

    protected boolean hasKey(int key) {
        try {
            var keyByte = ByteBufPool.allocate(Integer.BYTES);
            keyByte.writeInt(key);
            return db.get(keyByte.asArray()) != null;
        } catch (RocksDBException e) {
            log.error("Error checking key", e);
            return false;
        }
    }

    protected static byte[] composeKey(int key1, int key2) {
        var key = ByteBufPool.allocate(MemSize.bytes(2l * Integer.BYTES));
        key.writeInt(key1);
        key.writeInt(key2);
        return key.asArray();
    }

    protected static byte[] composeKey(byte[] key1, byte[] key2) {
        var compositeKey = ByteBufPool.allocate(MemSize.bytes((long)key1.length + key2.length));
        compositeKey.put(key1);
        compositeKey.put(key2);
        return compositeKey.asArray();
    }
}
