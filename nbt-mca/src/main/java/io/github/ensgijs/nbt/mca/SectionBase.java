package io.github.ensgijs.nbt.mca;

import io.github.ensgijs.nbt.tag.CompoundTag;
import io.github.ensgijs.nbt.util.ObservedCompoundTag;
import io.github.ensgijs.nbt.mca.io.LoadFlags;
import io.github.ensgijs.nbt.mca.util.TagWrapper;
import io.github.ensgijs.nbt.mca.util.TracksUnreadDataTags;

import java.util.*;

import static io.github.ensgijs.nbt.mca.io.LoadFlags.RAW;
import static io.github.ensgijs.nbt.mca.io.LoadFlags.RELEASE_CHUNK_DATA_TAG;

/**
 * Sections can be thought of as "sub-chunks" which are 16x16x16 block cubes
 * stacked atop each other to create a "chunk".
 */
public abstract class SectionBase<T extends SectionBase<?>> implements Comparable<T>, TagWrapper<CompoundTag>, TracksUnreadDataTags {
	/** Used to indicate an unset section Y value. */
	public static final int NO_SECTION_Y_SENTINEL = Integer.MIN_VALUE;
	/** for internal use only - modify with extreme care and precision - must be kept in sync with chunk data version */
	protected int dataVersion;
	private boolean raw;
	protected CompoundTag data;
	protected Set<String> unreadDataTagKeys;
	/**
	 * The height of the bottom of this section relative to Y0 as a section-y value, each 1 section-y is
	 * equal to 16 blocks.
	 * <p>AKA: "height"</p>
	 */
	protected int sectionY = NO_SECTION_Y_SENTINEL;

	/**
	 * {@inheritDoc}
	 */
	public Set<String> getUnreadDataTagKeys() {
		return unreadDataTagKeys;
	}

	/**
	 * {@inheritDoc}
	 * @return NotNull - if LoadFlags specified {@link LoadFlags#RAW} then the raw data is returned - else a new
	 * CompoundTag populated, by reference, with values that were not read during {@link #initReferences(long)}.
	 */
	public CompoundTag getUnreadDataTags() {
		if (raw) return data;
		CompoundTag unread = new CompoundTag(unreadDataTagKeys.size());
		data.forEach((k, v) -> {
			if (unreadDataTagKeys.contains(k)) {
				unread.put(k, v);
			}
		});
		return unread;
	}

	/**
	 * Due to how Java initializes objects and how this class hierarchy is setup it is ill-advised to use inline member
	 * initialization because {@link #initReferences(long)} will be called before members are initialized which WILL
	 * result in very confusing {@link NullPointerException}'s being thrown from within {@link #initReferences(long)}.
	 * This is not a problem that can be solved by moving initialization into your constructors, because you must call
	 * the super constructor as the first line of your child constructor!
	 * <p>So, to get around this hurdle, perform all member initialization you would normally inline in your
	 * class def, within this method instead. Implementers should never need to call this method themselves
	 * as ChunkBase will always call it, even from the default constructor. Remember to call {@code super();}
	 * from your default constructors to maintain this behavior.</p>
	 */
	protected void initMembers() { }

	protected SectionBase(int dataVersion) {
		data = new CompoundTag();
		this.dataVersion = dataVersion;
		initMembers();
	}

	protected SectionBase(CompoundTag sectionRoot, int dataVersion, long loadFlags) {
		Objects.requireNonNull(sectionRoot, "sectionRoot must not be null");
		this.data = sectionRoot;
		this.dataVersion = dataVersion;
		initMembers();
		initReferences0(loadFlags);
	}

	private void initReferences0(long loadFlags) {
		Objects.requireNonNull(data, "data cannot be null");
		// Note that if RAW was specified in the loadFlags section data will not be loaded by chunk loading.
		// However, the user may decide to not use chunk loading and create an instance of this class directly,
		// so we should honor it anyway.
		if ((loadFlags & RAW) != 0) {
			raw = true;
		} else {
			final ObservedCompoundTag observedData = new ObservedCompoundTag(data);
			data = observedData;
			initReferences(loadFlags);
			if (data != observedData) {
				throw new IllegalStateException("this.data was replaced during initReferences execution - this breaks unreadDataTagKeys behavior!");
			}
			unreadDataTagKeys = observedData.unreadKeys();

			if ((loadFlags & RELEASE_CHUNK_DATA_TAG) != 0) {
				data = new CompoundTag();
			} else {
				// stop observing the data tag
				data = observedData.wrappedTag();
			}
		}
	}

	/**
	 * Child classes should not call this method directly, it will be called for them.
	 * Raw and partial data handling is taken care of, this method will not be called if {@code loadFlags}
	 * contains {@link LoadFlags#RAW}.
	 */
	protected abstract void initReferences(final long loadFlags);

	/** Section data version must be kept in sync with chunk data version. Use with extreme care! */
	protected void syncDataVersion(int newDataVersion) {
		if (newDataVersion <= 0) {
			throw new IllegalArgumentException("Invalid data version - must be GT 0");
		}
		this.dataVersion = newDataVersion;
	}

	@Override
	public int compareTo(T o) {
		if (o == null) {
			return -1;
		}
		return Integer.compare(sectionY, o.sectionY);
	}

	/**
	 * Checks whether the data of this Section is empty.
	 * @return true if empty
	 */
	public boolean isEmpty() {
		return data.isEmpty();
	}

	/**
	 * Gets the height of the bottom of this section relative to Y0 as a section-y value, each 1 section-y is equal to
	 * 16 blocks.
	 * This library (as a whole) will attempt to keep the value returned by this function in sync with the actual
	 * location it has been placed within its chunk.
	 * <p>The value returned may be unreliable if this section is placed in multiple chunks at different heights
	 * or if this section is an instance of {@link TerrainSection} and user code calls {@link TerrainSection#setHeight(int)}
	 * on a section which is referenced by any chunk.</p>
	 * <p>Prefer using {@link TerrainChunk#getSectionY(SectionBase)} which will always be accurate within the context of the
	 * chunk.</p>
	 * @return The Y value of this section.
	 */
	public int getSectionY() {
		return sectionY;
	}

	protected void syncHeight(int height) {
		this.sectionY = height;
	}

	protected void checkY(int y) {
		if (y == NO_SECTION_Y_SENTINEL) {
			throw new IndexOutOfBoundsException("section Y (aka 'height') not set");
		}
		if (y < Byte.MIN_VALUE | y > Byte.MAX_VALUE) {
			throw new IndexOutOfBoundsException("section Y (aka 'height') must be in range of BYTE [-128..127] was: " + y);
		}
	}

	/**
	 * {@inheritDoc}
	 */
	public CompoundTag getHandle() {
		return data;
	}

	/**
	 * {@inheritDoc}
	 */
	public CompoundTag updateHandle() {
		if (data == null) {
			throw new UnsupportedOperationException(
					"Cannot updateHandle() because data tag is null. This is probably because "+
							"the LoadFlag RELEASE_CHUNK_DATA_TAG was specified");
		}
		return data;
	}
}
