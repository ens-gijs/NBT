package io.github.ensgijs.nbt.mca.entities;

import io.github.ensgijs.nbt.mca.McaEntitiesFile;
import io.github.ensgijs.nbt.mca.io.McaFileHelpers;
import io.github.ensgijs.nbt.mca.util.TagWrapper;
import io.github.ensgijs.nbt.tag.CompoundTag;
import io.github.ensgijs.nbt.util.ArgValidator;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

/**
 * Extremely basic, but complete, entity interface to allow users of this library to extend {@link McaEntitiesFile}
 * and rewire {@link McaFileHelpers} for easy integration with existing code.
 * @see EntityBase
 */
public interface Entity extends TagWrapper<CompoundTag> {
    short AIR_UNSET = Short.MIN_VALUE;

    /** String representation of the entity's ID. Does not exist for the Player entity. */
    String getId();

    /** @see #getId() */
    void setId(String id);

    /**
     * This entity's Universally Unique Identifier.
     * <p>May be null (but required by game). If uuid is null or ZERO when {@link #updateHandle()} is called,
     * a random UUID will be generated and assigned.
     */
    UUID getUuid();

    /**
     * This entity's Universally Unique Identifier.
     * @param uuid Nullable, but required by game. If uuid is null when {@link #updateHandle()} is called,
     *             a random UUID will be generated and assigned. Prefer calling {@link #generateNewUuid()} which
     *             will also clear UUID's of any riders instead of setting null here.
     */
    void setUuid(UUID uuid);

    /**
     * Generates a new random UUID for this entity and all passengers.
     * @return New UUID for this entity.
     */
    UUID generateNewUuid();

    /**
     * How much air the entity has, in ticks. Decreases by 1 per tick when unable to breathe
     * (except suffocating in a block). Increase by 1 per tick when it can breathe. If -20 while still
     * unable to breathe, the entity loses 1 health and its air is reset to 0. Most mobs can have a
     * maximum of 300 in air, while dolphins can reach up to 4800, and axolotls have 6000.
     * <p>{@link #AIR_UNSET} is used as sentinel value (by this library) to indicate no value.
     * However, generally MC stores a default of 300 even on things that don't need air.</p>
     */
    short getAir();

    /**
     * Set to {@link #AIR_UNSET} to indicate that "Air" should not be included in the NBT data.
     * @see #getAir()
     */
    void setAir(short air);

    /**
     * Distance the entity has fallen. Larger values cause more damage when the entity lands.
     */
    float getFallDistance();

    /** @see #getFallDistance() */
    void setFallDistance(float fallDistance);

    /**
     * Number of ticks until the fire is put out. Negative values reflect how long the entity can
     * stand in fire before burning. Default -20 or -1 when not on fire.
     */
    short getFire();

    /** @see #getFire() */
    void setFire(short fireTicks);

    /**
     * Optional. How many ticks the entity has been freezing. Although this tag is defined for
     * all entities, it is actually only used by mobs that are not in the freeze_immune_entity_types
     * entity type tag. Ticks up by 1 every tick while in powder snow, up to a maximum of 300
     * (15 seconds), and ticks down by 2 while out of it.
     */
    int getTicksFrozen();

    /** @see #getTicksFrozen() */
    void setTicksFrozen(int ticksFrozen);

    /**
     * The number of ticks before which the entity may be teleported back through a nether portal.
     * Initially starts at 300 ticks (15 seconds) after teleportation and counts down to 0.
     */
    int getPortalCooldown();

    /** @see #getPortalCooldown() */
    void setPortalCooldown(int portalCooldown);

    /**
     * The custom name JSON text component of this entity. Appears in player death messages and villager
     * trading interfaces, as well as above the entity when the player's cursor is over it.
     * May be empty or not exist.
     */
    String getCustomName();

    /** @see #getCustomName() */
    void setCustomName(String customName);

    /**
     *  if true, and this entity has a custom name, the name always appears above the entity, regardless of
     *  where the cursor points. If the entity does not have a custom name, a default name is shown.
     *  <p>May not exist. Default NULL</p>
     */
    boolean isCustomNameVisible();

    /** @see #isCustomNameVisible() */
    void setCustomNameVisible(boolean visible);

