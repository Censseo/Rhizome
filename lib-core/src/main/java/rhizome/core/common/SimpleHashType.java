package rhizome.core.common;

import io.activej.bytebuf.ByteBuf;

import java.security.SecureRandom;
import java.util.Arrays;
import java.lang.reflect.InvocationTargetException;

public interface SimpleHashType {

    int getSize();
    byte[] toBytes();

    default void checkSize(ByteBuf object){
        if (object.limit() != getSize()) {
            throw new IllegalArgumentException("Invalid address size");
        }
    }

    public static ByteBuf empty(int size) {
        var bytes = new byte[size];
        Arrays.fill(bytes, (byte) 0);
        return ByteBuf.wrapForReading(bytes);
    }

    public static ByteBuf random(int size) {
        var random = new byte[size];
        new SecureRandom().nextBytes(random);
        return ByteBuf.wrapForReading(random);
    }

    @SuppressWarnings("unchecked")
    public static <T> T fromByte(Class<T> clazz, byte[] data) {
        try {
            return (T) clazz.getDeclaredMethod("of", byte[].class).invoke(null, data);
        } catch (NoSuchMethodException | IllegalAccessException | InvocationTargetException e) {
            e.printStackTrace();
        }
        return null;
    }
}
