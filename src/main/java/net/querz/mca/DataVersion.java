package net.querz.mca;

import java.util.Arrays;
import java.util.Comparator;

// source: version.json file, found in the root directory of the client and server jars
// table of versions can also be found on https://minecraft.fandom.com/wiki/Data_version#List_of_data_versions
// google sheet to help generate enum values https://docs.google.com/spreadsheets/d/1VVGUPe9sfsd3rsFYcBGDnt1bTifBh3dUkKV9vQvNWQY
//   - paste rows from the fandom table into the sheet and sort ascending by data version (if you don't sort it the mc version may be wrong!)

/**
 * List of MC versions and MCA data versions back to 1.9.0
 * The set of non-full release versions does not need to be the complete set of all versions - only those
 * which introduce changes to the MCA data structure are useful. BUT - we humans do love completeness
 * and completeness would be useful for map viewers / editors.
 */
public enum DataVersion {
    // Kept in ASC order (unit test enforced)
    UNKNOWN(0, 0, 0),
    JAVA_1_9_15W32A(100, 9, 0, "15w32a"),
    JAVA_1_9_0(169, 9, 0),
    JAVA_1_9_1_PRE1(170, 9, 1, "PRE1"),
    JAVA_1_9_1_PRE2(171, 9, 1, "PRE2"),
    JAVA_1_9_1_PRE3(172, 9, 1, "PRE3"),
    JAVA_1_9_1(175, 9, 1),
    JAVA_1_9_2(176, 9, 2),
    JAVA_1_9_3_16W14A(177, 9, 3, "16w14a"),
    JAVA_1_9_3_16W15A(178, 9, 3, "16w15a"),
    JAVA_1_9_3_16W15B(179, 9, 3, "16w15b"),
    JAVA_1_9_3_PRE1(180, 9, 3, "PRE1"),
    JAVA_1_9_3_PRE2(181, 9, 3, "PRE2"),
    JAVA_1_9_3_PRE3(182, 9, 3, "PRE3"),
    JAVA_1_9_3(183, 9, 3),
    JAVA_1_9_4(184, 9, 4),
    JAVA_1_10_16W20A(501, 10, 0, "16w20a"),
    JAVA_1_10_16W21A(503, 10, 0, "16w21a"),
    JAVA_1_10_16W21B(504, 10, 0, "16w21b"),
    JAVA_1_10_PRE1(506, 10, 0, "PRE1"),
    JAVA_1_10_PRE2(507, 10, 0, "PRE2"),
    JAVA_1_10_0(510, 10, 0),
    JAVA_1_10_1(511, 10, 1),
    JAVA_1_10_2(512, 10, 2),
    JAVA_1_11_16W32A(800, 11, 0, "16w32a"),
    JAVA_1_11_16W32B(801, 11, 0, "16w32b"),
    JAVA_1_11_16W33A(802, 11, 0, "16w33a"),
    JAVA_1_11_16W35A(803, 11, 0, "16w35a"),
    JAVA_1_11_16W36A(805, 11, 0, "16w36a"),
    JAVA_1_11_16W38A(807, 11, 0, "16w38a"),
    JAVA_1_11_16W39A(809, 11, 0, "16w39a"),
    JAVA_1_11_16W39B(811, 11, 0, "16w39b"),
    JAVA_1_11_16W39C(812, 11, 0, "16w39c"),
    JAVA_1_11_16W40A(813, 11, 0, "16w40a"),
    JAVA_1_11_16W41A(814, 11, 0, "16w41a"),
    JAVA_1_11_16W42A(815, 11, 0, "16w42a"),
    JAVA_1_11_16W43A(816, 11, 0, "16w43a"),
    JAVA_1_11_16W44A(817, 11, 0, "16w44a"),
    JAVA_1_11_PRE1(818, 11, 0, "PRE1"),
    JAVA_1_11_0(819, 11, 0),
    JAVA_1_11_1_16W50A(920, 11, 1, "16w50a"),
    JAVA_1_11_1(921, 11, 1),
    JAVA_1_11_2(922, 11, 2),
    JAVA_1_12_17W06A(1022, 12, 0, "17w06a"),
    JAVA_1_12_17W13A(1122, 12, 0, "17w13a"),
    JAVA_1_12_17W13B(1123, 12, 0, "17w13b"),
    JAVA_1_12_17W14A(1124, 12, 0, "17w14a"),
    JAVA_1_12_17W15A(1125, 12, 0, "17w15a"),
    JAVA_1_12_17W16A(1126, 12, 0, "17w16a"),
    JAVA_1_12_17W16B(1127, 12, 0, "17w16b"),
    JAVA_1_12_17W17A(1128, 12, 0, "17w17a"),
    JAVA_1_12_17W17B(1129, 12, 0, "17w17b"),
    JAVA_1_12_17W18A(1130, 12, 0, "17w18a"),
    JAVA_1_12_17W18B(1131, 12, 0, "17w18b"),
    JAVA_1_12_PRE1(1132, 12, 0, "PRE1"),
    JAVA_1_12_PRE2(1133, 12, 0, "PRE2"),
    JAVA_1_12_PRE3(1134, 12, 0, "PRE3"),
    JAVA_1_12_PRE4(1135, 12, 0, "PRE4"),
    JAVA_1_12_PRE5(1136, 12, 0, "PRE5"),
    JAVA_1_12_PRE6(1137, 12, 0, "PRE6"),
    JAVA_1_12_PRE7(1138, 12, 0, "PRE7"),
    JAVA_1_12_0(1139, 12, 0),
    JAVA_1_12_1_17W31A(1239, 12, 1, "17w31a"),
    JAVA_1_12_1_PRE1(1240, 12, 1, "PRE1"),
    JAVA_1_12_1(1241, 12, 1),
    JAVA_1_12_2_PRE1(1341, 12, 2, "PRE1"),
    JAVA_1_12_2_PRE2(1342, 12, 2, "PRE2"),
    JAVA_1_12_2(1343, 12, 2),
    JAVA_1_13_17W43A(1444, 13, 0, "17w43a"),
    JAVA_1_13_17W43B(1445, 13, 0, "17w43b"),
    JAVA_1_13_17W45A(1447, 13, 0, "17w45a"),
    JAVA_1_13_17W45B(1448, 13, 0, "17w45b"),
    JAVA_1_13_17W46A(1449, 13, 0, "17w46a"),
    JAVA_1_13_17W47A(1451, 13, 0, "17w47a"),
    JAVA_1_13_17W47B(1452, 13, 0, "17w47b"),
    JAVA_1_13_17W48A(1453, 13, 0, "17w48a"),
    JAVA_1_13_17W49A(1454, 13, 0, "17w49a"),
    JAVA_1_13_17W49B(1455, 13, 0, "17w49b"),
    JAVA_1_13_17W50A(1457, 13, 0, "17w50a"),
    JAVA_1_13_18W01A(1459, 13, 0, "18w01a"),
    JAVA_1_13_18W02A(1461, 13, 0, "18w02a"),
    JAVA_1_13_18W03A(1462, 13, 0, "18w03a"),
    JAVA_1_13_18W03B(1463, 13, 0, "18w03b"),
    JAVA_1_13_18W05A(1464, 13, 0, "18w05a"),
    /** Believe this to be the end of the Level.LightPopulated tag (replaced by Level.isLightOn ?) */
    JAVA_1_13_18W06A(1466, 13, 0, "18w06a"),
    JAVA_1_13_18W07A(1467, 13, 0, "18w07a"),
    JAVA_1_13_18W07B(1468, 13, 0, "18w07b"),
    JAVA_1_13_18W07C(1469, 13, 0, "18w07c"),
    JAVA_1_13_18W08A(1470, 13, 0, "18w08a"),
    JAVA_1_13_18W08B(1471, 13, 0, "18w08b"),
    JAVA_1_13_18W09A(1472, 13, 0, "18w09a"),
    JAVA_1_13_18W10A(1473, 13, 0, "18w10a"),
    JAVA_1_13_18W10B(1474, 13, 0, "18w10b"),
    JAVA_1_13_18W10C(1476, 13, 0, "18w10c"),
    JAVA_1_13_18W10D(1477, 13, 0, "18w10d"),
    JAVA_1_13_18W11A(1478, 13, 0, "18w11a"),
    JAVA_1_13_18W14A(1479, 13, 0, "18w14a"),
    JAVA_1_13_18W14B(1481, 13, 0, "18w14b"),
    JAVA_1_13_18W15A(1482, 13, 0, "18w15a"),
    JAVA_1_13_18W16A(1483, 13, 0, "18w16a"),
    JAVA_1_13_18W19A(1484, 13, 0, "18w19a"),
    JAVA_1_13_18W19B(1485, 13, 0, "18w19b"),
    JAVA_1_13_18W20A(1489, 13, 0, "18w20a"),
    JAVA_1_13_18W20B(1491, 13, 0, "18w20b"),
    /** Believe this to be the end of the Level.hasLegacyStructureData tag */
    JAVA_1_13_18W20C(1493, 13, 0, "18w20c"),
    JAVA_1_13_18W21A(1495, 13, 0, "18w21a"),
    JAVA_1_13_18W21B(1496, 13, 0, "18w21b"),
    JAVA_1_13_18W22A(1497, 13, 0, "18w22a"),
    JAVA_1_13_18W22B(1498, 13, 0, "18w22b"),
    JAVA_1_13_18W22C(1499, 13, 0, "18w22c"),
    JAVA_1_13_PRE1(1501, 13, 0, "PRE1"),
    JAVA_1_13_PRE2(1502, 13, 0, "PRE2"),
    JAVA_1_13_PRE3(1503, 13, 0, "PRE3"),
    JAVA_1_13_PRE4(1504, 13, 0, "PRE4"),
    JAVA_1_13_PRE5(1511, 13, 0, "PRE5"),
    JAVA_1_13_PRE6(1512, 13, 0, "PRE6"),
    JAVA_1_13_PRE7(1513, 13, 0, "PRE7"),
    JAVA_1_13_PRE8(1516, 13, 0, "PRE8"),
    JAVA_1_13_PRE9(1517, 13, 0, "PRE9"),
    JAVA_1_13_PRE10(1518, 13, 0, "PRE10"),
    /** biome data now stored in IntArrayTag instead of ByteArrayTag (still 2D using only 256 entries) */
    JAVA_1_13_0(1519, 13, 0),
    JAVA_1_13_1_18W30A(1620, 13, 1, "18w30a"),
    JAVA_1_13_1_18W30B(1621, 13, 1, "18w30b"),
    JAVA_1_13_1_18W31A(1622, 13, 1, "18w31a"),
    JAVA_1_13_1_18W32A(1623, 13, 1, "18w32a"),
    JAVA_1_13_1_18W33A(1625, 13, 1, "18w33a"),
    JAVA_1_13_1_PRE1(1626, 13, 1, "PRE1"),
    JAVA_1_13_1_PRE2(1627, 13, 1, "PRE2"),
    JAVA_1_13_1(1628, 13, 1),
    JAVA_1_13_2_PRE1(1629, 13, 2, "PRE1"),
    JAVA_1_13_2_PRE2(1630, 13, 2, "PRE2"),
    JAVA_1_13_2(1631, 13, 2),
    JAVA_1_14_18W43A(1901, 14, 0, "18w43a"),
    JAVA_1_14_18W43B(1902, 14, 0, "18w43b"),
    JAVA_1_14_18W43C(1903, 14, 0, "18w43c"),
    JAVA_1_14_18W44A(1907, 14, 0, "18w44a"),
    JAVA_1_14_18W45A(1908, 14, 0, "18w45a"),
    JAVA_1_14_18W46A(1910, 14, 0, "18w46a"),
    JAVA_1_14_18W47A(1912, 14, 0, "18w47a"),
    JAVA_1_14_18W47B(1913, 14, 0, "18w47b"),
    JAVA_1_14_18W48A(1914, 14, 0, "18w48a"),
    JAVA_1_14_18W48B(1915, 14, 0, "18w48b"),
    JAVA_1_14_18W49A(1916, 14, 0, "18w49a"),
    JAVA_1_14_18W50A(1919, 14, 0, "18w50a"),
    JAVA_1_14_19W02A(1921, 14, 0, "19w02a"),
    JAVA_1_14_19W03A(1922, 14, 0, "19w03a"),
    JAVA_1_14_19W03B(1923, 14, 0, "19w03b"),
    JAVA_1_14_19W03C(1924, 14, 0, "19w03c"),
    JAVA_1_14_19W04A(1926, 14, 0, "19w04a"),
    JAVA_1_14_19W04B(1927, 14, 0, "19w04b"),
    JAVA_1_14_19W05A(1930, 14, 0, "19w05a"),
    JAVA_1_14_19W06A(1931, 14, 0, "19w06a"),
    JAVA_1_14_19W07A(1932, 14, 0, "19w07a"),
    JAVA_1_14_19W08A(1933, 14, 0, "19w08a"),
    JAVA_1_14_19W08B(1934, 14, 0, "19w08b"),
    JAVA_1_14_19W09A(1935, 14, 0, "19w09a"),
    JAVA_1_14_19W11A(1937, 14, 0, "19w11a"),
    JAVA_1_14_19W11B(1938, 14, 0, "19w11b"),
    JAVA_1_14_19W12A(1940, 14, 0, "19w12a"),
    JAVA_1_14_19W12B(1941, 14, 0, "19w12b"),
    JAVA_1_14_19W13A(1942, 14, 0, "19w13a"),
    JAVA_1_14_19W13B(1943, 14, 0, "19w13b"),
    JAVA_1_14_19W14A(1944, 14, 0, "19w14a"),
    JAVA_1_14_19W14B(1945, 14, 0, "19w14b"),
    JAVA_1_14_PRE1(1947, 14, 0, "PRE1"),
    JAVA_1_14_PRE2(1948, 14, 0, "PRE2"),
    JAVA_1_14_PRE3(1949, 14, 0, "PRE3"),
    JAVA_1_14_PRE4(1950, 14, 0, "PRE4"),
    JAVA_1_14_PRE5(1951, 14, 0, "PRE5"),
    /** /poi/r.X.Z.mca files introduced */
    JAVA_1_14_0(1952, 14, 0),
    JAVA_1_14_1_PRE1(1955, 14, 1, "PRE1"),
    JAVA_1_14_1_PRE2(1956, 14, 1, "PRE2"),
    JAVA_1_14_1(1957, 14, 1),
    JAVA_1_14_2_PRE1(1958, 14, 2, "PRE1"),
    JAVA_1_14_2_PRE2(1959, 14, 2, "PRE2"),
    JAVA_1_14_2_PRE3(1960, 14, 2, "PRE3"),
    JAVA_1_14_2_PRE4(1962, 14, 2, "PRE4"),
    JAVA_1_14_2(1963, 14, 2),
    JAVA_1_14_3_PRE1(1964, 14, 3, "PRE1"),
    JAVA_1_14_3_PRE2(1965, 14, 3, "PRE2"),
    JAVA_1_14_3_PRE3(1966, 14, 3, "PRE3"),
    JAVA_1_14_3_PRE4(1967, 14, 3, "PRE4"),
    JAVA_1_14_3(1968, 14, 3),
    JAVA_1_14_4_PRE1(1969, 14, 4, "PRE1"),
    JAVA_1_14_4_PRE2(1970, 14, 4, "PRE2"),
    JAVA_1_14_4_PRE3(1971, 14, 4, "PRE3"),
    JAVA_1_14_4_PRE4(1972, 14, 4, "PRE4"),
    JAVA_1_14_4_PRE5(1973, 14, 4, "PRE5"),
    JAVA_1_14_4_PRE6(1974, 14, 4, "PRE6"),
    JAVA_1_14_4_PRE7(1975, 14, 4, "PRE7"),
    JAVA_1_14_4(1976, 14, 4),
    JAVA_1_14_3_CT1(2067, 14, 3, "CT1"),
    JAVA_1_15_CT2(2068, 15, 0, "CT2"),
    JAVA_1_15_CT3(2069, 15, 0, "CT3"),
    JAVA_1_15_19W34A(2200, 15, 0, "19w34a"),
    JAVA_1_15_19W35A(2201, 15, 0, "19w35a"),
    /**
     * 3D Biomes added. Biomes array in the  Level tag for each chunk changed
     * to contain 1024 integers instead of 256 see {@link Chunk}
     */
    JAVA_1_15_19W36A(2203, 15, 0, "19w36a"),
    JAVA_1_15_19W37A(2204, 15, 0, "19w37a"),
    JAVA_1_15_19W38A(2205, 15, 0, "19w38a"),
    JAVA_1_15_19W38B(2206, 15, 0, "19w38b"),
    JAVA_1_15_19W39A(2207, 15, 0, "19w39a"),
    JAVA_1_15_19W40A(2208, 15, 0, "19w40a"),
    JAVA_1_15_19W41A(2210, 15, 0, "19w41a"),
    JAVA_1_15_19W42A(2212, 15, 0, "19w42a"),
    JAVA_1_15_19W44A(2213, 15, 0, "19w44a"),
    JAVA_1_15_19W45A(2214, 15, 0, "19w45a"),
    JAVA_1_15_19W45B(2215, 15, 0, "19w45b"),
    JAVA_1_15_19W46A(2216, 15, 0, "19w46a"),
    JAVA_1_15_19W46B(2217, 15, 0, "19w46b"),
    JAVA_1_15_PRE1(2218, 15, 0, "PRE1"),
    JAVA_1_15_PRE2(2219, 15, 0, "PRE2"),
    JAVA_1_15_PRE3(2220, 15, 0, "PRE3"),
    JAVA_1_15_PRE4(2221, 15, 0, "PRE4"),
    JAVA_1_15_PRE5(2222, 15, 0, "PRE5"),
    JAVA_1_15_PRE6(2223, 15, 0, "PRE6"),
    JAVA_1_15_PRE7(2224, 15, 0, "PRE7"),
    JAVA_1_15_0(2225, 15, 0),
    JAVA_1_15_1_PRE1(2226, 15, 1, "PRE1"),
    JAVA_1_15_1(2227, 15, 1),
    JAVA_1_15_2_PRE1(2228, 15, 2, "PRE1"),
    JAVA_1_15_2_PRE2(2229, 15, 2, "PRE2"),
    JAVA_1_15_2(2230, 15, 2),
    JAVA_1_16_CT4(2320, 16, 0, "CT4"),
    JAVA_1_16_CT5(2321, 16, 0, "CT5"),
    JAVA_1_16_20W06A(2504, 16, 0, "20w06a"),
    JAVA_1_16_20W07A(2506, 16, 0, "20w07a"),
    JAVA_1_16_20W08A(2507, 16, 0, "20w08a"),
    JAVA_1_16_20W09A(2510, 16, 0, "20w09a"),
    JAVA_1_16_20W10A(2512, 16, 0, "20w10a"),
    JAVA_1_16_20W11A(2513, 16, 0, "20w11a"),
    JAVA_1_16_20W12A(2515, 16, 0, "20w12a"),
    JAVA_1_16_20W13A(2520, 16, 0, "20w13a"),
    JAVA_1_16_20W13B(2521, 16, 0, "20w13b"),
    JAVA_1_16_20W14A(2524, 16, 0, "20w14a"),
    JAVA_1_16_20W15A(2525, 16, 0, "20w15a"),
    JAVA_1_16_20W16A(2526, 16, 0, "20w16a"),
    /** Block pallet packing changed in this version - see {@link Section} */
    JAVA_1_16_20W17A(2529, 16, 0, "20w17a"),
    JAVA_1_16_20W18A(2532, 16, 0, "20w18a"),
    JAVA_1_16_20W19A(2534, 16, 0, "20w19a"),
    JAVA_1_16_20W20A(2536, 16, 0, "20w20a"),
    JAVA_1_16_20W20B(2537, 16, 0, "20w20b"),
    JAVA_1_16_20W21A(2554, 16, 0, "20w21a"),
    JAVA_1_16_20W22A(2555, 16, 0, "20w22a"),
    JAVA_1_16_PRE1(2556, 16, 0, "PRE1"),
    JAVA_1_16_PRE2(2557, 16, 0, "PRE2"),
    JAVA_1_16_PRE3(2559, 16, 0, "PRE3"),
    JAVA_1_16_PRE4(2560, 16, 0, "PRE4"),
    JAVA_1_16_PRE5(2561, 16, 0, "PRE5"),
    JAVA_1_16_PRE6(2562, 16, 0, "PRE6"),
    JAVA_1_16_PRE7(2563, 16, 0, "PRE7"),
    JAVA_1_16_PRE8(2564, 16, 0, "PRE8"),
    JAVA_1_16_RC1(2565, 16, 0, "RC1"),
    JAVA_1_16_0(2566, 16, 0),
    JAVA_1_16_1(2567, 16, 1),
    JAVA_1_16_2_20W27A(2569, 16, 2, "20w27a"),
    JAVA_1_16_2_20W28A(2570, 16, 2, "20w28a"),
    JAVA_1_16_2_20W29A(2571, 16, 2, "20w29a"),
    JAVA_1_16_2_20W30A(2572, 16, 2, "20w30a"),
    JAVA_1_16_2_PRE1(2573, 16, 2, "PRE1"),
    JAVA_1_16_2_PRE2(2574, 16, 2, "PRE2"),
    JAVA_1_16_2_PRE3(2575, 16, 2, "PRE3"),
    JAVA_1_16_2_RC1(2576, 16, 2, "RC1"),
    JAVA_1_16_2_RC2(2577, 16, 2, "RC2"),
    JAVA_1_16_2(2578, 16, 2),
    JAVA_1_16_3_RC1(2579, 16, 3, "RC1"),
    JAVA_1_16_3(2580, 16, 3),
    JAVA_1_16_4_PRE1(2581, 16, 4, "PRE1"),
    JAVA_1_16_4_PRE2(2582, 16, 4, "PRE2"),
    JAVA_1_16_4_RC1(2583, 16, 4, "RC1"),
    JAVA_1_16_4(2584, 16, 4),
    JAVA_1_16_5_RC1(2585, 16, 5, "RC1"),
    JAVA_1_16_5(2586, 16, 5),
    /**
     * /entities/r.X.Z.mca files introduced.
     * Entities no longer inside region/r.X.Z.mca - except in un-migrated chunks AND during some phases of chunk generation.
     * <p>https://www.minecraft.net/en-us/article/minecraft-snapshot-20w45a</p>
     */
    JAVA_1_17_20W45A(2681, 17, 0, "20w45a"),
    JAVA_1_17_20W46A(2682, 17, 0, "20w46a"),
    JAVA_1_17_20W48A(2683, 17, 0, "20w48a"),
    JAVA_1_17_20W49A(2685, 17, 0, "20w49a"),
    JAVA_1_17_20W51A(2687, 17, 0, "20w51a"),
    JAVA_1_17_21W03A(2689, 17, 0, "21w03a"),
    JAVA_1_17_21W05A(2690, 17, 0, "21w05a"),
    JAVA_1_17_21W05B(2692, 17, 0, "21w05b"),
    JAVA_1_17_21W06A(2694, 17, 0, "21w06a"),
    JAVA_1_17_21W07A(2695, 17, 0, "21w07a"),
    JAVA_1_17_21W08A(2697, 17, 0, "21w08a"),
    JAVA_1_17_21W08B(2698, 17, 0, "21w08b"),
    JAVA_1_17_21W10A(2699, 17, 0, "21w10a"),
    JAVA_1_17_CT6(2701, 17, 0, "CT6"),
    JAVA_1_17_CT7(2702, 17, 0, "CT7"),
    JAVA_1_17_21W11A(2703, 17, 0, "21w11a"),
//    JAVA_1_17_CT7B(2703, 17, 0, "CT7b"),
    JAVA_1_17_CT7C(2704, 17, 0, "CT7c"),
    JAVA_1_17_21W13A(2705, 17, 0, "21w13a"),
//    JAVA_1_17_CT8(2705, 17, 0, "CT8"),
    JAVA_1_17_21W14A(2706, 17, 0, "21w14a"),
//    JAVA_1_17_CT8B(2706, 17, 0, "CT8b"),
    JAVA_1_17_CT8C(2707, 17, 0, "CT8c"),
    JAVA_1_17_21W15A(2709, 17, 0, "21w15a"),
    JAVA_1_17_21W16A(2711, 17, 0, "21w16a"),
    JAVA_1_17_21W17A(2712, 17, 0, "21w17a"),
    JAVA_1_17_21W18A(2713, 17, 0, "21w18a"),
    JAVA_1_17_21W19A(2714, 17, 0, "21w19a"),
    JAVA_1_17_21W20A(2715, 17, 0, "21w20a"),
    JAVA_1_17_PRE1(2716, 17, 0, "PRE1"),
    JAVA_1_17_PRE2(2718, 17, 0, "PRE2"),
    JAVA_1_17_PRE3(2719, 17, 0, "PRE3"),
    JAVA_1_17_PRE4(2720, 17, 0, "PRE4"),
    JAVA_1_17_PRE5(2721, 17, 0, "PRE5"),
    JAVA_1_17_RC1(2722, 17, 0, "RC1"),
    JAVA_1_17_RC2(2723, 17, 0, "RC2"),
    JAVA_1_17_0(2724, 17, 0),
    JAVA_1_17_1_PRE1(2725, 17, 1, "PRE1"),
    JAVA_1_17_1_PRE2(2726, 17, 1, "PRE2"),
    JAVA_1_17_1_PRE3(2727, 17, 1, "PRE3"),
    JAVA_1_17_1_RC1(2728, 17, 1, "RC1"),
    JAVA_1_17_1_RC2(2729, 17, 1, "RC2"),
    JAVA_1_17_1(2730, 17, 1),
    JAVA_1_18_XS1(2825, 18, 0, "XS1"),
    JAVA_1_18_XS2(2826, 18, 0, "XS2"),
    JAVA_1_18_XS3(2827, 18, 0, "XS3"),
    JAVA_1_18_XS4(2828, 18, 0, "XS4"),
    JAVA_1_18_XS5(2829, 18, 0, "XS5"),
    JAVA_1_18_XS6(2830, 18, 0, "XS6"),
    JAVA_1_18_XS7(2831, 18, 0, "XS7"),
    JAVA_1_18_21W37A(2834, 18, 0, "21w37a"),
    JAVA_1_18_21W38A(2835, 18, 0, "21w38a"),
    /**
     * https://www.minecraft.net/en-us/article/minecraft-snapshot-21w39a
     * <ul>
     * <li>Level.Sections[].BlockStates &amp; Level.Sections[].Palette have moved to a container structure in Level.Sections[].block_states
     * <li>Level.Biomes are now paletted and live in a similar container structure in Level.Sections[].biomes
     * <li>Level.CarvingMasks[] is now long[] instead of byte[]
     * </ul>
     */
    JAVA_1_18_21W39A(2836, 18, 0, "21w39a"),
    JAVA_1_18_21W40A(2838, 18, 0, "21w40a"),
    JAVA_1_18_21W41A(2839, 18, 0, "21w41a"),
    JAVA_1_18_21W42A(2840, 18, 0, "21w42a"),
    /**
     * https://www.minecraft.net/en-us/article/minecraft-snapshot-21w43a
     * <ul>
     * <li>Removed chunk’s Level and moved everything it contained up
     * <li>Chunk’s Level.Entities has moved to entities -- entities are stored in the terrain region file during chunk generation
     * <li>Chunk’s Level.TileEntities has moved to block_entities
     * <li>Chunk’s Level.TileTicks and Level.ToBeTicked have moved to block_ticks
     * <li>Chunk’s Level.LiquidTicks and Level.LiquidsToBeTicked have moved to fluid_ticks
     * <li>Chunk’s Level.Sections has moved to sections
     * <li>Chunk’s Level.Structures has moved to structures
     * <li>Chunk’s Level.Structures.Starts has moved to structures.starts
     * <li>Chunk’s Level.Sections[].BlockStates and Level.Sections[].Palette have moved to a container structure in sections[].block_states
     * <li>Chunk’s Level.Biomes are now paletted and live in a similar container structure in sections[].biomes
     * <li>Added yPos the minimum section y position in the chunk
     * <li>Added below_zero_retrogen containing data to support below zero generation
     * <li>Added blending_data containing data to support blending new world generation with existing chunks
     * </ul>
     */
    JAVA_1_18_21W43A(2844, 18, 0, "21w43a"),
    JAVA_1_18_21W44A(2845, 18, 0, "21w44a"),
    JAVA_1_18_PRE1(2847, 18, 0, "PRE1"),
    JAVA_1_18_PRE2(2848, 18, 0, "PRE2"),
    JAVA_1_18_PRE3(2849, 18, 0, "PRE3"),
    JAVA_1_18_PRE4(2850, 18, 0, "PRE4"),
    JAVA_1_18_PRE5(2851, 18, 0, "PRE5"),
    JAVA_1_18_PRE6(2853, 18, 0, "PRE6"),
    JAVA_1_18_PRE7(2854, 18, 0, "PRE7"),
    JAVA_1_18_PRE8(2855, 18, 0, "PRE8"),
    JAVA_1_18_RC1(2856, 18, 0, "RC1"),
    JAVA_1_18_RC2(2857, 18, 0, "RC2"),
    JAVA_1_18_RC3(2858, 18, 0, "RC3"),
    JAVA_1_18_RC4(2859, 18, 0, "RC4"),
    JAVA_1_18_0(2860, 18, 0),
    JAVA_1_18_1_PRE1(2861, 18, 1, "PRE1"),
    JAVA_1_18_1_RC1(2862, 18, 1, "RC1"),
    JAVA_1_18_1_RC2(2863, 18, 1, "RC2"),
    JAVA_1_18_1_RC3(2864, 18, 1, "RC3"),
    JAVA_1_18_1(2865, 18, 1),
    // need to evaluate msa changes from here forward
    JAVA_1_18_2_22W03A(2966, 18, 2, "22w03a"),
    JAVA_1_18_2_22W05A(2967, 18, 2, "22w05a"),
    JAVA_1_18_2_22W06A(2968, 18, 2, "22w06a"),
    JAVA_1_18_2_22W07A(2969, 18, 2, "22w07a"),
    JAVA_1_18_2_PRE1(2971, 18, 2, "PRE1"),
    JAVA_1_18_2_PRE2(2972, 18, 2, "PRE2"),
    JAVA_1_18_2_PRE3(2973, 18, 2, "PRE3"),
    JAVA_1_18_2_RC1(2974, 18, 2, "RC1"),
    JAVA_1_18_2(2975, 18, 2),
    JAVA_1_19_XS1(3066, 19, 0, "XS1"),
    JAVA_1_19_22W11A(3080, 19, 0, "22w11a"),
    JAVA_1_19_22W12A(3082, 19, 0, "22w12a"),
    JAVA_1_19_22W13A(3085, 19, 0, "22w13a"),
    JAVA_1_19_22W14A(3088, 19, 0, "22w14a"),
    JAVA_1_19_22W15A(3089, 19, 0, "22w15a"),
    JAVA_1_19_22W16A(3091, 19, 0, "22w16a"),
    JAVA_1_19_22W16B(3092, 19, 0, "22w16b"),
    JAVA_1_19_22W17A(3093, 19, 0, "22w17a"),
    JAVA_1_19_22W18A(3095, 19, 0, "22w18a"),
    JAVA_1_19_22W19A(3096, 19, 0, "22w19a"),
    JAVA_1_19_PRE1(3098, 19, 0, "PRE1"),
    JAVA_1_19_PRE2(3099, 19, 0, "PRE2"),
    JAVA_1_19_PRE3(3100, 19, 0, "PRE3"),
    JAVA_1_19_PRE4(3101, 19, 0, "PRE4"),
    JAVA_1_19_PRE5(3102, 19, 0, "PRE5"),
    JAVA_1_19_RC1(3103, 19, 0, "RC1"),
    JAVA_1_19_RC2(3104, 19, 0, "RC2"),
    JAVA_1_19_0(3105, 19, 0),
    JAVA_1_19_1_22W24A(3106, 19, 1, "22w24a"),
    JAVA_1_19_1_PRE1(3107, 19, 1, "PRE1"),
    JAVA_1_19_1_RC1(3109, 19, 1, "RC1"),
    JAVA_1_19_1_PRE2(3110, 19, 1, "PRE2"),
    JAVA_1_19_1_PRE3(3111, 19, 1, "PRE3"),
    JAVA_1_19_1_PRE4(3112, 19, 1, "PRE4"),
    JAVA_1_19_1_PRE5(3113, 19, 1, "PRE5"),
    JAVA_1_19_1_PRE6(3114, 19, 1, "PRE6"),
    JAVA_1_19_1_RC2(3115, 19, 1, "RC2"),
    JAVA_1_19_1_RC3(3116, 19, 1, "RC3"),
    JAVA_1_19_1(3117, 19, 1),
    JAVA_1_19_2_RC1(3118, 19, 2, "RC1"),
    JAVA_1_19_2_RC2(3119, 19, 2, "RC2"),
    JAVA_1_19_2(3120, 19, 2),
    JAVA_1_19_3_22W42A(3205, 19, 3, "22w42a"),
    JAVA_1_19_3_22W43A(3206, 19, 3, "22w43a"),
    JAVA_1_19_3_22W44A(3207, 19, 3, "22w44a"),
    JAVA_1_19_3_22W45A(3208, 19, 3, "22w45a"),
    JAVA_1_19_3_22W46A(3210, 19, 3, "22w46a"),
    JAVA_1_19_3_PRE1(3211, 19, 3, "PRE1"),
    JAVA_1_19_3_PRE2(3212, 19, 3, "PRE2"),
    JAVA_1_19_3_PRE3(3213, 19, 3, "PRE3"),
    JAVA_1_19_3_RC1(3215, 19, 3, "RC1"),
    JAVA_1_19_3_RC2(3216, 19, 3, "RC2"),
    JAVA_1_19_3_RC3(3217, 19, 3, "RC3"),
    JAVA_1_19_3(3218, 19, 3),
    JAVA_1_19_4_23W03A(3320, 19, 4, "23w03a"),
    JAVA_1_19_4_23W04A(3321, 19, 4, "23w04a"),
    JAVA_1_19_4_23W05A(3323, 19, 4, "23w05a"),
    JAVA_1_19_4_23W06A(3326, 19, 4, "23w06a"),
    JAVA_1_19_4_23W07A(3329, 19, 4, "23w07a"),
    JAVA_1_19_4_PRE1(3330, 19, 4, "PRE1"),
    JAVA_1_19_4_PRE2(3331, 19, 4, "PRE2"),
    JAVA_1_19_4_PRE3(3332, 19, 4, "PRE3"),
    JAVA_1_19_4_PRE4(3333, 19, 4, "PRE4"),
    JAVA_1_19_4_RC1(3334, 19, 4, "RC1"),
    JAVA_1_19_4_RC2(3335, 19, 4, "RC2"),
    JAVA_1_19_4_RC3(3336, 19, 4, "RC3"),
    JAVA_1_19_4(3337, 19, 4),
    JAVA_1_20_23W12A(3442, 20, 0, "23w12a"),
    JAVA_1_20_23W13A(3443, 20, 0, "23w13a"),
    JAVA_1_20_23W14A(3445, 20, 0, "23w14a"),
    JAVA_1_20_23W16A(3449, 20, 0, "23w16a"),
    JAVA_1_20_23W17A(3452, 20, 0, "23w17a"),
    JAVA_1_20_23W18A(3453, 20, 0, "23w18a"),
    JAVA_1_20_PRE1(3454, 20, 0, "PRE1"),
    JAVA_1_20_PRE2(3455, 20, 0, "PRE2"),
    JAVA_1_20_PRE3(3456, 20, 0, "PRE3"),
    JAVA_1_20_PRE4(3457, 20, 0, "PRE4"),
    JAVA_1_20_PRE5(3458, 20, 0, "PRE5"),
    JAVA_1_20_PRE6(3460, 20, 0, "PRE6"),
    JAVA_1_20_PRE7(3461, 20, 0, "PRE7"),
    JAVA_1_20_RC1(3462, 20, 0, "RC1"),
    JAVA_1_20_0(3463, 20, 0),
    JAVA_1_20_1_RC1(3464, 20, 1, "RC1"),
    JAVA_1_20_1(3465, 20, 1),
    JAVA_1_20_2_23W31A(3567, 20, 2, "23w31a"),
    JAVA_1_20_2_23W32A(3569, 20, 2, "23w32a"),
    JAVA_1_20_2_23W33A(3570, 20, 2, "23w33a"),
    JAVA_1_20_2_23W35A(3571, 20, 2, "23w35a"),
    JAVA_1_20_2_PRE1(3572, 20, 2, "PRE1"),
    JAVA_1_20_2_PRE2(3573, 20, 2, "PRE2"),
    JAVA_1_20_2_PRE3(3574, 20, 2, "PRE3"),
    JAVA_1_20_2_PRE4(3575, 20, 2, "PRE4"),
    JAVA_1_20_2_RC1(3576, 20, 2, "RC1"),
    JAVA_1_20_2_RC2(3577, 20, 2, "RC2"),
    JAVA_1_20_2(3578, 20, 2),  // fully complete list up to here
    JAVA_1_20_3(3698, 20, 3),
    JAVA_1_20_4(3700, 20, 4);

