package io.github.ensgijs.nbt.mca.util;


import java.util.Arrays;
import java.util.Locale;

import static io.github.ensgijs.nbt.mca.DataVersion.JAVA_1_13_18W06A;
import static io.github.ensgijs.nbt.mca.DataVersion.JAVA_1_18_21W37A;

/**
 * Provides data version aware integer to/from string conversion up to {@link io.github.ensgijs.nbt.mca.DataVersion#JAVA_1_18_21W37A} (exclusive).
 */
public final class LegacyBiomes {
    private LegacyBiomes() {}

    public static boolean versionHasLegacyBiomes(int dataVersion) {
        return dataVersion < JAVA_1_18_21W37A.id();
    }

    // TODO: add minimum data version
    private record Biome(int id, String name, String keyedName) {
        static Biome of(int id, String name) {
            return new Biome(id, name, "minecraft:" + name);
        }
    }

    // Mappings copied from MC source.
    // Note that these exact version id's (1506 & 2832) were never publicly released so the question of
    // inclusive vs exclusive is irrelevant.

    /** Mappings for data-version range (..JAVA_1_13_PRE4] */
    private static final Biome[] before1506;  // up to data-version 1506 (between JAVA_1_13_PRE4 & JAVA_1_13_PRE5).
    /** Mappings for data-version range [JAVA_1_13_PRE5..JAVA_1_18_21W37A) */
    private static final Biome[] after1506;  // up to data-version 2832 (between JAVA_1_17_1/JAVA_1_18_XS7 & JAVA_1_18_21W37A).

