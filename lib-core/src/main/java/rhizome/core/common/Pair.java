package rhizome.core.common;

public class Pair<L extends Comparable<L>, R> implements Comparable<Pair<L, R>> {
    private final L left;
    private final R right;

    public Pair(L left, R right) {
        this.left = left;
        this.right = right;
    }

    public L getLeft() {
        return left;
    }

    public R getRight() {
        return right;
    }

    public static <L extends Comparable<L>, R> Pair<L, R> of(L left, R right) {
        return new Pair<>(left, right);
    }

    @Override
	public int compareTo(Pair<L, R> o) {
		return left.compareTo(o.left);
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) {
			return true;
		}
		if (o == null || getClass() != o.getClass()) {
			return false;
		}

		Pair<?, ?> pair = (Pair<?, ?>) o;

		return left.equals(pair.left) && right.equals(pair.right);
	}

	@Override
	public int hashCode() {
		return 31 * left.hashCode() + right.hashCode();
	}

	@Override
	public String toString() {
		return "Pair{left=" + left + ", right=" + right + '}';
	}
}
