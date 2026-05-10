package io.github.ensgijs.nbt.mca;

import io.github.ensgijs.nbt.tag.CompoundTag;
import io.github.ensgijs.nbt.mca.util.TagWrapper;

import java.util.Objects;

/**
 * <p>
 *     <b>In summary</b>, if you have changed the block at a POI location, or altered the blocks in a {@link TerrainChunk}
 *     in such a way that may have added or removed POI blocks you have a few options (MC 1.14+)
 *     <ol>
 *         <li>calculate an accurate new poi state yourself by removing and adding poi records on the {@link PoiChunk},
 *         and to be truly accurate you must also modify villager "brains", but they will figure things out when
 *         they try to interact with their poi's and find them of the wrong type.</li>
 *         <li>invalidate the poi sub-chunk within which you have made alterations with {@link PoiChunk#invalidateSection(int)}</li>
 *         <li>remove the poi chunk from the poi file with {@link McaPoiFile#removeChunk(int)} or {@link McaPoiFile#removeChunk(int, int)}</li>
 *         <li>delete the entire poi mca file</li>
 *     </ol>
 *     All of the options, other than calculating poi state yourself, will trigger Minecraft to re-calculate poi records
 *     without causing errant behavior. The worst thing you can do is to do nothing - Minecraft will eventually notice
 *     but it may cause "strange behavior" and various WTF's until the game sorts itself out.
 * <p><b>About this class</b><br>
 * A record as found in POI MCA files (points of interest). Hashable and equatable, but does not consider
 * {@code freeTickets} in those operations as that field is largely MC internal state. POI mca files were added in
 * MC 1.14 to improve villager performance and only contained locations of blocks villagers interacted with. Over time
 * POI mca has evolved to include locations of other block types to optimize game performance - such as improving
 * nether portal lag by storing portal block locations in the poi files so the game doesn't need to scan every block
 * in every chunk until it finds a destination portal.
 * </p><p>At time of writing, 1.14 to 1.17.1, this class exposes all poi record fields. For now, there is no support for
 * reading or storing extra fields which this class does not wrap.</p>
 * <p>POI types As of 1.17
 * <ul>
 *     <li>minecraft:unemployed <i>- does not map to a block type</i></li>
 *     <li>minecraft:armorer <i>- block: blast_furnace</i></li>
 *     <li>minecraft:butcher <i>- block: smoker</i></li>
 *     <li>minecraft:cartographer <i>- block: cartography_table</i></li>
 *     <li>minecraft:cleric <i>- block: brewing_stand</i></li>
 *     <li>minecraft:farmer <i>- block: composter</i></li>
 *     <li>minecraft:fisherman <i>- block: barrel</i></li>
 *     <li>minecraft:fletcher <i>- block: fletching_table</i></li>
 *     <li>minecraft:leatherworker <i>- block: any cauldron block</i></li>
 *     <li>minecraft:librarian <i>- block: lectern</i></li>
 *     <li>minecraft:mason <i>- block: stonecutter</i></li>
 *     <li>minecraft:nitwit <i>- does not map to a block type</i></li>
 *     <li>minecraft:shepherd <i>- block: loom</i></li>
 *     <li>minecraft:toolsmith <i>- block: smithing_table</i></li>
 *     <li>minecraft:weaponsmith <i>- block: grindstone</i></li>
 *     <li>minecraft:home <i>- block: any bed</i></li>
 *     <li>minecraft:meeting <i>- block: bell</i></li>
 *     <li>minecraft:beehive <i>- block: beehive</i></li>
 *     <li>minecraft:bee_nest <i>- block: bee_nest</i></li>
 *     <li>minecraft:nether_portal <i>- block: nether_portal</i></li>
 *     <li>minecraft:lodestone <i>- block: lodestone</i></li>
 *     <li>minecraft:lightning_rod <i>- block: lightning_rod</i></li>
 * </ul>
 *
 * <br>
 * <b>What are "Tickets"?</b>
 * <p>
 *     Tickets are only used for blocks/poi's (points of interest) which villagers interact with. Internally
 *     Minecraft specifies a max tickets for each such poi type. This is the maximum number of villagers which
 *     can "take a ticket" (aka be using that poi at the same time; aka max number of villagers which
 *     can claim that poi and store it in their "brain"). For all villager eligible poi's that limit
 *     is one (1), with the single exception being minecraft:meeting (block minecraft:bell) which has a
 *     limit of 32.
 * </p><p>
 *     Poi entries which are not for villager interaction such as beehives, nether portals,
 *     lighting rods, etc., have a max ticket count of zero (0).
 * </p><p>
 *     A truly valid POI Record is one that satisfies all of the following conditions
 *     <ul>
 *         <li>the block at the poi location is appropriate for the poi type</li>
 *         <li>free tickets is never GT max tickets for that poi type</li>
 *         <li>{@link #getFreeTickets()} equals the count of all villagers who have stored the poi location in their
 *         "brain" subtracted from the max tickets for that poi type</li>
 *     </ul>
 */
