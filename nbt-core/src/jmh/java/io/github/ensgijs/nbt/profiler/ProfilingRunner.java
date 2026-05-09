package io.github.ensgijs.nbt.profiler;

import io.github.ensgijs.nbt.BenchmarkBase;
import io.github.ensgijs.nbt.io.BinaryNbtHelpers;
import io.github.ensgijs.nbt.io.CompressionType;

import java.util.ArrayList;
import java.util.List;

/**
 * A simple main class to be used for profiling with synthetic inputs.
 */
public class ProfilingRunner extends BenchmarkBase {
    private static final String[] FILENAMES = {
            "1_20_4/region/r.0.0/0299.11.9.snbt",
            "text_nbt_samples/named_item.snbt",
            "text_nbt_samples/named_tag_sample-with_bom.snbt",
            "text_nbt_samples/unnamed_tag_sample.snbt",
            "mca_palettes/block_states-1.20.4-6entries.snbt",
            "text_nbt_samples/little_of_everything.snbt"
    };

    public static void main(String[] args) {
        try {
            List<byte[]> inputs = new ArrayList<>();
            for (String filename : FILENAMES) {
                inputs.add(load(ProfilingRunner.class.getClassLoader(), filename));
            }
            long loops = 0;

            System.out.println("Starting profiling run... This will run for 10 seconds.");
            long endTime = System.currentTimeMillis() + 10_000;

            // Loop for a fixed duration to give the profiler time to collect data
            while (System.currentTimeMillis() < endTime) {
                for (byte[] input : inputs) {
                    BinaryNbtHelpers.deserializeBytes(input, CompressionType.NONE);
                }
                loops ++;
            }
            System.out.println("Profiling run finished. Loops executed: " + loops);
        } catch (Exception ex) {
            System.err.println("Profiling run FAILED");
            ex.printStackTrace();
        }
    }
}