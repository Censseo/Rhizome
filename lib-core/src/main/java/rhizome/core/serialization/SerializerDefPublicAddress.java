package rhizome.core.serialization;

import io.activej.serializer.BinaryInput;
import io.activej.serializer.BinaryOutput;
import io.activej.serializer.BinarySerializer;
import io.activej.serializer.CompatibilityLevel;
import io.activej.serializer.CorruptedDataException;
import io.activej.serializer.SimpleSerializerDef;
import rhizome.core.ledger.PublicAddress;

public class SerializerDefPublicAddress extends SimpleSerializerDef<PublicAddress> {
    @Override
    protected BinarySerializer<PublicAddress> createSerializer(int version, CompatibilityLevel compatibilityLevel) {
        return new BinarySerializer<PublicAddress>() {
            @Override
            public void encode(BinaryOutput out, PublicAddress address) {
                out.write(address.toBytes());
            }

            @Override
            public PublicAddress decode(BinaryInput in) throws CorruptedDataException {
                byte[] addressBytes = new byte[PublicAddress.SIZE];
                in.read(addressBytes);
                return PublicAddress.of(addressBytes);
            }
        };
    }
}