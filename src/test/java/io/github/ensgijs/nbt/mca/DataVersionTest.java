package io.github.ensgijs.nbt.mca;

import io.github.ensgijs.nbt.io.NamedTag;
import io.github.ensgijs.nbt.io.TextNbtDeserializer;
import io.github.ensgijs.nbt.io.TextNbtHelpers;
import io.github.ensgijs.nbt.io.TextNbtParser;
import io.github.ensgijs.nbt.query.NbtPath;
import io.github.ensgijs.nbt.tag.CompoundTag;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.URL;
import java.nio.channels.Channels;
import java.nio.channels.FileChannel;
import java.nio.channels.ReadableByteChannel;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

public class DataVersionTest extends McaTestCase {
    private static final Pattern ALLOWED_ENUM_DESCRIPTION_PATTERN = Pattern.compile("^(?:FINAL|\\d{2}w\\d{2}[a-z]|CT\\d+[a-z]?|(?:XS|PRE|RC)\\d+|)");

    public void testEnumNamesMatchVersionInformation() {
        for (DataVersion dv : DataVersion.values()) {
            if (dv.id() != 0) {
                StringBuilder sb = new StringBuilder("JAVA_1_");
                sb.append(dv.minor()).append('_');
                if (dv.isFullRelease()) {
                    sb.append(dv.patch());
                } else {
                    if (dv.patch() > 0) sb.append(dv.patch()).append('_');
                    sb.append(dv.getBuildDescription().toUpperCase());
                }
                assertEquals(sb.toString(), dv.name());
                assertTrue("Build description of " + dv.name() + " does not follow convention!",
                        ALLOWED_ENUM_DESCRIPTION_PATTERN.matcher(dv.getBuildDescription()).matches());
            }
        }
    }

    public void testEnumDataVersionCollisions() {
        Set<Integer> seen = new HashSet<>();
        for (DataVersion dv : DataVersion.values()) {
            if (dv.id() != 0) {
                assertTrue("duplicate data version " + dv.id(), seen.add(dv.id()));
            }
        }
    }

    public void testEnumDataVersionsIncreasingOrder() {
        int last = 0;
        for (DataVersion dv : DataVersion.values()) {
            if (dv.id() != 0) {
                assertTrue(dv.toString() + " is out of order", dv.id() >= last);
                last = dv.id();
            }
        }
    }

    public void testBestForNegativeValue() {
        assertEquals(DataVersion.UNKNOWN, DataVersion.bestFor(-42));
    }

    public void testBestForExactFirst() {
        assertEquals(DataVersion.UNKNOWN, DataVersion.bestFor(0));
    }

    public void testBestForExactArbitrary() {
        assertEquals(DataVersion.JAVA_1_15_0, DataVersion.bestFor(2225));
    }

    public void testBestForBetween() {
        assertEquals(DataVersion.JAVA_1_9_15W32C, DataVersion.bestFor(106));
    }

    public void testBestForExactLast() {
        final DataVersion last = DataVersion.values()[DataVersion.values().length - 1];
        assertEquals(last, DataVersion.bestFor(last.id()));
    }

    public void testBestForAfterLast() {
        final DataVersion last = DataVersion.values()[DataVersion.values().length - 1];
        assertEquals(last, DataVersion.bestFor(last.id() + 123));
    }

    public void testToString() {
        assertEquals("2724 (1.17)", DataVersion.JAVA_1_17_0.toString());
        assertEquals("2730 (1.17.1)", DataVersion.JAVA_1_17_1.toString());
        assertEquals("UNKNOWN", DataVersion.UNKNOWN.toString());
        assertEquals("2529 (1.16 20w17a)", DataVersion.JAVA_1_16_20W17A.toString());
        assertEquals("2864 (1.18.1 RC3)", DataVersion.JAVA_1_18_1_RC3.toString());
    }
    
