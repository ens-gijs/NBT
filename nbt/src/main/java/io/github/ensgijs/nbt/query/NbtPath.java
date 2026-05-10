package io.github.ensgijs.nbt.query;

import io.github.ensgijs.nbt.query.evaluator.Evaluator;
import io.github.ensgijs.nbt.query.evaluator.IndexEvaluator;
import io.github.ensgijs.nbt.query.evaluator.NameEvaluator;
import io.github.ensgijs.nbt.tag.*;
import io.github.ensgijs.nbt.util.ArgValidator;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Provides a simple mechanism to retrieve, and store, structured data without
 * having to handle the intermediate tags yourself.
 * <p>Use simple dot separated names and bracket indexes such as {@code "Level.Section[0].BlockLight"}</p>
 * <p>Colons are allowed in the path such as {@code Brain.memories.minecraft:home.value.pos}</p>
 */
public class NbtPath {
    private static final NbtPath IDENTITY_PATH = new NbtPath(Collections.emptyList());

    protected List<Evaluator> evalChain;

    protected NbtPath(List<Evaluator> evalChain) {
        this.evalChain = Collections.unmodifiableList(evalChain);
    }

    /**
     * Injects "&lt;&gt;" into the string at the specified position optionally wrapping some content as
     * specified by width.
     * @param str string to decorate
     * @param pos position to insert &lt;
     * @param width how many characters to skip before inserting &gt;
     * @return modified str
     */
    static String markLocation(String str, int pos, int width) {
        assert width >= 0;
        if (pos < 0) return "<>" + str;
        if (pos >= str.length()) return str + "<>";
        if (width <= 0) {
            return str.substring(0, pos) + "<>" + str.substring(pos);
        }
        return str.substring(0, pos) + "<" + str.substring(pos, pos + width) + ">" + str.substring(pos + width);
    }

    /**
     * Creates a new {@link NbtPath} from the given selector string.
     * @param selector Use dots to separate names/keys and use array notation for indexing {@link ListTag}
     *                 and {@link ArrayTag}'s. Example: {@code "Level.Section[0].BlockLight"}
     *                 Colons are allowed in the path such as {@code Brain.memories.minecraft:home.value.pos}
     * @return new {@link NbtPath}
     */
    public static NbtPath of(String selector) {
        if (selector == null || selector.isEmpty() || selector.equals(".")) return IDENTITY_PATH;
        List<Evaluator> evalChain = new ArrayList<>();
        int partPos = 0;
        String[] parts = selector.split("[.]", -1);
        boolean allowEmpty = true;

        for (String part : parts) {
            if (part.isEmpty() && !allowEmpty) throw new IllegalArgumentException("empty name at: " + markLocation(selector, partPos, 0));
            int bracketOpenIdx = part.indexOf('[');
            int bracketCloseIdx = part.indexOf(']');
            String name;
            if (bracketOpenIdx < 0 || bracketCloseIdx < bracketOpenIdx) {
                if (bracketCloseIdx >= 0) throw new IllegalArgumentException("unexpected close bracket at: " + markLocation(selector, bracketCloseIdx + partPos, 1));
                name = part;
            } else {
                if (bracketCloseIdx < 0) throw new IllegalArgumentException("unclose bracket at: " + markLocation(selector, bracketOpenIdx + partPos, 1));
                name = part.substring(0, bracketOpenIdx);
            }

            if (!name.isEmpty()) {
                evalChain.add(new NameEvaluator(name));
            } else if (!evalChain.isEmpty() || !allowEmpty) {
                throw new IllegalArgumentException("expected map key name at: " + markLocation(selector, partPos, 0));
            }
            while (bracketOpenIdx >= 0) {
                if (bracketCloseIdx < 0)
                    throw new IllegalArgumentException("missing close bracket for: " + markLocation(selector, bracketOpenIdx + partPos, 1));
                String valStr = part.substring(bracketOpenIdx + 1, bracketCloseIdx);
                if (valStr.isEmpty() || !valStr.chars().allMatch(Character::isDigit))
                    throw new IllegalArgumentException("list index must be a positive number at: " + markLocation(selector, bracketOpenIdx + partPos + 1, bracketCloseIdx - bracketOpenIdx - 1));

                evalChain.add(new IndexEvaluator(Integer.parseInt(valStr)));
                bracketOpenIdx = part.indexOf('[', bracketCloseIdx + 1);
                if (bracketOpenIdx != -1 && bracketOpenIdx != bracketCloseIdx + 1) {
                    throw new IllegalArgumentException("invalid path string - error at: " + markLocation(selector, bracketCloseIdx + 1 + partPos, 0));
                }
                bracketCloseIdx = part.indexOf(']', bracketCloseIdx + 1);
            }
            if (bracketCloseIdx > 0)
                throw new IllegalArgumentException();
            partPos += name.length() + 1;
            allowEmpty = false;
        }
        return new NbtPath(evalChain);
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        boolean canDot = false;
        for (Evaluator evaluator : evalChain) {
            if (evaluator instanceof NameEvaluator) {
                if (canDot) sb.append('.');
            }
            sb.append(evaluator);
            canDot = true;
        }
        return sb.toString();
    }

