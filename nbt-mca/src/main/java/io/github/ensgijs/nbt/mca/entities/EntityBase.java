package io.github.ensgijs.nbt.mca.entities;

import io.github.ensgijs.nbt.tag.CompoundTag;
import io.github.ensgijs.nbt.tag.ListTag;
import io.github.ensgijs.nbt.mca.util.VersionedDataContainer;
import io.github.ensgijs.nbt.util.ArgValidator;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

/**
 * Provides a rich default implementation of {@link Entity} that exposes all properties of all
 * entities in vanilla Minecraft and behaves in an intelligent way, making it easier to manipulate them.
 * <p>
 *     <b>Features worth special note</b>
 *     <ul>
 *         <li>{@link #generateNewUuid()} - generates a new uuid for this entity and all mounted passengers,
 *              and their passengers, and so on.</li>
 *         <li>{@link #setPosition(double, double, double)}, {@link #setX(double)}, {@link #setY(double)},
 *              {@link #setZ(double)}, {@link #movePosition(double, double, double)} -
 *              cascade relative position changes to all passengers, and their passengers, and so on.</li>
 *         <li>{@link #setMotion(double, double, double)}, {@link #setMotionDX(double)}, {@link #setMotionDY(double)},
 *              {@link #setMotionDZ(double)} - cascade changes to passengers, and their passengers, and so on.</li>
 *         <li>{@link #addPassenger(Entity)} - sets the new passengers motion to match their mount and if
 *              the passenger has no valid position set, updates it to match their mount as well.</li>
 *         <li>{@link #setRotationYaw(float)} and {@link #setRotationPitch(float)} - automatically and intelligently
 *         keep values in range, [0..360) and [-90..90] respectively. These functions DO NOT affect passengers.</li>
 *         <li>{@link #getFacingCardinalAngle()} and {@link #setFacingCardinalAngle(float)} -
 *              convenience functions where north is 0 deg, east is 90 deg, south is 180 deg, and west is 270 deg.
 *              These functions are just wrappers around {@link #getRotationYaw()} and {@link #setRotationYaw(float)}
 *              that perform the simple 180 deg rotation for you - because working with yaw angles is nonsensical.
 *         </li>
 *     </ul>
 */
public class EntityBase implements Entity, VersionedDataContainer {


    protected CompoundTag data;
    protected int dataVersion;
    /** not null */
    protected String id;
    /** nullable, if not set {@link #updateHandle()} must calculate a random UUID and assign it. UUID of ZERO must also be treated as unset */
    protected UUID uuid;
    /** Note {@link Entity#AIR_UNSET} is used as sentinel value indicating no value. */
    protected short air = AIR_UNSET;
    protected int portalCooldown;
    protected float fallDistance;
    protected short fireTicks = -1;
    protected int ticksFrozen;
    /** nullable, otherwise text nbt */
    protected String customName;
    protected boolean isCustomNameVisible;
    protected boolean isInvulnerable;
    protected boolean isSilent;
    protected boolean isGlowing;
    protected boolean isOnGround;
    protected boolean noGravity;
    protected boolean hasVisualFire;
    protected double x = Double.NaN;
    protected double y = Double.NaN;
    protected double z = Double.NaN;
    protected float yaw;
    protected float pitch;
    // motion
    protected double dx;
    protected double dy;
    protected double dz;
    /** nullable */
    protected List<String> scoreboardTags;
    /** nullable */
    protected List<Entity> passengers;

    public EntityBase(int dataVersion) {
        this.dataVersion = ArgValidator.check(dataVersion, dataVersion >= 0);
        this.data = new CompoundTag();
    }

    public EntityBase(int dataVersion, String id) {
        this.dataVersion = ArgValidator.check(dataVersion, dataVersion > 0);
        this.id = ArgValidator.requireNotEmpty(id);
        this.data = new CompoundTag();
    }

    public EntityBase(int dataVersion, String id, double x, double y, double z) {
        this(dataVersion, id, x, y, z, 0, 0);
    }

