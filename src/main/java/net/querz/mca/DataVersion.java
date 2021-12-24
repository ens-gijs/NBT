package net.querz.mca;

import java.util.Arrays;
import java.util.Comparator;

// source: version.json file, found in the root directory of the client and server jars
// table of versions can also be found on https://minecraft.fandom.com/wiki/Data_version#List_of_data_versions

/**
 * List of significant MC versions and MCA data versions.
 * Non-full release versions are intended for use in data handling logic.
 * The set of non-full release versions is not, and does not need to be, the complete set of all versions - only those
 * which introduce changes to the MCA data structure are useful. BUT - we humans do love completeness so
 * the list might be made complete some day and completeness would be useful for map viewers / editors.
 */
public enum DataVersion {
    // Kept in ASC order (unit test enforced)
    UNKNOWN(0, 0, 0),
    JAVA_1_9_0(169, 9, 0),
    JAVA_1_9_1(175, 9, 1),
    JAVA_1_9_2(176, 9, 2),
    JAVA_1_9_3(183, 9, 3),
    JAVA_1_9_4(184, 9, 4),

    JAVA_1_10_0(510, 10, 0),
    JAVA_1_10_1(511, 10, 1),
    JAVA_1_10_2(512, 10, 2),

    JAVA_1_11_0(819, 11, 0),
    JAVA_1_11_1(921, 11, 1),
    JAVA_1_11_2(922, 11, 2),

    JAVA_1_12_0(1139, 12, 0),
    JAVA_1_12_1(1241, 12, 1),
    JAVA_1_12_2(1343, 12, 2),

    JAVA_1_13_0(1519, 13, 0),
    JAVA_1_13_1(1628, 13, 1),
    JAVA_1_13_2(1631, 13, 2),

    /** /poi/r.X.Z.mca files introduced */
    JAVA_1_14_0(1952, 14, 0),
    JAVA_1_14_1(1957, 14, 1),
    JAVA_1_14_2(1963, 14, 2),
    JAVA_1_14_3(1968, 14, 3),
    JAVA_1_14_4(1976, 14, 4),

    /**
     * 3D Biomes added. Biomes array in the  Level tag for each chunk changed
     * to contain 1024 integers instead of 256 see {@link Chunk}
     */
    JAVA_1_15_19W36A(2203, 15, -1, "19w36a"),
    JAVA_1_15_0(2225, 15, 0),
    JAVA_1_15_1(2227, 15, 1),
    JAVA_1_15_2(2230, 15, 2),

    /** Block pallet packing changed in this version - see {@link Section} */
    JAVA_1_16_20W17A(2529, 16, 0, "20w17a"),
    JAVA_1_16_0(2566, 16, 0),
    JAVA_1_16_1(2567, 16, 1),
    JAVA_1_16_2(2578, 16, 2),
    JAVA_1_16_3(2580, 16, 3),
    JAVA_1_16_4(2584, 16, 4),
    JAVA_1_16_5(2586, 16, 5),

    /**
     * /entities/r.X.Z.mca files introduced.
     * Entities no longer inside region/r.X.Z.mca - except in un-migrated chunks of course.
     * <p>https://www.minecraft.net/en-us/article/minecraft-snapshot-20w45a</p>
     */
    JAVA_1_17_20W45A(2681, 17, 0, "20w45a"),

    JAVA_1_17_0(2724, 17, 0),
    JAVA_1_17_1(2730, 17, 1),

    // fist experimental 1.18 build
    JAVA_1_18_XS1(2825, 18, 0, "XS1"),

    /**
     * https://www.minecraft.net/en-us/article/minecraft-snapshot-21w39a
     * <ul>
     * <li>Level.Sections[].BlockStates &amp; Level.Sections[].Palette have moved to a container structure in Level.Sections[].block_states
     * <li>Level.Biomes are now paletted and live in a similar container structure in Level.Sections[].biomes
     * <li>Level.CarvingMasks[] is now long[] instead of byte[]
     * </ul>
     */
    JAVA_1_18_21W39A(2836, 18, 0, "21w39a"),