    private static final int[] ids;
    private static final DataVersion latestFullReleaseVersion;
    private final int id;
    private final int minor;
    private final int patch;
    private final boolean isFullRelease;
    private final String buildDescription;
    private final String str;

    static {
        ids = Arrays.stream(values()).sorted().mapToInt(DataVersion::id).toArray();
        latestFullReleaseVersion = Arrays.stream(values())
                .sorted(Comparator.reverseOrder())
                .filter(DataVersion::isFullRelease)
                .findFirst().get();
    }

    DataVersion(int id, int minor, int patch) {
        this(id, minor, patch, null);
    }

    /**
     * @param id data version
     * @param minor minor version
     * @param patch patch number, LT0 to indicate this data version is not a full release version
     * @param buildDescription Suggested convention (unit test enforced): <ul>
     *                         <li>NULL (given value ignored) for full release</li>
     *                         <li>CT# for combat tests (e.g. CT6, CT6b)</li>
     *                         <li>XS# for experimental snapshots(e.g. XS1, XS2)</li>
     *                         <li>YYwWWz for weekly builds (e.g. 21w37a, 21w37b)</li>
     *                         <li>PRE# for pre-releases (e.g. PRE1, PRE2)</li>
     *                         <li>RC# for release candidates (e.g. RC1, RC2)</li></ul>
     */
    DataVersion(int id, int minor, int patch, String buildDescription) {
        this.isFullRelease = buildDescription == null || "FINAL".equalsIgnoreCase(buildDescription);
        if (!isFullRelease && buildDescription.isEmpty())
            throw new IllegalArgumentException("buildDescription required for non-full releases");
        this.id = id;
        this.minor = minor;
        this.patch = patch;
        this.buildDescription = isFullRelease ? "FINAL" : buildDescription;
        if (minor > 0) {
            StringBuilder sb = new StringBuilder();
            sb.append(id).append(" (1.").append(minor);
            if (patch > 0) sb.append('.').append(patch);
            if (!isFullRelease) sb.append(' ').append(buildDescription);
            this.str = sb.append(')').toString();
        } else {
            this.str = name();
        }
    }

