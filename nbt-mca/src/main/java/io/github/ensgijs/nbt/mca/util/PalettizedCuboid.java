package io.github.ensgijs.nbt.mca.util;

import io.github.ensgijs.nbt.io.TextNbtHelpers;
import io.github.ensgijs.nbt.tag.*;

import java.util.*;
import java.util.function.Predicate;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

import static io.github.ensgijs.nbt.util.ArgValidator.*;

/**
 * A PalettizedCuboid is serialized as a {@link CompoundTag} containing {@code data}
 * and {@code palette} entries. {@code data} is always of type {@link LongArrayTag}
 * while {@code palette} is a {@link ListTag} of some type {@link E}. If {@code palette}
 * contains only a single value then {@code data} is omitted and all data values are assumed to be the same.
 *
 * <p>The {@link #size()} is fixed and a cube of a power of two, typically 16 or 4.</p>
 * <p>A cuboid is a 3D rectangle, however in MCA chunk's x, y, and z sizes are all equal (currently).</p>
 *
 * <h2>NBT Examples</h2>
 * <h3>block_states</h3>
 * <pre>
 * "block_states": {
 *   "type": "CompoundTag",
 *   "value": {
 *     "data": {
 *       "type": "LongArrayTag",
 *       "value": [...]
 *     },
 *     "palette": {
 *       "type": "ListTag",
 *       "value": {
 *         "type": "CompoundTag",
 *         "list": [
 *           {
 *             "Name": {
 *               "type": "StringTag",
 *               "value": "minecraft:bedrock"
 *             }
 *           },
 *           ...
 *         ]
 *       }
 *     }
 *   }
 * </pre>
 * <h3>biomes</h3>
 * <pre>
 * "biomes": {
 *   "type": "CompoundTag",
 *   "value": {
 *     "data": {
 *       "type": "LongArrayTag",
 *       "value": [
 *         0,
 *         -7686143365460459264
 *       ]
 *     },
 *     "palette": {
 *       "type": "ListTag",
 *       "value": {
 *         "type": "StringTag",
 *         "list": [
 *           "minecraft:deep_dark",
 *           "minecraft:snowy_taiga",
 *           "minecraft:grove"
 *         ]
 *       }
 *     }
 *   }
 * </pre>
 *
 * @see BlockStateTag
 */
public class PalettizedCuboid<E extends Tag<?>> implements TagWrapper<CompoundTag>, Iterable<E>, Cloneable {
    //    public static boolean DEBUG = false;
    record CubeInfo(int edgeLength, int cordBitMask , int zShift , int yShift) {
        public int entryCount() {
            return edgeLength * edgeLength * edgeLength;
        }
    }

    /**
     * Flyweight instances - reuse these instead of having an instance for every PalettizedCuboid instance. 
     * @see #nilSentinelFor(Class)
     */
    private static final Map<Class<?>, Tag<?>> EMPTY_VALUE_SENTINEL_CACHE = new HashMap<>();
    /**
     * Flyweight instances - reuse these instead of having an instance for every PalettizedCuboid instance.
     * @see #cubeInfoFor(int) 
     */
    private static final Map<Integer, CubeInfo> CUBE_INFO_CACHE = new HashMap<>();

    protected final CubeInfo cubeInfo;
    protected final Class<E> paletteEntryClass;
    protected transient int paletteModCount = 0;
    protected final CompoundTag paletteContainerTag;
    protected final ListTag<E> palette;
    protected final LongArrayTagPackedIntegers packedData;

    @SuppressWarnings("unchecked")
    protected static <T extends Tag<?>> T nilSentinelFor(Class<T> clazz) {
        T val = (T) EMPTY_VALUE_SENTINEL_CACHE.get(clazz);
        if (val == null) {
            try {
                val = clazz.getDeclaredConstructor().newInstance();
                EMPTY_VALUE_SENTINEL_CACHE.put(clazz, val);
            } catch (ReflectiveOperationException ex) {
                throw new IllegalArgumentException("Failed to create a default instance of " + clazz.getName(), ex);
            }
        }
        return val;
    }

    protected static CubeInfo cubeInfoFor(final int edgeLength) {
        return CUBE_INFO_CACHE.computeIfAbsent(edgeLength, (k) -> {
            final int bits = calculatePowerOfTwoExponent(edgeLength, true);
            return new CubeInfo(edgeLength, calculateBitMask(bits), bits, bits * 2);
        });
    }

    /**
     * @param cubeEdgeLength The size of this {@link PalettizedCuboid} will be {@code cubeEdgeLength^3}.
     *                       The value of {@code cubeEdgeLength} is typically 16 or 4.
     *                       Must be a power of 2.
     * @param fillWith Required. Value to fill the initial {@link PalettizedCuboid} with.
     *                       The given value is cloned for each entry in the cuboid.
     */
    @SuppressWarnings("unchecked")
    public PalettizedCuboid(final int cubeEdgeLength, E fillWith) {
        this(cubeEdgeLength, (Class<E>) fillWith.getClass(), fillWith, false);
    }

