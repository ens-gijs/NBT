package io.github.ensgijs.nbt.mca.entities;

import io.github.ensgijs.nbt.mca.DataVersion;
import io.github.ensgijs.nbt.mca.McaTestCase;
import io.github.ensgijs.nbt.tag.CompoundTag;
import io.github.ensgijs.nbt.tag.ListTag;

import java.util.ArrayList;
import java.util.List;

public class EntityFactoryTest extends McaTestCase {

    // <editor-fold desc="Test Stubs" defaultstate="collapsed">
    private static class EntityStub extends EntityBase {
        final String givenNormalizedId;
        final EntityCreatorStub creator;

        public EntityStub(EntityCreatorStub creator, String givenNormalizedId, CompoundTag data, int dataVersion) {
            super(data, dataVersion);
            this.creator = creator;
            this.givenNormalizedId = givenNormalizedId;
            this.dataVersion = dataVersion;
        }

        public EntityStub(String id, int dataVersion) {
            super(dataVersion, id, 0, 0, 0);
            this.creator = null;
            this.givenNormalizedId = null;
        }
    }

    private static class EntityCreatorStub implements EntityCreator<EntityStub> {
        final String name;
        boolean returnNull;
        boolean violateIdContract;
        RuntimeException throwMe;
        public EntityCreatorStub(String name) {
            this.name = name;
        }

        @Override
        public EntityStub create(String normalizedId, CompoundTag tag, int dataVersion) {
            if (throwMe != null) throw throwMe;
            if (returnNull) return null;
            EntityStub entity = new EntityStub(this, normalizedId, tag, dataVersion);
            if (violateIdContract) {
                entity.id = null;
            }
            return entity;
        }

        @Override
        public String toString() {
            return name;
        }
    }
    // </editor-fold>

    final EntityCreatorStub defaultedCreator = new EntityCreatorStub("TEST DEFAULT CREATOR");

    @Override
    public void setUp() throws Exception {
        super.setUp();
        EntityFactory.clearCreators();
        EntityFactory.clearEntityIdRemap();
        EntityFactory.setDefaultEntityCreator(defaultedCreator);
    }

    @Override
    public void tearDown() throws Exception {
        super.tearDown();
        EntityFactory.clearCreators();
        EntityFactory.resetEntityIdRemap();
    }

    public void testNormalizeId() {
        assertThrowsIllegalArgumentException(() -> EntityFactory.normalizeId(null));
        assertThrowsNoException(() -> EntityFactory.normalizeId("not_null"));
        assertThrowsIllegalArgumentException(() -> EntityFactory.normalizeId("minecraft:"));

        assertEquals("PIG_ZIG", EntityFactory.normalizeId("minecraft:Pig_zig"));
        assertEquals("PIGZIG", EntityFactory.normalizeId("minecraft:PigZig"));
    }

    public void testNormalizeAndRemapId() {
        assertThrowsIllegalArgumentException(() -> EntityFactory.normalizeAndRemapId(null));
        assertThrowsNoException(() -> EntityFactory.normalizeAndRemapId("not_null"));
        assertThrowsIllegalArgumentException(() -> EntityFactory.normalizeAndRemapId("minecraft:"));
        // not mapped
        assertEquals("PIGZIG", EntityFactory.normalizeAndRemapId("minecraft:PigZig"));
        // remap
        EntityFactory.registerIdRemap("pigzig", "pig_zig");
        assertEquals("PIG_ZIG", EntityFactory.normalizeAndRemapId("minecraft:PigZig"));
    }

    public void testDefaultCreatorNotNull() {
        assertNotNull(EntityFactory.getDefaultEntityCreator());
    }

    public void testSetDefaultCreator() {
        assertNotNull(EntityFactory.getDefaultEntityCreator());
        assertThrowsIllegalArgumentException(() -> EntityFactory.setDefaultEntityCreator(null));
        EntityCreator<?> ec = new EntityCreatorStub(getName());
        EntityFactory.setDefaultEntityCreator(ec);
        assertSame(ec, EntityFactory.getDefaultEntityCreator());
    }

