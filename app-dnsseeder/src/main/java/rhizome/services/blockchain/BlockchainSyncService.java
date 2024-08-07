package rhizome.services.blockchain;

import io.activej.eventloop.Eventloop;
import io.activej.promise.Promise;
import rhizome.core.services.BaseService;
import rhizome.net.service.PeerManagerService;

public class BlockchainSyncService extends BaseService {
    
    private final PeerManagerService peerManagerService;
    
    private BlockchainSyncService(Eventloop eventloop, PeerManagerService peerManagerService) {
        super(eventloop);
        this.peerManagerService = peerManagerService;
    }

    public static BlockchainSyncService create(Eventloop eventloop, PeerManagerService peerManagerService) {
        return new BlockchainSyncService(eventloop, peerManagerService)
            .addRoutine(BlockchainSyncService::sync)
            .build();
    }

    private static Promise<Void> sync() {
        return Promise.complete();
    }
}
