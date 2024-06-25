package rhizome;

import org.junit.jupiter.api.Test;

import rhizome.core.transaction.Transaction;
import rhizome.core.transaction.TransactionImpl;
import rhizome.core.transaction.dto.TransactionDto;
import rhizome.core.user.User;

import static org.junit.jupiter.api.Assertions.*;

class TransactionTests {

    @Test
    void checkTransactionJsonSerialization() {
        User miner = User.create();
        User receiver = User.create();

        Transaction t = miner.mine();
        Transaction t2 = miner.send(receiver, 30.0);

        assertTrue(t2.signatureValid());

        // Test the send transaction
        long ts = ((TransactionImpl) t2).timestamp();
        Transaction deserialized = Transaction.of(t2.toJson());

        assertTrue(deserialized.signatureValid());
        assertEquals(t2, deserialized);
        assertEquals(ts, ((TransactionImpl) deserialized).timestamp());

        // Test mining transaction
        deserialized = Transaction.of(t.toJson());
        ts = ((TransactionImpl) t).timestamp();

        assertEquals(t.hashContents(), deserialized.hashContents());
        assertEquals(t, deserialized);
        assertEquals(ts, ((TransactionImpl) deserialized).timestamp());
    }

    @Test
    void checkTransactionStructSerialization() {
        User miner = User.create();
        User receiver = User.create();

        Transaction t = miner.mine();
        Transaction t2 = miner.send(receiver, 30.0);

        assertTrue(t2.signatureValid());

        // Test the send transaction
        long ts = ((TransactionImpl) t2).timestamp();
        TransactionDto serialized = t2.serialize();
        Transaction deserialized = Transaction.of(serialized);

        assertTrue(deserialized.signatureValid());
        assertEquals(t2, deserialized);
        assertEquals(ts, ((TransactionImpl) deserialized).timestamp());

        // Test mining transaction
        serialized = t.serialize();
        deserialized = Transaction.of(serialized);
        ts = ((TransactionImpl) t).timestamp();

        assertEquals(t.hashContents(), deserialized.hashContents());
        assertEquals(t, deserialized);
        assertEquals(ts, ((TransactionImpl) deserialized).timestamp());
    }

    @Test
    void checkTransactionCopy() {
        User miner = User.create();
        User receiver = User.create();

        Transaction t = miner.mine();
        Transaction t2 = miner.send(receiver, 30.0);

        Transaction a = Transaction.of(t);
        Transaction b = Transaction.of(t2);

        assertEquals(a, t);
        assertEquals(b, t2);
    }
}