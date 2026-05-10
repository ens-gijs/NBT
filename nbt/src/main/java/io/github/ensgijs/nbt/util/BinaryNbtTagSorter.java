package io.github.ensgijs.nbt.util;

import java.io.IOException;
import java.nio.BufferOverflowException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

/**
 * A high-performance hybrid NBT sorter.
 *
 * <p>This implementation uses a hybrid strategy to achieve maximum throughput:
 * <ol>
 * <li><b>Scan-Once Tokenizer:</b> Parse and tokenize the entire NBT byte stream in a single
 * pass, creating a flat array of {@link TagPointer} objects that map the nbt structure.
 * This avoids costly re-scans of the input data.</li>
 * <li><b>In-Place Index Sorting:</b> Instead of array copying heavy {@code TagPointer}
 * objects to directly reorder them, a lightweight array of integer indices is used as a
 * proxy for ordering the children of each compound tag completely eliminating
 * {@code System.arraycopy} calls for reordering.
 * This sorted "index map" is attached to the parent tag's pointer.</li>
 * <li><b>Recursive Emitter:</b> A lean, recursive function is used to write the output.
 * This emitter traverses the token array, following the guidance of the sorted index maps
 * to construct the final, sorted byte stream.</li>
 * <li><b>Pooled tokens:</b> Tokens are pooled and reused between {@link #sort(byte[])}
 * calls. <em>There is little performance difference between pooling and not pooling tokens
 * in benchmark tests, however, pooling and recycling tokens reduces memory pressure which
 * may provide performance benefits when this class is used in a real application. Each token
 * occupies 40 or 48 bytes of memory (depending on JVM settings).</em></li>
 * </ol>
 * <p><b>Recommended usage pattern</b> is to reuse a BinaryNbtTagSorter instance and for the
 * instance to have a medium to immortal lifetime. Medium lifetime example would be creating
 * an instance to process a batch of NBT data then discarding it. An immortal lifetime would
 * be to create an instance which shares its lifetime with your plugin. The overhead of creating
 * a new BinaryNbtTagSorter to process each entry in a batch is marginally less performant
 * (a few percent slower) than reusing an instance. This penalty will vary depending on the
 * size and complexity of the nbt data being processed. If you are processing truly huge
 * nbt data (each nbt multiple megabytes in size) you'll need to decide how you want to
 * balance performance of sorting throughput against the fixed memory footprint cost of
 * holding the BinaryNbtTagSorter instance for reuse.</p>
 */
public class BinaryNbtTagSorter {
//    private static final boolean DEBUG_LOG = true;

    private static final byte END = (byte) 0;
    private static final byte BYTE = (byte) 1;
    private static final byte SHORT = (byte) 2;
    private static final byte INT = (byte) 3;
    private static final byte LONG = (byte) 4;
    private static final byte FLOAT = (byte) 5;
    private static final byte DOUBLE = (byte) 6;
    private static final byte BYTE_ARRAY = (byte) 7;
    private static final byte STRING = (byte) 8;
    private static final byte LIST = (byte) 9;
    private static final byte COMPOUND = (byte) 10;
    private static final byte INT_ARRAY = (byte) 11;
    private static final byte LONG_ARRAY = (byte) 12;
    private static final byte[] TAG_DATA_SIZE = new byte[] {0, 1, 2, 4, 8, 4, 8, 1, -1, -1, -1, 4, 8};

    private UnsafeFastByteArrayIO in;
    private TagPointer[] tokens = null;
    private int tokenCount;

    private TagPointer allocToken(final int start) {
        if (tokenCount >= tokens.length) {
            tokens = Arrays.copyOf(tokens, (tokens.length * 3 + 1) / 2);
            // if (DEBUG_LOG) System.out.printf("%s- Resizing tokens array to %d\n", "  ".repeat(depth), tokens.length);
        }
        TagPointer token = tokens[tokenCount++];
        if (token != null) {
            token.init(start);
            return token;
        } else {
            tokens[tokenCount - 1] = token = new TagPointer();
            token.init(start);
            return token;
        }
    }

    /**
     * The worst case count of {@link TagPointer}'s currently being held in the token pool.
     * <p>Each pooled token costs 40 or 48 bytes to hold (depending on JVM settings).</p>
     */
    public int tokenPoolSize() {
        return tokens.length;
    }

