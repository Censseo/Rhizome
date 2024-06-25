package rhizome.core.serialization;

import java.lang.reflect.InvocationTargetException;

import io.activej.serializer.BinaryInput;
import io.activej.serializer.BinaryOutput;
import io.activej.serializer.BinarySerializer;
import io.activej.serializer.CompatibilityLevel;
import io.activej.serializer.CorruptedDataException;
import io.activej.serializer.SimpleSerializerDef;
import rhizome.core.common.SimpleHashType;

// TODO: finish implementation into BinarySerializable interface
public class SerializerDefSimpleHashType<T extends SimpleHashType> extends SimpleSerializerDef<T> {

    private final Class<T> typeClass;

    public SerializerDefSimpleHashType(Class<T> typeClass) {
        this.typeClass = typeClass;
    }

    @Override
    protected BinarySerializer<T> createSerializer(int version, CompatibilityLevel compatibilityLevel) {
        return new BinarySerializer<T>() {
            @Override
            public void encode(BinaryOutput out, T object) {
                out.write(object.toBytes());
            }

            @Override
            public T decode(BinaryInput in) throws CorruptedDataException {
                int size;
                try {
                    size = typeClass.getDeclaredConstructor().newInstance().getSize();
                    byte[] objectBytes = new byte[size];
                    in.read(objectBytes);
                    return SimpleHashType.fromByte(typeClass, objectBytes);
                } catch (InstantiationException | IllegalAccessException | IllegalArgumentException
                        | InvocationTargetException | NoSuchMethodException | SecurityException e) {
                    e.printStackTrace();
                }
                return null;
            }
        };
    }
}