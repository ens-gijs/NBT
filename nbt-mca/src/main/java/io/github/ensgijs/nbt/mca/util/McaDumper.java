package io.github.ensgijs.nbt.mca.util;

import io.github.ensgijs.nbt.io.TextNbtHelpers;
import io.github.ensgijs.nbt.mca.ChunkBase;
import io.github.ensgijs.nbt.mca.io.LoadFlags;
import io.github.ensgijs.nbt.mca.io.McaFileChunkIterator;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;

public class McaDumper {
    private McaDumper() {}

    /**
     * @param mcaFile MCA file to dump the chunk data for.
     * @param outputRoot Root directory to dump into. The output will be written to
     *                   "&lt;mca type&gt;/rX.Z/&lt;chunk index&gt;.X.Z.snbt;"
     *                   Example: "region/r.0.0/0042.168.-475.snbt"
     * @return The path containing the dumped chunk text nbt (.snbt) files.
     * @throws IOException
     */
    public static Path dumpChunksAsTextNbt(File mcaFile, Path outputRoot) throws IOException {
        try (McaFileChunkIterator<?> iter = McaFileChunkIterator.iterate(mcaFile, LoadFlags.RAW)) {
            File dir = Paths.get(
                    outputRoot.toString(),
                    mcaFile.getAbsoluteFile().getParentFile().getName(),
                    iter.regionXZ().toString("r.%d.%d")).toFile();
            if (!dir.exists()) {
                dir.mkdirs();
            }
            final String dirName = dir.getPath();
            while (iter.hasNext()) {
                ChunkBase chunk = iter.next();
                TextNbtHelpers.writeTextNbtFile(
                        Paths.get(
                                dirName,
                                String.format("%04d.%d.%d.snbt",
                                        chunk.getIndex(),
                                        chunk.getChunkX(),
                                        chunk.getChunkZ())
                        ), chunk.getHandle(), /*pretty print*/ true, /*sorted*/ true);

            }
            return dir.toPath();
        }
    }

    /** Writes the given chunk to the specified destinationFile. */
    public static Path dumpChunkAsTextNbtToFile(ChunkBase chunk, File destinationFile) throws IOException {
        Path path = destinationFile.toPath();
        TextNbtHelpers.writeTextNbtFile(path, chunk.getHandle(), /*pretty print*/ true, /*sorted*/ true);
        return path;
    }

    /**
     * Writes the given chunk to a file within the given outputRoot.
     * @param chunk Chunk to dump.
     * @param outputRoot Root directory to dump into. The output will be written to
     *                   "&lt;mca type&gt;/r.X.Z/&lt;chunk index&gt;.X.Z.snbt;"
     *                   Example: "region/r0.0/0042.168.-475.snbt"
     * @return The full path of the output snbt file.
     * @throws IOException
     */
    public static Path dumpChunkAsTextNbtAutoFilename(ChunkBase chunk, Path outputRoot) throws IOException {
        IntPointXZ regionXZ = chunk.getRegionXZ();
        File dir = Paths.get(
                outputRoot.toString(),
                chunk.getMcaType(),
                "r." + regionXZ.getX() + "." + regionXZ.getZ()).toFile();
        if (!dir.exists()) {
            dir.mkdirs();
        }
        Path outFilePath = Paths.get(
                dir.getPath(),
                String.format("%04d.%d.%d.snbt",
                        chunk.getIndex(),
                        chunk.getChunkX(),
                        chunk.getChunkZ()));
        TextNbtHelpers.writeTextNbtFile(outFilePath, chunk.getHandle(), /*pretty print*/ true, /*sorted*/ true);
        return outFilePath;
    }
}
