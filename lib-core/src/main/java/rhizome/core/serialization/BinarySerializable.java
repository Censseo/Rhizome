package rhizome.core.serialization;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.jetbrains.annotations.NotNull;

import io.activej.serializer.BinarySerializer;
import io.activej.serializer.SerializerFactory;
import rhizome.core.crypto.PublicKey;
import rhizome.core.crypto.SHA256Hash;
import rhizome.core.ledger.PublicAddress;
import rhizome.core.transaction.TransactionSignature;

public interface BinarySerializable {
    static Map<Class<? extends BinarySerializable>, BinarySerializer<? extends BinarySerializable>> serializerCache = new ConcurrentHashMap<>();

    public static <T extends BinarySerializable> T fromBuffer(byte[] buffer, Class<T> clazz) {
        return fromBuffer(buffer, 0, clazz);
    }

    public static <T extends BinarySerializable> T fromBuffer(byte[] buffer, int pos, Class<T> clazz) {
        var serializer = getSerializer(clazz);
        return serializer.decode(buffer, pos);
    }

    @SuppressWarnings("unchecked")
    public default <T extends BinarySerializable> byte[] toBuffer() {
        var buffer = new byte[getSize()];
        var serializer = getSerializer((Class<T>) this.getClass());
        serializer.encode(buffer, 0, (T) this);
        return buffer;
    }

    @NotNull
    int getSize();

    @SuppressWarnings("unchecked")
    static <T extends BinarySerializable> BinarySerializer<T> getSerializer(Class<T> clazz) {
        return (BinarySerializer<T>) serializerCache.computeIfAbsent(clazz, k ->
            SerializerFactory.builder()
            .with(SHA256Hash.class, ctx -> new SerializerDefSHA256Hash())
            .with(PublicAddress.class, ctx -> new SerializerDefPublicAddress())
            .with(PublicKey.class, ctx -> new SerializerDefPublicKey())
            .with(TransactionSignature.class, ctx -> new SerializerDefTransactionSignature())
            .build()
            .create(k));
    }
}
