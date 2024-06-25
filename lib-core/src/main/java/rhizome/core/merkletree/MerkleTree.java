package rhizome.core.merkletree;
import lombok.Getter;
import lombok.Setter;
import rhizome.core.crypto.SHA256Hash;
import rhizome.core.transaction.Transaction;


import static rhizome.core.common.Crypto.concatHashes;

import java.util.*;

@Getter @Setter
public class MerkleTree {
    private HashTree root;
    private Map<SHA256Hash, HashTree> fringeNodes;

    public MerkleTree() {
        this.root = null;
        this.fringeNodes = new HashMap<>();
    }

    public void setItems(List<Transaction> items) {
        var sortedItems = new ArrayList<>(items);
        sortedItems.sort(Comparator.comparing(Transaction::hash).reversed());

        var q = new LinkedList<HashTree>();
        sortedItems.forEach(item -> {
            var hash = item.hash();
            var node = new HashTree(hash);
            fringeNodes.put(hash, node);
            q.add(node);
        });

        if (q.size() % 2 == 1) {
            q.add(new HashTree(q.peek().hash()));
        }

        while (q.size() > 1) {
            var a = q.poll();
            var b = q.poll();
            var parent = new HashTree(concatHashes(a.hash(), b.hash(), false, false));
            a.parent(parent);
            b.parent(parent);
            parent.left(a);
            parent.right(b);
            q.add(parent);
        }

        this.root = q.poll();
    }

    public SHA256Hash getRootHash() {
        return this.root.hash();
    }

    public Optional<HashTree> getMerkleProof(Transaction t) {
        return Optional.ofNullable(fringeNodes.get(t.hash()))
                       .map(f -> buildProof(f, null));
    }
    
    private HashTree buildProof(HashTree fringe, HashTree previousNode) {
        var result = new HashTree(fringe.hash());
        if (previousNode != null) {
            if (fringe.left() != null && fringe.left() != previousNode) {
                result.left(fringe.left());
                result.right(previousNode);
            } else if (fringe.right() != null && fringe.right() != previousNode) {
                result.right(fringe.right());
                result.left(previousNode);
            }
        }
        if (fringe.parent() != null) {
            return buildProof(fringe.parent(), fringe);
        } else {
            return result;
        }
    }

    @Getter
    @Setter
    public static class HashTree {
        private SHA256Hash hash;
        private HashTree parent;
        private HashTree left;
        private HashTree right;

        public HashTree(SHA256Hash hash) {
            this.hash = hash;
            this.parent = this.left = this.right = null;
        }
    }

}
