package rhizome.core.serialization;

import io.activej.bytebuf.ByteBuf;
import io.activej.serializer.BinaryInput;
import io.activej.serializer.BinaryOutput;
import io.activej.serializer.BinarySerializer;
import io.activej.serializer.CompatibilityLevel;
import io.activej.serializer.CorruptedDataException;
import io.activej.serializer.SimpleSerializerDef;
import rhizome.core.crypto.SHA256Hash;

public final class SerializerDefSHA256Hash extends SimpleSerializerDef<SHA256Hash> {
    @Override
    protected BinarySerializer<SHA256Hash> createSerializer(int version, CompatibilityLevel compatibilityLevel) {
        return new BinarySerializer<SHA256Hash>() {
            @Override
            public void encode(BinaryOutput out, SHA256Hash hash) {
                out.write(hash.toBytes());
            }

            @Override
            public SHA256Hash decode(BinaryInput in) throws CorruptedDataException {
                byte[] hashBytes = new byte[SHA256Hash.SIZE];
                in.read(hashBytes);
                return new SHA256Hash(ByteBuf.wrapForReading(hashBytes));
            }
        };
    }
}