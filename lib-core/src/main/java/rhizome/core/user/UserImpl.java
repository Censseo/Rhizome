package rhizome.core.user;

import java.util.Arrays;
import java.util.Objects;

import org.json.JSONObject;

import lombok.Builder;
import lombok.Data;
import rhizome.core.crypto.PrivateKey;
import rhizome.core.crypto.PublicKey;

@Data
@Builder
public class UserImpl implements User {
    private PublicKey publicKey;
    private PrivateKey privateKey;

    @Override
    public JSONObject toJson() {
        return toJson(this);
    }

    @Override
    public boolean equals(Object o) {
        if (o == this) return true;
        if (!(o instanceof User)) return false;
        User user = (User) o;
        return Arrays.equals(publicKey().toBytes(), user.publicKey().toBytes()) &&
            Arrays.equals(privateKey().key().getEncoded(), user.privateKey().key().getEncoded());
    }

    @Override
    public int hashCode() {
        return Objects.hash(publicKey, privateKey);
    }
}