    public EntityBase(int dataVersion, String id, double x, double y, double z, float yaw) {
        this(dataVersion, id, x, y, z, yaw, 0);
    }

    public EntityBase(int dataVersion, String id, double x, double y, double z, float yaw, float pitch) {
        this.dataVersion = ArgValidator.check(dataVersion, dataVersion > 0);
        this.id = ArgValidator.requireNotEmpty(id);
        this.data = new CompoundTag();
        this.x = x;
        this.y = y;
        this.z = z;
        this.yaw = EntityUtil.normalizeYaw(yaw);
        this.pitch = EntityUtil.clampPitch(pitch);
    }

    /**
     * Copy constructor.
     * <ul>
     *     <li>Performs a DEEP COPY of this entity and all passengers.
     *     <li>For passengers, {@link EntityFactory#create(CompoundTag, int)} is invoked to create strongly typed
     *         entity instances instead of relying on each passengers clone implementation.
     *     <li>DOES NOT copy UUID's of self or passengers, instead calls {@link #generateNewUuid()}
     *         to ensure that each gets a new UUID.
     *     <li>Triggers {@code other.updateHandle()} which may cause {@link #updateHandle()} to throw
     *         {@link IllegalStateException} if it is not in a valid state.
     *     <li>New object receives a new {@code data} {@link CompoundTag} cloned from the updated handle of
     *         {@code other}.
     * </ul>
     * @param other Object to clone.
     */
    public EntityBase(EntityBase other) {
        // need to call update handle to make copying passengers clean and tidy
        this.data = other.updateHandle().clone();
        this.dataVersion = other.dataVersion;
        this.id = ArgValidator.requireNotEmpty(other.id);
        this.uuid = null;
        this.portalCooldown = other.portalCooldown;
        this.fallDistance = other.fallDistance;
        this.ticksFrozen = other.ticksFrozen;
        this.customName = other.customName;
        this.isCustomNameVisible = other.isCustomNameVisible;
        this.isInvulnerable = other.isInvulnerable;
        this.isSilent = other.isSilent;
        this.isGlowing = other.isGlowing;
        this.isOnGround = other.isOnGround;
        this.noGravity = other.noGravity;
        this.hasVisualFire = other.hasVisualFire;
        this.x = other.x;
        this.y = other.y;
        this.z = other.z;
        this.yaw = other.yaw;
        this.pitch = other.pitch;
        this.dx = other.dx;
        this.dy = other.dy;
        this.dz = other.dz;
        this.scoreboardTags = other.scoreboardTags == null ? null : new ArrayList<>(other.scoreboardTags);
        this.passengers = !this.data.containsKey("Passengers") ? null
                : StreamSupport.stream(this.data.getListTag("Passengers").asCompoundTagList().spliterator(), false)
                .map(tag -> EntityFactory.create(tag, dataVersion))
                .collect(Collectors.toList());

        this.generateNewUuid();
    }

    public EntityBase(CompoundTag data, int dataVersion) {
        this.data = data;
        this.dataVersion = dataVersion;
        this.id = ArgValidator.requireNotEmpty(data.getString("id", null), "id tag");
        this.uuid = EntityUtil.getUuid(dataVersion, data);
        this.air = data.getShort("Air", AIR_UNSET);
        this.portalCooldown = data.getInt("PortalCooldown");
        this.fallDistance = data.getFloat("FallDistance");
        this.fireTicks = data.getShort("Fire", (short) -1);
        this.ticksFrozen = data.getInt("TicksFrozen", 0);
        this.customName = data.getString("CustomName", null);
        this.isCustomNameVisible = data.getBoolean("CustomNameVisible");
        this.isInvulnerable = data.getBoolean("Invulnerable");
        this.isSilent = data.getBoolean("Silent");
        this.isGlowing = data.getBoolean("Glowing");
        this.hasVisualFire = data.getBoolean("HasVisualFire");
        this.isOnGround = data.getBoolean("OnGround");
        this.noGravity = data.getBoolean("NoGravity");
        double[] pos = data.getDoubleTagListAsArray("Pos");
        if (pos != null && pos.length == 3) {
            this.x = pos[0];
            this.y = pos[1];
            this.z = pos[2];
        }
        float[] rotation = data.getFloatTagListAsArray("Rotation");
        if (rotation != null && rotation.length == 2) {
            this.yaw = rotation[0];
            this.pitch = rotation[1];
        }
        double[] motion = data.getDoubleTagListAsArray("Motion");
        if (motion != null && motion.length == 3) {
            this.dx = motion[0];
            this.dy = motion[1];
            this.dz = motion[2];
        }
        ListTag<CompoundTag> passengersTag = data.getListTagAutoCast("Passengers");
        if (passengersTag != null && passengersTag.size() > 0) {
            this.passengers = new ArrayList<>(passengersTag.size());
            for (CompoundTag ptag : passengersTag) {
                this.passengers.add(EntityFactory.create(ptag, dataVersion));
            }
        }
        this.scoreboardTags = data.getStringTagListValues("Tags");
    }