    protected String makeErrorHint(int atIndex) {
        return makeErrorHint(evalChain.get(atIndex));
    }

    /**
     * Just like toString except it wraps the specified evaluator in &lt; and &gt;
     * @param atEvaluator evaluator to wrap
     * @return modified toString
     */
    protected String makeErrorHint(Evaluator atEvaluator) {
        StringBuilder sb = new StringBuilder();
        boolean canDot = false;
        for (Evaluator evaluator : evalChain) {
            if (evaluator instanceof NameEvaluator) {
                if (canDot) sb.append('.');
            }
            if (evaluator == atEvaluator) sb.append("<");
            sb.append(evaluator);
            if (evaluator == atEvaluator) sb.append(">");
            canDot = true;
        }
        return sb.toString();
    }

    /**
     * Evaluates this {@link NbtPath} against the provided {@link Tag}. Note that this method may return
     * {@link Tag}'s, but it may also return a primitive type such as byte/int/long if the leaf in the
     * path is an index into one of the {@link ArrayTag} types.
     *
     * <p>The caller must know the type which will be returned. Any type errors will result in a
     * {@link ClassCastException} being thrown at the call site of this method - not from within this method.</p>
     * @param root tag to begin traversal from.
     * @param <R> return type - note that if this function returns an unexpected type you will see a
     *      {@link ClassCastException} thrown at the call site of this method - not from within this method.
     * @return result or null
     * @see #getTag(Tag) 
     */
    @SuppressWarnings("unchecked")
    public <R> R get(Tag<?> root) {
        Object node = root;
        for (Evaluator evaluator : evalChain) {
            if (node == null) return null;
            if (!(node instanceof Tag)) throw new IllegalStateException("Expected TAG but was " + node.getClass().getTypeName() + "\n" + makeErrorHint(evaluator));
            node = evaluator.eval((Tag<?>) node);
        }
        return (R) node;
    }

    /**
     * Just like {@link #get(Tag)} except auto-castable to a {@link Tag} type.
     *
     * <p>The caller must know the type which will be returned. Any type errors will result in a
     * {@link ClassCastException} being thrown at the call site of this method - not from within this method.</p>
     * @see #get(Tag)
     */
    public <R extends Tag<?>> R getTag(Tag<?> root) {
        return get(root);
    }

    /**
     * Gets the value found by this path as a String.
     * @param root tag to begin traversal from.
     * @return String value or null if not found (note that a StringTag may be the empty string but never contain null;
     * therefore null indicates the value was not set).
     * @throws ClassCastException if the tag exists and was not a {@link StringTag}
     */
    public String getString(Tag<?> root) {
        Object value = get(root);
        if (value instanceof StringTag) return ((StringTag) value).getValue();
        if (value instanceof String) return (String) value;
        if (value == null) return null;
        throw new ClassCastException("expected string but was " + value.getClass().getTypeName());
    }

    /**
     * Gets the value found by this path as a boolean. This helper follows the convention that the truthiness of
     * a tag is FALSE for all cases except a {@link ByteTag} with a positive value.
     * @param root tag to begin traversal from.
     * @return truthiness of the tag found by this path (default of false if the tag does not exist or is not a
     * {@link ByteTag} with a positive value).
     */
    public boolean getBoolean(Tag<?> root) {
        Object value = get(root);
        return value instanceof ByteTag && ((ByteTag) value).asByte() > 0;
    }

    /**
     * Gets the value found by this path as a byte. Note that this method can be used on any {@link NumberTag} or
     * an index into any {@link ArrayTag}, not just {@link ByteTag}'s.
     * @param root tag to begin traversal from.
     * @return value cast to a byte
     */
    public byte getByte(Tag<?> root) {
        Object value = get(root);
        if (value instanceof NumberTag) {
            return ((NumberTag<?>) value).asByte();
        }
        if (value == null) return 0;
        return (byte) value;
    }

    public byte[] getByteArray(Tag<?> root) {
        Object value = get(root);
        if (value instanceof ByteArrayTag) {
            return ((ByteArrayTag) value).getValue();
        }
        return null;
    }