    /**
     * true if the entity should not take damage. This applies to living and nonliving entities alike: mobs should
     * not take damage from any source (including potion effects), and cannot be moved by fishing rods, attacks,
     * explosions, or projectiles, and objects such as vehicles and item frames cannot be destroyed unless their
     * supports are removed.
     */
    boolean isInvulnerable();

    /** @see #isInvulnerable() */
    void setInvulnerable(boolean invulnerable);

    /**
     * if true, this entity is silenced.
     * May not exist.
     */
    boolean isSilent();

    /** @see #isSilent() */
    void setSilent(boolean silent);

    /** true if the entity has a glowing outline. */
    boolean isGlowing();

    /** @see #isGlowing() */
    void setGlowing(boolean glowing);

    /** If true, the entity does not fall down naturally. Set to true by striders in lava. */
    boolean hasNoGravity();

    /** @see #hasNoGravity() */
    void setNoGravity(boolean noGravity);

    /** true if the entity is touching the ground. */
    boolean isOnGround();

    /** @see #isOnGround() */
    void setOnGround(boolean onGround);

    /** If true, the entity visually appears on fire, even if it is not actually on fire. */
    boolean hasVisualFire();

    /** @see #hasVisualFire() */
    void setHasVisualFire(boolean hasVisualFire);

    double getX();
    void setX(double x);

    double getY();
    void setY(double y);

    double getZ();
    void setZ(double z);

    default void setPosition(double x, double y, double z) {
        setX(x);
        setY(y);
        setZ(z);
    }

    default void movePosition(double dx, double dy, double dz) {
        if (!isPositionValid()) {
            throw new IllegalStateException("cannot move an invalid position");
        }
        setX(getX() + dx);
        setY(getY() + dy);
        setZ(getZ() + dz);
    }

    /**
     * @return True if x, y, and z all have finite values. Does not check for reasonable finite values.
     */
    default boolean isPositionValid() {
        return Double.isFinite(getX()) && Double.isFinite(getY()) && Double.isFinite(getZ());
    }

    /** X velocity of the entity in meters per tick. */
    double getMotionDX();

    /**
     * Updates this entities motion and the motion of all passengers.
     * @see #getMotionDX()
     */
    void setMotionDX(double dx);

    /** Y velocity of the entity in meters per tick. */
    double getMotionDY();

    /**
     * Updates this entities motion and the motion of all passengers.
     * @see #getMotionDY()
     */
    void setMotionDY(double dy);

    /** Z velocity of the entity in meters per tick. */
    double getMotionDZ();

    /**
     * Updates this entities motion and the motion of all passengers.
     * @see #getMotionDZ()
     */
    void setMotionDZ(double dz);

    /**
     * Updates this entities motion and the motion of all passengers.
     */
    default void setMotion(double dx, double dy, double dz) {
        setMotionDX(dx);
        setMotionDY(dy);
        setMotionDZ(dz);
    }

    /**
     * @return True if dx, dy, and dz all have finite values. Does not check for reasonable finite values.
     */
    default boolean isMotionValid() {
        return Double.isFinite(getMotionDX()) && Double.isFinite(getMotionDY()) && Double.isFinite(getMotionDZ());
    }

    /**
     * The entity's rotation clockwise around the Y axis (called yaw).
     * Due south is 0, west is 90, north is 180, east is 270.
     * @return yaw in degrees in range [0..360)
     * @see #getFacingCardinalAngle()
     */
    float getRotationYaw();

    /**
     * Sets entity yaw (clockwise rotation about the y-axis) in degrees.
     * Due south is 0, west is 90, north is 180, east is 270.
     * @see #getRotationYaw()
     * @see #rotate(float)
     */
    void setRotationYaw(float yaw);

    /**
     * Convenience function for working with yaw values in cardinal angles -
     * where 0 is north, 90 is east, 180 is south, 270 is west.
     * <p><i>Because dealing with yaw values is error prone and somewhat nonsensical.</i>
     * @return The direction the entity is facing in cardinal angle in degrees in range [0..360)
     */
    default float getFacingCardinalAngle() {
        return EntityUtil.normalizeYaw(getRotationYaw() + 180);
    }

