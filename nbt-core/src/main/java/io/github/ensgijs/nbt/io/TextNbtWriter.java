package io.github.ensgijs.nbt.io;

import io.github.ensgijs.nbt.tag.ByteArrayTag;
import io.github.ensgijs.nbt.tag.ByteTag;
import io.github.ensgijs.nbt.tag.CompoundTag;
import io.github.ensgijs.nbt.tag.DoubleTag;
import io.github.ensgijs.nbt.tag.EndTag;
import io.github.ensgijs.nbt.tag.FloatTag;
import io.github.ensgijs.nbt.tag.IntArrayTag;
import io.github.ensgijs.nbt.tag.IntTag;
import io.github.ensgijs.nbt.tag.ListTag;
import io.github.ensgijs.nbt.tag.LongArrayTag;
import io.github.ensgijs.nbt.tag.LongTag;
import io.github.ensgijs.nbt.tag.ShortTag;
import io.github.ensgijs.nbt.tag.StringTag;
import io.github.ensgijs.nbt.tag.Tag;
import java.io.IOException;
import java.io.Writer;
import java.lang.reflect.Array;
import java.util.Iterator;

/**
 * TextNbtWriter creates a text NBT String.
 */
public final class TextNbtWriter implements MaxDepthIO {

	private Writer writer;

	private TextNbtWriter(Writer writer) {
		this.writer = writer;
	}

	public static void write(NamedTag tag, Writer writer, boolean sortCompoundTagEntries, int maxDepth) throws IOException {
		new TextNbtWriter(writer).writeAnything(tag, sortCompoundTagEntries, maxDepth);
	}

	public static void write(NamedTag tag, Writer writer, int maxDepth) throws IOException {
		new TextNbtWriter(writer).writeAnything(tag, false, maxDepth);
	}

	public static void write(NamedTag tag, Writer writer) throws IOException {
		write(tag, writer, Tag.DEFAULT_MAX_DEPTH);
	}

	public static void write(Tag<?> tag, Writer writer, int maxDepth) throws IOException {
		new TextNbtWriter(writer).writeAnything(tag, false, maxDepth);
	}

	public static void write(Tag<?> tag, Writer writer) throws IOException {
		write(tag, writer, Tag.DEFAULT_MAX_DEPTH);
	}

	private void writeAnything(NamedTag tag, boolean sortCompoundTagEntries, int maxDepth) throws IOException {
		// note to future self: if you're ever compelled not write an empty name be sure to
		// consider what that means for TextNbtParser#readTag(int)
		if (tag.getName() != null) {
			writer.write(tag.getEscapedName());
			writer.write(':');
		}
		writeAnything(tag.getTag(), sortCompoundTagEntries, maxDepth);
	}

	private void writeAnything(Tag<?> tag, boolean sortCompoundTagEntries, int maxDepth) throws IOException {
		switch (tag.getID()) {
		case EndTag.ID:
			//do nothing
			break;
		case ByteTag.ID:
			writer.append(Byte.toString(((ByteTag) tag).asByte())).write('b');
			break;
		case ShortTag.ID:
			writer.append(Short.toString(((ShortTag) tag).asShort())).write('s');
			break;
		case IntTag.ID:
			writer.write(Integer.toString(((IntTag) tag).asInt()));
			break;
		case LongTag.ID:
			writer.append(Long.toString(((LongTag) tag).asLong())).write('l');
			break;
		case FloatTag.ID:
			writer.append(Float.toString(((FloatTag) tag).asFloat())).write('f');
			break;
		case DoubleTag.ID:
			writer.append(Double.toString(((DoubleTag) tag).asDouble())).write('d');
			break;
		case ByteArrayTag.ID:
			writeArray(((ByteArrayTag) tag).getValue(), ((ByteArrayTag) tag).length(), "B");
			break;
		case StringTag.ID:
			writer.write(StringTag.escapeString(((StringTag) tag).getValue(), true));
			break;
		case ListTag.ID:
			writer.write('[');
			for (int i = 0; i < ((ListTag<?>) tag).size(); i++) {
				writer.write(i == 0 ? "" : ",");
				writeAnything(((ListTag<?>) tag).get(i), sortCompoundTagEntries, decrementMaxDepth(maxDepth));
			}
			writer.write(']');
			break;
		case CompoundTag.ID:
			writer.write('{');
			boolean first = true;
			Iterator<NamedTag> iter;
			if (sortCompoundTagEntries) iter = ((CompoundTag) tag).stream().sorted(NamedTag::compare).iterator();
			else iter = ((CompoundTag) tag).iterator();
			while (iter.hasNext()) {
				NamedTag entry = iter.next();
				writer.write(first ? "" : ",");
				writer.append(NamedTag.escapeName(entry.getName())).write(':');
				writeAnything(entry.getTag(), sortCompoundTagEntries, decrementMaxDepth(maxDepth));
				first = false;
			}
			writer.write('}');
			break;
		case IntArrayTag.ID:
			writeArray(((IntArrayTag) tag).getValue(), ((IntArrayTag) tag).length(), "I");
			break;
		case LongArrayTag.ID:
			writeArray(((LongArrayTag) tag).getValue(), ((LongArrayTag) tag).length(), "L");
			break;
		default:
			throw new IOException("unknown tag with id \"" + tag.getID() + "\"");
		}
	}

	private void writeArray(Object array, int length, String prefix) throws IOException {
		writer.append('[').append(prefix).write(';');
		for (int i = 0; i < length; i++) {
			writer.append(i == 0 ? "" : ",").write(Array.get(array, i).toString());
		}
		writer.write(']');
	}
}