    public void testRegisterCreator_basic() {
        EntityCreator<?> ec = new EntityCreatorStub(getName());
        assertNull(EntityFactory.getCreatorById("FOO"));
        EntityFactory.registerCreator(ec, "foo");
        assertSame(ec, EntityFactory.getCreatorById("FOO"));
        assertNull(EntityFactory.getCreatorById("BAR"));
    }

    public void testRegisterCreator_multiple() {
        EntityCreator<?> ec = new EntityCreatorStub(getName());
        EntityFactory.registerCreator(ec, "foo", "minecraft:bar");
        assertSame(ec, EntityFactory.getCreatorById("FOO"));
        assertSame(ec, EntityFactory.getCreatorById("BAR"));
        assertNull(EntityFactory.getCreatorById("BAZ"));
    }

    public void testRegisterCreator_throwsOnNullId() {
        EntityCreator<?> ec = new EntityCreatorStub(getName());
        assertThrowsIllegalArgumentException(() -> EntityFactory.registerCreator(ec, (String) null));
        assertThrowsIllegalArgumentException(() -> EntityFactory.registerCreator(ec, "foo", null, "minecraft:bar"));
    }

    public void testRegisterCreator_afterRegisteringRemapping() {
        EntityCreator<?> fooEc = new EntityCreatorStub(getName());
        EntityFactory.registerIdRemap("bar", "foo");
        EntityFactory.registerIdRemap("baz", "foo");
        EntityFactory.registerCreator(fooEc, "foo");
        assertSame(fooEc, EntityFactory.getCreatorById("FOO"));
        assertSame(fooEc, EntityFactory.getCreatorById("BAR"));
        assertSame(fooEc, EntityFactory.getCreatorById("BAZ"));
        assertNull(EntityFactory.getCreatorById("ZAP"));
    }

    public void testRegisteringRemapping_afterRegisterCreator() {
        EntityCreator<?> fooEc = new EntityCreatorStub(getName());
        EntityFactory.registerCreator(fooEc, "foo");
        EntityFactory.registerIdRemap("bar", "foo");
        EntityFactory.registerIdRemap("baz", "foo");
        assertSame(fooEc, EntityFactory.getCreatorById("FOO"));
        assertSame(fooEc, EntityFactory.getCreatorById("BAR"));
        assertSame(fooEc, EntityFactory.getCreatorById("BAZ"));
        assertNull(EntityFactory.getCreatorById("ZAP"));
    }

    public void testReverseIdRemap() {
        List<String> rev = EntityFactory.reverseIdRemap("foo");
        assertNotNull(rev);
        assertTrue(rev.isEmpty());

        EntityFactory.registerIdRemap("bar", "foo");

        // this isn't a reverse lookup, it would be forward
        rev = EntityFactory.reverseIdRemap("bar");
        assertNotNull(rev);
        assertTrue(rev.isEmpty());

        // one reverse match
        rev = EntityFactory.reverseIdRemap("foo");
        assertNotNull(rev);
        assertEquals(1, rev.size());


        EntityFactory.registerIdRemap("oof", "foo");
        rev = EntityFactory.reverseIdRemap("foo");
        assertNotNull(rev);
        assertEquals(2, rev.size());
    }

    public void testGetRegisteredCreatorIdKeys() {
        assertTrue(EntityFactory.getRegisteredCreatorIdKeys().isEmpty());
        EntityFactory.registerCreator(new EntityCreatorStub("A"), "FOO", "BAR");
        EntityFactory.registerCreator(new EntityCreatorStub("B"), "ZOO");
        assertEquals(3, EntityFactory.getRegisteredCreatorIdKeys().size());
        assertTrue(EntityFactory.getRegisteredCreatorIdKeys().contains("FOO"));
        assertTrue(EntityFactory.getRegisteredCreatorIdKeys().contains("BAR"));
        assertTrue(EntityFactory.getRegisteredCreatorIdKeys().contains("ZOO"));
    }

