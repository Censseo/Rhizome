package rhizome.core.common;

public class PufferfishConstants {

    private static final int PF_SALTSPACE = 16; // Just an example value.
    private static final int PF_DIGEST_LENGTH = 32; // Example value for a 256-bit hash.

    public static final int PF_HASHSPACE = PF_SALTSPACE + bin2enc_len(PF_DIGEST_LENGTH);


    private static int bin2enc_len(int binaryLength) {
           return (int) Math.ceil(binaryLength * 4 / 3.0); // This is just an example for base64 encoding length.
    }
}