    public int id() {
        return id;
    }

    /**
     * Version format: major.minor.patch
     */
    public int major() {
        return 1;
    }

    /**
     * Version format: major.minor.patch
     */
    public int minor() {
        return minor;
    }

    /**
     * Version format: major.minor.patch
     */
    public int patch() {
        return patch;
    }

    /**
     * True for full release.
     * False for all other builds (e.g. experimental, pre-releases, and release-candidates).
     */
    public boolean isFullRelease() {
        return isFullRelease;
    }

    /**
     * Description of the minecraft build which this {@link DataVersion} refers to.
     * You'll find {@link #toString()} to be more useful in general.
     * <p>Convention used: <ul>
     * <li>"FULL" for full release</li>
     * <li>YYwWWz for weekly builds (e.g. 21w37a, 21w37b)</li>
     * <li>CT# for combat tests (e.g. CT6, CT6b)</li>
     * <li>XS# for experimental snapshots(e.g. XS1, XS2)</li>
     * <li>PR# for pre-releases (e.g. PR1, PR2)</li>
     * <li>RC# for release candidates (e.g. RC1, RC2)</li></ul>
     */
    public String getBuildDescription() {
        return buildDescription;
    }

    /**
     * TRUE as of 1.14
     * Indicates if point of interest .mca files exist. E.g. 'poi/r.0.0.mca'
     */
    public boolean hasPoiMca() {
        return minor >= 14;
    }

