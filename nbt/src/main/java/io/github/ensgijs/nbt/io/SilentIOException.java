package io.github.ensgijs.nbt.io;

import java.io.IOException;

/**
 * Used to wrap/throw IOExceptions in contests where checked exceptions cannot be used,
 * such as when implementing Iterator.next()
 */
public class SilentIOException extends RuntimeException {
    public SilentIOException() {
        super();
    }

    public SilentIOException(String message) {
        super(message);
    }

    public SilentIOException(String message, IOException cause) {
        super(message, cause);
    }

    public SilentIOException(IOException cause) {
        super(cause);
    }
}
