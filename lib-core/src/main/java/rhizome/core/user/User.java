package rhizome.core.user;

import org.bouncycastle.crypto.params.Ed25519PrivateKeyParameters;
import org.bouncycastle.crypto.params.Ed25519PublicKeyParameters;
import org.json.JSONException;
import org.json.JSONObject;

import rhizome.core.crypto.PrivateKey;
import rhizome.core.crypto.PublicKey;
import rhizome.core.ledger.PublicAddress;
import rhizome.core.serialization.Serializable;
import rhizome.core.transaction.Transaction;
import rhizome.core.transaction.TransactionAmount;

import static rhizome.core.common.Helpers.PDN;
import static rhizome.core.common.Crypto.generateKeyPair;

public interface User {

    public static User create() {
        var kp = generateKeyPair();
        return UserImpl.builder()
                .publicKey(PublicKey.of((Ed25519PublicKeyParameters) kp.getPublic()))
                .privateKey(new PrivateKey((Ed25519PrivateKeyParameters) kp.getPrivate()))
                .build();
    }

    public static User of(JSONObject json){
        return serializer().fromJson(json);
    }

    public PublicKey publicKey();
    public PrivateKey privateKey();

    default PublicAddress getAddress() {
        return PublicAddress.of(publicKey());
    }

    default Transaction mine() {
        return Transaction.of(getAddress(), PDN(50));
    }

    default Transaction send(User receiver, double i) {
        return send(receiver, PDN(i));
    }

    default Transaction send(User to, TransactionAmount amount) {
        return Transaction.of(getAddress(), to.getAddress(), amount, publicKey())
            .sign(privateKey());
    }

    default void signTransaction(Transaction transaction) {
        transaction.sign(privateKey());
    }

    public JSONObject toJson();
    default JSONObject toJson(User transaction) {
        return serializer().toJson(transaction);
    }

    static UserSerializer serializer(){
        return UserSerializer.instance;
    }

    /**
     * Serializes the Transaction
     */
    static class UserSerializer implements Serializable<JSONObject, User> {

        static final String PUBLIC_KEY = "publicKey";
        static final String PRIVATE_KEY = "privateKey";

        static UserSerializer instance = new UserSerializer();

        @Override
        public JSONObject serialize(User object) {
            throw new UnsupportedOperationException("Unimplemented method 'serialize'");
        }
        @Override
        public User deserialize(JSONObject object) {
            throw new UnsupportedOperationException("Unimplemented method 'deserialize'");
        }
        @Override
        public User fromJson(JSONObject json) {
            try {
                return UserImpl.builder()
                        .publicKey(PublicKey.of(json.getString(PUBLIC_KEY)))
                        .privateKey(PrivateKey.of(json.getString(PRIVATE_KEY)))
                        .build();
            } catch (JSONException e) {
                e.printStackTrace();
            }
            return null;
        }
        @Override
        public JSONObject toJson(User user) {
            var userImpl = (UserImpl) user;
            JSONObject result = new JSONObject();
            result.put(PUBLIC_KEY, userImpl.publicKey().toHexString());
            result.put(PRIVATE_KEY, userImpl.privateKey().toHexString());
            return result;
        }
    }
}