    @SuppressWarnings("unchecked")
    public PalettizedCuboid(PalettizedCuboid<E> other) {
        this.cubeInfo = other.cubeInfo;
        this.paletteEntryClass = other.paletteEntryClass;
        paletteContainerTag = new CompoundTag();
        palette = new ListTag<>(paletteEntryClass, other.size());
        paletteContainerTag.put("palette", palette);
        for (E e : other.palette) {
            this.palette.add((E) e.clone());
        }
        this.packedData = other.packedData.clone();
    }

    @SuppressWarnings("unchecked")
    protected PalettizedCuboid(final int cubeEdgeLength, Class<E> paletteEntryClass, E fillWith, boolean allowNullFill) {
        if (!allowNullFill) {
            requireValue(fillWith, "fillWith");
        }
        cubeInfo = cubeInfoFor(cubeEdgeLength);
        this.paletteEntryClass = paletteEntryClass;
        paletteContainerTag = new CompoundTag();
        palette = new ListTag<>(paletteEntryClass);
        paletteContainerTag.put("palette", palette);
        this.packedData = LongArrayTagPackedIntegers.builder()
                .length(cubeInfo.entryCount())
                .minBitsPerValue(cubeEdgeLength == 16 ? 4 /*blocks*/: 1 /*biomes*/)
                .build();
        if (fillWith != null) {
            this.palette.add((E) fillWith.clone());
        }
    }

    /**
     * Protected access because of the annoyance of needing to know the paletteEntryClass apriori to the call.
     * @see #fromCompoundTag
     */
    protected PalettizedCuboid(final int cubeEdgeLength, Class<E> paletteEntryClass, CompoundTag paletteContainerTag, int dataVersion) {
        requireValue(paletteContainerTag, "paletteContainerTag");
        check(paletteContainerTag.containsKey("palette"), "paletteContainerTag must contain a 'palette' ListTag");
        cubeInfo = cubeInfoFor(cubeEdgeLength);
        this.paletteContainerTag = paletteContainerTag;
        this.paletteEntryClass = paletteEntryClass;
        palette = this.paletteContainerTag.getListTag("palette").asTypedList(paletteEntryClass);
        var builder = LongArrayTagPackedIntegers.builder()
                .length(cubeInfo.entryCount())
                .dataVersion(dataVersion)
                .minBitsPerValue(cubeEdgeLength == 16 ? 4 /*blocks*/: 1 /*biomes*/)
                .initializeForStoring(palette.size() - 1);
        if (paletteContainerTag.containsKey("data")) {
            this.packedData = builder.build(paletteContainerTag.getLongArrayTag("data"));
        } else {
            this.packedData = builder.build();
        }
    }

    /**
     * @param values Must be of a length that has a cube root and that root must itself be a power of 2.
     *               Ex. 4096 is 16^3 and 16 is 4^2; Ex. 64 is 4^3 and 4 is 2^2. The given values are cloned,
     *               neither the given values array nor its elements are taken by reference. However, any value
     *               which is repeated is only entered into the palette once.
     */
    @SuppressWarnings("unchecked")
    public PalettizedCuboid(E[] values) {
        this(cubeRoot(values.length), (Class<E>) values.getClass().getComponentType(), null, true);
        Map<E, Integer> indexLookup = new HashMap<>();
        for (int i = 0; i < packedData.length; i++) {
            check(values[i] != null, "values must not contain nulls!");
            int paletteIndex = indexLookup.computeIfAbsent(values[i], k -> {
                palette.add((E) k.clone());
                return palette.size() - 1;
            });
            this.packedData.set(i, paletteIndex);
        }
    }

