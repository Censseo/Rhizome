package rhizome;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static rhizome.core.common.Helpers.PDN;

import org.json.JSONObject;
import org.junit.jupiter.api.Test;

import rhizome.core.block.Block;
import rhizome.core.transaction.Transaction;
import rhizome.core.user.User;

class UserTests {
        @Test
    void checkSignature() {
        Block b = Block.empty();
        
        User miner = User.create();
        User receiver = User.create();

        Transaction t = miner.mine();
        b.addTransaction(t);
        Transaction t2 = miner.send(receiver, PDN(30.0));
        b.addTransaction(t2);

        assertTrue(t2.signatureValid());
    }

    @Test
    void checkUserSerialization() {
        Block b = Block.empty();
        
        User miner = User.create();
        User receiver = User.create();

        Transaction t = miner.mine();
        b.addTransaction(t);
        Transaction t2 = miner.send(receiver, PDN(30.0));
        b.addTransaction(t2);

        assertTrue(t2.signatureValid());

        // Recreate miner from JSON
        String serialized = miner.toJson().toString();
        JSONObject parsed = new JSONObject(serialized);

        User minerCopy = User.of(parsed);
        // Test the signature still works
        minerCopy.signTransaction(t2);
        assertTrue(t2.signatureValid());
    }
}
