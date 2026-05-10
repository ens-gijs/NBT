package io.github.ensgijs.nbt.mca.entities;

import io.github.ensgijs.nbt.mca.EntitiesChunkBase;
import io.github.ensgijs.nbt.mca.io.McaFileHelpers;
import io.github.ensgijs.nbt.tag.CompoundTag;
import io.github.ensgijs.nbt.tag.ListTag;
import io.github.ensgijs.nbt.util.ArgValidator;
import io.github.ensgijs.nbt.util.IdentityHelper;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Provides a way to customize entity data deserialization. Used by {@link EntitiesChunkBase}
 * to provide users with {@link Entity} objects to work with instead of having to work with raw NBT data.
 * This factory can be configured to produce any custom class that implements {@link Entity}.
 * <p>Further customization is possible by also configuring the mca file creator factory lambda functions found in
 * {@link McaFileHelpers}, specifically the {@link McaFileHelpers#MCA_CREATORS} map</p>
 */
public final class EntityFactory {
    private EntityFactory() { }

    // TODO: Implement "creator ai" solution to learn which creators make the "best" entity instances for
    //  any given entity id.
    //  Strategy Idea:
    //      Creator Nomination - given a data tag and returns null if uninterested, or a creator instance which
    //          can be used (doesn't have to be "the best" creator - just one capable).
    //      Creator Selection - each nominated creator is asked to create an entity from the tag.
    //          It is expected that all entity types have a common inheritance ancestor and that all
    //          created entity types exist on a linear inheritance path such that there is only one entity E
    //          which is the most distant from Object and no entity falls outside the direct line of inheritance
    //          between E and Object.
    //      Association - register the highest quality creator for the entity id that started it all.
    //          If no creators were nominated then the default creator is registered for that id,
    //          for performance reasons.
    //  Variation:
    //      If Creator Selection produces entity types which do not all fall on a single inheritance line, or if
    //      this variation is simply preferred, consider making CreatorNomination objects themselves comparable
    //      - though probably not via the Comparable interface - but perhaps via a Comparator which can be user
    //      supplied. In fact the Creator Selection phase described previously could simply be one such Comparator
    //      and the registered default creator can simply "always" be in the running.

    /**
     * This map controls the factory creation behavior, keys are entity ID's (such as "PIG").
     * ID names in this map should not contain the "minecraft:" prefix and should be all UPPER CASE.
     * @see #normalizeId(String)
     */
    private static final Map<String, EntityCreator<?>> ENTITY_CREATORS_BY_ID = new HashMap<>();
    private static EntityCreator<?> DEFAULT_ENTITY_CREATOR = new DefaultEntityCreator();

    /** Provides a mapping table to translate "old id" names to new ones to simplify creator registration. */
    private static final Map<String, String> ID_REMAP;

    static {
        ID_REMAP = new HashMap<>();
        resetEntityIdRemap();
    }

    /**
     * Clears the entity id remapping table and removes any creators registered to one of the "old id's".
     * This should be generally safe, but if you have explicitly associated a creator with an old name, know that
     * you need to re-associate it after making this call.
     * <p>The remapping table to translate "old id" names to new ones to simplify creator registration.</p>
     * @see #registerIdRemap(String, String)
     */
    public static void clearEntityIdRemap() {
        // contract of supporting functions ensure that no key is also a value in this map, therefore
        // it is not possible that we remove an explicit "value" mapping.
        ENTITY_CREATORS_BY_ID.keySet().removeAll(ID_REMAP.keySet());
        ID_REMAP.clear();
    }

    /**
     * Resets the entity id remapping table to hard-coded defaults.
     * @see #clearCreators()
     * @see #registerIdRemap(String, String)
     */
    public static void resetEntityIdRemap() {
        clearEntityIdRemap();
        // sources:
        // https://technical-minecraft.fandom.com/wiki/Entity
        // https://minecraft.fandom.com/wiki/Java_Edition_data_values#Entities
        registerIdRemap("ArmorStand", "armor_stand");
        registerIdRemap("CaveSpider", "cave_spider");
        registerIdRemap("Dragon", "ender_dragon");
        registerIdRemap("EnderCrystal", "end_crystal");
        registerIdRemap("ender_crystal", "end_crystal");
        registerIdRemap("EnderEye", "eye_of_ender");
        registerIdRemap("EnderPearl", "ender_pearl");
        registerIdRemap("ExpBottle", "experience_bottle");
        registerIdRemap("FallingBlock", "falling_block");
        registerIdRemap("FireworkRocket", "firework_rocket");
        registerIdRemap("GiantZombie", "giant");
        registerIdRemap("IronGolem", "iron_golem");
        registerIdRemap("ItemFrame", "item_frame");
        registerIdRemap("LargeFireball", "fireball");
        registerIdRemap("LeashKnot", "leash_knot");
        registerIdRemap("LightningBolt", "lightning_bolt");
        registerIdRemap("MagmaCube", "magma_cube");
        // TODO: find old name for command_block_minecart - not that anyone is likely to notice
        registerIdRemap("MinecartChest", "chest_minecart");
        registerIdRemap("MinecartEmpty", "minecart");
        registerIdRemap("MinecartFurnace", "furnace_minecart");
        registerIdRemap("MinecartHopper", "hopper_minecart");
        registerIdRemap("MinecartMobSpawner", "spawner_minecart");
        registerIdRemap("MinecartTNT", "tnt_minecart");
        registerIdRemap("PigZombie", "zombified_piglin");
        registerIdRemap("zombie_pigman", "zombified_piglin");
        registerIdRemap("SmallFireball", "small_fireball");
        registerIdRemap("Snowman", "snow_golem");
        registerIdRemap("TNTPrimed", "tnt");
        registerIdRemap("WitherSkull", "wither_skull");
        registerIdRemap("XPOrb", "experience_orb");
    }

    /**
     * Registers a mapping from an old id name to a new one.
     * Chaining of mappings is not supported and is guarded against.
     * Maintains ENTITY_CREATORS_BY_ID map to ensure any creator registered for the preferredId is fired when the oldId
     * is encountered IFF there is not already a creator registered for the oldId.
     * <p>Note that creators are ALWAYS passed the preferredId even if the data source used an old id.</p>
     * @param oldId ID found in entity nbt data for versions of minecraft prior to, or even later than, the preferred version.
     * @param preferredId Preferred ID found in the entity nbt data for the most current supported minecraft version.
     */
    public static void registerIdRemap(String oldId, String preferredId) {
        String oldIdNorm = normalizeId(oldId);
        String newIdNorm = normalizeId(preferredId);
        ArgValidator.check(!ID_REMAP.containsKey(newIdNorm) && !ID_REMAP.containsValue(oldIdNorm),
                String.format("Chaining of mappings not supported. While adding %s -> %s", oldIdNorm, newIdNorm));
        ID_REMAP.put(oldIdNorm, newIdNorm);
        if (ENTITY_CREATORS_BY_ID.containsKey(newIdNorm)) {
            ENTITY_CREATORS_BY_ID.putIfAbsent(oldIdNorm, ENTITY_CREATORS_BY_ID.get(newIdNorm));
        }
    }

    /**
     * Performs a reverse lookup in the remap table to find all of the old id's which are mapped to the given one.
     * Use infrequently - this is not an optimized operation.
     * @param currentId Current ID (will be passed through {@link #normalizeId(String)})
     * @return not null; list of old id's (in normalized form) that are mapped to the given currentID
     */
    public static List<String> reverseIdRemap(String currentId) {
        final String idNorm = normalizeId(currentId);
        return ID_REMAP.entrySet().stream()
                .filter(e -> idNorm.equals(e.getValue()))
                .map(Map.Entry::getKey)
                .collect(Collectors.toList());
    }

    /**
     * Sets the default creator to use when one has not been registered for a particular entity id.
     * @see #registerCreator(EntityCreator, String...)
     */
    public static void setDefaultEntityCreator(EntityCreator<?> creator) {
        if (creator == null) throw new IllegalArgumentException();
        DEFAULT_ENTITY_CREATOR = creator;
    }

    /** Gets the current default entity creator instance. */
    public static EntityCreator<?> getDefaultEntityCreator() {
        return DEFAULT_ENTITY_CREATOR;
    }

    /**
     * Gets the set of NORMALIZED id's which have creators registered.
     * @see #normalizeId(String)
     * @return Key set from underlying map, modifications to this map will affect the set of registered creators.
     */
    public static Set<String> getRegisteredCreatorIdKeys() {
        return ENTITY_CREATORS_BY_ID.keySet();
    }

    /**
     * Exposed for advanced usage only, most use cases should not need to call this function.
     * @param id NORMALIZED ID. You can normalize an id by calling {@link #normalizeId(String)}
     * @return Registered creator or null if there is none (does not fall back to the default creator).
     */
    public static EntityCreator<?> getCreatorById(String id) {
        return ENTITY_CREATORS_BY_ID.get(id);
    }

    /**
     * Clears this factory's creators map and restores the {@link DefaultEntityCreator} as the default.
     * Does NOT reset the entity id remap - call {@link #resetEntityIdRemap()} to do that.
     */
    public static void clearCreators() {
        ENTITY_CREATORS_BY_ID.clear();
        DEFAULT_ENTITY_CREATOR = new DefaultEntityCreator();
    }

    /**
     * Checks that the given id has a value then normalizes it by removing any "minecraft:"
     * prefix and making them ALL CAPS for ease of use with custom enum lookups by name.
     *
     * <p>This function DOES NOT perform any name remapping for id's from old versions.
     * If that is what you are looking for, use {@link #normalizeAndRemapId(String)} instead.</p>
     *
     * @param id Entity ID
     * @return Normalized entity ID - DO NOT set this value as the value of an "id" nbt tag,
     * that's not what this function is for.
     * @throws IllegalArgumentException Thrown when ID is null or empty (after removing any "minecraft:" prefix).
     */
    public static String normalizeId(String id) {
        ArgValidator.requireValue(id);
        id = id.toUpperCase();
        if (id.startsWith("MINECRAFT:")) id = id.substring(10);
        ArgValidator.requireNotEmpty(id);
        return id;
    }

    /**
     * @param id Entity ID such as "pig" or "minecraft:pig"
     * @return Remapped normalized id if there is one, otherwise same as calling {@link #normalizeId(String)}
     */
    public static String normalizeAndRemapId(String id) {
        final String idNorm = normalizeId(id);
        return ID_REMAP.getOrDefault(idNorm, idNorm);
    }

    /**
     * Registers a creator for one or more entity id's. If there is already a creator registered for the id
     * it is silently replaced. There is no need to list all current and legacy id's, only the current ones.
     * This function will also register the given creator for all mapped legacy id's which do not already
     * have a creator associated.
     * @param creator Entity creator
     * @param entityId One or more entity id's. ID matching is performed case-insensitive and any "minecraft:"
     *                 prefixes are stripped (therefore do not need to be included).
     * @see #registerIdRemap(String, String) 
     */
    public static void registerCreator(EntityCreator<?> creator, String... entityId) {
        if (creator == null) throw new IllegalArgumentException("creator must not be null");
        for (String id : entityId) {
            String idNorm = normalizeId(id);
            ENTITY_CREATORS_BY_ID.put(idNorm, creator);
            for (String legacyId : reverseIdRemap(idNorm)) {
                ENTITY_CREATORS_BY_ID.putIfAbsent(legacyId, creator);
            }
        }
    }

    /**
     * Creates and initializes an entity from the given information.
     * @param tag must not be null; must contain an "id" tag representation of the entity's ID
     * @param dataVersion chunk data version to pass along to the creator
     * @return new entity object; never null
     * @throws IllegalEntityTagException if the creator failed to create an instance (returned null)
     * or threw any other type of exception. Note that the offending tag is captured by this exception type.
     * @throws IllegalStateException if the entity produced by the creator does not have an ID
     */
    public static Entity create(CompoundTag tag, int dataVersion) {
        if (tag == null) throw new IllegalArgumentException("tag must not be null");
        String idRaw = tag.getString("id", null);
        String idNorm = normalizeId(idRaw);
        String idPreferredNorm = ID_REMAP.getOrDefault(idNorm, idNorm);
        EntityCreator<?> creator = ENTITY_CREATORS_BY_ID.getOrDefault(idNorm, DEFAULT_ENTITY_CREATOR);
        Entity entity;
        try {
            entity = creator.create(idPreferredNorm, tag, dataVersion);
        } catch (IllegalEntityTagException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new IllegalEntityTagException(tag, ex);
        }
        if (entity == null) {
            throw new IllegalEntityTagException(tag, String.format(
                    "creator %s for %s returned null (it should throw IllegalEntityTagException itself, but didn't)",
                    creator.getClass().getSimpleName(),
                    idNorm));
        }
        if (entity.getId() == null) {
            throw new IllegalStateException(String.format(
                    "Creator '%s' messed up! Normalized ID used was '%s' created from id '%s' read from nbt "
                    + "but entity produced by creator did not set an id!",
                    creator.getClass().getSimpleName(),
                    idNorm,
                    idRaw));
        }
        return entity;
    }

    /**
     * Use this method when you know the return type - for example if you have your own base class and have
     * reconfigured this factory with creators which always return that base.
     * Any casting exceptions which result will be thrown from the call site - not from within this function.
     * @see #create(CompoundTag, int)
     */
    @SuppressWarnings("unchecked")
    public static <T extends Entity> T createAutoCast(CompoundTag tag, int dataVersion) {
        return (T) create(tag, dataVersion);
    }

    /**
     * Convenience function to populate, or create then populate, a {@code ListTag&lt;CompoundTag&gt;} given a
     * {@code List&lt;Entity&gt;}.
     * <p>Calls {@link Entity#updateHandle()} and adds the result to the returned {@link CompoundTag}.</p>
     * 
     * @param entities List of entities. This list must not contain the same entity instance multiple times.
     * @param entitiesTag Optional, if provided this ListTag is cleared then filled with Compound tags from the
     *                    given entities. If this argument is null, then a new ListTag will be created and populated.
     * @return The given entitiesTag or a new ListTag if entitiesTag was null
     * @throws IllegalArgumentException Thrown if entities contains the same Entity instance more than once.
     */
    public static <T extends Entity> ListTag<CompoundTag> toListTag(List<T> entities, ListTag<CompoundTag> entitiesTag) {
        Set<IdentityHelper<T>> seen = new HashSet<>();
        if (entitiesTag == null) entitiesTag = new ListTag<>(CompoundTag.class, entities.size());
        else entitiesTag.clear();
        for (T entity : entities) {
            if (seen.add(IdentityHelper.of(entity))) {
                entitiesTag.add(entity.updateHandle());
            } else {
                throw new IllegalArgumentException("entities list contained the same entity multiple times");
            }
        }
        return entitiesTag;
    }

    /**
     * Convenience function to create then populate a {@code ListTag&lt;CompoundTag&gt} given a {@code List&lt;Entity&gt;}.
     * @see #toListTag(List, ListTag)
     */
    public static <T extends Entity> ListTag<CompoundTag> toListTag(List<T> entities) {
        return toListTag(entities, null);
    }

    /**
     * Convenience function to create then populate a {@code List&lt;Entity&gt;} given a {@code ListTag&lt;CompoundTag&gt}.
     */
    @SuppressWarnings("unchecked")
    public static <T extends Entity> List<T> fromListTag(ListTag<CompoundTag> entitiesTag, int dataVersion) {
        List<T> entities = new ArrayList<>();
        for (CompoundTag entityTag : entitiesTag) {
            entities.add((T) EntityFactory.create(entityTag, dataVersion));
        }
        return entities;
    }
}
