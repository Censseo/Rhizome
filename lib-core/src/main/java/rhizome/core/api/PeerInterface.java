package rhizome.core.api;

import java.io.IOException;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.json.JSONObject;

import io.activej.bytebuf.ByteBuf;
import rhizome.core.block.Block;
import rhizome.core.block.dto.BlockDto;
import rhizome.core.serialization.BinarySerializable;
import rhizome.core.transaction.Transaction;
import rhizome.core.transaction.dto.TransactionDto;

// TODO: basic translation from C++ , need refactor
public class PeerInterface {

    private static final int TIMEOUT_MS = 10000;
    private static final int TIMEOUT_BLOCKHEADERS_MS = 15000;
    private static final int TIMEOUT_BLOCK_MS = 20000;
    private static final int BLOCKHEADER_BUFFER_SIZE = 80;
    private static final int TRANSACTIONINFO_BUFFER_SIZE = 100; 
    private static final int TIMEOUT_SUBMIT_MS = 10000; 


    public static Optional<Long> getCurrentBlockCount(String hostUrl) {
        return tryGetLong(hostUrl + "/block_count");
    }

    public static Optional<JSONObject> getTotalWork(String hostUrl) {
        return tryGetJson(hostUrl + "/total_work");
    }

    public static Optional<String> getName(String hostUrl) {
        return tryGetString(hostUrl + "/name");
    }

    public static Optional<JSONObject> getBlockData(String hostUrl, int idx) {
        return tryGetJson(hostUrl + "/block?blockId=" + idx);
    }

    public static Optional<JSONObject> pingPeer(String hostUrl, String peerUrl,
                                                long networkTime, String version,
                                                String networkName) {
        JSONObject info = new JSONObject();
        info.put("address", peerUrl);
        info.put("networkName", networkName);
        info.put("time", networkTime);
        info.put("version", version);

        return sendJsonRequest(hostUrl + "/add_peer", info, "POST");
    }