    /** This is a very verbose, multi-line, string that includes the full palette and full data cube. */
    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder("<PALETTE>\n");
        for (int i = 0; i < palette.size(); i++) {
            sb.append(i).append(":= ").append(TextNbtHelpers.toTextNbt(palette.get(i), true)).append('\n');
        }
        return sb.append("<DATA>\n").append(packedData.toString3dGrid()).toString();
    }

    static int cubeRoot(int num) {
        int k = (int) Math.round(Math.pow(num, 1/3d));
        if (k * k * k != num) {
            throw new IllegalArgumentException("the cube root of " + num + " is not an integer!");
        }
        return k;
    }

    /**
     * If strict is true and num is not a power of 2 then IllegalArgumentException is thrown.
     * <p>If strict is false and num is not a power of 2 then the return value is the power of 2 which
     * is large enough to include num. Ex. num = 7 -> 3 (2^3 = 8)</p>
     */
    static int calculatePowerOfTwoExponent(int num, boolean strict) {
        int k = 0;
        boolean bump = false;
        while (num > 1) {
            k++;
            if (num % 2 != 0) {
                if (strict) {
                    throw new IllegalArgumentException(num + " isn't a power of two!");
                } else {
                    bump = true;
                }
            }
            num /= 2;
        }
        return k + (bump ? 1 : 0);
    }

    static int calculateBitMask(int numberOfBits) {
        if (numberOfBits < 0 || numberOfBits >= 32) {
            throw new IllegalArgumentException(Integer.toString(numberOfBits));
        }
        return ~(-1 << numberOfBits);
    }

    public void setDataVersion(int newDataVersion) {
        packedData.setPackingStrategy(LongArrayTagPackedIntegers.MOJANG_PACKING_STRATEGY.get(newDataVersion));
    }

    /** size of data array (ex. 64 for a 4x4x4 cuboid) */
    public int size() {
        return packedData.length;
    }

    /**
     * Length of one edge of the cuboid (ex. 4 for a 4x4x4 cuboid).
     */
    public int cubeEdgeLength() {
        return cubeInfo.edgeLength;
    }

    /** The current palette size. Note for an exact accurate palette count call {@link #optimizePalette()} first. */
    public int paletteSize() {
        return palette.size();
    }

    /**
     * @see #countIf(Predicate)
     */
    public boolean contains(E o) {
        return palette.contains(o);
    }

    /**
     * Counts the number of data entries which match the given filter.
     */
    public int countIf(Predicate<E> filter) {
        Collection<Integer> counting = new HashSet<>();
        final int expectPaletteModCount = paletteModCount;
        for (int i = 0; i < palette.size(); i++) {
            E paletteValue = palette.get(i);
            if (paletteValue == null) {
                continue;
            }
            final int hash = paletteValue.hashCode();
            if (filter.test(paletteValue)) {
                counting.add(i);
            }
            if (paletteValue.hashCode() != hash) {
                throw new PaletteCorruptedException("Palette element modified during countIf filter call! Consider using replaceIf() instead.");
            }
        }
        if (expectPaletteModCount != paletteModCount) {
            throw new ConcurrentModificationException();
        }
        if (counting.isEmpty()) {
            return 0;
        }
        if (counting.size() == 1) {
            counting = Collections.singleton(counting.iterator().next());
        } else if (counting.size() < 5) {
            counting = new ArrayList<>(counting);
        }
        return packedData.count(counting::contains);
    }

    /**
     * Returns a copy of the palette values for every position in this cuboid.
     * <p>Modifying the returned value can be done safely, it will have no effect on this cuboid.</p>
     * <p>To avoid the overhead of making N copies of palette tags use {@link #toArrayByRef()} instead.</p>
     */
    @SuppressWarnings("unchecked")
    public E[] toArray() {
        E[] a = (E[]) java.lang.reflect.Array.newInstance(paletteEntryClass, packedData.length);
        for (int i = 0; i < packedData.length; i++) {
            a[i] = (E) palette.get(packedData.get(i)).clone();
        }
        return a;
    }

    /**
     * Returns the palette value for every position in this cuboid.
     * <p><b>WARNING if the returned Tags are modified it modifies every value which references the same palette entry!</b></p>
     * <p>Modifying the returned array itself does not change the cuboid.</p>
     */
    @SuppressWarnings("unchecked")
    public E[] toArrayByRef() {
        E[] a = (E[]) java.lang.reflect.Array.newInstance(paletteEntryClass, packedData.length);
        for (int i = 0; i < packedData.length; i++) {
            a[i] = palette.get(packedData.get(i));
        }
        return a;
    }

    @SuppressWarnings("unchecked")
    private boolean replace(Collection<Integer> replacing, E replacement) {
        requireValue(replacement, "replacement");
        paletteModCount ++;
        replacing.remove(-1);  // TODO: document semantics
        if (replacing.isEmpty()) {
            return false;
        }
        int replacementPaletteIndex = palette.indexOf(replacement);
        boolean addReplacementToPaletteIfDataModified;
        if (replacementPaletteIndex < 0) {
            replacementPaletteIndex = palette.size();
            addReplacementToPaletteIfDataModified = true;
        } else {
            replacing.remove(replacementPaletteIndex);
            if (replacing.isEmpty()) {
                return false;
            }
            addReplacementToPaletteIfDataModified = false;
        }

        // TODO: run some simulations to see if this "optimization" matters at all and if it does, tune the list/set threshold
        if (replacing.size() == 1) {
            replacing = Collections.singleton(replacing.iterator().next());
        } else if (replacing.size() < 5) {
            // small lists are (generally) faster than small hashsets
            if (!(replacing instanceof List))
                replacing = new ArrayList<>(replacing);
        } else if (!(replacing instanceof Set)) {
            replacing =  new HashSet<>(replacing);
        }

        boolean modified = false;
        for (int i = 0; i < packedData.length; i++) {
            if (replacing.contains(packedData.get(i))) {
                modified = true;
                packedData.set(i, replacementPaletteIndex);
            }
        }
        if (modified) {
            final var nilValue = nilSentinelFor(paletteEntryClass);
            for (int i : replacing) {
                palette.set(i, nilValue);  // paletteModCount incremented at top of method
            }
            if (addReplacementToPaletteIfDataModified)
                palette.add((E) replacement.clone());  // paletteModCount incremented at top of method
        }
        return modified;
    }

    /**
     * @return True if any modifications were made, false otherwise.
     */
    public boolean replace(E oldValue, E newValue) {
        requireValue(oldValue, "oldValue");
        requireValue(newValue, "newValue");
        if (oldValue.equals(newValue)) {
            return false;
        }
        // Don't pass a singleton list/set type - they are immutable and will cause errors.
        return replace(new ArrayList<>(Collections.singletonList(palette.indexOf(oldValue))), newValue);
    }

    public final boolean replaceAll(E[] a, E replacement) {
        return replaceAll(Arrays.asList(a), replacement);
    }

    public boolean replaceAll(Collection<E> c, E replacement) {
        requireValue(replacement, "replacement");
        if (c.isEmpty()) {
            return false;
        }
        Set<Integer> replacing = new HashSet<>();
        for (E e : c) {
            int i = palette.indexOf(e);
            if (i >= 0) {
                replacing.add(i);
            }
        }
        return replace(replacing, replacement);
    }

    public boolean replaceIf(Predicate<E> filter, E replacement) {
        requireValue(replacement, "replacement");
        final int expectPaletteModCount = paletteModCount;
        Set<Integer> replacing = new HashSet<>();
        final var nilValue = nilSentinelFor(paletteEntryClass);
        for (int i = 0; i < palette.size(); i++) {
            E paletteValue = palette.get(i);
            if (paletteValue == nilValue) {
                continue;
            }
            final int hash = paletteValue.hashCode();
            if (filter.test(paletteValue)) {
                replacing.add(i);
            }
            if (paletteValue.hashCode() != hash) {
                throw new PaletteCorruptedException("Palette element passed to filter modified unexpectedly!");
            }
            if (expectPaletteModCount != paletteModCount) {
                throw new ConcurrentModificationException();
            }
        }
        return replace(replacing, replacement);
    }

    public final boolean retainAll(E[] a, E replacement) {
        return retainAll(Arrays.asList(a), replacement);
    }

    public boolean retainAll(Collection<E> c, E replacement) {
        requireValue(replacement, "replacement");
        Set<Integer> replacing = new HashSet<>();
        final var nilValue = nilSentinelFor(paletteEntryClass);
        for (int i = 0; i < palette.size(); i++) {
            E paletteValue = palette.get(i);
            if (paletteValue != nilValue && !c.contains(paletteValue)) {
                replacing.add(i);
            }
        }
        return replace(replacing, replacement);
    }

    /**
     * Sets the entire volume to the given value.
     * @param fillWith value to fill volume with, this value is cloned (not taken by reference).
     */
    @SuppressWarnings("unchecked")
    public void fill(E fillWith) {
        requireValue(fillWith, "fillWith");
        paletteModCount ++;
        palette.clear();
        palette.add((E) fillWith.clone());
        packedData.clear(true);
    }

    /**
     * Computes the wrapped index of XYZ. If, for example, xSize is 16 and a value of 20 is passed
     * for X it will behave the same as if a value of 4 were passed instead.
     * @param x X index
     * @param y Y index
     * @param z Z index
     * @return wrapped element index
     */
    public int indexOf(int x, int y, int z) {
        return ((y & cubeInfo.cordBitMask) << cubeInfo.yShift) |
                ((z & cubeInfo.cordBitMask) << cubeInfo.zShift) |
                (x & cubeInfo.cordBitMask);
    }

    public int indexOf(IntPointXYZ xyz) {
        return indexOf(xyz.x, xyz.y, xyz.z);
    }

    /**
     * Calculates the x, y, z (in cuboid space) of the given index.
     */
    public IntPointXYZ xyzOf(int index) {
        return new IntPointXYZ(
                index & cubeInfo.cordBitMask,
                (index >> cubeInfo.yShift) & cubeInfo.cordBitMask,
                (index >> cubeInfo.zShift) & cubeInfo.cordBitMask
        );
    }

    /**
     * Wraps the given x, y, z into cuboid space.
     */
    public IntPointXYZ xyzOf(int x, int y, int z) {
        int index = indexOf(x, y, z);
        return new IntPointXYZ(
                index & cubeInfo.cordBitMask,
                (index >> cubeInfo.yShift) & cubeInfo.cordBitMask,
                (index >> cubeInfo.zShift) & cubeInfo.cordBitMask
        );
    }

    /**
     * Returns a copy of the palette value at the specified position in this cuboid.
     * <p>Modifying the returned value can be done safely, it will have no effect on this cuboid.</p>
     * <p>To avoid the overhead of making a copy use {@link #getByRef(int)} instead.</p>
     *
     * @param index index of the element to return
     * @return the element at the specified position in this cuboid
     * @throws IndexOutOfBoundsException if the index is out of range
     *         (index &lt; 0 || index &gt;= size())
     */
    @SuppressWarnings("unchecked")
    public E get(int index) {
        return (E) palette.get(packedData.get(index)).clone();
    }
    /**
     * Returns a copy of the palette value at the specified position in this cuboid.
     * <p>Modifying the returned value can be done safely, it will have no effect on this cuboid.</p>
     * <p>To avoid the overhead of making a copy use {@link #getByRef(int, int, int)} instead.</p>
     *
     * <p>Never throws IndexOutOfBoundsException. XYZ are always wrapped into bounds.</p>
     * @return the element at the specified position in this cuboid.
     */
    public E get(int x, int y, int z) {
        return get(indexOf(x, y, z));
    }

    public E get(IntPointXYZ xyz) {
        return get(indexOf(xyz));
    }


    /**
     * Returns the palette value at the specified position in this cuboid.
     * <p><b>WARNING if the returned value is modified it modifies every value which references the same palette entry!</b></p>
     *
     * @param index index of the element to return
     * @return the element at the specified position in this cuboid
     * @throws IndexOutOfBoundsException if the index is out of range
     *         (index &lt; 0 || index &gt;= size())
     */
    public E getByRef(int index) {
        return palette.get(packedData.get(index));
    }

    /**
     * Returns the palette value at the specified position in this cuboid.
     * <p><b>WARNING if the returned value is modified it modifies every value which references the same palette entry!</b></p>
     *
     * <p>Never throws IndexOutOfBoundsException. XYZ are always wrapped into bounds.</p>
     * @return the element at the specified position in this cuboid.
     */
    public E getByRef(int x, int y, int z) {
        return getByRef(indexOf(x, y, z));
    }

    public E getByRef(IntPointXYZ xyz) {
        return getByRef(indexOf(xyz));
    }

    /**
     * Replaces the element at the specified position in this cuboid with
     * the specified element.
     *
     * @param index index of the element to replace
     * @param element element to be stored at the specified position
     * @throws IndexOutOfBoundsException if the index is out of range (index &lt; 0 || index &gt;= size())
     */
    @SuppressWarnings("unchecked")
    public void set(int index, E element) {
        requireValue(element, "element");
        if (index < 0 || index >= packedData.length) {
            throw new IndexOutOfBoundsException();
        }
        paletteModCount ++;
        int paletteIndex = palette.indexOf(element);
        if (paletteIndex < 0) {
            paletteIndex = palette.size();
            palette.add((E) element.clone());  // paletteModCount incremented at top of method
        }
        packedData.set(index, paletteIndex);
    }

    /**
     * Replaces the element at the specified position in this cuboid with
     * the specified element.
     *
     * <p>Never throws IndexOutOfBoundsException. XYZ are always wrapped into bounds.</p>
     * @param x X index of the element to replace
     * @param y Y index of the element to replace
     * @param z Z index of the element to replace
     * @param element element to be stored at the specified position
     */
    public void set(int x, int y, int z, E element) {
        set(indexOf(x, y, z), element);
    }

    public void set(IntPointXYZ xyz, E element) {
        set(indexOf(xyz.x, xyz.y, xyz.z), element);
    }

    /**
     * Sets a range of entries. The given coordinates must be in cuboid space (not absolute) and be contained
     * within the bounds of this cuboid as wrapping these bounds would cause strange artifacts.
     * @param x1 inclusive bound
     * @param y1 inclusive bound
     * @param z1 inclusive bound
     * @param element fill with
     * @param x2 inclusive bound
     * @param y2 inclusive bound
     * @param z2 inclusive bound
     */
    @SuppressWarnings("unchecked")
    public void set(int x1, int y1, int z1, E element, int x2, int y2, int z2 ) {
        requireValue(element, "element");
        checkBounds(x1, y1, z1);
        checkBounds(x2, y2, z2);
        if (x1 > x2) { int t = x2; x2 = x1 + 1; x1 = t; } else { x2++; }
        if (y1 > y2) { int t = y2; y2 = y1 + 1; y1 = t; } else { y2++; }
        if (z1 > z2) { int t = z2; z2 = z1 + 1; z1 = t; } else { z2++; }
        if ((x2 - x1) * (y2 - y1) * (z2 - z1) == size()) {
            fill(element);
            return;
        }

        int paletteIndex = palette.indexOf(element);
        if (paletteIndex < 0) {
            paletteModCount ++;
            paletteIndex = palette.size();
            palette.add((E) element.clone());
        }

        // detect and optimize XZ plain fills
        if (x1 == 0 && z1 == 0 && x2 == cubeInfo.edgeLength && z2 == cubeInfo.edgeLength) {
            final int endIndex = indexOf(x2 - 1, y2 - 1, z2 - 1);
            for (int i = indexOf(x1, y1, z1); i <= endIndex; i++) {
                packedData.set(i, paletteIndex);
            }
            return;
        }

        // iteration order x, z, y
        for (int y = y1; y < y2; y++) {
            for (int z = z1; z < z2; z++) {
                for (int x = x1; x < x2; x++) {
                    packedData.set(indexOf(x, y , z), paletteIndex);
                }
            }
        }
    }

    /**
     * Sets a range of entries. The given coordinates must be in cuboid space (not absolute) and be contained
     * within the bounds of this cuboid as wrapping these bounds would cause strange artifacts.
     * @param xyz1 inclusive bound
     * @param element fill with
     * @param xyz2 inclusive bound
     */
    public void set(IntPointXYZ xyz1, E element, IntPointXYZ xyz2) {
        set(xyz1.x, xyz1.y, xyz1.z, element, xyz2.x, xyz2.y, xyz2.z);
    }

    protected void checkBounds(int x, int y, int z) {
        if (x < 0 || y < 0 || z < 0 || x >= cubeInfo.edgeLength || y >= cubeInfo.edgeLength || z >= cubeInfo.edgeLength) {
            throw new IndexOutOfBoundsException();
        }
    }

    /**
     * Removes empty value sentinels from the palette and remaps value references as-needed.
     * @return true if any modifications were made
     */
    protected boolean optimizePalette() {
        paletteModCount ++;
        // 1. identify unused palette id's & remove them from palette
        Set<Integer> seenIds = new HashSet<>();
        for (int id : packedData) {
            seenIds.add(id);
        }
        int maxId = seenIds.stream().mapToInt(v -> v).max().getAsInt();
        if (maxId >= palette.size()) {
//            String dataStr = Arrays.stream(data)
//                    .mapToObj(String::valueOf)
//                    .collect(Collectors.joining(", "));
            throw new IllegalStateException("data[" + /*dataStr +*/ "] contained an out of bounds palette id " + maxId + " palette size " + palette.size());
        }
        final E nilValue = nilSentinelFor(paletteEntryClass);
        for (int i = 0; i < palette.size(); i++) {
            if (!seenIds.contains(i)) {
                palette.set(i, nilValue);  // paletteModCount at top of function
            }
        }

        // 2. calculate palette defragmentation
        int cursor = 0;
        Map<Integer, Integer> remapping = new HashMap<>();
        for (int i = 0; i < palette.size(); i++) {
            if (palette.get(i) != nilValue) {
                if (i != cursor) {
                    remapping.put(i, cursor);
                }
                cursor++;
            }
        }

        // 3. remove nilValue's from palette
        palette.removeAll(Collections.singletonList(nilValue));  // paletteModCount at top of function

        // 4. perform id remapping
        if (remapping.isEmpty()) {
            packedData.compact();
            return false;
        } else {
            packedData.remap(remapping);
            packedData.compact();
            return true;
        }
    }

    @Override
    @SuppressWarnings("unchecked")
    public PalettizedCuboid<E> clone() {
        optimizePalette();
        return new PalettizedCuboid<>(this);
    }

    /** Serializes this cuboid to a {@link CompoundTag} assuming the latest data version (fine if only working with &gt;= JAVA_1_16_20W17A). */
    public CompoundTag toCompoundTag() {
        return toCompoundTag(0);
    }

    public CompoundTag toCompoundTag(int dataVersion) {
        return this.toCompoundTag(dataVersion, -1);
    }

    /**
     * Serializes this cuboid to a {@link CompoundTag}.
     *
     * @param dataVersion Optional - data version for formatting / packing.
     *                    If GT 0 then packing strategy is updated to match the given versions behavior.
     * @param minimumBitsPerIndex Optional - Minimum bits per index to use for packing.
     *                            If GT 0, the long[] packing is updated to respect this value.
     */
    public CompoundTag toCompoundTag(int dataVersion, int minimumBitsPerIndex) {
        optimizePalette();
        if (palette.size() > 1) {
            if (minimumBitsPerIndex > 0 || dataVersion > 0) {
                if (minimumBitsPerIndex > 0) {
                    packedData.setMinBitsPerValue(minimumBitsPerIndex);
                }
                if (dataVersion > 0) {
                    packedData.setPackingStrategy(LongArrayTagPackedIntegers.MOJANG_PACKING_STRATEGY.get(dataVersion));
                }
                packedData.compact();
            }
            // don't need to call packedData.updateHandle() because we already compacted in optimizePalette() - or above
            paletteContainerTag.put("data", packedData.getHandle());
        } else {
            paletteContainerTag.remove("data");
        }
        return paletteContainerTag;
    }

    /**
     * @param tag Must contain a 'palette' entry of type ListTag (usually then of type ListTag&lt;StringTag&gt; or
     *            &lt;CompoundTag&gt;) and MAY contain a 'data' tag of type {@link LongArrayTag}. The palette
     *            must contain at least one record to be considered valid.
     * @param expectedCubeEdgeLength The length of one edge of the cuboid, usually a power of two, typically 16 or 4.
     * @param <T> The type of the palette list entries. Usually {@link CompoundTag} or {@link StringTag}
     * @return New PalettizedCuboid wrapping the supplied tag.
     */
    public static <T extends Tag<?>> PalettizedCuboid<T> fromCompoundTag(CompoundTag tag, int expectedCubeEdgeLength) {
        return fromCompoundTag(tag, expectedCubeEdgeLength, 0);
    }

    /**
     * @param tag Must contain a 'palette' entry of type ListTag (usually then of type ListTag&lt;StringTag&gt; or
     *            &lt;CompoundTag&gt;) and MAY contain a 'data' tag of type {@link LongArrayTag}. The palette
     *            must contain at least one record to be considered valid.
     * @param expectedCubeEdgeLength The length of one edge of the cuboid, usually a power of two, typically 16 or 4.
     * @param dataVersion if GT 0, this dataversion is used to determine the long[] packing strategy,
     *                    see {@link LongArrayTagPackedIntegers#MOJANG_PACKING_STRATEGY}. If EQ 0 then the latest
     *                    packing standard is used, {@link LongArrayTagPackedIntegers.Builder#dataVersion(int)}.
     * @param <T> The type of the palette list entries. Usually {@link CompoundTag} or {@link StringTag}
     * @return New PalettizedCuboid wrapping the supplied tag.
     */
    @SuppressWarnings("unchecked")
    public static <T extends Tag<?>> PalettizedCuboid<T> fromCompoundTag(CompoundTag tag, int expectedCubeEdgeLength, int dataVersion) {
        if (tag == null) {
            return null;
        }
        ListTag<T> paletteListTag = tag.getListTagAutoCast("palette");
        check(paletteListTag != null, "Did not find 'palette' ListTag");
        check(!paletteListTag.isEmpty(), "'palette' ListTag exists but it was empty!");
        LongArrayTag dataTag = tag.getLongArrayTag("data");
        if ((dataTag == null || dataTag.getValue().length == 0) && paletteListTag.size() > 1) {
            throw new IllegalArgumentException("Did not find 'data' LongArrayTag when expected");
        }
        return new PalettizedCuboid<>(expectedCubeEdgeLength, (Class<T>) paletteListTag.get(0).getClass(), tag, dataVersion);
    }

    /**
     * Creates a by-value iterator that will visit every entry and yield a clone of each entry.
     * <p>Tip: if you are using a java version that supports the {@code var} keyword (java 10+),
     * you can avoid using the otherwise unavoidably long typename with</p>
     * <pre>{@code var iter = cuboid.iterator();}</pre>
     *
     * @see CursorIterator
     */
    @Override
    public CursorIterator iterator() {
        return new CursorIterator(null);
    }

    /**
     * Creates a by-value iterator that will only visit entries which match the provided filter nd yield a clone of
     * each entry.
     * @see CursorIterator
     */
    public CursorIterator iterator(Predicate<E> filter) {
        return new CursorIterator(filter);
    }

    public Stream<E> stream() {
        return StreamSupport.stream(spliterator(), false);
    }

    @Override
    public CompoundTag getHandle() {
        return paletteContainerTag;
    }

    @Override
    public CompoundTag updateHandle() {
        return toCompoundTag();
    }

    /**
     * This is both an iterator and a cursor. This cursor knows where it is and can
     * be inspected to get additional information about the last item returned by {@link CursorIterator#next()}
     * such as its XYZ cuboid location and index in the data array.
     *
     * <h2>Warning - this iterator yields values by reference</h2>
     * This iterator yields palette values by reference! Do not modify the object returned by {@link #next()} or
     * {@link #current()}. If you want to keep a copy of the yielded value {@code .clone()} it. If you want to modify
     * the value at the current iteration position {@code .clone()} it first, then call {@link #set(Tag)}. You should
     * not modify the cuboid while iterating over it other than through the iterator itself.
     *
     * <p>The iterator will make a best-effort to detect palette corruption and throw a
     * {@link PaletteCorruptedException} if detected and throw {@link ConcurrentModificationException} if the
     * cuboid palette is modified during iteration.</p>
     *
     * <h2>Remember</h2>
     * All {@link Tag}'s support cloning. If you are careful it's always faster / more efficient
     * to iterate by reference and clone entries only when needed. But this optimization comes at the risk of
     * accidentally corrupting the palette.
     *
     * <h2>Filtering Mode</h2>
     * When a filter is provided {@link #next()} will only return the next matching entry, skipping as necessary.
     * {@link #hasNext()} will behave appropriately and indicate if there is a next matching entry or not.
     * <p>While you could use a {@link Stream} to filter and process entries of interest, using a filter on a cursor
     * iterator allows you to get more information about the current entry, such as its xyz cuboid position.</p>
     *
     * @see PalettizedCuboid#iterator(Predicate)
     */
    public class CursorIterator implements java.util.Iterator<E> {
        private final Predicate<E> filter;  // nullable
        private int currentIndex = -1;
        private int nextIndex = 0;  // only used if filter isn't null
        int currentPaletteHash;
        int expectPaletteModCount = paletteModCount;

        CursorIterator(Predicate<E> filter) {
            this.filter = filter;
        }

        private void checkNotModified() {
            if (expectPaletteModCount != paletteModCount) {
                throw new ConcurrentModificationException();
            }
            if (currentIndex >= 0 && currentPaletteHash != get(currentIndex).hashCode()) {
                throw new PaletteCorruptedException(
                        "Palette modified during iteration! Be sure to .clone() on the yielded element before " +
                                "modifying it and use .set() to update the data value at the current position.");
            }
        }

        private void checkCurrentIndex() {
            if (currentIndex < 0) {  // note currentIndex will never be GE data.length
                throw new NoSuchElementException();
            }
        }

        @Override
        public boolean hasNext() {
            checkNotModified();
            if (filter == null) {
                return currentIndex < packedData.length - 1;
            }
            while (nextIndex < packedData.length) {
                if (filter.test(get(nextIndex))) {
                    return true;
                }
                nextIndex ++;
            }
            return false;
        }

        @Override
        public E next() {
            if (!hasNext()) {
                throw new NoSuchElementException();
            }
            E ret;
            if (filter == null) {
                ret = getByRef(++currentIndex);
            } else {
                ret = get(currentIndex = nextIndex++);
            }
            currentPaletteHash = ret.hashCode();
            return ret;
        }

        /** Advances to the next entry and returns its XYZ. */
        public IntPointXYZ nextXYZ() {
            next();
            return currentXYZ();
        }

        /**
         * Updates the cuboid entry at the index belonging to the last value returned by {@link #next()}.
         */
        public void set(E replacement) {
            checkNotModified();
            checkCurrentIndex();
            PalettizedCuboid.this.set(currentIndex, replacement);
            expectPaletteModCount = paletteModCount;
            currentPaletteHash = replacement.hashCode();
        }

        /**
         * Gets the last entry returned by {@link #next()}.
         */
        public E current() {
            checkCurrentIndex();
            return getByRef(currentIndex);
        }

        /** Gets the cuboid data index of the last entry returned by {@link #next()}. */
        public int currentIndex() {
            checkCurrentIndex();
            return currentIndex;
        }

        /** Gets the cuboid x position of the last entry returned by {@link #next()}. */
        public int currentX() {
            checkCurrentIndex();
            return currentIndex & cubeInfo.cordBitMask;
        }

        /** Gets the cuboid y position of the last entry returned by {@link #next()}. */
        public int currentY() {
            checkCurrentIndex();
            return (currentIndex >> cubeInfo.yShift) & cubeInfo.cordBitMask;
        }

        /** Gets the cuboid z position of the last entry returned by {@link #next()}. */
        public int currentZ() {
            checkCurrentIndex();
            return (currentIndex >> cubeInfo.zShift) & cubeInfo.cordBitMask;
        }

        /** Gets the cuboid xyz position of the last entry returned by {@link #next()}. */
        public IntPointXYZ currentXYZ() {
            checkCurrentIndex();
            return xyzOf(currentIndex);
        }
    }

    public static class PaletteCorruptedException extends RuntimeException {
        public PaletteCorruptedException(String message) {
            super(message);
        }
    }
}