    public void testIsCrossedByTransition() {
        assertFalse(DataVersion.JAVA_1_15_19W36A.isCrossedByTransition(DataVersion.JAVA_1_15_19W36A.id(), DataVersion.JAVA_1_15_19W36A.id()));
        assertFalse(DataVersion.JAVA_1_15_19W36A.isCrossedByTransition(DataVersion.JAVA_1_15_0.id(), DataVersion.JAVA_1_15_1.id()));
        assertFalse(DataVersion.JAVA_1_15_19W36A.isCrossedByTransition(DataVersion.JAVA_1_14_3.id(), DataVersion.JAVA_1_14_4.id()));

        assertFalse(DataVersion.JAVA_1_15_19W36A.isCrossedByTransition(DataVersion.JAVA_1_15_19W36A.id(), DataVersion.JAVA_1_15_19W36A.id() + 1));
        assertFalse(DataVersion.JAVA_1_15_19W36A.isCrossedByTransition(DataVersion.JAVA_1_15_19W36A.id() + 1, DataVersion.JAVA_1_15_19W36A.id()));

        assertTrue(DataVersion.JAVA_1_15_19W36A.isCrossedByTransition(DataVersion.JAVA_1_15_19W36A.id() - 1, DataVersion.JAVA_1_15_19W36A.id()));
        assertTrue(DataVersion.JAVA_1_15_19W36A.isCrossedByTransition(DataVersion.JAVA_1_15_19W36A.id(), DataVersion.JAVA_1_15_19W36A.id() - 1));

        assertTrue(DataVersion.JAVA_1_15_19W36A.isCrossedByTransition(DataVersion.JAVA_1_15_19W36A.id() - 1, DataVersion.JAVA_1_15_19W36A.id() + 1));
        assertTrue(DataVersion.JAVA_1_15_19W36A.isCrossedByTransition(DataVersion.JAVA_1_15_19W36A.id() + 1, DataVersion.JAVA_1_15_19W36A.id() - 1));

        assertTrue(DataVersion.JAVA_1_15_19W36A.isCrossedByTransition(DataVersion.JAVA_1_14_4.id(), DataVersion.JAVA_1_16_0.id()));
        assertTrue(DataVersion.JAVA_1_15_19W36A.isCrossedByTransition(DataVersion.JAVA_1_16_0.id(), DataVersion.JAVA_1_14_4.id()));
    }

    public void testThrowUnsupportedVersionChangeIfCrossed() {
        assertThrowsException(() -> DataVersion.JAVA_1_15_19W36A.throwUnsupportedVersionChangeIfCrossed(DataVersion.JAVA_1_14_4.id(), DataVersion.JAVA_1_16_0.id()),
                UnsupportedVersionChangeException.class);
        assertThrowsNoException(() -> DataVersion.JAVA_1_15_19W36A.throwUnsupportedVersionChangeIfCrossed(DataVersion.JAVA_1_15_19W36A.id(), DataVersion.JAVA_1_15_19W36A.id()));
    }

    public void testPrevious() {
        assertSame(DataVersion.JAVA_1_9_1_PRE2, DataVersion.JAVA_1_9_1_PRE3.previous());
        assertNull(DataVersion.values()[0].previous());
    }

    public void testNext() {
        assertSame(DataVersion.JAVA_1_9_1, DataVersion.JAVA_1_9_1_PRE3.next());
        assertNull(DataVersion.values()[DataVersion.values().length - 1].next());
    }

