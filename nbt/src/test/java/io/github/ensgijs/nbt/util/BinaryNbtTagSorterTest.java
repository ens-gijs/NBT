package io.github.ensgijs.nbt.util;

import io.github.ensgijs.nbt.NbtTestCase;
import io.github.ensgijs.nbt.io.*;
import io.github.ensgijs.nbt.tag.CompoundTag;

import java.io.IOException;
import java.util.*;
import java.util.concurrent.TimeUnit;

import static org.junit.Assert.*;

public class BinaryNbtTagSorterTest extends NbtTestCase {
    private static final HexFormat HEX_FORMATTER = HexFormat.ofDelimiter(" ").withUpperCase();

    private void validateNbt(String filename) throws IOException {
        CompoundTag tag = (CompoundTag) deserializeFromFile(filename).getTag();
        byte[] controlOrdered = serialize(tag, true);
        byte[] controlUnordered = serialize(tag, false);

//        System.out.println("\nINPUT(TEXT NBT)");
//        System.out.println(TextNbtHelpers.toTextNbt(new NamedTag("", tag), false, false));
//        System.out.println("\nINPUT(HEX TABLE)");
//        System.out.println(HexDump.dumpToHexString(controlUnordered));

//        System.out.println("\nINPUT(HEX DATA)");
//        System.out.println(HEX_FORMATTER.formatHex(controlUnordered));

        byte[] actual = new BinaryNbtTagSorter().sort(controlUnordered);

//        System.out.println("\nEXPECTED(ORDERED HEX DATA)");
//        System.out.println(HEX_FORMATTER.formatHex(controlOrdered));
//        System.out.println(TextNbtHelpers.toTextNbt(new NamedTag("", tag), false, true));

//        System.out.println("\nOUTPUT");
//        System.out.println(HEX_FORMATTER.formatHex(actual));
//        System.out.println(TextNbtHelpers.toTextNbt(BinaryNbtHelpers.deserializeBytes(actual), true, false));

//        CompoundTag tagOut = (CompoundTag) assertThrowsNoException(() -> BinaryNbtHelpers.deserializeBytes(actual, CompressionType.NONE)).getTag();
//        assertEquals(tag, tagOut);
        assertArrayEquals(controlOrdered, actual);
    }

    public void _testBugProbe() throws IOException {
        byte[] nbt = HEX_FORMATTER.parseHex("0A 00 00 08 00 04 74 65 73 74 00 05 76 61 6C 75 65 00");
//        NamedTag parsedTag = BinaryNbtHelpers.deserializeBytes(nbt);
//        System.out.println(TextNbtHelpers.toTextNbt(parsedTag, false, false));
        byte[] sortedNbt = new BinaryNbtTagSorter().sort(nbt);
        assertArrayEquals(nbt, sortedNbt);
    }

    public void testBinNbtSorter() throws IOException {
        validateNbt("text_nbt_samples/named_tag_sample-with_bom.snbt");
        validateNbt("text_nbt_samples/unnamed_tag_sample.snbt");
        validateNbt("text_nbt_samples/named_item.snbt");
        validateNbt("mca_palettes/block_states-1.20.4-6entries.snbt");
        validateNbt("text_nbt_samples/little_of_everything.snbt");
        validateNbt("1_20_4/region/r.0.0/0299.11.9.snbt");

        validateNbt("1_20_4/region/r.0.0/0299.11.9_sorted.snbt");
        validateNbt("text_nbt_samples/named_item_sorted.snbt");
        validateNbt("text_nbt_samples/unnamed_tag_sample_sorted.snbt");
        validateNbt("mca_palettes/block_states-1.20.4-6entries_sorted.snbt");
        validateNbt("text_nbt_samples/little_of_everything_sorted.snbt");
        validateNbt("text_nbt_samples/basic-nested-compounds-and-lists-with-root-tag-name_sorted.snbt");
    }

