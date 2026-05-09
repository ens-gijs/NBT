package io.github.ensgijs.nbt.mca.io;

import java.io.IOException;

public class CorruptMcaFileException extends IOException {
    public CorruptMcaFileException() {
        super();
    }

    public CorruptMcaFileException(String message) {
        super(message);
    }

    public CorruptMcaFileException(String message, Throwable cause) {
        super(message, cause);
    }

    public CorruptMcaFileException(Throwable cause) {
        super(cause);
    }
}