    public void testCreate() {
        CompoundTag tag = new CompoundTag();
        tag.putString("id", "whatever");
        EntityStub entityStub = EntityFactory.createAutoCast(tag, DataVersion.latest().id());
        assertNotNull(entityStub);
        assertSame(defaultedCreator, entityStub.creator);
        assertEquals("WHATEVER", entityStub.givenNormalizedId);
        assertSame(tag, entityStub.getHandle());

        EntityCreatorStub ec = new EntityCreatorStub(getName());
        EntityFactory.registerIdRemap("Muggle", "non_wizard");
        EntityFactory.registerCreator(ec, "non_wizard");

        // check that the default is still used
        entityStub = EntityFactory.createAutoCast(tag, DataVersion.latest().id());
        assertNotNull(entityStub);
        assertSame(defaultedCreator, entityStub.creator);
        assertEquals("WHATEVER", entityStub.givenNormalizedId);
        assertSame(tag, entityStub.getHandle());

        // check behavior with use of preferred name
        tag = new CompoundTag();
        tag.putString("id", "non_wizard");
        entityStub = EntityFactory.createAutoCast(tag, DataVersion.latest().id());
        assertNotNull(entityStub);
        assertSame(ec, entityStub.creator);
        assertEquals("NON_WIZARD", entityStub.givenNormalizedId);
        assertSame(tag, entityStub.getHandle());

        // check behavior with use of legacy name
        tag = new CompoundTag();
        tag.putString("id", "Muggle");
        entityStub = EntityFactory.createAutoCast(tag, DataVersion.latest().id());
        assertNotNull(entityStub);
        assertSame(ec, entityStub.creator);
        assertEquals("NON_WIZARD", entityStub.givenNormalizedId);
        assertSame(tag, entityStub.getHandle());
    }

    public void testToListTag() {
        List<EntityStub> entities = new ArrayList<>();
        entities.add(new EntityStub("frank", DataVersion.latest().id()));
        entities.add(new EntityStub("sam", DataVersion.latest().id()));
        ListTag<CompoundTag> eTag = EntityFactory.toListTag(entities);
        assertEquals(2, eTag.size());
        assertEquals("frank", eTag.get(0).getString("id"));

        entities.clear();
        EntityStub bubba = new EntityStub("bubba", DataVersion.latest().id());
        entities.add(bubba);
        assertSame(eTag, EntityFactory.toListTag(entities, eTag));
        assertEquals(1, eTag.size());
        assertEquals("bubba", eTag.get(0).getString("id"));

        entities.add(bubba);
        assertThrowsIllegalArgumentException(() -> EntityFactory.toListTag(entities));
    }

    public void testCreate_throwsIllegalEntityTagException_whenCreatorReturnsNull() {
        CompoundTag tag = new CompoundTag();
        tag.putString("id", "whatever");
        defaultedCreator.returnNull = true;
        assertThrowsException(() -> EntityFactory.createAutoCast(tag, DataVersion.latest().id()),
                IllegalEntityTagException.class);
    }

    public void testCreate_throwsIllegalEntityTagException_whenCreatorThrows() {
        CompoundTag tag = new CompoundTag();
        tag.putString("id", "whatever");
        defaultedCreator.throwMe = new NullPointerException();
        try {
            EntityFactory.createAutoCast(tag, DataVersion.latest().id());
            fail();
        } catch (IllegalEntityTagException ex) {
            assertSame(tag, ex.getTag());
        }
    }

    public void testCreate_throwsIllegalStateException_whenCreatorReturnsEntityWithoutId() {
        CompoundTag tag = new CompoundTag();
        tag.putString("id", "whatever");
        defaultedCreator.violateIdContract = true;
        assertThrowsException(() -> EntityFactory.createAutoCast(tag, DataVersion.latest().id()),
                IllegalStateException.class);
    }
}