    // <editor-fold desc="Getters Setters" defaultstate="collapsed">

    @Override
    public int getDataVersion() {
        return dataVersion;
    }

    @Override
    public void setDataVersion(int dataVersion) {
        this.dataVersion = dataVersion;
    }

    /** {@inheritDoc} */
    @Override
    public String getId() {
        return id;
    }

    /** {@inheritDoc} */
    @Override
    public void setId(String id) {
        this.id = ArgValidator.requireNotEmpty(id);
    }

    /** {@inheritDoc} */
    @Override
    public UUID getUuid() {
        return uuid;
    }

    /** {@inheritDoc} */
    @Override
    public void setUuid(UUID uuid) {
        this.uuid = uuid;
    }

    /** {@inheritDoc} */
    @Override
    public UUID generateNewUuid() {
        this.uuid = UUID.randomUUID();
        if (passengers != null) {
            for (Entity passenger : passengers) {
                passenger.generateNewUuid();
            }
        }
        return uuid;
    }

    /** {@inheritDoc} */
    @Override
    public short getAir() {
        return air;
    }

    /** {@inheritDoc} */
    @Override
    public void setAir(short air) {
        this.air = air;
    }

    /** {@inheritDoc} */
    @Override
    public float getFallDistance() {
        return fallDistance;
    }

    /** {@inheritDoc} */
    @Override
    public void setFallDistance(float fallDistance) {
        this.fallDistance = fallDistance;
    }

    /** {@inheritDoc} */
    @Override
    public short getFire() {
        return fireTicks;
    }

    /** {@inheritDoc} */
    @Override
    public void setFire(short fireTicks) {
        this.fireTicks = fireTicks;
    }

    /** {@inheritDoc} */
    @Override
    public int getTicksFrozen() {
        return ticksFrozen;
    }

    /** {@inheritDoc} */
    @Override
    public void setTicksFrozen(int ticksFrozen) {
        this.ticksFrozen = ticksFrozen;
    }

    /** {@inheritDoc} */
    @Override
    public int getPortalCooldown() {
        return portalCooldown;
    }

    /** {@inheritDoc} */
    @Override
    public void setPortalCooldown(int portalCooldown) {
        this.portalCooldown = portalCooldown;
    }

    /** {@inheritDoc} */
    @Override
    public String getCustomName() {
        return customName;
    }

    /** {@inheritDoc} */
    @Override
    public void setCustomName(String customName) {
        this.customName = customName;
    }

    /** {@inheritDoc} */
    @Override
    public boolean isCustomNameVisible() {
        return isCustomNameVisible;
    }

    /** {@inheritDoc} */
    @Override
    public void setCustomNameVisible(boolean visible) {
        this.isCustomNameVisible = visible;
    }

    /** {@inheritDoc} */
    @Override
    public boolean isInvulnerable() {
        return isInvulnerable;
    }

    /** {@inheritDoc} */
    @Override
    public void setInvulnerable(boolean invulnerable) {
        this.isInvulnerable = invulnerable;
    }

