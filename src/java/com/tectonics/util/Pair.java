package com.tectonics.util;

/**
 * Utility class to represent a pair of objects, potentially of different types.
 */
public class Pair<T, U> {

    /**
     * The first object
     */
    public final T first;

    /**
     * The second object
     */
    public final U second;

    /**
     * @param first the first object
     * @param second the second object
     */
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
