// Base code copied from com.google.common.base.Stopwatch (Apache V2 licenced source)
package io.github.ensgijs.nbt.util;

import java.io.Closeable;
import java.text.DecimalFormat;
import java.time.Duration;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

import static java.util.concurrent.TimeUnit.*;

public final class Stopwatch {
    private final Supplier<Long> ticker = System::nanoTime;
    private boolean isRunning;
    private long elapsedNanos;
    private long startTick;
    /** times {@link #start()} has been called */
    private long laps;

    public static Stopwatch createUnstarted() {
        return new Stopwatch();
    }

    public static Stopwatch createStarted() {
        return new Stopwatch().start();
    }

    public class LapToken implements Closeable {
        final long startNanos;
        long stopNanos;

        LapToken() {
            startNanos = elapsedNanos;
            start();
        }

        @Override
        public void close() {
            stop();
            stopNanos = elapsedNanos;
        }

        public long startNanos() {
            return startNanos;
        }

        public long stopNanos() {
            return stopNanos;
        }

        public long elapsedNanos() {
            return stopNanos - startNanos;
        }

        /**
         * Returns the current elapsed time shown on this stopwatch, expressed in the desired time unit,
         * with any fraction rounded down.
         *
         * <p><b>Note:</b> the overhead of measurement can be more than a microsecond, so it is generally
         * not useful to specify {@link TimeUnit#NANOSECONDS} precision here.
         *
         * <p>It is generally not a good idea to use an ambiguous, unitless {@code long} to represent
         * elapsed time. Therefore, we recommend using {@link #elapsed()} instead, which returns a
         * strongly-typed {@code Duration} instance.
         *
         * @since 14.0 (since 10.0 as {@code elapsedTime()})
         */
        public long elapsed(TimeUnit desiredUnit) {
            return desiredUnit.convert(elapsedNanos(), NANOSECONDS);
        }

        public String toString() {
            long nanos = elapsedNanos();
            TimeUnit unit = chooseUnit(nanos);
            double value = (double) nanos / NANOSECONDS.convert(1, unit);
            return String.format("%.4g", value) + " " + abbreviate(unit);
        }
    }

    Stopwatch() {}

    /**
     * @return A new Stopwatch instance with an elapsed time equal to the sum of this
     * and all others elapsed time.
     */
    public Stopwatch add(Stopwatch... others) {
        Stopwatch out = new Stopwatch();
        out.elapsedNanos = this.elapsedNanos();
        for (Stopwatch s : others) {
            out.elapsedNanos += s.elapsedNanos();
        }
        return out;
    }

    /**
     * @return A new Stopwatch instance with an elapsed time equal to the product of this
     * instances elapsed time minus all others elapsed time. The result may be negative.
     */
    public Stopwatch subtract(Stopwatch... others) {
        Stopwatch out = new Stopwatch();
        out.elapsedNanos = this.elapsedNanos();
        for (Stopwatch s : others) {
            out.elapsedNanos -= s.elapsedNanos();
        }
        return out;
    }

    /**
     * Returns {@code true} if {@link #start()} has been called on this stopwatch, and {@link #stop()}
     * has not been called since the last call to {@code start()}.
     */
    public boolean isRunning() {
        return isRunning;
    }

    /**
     * Starts the stopwatch.
     *
     * @return this {@code Stopwatch} instance
     * @throws IllegalStateException if the stopwatch is already running.
     */
    public Stopwatch start() {
        ArgValidator.checkState(!isRunning, "This stopwatch is already running.");
        isRunning = true;
        laps++;
        startTick = ticker.get();
        return this;
    }

    /**
     * Use inside {@code try (Stopwatch.LapToken lap1 = totalWriteStopwatch.startLap()){..}} blocks for an auto-closing stopwatch timer.
     */
    public LapToken startLap() {
        return new LapToken();
    }

    /**
     * Number of times {@link #start()} has been called.
     */
    public long laps() {
        return laps;
    }