    /**
     * Gets the value found by this path as a short. Note that this method can be used on any {@link NumberTag} or
     * an index into any {@link ArrayTag}, not just {@link ShortTag}'s.
     * @param root tag to begin traversal from.
     * @return value cast to a short
     */
    public short getShort(Tag<?> root) {
        Object value = get(root);
        if (value instanceof NumberTag) {
            return ((NumberTag<?>) value).asShort();
        }
        if (value == null) return 0;
        return (short) value;
    }

    /**
     * Gets the value found by this path as an int. Note that this method can be used on any {@link NumberTag} or
     * an index into any {@link ArrayTag}, not just {@link IntTag}'s.
     * @param root tag to begin traversal from.
     * @return value cast to an int
     */
    public int getInt(Tag<?> root) {
        Object value = get(root);
        if (value instanceof NumberTag) {
            return ((NumberTag<?>) value).asInt();
        }
        if (value == null) return 0;
        return (int) value;
    }

    public int[] getIntArray(Tag<?> root) {
        Object value = get(root);
        if (value instanceof IntArrayTag) {
            return ((IntArrayTag) value).getValue();
        }
        return null;
    }

    /**
     * Gets the value found by this path as a long. Note that this method can be used on any {@link NumberTag} or
     * an index into any {@link ArrayTag}, not just {@link LongTag}'s.
     * @param root tag to begin traversal from.
     * @return value cast to a long
     */
    public long getLong(Tag<?> root) {
        Object value = get(root);
        if (value instanceof NumberTag) {
            return ((NumberTag<?>) value).asLong();
        }
        if (value == null) return 0;
        return (long) value;
    }

    public long[] getLongArray(Tag<?> root) {
        Object value = get(root);
        if (value instanceof LongArrayTag) {
            return ((LongArrayTag) value).getValue();
        }
        return null;
    }


    /**
     * Gets the value found by this path as a float. Note that this method can be used on any {@link NumberTag} or
     * an index into any {@link ArrayTag}, not just {@link FloatTag}'s.
     * @param root tag to begin traversal from.
     * @return value cast to a float
     */
    public float getFloat(Tag<?> root) {
        Object value = get(root);
        if (value instanceof NumberTag) {
            return ((NumberTag<?>) value).asFloat();
        }
        if (value == null) return 0;
        return (float) value;
    }

    /**
     * Gets the value found by this path as a double. Note that this method can be used on any {@link NumberTag} or
     * an index into any {@link ArrayTag}, not just {@link DoubleTag}'s.
     * @param root tag to begin traversal from.
     * @return value cast to a double
     */
    public double getDouble(Tag<?> root) {
        Object value = get(root);
        if (value instanceof NumberTag) {
            return ((NumberTag<?>) value).asDouble();
        }
        if (value == null) return 0;
        return (double) value;
    }

    /**
     * Shorthand for {@code putTag(root, value, createParents=false);}
     * @see #putTag(Tag, Tag, boolean)
     */
    public <T extends Tag<?>> T putTag(Tag<?> root, Tag<?> value) {
        return putTag(root, value, false);
    }

    /**
     * Putting a new list of lists tag can be finicky - if you get an "inference variable has incompatible upper bounds"
     * error try this.
     * <pre>{@code nbtpath.putTag(tag, new ListTag<ListTag<?>>(ListTag.class))}</pre>
     * @param root root tag to evaluate this path from
     * @param value tag value to set, may be null to remove the tag
     * @param createParents when true CompoundTag's will be created as necessary. Will not create ListTag's or ArrayTag's.
     * @param <T> value tag type
     * @return the previous value associated with key, or null if there was no mapping for key.
     */
    @SuppressWarnings("unchecked")
    public <T extends Tag<?>> T putTag(Tag<?> root, Tag<?> value, boolean createParents) {
        ArgValidator.requireValue(root, "root");
        ArgValidator.check(evalChain.size() > 0);
        Tag<?> parent;
        Tag<?> node = root;
        for (int i = 0, l = evalChain.size() - 1; i < l; i ++) {
            parent = node;
            Evaluator evaluator = evalChain.get(i);
            node = (Tag<?>) evaluator.eval(node);
            if (node == null && createParents) {
                if (parent instanceof CompoundTag && evaluator instanceof NameEvaluator && evalChain.get(i + 1) instanceof NameEvaluator) {
                    node = new CompoundTag();
                    ((CompoundTag) parent).put(((NameEvaluator) evaluator).key(), node);
                } else {
                    throw new UnsupportedOperationException("cannot auto-create child tag at " + makeErrorHint(evaluator));
                }
            }

            if (node == null) {
                throw new IllegalArgumentException("Tag does not exist: " + makeErrorHint(evaluator));
            }
        }
        Evaluator last = evalChain.get(evalChain.size() - 1);
        if (node instanceof CompoundTag) {
            if (!(last instanceof NameEvaluator)) throw new IllegalArgumentException();
            if (value != null) {
                node = ((CompoundTag) node).put(((NameEvaluator) last).key(), value);
            } else {
                node = ((CompoundTag) node).remove(((NameEvaluator) last).key());
            }
        } else {
            throw new UnsupportedOperationException("expected CompoundTag but was " + node.getClass().getSimpleName() + " at: "
                    + makeErrorHint(last));
        }
        return (T) node;
    }

