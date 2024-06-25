package rhizome.core.serialization;

import io.activej.serializer.BinaryInput;
import io.activej.serializer.BinaryOutput;
import io.activej.serializer.BinarySerializer;
import io.activej.serializer.CompatibilityLevel;
import io.activej.serializer.CorruptedDataException;
import io.activej.serializer.SimpleSerializerDef;
import rhizome.core.crypto.PublicKey;

public class SerializerDefPublicKey extends SimpleSerializerDef<PublicKey> {
    @Override
    protected BinarySerializer<PublicKey> createSerializer(int version, CompatibilityLevel compatibilityLevel) {
        return new BinarySerializer<PublicKey>() {
            @Override
            public void encode(BinaryOutput out, PublicKey key) {
                out.write(key.toBytes());
            }

            @Override
            public PublicKey decode(BinaryInput in) throws CorruptedDataException {
                byte[] keyBytes = new byte[PublicKey.SIZE];
                in.read(keyBytes);
                return PublicKey.of(keyBytes);
            }
        };
    }
}