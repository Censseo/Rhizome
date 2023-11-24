package rhizome.core.blockchain;

import static rhizome.core.common.Crypto.addWork;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import lombok.Builder;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import rhizome.core.block.Block;
import rhizome.core.block.dto.BlockDto;
import rhizome.core.common.Constants;
import rhizome.core.crypto.SHA256Hash;
import rhizome.core.transaction.Transaction;
import rhizome.persistence.BlockPersistence;

@Data
@Builder
@Slf4j
public class HeaderChain {

    private String host;
    private boolean failed;
    private long offset;
    private BigInteger totalWork;
    private long chainLength;
    private BlockPersistence blockStore;
    private boolean triedBlockStoreCache;
    private List<Thread> syncThread;
    private Map<Long, SHA256Hash> checkPoints;
    private Map<Long, SHA256Hash> bannedHashes;
    private List<SHA256Hash> blockHashes;
    
    void reset() {
        this.failed = false;
        this.offset = 0;
        this.totalWork = BigInteger.ZERO;
        this.chainLength = 0;
    }

    boolean isValid() {
        return !failed && totalWork.compareTo(BigInteger.ZERO) > 0;
    }

    BigInteger getTotalWork() {
        if(failed) {
            return BigInteger.ZERO;
        }
        return totalWork;
    }

    long getChainLength() {
        if(failed) {
            return 0;
        }
        return chainLength;
    }

    long getCurrentDownloaded() {
        return blockHashes.size();
    }

    public void load() {
        Optional<Long> opt = getCurrentBlockCount(this.host);
        if (!opt.isPresent()) {
            this.failed = true;
            return;
        }
        long targetBlockCount = opt.get();
        
        SHA256Hash lastHash = SHA256Hash.empty();
        if (!this.blockHashes.isEmpty()) {
            lastHash = this.blockHashes.get(this.blockHashes.size() - 1);
        }
        long numBlocks = this.blockHashes.size();
        long startBlocks = numBlocks;
        BigInteger totalWork = this.totalWork;

        for (long i = numBlocks + 1; i <= targetBlockCount; i += Constants.BLOCK_HEADERS_PER_FETCH) {
            try {
                long end = Math.min(targetBlockCount, i + Constants.BLOCK_HEADERS_PER_FETCH - 1);
                boolean failure = false;
                ArrayList<BlockDto> blockHeaders = new ArrayList<>();
                BlockchainNode.readRawHeaders(this.host, i, end, blockHeaders); // Adaptez cette méthode

                for (BlockDto b : blockHeaders) {
                    ArrayList<Transaction> empty = new ArrayList<>();
                    var block = Block.of(b, empty);
                    long curr = b.getId();

                    if (this.bannedHashes.containsKey(curr) && block.getHash().equals(this.bannedHashes.get(curr))) {
                        log.info("Banned hash found for block: {}", curr);
                        failure = true;
                        break;
                    }

                    if (this.checkPoints.containsKey(curr) && !block.getHash().equals(this.checkPoints.get(curr))) {
                        failure = true;
                        break;
                    }

                    if (!block.verifyNonce()) {
                        failure = true;
                        break;
                    }

                    if (!block.getLastBlockHash().equals(lastHash)) {
                        failure = true;
                        break;
                    }

                    lastHash = block.getHash();
                    this.blockHashes.add(lastHash);
                    totalWork = addWork(totalWork, block.getDifficulty());
                    numBlocks++;
                    this.chainLength = numBlocks;
                    this.totalWork = totalWork;
                }

                if (failure) {
                    log.warn("header chain sync failed host={}", this.host);
                    this.failed = true;
                    this.reset();
                    return;
                }
            } catch (Exception e) {
                this.failed = true;
                this.reset();
                return;
            }
        }
        this.failed = false;
        if (numBlocks != startBlocks) {
            log.info("Chain for {} updated to length= {}  total_work= {}", this.host, this.chainLength, this.totalWork);
        }
    }

    private Optional<Long> getCurrentBlockCount(String host2) {
        return null;
    }
}