    /**
     * Sorts the given nbt data by ordering the contents of every CompoundTag (ListTag and array elements are NOT
     * reordered).
     * @param nbtData Data to be sorted.
     * @return If the given nbt data is already sorted the provided byte[] is returned,
     * otherwise a new ordered nbt byte[].
     * @throws IOException Parsing error.
     */
    public byte[] sort(final byte[] nbtData) throws IOException {
        // if (DEBUG_LOG) System.out.printf("--- Starting sort for %d bytes ---\n", nbtData.length);
        tokenCount = 0;
        // if (DEBUG_LOG) depth = 0;
        int want = Math.max(16, nbtData.length / 100);
        if (tokens == null || tokens.length < want) {
            tokens = new TagPointer[want];
            for (int i = tokenCount; i < tokens.length; i++) {
                tokens[i] = new TagPointer();
            }
            // if (DEBUG_LOG) System.out.printf("--- (Re)alloc tokens array size to %d ---\n", tokens.length);
        }
        in = new UnsafeFastByteArrayIO(nbtData);

        if (in.readByte() != COMPOUND)
            throw new IOException("Root tag is not a CompoundTag.");
        in.skipString();
        TagPointer rootTag = allocToken(0);
        rootTag.nameEnd = in.position;

        // if (DEBUG_LOG) System.out.printf("--- Root tag header scanned. Starting scanCompound at pos %d ---\n", in.position);
        scanCompound(rootTag);
        // if (DEBUG_LOG && depth != 0) throw new IllegalStateException();

        byte[] ret;
        if (rootTag.isContentsReordered) {
            // if (DEBUG_LOG) System.out.printf("--- Contents reordered. Emitting %d tokens ---\n", tokenCount);
            ret = emitTokens();
        } else {
            // if (DEBUG_LOG) System.out.println("--- No reordering needed. Returning original data ---");
            ret = nbtData;
        }
        in = null;
        return ret;
    }

    private void scanCompound(final TagPointer tagPtr) throws IOException {
        in.position = tagPtr.nameEnd > 0 ? tagPtr.nameEnd : tagPtr.start;
        // if (DEBUG_LOG) System.out.printf("%s>> Scanning CompoundTag (start=%d, depth=%d, pos=%d)\n", "  ".repeat(depth++), tagPtr.start, depth, in.position);
        int childCount = 0;
        final int firstChildTokenIndex = tokenCount;
        boolean childrenUnordered = false;
        int previousChildTokenIndex = -1;

        while (in.position < in.limit) {
            final byte type = in.readByte();
            if (type == END) {
                // if (DEBUG_LOG) System.out.printf("%s- Found END tag at pos %d\n", "  ".repeat(depth), in.position - 1);
                break;
            }
            childCount++;
            final int currentTokenIndex = tokenCount;
            final TagPointer childPtr = allocToken(in.position - 1);

            in.skipString();
            childPtr.nameEnd = in.position;
            // if (DEBUG_LOG) System.out.printf("%s- Found TagPointer #%d ('%s', Type %d) nameEnd=%d\n", "  ".repeat(depth), tokenCount - 1, in.debugReadString(childPtr.start + 1), type, childPtr.nameEnd);

            if (!childrenUnordered) {
                if (previousChildTokenIndex != -1 && compareTokens(previousChildTokenIndex, currentTokenIndex) > 0) {
                    childrenUnordered = true;
                    // if (DEBUG_LOG) System.out.printf("%s! Children discovered to be out-of-order, propagating reordered flag to parent\n", "  ".repeat(depth));
                }
                previousChildTokenIndex = currentTokenIndex;
            }

            scanTagPayload(type, childPtr);
            childPtr.end = in.position;
            tagPtr.tokenCount += childPtr.tokenCount;
            tagPtr.isContentsReordered |= childPtr.isContentsReordered;
            // if (DEBUG_LOG && childPtr.isContentsReordered) System.out.printf("%s! propagating child reordered flag to parent\n", "  ".repeat(depth));
        }
        tagPtr.end = in.position;
        tagPtr.childCount = childCount;
        tagPtr.childrenUnordered = childrenUnordered;

        if (childrenUnordered) {
            tagPtr.isContentsReordered = true;
        }

        if (!tagPtr.isContentsReordered) {
            // if (DEBUG_LOG) System.out.printf("%s! Children already ordered, crushed %d tokens (end=%d)\n", "  ".repeat(depth), tokenCount - firstChildTokenIndex, tagPtr.end);
            tokenCount = firstChildTokenIndex;
            tagPtr.tokenCount = 1;
        }
        // if (DEBUG_LOG) System.out.printf("%s<< CompoundTag scan ended at pos %d with %d children expressed by %d tokens\n", "  ".repeat(depth--), tagPtr.end, childCount, tagPtr.tokenCount);
    }