    /**
     * https://www.minecraft.net/en-us/article/minecraft-snapshot-21w43a
     * <ul>
     * <li>Removed chunk’s Level and moved everything it contained up
     * <li>Chunk’s Level.Entities has moved to entities -- entities are stored in the terrain region file during chunk generation
     * <li>Chunk’s Level.TileEntities has moved to block_entities
     * <li>Chunk’s Level.TileTicks and Level.ToBeTicked have moved to block_ticks
     * <li>Chunk’s Level.LiquidTicks and Level.LiquidsToBeTicked have moved to fluid_ticks
     * <li>Chunk’s Level.Sections has moved to sections
     * <li>Chunk’s Level.Structures has moved to structures
     * <li>Chunk’s Level.Structures.Starts has moved to structures.starts
     * <li>Chunk’s Level.Sections[].BlockStates and Level.Sections[].Palette have moved to a container structure in sections[].block_states
     * <li>Chunk’s Level.Biomes are now paletted and live in a similar container structure in sections[].biomes
     * <li>Added yPos the minimum section y position in the chunk
     * <li>Added below_zero_retrogen containing data to support below zero generation
     * <li>Added blending_data containing data to support blending new world generation with existing chunks
     * </ul>
     */
    JAVA_1_18_21W43A(2844, 18, 0, "21w43a"),
    JAVA_1_18_PRE1(2847, 18, 0, "PRE1"),
    JAVA_1_18_PRE2(2848, 18, 0, "PRE2"),
    JAVA_1_18_PRE3(2849, 18, 0, "PRE3"),
    JAVA_1_18_PRE4(2850, 18, 0, "PRE4"),
    JAVA_1_18_PRE5(2851, 18, 0, "PRE5"),
    JAVA_1_18_PRE6(2853, 18, 0, "PRE6"),
    JAVA_1_18_PRE7(2854, 18, 0, "PRE7"),
    JAVA_1_18_PRE8(2855, 18, 0, "PRE8"),
    JAVA_1_18_RC1(2856, 18, 0, "RC1"),
    JAVA_1_18_RC2(2857, 18, 0, "RC2"),
    JAVA_1_18_RC3(2858, 18, 0, "RC3"),
    JAVA_1_18_RC4(2859, 18, 0, "RC4"),
    JAVA_1_18_0(2860, 18, 0),
    JAVA_1_18_1_PRE1(2861, 18, 1, "PRE1"),
    JAVA_1_18_1_RC1(2862, 18, 1, "RC1"),
    JAVA_1_18_1_RC2(2863, 18, 1, "RC2"),
    JAVA_1_18_1_RC3(2864, 18, 1, "RC3"),
    JAVA_1_18_1(2865, 18, 1);


    private static final int[] ids;
    private static final DataVersion latestFullReleaseVersion;
    private final int id;
    private final int minor;
    private final int patch;
    private final boolean isFullRelease;
    private final String buildDescription;
    private final String str;

    static {
        ids = Arrays.stream(values()).sorted().mapToInt(DataVersion::id).toArray();
        latestFullReleaseVersion = Arrays.stream(values())
                .sorted(Comparator.reverseOrder())
                .filter(DataVersion::isFullRelease)
                .findFirst().get();
    }

    DataVersion(int id, int minor, int patch) {
        this(id, minor, patch, null);
    }

    /**
     * @param id data version
     * @param minor minor version
     * @param patch patch number, LT0 to indicate this data version is not a full release version
     * @param buildDescription Suggested convention (unit test enforced): <ul>
     *                         <li>NULL (given value ignored) for full release</li>
     *                         <li>CT# for combat tests (e.g. CT6, CT6b)</li>
     *                         <li>XS# for experimental snapshots(e.g. XS1, XS2)</li>
     *                         <li>YYwWWz for weekly builds (e.g. 21w37a, 21w37b)</li>
     *                         <li>PRE# for pre-releases (e.g. PRE1, PRE2)</li>
     *                         <li>RC# for release candidates (e.g. RC1, RC2)</li></ul>
     */
    DataVersion(int id, int minor, int patch, String buildDescription) {
        this.isFullRelease = buildDescription == null || "FINAL".equalsIgnoreCase(buildDescription);
        if (!isFullRelease && buildDescription.isEmpty())
            throw new IllegalArgumentException("buildDescription required for non-full releases");
        this.id = id;
        this.minor = minor;
        this.patch = patch;
        this.buildDescription = isFullRelease ? "FINAL" : buildDescription;
        if (minor > 0) {
            StringBuilder sb = new StringBuilder();
            sb.append(id).append(" (1.").append(minor);
            if (patch > 0) sb.append('.').append(patch);
            if (!isFullRelease) sb.append(' ').append(buildDescription);
            this.str = sb.append(')').toString();
        } else {
            this.str = name();
        }
    }