public class PoiRecord implements TagWrapper<CompoundTag>, Comparable<PoiRecord> {
    protected String type;
    protected int freeTickets;
    protected int x;
    protected int y;
    protected int z;

    public PoiRecord() { }

    /**
     * copy constructor
     */
    public PoiRecord(PoiRecord other) {
        this.type = other.type;
        this.freeTickets = other.freeTickets;
        this.x = other.x;
        this.y = other.y;
        this.z = other.z;
    }

    public PoiRecord(CompoundTag data) {
        this.freeTickets = data.getInt("free_tickets");
        this.type = data.getString("type");
        int[] pos = data.getIntArray("pos");
        this.x = pos[0];
        this.y = pos[1];
        this.z = pos[2];
    }

    /**
     * Defaults free tickets to result of passing the given type to {@link #maxFreeTickets(String)}
     * @param x world block x
     * @param y world block y - must be a within the absolute maximum limit of blocks
     *          theoretically supportable by chunk sections [-2048..2032)
     * @param z world block z
     * @param type required, poi type name
     */
    public PoiRecord(int x, int y, int z, String type) {
        this(x, y, z, type, maxFreeTickets(type));
    }

    /**
     * @param x world block x
     * @param y world block y - must be a within the absolute maximum limit of blocks
     *          theoretically supportable by chunk sections [-2048..2048)
     * @param z world block z
     * @param type required, poi type name
     * @param freeTickets must be GT 0
     */
    public PoiRecord(int x, int y, int z, String type, int freeTickets) {
        this.type = validateType(type);
        this.freeTickets = validateFreeTickets(freeTickets);
        this.y = validateY(y);
        this.x = x;
        this.z = z;
    }

    private String validateType(String type) {
        if (type == null || type.isEmpty()) {
            throw new IllegalArgumentException("poi type must not be null or empty");
        }
        return type;
    }

    private int validateFreeTickets(int freeTickets) {
        if (freeTickets < 0) {
            throw new IllegalArgumentException("freeTickets must be GE 0");
        }
        return freeTickets;
    }

    private int validateY(int y) {
        if (y < Byte.MIN_VALUE * 16 || y > Byte.MAX_VALUE * 16 + 15) {
            throw new IndexOutOfBoundsException(String.format(
                    "Given Y value %d is out of range for any legal block. Y must be in range [%d..%d]",
                    y, Byte.MIN_VALUE * 16, Byte.MAX_VALUE * 16 + 15));
        }
        return y;
    }

    /**
     * Returns a {@link CompoundTag} representing this record.
     * The tag returned is newly created and not a reference to a tag held by any other object. This is a different
     * behavior than most other {@code getHandle()} implementations.
     */
    @Override
    public CompoundTag updateHandle() {
        CompoundTag data = new CompoundTag();
        data.putInt("free_tickets", freeTickets);
        data.putString("type", type);
        data.putIntArray("pos", new int[] {x, y, z});
        return data;
    }