    /**
     * Convenience function for working with yaw values in cardinal angles -
     * where 0 is north, 90 is east, 180 is south, 270 is west.
     * <p><i>Because dealing with yaw values is error prone and somewhat nonsensical.</i>
     * @param cardinalAngle Cardinal angle in degrees, used to calculate and set a new yaw value.
     */
    default void setFacingCardinalAngle(float cardinalAngle) {
        setRotationYaw(EntityUtil.normalizeYaw(cardinalAngle - 180));
    }

    /**
     * The entity's declination from the horizon (called pitch). Horizontal is 0. Positive values look downward.
     * Does not exceed positive or negative 90 degrees.
     */
    float getRotationPitch();

    /** @see #getRotationPitch() */
    void setRotationPitch(float pitch);

    /**
     * @see #getRotationYaw()
     * @see #getRotationPitch()
     * @see #rotate(float)
     */
    default void setRotation(float yaw, float pitch) {
        setRotationYaw(yaw);
        setRotationPitch(pitch);
    }

    /**
     * Rotates this entity, and all passengers, by the given angelDegrees.
     * <p>Note that this is different from {@link #setRotationYaw(float)} as this function is relative to
     * the current yaw and affects passengers.
     * @param angleDegrees Angle in degrees to rotate this entity and all passengers. May be positive or negative.
     * @throws IllegalStateException Thrown if current yaw is not finite
     */
    default void rotate(float angleDegrees) {
        if (angleDegrees == 0f) return;
        ArgValidator.check(Float.isFinite(angleDegrees));
        // Given the nature of floating point numbers, an extremely large given angleDegrees might
        // squash the current yaw when added to it - this problem can be avoided by first normalizing it.
        angleDegrees = EntityUtil.normalizeYaw(angleDegrees);
        float currentYaw = getRotationYaw();
        if (!Float.isFinite(currentYaw)) {
            throw new IllegalStateException("cannot rotate non-finite yaw");
        }
        setRotationYaw(EntityUtil.normalizeYaw(currentYaw + angleDegrees));
        if (hasPassengers()) {
            for (Entity passenger : getPassengers()) {
                passenger.rotate(angleDegrees);
            }
        }
    }

    /**
     * Overload taking a double for your convenience and to provide increased accuracy when passing high magnitude
     * values - the given double precision angle is normalized into the range [0..360) before passing it to
     * {@link #rotate(float)}
     * @see #rotate(float)
     */
    default void rotate(double angleDegrees) {
        rotate(EntityUtil.normalizeYaw(angleDegrees));
    }

    /**
     * @return True if yaw and pitch have finite values. Does not check for reasonable finite values.
     */
    default boolean isRotationValid() {
        return Float.isFinite(getRotationYaw()) && Float.isFinite(getRotationPitch());
    }

    /**
     * @see #getRotationYaw()
     * @see #getRotationPitch()
     */
    default void setPosition(double x, double y, double z, float yaw, float pitch) {
        setPosition(x, y, z);
        setRotation(yaw, pitch);
    }

    /**
     * The data of the entity(s) that is riding this entity. Note that both entities control movement and the
     * topmost entity controls spawning conditions when created by a mob spawner.
     * May be null.
     */
    List<Entity> getPassengers();

    /** @see #getPassengers() */
    void setPassengers(List<Entity> passengers);

    default void setPassengers(Entity... passengers) {
        if (passengers == null || passengers.length == 0 || (passengers.length == 1 && passengers[0] == null)) {
            clearPassengers();
            return;
        }
        List<Entity> list = new ArrayList<>(passengers.length);
        list.addAll(Arrays.asList(passengers));
        setPassengers(list);
    }

    /**
     * Adds a passenger, initializing the passenger list if necessary
     * @param passenger non-null passenger
     */
    void addPassenger(Entity passenger);

    /**
     * Removes all passengers (sets passengers list to null - does not actually clear that list)
     */
    void clearPassengers();

    default boolean hasPassengers() {
        return getPassengers() != null && !getPassengers().isEmpty();
    }

    /**
     * List of scoreboard tags of this entity.
     * Optional - null if not present.
     */
    List<String> getScoreboardTags();

    /** @see #getScoreboardTags() */
    void setScoreboardTags(List<String> scoreboardTags);

    default boolean hasScoreboardTags() {
        return getPassengers() != null && !getPassengers().isEmpty();
    }
}