    public int id() {
        return id;
    }

    /**
     * Version format: major.minor.patch
     */
    public int major() {
        return 1;
    }

    /**
     * Version format: major.minor.patch
     */
    public int minor() {
        return minor;
    }

    /**
     * Version format: major.minor.patch
     */
    public int patch() {
        return patch;
    }

    /**
     * True for full release.
     * False for all other builds (e.g. experimental, pre-releases, and release-candidates).
     */
    public boolean isFullRelease() {
        return isFullRelease;
    }

    /**
     * Description of the minecraft build which this {@link DataVersion} refers to.
     * You'll find {@link #toString()} to be more useful in general.
     * <p>Convention used: <ul>
     * <li>"FULL" for full release</li>
     * <li>YYwWWz for weekly builds (e.g. 21w37a, 21w37b)</li>
     * <li>CT# for combat tests (e.g. CT6, CT6b)</li>
     * <li>XS# for experimental snapshots(e.g. XS1, XS2)</li>
     * <li>PR# for pre-releases (e.g. PR1, PR2)</li>
     * <li>RC# for release candidates (e.g. RC1, RC2)</li></ul>
     */
    public String getBuildDescription() {
        return buildDescription;
    }

    /**
     * TRUE as of 1.14
     * Indicates if point of interest .mca files exist. E.g. 'poi/r.0.0.mca'
     */
    public boolean hasPoiMca() {
        return minor >= 14;
    }

    /**
     * TRUE as of 1.17
     * Entities were pulled out of terrain 'region/r.X.Z.mca' files into their own .mca files. E.g. 'entities/r.0.0.mca'
     */
    public boolean hasEntitiesMca() {
        return minor >= 17;
    }

    public static DataVersion bestFor(int dataVersion) {
        int found = Arrays.binarySearch(ids, dataVersion);
        if (found < 0) {
            found = (found + 2) * -1;
            if (found < 0) return UNKNOWN;
        }
        return values()[found];
    }

    /**
     * @return The latest full release version defined.
     */
    public static DataVersion latest() {
        return latestFullReleaseVersion;
    }

    @Override
    public String toString() {
        return str;
    }

    /**
     * Indicates if this version would be crossed by the transition between versionA and versionB.
     * This is useful for determining if a data upgrade or downgrade would be required to support
     * changing from versionA to versionB. The order of A and B don't matter.
     *
     * <p>When using this function, call it on the data version in which a change exists. For
     * example if you need to know if changing from A to B would require changing to/from 3D
     * biomes then use {@code JAVA_1_15_19W36A.isCrossedByTransition(A, B)} as
     * {@link #JAVA_1_15_19W36A} is the version which added 3D biomes.</p>
     *
     * <p>In short, if this function returns true then the act of changing data versions from A
     * to B can be said to "cross" this version which is an indication that such a change should
     * either be considered illegal or that upgrade/downgrade action is required.</p>
     *
     * @param versionA older or newer data version than B
     * @param versionB older or newer data version than A
     * @return true if chaining from version A to version B, or form B to A, would result in
     * crossing this version. This version is considered to be crossed if {@code A != B} and
     * {@code min(A, B) < this.id <= max(A, B)}
     * @see #throwUnsupportedVersionChangeIfCrossed(int, int)
     */
    public boolean isCrossedByTransition(int versionA, int versionB) {
        if (versionA == versionB) return false;
        if (versionA < versionB) {
            return versionA < id && id <= versionB;
        } else {
            return versionB < id && id <= versionA;
        }
    }

    /**
     * Throws {@link UnsupportedVersionChangeException} if {@link #isCrossedByTransition(int, int)}
     * were to return true for the given arguments.
     */
    public void throwUnsupportedVersionChangeIfCrossed(int versionA, int versionB) {
        if (isCrossedByTransition(versionA, versionB)) {
            throw new UnsupportedVersionChangeException(this, versionA, versionB);
        }
    }
}