    /**
     * TRUE as of 1.17
     * Entities were pulled out of terrain 'region/r.X.Z.mca' files into their own .mca files. E.g. 'entities/r.0.0.mca'
     */
    public boolean hasEntitiesMca() {
        return minor >= 17;
    }

    public static DataVersion bestFor(int dataVersion) {
        int found = Arrays.binarySearch(ids, dataVersion);
        if (found < 0) {
            found = (found + 2) * -1;
            if (found < 0) return UNKNOWN;
        }
        return values()[found];
    }

    /**
     * @return The latest full release version defined.
     */
    public static DataVersion latest() {
        return latestFullReleaseVersion;
    }

    @Override
    public String toString() {
        return str;
    }

    /**
     * Indicates if this version would be crossed by the transition between versionA and versionB.
     * This is useful for determining if a data upgrade or downgrade would be required to support
     * changing from versionA to versionB. The order of A and B don't matter.
     *
     * <p>When using this function, call it on the data version in which a change exists. For
     * example if you need to know if changing from A to B would require changing to/from 3D
     * biomes then use {@code JAVA_1_15_19W36A.isCrossedByTransition(A, B)} as
     * {@link #JAVA_1_15_19W36A} is the version which added 3D biomes.</p>
     *
     * <p>In short, if this function returns true then the act of changing data versions from A
     * to B can be said to "cross" this version which is an indication that such a change should
     * either be considered illegal or that upgrade/downgrade action is required.</p>
     *
     * @param versionA older or newer data version than B
     * @param versionB older or newer data version than A
     * @return true if chaining from version A to version B, or form B to A, would result in
     * crossing this version. This version is considered to be crossed if {@code A != B} and
     * {@code min(A, B) < this.id <= max(A, B)}
     * @see #throwUnsupportedVersionChangeIfCrossed(int, int)
     */
    public boolean isCrossedByTransition(int versionA, int versionB) {
        if (versionA == versionB) return false;
        if (versionA < versionB) {
            return versionA < id && id <= versionB;
        } else {
            return versionB < id && id <= versionA;
        }
    }

    /**
     * Throws {@link UnsupportedVersionChangeException} if {@link #isCrossedByTransition(int, int)}
     * were to return true for the given arguments.
     */
    public void throwUnsupportedVersionChangeIfCrossed(int versionA, int versionB) {
        if (isCrossedByTransition(versionA, versionB)) {
            throw new UnsupportedVersionChangeException(this, versionA, versionB);
        }
    }
}
