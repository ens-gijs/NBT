package io.github.ensgijs.nbt.io;

@FunctionalInterface
public interface ExceptionTriConsumer<T, U, V, E extends Exception> {

	void accept(T t, U u, V v) throws E;
}
