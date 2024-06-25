package rhizome.core.blockchain;

import rhizome.net.p2p.peer.PeerOLD;

// TODO: basic translation from C++ , need refactor
public class ChainSync implements Runnable {
    private final PeerOLD chain;

    public ChainSync(PeerOLD chain) {
        this.chain = chain;
    }

    @Override
    public void run() {
        while (true) {
            if (!chain.triedBlockStoreCache() && chain.blockStore() != null) {
                long chainLength = chain.blockStore().getBlockCount();
                for (long i = 1; i <= chainLength; i++) {
                    chain.blockHashes().add(chain.blockStore().getBlock((int)i).hash());
                }
                chain.totalWork(chain.blockStore().getTotalWork());
                chain.chainLength(chainLength);
                chain.triedBlockStoreCache(true);
                chain.load();
            } else {
                chain.load();
            }
            try {
                Thread.sleep(10000);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return;
            }
        }
    }
}
