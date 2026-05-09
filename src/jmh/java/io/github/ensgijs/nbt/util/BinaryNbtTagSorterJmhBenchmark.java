package io.github.ensgijs.nbt.util;

import io.github.ensgijs.nbt.BenchmarkBase;
import io.github.ensgijs.nbt.io.BinaryNbtHelpers;
import io.github.ensgijs.nbt.io.CompressionType;
import io.github.ensgijs.nbt.io.NamedTag;
import org.openjdk.jmh.annotations.*;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

@Fork(value = 1, jvmArgsAppend = {"-Xms2G", "-Xmx2G"})
@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.NANOSECONDS)
@State(Scope.Benchmark)
@Warmup(time = 300, timeUnit = TimeUnit.MILLISECONDS)
@Measurement(time = 300, timeUnit = TimeUnit.MILLISECONDS)
public class BinaryNbtTagSorterJmhBenchmark extends BenchmarkBase {
    @Param({
            "1_20_4/region/r.0.0/0299.11.9.snbt",
            "text_nbt_samples/named_item.snbt",
//            "text_nbt_samples/named_tag_sample-with_bom.snbt",
            "text_nbt_samples/unnamed_tag_sample.snbt",
            "mca_palettes/block_states-1.20.4-6entries.snbt",
            "text_nbt_samples/little_of_everything.snbt",
            "text_nbt_samples/basic-nested-compounds-and-lists-with-root-tag-name.snbt",

            "1_20_4/region/r.0.0/0299.11.9_sorted.snbt",
            "text_nbt_samples/named_item_sorted.snbt",
//            "text_nbt_samples/named_tag_sample-with_bom_sorted.snbt",
            "text_nbt_samples/unnamed_tag_sample_sorted.snbt",
            "mca_palettes/block_states-1.20.4-6entries_sorted.snbt",
            "text_nbt_samples/little_of_everything_sorted.snbt",
            "text_nbt_samples/basic-nested-compounds-and-lists-with-root-tag-name_sorted.snbt",
    })
    public String filename;

    private byte[] nbt;
    private final BinaryNbtTagSorter sorter = new BinaryNbtTagSorter();

    @Setup
    public void setup() throws IOException {
        this.nbt = load(filename);
    }

    @Benchmark
    public byte[] candidate() throws IOException {
        return sorter.sort(nbt);
    }

//    @Benchmark
//    public byte[] control() throws IOException {
//        NamedTag tag = BinaryNbtHelpers.deserializeBytes(nbt, CompressionType.NONE);
//        return BinaryNbtHelpers.serializeAsBytes(tag, CompressionType.NONE, true);
//    }
}