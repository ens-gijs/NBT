package io.github.ensgijs.nbt.mca;

/**
 * Thrown when an attempt is made to do something that can't be supported by a data version.
 * <p>The provided version information is automatically appended to the exception message.</p>
 */
public class VersionLacksSupportException extends RuntimeException {
    private final int unsupportedDataVersion;
    private final DataVersion minimumSupportedDataVersion;  // nullable
    private final DataVersion maximumSupportedDataVersion;  // nullable

    private static String addVersionInfo(String message, int udv, DataVersion mindv, DataVersion maxdv) {
        String s = (message != null ? message + " " : "") +"[unsupported data version: " + udv;
        if (mindv != null) s += "; minimum supported data version: " + mindv;
        if (maxdv != null) s += "; maximum supported data version: " + maxdv;
        s += "]";
        return s;
    }

    public VersionLacksSupportException(
            int unsupportedDataVersion, DataVersion minimumSupportedDataVersion, DataVersion maximumSupportedDataVersion
    ) {
        super(addVersionInfo(null, unsupportedDataVersion, minimumSupportedDataVersion, maximumSupportedDataVersion));
        this.unsupportedDataVersion = unsupportedDataVersion;
        this.minimumSupportedDataVersion = minimumSupportedDataVersion;
        this.maximumSupportedDataVersion = maximumSupportedDataVersion;
    }

    public VersionLacksSupportException(
            int unsupportedDataVersion, DataVersion minimumSupportedDataVersion, DataVersion maximumSupportedDataVersion, String message
    ) {
        super(addVersionInfo(message, unsupportedDataVersion, minimumSupportedDataVersion, maximumSupportedDataVersion));
        this.unsupportedDataVersion = unsupportedDataVersion;
        this.minimumSupportedDataVersion = minimumSupportedDataVersion;
        this.maximumSupportedDataVersion = maximumSupportedDataVersion;
    }

    public VersionLacksSupportException(
            int unsupportedDataVersion, DataVersion minimumSupportedDataVersion, DataVersion maximumSupportedDataVersion, String message, Throwable cause
    ) {
        super(addVersionInfo(message, unsupportedDataVersion, minimumSupportedDataVersion, maximumSupportedDataVersion), cause);
        this.unsupportedDataVersion = unsupportedDataVersion;
        this.minimumSupportedDataVersion = minimumSupportedDataVersion;
        this.maximumSupportedDataVersion = maximumSupportedDataVersion;
    }

    public VersionLacksSupportException(
            int unsupportedDataVersion, DataVersion minimumSupportedDataVersion, DataVersion maximumSupportedDataVersion, Throwable cause) {
        super(addVersionInfo(null, unsupportedDataVersion, minimumSupportedDataVersion, maximumSupportedDataVersion), cause);
        this.unsupportedDataVersion = unsupportedDataVersion;
        this.minimumSupportedDataVersion = minimumSupportedDataVersion;
        this.maximumSupportedDataVersion = maximumSupportedDataVersion;
    }

    public int unsupportedDataVersion() {
        return unsupportedDataVersion;
    }

    /** nullable */
    public DataVersion minimumSupportedDataVersion() {
        return minimumSupportedDataVersion;
    }

    /** nullable */
    public DataVersion maximumSupportedDataVersion() {
        return maximumSupportedDataVersion;
    }
}
