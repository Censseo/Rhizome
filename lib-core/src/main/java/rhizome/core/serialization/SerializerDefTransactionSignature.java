package rhizome.core.serialization;

import io.activej.serializer.BinaryInput;
import io.activej.serializer.BinaryOutput;
import io.activej.serializer.BinarySerializer;
import io.activej.serializer.CompatibilityLevel;
import io.activej.serializer.CorruptedDataException;
import io.activej.serializer.SimpleSerializerDef;
import rhizome.core.transaction.TransactionSignature;

public class SerializerDefTransactionSignature extends SimpleSerializerDef<TransactionSignature> {
    @Override
    protected BinarySerializer<TransactionSignature> createSerializer(int version, CompatibilityLevel compatibilityLevel) {
        return new BinarySerializer<TransactionSignature>() {
            @Override
            public void encode(BinaryOutput out, TransactionSignature signature) {
                out.write(signature.toBytes());
            }

            @Override
            public TransactionSignature decode(BinaryInput in) throws CorruptedDataException {
                byte[] signatureBytes = new byte[TransactionSignature.SIZE];
                in.read(signatureBytes);
                return TransactionSignature.of(signatureBytes);
            }
        };
    }
}