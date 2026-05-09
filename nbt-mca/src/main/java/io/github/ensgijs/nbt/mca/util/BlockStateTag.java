package io.github.ensgijs.nbt.mca.util;

import io.github.ensgijs.nbt.io.TextNbtHelpers;
import io.github.ensgijs.nbt.tag.CompoundTag;
import io.github.ensgijs.nbt.tag.StringTag;
import io.github.ensgijs.nbt.tag.Tag;
import io.github.ensgijs.nbt.util.ArgValidator;

import java.util.*;
import java.util.function.BiFunction;
import java.util.function.Function;

/**
 * Helper to create, modify, and interact with block state nbt data.
 */
public class BlockStateTag implements TagWrapper<CompoundTag> {
    private CompoundTag root;
    private StringTag name;
    private CompoundTag properties;

    /**
     * @param name block name, should include "minecraft:" prefix - one will not be added.
     */
    public BlockStateTag(String name) {
        Objects.requireNonNull(name);
        root = new CompoundTag();
        this.name = new StringTag(name);
        properties = new CompoundTag();
        root.put("Name", this.name);
    }

    /**
     * @param name block name, should include "minecraft:" prefix - one will not be added.
     * @param properties may be map of primitives or Tag's. If map of tags then map is shallow copied.
     *                   If map of primitives then all are wrapped as Tags.
     */
    public BlockStateTag(String name, Map<String, Object> properties) {
        Objects.requireNonNull(name, "name");
        Objects.requireNonNull(properties, "properties");
        root = new CompoundTag();
        this.name = new StringTag(name);
        root.put("Name", this.name);
        this.properties = new CompoundTag();
        setProperties(properties);
    }

    public BlockStateTag(CompoundTag tag) {
        Objects.requireNonNull(tag);
        root = tag;
        this.name = tag.getStringTag("Name");
        ArgValidator.check(this.name != null, "Missing required tag 'Name'");
        properties = tag.getCompoundTag("Properties");
        if (properties == null) this.properties = new CompoundTag();
    }

    public void setProperties(Map<String, Object> newProperties) {
        properties.clear();
        putAll(newProperties);
    }

    /** value is taken by reference (not copied) */
    public void setProperties(CompoundTag newProperties) {
        root.remove("Properties");
        if (properties != newProperties) {
            if (newProperties != null) {
                properties = newProperties;
                if (!properties.isEmpty()) {
                    root.put("Properties", properties);
                }
            } else {
                properties = new CompoundTag();
            }
        }
    }

    private StringTag wrap(Object value) {
        if (value == null) return null;
        if (value instanceof StringTag tag) {
            return tag;
        }
        if (value instanceof Tag<?> t) {
            return new StringTag(t.valueToString());
        }
        return new StringTag(value.toString());
    }


    /**
     * @return block name, including any "minecraft:" prefix
     */
    public String getName() {
        return name.getValue();
    }

    /**
     * @param name block name, should include "minecraft:" prefix - one will not be added.
     */
    public void setName(String name) {
        this.name.setValue(name);
    }

    @Override
    public CompoundTag getHandle() {
        return root;
    }

    @Override
    public CompoundTag updateHandle() {
        if (properties.isEmpty()) {
            root.remove("Properties");
        } else {
            root.put("Properties", properties);
        }
        return root;
    }

    public int size() {
        return properties.size();
    }

    public boolean isEmpty() {
        return properties.isEmpty();
    }

    public boolean hasProperty(String key) {
        return properties.containsKey(key);
    }

    public String get(String key) {
        StringTag tag = (StringTag) properties.get(key);
        return tag != null ? tag.getValue() : null;
    }

    public String put(String key, Object value) {
        if (value != null) {
            StringTag old = (StringTag) properties.put(key, wrap(value));
            return old != null ? old.getValue() : null;
        }
        return remove(key);
    }

    public String remove(String key) {
        StringTag old = (StringTag) properties.remove(key);
        return old != null ? old.getValue() : null;
    }

    public void putAll(Map<String, Object> m) {
        for (var e : m.entrySet()) {
            put(e.getKey(), e.getValue());
        }
        if (!properties.isEmpty()) {
            root.put("Properties", this.properties);
        }
    }

    public void clear() {
        properties.clear();
        root.remove("Properties");
    }

    public Set<String> keySet() {
        return properties.keySet();
    }

    public Collection<Tag<?>> values() {
        return properties.values();
    }

    public Set<Map.Entry<String, Tag<?>>> entrySet() {
        return properties.entrySet();
    }

    /**
     * Returns the value to which the specified property key is mapped, or
     * {@code defaultValue} if no property mapping for the key.
     *
     * @param key the key whose associated value is to be returned
     * @param defaultValue the default mapping of the key
     * @return the value to which the specified key is mapped, or
     * {@code defaultValue} if this map contains no mapping for the key
     */
    public String getOrDefault(String key, Object defaultValue) {
        StringTag tag = (StringTag) properties.get(key);
        if (tag != null)
            return tag.getValue();
        return defaultValue != null ? wrap(defaultValue).getValue() : null;
    }