    static {

        // up to data-version 1506 (between JAVA_1_13_PRE4 & JAVA_1_13_PRE5).
        before1506 = new Biome[168];
        before1506[0] = Biome.of(0, "ocean");
        before1506[1] = Biome.of(1, "plains");
        before1506[2] = Biome.of(2, "desert");
        before1506[3] = Biome.of(3, "mountains");
        before1506[4] = Biome.of(4, "forest");
        before1506[5] = Biome.of(5, "taiga");
        before1506[6] = Biome.of(6, "swamp");
        before1506[7] = Biome.of(7, "river");
        before1506[8] = Biome.of(8, "nether");
        before1506[9] = Biome.of(9, "the_end");
        before1506[10] = Biome.of(10, "frozen_ocean");
        before1506[11] = Biome.of(11, "frozen_river");
        before1506[12] = Biome.of(12, "snowy_tundra");
        before1506[13] = Biome.of(13, "snowy_mountains");
        before1506[14] = Biome.of(14, "mushroom_fields");
        before1506[15] = Biome.of(15, "mushroom_field_shore");
        before1506[16] = Biome.of(16, "beach");
        before1506[17] = Biome.of(17, "desert_hills");
        before1506[18] = Biome.of(18, "wooded_hills");
        before1506[19] = Biome.of(19, "taiga_hills");
        before1506[20] = Biome.of(20, "mountain_edge");
        before1506[21] = Biome.of(21, "jungle");
        before1506[22] = Biome.of(22, "jungle_hills");
        before1506[23] = Biome.of(23, "jungle_edge");
        before1506[24] = Biome.of(24, "deep_ocean");
        before1506[25] = Biome.of(25, "stone_shore");
        before1506[26] = Biome.of(26, "snowy_beach");
        before1506[27] = Biome.of(27, "birch_forest");
        before1506[28] = Biome.of(28, "birch_forest_hills");
        before1506[29] = Biome.of(29, "dark_forest");
        before1506[30] = Biome.of(30, "snowy_taiga");
        before1506[31] = Biome.of(31, "snowy_taiga_hills");
        before1506[32] = Biome.of(32, "giant_tree_taiga");
        before1506[33] = Biome.of(33, "giant_tree_taiga_hills");
        before1506[34] = Biome.of(34, "wooded_mountains");
        before1506[35] = Biome.of(35, "savanna");
        before1506[36] = Biome.of(36, "savanna_plateau");
        before1506[37] = Biome.of(37, "badlands");
        before1506[38] = Biome.of(38, "wooded_badlands_plateau");
        before1506[39] = Biome.of(39, "badlands_plateau");
        before1506[40] = Biome.of(40, "small_end_islands");
        before1506[41] = Biome.of(41, "end_midlands");
        before1506[42] = Biome.of(42, "end_highlands");
        before1506[43] = Biome.of(43, "end_barrens");
        before1506[44] = Biome.of(44, "warm_ocean");
        before1506[45] = Biome.of(45, "lukewarm_ocean");
        before1506[46] = Biome.of(46, "cold_ocean");
        before1506[47] = Biome.of(47, "deep_warm_ocean");
        before1506[48] = Biome.of(48, "deep_lukewarm_ocean");
        before1506[49] = Biome.of(49, "deep_cold_ocean");
        before1506[50] = Biome.of(50, "deep_frozen_ocean");
        before1506[127] = Biome.of(127, "the_void");
        before1506[129] = Biome.of(129, "sunflower_plains");
        before1506[130] = Biome.of(130, "desert_lakes");
        before1506[131] = Biome.of(131, "gravelly_mountains");
        before1506[132] = Biome.of(132, "flower_forest");
        before1506[133] = Biome.of(133, "taiga_mountains");
        before1506[134] = Biome.of(134, "swamp_hills");
        before1506[140] = Biome.of(140, "ice_spikes");
        before1506[149] = Biome.of(149, "modified_jungle");
        before1506[151] = Biome.of(151, "modified_jungle_edge");
        before1506[155] = Biome.of(155, "tall_birch_forest");
        before1506[156] = Biome.of(156, "tall_birch_hills");
        before1506[157] = Biome.of(157, "dark_forest_hills");
        before1506[158] = Biome.of(158, "snowy_taiga_mountains");
        before1506[160] = Biome.of(160, "giant_spruce_taiga");
        before1506[161] = Biome.of(161, "giant_spruce_taiga_hills");
        before1506[162] = Biome.of(162, "modified_gravelly_mountains");
        before1506[163] = Biome.of(163, "shattered_savanna");
        before1506[164] = Biome.of(164, "shattered_savanna_plateau");
        before1506[165] = Biome.of(165, "eroded_badlands");
        before1506[166] = Biome.of(166, "modified_wooded_badlands_plateau");
        before1506[167] = Biome.of(167, "modified_badlands_plateau");

        // up to data-version 2832 (between JAVA_1_17_1/JAVA_1_18_XS7 & JAVA_1_18_21W37A) - after this MC went to palette biomes
        after1506 = new Biome[183];
        after1506[0] = Biome.of(0, "ocean");
        after1506[1] = Biome.of(1, "plains");
        after1506[2] = Biome.of(2, "desert");
        after1506[3] = Biome.of(3, "mountains");
        after1506[4] = Biome.of(4, "forest");
        after1506[5] = Biome.of(5, "taiga");
        after1506[6] = Biome.of(6, "swamp");
        after1506[7] = Biome.of(7, "river");
        after1506[8] = Biome.of(8, "nether_wastes");
        after1506[9] = Biome.of(9, "the_end");
        after1506[10] = Biome.of(10, "frozen_ocean");
        after1506[11] = Biome.of(11, "frozen_river");
        after1506[12] = Biome.of(12, "snowy_tundra");
        after1506[13] = Biome.of(13, "snowy_mountains");
        after1506[14] = Biome.of(14, "mushroom_fields");
        after1506[15] = Biome.of(15, "mushroom_field_shore");
        after1506[16] = Biome.of(16, "beach");
        after1506[17] = Biome.of(17, "desert_hills");
        after1506[18] = Biome.of(18, "wooded_hills");
        after1506[19] = Biome.of(19, "taiga_hills");
        after1506[20] = Biome.of(20, "mountain_edge");
        after1506[21] = Biome.of(21, "jungle");
        after1506[22] = Biome.of(22, "jungle_hills");
        after1506[23] = Biome.of(23, "jungle_edge");
        after1506[24] = Biome.of(24, "deep_ocean");
        after1506[25] = Biome.of(25, "stone_shore");
        after1506[26] = Biome.of(26, "snowy_beach");
        after1506[27] = Biome.of(27, "birch_forest");
        after1506[28] = Biome.of(28, "birch_forest_hills");
        after1506[29] = Biome.of(29, "dark_forest");
        after1506[30] = Biome.of(30, "snowy_taiga");
        after1506[31] = Biome.of(31, "snowy_taiga_hills");
        after1506[32] = Biome.of(32, "giant_tree_taiga");
        after1506[33] = Biome.of(33, "giant_tree_taiga_hills");
        after1506[34] = Biome.of(34, "wooded_mountains");
        after1506[35] = Biome.of(35, "savanna");
        after1506[36] = Biome.of(36, "savanna_plateau");
        after1506[37] = Biome.of(37, "badlands");
        after1506[38] = Biome.of(38, "wooded_badlands_plateau");
        after1506[39] = Biome.of(39, "badlands_plateau");
        after1506[40] = Biome.of(40, "small_end_islands");
        after1506[41] = Biome.of(41, "end_midlands");
        after1506[42] = Biome.of(42, "end_highlands");
        after1506[43] = Biome.of(43, "end_barrens");
        after1506[44] = Biome.of(44, "warm_ocean");
        after1506[45] = Biome.of(45, "lukewarm_ocean");
        after1506[46] = Biome.of(46, "cold_ocean");
        after1506[47] = Biome.of(47, "deep_warm_ocean");
        after1506[48] = Biome.of(48, "deep_lukewarm_ocean");
        after1506[49] = Biome.of(49, "deep_cold_ocean");
        after1506[50] = Biome.of(50, "deep_frozen_ocean");
        after1506[127] = Biome.of(127, "the_void");
        after1506[129] = Biome.of(129, "sunflower_plains");
        after1506[130] = Biome.of(130, "desert_lakes");
        after1506[131] = Biome.of(131, "gravelly_mountains");
        after1506[132] = Biome.of(132, "flower_forest");
        after1506[133] = Biome.of(133, "taiga_mountains");
        after1506[134] = Biome.of(134, "swamp_hills");
        after1506[140] = Biome.of(140, "ice_spikes");
        after1506[149] = Biome.of(149, "modified_jungle");
        after1506[151] = Biome.of(151, "modified_jungle_edge");
        after1506[155] = Biome.of(155, "tall_birch_forest");
        after1506[156] = Biome.of(156, "tall_birch_hills");
        after1506[157] = Biome.of(157, "dark_forest_hills");
        after1506[158] = Biome.of(158, "snowy_taiga_mountains");
        after1506[160] = Biome.of(160, "giant_spruce_taiga");
        after1506[161] = Biome.of(161, "giant_spruce_taiga_hills");
        after1506[162] = Biome.of(162, "modified_gravelly_mountains");
        after1506[163] = Biome.of(163, "shattered_savanna");
        after1506[164] = Biome.of(164, "shattered_savanna_plateau");
        after1506[165] = Biome.of(165, "eroded_badlands");
        after1506[166] = Biome.of(166, "modified_wooded_badlands_plateau");
        after1506[167] = Biome.of(167, "modified_badlands_plateau");
        after1506[168] = Biome.of(168, "bamboo_jungle");
        after1506[169] = Biome.of(169, "bamboo_jungle_hills");
        after1506[170] = Biome.of(170, "soul_sand_valley");
        after1506[171] = Biome.of(171, "crimson_forest");
        after1506[172] = Biome.of(172, "warped_forest");
        after1506[173] = Biome.of(173, "basalt_deltas");
        after1506[174] = Biome.of(174, "dripstone_caves");
        after1506[175] = Biome.of(175, "lush_caves");
        after1506[177] = Biome.of(177, "meadow");
        after1506[178] = Biome.of(178, "grove");
        after1506[179] = Biome.of(179, "snowy_slopes");
        after1506[180] = Biome.of(180, "snowcapped_peaks");
        after1506[181] = Biome.of(181, "lofty_peaks");
        after1506[182] = Biome.of(182, "stony_peaks");
    }


    public static String name(int dataVersion, int id) {
        if (id < 0) return null;
        Biome b;
        if (dataVersion > 1506) {
            b = after1506[id];
        } else {
            b = before1506[id];
        }
        return b != null ? b.name : null;
    }

    public static String keyedName(int dataVersion, int id) {
        if (id < 0) return null;
        Biome b;
        if (dataVersion > 1506) {
            b = after1506[id];
        } else {
            b = before1506[id];
        }
        return b != null ? b.keyedName : null;
    }

    public static int id(int dataVersion, String name) {
        if (name == null) return -1;
        name = name.toLowerCase(Locale.ENGLISH);
        final String searchFor = !name.startsWith("minecraft:") ? name : name.substring(10);
        return Arrays.stream(dataVersion >= JAVA_1_13_18W06A.id() ? after1506 : before1506)
                .filter(b -> b.name.equals(searchFor))
                .findFirst()
                .map(Biome::id)
                .orElse(-1);
    }
}