    /** {@inheritDoc} */
    @Override
    public boolean isSilent() {
        return isSilent;
    }

    /** {@inheritDoc} */
    @Override
    public void setSilent(boolean silent) {
        this.isSilent = silent;
    }

    /** {@inheritDoc} */
    @Override
    public boolean isGlowing() {
        return isGlowing;
    }

    /** {@inheritDoc} */
    @Override
    public void setGlowing(boolean glowing) {
        this.isGlowing = glowing;
    }

    /** {@inheritDoc} */
    @Override
    public boolean hasNoGravity() {
        return noGravity;
    }

    /** {@inheritDoc} */
    @Override
    public void setNoGravity(boolean noGravity) {
        this.noGravity = noGravity;
    }

    /** {@inheritDoc} */
    @Override
    public boolean isOnGround() {
        return this.isOnGround;
    }

    /** {@inheritDoc} */
    @Override
    public void setOnGround(boolean onGround) {
        this.isOnGround = onGround;
    }

    /** {@inheritDoc} */
    @Override
    public boolean hasVisualFire() {
        return hasVisualFire;
    }

    /** {@inheritDoc} */
    @Override
    public void setHasVisualFire(boolean hasVisualFire) {
        this.hasVisualFire = hasVisualFire;
    }

    /**
     * {@inheritDoc}
     * May be {@link Double#NaN} if unset.
     * @see #isPositionValid()
     */
    @Override
    public double getX() {
        return x;
    }

    /** {@inheritDoc} */
    @Override
    public void setX(final double x) {
        if (passengers != null) {
            if (!Double.isFinite(this.x) || !Double.isFinite(x)) {
                for (Entity passenger : passengers) {
                    passenger.setX(x);
                }
            } else {
                final double dx = x - this.x;
                for (Entity passenger : passengers) {
                    double px = passenger.getX();
                    if (Double.isFinite(px)) {
                        passenger.setX(px + dx);
                    } else {
                        passenger.setX(x);
                    }
                }
            }
        }
        this.x = x;
    }

    /**
     * {@inheritDoc}
     * May be {@link Double#NaN} if unset.
     * @see #isPositionValid()
     */
    @Override
    public double getY() {
        return y;
    }

    /** {@inheritDoc} */
    @Override
    public void setY(final double y) {
        if (passengers != null) {
            if (!Double.isFinite(this.y) || !Double.isFinite(y)) {
                for (Entity passenger : passengers) {
                    passenger.setY(y);
                }
            } else {
                final double dy = y - this.y;
                for (Entity passenger : passengers) {
                    double py = passenger.getY();
                    if (Double.isFinite(py)) {
                        passenger.setY(py + dy);
                    } else {
                        passenger.setY(y);
                    }
                }
            }
        }
        this.y = y;
    }

    /**
     * {@inheritDoc}
     * May be {@link Double#NaN} if unset.
     * @see #isPositionValid()
     */
    @Override
    public double getZ() {
        return z;
    }

    /** {@inheritDoc} */
    @Override
    public void setZ(final double z) {
        if (passengers != null) {
            if (!Double.isFinite(this.z) || !Double.isFinite(z)) {
                for (Entity passenger : passengers) {
                    passenger.setZ(z);
                }
            } else {
                final double dz = z - this.z;
                for (Entity passenger : passengers) {
                    double pz = passenger.getZ();
                    if (Double.isFinite(pz)) {
                        passenger.setZ(pz + dz);
                    } else {
                        passenger.setZ(z);
                    }
                }
            }
        }
        this.z = z;
    }

    /** {@inheritDoc} */
    @Override
    public float getRotationYaw() {
        return yaw;
    }

    /**
     * Sets entity yaw (rotation about the y-axis) in degrees, with 0 being due south. The caller does not need
     * to worry about passing a {@code yaw} value in the range [0..360), the given value will be normalized
     * into the valid range.
     * @see #getRotationYaw()
     * @see #rotate(float)
     */
    @Override
    public void setRotationYaw(float yaw) {
        this.yaw = EntityUtil.normalizeYaw(yaw);
    }