    public void _testPerformance() throws IOException {
        List<PerfTest> tests = new ArrayList<>();
        tests.add(new PerfTest("1_20_4/region/r.0.0/0299.11.9.snbt"));
        tests.add(new PerfTest("text_nbt_samples/named_item.snbt"));
        tests.add(new PerfTest("text_nbt_samples/named_tag_sample-with_bom.snbt"));
        tests.add(new PerfTest("text_nbt_samples/unnamed_tag_sample.snbt"));
        tests.add(new PerfTest("mca_palettes/block_states-1.20.4-6entries.snbt"));
        tests.add(new PerfTest("text_nbt_samples/little_of_everything.snbt"));

        // touch code paths so JIT loading doesn't mess with timing and JIT compilation can optimize
        for (int j = 0; j < 10000; j++) {
            for (var t : tests) {
                t.execCandidate();
                t.execControl();
            }
        }
        for (var t : tests) {
            t.reset();
        }
        for (int j = 0; j < 10000; j++) {
            for (var t : tests) {
                for (int i = 0; i < 10; i++) {
                    t.execCandidate();
                    t.execControl();
                }
            }
        }

        for (var t : tests) {
            System.out.println(t);
        }
    }

//    /**
//     * Instrument BinaryNbtTagSorter's code to time a section with stopwatch, use this test to
//     * evaluate the time taken by the targeted block and to measure impact of changes.
//     */
//    public void testPerformance_evaluateMicroImprovements() throws IOException {
//        List<PerfTest> tests = new ArrayList<>();
//        tests.add(new PerfTest("1_20_4/region/r.0.0/0299.11.9.snbt"));
//        tests.add(new PerfTest("text_nbt_samples/named_item.snbt"));
//        tests.add(new PerfTest("text_nbt_samples/named_tag_sample-with_bom.snbt"));
//        tests.add(new PerfTest("text_nbt_samples/unnamed_tag_sample.snbt"));
//        tests.add(new PerfTest("mca_palettes/block_states-1.20.4-6entries.snbt"));
//        tests.add(new PerfTest("text_nbt_samples/little_of_everything.snbt"));
//
//        // touch code paths so JIT loading doesn't mess with timing
//        for (var t : tests) {
//            t.execCandidate();
//        }
//        BinaryNbtTagSorter.stopwatch.reset();
//
//        Stopwatch stopwatch = Stopwatch.createStarted();
//        for (int j = 0; j < 100000; j++) {
//            for (var t : tests) {
//                t.execCandidate();
//            }
//        }
//        stopwatch.stop();
//
//        System.out.println("BinaryNbtTagSorter STOPWATCH: "
//                + BinaryNbtTagSorter.stopwatch.toString(4, true)
//                + " of " + stopwatch
//                + " (" + String.format("%.3f%%", 100 * (BinaryNbtTagSorter.stopwatch.elapsed(TimeUnit.NANOSECONDS) / (double) stopwatch.elapsed(TimeUnit.NANOSECONDS))) + ")"
//        );
//    }

    private class PerfTest {
        final String filename;
        final byte[] nbt;
        final Stopwatch candidateStopwatch = Stopwatch.createUnstarted();
        final Stopwatch controlStopwatch = Stopwatch.createUnstarted();
        final List<Long> candidateLapTimes = new ArrayList<>(100000);
        final List<Long> controlLapTimes = new ArrayList<>(100000);

        public void reset() {
            candidateStopwatch.reset();
            controlStopwatch.reset();
            candidateLapTimes.clear();
            controlLapTimes.clear();
        }

        PerfTest(String filename) {
            this.filename = filename;
            CompoundTag tag = (CompoundTag) deserializeFromFile(filename).getTag();
            nbt = serialize(tag, false);
        }

        void execCandidate() throws IOException {
            var lap = candidateStopwatch.startLap();
            new BinaryNbtTagSorter().sort(nbt);
            lap.close();
            candidateLapTimes.add(lap.elapsedNanos());
        }

        void execControl() throws IOException {
            var lap = controlStopwatch.startLap();
//            new BinaryNbtTagSorterV3d().sort(nbt);
            NamedTag tag = BinaryNbtHelpers.deserializeBytes(nbt, CompressionType.NONE);
            serialize(tag.getTag(), true);
            lap.close();
            controlLapTimes.add(lap.elapsedNanos());
        }