    // <editor-fold desc="put primitive values" defaultstate="collapsed">
    public ByteTag put(Tag<?> root, boolean value) {
        return putTag(root, new ByteTag(value), false);
    }

    public ByteArrayTag put(Tag<?> root, byte[] value) {
        return putTag(root, new ByteArrayTag(value), false);
    }

    public ByteTag put(Tag<?> root, byte value) {
        return putTag(root, new ByteTag(value), false);
    }

    public DoubleTag put(Tag<?> root, double value) {
        return putTag(root, new DoubleTag(value), false);
    }

    public FloatTag put(Tag<?> root, float value) {
        return putTag(root, new FloatTag(value), false);
    }

    public IntArrayTag put(Tag<?> root, int[] value) {
        return putTag(root, new IntArrayTag(value), false);
    }

    public IntTag put(Tag<?> root, int value) {
        return putTag(root, new IntTag(value), false);
    }

    public LongArrayTag put(Tag<?> root, long[] value) {
        return putTag(root, new LongArrayTag(value), false);
    }

    public LongTag put(Tag<?> root, long value) {
        return putTag(root, new LongTag(value), false);
    }

    public ShortTag put(Tag<?> root, short value) {
        return putTag(root, new ShortTag(value), false);
    }

    public StringTag put(Tag<?> root, String value) {
        return putTag(root, new StringTag(value), false);
    }


    public ByteTag put(Tag<?> root, boolean value, boolean createParents) {
        return putTag(root, new ByteTag(value), createParents);
    }

    public ByteArrayTag put(Tag<?> root, byte[] value, boolean createParents) {
        return putTag(root, new ByteArrayTag(value), createParents);
    }

    public ByteTag put(Tag<?> root, byte value, boolean createParents) {
        return putTag(root, new ByteTag(value), createParents);
    }

    public DoubleTag put(Tag<?> root, double value, boolean createParents) {
        return putTag(root, new DoubleTag(value), createParents);
    }

    public FloatTag put(Tag<?> root, float value, boolean createParents) {
        return putTag(root, new FloatTag(value), createParents);
    }

    public IntArrayTag put(Tag<?> root, int[] value, boolean createParents) {
        return putTag(root, new IntArrayTag(value), createParents);
    }

    public IntTag put(Tag<?> root, int value, boolean createParents) {
        return putTag(root, new IntTag(value), createParents);
    }

    public LongArrayTag put(Tag<?> root, long[] value, boolean createParents) {
        return putTag(root, new LongArrayTag(value), createParents);
    }

    public LongTag put(Tag<?> root, long value, boolean createParents) {
        return putTag(root, new LongTag(value), createParents);
    }

    public ShortTag put(Tag<?> root, short value, boolean createParents) {
        return putTag(root, new ShortTag(value), createParents);
    }

    public StringTag put(Tag<?> root, String value, boolean createParents) {
        return putTag(root, new StringTag(value), createParents);
    }
    // </editor-fold>

    /**
     * Gets the size, or length, of the tag at this path.
     * @param root root tag to evaluate this path from
     * @return size/length of tag if exists, 0 if the tag doesn't exist.
     * @throws IllegalArgumentException if the type of tag found by this path doesn't have a reasonable size/length
     * property - such as a {@link DoubleTag}.
     */
    public int size(Tag<?> root) {
        Tag<?> tag = getTag(root);
        if (tag instanceof CompoundTag) {
            return ((CompoundTag) tag).size();
        } else if (tag instanceof ListTag) {
            return ((ListTag<?>) tag).size();
        } else if (tag instanceof ArrayTag) {
            return ((ArrayTag<?>) tag).length();
        } else if (tag instanceof StringTag) {
            String str = ((StringTag) tag).getValue();
            return str != null ? str.length() : 0;
        }
        if (tag == null) return 0;
        throw new IllegalArgumentException("don't know how to get the size of " + tag.getClass().getTypeName());
    }

    public boolean exists(Tag<?> root) {
        return root != null && get(root) != null;
    }
}
