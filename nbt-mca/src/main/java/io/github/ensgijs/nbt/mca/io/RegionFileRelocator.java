package io.github.ensgijs.nbt.mca.io;

import io.github.ensgijs.nbt.mca.ChunkBase;
import io.github.ensgijs.nbt.mca.util.IntPointXZ;
import io.github.ensgijs.nbt.util.Stopwatch;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;
import java.util.function.Predicate;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

// TODO: add class docs including basic usage
public class RegionFileRelocator implements Closeable {
    private McaStreamSupplier mcaStreamSupplier;
    private String destinationRoot;
    private long moveChunkFlags = MoveChunkFlags.MOVE_CHUNK_DEFAULT_FLAGS;
    private final Stopwatch stopwatch = Stopwatch.createUnstarted();
    private int regionFilesRelocated = 0;
    private int poiFilesRelocated = 0;
    private int entitiesFilesRelocated = 0;

    @Override
    public String toString() {
        return String.format("relocations[region %d; entities %d; poi %d]; total time %s",
                regionFilesRelocated, entitiesFilesRelocated, poiFilesRelocated, stopwatch);
    }

    private void check() {
        if (mcaStreamSupplier == null) {
            throw new IllegalStateException("Must set a sourceRoot first!");
        }
        if (destinationRoot == null) {
            throw new IllegalStateException("Must set a destinationRoot first!");
        }
    }

    /**
     * @param sourceRoot A path which contains region/poi/entities folders as immediate children.
     *                   This value may include .zip or .jar files as part of the path.
     */
    public RegionFileRelocator sourceRoot(String sourceRoot) throws IOException {
        SourcePathParser spp = new SourcePathParser(sourceRoot);

        mcaStreamSupplier = spp.archiveName != null ?
                new ArchiveMcaStreamSupplier(spp.archiveName, spp.path) :
                new FileMcaStreamSupplier(spp.path);
        return this;
    }

    /**
     * @param destinationRoot A path which contains region/poi/entities folders that relocated regions will be written to.
     *                        Note: this path does not support writing into .zip or .jar files.
     */
    public RegionFileRelocator destinationRoot(String destinationRoot) {
        this.destinationRoot = destinationRoot;
        return this;
    }

    public long getMoveChunkFlags() {
        return moveChunkFlags;
    }

    public RegionFileRelocator setMoveChunkFlags(long moveChunkFlags) {
        this.moveChunkFlags = moveChunkFlags;
        return this;
    }

    public RegionFileRelocator addMoveChunkFlags(long moveChunkFlags) {
        this.moveChunkFlags |= moveChunkFlags;
        return this;
    }

    public RegionFileRelocator removeMoveChunkFlags(long moveChunkFlags) {
        this.moveChunkFlags &= ~moveChunkFlags;
        return this;
    }

    /**
     * Resets the time elapsed stopwatch and resets all relocation counters.
     */
    public RegionFileRelocator resetPerformanceMetrics() {
        stopwatch.reset();
        regionFilesRelocated = 0;
        poiFilesRelocated = 0;
        entitiesFilesRelocated = 0;
        return this;
    }

    /** Gets a copy of the stopwatch populated with the current total relocate elapsed time. */
    public Stopwatch elapsed() {
        return Stopwatch.createUnstarted().add(stopwatch);
    }

    public int regionFilesRelocated() {
        return regionFilesRelocated;
    }

    public int poiFilesRelocated() {
        return poiFilesRelocated;
    }

    public int entitiesFilesRelocated() {
        return entitiesFilesRelocated;
    }

    /**
     * @param source A file name such as r.0.0.mca which will be used to read region/poi/entities from {@link #sourceRoot(String)}.
     * @param destination A file name such as r.1.1.mca which will be used to write region/poi/entities into {@link #destinationRoot(String)}.
     * @return True if any mca files were written, false otherwise.
     */
    public boolean relocate(String source, String destination) throws IOException {
        check();
        try (Stopwatch.LapToken lap = stopwatch.startLap()) {
            boolean didSomething = false;
            if (relocate("region", source, destination)) {
                didSomething = true;
                regionFilesRelocated++;
            }
            if (relocate("entities", source, destination)) {
                didSomething = true;
                entitiesFilesRelocated++;
            }
            if (relocate("poi", source, destination)) {
                didSomething = true;
                poiFilesRelocated++;
            }
            return didSomething;
        }
    }

    public boolean relocate(int sourceX, int sourceZ, int destinationX, int destinationZ) throws IOException {
        return relocate(
                McaFileHelpers.createNameFromRegionLocation(sourceX, sourceZ),
                McaFileHelpers.createNameFromRegionLocation(destinationX, destinationZ));
    }

