package rhizome;

import org.junit.jupiter.api.Test;

import rhizome.core.block.Block;
import rhizome.core.block.dto.BlockDto;
import rhizome.core.serialization.BinarySerializable;
import rhizome.core.user.User;

import static org.junit.jupiter.api.Assertions.*;

import org.json.JSONObject;

class BlockTest {

    @Test
    void checkBlockJsonSerialization() {
        var a = Block.empty();
        var miner = User.create();
        var receiver = User.create();
        var t = miner.mine();
        a.addTransaction(t);
        // send tiny shares to receiver:
        for(int i = 0; i < 5; i++) {
            a.addTransaction(miner.send(receiver, 1));
        }
        Block b;
        b = Block.of(a.toJson());
        assertEquals(a, b);
    }

    @Test
    void checkBlockStructSerialization() {
        var a = Block.empty();
        var miner = User.create();
        var receiver = User.create();
        var t = miner.mine();
        a.addTransaction(t);
        // send tiny shares to receiver:
        for(int i = 0; i < 5; i++) {
            a.addTransaction(miner.send(receiver, 1));
        }
        var d = a.serialize();
        var b = Block.of(d, a.transactions());
        assertEquals(a, b);
    }

    @Test
    void checkBlockNetworkSerialization() {
        var blockA = Block.empty();
        var miner = User.create();
        var receiver = User.create();
        var transaction = miner.mine();
        blockA.addTransaction(transaction);
        // send tiny shares to receiver:
        for(int i = 0; i < 5; i++) {
            blockA.addTransaction(miner.send(receiver, 1));
        }
        var blockADto = blockA.serialize();
        var blockABuf = blockADto.toBuffer();
        var blockBDto = BinarySerializable.fromBuffer(blockABuf, BlockDto.class);
        var blockB = Block.of(blockBDto, blockA.transactions());
        assertEquals(blockB, blockA);
    }

    @Test
    void blockHashConsistency() {
        var A = "{\"difficulty\":25,\"id\":4780,\"lastBlockHash\":\"408B78E303A2130FCB5903887467286CE7D320D1080CBE8D3DE8198053B43AD6\",\"merkleRoot\":\"6B6431D62D1095CBB4D20F10168C1B39754D34997C528C3CE92AE68BB0BDE2CB\",\"nonce\":\"4566676B61BAB77BF15A658D6974592194EA453E7EBA975E74B2AD81056B094A\",\"timestamp\":\"1637858747\",\"transactions\":[{\"amount\":500000,\"fee\":0,\"from\":\"\",\"id\":4780,\"nonce\":\"92zgZDU7\",\"timestamp\":\"1637858747\",\"to\":\"00B28B48A25AFFBF80122963F2CB7957571AE2612B0126BF0D\"}]}";
        var B = "{\"difficulty\":25,\"id\":4780,\"lastBlockHash\":\"408B78E303A2130FCB5903887467286CE7D320D1080CBE8D3DE8198053B43AD6\",\"merkleRoot\":\"EB334D4DA3054E7463210F90EC2D3A0B33105F69AC099BADB863DEC97C7A347F\",\"nonce\":\"236278BC9D738823F361906FBD398DBEB7E8EB1C1E18FAD4099477BD3FBDBD62\",\"timestamp\":\"1637858713\",\"transactions\":[{\"amount\":500000,\"fee\":0,\"from\":\"\",\"id\":4780,\"nonce\":\"I2iyrN6f\",\"timestamp\":\"1637858713\",\"to\":\"00D1B6AF7C92153D2B3BD643B53FF4311746FCF889BE8F9078\"}]}";
        var a = Block.of(new JSONObject(A));
        var b = Block.of(new JSONObject(B));
        assertNotEquals(a, b);
    }
}
