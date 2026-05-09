package io.github.ensgijs.nbt.mca.entities;

import io.github.ensgijs.nbt.mca.DataVersion;
import io.github.ensgijs.nbt.mca.McaTestCase;
import io.github.ensgijs.nbt.tag.CompoundTag;

import java.util.UUID;

public class EntityUtilTest extends McaTestCase {

    public void testUuidSetGet() {
        final UUID uuid = UUID.fromString("7a10303f-dacf-4e35-a8be-1ce2818b0372");  // UUID.randomUUID();

        // test set and get uuid symmetry
        CompoundTag tag = new CompoundTag();
        EntityUtil.setUuid(DataVersion.JAVA_1_14_4.id(), tag, uuid);
        assertEquals(tag.toString(), uuid, EntityUtil.getUuid(DataVersion.JAVA_1_14_4.id(), tag));

        tag = new CompoundTag();
        EntityUtil.setUuid(DataVersion.JAVA_1_17_1.id(), tag, uuid);
        assertEquals(tag.toString(), uuid, EntityUtil.getUuid(DataVersion.JAVA_1_17_1.id(), tag));

        // test no uuid fields produces null
        tag = new CompoundTag();
        assertNull(EntityUtil.getUuid(DataVersion.JAVA_1_14_4.id(), tag));
        assertNull(EntityUtil.getUuid(DataVersion.JAVA_1_17_1.id(), tag));

        // test attempt to set zero uuid
        assertThrowsIllegalArgumentException(() ->
                EntityUtil.setUuid(DataVersion.JAVA_1_14_4.id(), new CompoundTag(), EntityUtil.ZERO_UUID));
        assertThrowsIllegalArgumentException(() ->
                EntityUtil.setUuid(DataVersion.JAVA_1_17_1.id(), new CompoundTag(), EntityUtil.ZERO_UUID));

        // test attempt to set null
        assertThrowsIllegalArgumentException(() ->
                EntityUtil.setUuid(DataVersion.JAVA_1_14_4.id(), null, EntityUtil.ZERO_UUID));
        assertThrowsIllegalArgumentException(() ->
                EntityUtil.setUuid(DataVersion.JAVA_1_17_1.id(), null, EntityUtil.ZERO_UUID));


        // test reading zero uuid produces null
        tag = new CompoundTag();
        tag.putLong("UUIDMost", 0);
        tag.putLong("UUIDLeast", 0);
        assertNull(EntityUtil.getUuid(DataVersion.JAVA_1_14_4.id(), tag));

        tag = new CompoundTag();
        tag.putIntArray("UUID", new int[] {0, 0, 0, 0});
        assertNull(EntityUtil.getUuid(DataVersion.JAVA_1_17_1.id(), tag));
    }

    public void testRemoveUuid() {
        final UUID uuid = UUID.fromString("7a10303f-dacf-4e35-a8be-1ce2818b0372");  // UUID.randomUUID();

        // test set and get uuid symmetry
        CompoundTag tag = new CompoundTag();
        EntityUtil.setUuid(DataVersion.JAVA_1_14_4.id(), tag, uuid);
        EntityUtil.setUuid(DataVersion.JAVA_1_17_1.id(), tag, uuid);
        assertTrue(tag.containsKey("UUIDMost"));
        assertTrue(tag.containsKey("UUIDLeast"));
        assertTrue(tag.containsKey("UUID"));

        EntityUtil.removeUuid(tag);
        assertFalse(tag.containsKey("UUIDMost"));
        assertFalse(tag.containsKey("UUIDLeast"));
        assertFalse(tag.containsKey("UUID"));
    }

    public void testNormalizeYaw_floats() {
        assertEquals(0f, EntityUtil.normalizeYaw(360f), 1e-4f);
        assertEquals(0f, EntityUtil.normalizeYaw(-360f), 1e-4f);
        assertEquals(270f, EntityUtil.normalizeYaw(-90f), 1e-4f);
        assertEquals(72.654f, EntityUtil.normalizeYaw(360f + 72.654f), 1e-4f);
        assertEquals(360f - 72.654f, EntityUtil.normalizeYaw(-72.654f), 1e-4f);
        assertEquals(30f, EntityUtil.normalizeYaw(-7 * 360f + 30f), 1e-4f);
        assertEquals(330f, EntityUtil.normalizeYaw(-7 * 360f - 30f), 1e-4f);
        assertTrue(Float.isNaN(EntityUtil.normalizeYaw(Float.NaN)));
    }

    public void testNormalizeYaw_doubles() {
        assertEquals(0f, EntityUtil.normalizeYaw(360d), 1e-4f);
        assertEquals(0f, EntityUtil.normalizeYaw(-360d), 1e-4f);
        assertEquals(270f, EntityUtil.normalizeYaw(-90d), 1e-4f);
        assertEquals(72.654f, EntityUtil.normalizeYaw(360d + 72.654d), 1e-4f);
        assertEquals(360f - 72.654f, EntityUtil.normalizeYaw(-72.654d), 1e-4f);
        assertEquals(30f, EntityUtil.normalizeYaw(-7d * 360d + 30d), 1e-4f);
        assertEquals(330f, EntityUtil.normalizeYaw(-7d * 360d - 30d), 1e-4f);
        assertTrue(Float.isNaN(EntityUtil.normalizeYaw(Double.NaN)));
    }

    public void testClampPitch() {
        assertEquals(-90f, EntityUtil.clampPitch(-1111f), 1e-4f);
        assertEquals(-90f, EntityUtil.clampPitch(-90.001f), 1e-4f);
        assertEquals(-89.999f, EntityUtil.clampPitch(-89.999f), 1e-4f);
        assertEquals(-0.001f, EntityUtil.clampPitch(-0.001f), 1e-4f);
        assertEquals(0f, EntityUtil.clampPitch(0f), 1e-4f);
        assertEquals(0.001f, EntityUtil.clampPitch(0.001f), 1e-4f);
        assertEquals(89.999f, EntityUtil.clampPitch(89.999f), 1e-4f);
        assertEquals(90f, EntityUtil.clampPitch(90.001f), 1e-4f);
        assertEquals(90f, EntityUtil.clampPitch(1111f), 1e-4f);
        assertTrue(Float.isNaN(EntityUtil.clampPitch(Float.NaN)));
    }
}
