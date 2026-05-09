package io.github.ensgijs.nbt.mca;

import io.github.ensgijs.nbt.tag.CompoundTag;
import junit.framework.TestCase;

public class PoiRecordTest extends TestCase {
    public static CompoundTag makeTag(int tickets, String type, int x, int y, int z) {
        CompoundTag tag = new CompoundTag();
        tag.putString("type", type);
        tag.putIntArray("pos", new int[] {x, y, z});
        tag.putInt("free_tickets", tickets);
        return tag;
    }

    public void testConstructor_CompoundTag() {
        PoiRecord record = new PoiRecord(makeTag(3, "minecraft:test", 7, -42, 1777));
        assertEquals(3, record.getFreeTickets());
        assertEquals("minecraft:test", record.getType());
        assertEquals(7, record.getX());
        assertEquals(-42, record.getY());
        assertEquals(1777, record.getZ());
    }

    public void testCopyConstructor() {
        CompoundTag tag = makeTag(3, "minecraft:test", 1_234_679, 315, -8_546_776);
        PoiRecord recordA = new PoiRecord(tag);
        PoiRecord recordB = new PoiRecord(recordA);
        assertEquals(recordA, recordB);
        assertEquals(recordA.getFreeTickets(), recordB.getFreeTickets());
        assertEquals(recordA.getHandle(), recordB.getHandle());
        assertNotSame(recordA.getHandle(), recordB.getHandle());
    }

    public void testUpdateHandle() {
        CompoundTag tag = makeTag(0, "minecraft:test", 1_234_679, 315, -8_546_776);
        PoiRecord record = new PoiRecord(tag);
        assertEquals(tag, record.updateHandle());
        // if impl changes to hold onto tag this test will need to be updated, this is here to catch that
        assertNotSame(tag, record.updateHandle());
    }

    public void testGetHandle() {
        CompoundTag tag = makeTag(0, "minecraft:test", 1_234_679, 315, -8_546_776);
        PoiRecord record = new PoiRecord(tag);
        assertEquals(tag, record.getHandle());
        // if impl changes to hold onto tag this test will need to be updated, this is here to catch that
        assertNotSame(tag, record.getHandle());
    }

    public void testGetSectionY() {
        PoiRecord record = new PoiRecord();
        record.setY(77);
        assertEquals(4, record.getSectionY());
        record.setY(-38);
        assertEquals(-3, record.getSectionY());
    }

    public void testEquals() {
        PoiRecord record1 = new PoiRecord(1, 2, 3, "foo", 5);
        PoiRecord record2 = new PoiRecord(1, 2, 3, "foo", 5);
        assertEquals(record1, record2);
        record1.setX(0);
        assertFalse(record1.equals(record2));
        record1.setX(1);
        record1.setY(0);
        assertFalse(record1.equals(record2));
        record1.setY(2);
        record1.setZ(0);
        assertFalse(record1.equals(record2));
        record1.setZ(3);
        record1.setFreeTickets(0);
        assertTrue(record1.equals(record2));  // free tickets should not be considered in equals
        record1.setFreeTickets(5);
        record1.setType("bar");
        assertFalse(record1.equals(record2));
        record1.setType("foo");
        assertEquals(record1, record2);  // make sure there wasn't a fubar in the test chain
    }

    public void testHashCode_ignoresFreeTickets() {
        PoiRecord record1 = new PoiRecord(1, 2, 3, "foo", 5);
        PoiRecord record2 = new PoiRecord(1, 2, 3, "foo", 7);
        assertEquals(record1.hashCode(), record2.hashCode());
    }
}