    public boolean relocate(IntPointXZ sourceXZ, IntPointXZ destinationXZ) throws IOException {
        return relocate(
                McaFileHelpers.createNameFromRegionLocation(sourceXZ),
                McaFileHelpers.createNameFromRegionLocation(destinationXZ));
    }

    private boolean relocate(String mcaType, String source, String destination) throws IOException {
        Stopwatch totalStopwatch = Stopwatch.createStarted();
        Stopwatch supplierGetStopwatch = Stopwatch.createStarted();
        try (InputStream in = mcaStreamSupplier.get(mcaType, source)) {
            supplierGetStopwatch.stop();
            if (in != null) {
                Stopwatch iterNextStopwatch = Stopwatch.createUnstarted();
                Stopwatch moveChunkStopwatch = Stopwatch.createUnstarted();
                File destFolder = Paths.get(destinationRoot, mcaType).toFile();
                if (!destFolder.exists()) destFolder.mkdirs();
                McaFileChunkIterator<?> iter = McaFileChunkIterator.iterate(in, mcaType + "/" + source, LoadFlags.RAW);
                try (McaFileStreamingWriter writer = new McaFileStreamingWriter(Paths.get(destinationRoot, mcaType, destination))) {
                    final IntPointXZ sourceAnchorXZ = iter.chunkAbsXzOffset();
                    final IntPointXZ destAnchorXZ = McaFileHelpers.regionXZFromFileName(destination).transformRegionToChunk();
                    final IntPointXZ deltaXZ = destAnchorXZ.subtract(sourceAnchorXZ);

                    while (iter.hasNext()) {
                        iterNextStopwatch.start();
                        ChunkBase chunk = iter.next();
                        iterNextStopwatch.stop();
                        if (!deltaXZ.isZero()) {
                            moveChunkStopwatch.start();
                            chunk.moveChunk(chunk.getChunkX() + deltaXZ.getX(), chunk.getChunkZ() + deltaXZ.getZ(), moveChunkFlags);
                            chunk.updateHandle();   // not necessary when loaded in RAW, but also a very low cost call when in raw so leave it in to avoid bugs when not loading raw.
                            moveChunkStopwatch.stop();
                        }
                        writer.write(chunk);
                    }
                    writer.close();
                    totalStopwatch.stop();
//                    System.out.println(
//                            "RELOCATE " + writer + "; relocate[total " + totalStopwatch +
//                                    "; stream supplier " + supplierGetStopwatch +
//                                    "; iter.next " + iterNextStopwatch + "; moveChunk " + moveChunkStopwatch +
//                                    "]; " + mcaType + "/" + source + " -> " + destination );
                }
                return true;
            }
            return false;
        } catch (IOException ex) {
            throw new IOException("Error while relocating " + mcaType + "/" + source, ex);
        }
    }

    public List<String> listSourceRegions() throws IOException {
        check();
        return mcaStreamSupplier.list();
    }

    @Override
    public void close() throws IOException {
        if (mcaStreamSupplier != null)
            mcaStreamSupplier.close();
    }

    public int relocateAll(int deltaXRegions, int deltaZRegions) throws IOException {
        final IntPointXZ deltaXZ = new IntPointXZ(deltaXRegions, deltaZRegions);
        int relocated = 0;
        for (String source : listSourceRegions()) {
//            System.out.println("RELOCATING " + source);
            IntPointXZ newXZ = McaFileHelpers.regionXZFromFileName(source).add(deltaXZ);
            if (relocate(source, McaFileHelpers.createNameFromRegionLocation(newXZ))) {
                relocated++;
            }
        }
        return relocated;
    }

    public int relocateAll(IntPointXZ deltaXZRegions) throws IOException {
        return relocateAll(deltaXZRegions.getX(), deltaXZRegions.getZ());
    }

    /** All methods may return null if the specified mca file does not exist. */
    private interface McaStreamSupplier extends Closeable {
        InputStream get(String mcaType, String mcaName) throws IOException;
        List<String> list() throws IOException;
    }


    private static final Predicate<String> IS_MCA_FILE = Pattern.compile("^r[.]-?\\d+[.]-?\\d+[.]mca$", Pattern.CASE_INSENSITIVE).asPredicate();
    private static final Pattern ZIP_PATH_SPLITTER = Pattern.compile("(?:[.]zip|[.]jar)(?:/|$)", Pattern.CASE_INSENSITIVE);


    private static String normalizeSlashes(String s) {
        return s.replaceAll("[\\\\/]+", "/");
    }