    public static Optional<JSONObject> submitBlock(String hostUrl, Block block) {
        try {
            URL url = new URL(hostUrl + "/submit");
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("POST");
            connection.setConnectTimeout(TIMEOUT_SUBMIT_MS);
            connection.setRequestProperty("Content-Type", "application/octet-stream");
            connection.setDoOutput(true);

            BlockDto b = block.serialize();
            ByteBuffer buffer = ByteBuffer.allocate(BLOCKHEADER_BUFFER_SIZE +
                                                    TRANSACTIONINFO_BUFFER_SIZE * b.numTransactions());
            buffer.put(b.toBuffer());
            
            for (Transaction t : block.transactions()) {
                TransactionDto tx = t.serialize();
                buffer.put(tx.toBuffer());
            }

            try (OutputStream os = connection.getOutputStream()) {
                os.write(buffer.array());
            }

            if (connection.getResponseCode() == HttpURLConnection.HTTP_OK) {
                String response = new String(connection.getInputStream().readAllBytes());
                return Optional.of(new JSONObject(response));
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return Optional.empty();
    }
    
    public static Optional<JSONObject> getMiningProblem(String hostUrl) {
        return tryGetJson(hostUrl + "/mine");
    }
    
    public static String sendTransaction(String hostUrl, Transaction transaction) throws IOException {
        URL url = new URL(hostUrl + "/add_transaction");
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        connection.setRequestMethod("POST");
        connection.setConnectTimeout(TIMEOUT_MS * 3);
        connection.setRequestProperty("Content-Type", "application/octet-stream");
        connection.setDoOutput(true);

        TransactionDto info = transaction.serialize(); // Assurez-vous que cette méthode existe
        byte[] bytes = info.toBuffer();
        
        try (OutputStream os = connection.getOutputStream()) {
            os.write(bytes);
        }

        int responseCode = connection.getResponseCode();
        if (responseCode == HttpURLConnection.HTTP_OK) {
            return new String(connection.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
        } else {
            return "";
        }
    }

    public static Optional<JSONObject> verifyTransaction(String hostUrl, Transaction transaction) {
        try {
            URL url = new URL(hostUrl + "/verify_transaction");
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("POST");
            connection.setConnectTimeout(TIMEOUT_MS);
            connection.setRequestProperty("Content-Type", "application/octet-stream");
            connection.setDoOutput(true);

            byte[] serializedTransaction = transaction.serialize().toBuffer();

            try (OutputStream os = connection.getOutputStream()) {
                os.write(serializedTransaction);
            }

            if (connection.getResponseCode() == HttpURLConnection.HTTP_OK) {
                String responseStr = new String(connection.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
                return Optional.of(new JSONObject(responseStr));
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return Optional.empty();
    }

    public static void readRawHeaders(String hostUrl, long startId, long endId,
                                    List<BlockDto> blockHeaders) throws IOException {
        URL url = new URL(hostUrl + "/block_headers?start=" + startId + "&end=" + endId);
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        connection.setRequestMethod("GET");
        connection.setConnectTimeout(TIMEOUT_BLOCKHEADERS_MS);
        connection.setRequestProperty("Content-Type", "application/octet-stream");

        byte[] bytes = connection.getInputStream().readAllBytes();
        ByteBuf buffer = ByteBuf.wrapForReading(bytes);
        int numBlocks = bytes.length / BLOCKHEADER_BUFFER_SIZE;

        for (int i = 0; i < numBlocks; i++) {
            byte[] blockBytes = new byte[BLOCKHEADER_BUFFER_SIZE];
            buffer.read(blockBytes, 0, BLOCKHEADER_BUFFER_SIZE);
            var blockHeader = BinarySerializable.fromBuffer(blockBytes, BlockDto.class);
            blockHeaders.add(blockHeader);
        }

        connection.disconnect();
    }

    public static void readRawBlocks(String hostUrl, int startId, int endId, List<Block> blocks) throws IOException {
        URL url = new URL(hostUrl + "/sync?start=" + startId + "&end=" + endId);
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        connection.setRequestMethod("GET");
        connection.setConnectTimeout(TIMEOUT_BLOCK_MS);
        connection.setRequestProperty("Content-Type", "application/octet-stream");

        byte[] bytes = connection.getInputStream().readAllBytes();
        if (bytes.length < BLOCKHEADER_BUFFER_SIZE) {
            throw new IOException("Invalid data for block");
        }

        int bytesRead = 0;
        int currIndex = 0;
        while (currIndex < bytes.length) {
            BlockDto blockHeader = BinarySerializable.fromBuffer(bytes, currIndex, BlockDto.class);
            currIndex += BLOCKHEADER_BUFFER_SIZE;
            bytesRead += BLOCKHEADER_BUFFER_SIZE;

            List<Transaction> transactions = new ArrayList<>();
            for (int i = 0; i < blockHeader.numTransactions(); i++) {
                TransactionDto transactionInfo = BinarySerializable.fromBuffer(bytes, currIndex, TransactionDto.class);
                transactions.add(Transaction.of(transactionInfo));
                currIndex += TRANSACTIONINFO_BUFFER_SIZE;
                bytesRead += TRANSACTIONINFO_BUFFER_SIZE;
            }

            blocks.add(Block.of(blockHeader, transactions));
        }
    }

    public static void readRawTransactions(String hostUrl, List<Transaction> transactions) {
        try {
            URL url = new URL(hostUrl + "/gettx");
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("GET");
            connection.setConnectTimeout(TIMEOUT_MS);
            connection.setRequestProperty("Content-Type", "application/octet-stream");

            byte[] bytes = connection.getInputStream().readAllBytes();

            int numTx = bytes.length / TRANSACTIONINFO_BUFFER_SIZE;
            ByteBuffer buffer = ByteBuffer.wrap(bytes);

            for (int i = 0; i < numTx; i++) {
                byte[] transactionBytes = new byte[TRANSACTIONINFO_BUFFER_SIZE];
                buffer.get(transactionBytes, 0, TRANSACTIONINFO_BUFFER_SIZE);
                TransactionDto transactionInfo = BinarySerializable.fromBuffer(transactionBytes, TransactionDto.class);
                transactions.add(Transaction.of(transactionInfo));
            }

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static Optional<JSONObject> tryGetJson(String urlString) {
        try {
            URL url = new URL(urlString);
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("GET");
            connection.setConnectTimeout(TIMEOUT_MS);
            connection.setRequestProperty("Content-Type", "application/json");

            if (connection.getResponseCode() == HttpURLConnection.HTTP_OK) {
                String responseBody = new String(connection.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
                return Optional.of(new JSONObject(responseBody));
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return Optional.empty();
    }

    private static Optional<String> tryGetString(String urlString) {
        try {
            URL url = new URL(urlString);
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("GET");
            connection.setConnectTimeout(TIMEOUT_MS);
            connection.setRequestProperty("Content-Type", "application/json");

            if (connection.getResponseCode() == HttpURLConnection.HTTP_OK) {
                String responseBody = new String(connection.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
                return Optional.of(responseBody);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return Optional.empty();
    }

    private static Optional<Long> tryGetLong(String urlString) {
        try {
            URL url = new URL(urlString);
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("GET");
            connection.setConnectTimeout(TIMEOUT_MS);
            connection.setRequestProperty("Content-Type", "application/json");

            if (connection.getResponseCode() == HttpURLConnection.HTTP_OK) {
                String responseBody = new String(connection.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
                return Optional.of(Long.valueOf(responseBody));
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return Optional.empty();
    }

    private static Optional<JSONObject> sendJsonRequest(String urlString, JSONObject json, String method) {
        try {
            URL url = new URL(urlString);
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod(method);
            connection.setConnectTimeout(TIMEOUT_MS);
            connection.setRequestProperty("Content-Type", "application/json");
            connection.setDoOutput(true);

            try (var os = connection.getOutputStream()) {
                byte[] input = json.toString().getBytes(StandardCharsets.UTF_8);
                os.write(input, 0, input.length);
            }

            if (connection.getResponseCode() == HttpURLConnection.HTTP_OK) {
                String responseBody = new String(connection.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
                return Optional.of(new JSONObject(responseBody));
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return Optional.empty();
    }
}