    /** {@inheritDoc} */
    @Override
    public float getRotationPitch() {
        return pitch;
    }

    /** {@inheritDoc} */
    @Override
    public void setRotationPitch(float pitch) {
        this.pitch = EntityUtil.clampPitch(pitch);
    }

    /** {@inheritDoc} */
    @Override
    public void setRotation(float yaw, float pitch) {
        this.yaw = EntityUtil.normalizeYaw(yaw);
        this.pitch = EntityUtil.clampPitch(pitch);
    }

    /** {@inheritDoc} */
    @Override
    public void setPosition(double x, double y, double z) {
        if (!hasPassengers()) {
            this.x = x;
            this.y = y;
            this.z = z;
        } else {
            // it's just easier to handle the passenger move logic this way
            setX(x);
            setY(y);
            setZ(z);
        }
    }
    /** {@inheritDoc} */
    @Override
    public double getMotionDX() {
        return dx;
    }

    /** {@inheritDoc} */
    @Override
    public void setMotionDX(double dx) {
        this.dx = dx;
        if (passengers != null) {
            for (Entity passenger : passengers) {
                passenger.setMotionDX(dx);
            }
        }
    }

    /** {@inheritDoc} */
    @Override
    public double getMotionDY() {
        return dy;
    }

    /** {@inheritDoc} */
    @Override
    public void setMotionDY(double dy) {
        this.dy = dy;
        if (passengers != null) {
            for (Entity passenger : passengers) {
                passenger.setMotionDY(dy);
            }
        }
    }

    /** {@inheritDoc} */
    @Override
    public double getMotionDZ() {
        return dz;
    }

    /** {@inheritDoc} */
    @Override
    public void setMotionDZ(double dz) {
        this.dz = dz;
        if (passengers != null) {
            for (Entity passenger : passengers) {
                passenger.setMotionDZ(dz);
            }
        }
    }

    /** {@inheritDoc} */
    @Override
    public void setMotion(double dx, double dy, double dz) {
        this.dx = dx;
        this.dy = dy;
        this.dz = dz;
        if (passengers != null) {
            for (Entity passenger : passengers) {
                passenger.setMotion(dx, dy, dz);
            }
        }
    }

    /** {@inheritDoc} */
    @Override
    public List<Entity> getPassengers() {
        return passengers;
    }

    /** {@inheritDoc} */
    @Override
    public void setPassengers(List<Entity> passengers) {
        if (passengers != null && !passengers.isEmpty()) {
            for (Entity passenger : passengers) {
                if (passenger != null) {
                    passenger.setMotion(dx, dy, dz);
                    if (!passenger.isPositionValid()) {
                        passenger.setPosition(x, y, z);
                    }
                }
            }
            this.passengers = passengers;
        } else {
            clearPassengers();
        }
    }

    /**
     * {@inheritDoc}
     * <p>The caller is generally responsible for ensuring that the positions of passengers make sense or can be
     * corrected by the game. However, if the given passenger does not satisfy {@link #isPositionValid()}
     * then its position will be set to be the same as this entities position (which only helps if this entity
     * satisfies {@link #isPositionValid()}).</p>
     * <p>Also sets passenger motion to match this entities motion.</p>
     * @throws IndexOutOfBoundsException if {@link #setPassengers(List)} was given a list wrapped array
     * @throws UnsupportedOperationException if {@link #setPassengers(List)} was given an unmodifiable list
     */
    @Override
    public void addPassenger(Entity passenger) {
        ArgValidator.requireValue(passenger);
        ArgValidator.check(passenger != this);  // at least prevent direct recursion
        if (passengers == null) {
            passengers = new ArrayList<>();
        }
        if (!passenger.isPositionValid()) {
            passenger.setPosition(x, y, z);
        }
        passenger.setMotion(dx, dy, dz);
        passengers.add(passenger);
    }

    /** {@inheritDoc} */
    @Override
    public void clearPassengers() {
        passengers = null;
    }