    /**
     * Returns a {@link CompoundTag} representing this record.
     * The tag returned is newly created and not a reference to a tag held by any other object. This is a different
     * behavior than most other {@code getHandle()} implementations.
     * @return data handle, never null
     */
    @Override
    public CompoundTag getHandle() {
        return updateHandle();
    }

    /**
     * See class doc {@link PoiRecord}
     */
    public int getFreeTickets() {
        return freeTickets;
    }

    /**
     * See class doc {@link PoiRecord}
     */
    public PoiRecord setFreeTickets(int freeTickets) {
        this.freeTickets = validateFreeTickets(freeTickets);
        return this;
    }

    /**
     * Sets freeTickets to the default max free tickets for this poi type.
     * see class doc {@link PoiRecord}
     */
    public PoiRecord resetFreeTickets() {
        this.freeTickets = maxFreeTickets(this.type);
        return this;
    }

    /** Type of the point, for example: minecraft:home, minecraft:meeting, minecraft:butcher, minecraft:nether_portal */
    public String getType() {
        return type;
    }

    /** Type of the point, for example: minecraft:home, minecraft:meeting, minecraft:butcher, minecraft:nether_portal */
    public PoiRecord setType(String type) {
        this.type = validateType(type);
        return this;
    }

    /** world x location */
    public int getX() {
        return x;
    }

    /** world x location */
    public PoiRecord setX(int x) {
        this.x = x;
        return this;
    }

    /** world y location */
    public int getY() {
        return y;
    }

    /**
     * @param y must be a within the absolute maximum limit of blocks
     *          theoretically supportable by chunk sections [-2048..2048)
     */
    public PoiRecord setY(int y) {
        this.y = validateY(y);
        return this;
    }

    /** world z location */
    public int getZ() {
        return z;
    }

    /** world z location */
    public PoiRecord setZ(int z) {
        this.z = z;
        return this;
    }

    /**
     * Sets XYZ
     * @param x world block x
     * @param y world block y
     * @param z world block z
     * @return self
     */
    public PoiRecord setYXZ(int x, int y, int z) {
        this.x = x;
        this.y = y;
        this.z = z;
        return this;
    }

    @Override
    public int hashCode() {
        return Objects.hash(type, x, y, z);
    }

    @Override
    public boolean equals(Object other) {
        if (!(other instanceof PoiRecord)) return false;
        PoiRecord that = (PoiRecord) other;
        return this.y == that.y && this.x == that.x && this.z == that.z && Objects.equals(this.type, that.type);
    }

    @Override
    public int compareTo(PoiRecord other) {
        if (other == null) {
            return -1;
        }
        return Integer.compare(this.y, other.y);
    }

    public boolean matches(int x, int y, int z) {
        return this.y == y && this.x == x && this.z == z;
    }

    public boolean matches(String type) {
        return this.type.equals(type);
    }

    public int getSectionY() {
        return this.y >> 4;
    }

    /**
     * Gets the default max free tickets for the given poi type.
     * @param poiType poi type - NOT block type
     * @return default (vanilla) max free tickets for the given type.
     */
    public static int maxFreeTickets(String poiType) {
        switch (poiType) {
            case "minecraft:unemployed":
            case "minecraft:armorer":
            case "minecraft:butcher":
            case "minecraft:cartographer":
            case "minecraft:cleric":
            case "minecraft:farmer":
            case "minecraft:fisherman":
            case "minecraft:fletcher":
            case "minecraft:leatherworker":
            case "minecraft:librarian":
            case "minecraft:mason":
            case "minecraft:nitwit":
            case "minecraft:shepherd":
            case "minecraft:toolsmith":
            case "minecraft:weaponsmith":
            case "minecraft:home":
                return 1;
            case "minecraft:meeting":
                return 32;
        }
        return 0;
    }
}