    /**
     * If the specified key is not already associated with a value
     * associates it with the given value and returns null, else returns the current value.
     *
     * @param key key with which the specified value is to be associated
     * @param value value to be associated with the specified key
     * @return the previous value associated with the specified key, or
     *         {@code null} if there was no mapping for the key.
     */
    public String putIfAbsent(String key, Object value) {
        Objects.requireNonNull(key);
        Objects.requireNonNull(value);
        StringTag v = (StringTag) properties.get(key);
        if (v == null) {
            v = wrap(value);
            properties.put(key, v);
            return null;
        }
        return v.getValue();
    }

    /**
     * Removes the entry for the specified key only if it is currently mapped to the specified value.
     */
    public boolean remove(String key, Object value) {
        StringTag curValue = (StringTag) properties.get(key);
        StringTag v = wrap(value);
        if (!Objects.equals(curValue, v) || (curValue == null && !hasProperty(key))) {
            return false;
        }
        remove(key);
        return true;
    }

    /**
     * Replaces the entry for the specified key only if currently
     * mapped to the specified value.
     *
     * @param key key with which the specified value is associated
     * @param oldValue value expected to be associated with the specified key
     * @param newValue value to be associated with the specified key
     * @return {@code true} if the value was replaced
     */
    public boolean replace(String key, Object oldValue, Object newValue) {
        String curValue = get(key);
        if (curValue == null)
            return false;
        String oldValueStr = wrap(oldValue).getValue();

        if (!Objects.equals(curValue, oldValue)) {
            return false;
        }
        put(key, wrap(newValue));
        return true;
    }

    /**
     * Replaces the entry for the specified key only if it is
     * currently mapped to some value.
     *
     * @param key key with which the specified value is associated
     * @param value value to be associated with the specified key
     * @return the previous value associated with the specified key, or
     *         {@code null} if there was no mapping for the key.
     */
    public String replace(String key, Object value) {
        String curValue;
        if (((curValue = get(key)) != null) || hasProperty(key)) {
            curValue = put(key, wrap(value));
        }
        return curValue;
    }

    /**
     * If the specified key is not already associated with a value (or is mapped
     * to {@code null}), attempts to compute its value using the given mapping
     * function and enters it into this map unless {@code null}.
     *
     * <p>If the mapping function returns {@code null}, no mapping is recorded.
     * If the mapping function itself throws an (unchecked) exception, the
     * exception is rethrown, and no mapping is recorded.
     *
     * @param key key with which the specified value is to be associated
     * @param mappingFunction the mapping function to compute a value
     * @return the current (existing or computed) value associated with
     *         the specified key, or null if the computed value is null
     */
    public String computeIfAbsent(String key, Function<String, ?> mappingFunction) {
        Objects.requireNonNull(mappingFunction);
        String v;
        if ((v = get(key)) == null) {
            StringTag newValue;
            if ((newValue = wrap(mappingFunction.apply(key))) != null) {
                put(key, newValue);
                return newValue.getValue();
            }
        }

        return v;
    }

    /**
     * If the value for the specified key is present and non-null, attempts to
     * compute a new mapping given the key and its current mapped value.
     *
     * <p>If the remapping function returns {@code null}, the mapping is removed.
     * If the remapping function itself throws an (unchecked) exception, the
     * exception is rethrown, and the current mapping is left unchanged.
     *
     * <p>The remapping function should not modify this map during computation.
     *
     * @param key key with which the specified value is to be associated
     * @param remappingFunction the remapping function to compute a value
     * @return the new value associated with the specified key, or null if none
     */
    public String computeIfPresent(String key, BiFunction<String, String, ?> remappingFunction) {
        Objects.requireNonNull(remappingFunction);
        String oldValue;
        if ((oldValue = get(key)) != null) {
            StringTag newValue = wrap(remappingFunction.apply(key, oldValue));
            if (newValue != null) {
                put(key, newValue);
                return newValue.getValue();
            } else {
                remove(key);
                return null;
            }
        } else {
            return null;
        }
    }

    /**
     * Attempts to compute a mapping for the specified key and its current
     * mapped value (or {@code null} if there is no current mapping).
     *
     * @param key key with which the specified value is to be associated
     * @param remappingFunction the remapping function to compute a value
     * @return the new value associated with the specified key, or null if none
     */
    public String compute(String key, BiFunction<String, String, ?> remappingFunction) {
        Objects.requireNonNull(remappingFunction);
        String oldValue = get(key);
        StringTag newValue = wrap(remappingFunction.apply(key, oldValue));
        if (newValue == null) {
            // delete mapping
            if (oldValue != null || hasProperty(key)) {
                // something to remove
                remove(key);
                return null;
            } else {
                // nothing to do. Leave things as they were.
                return null;
            }
        } else {
            // add or replace old mapping
            put(key, newValue);
            return newValue.getValue();
        }
    }

    @Override
    public int hashCode() {
        return name.hashCode() * 31 ^ properties.hashCode();
    }

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof BlockStateTag other) {
            return Objects.equals(this.name, other.name) &&
                    Objects.equals(this.properties, other.properties);
        }
        return false;
    }

    @Override
    public String toString() {
        return TextNbtHelpers.toTextNbt(updateHandle(), false);
    }
}
