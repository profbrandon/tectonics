public class Pair<T, U> {
    public final T first;
    public final U second;

    public Pair(final T first, final U second) {
        this.first = first;
        this.second = second;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof Pair) {
            final Pair<T, U> pair = (Pair<T, U>) obj;
            return first.equals(pair.first) && second.equals(pair.second);
        }
        return false;
    }

    @Override
    public String toString() {
        return "Pair(" + this.first.toString() + ", " + this.second.toString() + ")";
    }

    @Override
    public int hashCode() {
        return this.first.hashCode() * 97 + this.second.hashCode();
    }
}
