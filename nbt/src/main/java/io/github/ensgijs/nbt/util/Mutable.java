package io.github.ensgijs.nbt.util;

/**
 * Simple utility for passing mutable objects into/out of lambdas, like {@link java.util.Optional}, but mutable.
 * @param <T> Mutable type
 */
public class Mutable<T> {

    T value;

    public Mutable() { }

    public Mutable(T value) {
        this.value = value;
    }

    public T get() {
        return value;
    }

    public void set(T value) {
        this.value = value;
    }
}
