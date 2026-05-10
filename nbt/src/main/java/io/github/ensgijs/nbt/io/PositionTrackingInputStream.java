package io.github.ensgijs.nbt.io;

import java.io.EOFException;
import java.io.IOException;
import java.io.InputStream;

/**
 * Wraps an InputStream to track the read position and to allow you to skip ahead to a known position.
 * Note: position 0 isn't necessarily the first byte in the stream - it's the first byte will be read
 * after this object is created.
 */
public class PositionTrackingInputStream extends InputStream {
    private long pos = 0;
    private long markedPos = -1;
    private final InputStream stream;
    private long softEof = 0;

    public PositionTrackingInputStream(InputStream in) {
        stream = in;
    }

    public long pos() {
        return pos;
    }

    /**
     * Soft EOF will prevent byte[] reads that cross this position. Such reads won't fail, they'll simply only be
     * filled up to softEof - 1. Set to LE0 to disable completely (default).
     */
    public void setSoftEof(long softEof) {
        this.softEof = softEof;
    }

    @Override
    public int read() throws IOException {
        int ret = stream.read();
        if (ret >= 0) pos++;
        return ret;
    }

    @Override
    public int read(byte[] b) throws IOException {
        int len = b.length;
        if (pos < softEof && (pos + len) > softEof) {
            len = (int) (softEof - pos);
        }
        int ret = stream.read(b, 0, len);
        if (ret > 0) pos += ret;
        return ret;
    }

    @Override
    public int read(byte[] b, int off, int len) throws IOException {
        if (pos < softEof && (pos + len) >= softEof) {
            len = (int) (softEof - pos);
        }
        int ret = stream.read(b, off, len);
        if (ret > 0) pos += ret;
        return ret;
    }

    @Override
    public long skip(long n) throws IOException {
        long ret = stream.skip(n);
        if (ret > 0) pos += ret;
        return ret;
    }

    @Override
    public int available() throws IOException {
        return stream.available();
    }

    @Override
    public void close() throws IOException {
        stream.close();
    }

    /**
     * Sets soft EOF automatically (pos() + readLimit).
     * @see #setSoftEof(long)
     */
    @Override
    public synchronized void mark(int readlimit) {
        if (super.markSupported()) {
            markedPos = pos;
        }
        stream.mark(readlimit);
        setSoftEof(pos + readlimit);
    }

    @Override
    public synchronized void reset() throws IOException {
        stream.reset();
        if (markedPos < 0) throw new IOException("mark position is unknown!");
        pos = markedPos;
    }

    @Override
    public boolean markSupported() {
        return stream.markSupported();
    }

    public void skipTo(long pos) throws IOException {
        if (pos < this.pos)
            throw new IOException(
                    "cannot skip backwards from 0x" + Long.toString(this.pos, 16) + " (" + this.pos +
                            ") to 0x" + Long.toString(pos, 16)+ " (" + pos + ")");
        final long originalPos = this.pos;
        while (this.pos < pos) {
            if (skip(pos - this.pos) <= 0) {
                throw new EOFException(String.format(
                        "Asked to skip from %,d to %,d (%,d bytes) but only skipped %,d bytes; new pos = %,d; soft EOF = %,d",
                        originalPos, pos, pos - originalPos, this.pos - originalPos, this.pos, softEof));
            }
        }
        if (pos != this.pos)
            throw new IllegalStateException();
    }
}
