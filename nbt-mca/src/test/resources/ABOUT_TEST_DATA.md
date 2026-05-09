# MCA Palette Samples

### 1.20.4
* **biomes-1.20.4-r.0.0_X21Y-3Z3_6entries.snbt** -
  interesting because it highlights the need to compute bits per index from palette size and not from the
  number of longs - computing from number of longs gives the wrong answer because this sample overflows
  3 longs by as single record.
* **block_states-1.20.4-6entries.snbt** -
  interesting because only 3 bits are required to encode size 6 palette data, but it actually uses 4 bits.
* **block_states-1.20.4-14entries.snbt** -
  interesting because bit packing has no waste on the per-long level, 4 bits to encode size 14 palette data.
* **block_states-1.20.4-r.0.0_X6Y-3Z23_72entries.snbt** -
  interesting because it's the largest chunk section found in a random world's region file having a 
  bits per index of 7.

# Chunk Mover Samples

### single_start_record_with_all_xz_fields
A hand curated structure start (single record) that is defined for two chunk locations so one can be read,
moved to the location of the other, and validated with a simple equality check.

The POST_MOVE_CLIPPED file is the result of moving to chunk 0 0 which clips the structure on the r.0.0 region boundary.

The POST_MOVE_NOCLIP file is the result of moving to chunk 0 0 without a clipping region defined.

# Region Files

## Older versions

### 1.9.4
has pigs in it, nothing special
Chunks:
- 88 -20

### 1.12.2
has sheep about and various villager shoved in a 2x2 hole

Chunks:
- 10 11

### 1.13.0 / r.0.0.mca
Chunk has villager, chicken, turtle eggs, bed, and multiple biomes - uhh no it doesn't! no entities, just a mineshaft reference.

Chunks:
- 6 10
 
### 1.13.1 / r.2.2.mca
OG test region file used for most initial development / testing. Really, these are boring chunks and not good for much except the basics.

Contains 3 chunks - all have "UpgradeData" tag with some "Indices" populated

### 1.13.2
has chickens, horses, and villagers

Chunks:
- -42 -45

### 1.14.4
has villagers, lecturn, bell, bed. The librarian is bound to the lecturn in the chunk 

Chunks:
- -1 16

### 1.15.2 / r.0.0.mca
OG test region file used exclusively for biome testing in MCAFileTest::test1_15GetBiomeAt

### 1.15.2 / r.-1.0.mca
has a horse, villagers, lecturn, bell, bed (and half a bed).

Chunks:
- -3 11 - has a reference to a village that resides in another region file in chunk 1 12


### 1.16.5
has an iron golumn, villagers, fletching table, bell
berry bushes

Chunks:
- 4 -27

## 1.17.1 (DV 2730)

### 1_17_1 / r.-3.2.mca (seed: -592955240269541309)
Village, with villager with POI of cartography table and bed as well as nether portal

Chunks:
- -65 -42

## 1.18
Vanilla world height from Y -64 to 320

Region chunk NBT data is not wrapped in the "Level" tag.

### 1_18_PRE1
villager, lush caves, beds, bell, workstations, chickens, cat, geode

Chunks:
- -60 -69

### 1_18_1 / r.0.-2.mca
villager, cat, iron golem, bell

Chunks:
- 19 -47

### 1_18_1 / r.8.1.mca
lush caves, loot minecart

Chunks:
- 275 33

### 1_20_4
### r.-3.-3.mca
pillager outpost and a section of a mineshaft - world seed: -4846182428012336372L

Chunks:
- pillager outpost
  - XZ(-94, -85)
  - XZ(-94, -86)
  - XZ(-95, -85)
  - XZ(-95, -86)
- mineshaft "starts" data - runs under pillager outpost chunks
  - XZ(-91, -87)

Note that the POI file is the complete file for the chunk and contains various beehives scattered about through 6 chunks.
Poi chunks: -77 -84; -77 -73; -94 -71; -78 -70; -77 -68; -82 -67 

### 1_20_4/entities/double_passengers
Has a baby zombie ridding a pillager riding a ravager and a few other mobs in the chunk.