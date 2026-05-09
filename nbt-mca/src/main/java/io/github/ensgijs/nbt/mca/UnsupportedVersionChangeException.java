package io.github.ensgijs.nbt.mca;

/**
 * Thrown when the requested data version change is not supported because it would require a data upgrade or downgrade
 * that is itself, not supported.
 */
public class UnsupportedVersionChangeException extends IllegalArgumentException {
    public UnsupportedVersionChangeException() {
        super();
    }

    public UnsupportedVersionChangeException(DataVersion criticalCrossing, int fromVersion, int toVersion) {
        super(String.format("Migrating data version from %d to %d not supported because it crosses %s",
                fromVersion, toVersion, criticalCrossing));
    }

    public UnsupportedVersionChangeException(String message) {
        super(message);
    }

    public UnsupportedVersionChangeException(String message, Throwable cause) {
        super(message, cause);
    }

    public UnsupportedVersionChangeException(Throwable cause) {
        super(cause);
    }
}
