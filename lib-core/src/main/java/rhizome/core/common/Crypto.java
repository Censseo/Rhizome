package rhizome.core.common;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Arrays;
import java.util.Random;
import java.util.concurrent.ConcurrentHashMap;
import java.math.BigInteger;

import org.bouncycastle.crypto.AsymmetricCipherKeyPair;
import org.bouncycastle.crypto.Signer;
import org.bouncycastle.crypto.generators.Ed25519KeyPairGenerator;
import org.bouncycastle.crypto.params.Ed25519KeyGenerationParameters;
import org.bouncycastle.crypto.signers.Ed25519Signer;

import rhizome.core.crypto.PrivateKey;
import rhizome.core.crypto.PublicKey;
import rhizome.core.crypto.SHA256Hash;

public class Crypto {

    private Crypto() {}

    public static byte[] signWithPrivateKey(String content, PrivateKey privateKey) {
        return signWithPrivateKey(content.getBytes(), privateKey);
    }

    public static byte[] signWithPrivateKey(byte[] message, PrivateKey privateKey) {
        try {
            Signer signer = new Ed25519Signer();
            signer.init(true, privateKey.key());
            signer.update(message, 0, message.length);
            return signer.generateSignature();
        } catch (Exception e) {
            e.printStackTrace();
            return new byte[0];
        }
    }

    public static boolean checkSignature(String content, byte[] signature, PublicKey publicKey) {
        return checkSignature(content.getBytes(), signature, publicKey);
    }
    
    public static boolean checkSignature(byte[] bytes, byte[] signature, PublicKey publicKey) {
        Ed25519Signer signer = new Ed25519Signer();
        signer.init(false, publicKey.get());
        signer.update(bytes, 0, bytes.length);
        return signer.verifySignature(signature);
    }

    public static AsymmetricCipherKeyPair generateKeyPair() {
        Ed25519KeyPairGenerator keyGen = new Ed25519KeyPairGenerator();
        keyGen.init(new Ed25519KeyGenerationParameters(new SecureRandom()));
        return keyGen.generateKeyPair();
    }

    private static final ConcurrentHashMap<SHA256Hash, SHA256Hash> pufferfishCache = new ConcurrentHashMap<>();

    public static SHA256Hash PUFFERFISH(byte[] input, boolean useCache) {
        SHA256Hash inputHash = SHA256Hash.of(input);
        
        if (useCache) {
            // Implement cache retrieval logic here
            SHA256Hash cachedHash = pufferfishCache.get(inputHash);
            if (cachedHash != null) {
                return cachedHash;
            }
        }
        
        byte[] hash = new byte[PufferfishConstants.PF_HASHSPACE];
        hash = PufferfishAlgorithm.compute(input);
        
        SHA256Hash finalHash = SHA256(hash); // Assuming SHA256 is standard SHA-256 hash
        
        if (useCache) {
            // Implement cache storage logic here
            pufferfishCache.put(inputHash, finalHash);
        }
        
        return finalHash;
    }

    public static SHA256Hash SHA256(byte[] hash) {
        return SHA256(hash, false, false);
    }

    public static SHA256Hash SHA256(byte[] data, boolean usePufferFish, boolean useCache) {
        if (usePufferFish) {
            return PUFFERFISH(data, useCache);
        }
        
        // Standard SHA-256 Hashing
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(data);
            return SHA256Hash.of(hash);
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("Unable to find SHA-256 algorithm", e);
        }
    }

    public SHA256Hash mineHash(SHA256Hash target, byte challengeSize, boolean usePufferFish) {
        int hashes = 0;
        long st = System.currentTimeMillis();

        byte[] concat = new byte[2 * 32];
        Random rand = new SecureRandom();

        // Copy the target hash into the first part of concat.
        System.arraycopy(target.hash().getArray(), 0, concat, 0, 32);
        // Fill with random data for privacy
        byte[] randomBytes = new byte[32];
        rand.nextBytes(randomBytes);
        System.arraycopy(randomBytes, 0, concat, 32, 32);

        long i = 0;
        SHA256Hash solution;
        while (true) {
            if (++hashes > 1024) {
                long elapsed = System.currentTimeMillis() - st;
                double hps = hashes / (elapsed / 1000.0);
                System.out.println("Mining at " + hps + " h/sec");
                hashes = 0;
                st = System.currentTimeMillis();
            }

            i++;
            incrementByteArrayByOne(concat, 32);

            // This assumes solution is mutable.
            solution = SHA256Hash.of(Arrays.copyOfRange(concat, 32, 64));
            SHA256Hash fullHash = concatHashes(target, solution, usePufferFish, false);

            boolean found = checkLeadingZeroBits(fullHash, challengeSize);

            if (found) {
                break;
            }
        }

        return solution;
    }

    public static boolean verifyHash(SHA256Hash target, SHA256Hash nonce, int challengeSize, boolean usePufferFish, boolean useCache) {
        SHA256Hash fullHash = concatHashes(target, nonce, usePufferFish, useCache);
        return checkLeadingZeroBits(fullHash, challengeSize);
    }

    private void incrementByteArrayByOne(byte[] array, int offset) {
        for (int i = offset; i < array.length; i++) {
            if (++array[i] != 0) {
                break;
            }
        }
    }

    public static SHA256Hash concatHashes(SHA256Hash a, SHA256Hash b, boolean usePufferFish, boolean useCache) {
        // Assuming SHA256 is a method that takes a byte array and returns a SHA256Hash object
        // Pufferfish and caching functionality would need to be implemented within the SHA256 method.
        byte[] data = new byte[64];
        System.arraycopy(a.hash().getArray(), 0, data, 0, 32);
        System.arraycopy(b.hash().getArray(), 0, data, 32, 32);
        
        // Instead of 'usePufferFish' and 'useCache' which are not standard,
        // you'd typically call a standard Java SHA-256 implementation here.
        // If 'usePufferFish' refers to a custom hashing algorithm, you'd need to implement it accordingly.
        return SHA256(data);
    }

    public static boolean checkLeadingZeroBits(SHA256Hash hash, int challengeSize) {
        byte[] a = hash.hash().getArray();
        int bytes = challengeSize / 8;
        for (int i = 0; i < bytes; i++) {
            if (a[i] != 0) return false;
        }
        int remainingBits = challengeSize % 8;
        if (remainingBits > 0) {
            // Create a bitmask to check only the required remaining bits
            int bitmask = (1 << remainingBits) - 1;
            return (a[bytes] & (bitmask << (8 - remainingBits))) == 0;
        } else {
            return true;
        }
    }

    public static BigInteger addWork(BigInteger work, int exponent) {
        BigInteger base = BigInteger.valueOf(2);
        return work.add(base.pow(exponent));
    }

}