        @Override
        public String toString() {
            final double candidateMean = candidateStopwatch.elapsed(TimeUnit.NANOSECONDS) / (double) candidateLapTimes.size();
            final double controlMean = controlStopwatch.elapsed(TimeUnit.NANOSECONDS) / (double) controlLapTimes.size();
            StringBuilder sb = new StringBuilder();
            sb.append('[').append(filename).append("]\n");

            sb.append("  Candidate Time.: ").append(candidateStopwatch).append('\n');
            candidateLapTimes.sort(Comparator.comparingLong(Long::valueOf));
            long low1 = candidateLapTimes.get(0);
            long low1count0 = candidateLapTimes.stream().filter(v -> v == low1).count();
            long low1count1 = candidateLapTimes.stream().filter(v -> v <= low1 + 100).count();
            long low1count2 = candidateLapTimes.stream().filter(v -> v <= low1 + 200).count();
            long p50_1 = candidateLapTimes.get(candidateLapTimes.size() / 2);
            long p90_1 = candidateLapTimes.get((int) ((candidateLapTimes.size() - 1) * 0.90));
            long p95_1 = candidateLapTimes.get((int) ((candidateLapTimes.size() - 1) * 0.95));
            long p99_1 = candidateLapTimes.get((int) ((candidateLapTimes.size() - 1) * 0.99));
            long p999_1 = candidateLapTimes.get((int) ((candidateLapTimes.size() - 1) * 0.999));
            long p9999_1 = candidateLapTimes.get((int) ((candidateLapTimes.size() - 1) * 0.9999));
            long high_d2_1 = candidateLapTimes.get(candidateLapTimes.size() - 3);
            long high_d1_1 = candidateLapTimes.get(candidateLapTimes.size() - 2);
            long high1 = candidateLapTimes.get(candidateLapTimes.size() - 1);

            double p99_1_index = (int) ((candidateLapTimes.size() - 1) * 0.99);
            final double candidateMeanP99 = candidateLapTimes.stream().mapToLong(Long::valueOf).filter(v -> v <= p99_1).sum() / p99_1_index;
            double p90_1_index = (int) ((candidateLapTimes.size() - 1) * 0.99);
            final double candidateMeanP90 = candidateLapTimes.stream().mapToLong(Long::valueOf).filter(v -> v <= p90_1).sum() / p90_1_index;
//            sb.append("    std-dev.: ").append(calculateStandardDeviation(candidateLapTimes, candidateMean)).append('\n');
            sb.append("    mean....: ").append(candidateMean).append('\n');
            sb.append("    mean p99: ").append(candidateMeanP99).append('\n');
            sb.append("    mean p90: ").append(candidateMeanP90).append('\n');
            sb.append("    low.....: ").append(low1)
                    .append(" (")
                    .append(low1count0)
                    .append(", ")
                    .append(low1count1)
                    .append(", ")
                    .append(low1count2)
                    .append(")\n");
            sb.append("    p50.....: ").append(p50_1).append('\n');
            sb.append("    p90.....: ").append(p90_1).append('\n');
            sb.append("    p95.....: ").append(p95_1).append('\n');
            sb.append("    p99.....: ").append(p99_1).append('\n');
            sb.append("    p999....: ").append(p999_1).append('\n');
            sb.append("    p9999...: ").append(p9999_1).append('\n');
            sb.append("    high-2..: ").append(high_d2_1).append('\n');
            sb.append("    high-1..: ").append(high_d1_1).append('\n');
            sb.append("    high....: ").append(high1).append('\n');

            sb.append("  Control Time...: ").append(controlStopwatch).append('\n');
            controlLapTimes.sort(Comparator.comparingLong(Long::valueOf));
            long low2 = controlLapTimes.get(0);
            long low2count0 = controlLapTimes.stream().filter(v -> v == low2).count();
            long low2count1 = controlLapTimes.stream().filter(v -> v <= low2 + 100).count();
            long low2count2 = controlLapTimes.stream().filter(v -> v <= low2 + 200).count();
            long p50_2 = controlLapTimes.get(controlLapTimes.size() / 2);
            long p90_2 = controlLapTimes.get((int) ((controlLapTimes.size() - 1) * 0.90));
            long p95_2 = controlLapTimes.get((int) ((controlLapTimes.size() - 1) * 0.95));
            long p99_2 = controlLapTimes.get((int) ((controlLapTimes.size() - 1) * 0.99));
            long p999_2 = controlLapTimes.get((int) ((controlLapTimes.size() - 1) * 0.999));
            long p9999_2 = controlLapTimes.get((int) ((controlLapTimes.size() - 1) * 0.9999));
            long high_d2_2 = controlLapTimes.get(controlLapTimes.size() - 3);
            long high_d1_2 = controlLapTimes.get(controlLapTimes.size() - 2);
            long high2 = controlLapTimes.get(controlLapTimes.size() - 1);
            double p99_2_index = (int) ((controlLapTimes.size() - 1) * 0.99);
            final double controlMeanP99 = controlLapTimes.stream().mapToLong(Long::valueOf).filter(v -> v <= p99_2).sum() / p99_2_index;
            double p90_2_index = (int) ((controlLapTimes.size() - 1) * 0.99);
            final double controlMeanP90 = controlLapTimes.stream().mapToLong(Long::valueOf).filter(v -> v <= p90_2).sum() / p90_2_index;
//            sb.append("    std-dev.: ").append(calculateStandardDeviation(controlLapTimes, controlMean)).append('\n');
            sb.append("    mean....: ").append(controlMean).append('\n');
            sb.append("    mean p99: ").append(controlMeanP99).append('\n');
            sb.append("    mean p90: ").append(controlMeanP90).append('\n');
            sb.append("    low.....: ").append(low2)
                    .append(" (")
                    .append(low2count0)
                    .append(", ")
                    .append(low2count1)
                    .append(", ")
                    .append(low2count2)
                    .append(")\n");
            sb.append("    p50.....: ").append(p50_2).append('\n');
            sb.append("    p90.....: ").append(p90_2).append('\n');
            sb.append("    p95.....: ").append(p95_2).append('\n');
            sb.append("    p99.....: ").append(p99_2).append('\n');
            sb.append("    p999....: ").append(p999_2).append('\n');
            sb.append("    p9999...: ").append(p9999_2).append('\n');
            sb.append("    high-2..: ").append(high_d2_2).append('\n');
            sb.append("    high-1..: ").append(high_d1_2).append('\n');
            sb.append("    high....: ").append(high2).append('\n');

            sb.append("  Speedup: ").append('\n');
            sb.append("    mean....: ").append(formatPercentDifference(controlMean, candidateMean)).append('\n');
            sb.append("    mean p99: ").append(formatPercentDifference(controlMeanP99, candidateMeanP99)).append('\n');
            sb.append("    mean p90: ").append(formatPercentDifference(controlMeanP90, candidateMeanP90)).append('\n');
            sb.append("    low.....: ").append(formatPercentDifference(low2, low1)).append('\n');
            long uh = candidateLapTimes.stream().filter(v -> v <= p50_2).count();
            // this is the number of binSortLapTimes with times under fullCycleLapTimes' p50 value
            sb.append("    u(p50): u").append(p50_2).append(" = ").append(uh)
                    .append(String.format(" (%.3f%%)", 100 * uh / (double) controlLapTimes.size())).append('\n');

            return sb.toString();
        }
    }

    private static String formatPercentDifference(double a, double b) {
        double p = 100 * (Math.abs(a - b) / ((a + b) / 2));
        return (a > b ? "+" : "-") + String.format("%.3f%% (%.3fx)", p, a / b);
    }

    private static double calculateStandardDeviation(List<Long> data, double mean) {
        double sumOfSquaredDifferences = 0;
        for (long num : data) {
            sumOfSquaredDifferences += Math.pow(num - mean, 2);
        }
        // For population standard deviation, divide by data.length
        // For sample standard deviation, divide by (data.length - 1)
        return Math.sqrt(sumOfSquaredDifferences / data.size());
    }
}
