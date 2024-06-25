package rhizome.persistence.leveldb;

import org.apache.commons.io.FileUtils;
import org.iq80.leveldb.DB;
import org.iq80.leveldb.DBException;
import org.iq80.leveldb.Options;
import org.iq80.leveldb.ReadOptions;
import org.iq80.leveldb.WriteOptions;

import io.activej.bytebuf.ByteBuf;
import io.activej.bytebuf.ByteBufPool;
import io.activej.common.MemSize;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

import org.iq80.leveldb.DBIterator;

import java.io.File;
import java.io.IOException;
import java.math.BigInteger;
import java.nio.ByteBuffer;

import static org.iq80.leveldb.impl.Iq80DBFactory.factory;
import static java.nio.charset.StandardCharsets.UTF_8;

@Getter @Setter @Slf4j
public class LevelDBDataStore {
    private DB db;
    private String path;

    public LevelDBDataStore() {
        this.db = null;
        this.path = "";
    }

    public void init(String path) throws IOException {
        if (this.db != null) {
            this.closeDB();
        }
        this.path = path;
        Options options = new Options();
        options.createIfMissing(true);
        this.db = factory.open(new File(path), options);
    }

    public void deleteDB() throws IOException {
        this.closeDB();
        factory.destroy(new File(path), new Options());
        File directory = new File(path);
        try {
            FileUtils.deleteDirectory(directory);
        } catch (IOException e) {
            throw new LevelDBException("Could not clear path " + path, e);
        }
    }

    public void closeDB() {
        if (this.db != null) {
            try {
                this.db.close();
            } catch (IOException e) {
                throw new LevelDBException("Could not close DataStore db", e);
            }
            this.db = null;
        }
    }

    public void clear() {
        try (DBIterator iterator = db.iterator()) {
            for (iterator.seekToFirst(); iterator.hasNext(); iterator.next()) {
                String key = new String(iterator.peekNext().getKey(), UTF_8);
                db.delete(key.getBytes(UTF_8));
            }
        } catch (IOException e) {
            throw new LevelDBException("Could not clear data store", e);
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
        db.put(key, value, new WriteOptions().sync(true));
    }

    protected Object get(String key, Class<?> type) {
        return get(key.getBytes(UTF_8), type);
    }
    protected Object get(int key, Class<?> type) {
        var keyByte = ByteBufPool.allocate(Integer.BYTES);
        keyByte.writeInt(key);
        return get(keyByte.asArray(), type);
    }
    protected Object get(byte[] key, Class<?> type) {
        var value = db.get(key, new ReadOptions());
        if (value == null) {
            throw new LevelDBException("Empty key: " + key);
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
                throw new LevelDBException("Could not read value of record " + key + " from BlockStore db.");
            }

            return buff;
        } else {
            throw new LevelDBException("Unsupported type");
        }
    }

    protected boolean hasKey(String key) {
        try {
            return db.get(key.getBytes(UTF_8), new ReadOptions()) != null;
        } catch (DBException e) {
            log.error("Error checking key", e);
            return false;
        }
    }

    protected boolean hasKey(int key) {
        try {
            var keyByte = ByteBufPool.allocate(Integer.BYTES);
            keyByte.writeInt(key);
            return db.get(keyByte.asArray()) != null;
        } catch (DBException e) {
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