    /** {@inheritDoc} */
    @Override
    public List<String> getScoreboardTags() {
        return scoreboardTags;
    }

    /** {@inheritDoc} */
    @Override
    public void setScoreboardTags(List<String> scoreboardTags) {
        this.scoreboardTags = scoreboardTags;
    }

    // </editor-fold>

    /** {@inheritDoc} */
    @Override
    public CompoundTag getHandle() {
        return data;
    }

    /**
     *  {@inheritDoc}
     *  <p>If this tag is being saved into a chunk the caller is responsible for checking the
     *  result of {@link #isPositionValid()} - this method will simply leave out the "Pos"
     *  tag if {@link #isPositionValid()} would return false.</p>
     *  <p>If the uuid is not defined on this entity when this method is called, a random uuid
     *  will be generated and set.</p>
     */
    @Override
    public CompoundTag updateHandle() {
        // TODO: restrict field outputs to be version appropriate - there's no harm in extra fields but might as well
        //       be clean about it.
        data.putString("id", ArgValidator.requireNotEmpty(id, "id"));
        if (uuid == null || EntityUtil.ZERO_UUID.equals(uuid)) {
            uuid = UUID.randomUUID();
        }
        EntityUtil.setUuid(dataVersion, data, uuid);

        if (isPositionValid()) {
            data.putDoubleArrayAsTagList("Pos", x, y, z);
        } else {
            // For passengers... it's probably OK to not require a position and for sake of this being an
            // abstraction / wrapper layer - we'll allow it to provide wider use case support and make
            // caller responsible ensuring valid usage.
            data.remove("Pos");
        }

        if (isRotationValid()) {
            data.putFloatArrayAsTagList("Rotation", yaw, pitch);
        } else {
            data.remove("Rotation");
        }

        if (isMotionValid()) {
            data.putDoubleArrayAsTagList("Motion", dx, dy, dz);
        } else {
            data.remove("Motion");
        }

        if (air != AIR_UNSET) {
            data.putShort("Air", air);
        } else {
            data.remove("Air");
        }

        if (customName != null && !customName.isEmpty()) {
            data.putString("CustomName", customName);
        } else {
            data.remove("CustomName");
        }

        if (isCustomNameVisible || data.containsKey("CustomNameVisible")) {
            data.putBoolean("CustomNameVisible", isCustomNameVisible);
        }

        data.putFloat("FallDistance", fallDistance);
        data.putShort("Fire", fireTicks);

        if (isGlowing || data.containsKey("Glowing")) {
            data.putBoolean("Glowing", isGlowing);
        }
        if (hasVisualFire || data.containsKey("HasVisualFire")) {
            data.putBoolean("HasVisualFire", hasVisualFire);
        }
        if (isInvulnerable || data.containsKey("Invulnerable")) {
            data.putBoolean("Invulnerable", isInvulnerable);
        }
        if (noGravity || data.containsKey("NoGravity")) {
            data.putBoolean("NoGravity", noGravity);
        }

        data.putBoolean("OnGround", isOnGround);
        data.putInt("PortalCooldown", portalCooldown);

        if (isSilent || data.containsKey("Silent")) {
            data.putBoolean("Silent", isSilent);
        }

        data.putStringsAsTagList("Tags", scoreboardTags);
        data.putInt("TicksFrozen", ticksFrozen);

        if (passengers != null && !passengers.isEmpty()) {
            ListTag<CompoundTag> passengersTag = new ListTag<>(CompoundTag.class, passengers.size());
            for (Entity passenger : passengers) {
                if (passenger != null) {
                    passengersTag.add(passenger.updateHandle());
                }
            }
            data.put("Passengers", passengersTag);
        } else {
            data.remove("Passengers");
        }
        return data;
    }

    /**
     * Calls the copy constructor.
     * @return Deep clone of this entity.
     * @see EntityBase#EntityBase(EntityBase)
     */
    @Override
    public EntityBase clone() {
        return new EntityBase(this);
    }

    @Override
    public String toString() {
        return String.format("%s %.2f %.2f %.2f", id, x, y, z);
    }
}