    /**
     * Stops the stopwatch. Future reads will return the fixed duration that had elapsed up to this
     * point.
     *
     * @return this {@code Stopwatch} instance
     * @throws IllegalStateException if the stopwatch is already stopped.
     */
    public Stopwatch stop() {
        long tick = ticker.get();
        ArgValidator.checkState(isRunning, "This stopwatch is already stopped.");
        isRunning = false;
        elapsedNanos += tick - startTick;
        return this;
    }

    /**
     * Sets the elapsed time for this stopwatch to zero, and places it in a stopped state.
     *
     * @return this {@code Stopwatch} instance
     */
    public Stopwatch reset() {
        elapsedNanos = 0;
        laps = 0;
        isRunning = false;
        return this;
    }

    private long elapsedNanos() {
        return isRunning ? ticker.get() - startTick + elapsedNanos : elapsedNanos;
    }

    /**
     * Returns the current elapsed time shown on this stopwatch, expressed in the desired time unit,
     * with any fraction rounded down.
     *
     * <p><b>Note:</b> the overhead of measurement can be more than a microsecond, so it is generally
     * not useful to specify {@link TimeUnit#NANOSECONDS} precision here.
     *
     * <p>It is generally not a good idea to use an ambiguous, unitless {@code long} to represent
     * elapsed time. Therefore, we recommend using {@link #elapsed()} instead, which returns a
     * strongly-typed {@code Duration} instance.
     *
     * @since 14.0 (since 10.0 as {@code elapsedTime()})
     */
    public long elapsed(TimeUnit desiredUnit) {
        return desiredUnit.convert(elapsedNanos(), NANOSECONDS);
    }

    /**
     * Returns the current elapsed time shown on this stopwatch as a {@link Duration}. Unlike {@link
     * #elapsed(TimeUnit)}, this method does not lose any precision due to rounding.
     */
    public Duration elapsed() {
        return Duration.ofNanos(elapsedNanos());
    }

    /** Returns a string representation of the current elapsed time. */
    @Override
    public String toString() {
        long nanos = elapsedNanos();
        TimeUnit unit = chooseUnit(nanos);
        double value = (double) nanos / NANOSECONDS.convert(1, unit);
        return String.format("%.4g", value) + " " + abbreviate(unit);
    }

    private DecimalFormat thousandsFormat = null;

    /**
     * @param precision number of digits, default=4
     * @param showLapCount when true " (#,### laps)" will be included in the returned value.
     * @return String representation of the current elapsed time.
     */
    public String toString(int precision, boolean showLapCount) {
        long nanos = elapsedNanos();
        TimeUnit unit = chooseUnit(nanos);
        double value = (double) nanos / NANOSECONDS.convert(1, unit);
        String s = String.format("%." + precision + "g", value) + " " + abbreviate(unit);
        if (showLapCount) {
            if (thousandsFormat != null) {
                s += " (" + thousandsFormat.format(laps) + " laps)";
            } else {
                thousandsFormat = new DecimalFormat("#,###");
                s += " (" + thousandsFormat.format(laps) + " laps)";
            }
        }
        return s;
    }

    private static TimeUnit chooseUnit(long nanos) {
        if (nanos < 0) nanos = -nanos;
        if (DAYS.convert(nanos, NANOSECONDS) > 0) {
            return DAYS;
        }
        if (HOURS.convert(nanos, NANOSECONDS) > 0) {
            return HOURS;
        }
        if (MINUTES.convert(nanos, NANOSECONDS) > 0) {
            return MINUTES;
        }
        if (SECONDS.convert(nanos, NANOSECONDS) > 0) {
            return SECONDS;
        }
        if (MILLISECONDS.convert(nanos, NANOSECONDS) > 0) {
            return MILLISECONDS;
        }
        if (MICROSECONDS.convert(nanos, NANOSECONDS) > 0) {
            return MICROSECONDS;
        }
        return NANOSECONDS;
    }

    private static String abbreviate(TimeUnit unit) {
        return switch (unit) {
            case NANOSECONDS -> "ns";
            case MICROSECONDS -> "us";  //  "\u03bcs"; // μs
            case MILLISECONDS -> "ms";
            case SECONDS -> "s";
            case MINUTES -> "min";
            case HOURS -> "h";
            case DAYS -> "d";
        };
    }
}