    private void scanList(final TagPointer tagPtr) throws IOException {
        tagPtr.isList = true;
        in.position = tagPtr.nameEnd > 0 ? tagPtr.nameEnd : tagPtr.start;
        // if (DEBUG_LOG) System.out.printf("%s>> Scanning ListTag (start=%d, depth=%d, pos=%d)\n", "  ".repeat(++depth), tagPtr.start, depth, in.position);

        final byte listType = in.readByte();
        final int listLength = in.readInt();
        tagPtr.childCount = listLength;
        // if (DEBUG_LOG) System.out.printf("%s- List type: %d, length: %d. Pos after header: %d\n", "  ".repeat(depth), listType, listLength, in.position);

        if (listType == COMPOUND || listType == LIST) {
            for (int i = 0; i < listLength; i++) {
                final TagPointer childPtr = allocToken(in.position);
                // if (DEBUG_LOG) System.out.printf("%s- List Item #%d. New TagPointer #%d at pos %d (Type %d)\n", "  ".repeat(depth), i, tokenCount - 1, in.position, listType);

                scanTagPayload(listType, childPtr);
                childPtr.end = in.position;
                tagPtr.tokenCount += childPtr.tokenCount;
                tagPtr.isContentsReordered |= childPtr.isContentsReordered;
                // if (DEBUG_LOG && childPtr.isContentsReordered) System.out.printf("%s! propagating child reordered flag to parent\n", "  ".repeat(depth));
            }
        } else if (listType >= BYTE_ARRAY) {
            for (int i = 0; i < listLength; i++) {
                scanTagPayload(listType, null);
            }
        } else {
            in.position += TAG_DATA_SIZE[listType] * listLength;
        }
        // if (DEBUG_LOG) depth--;
    }

    private void scanTagPayload(final byte type, final TagPointer childPtr) throws IOException {
        switch (type) {
            case BYTE:
                in.position++;
                break;
            case SHORT:
                in.position += 2;
                break;
            case INT, FLOAT:
                in.position += 4;
                break;
            case LONG, DOUBLE:
                in.position += 8;
                break;
            case STRING:
                in.skipString();
                break;
            case BYTE_ARRAY:
                in.position = in.readInt() + in.position;
                break;
            case INT_ARRAY:
                in.position = in.readInt() * 4 + in.position;
                break;
            case LONG_ARRAY:
                in.position = in.readInt() * 8 + in.position;
                break;
            case LIST:
                scanList(childPtr);
                break;
            case COMPOUND:
                scanCompound(childPtr);
                break;
            default:
                throw new IOException("Unknown tag type: " + type + " at pos " + in.position);
        }
    }

    private byte[] emitTokens() {
        UnsafeFastByteArrayIO out = new UnsafeFastByteArrayIO(new byte[in.limit]);
        // if (DEBUG_LOG) System.out.print("--- Starting emitRecursive at root token #0 ---\n");
        emitRecursive(0, out);
        // if (DEBUG_LOG) System.out.printf("--- Emit finished. Output length: %d ---\n", out.position);
        // if (DEBUG_LOG && depth != 0) throw new IllegalStateException();
        return out.buffer;
    }

    private int emitRecursive(int tokenIndex, final UnsafeFastByteArrayIO out) {
        final TagPointer tp = tokens[tokenIndex];
        // if (DEBUG_LOG) System.out.printf("%s>> Emitting token #%d (start=%d, end=%d, tokenCount=%d)\n", "  ".repeat(depth++), tokenIndex, tp.start, tp.end, tp.tokenCount);

        // Write this tag's header.
        if (tp.isList) {
            int headerStart = tp.start;
            int headerEnd = (tp.nameEnd > 0 ? tp.nameEnd : tp.start) + 5;
            out.write(in.buffer, headerStart, headerEnd - headerStart);
        } else {
            if (tp.nameEnd > 0) out.write(in.buffer, tp.start, tp.nameEnd - tp.start);
        }

        // Write payload for primitives/arrays, or process children for containers.
        if (tp.tokenCount == 1) { // Primitive or list-of-primitives or fully sorted already
            int payloadStart = tp.nameEnd > 0 ? tp.nameEnd : tp.start;
            if (tp.isList) payloadStart = (tp.nameEnd > 0 ? tp.nameEnd : tp.start) + 5;
            if (tp.end > payloadStart) {
                out.write(in.buffer, payloadStart, tp.end - payloadStart);
            }
        } else { // Container with children tokens
            if (!tp.isList) { // COMPOUND
                if (tp.childrenUnordered) {
                    int[] sortingIndexBuffer = new int[tp.childCount];
                    int currentTokenIndex = tokenIndex + 1;
                    for (int i = 0; i < tp.childCount; i++) {
                        sortingIndexBuffer[i] = currentTokenIndex;
                        currentTokenIndex += tokens[currentTokenIndex].tokenCount;
                    }

                    if (tp.childCount < 20) {
                        insertionSort(sortingIndexBuffer, tp.childCount);
                    } else {
                        quicksort(sortingIndexBuffer, 0, tp.childCount - 1);
                    }

                    for (int i = 0; i < tp.childCount; i++) {
                        emitRecursive(sortingIndexBuffer[i], out);
                    }
                } else {
                    int currentIndex = tokenIndex + 1;
                    for (int i = 0; i < tp.childCount; i++) {
                        currentIndex = emitRecursive(currentIndex, out);
                    }
                }
                out.writeByte(END);
            } else { // LIST of Compounds or Lists
                int currentIndex = tokenIndex + 1;
                for (int i = 0; i < tp.childCount; i++) {
                    currentIndex = emitRecursive(currentIndex, out);
                }
            }
        }
        // if (DEBUG_LOG) depth--;
        return tokenIndex + tp.tokenCount;
    }