    private static String trimTrailingSlash(String s) {
        if (s.endsWith("/")) {
            return s.substring(0, s.length() - 1);
        }
        return s;
    }
    static class SourcePathParser {
        public final String archiveName;
        public final String path;

        /**
         * Normalizes all slashes in name to forward slash.
         * Trims trailing slashes from all returned parts.
         * Splits name around .zip or .jar extensions.
         */
        public SourcePathParser(String name) throws FileNotFoundException {
            name = normalizeSlashes(name);
            List<String> parts = new ArrayList<>();
            Matcher m = ZIP_PATH_SPLITTER.matcher(name);
            if (m.find()) {
                int lastEnd = 0;
                do {
                    parts.add(trimTrailingSlash(name.substring(lastEnd, m.end())));
                    lastEnd = m.end();
                } while (m.find());
                if (lastEnd < name.length()) {
                    parts.add(trimTrailingSlash(name.substring(lastEnd)));
                }
            } else {
                parts.add(trimTrailingSlash(name));
            }

            String sofar = "";
            String archiveName = null;
            int i;
            for (i = 0; i < parts.size(); i++) {
                File f = Paths.get(sofar, parts.get(i)).toFile();
                sofar = f.getPath();
                if (!f.exists())
                    throw new FileNotFoundException(sofar);
                if (f.isFile()) {
                    archiveName = sofar;
                    break;
                }
            }
            this.archiveName = archiveName;
            if (archiveName == null) {
                path = trimTrailingSlash(name);
            } else {
                sofar = "";
                for (i++; i < parts.size(); i++) {
                    sofar = Paths.get(sofar, parts.get(i)).toString();
                }
                path = sofar;
            }
        }
    }

    /**
     * Supplier of mca InputStreams sourced from the filesystem.
     */
    private static class FileMcaStreamSupplier implements McaStreamSupplier {
        private final String root;

        public FileMcaStreamSupplier(String root) throws FileNotFoundException {
            this.root = root;
            File file = new File(root);
            if (!file.exists()) throw new FileNotFoundException();
            if (!file.isDirectory()) throw new FileNotFoundException("location exists but is not a directory");
        }

        @Override
        public InputStream get(String mcaType, String mcaName) throws IOException {
            File file = Paths.get(root, mcaType, mcaName).toFile();
            if (file.exists() && file.isFile() && Files.size(file.toPath()) >= 0x2000) {
                return new BufferedInputStream(new FileInputStream(file));
            }
            return null;
        }

        @Override
        public List<String> list() {
            File regionDir = Paths.get(root, "region").toFile();
            if (!regionDir.exists() || !regionDir.isDirectory()) {
                return Collections.emptyList();
            }
            String[] files = regionDir.list();
            if (files == null) return Collections.emptyList();
            return Arrays.stream(files)
                    .filter(IS_MCA_FILE)
                    .collect(Collectors.toList());
        }

        @Override
        public void close() throws IOException {
            // no-op
        }
    }

    /**
     * Supplier of mca InputStreams sourced from a zip or jar archive file.
     */
    static class ArchiveMcaStreamSupplier implements McaStreamSupplier {
        final ZipFile zip;
        final String pathPrefix;

        public ArchiveMcaStreamSupplier(String archive, String path) throws IOException {
            zip = new ZipFile(archive);
            pathPrefix = path;

            String regionPath = Paths.get(pathPrefix, "region").toString();
            ZipEntry ze = zip.getEntry(regionPath);
            if (ze == null || !ze.isDirectory()) {
                throw new FileNotFoundException("Expected `" + regionPath + "` folder within archive " + zip.getName());
            }
        }

        @Override
        public InputStream get(String mcaType, String mcaName) throws IOException {
            String p =  normalizeSlashes(Paths.get(pathPrefix, mcaType, mcaName).toString());
            ZipEntry ze = zip.getEntry(p);
            return ze != null && ze.getSize() >= 0x2000 ? zip.getInputStream(ze) : null;
        }

        @Override
        public List<String> list() throws IOException {
            List<String> list = new ArrayList<>();
            final String regionPath = Paths.get(pathPrefix, "region") + "/";
            for (Enumeration<? extends ZipEntry> e = zip.entries(); e.hasMoreElements(); ) {
                ZipEntry ze = e.nextElement();
                if (!ze.isDirectory() && ze.getName().startsWith(regionPath)) {
                    String[] parts = ze.getName().split("/", 2);
                    if (parts.length == 2 && IS_MCA_FILE.test(parts[1])) {
                        list.add(parts[1]);
                    }
                }
            }
            return list;
        }

        @Override
        public void close() throws IOException {
            zip.close();
        }
    }
}