    // Note that while this test takes a lot of the work out of keeping DataVersions updated it
    // is limited by what Mojang puts into the version manifest. Some versions, it appears, don't
    // make it into the manifest such as the combat test builds and other experimental builds.
    public void testFetchMissingDataVersionInformation() throws IOException {
        Path minecraftVersionsDirectory = Paths.get(System.getenv("APPDATA"), ".minecraft", "versions");
        if (!minecraftVersionsDirectory.toFile().exists()) {
            // probably not on Windows
            return;
        }
        // 1: weekly
        // 2: minor
        // 3: patch?
        // 4: descriptor? (pre#, rc#, etc)
        final Pattern vanillaVersionPattern = Pattern.compile("^(?:(\\d{2}w\\d{2}[a-z])|1[.](\\d+)(?:[.](\\d+))?(?:-(.+))?)$");
        final var isSaneVersionName = vanillaVersionPattern.asPredicate();
        final String mcVerRootStr = minecraftVersionsDirectory.toFile().getAbsolutePath();

        // phase 1 - scan version manifest, download version json files for unknown versions
        CompoundTag versionManifest = TextNbtParser.parseInline(Files.readString(Paths.get(minecraftVersionsDirectory.toString(), "version_manifest_v2.json")));
//        System.out.println(TextNbtHelpers.toTextNbt(versionManifest, true, false));
        for (CompoundTag versionTag : versionManifest.getCompoundList("versions")) {
            String version = versionTag.getString("id");
            if ("18w47b".equals(version)) {  // the version.json file was added to the client/server jars here
                break;  // 1.9 is about as far back as the data version concept exists
            }
            if (DataVersion.find(version) != null || !isSaneVersionName.test(version))
                continue;  // we already know about this version - or it's some crazy thing like "1.RV-Pre1"

            File versionFolder = Paths.get(mcVerRootStr, version).toFile();
            if (!versionFolder.exists()) {
                versionFolder.mkdirs();
                URL url = new URL(versionTag.getString("url"));
                System.out.println("Downloading " + version + ".json from " + url);
                downloadFile(url, Paths.get(mcVerRootStr, version, version + ".json").toFile());
            }
        }

        // phase 2 - download client jars, extract version.json info, build missing DataVersion enums.
        record NewDataVersion(int dataVersion, String enumDef) {}
        List<NewDataVersion> newDataVersionDefs = new ArrayList<>();
        for (String version : minecraftVersionsDirectory.toFile().list()) {
            Matcher m = vanillaVersionPattern.matcher(version);
            if (!m.matches())
                continue;
            if (Paths.get(mcVerRootStr, version).toFile().isDirectory()) {
                DataVersion dv = DataVersion.find(version);
                if (dv == null) {
                    File jarFile = Paths.get(mcVerRootStr, version, version + ".jar").toFile();
                    if (!jarFile.exists()) {
                        URL url = getClientJarUrl(Paths.get(mcVerRootStr, version, version + ".json"));
                        System.out.println("Downloading " + version + ".jar from " + url);
                        downloadFile(url, Paths.get(mcVerRootStr, version, version + ".jar").toFile());
                    }
                    ZipFile zip = new ZipFile(jarFile);
                    ZipEntry ze = zip.getEntry("version.json");
                    if (ze == null) {
                        System.err.println(version + "> Didn't find version.json file in " + jarFile.toPath());
                        continue;
                    }
                    NamedTag versionInfo = new TextNbtDeserializer().fromStream(zip.getInputStream(ze));
                    int dataVersion = ((CompoundTag) versionInfo.getTag()).getInt("world_version");
                    StringBuilder sb = new StringBuilder("JAVA_1_");
                    StringBuilder sbArgs = new StringBuilder("(").append(dataVersion);
                    String comment = "";

                    if (m.group(1) != null) {  // weekly
                        DataVersion nearest = DataVersion.bestFor(dataVersion);
                        if (nearest != null) {
                            sb.append(nearest.minor()).append('_').append(nearest.patch());
                            sbArgs.append(", ").append(nearest.minor()).append(", ").append(nearest.patch());
                            comment += "  // TODO: verify minor and patch versions are correct";
                        } else {
                            sb.append("?_?");
                            comment += "  // TODO: determine minor and patch versions";
                        }
                        sb.append('_').append(m.group(1).toUpperCase());
                        sbArgs.append(", ").append('"').append(m.group(1)).append('"');
                    } else {
                        sb.append(m.group(2));
                        sbArgs.append(", ").append(m.group(2));
                        sb.append('_');
                        if (m.group(3) != null) {
                            sb.append(m.group(3));
                            sbArgs.append(", ").append(m.group(3));
                        } else {
                            sb.append(0);
                            sbArgs.append(", ").append(0);
                        }
                        if (m.group(4) != null) {  // RC, PRE, etc
                            sb.append('_').append(m.group(4).toUpperCase());
                            sbArgs.append(", ").append('"').append(m.group(4).toUpperCase()).append('"');
                        }
                    }
                    sbArgs.append("),");
                    sb.append(sbArgs).append(comment);
                    newDataVersionDefs.add(new NewDataVersion(dataVersion, sb.toString()));
                }
            }
        }
        if (!newDataVersionDefs.isEmpty()) {
            newDataVersionDefs.sort(Comparator.comparingInt(a -> a.dataVersion));
            for (var dv : newDataVersionDefs) {
                System.out.println(dv.enumDef);
            }
            fail("Missing DataVersion's found! Please update DataVersion enums");
        }
    }

    private void downloadFile(URL url, File saveToFile) throws IOException {
        ReadableByteChannel readableByteChannel = Channels.newChannel(url.openStream());
        FileOutputStream fileOutputStream = new FileOutputStream(saveToFile);
        FileChannel fileChannel = fileOutputStream.getChannel();
        fileChannel.transferFrom(readableByteChannel, 0, /* 100MB max */ 100 * (long) Math.pow(2, 20));
    }

    private URL getClientJarUrl(Path versionJsonPath) throws IOException {
        String blob = Files.readString(versionJsonPath);
        int start = blob.indexOf('{', blob.indexOf("\"downloads\""));
        int brackets = 1;
        int end;
        for (end = start + 1; end < blob.length() && brackets > 0; end++) {
            char c = blob.charAt(end);
            if (c == '{') brackets ++;
            if (c == '}') brackets --;
        }
        return new URL(NbtPath.of("client.url").getString(TextNbtParser.parseInline(blob.substring(start, end))));
    }
}