    //<editor-fold desc="custom sorting implementations">
    private void insertionSort(final int[] arr, final int count) {
        for (int i = 1; i < count; i++) {
            int key = arr[i];
            int j = i - 1;
            while (j >= 0 && compareTokens(arr[j], key) > 0) {
                arr[j + 1] = arr[j];
                j--;
            }
            arr[j + 1] = key;
        }
    }

    private void quicksort(final int[] arr, final int begin, final int end) {
        if (begin < end) {
            int partitionIndex = partition(arr, begin, end);
            quicksort(arr, begin, partitionIndex - 1);
            quicksort(arr, partitionIndex + 1, end);
        }
    }

    private int partition(final int[] arr, final int begin, final int end) {
        int pivot = arr[end];
        int i = (begin - 1);
        for (int j = begin; j < end; j++) {
            if (compareTokens(arr[j], pivot) <= 0) {
                i++;
                int swapTemp = arr[i];
                arr[i] = arr[j];
                arr[j] = swapTemp;
            }
        }
        int swapTemp = arr[i + 1];
        arr[i + 1] = arr[end];
        arr[end] = swapTemp;
        return i + 1;
    }

    private int compareTokens(final int tokenIndex1, final int tokenIndex2) {
        TagPointer t1 = tokens[tokenIndex1];
        TagPointer t2 = tokens[tokenIndex2];
        return Arrays.compare(in.buffer, t1.start + 3, t1.nameEnd, in.buffer, t2.start + 3, t2.nameEnd);
    }
    //</editor-fold>

    public static class TagPointer {
        int start;
        int nameEnd;
        int end;
        int tokenCount;
        int childCount;
        boolean isContentsReordered;
        boolean childrenUnordered;
        boolean isList;

        void init(int start) {
            this.start = start;
            end = 0;
            nameEnd = 0;
            tokenCount = 1;
            childCount = 0;
            isContentsReordered = false;
            childrenUnordered = false;
            isList = false;
        }
    }

    private static class UnsafeFastByteArrayIO {
        protected final byte[] buffer;
        protected final int limit;
        protected int position;

        public UnsafeFastByteArrayIO(final byte[] buffer) {
            this.buffer = buffer;
            this.limit = buffer.length;
            this.position = 0;
        }

        public byte readByte() {
            // if (DEBUG_IO) checkPosition(1);
            return buffer[position++];
        }

        public int readUShort() {
            // if (DEBUG_IO) checkPosition(2);
            return ((0xFF & buffer[position++]) << 8) | (0xFF & buffer[position++]);
        }

        public int readInt() {
            // if (DEBUG_IO) checkPosition(4);
            return ((0xFF & buffer[position++]) << 24) | ((0xFF & buffer[position++]) << 16)
                    | ((0xFF & buffer[position++]) << 8) | (0xFF & buffer[position++]);
        }

        public void skipString() {
            int length = readUShort();
            // if (DEBUG_IO) checkPosition(length);
            position += length;
        }

        public void write(final byte[] source, final int sourcePos, final int length) {
            System.arraycopy(source, sourcePos, this.buffer, this.position, length);
            this.position += length;
        }

        public void writeByte(byte v) {
            // if (DEBUG_IO) checkPosition(1);
            buffer[position++] = v;
        }

        private void checkPosition(final int requiredBytes) {
            if (position + requiredBytes > limit) throw new BufferOverflowException();
        }

        public String debugReadString(final int position) {
            if (position + 2 > limit) throw new BufferOverflowException();
            int len = ((0xFF & buffer[position]) << 8) | (0xFF & buffer[position + 1]);
            if (position + 2 + len > limit) throw new BufferOverflowException();
            return new String(buffer, position + 2, len, StandardCharsets.UTF_8);
        }
    }
}