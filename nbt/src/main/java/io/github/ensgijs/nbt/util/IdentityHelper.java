package io.github.ensgijs.nbt.util;

/**
 * Use this in sets/map-keys when you need to track which objects you have visited
 * which may evaluate {@code Objects.equals(..) == true}
 *
 * <p>Created to track visitation of values attached to nodes in a graph where those values may or may not be
 * identical and may or may not be the same instance - but you need to visit each instance only once in
 * either case, or in mixed cases.
 *
 * <p>Example use case
 * <pre>{@code
 *         Set<Object> set = new HashSet<>();
 *         String s1 = new String("net.Foo");
 *         String s2 = new String("net.Foo");
 *
 *         set.add(new IdentityHelper<>(s1));
 *         set.add(new IdentityHelper<>(s2));
 *         set.add(new IdentityHelper<>(s2));
 *         Assert.assertEquals(2, set.size());
 * }</pre>
 */
public class IdentityHelper <T> {
    private final T value;

    public T getValue() {
        return value;
    }

    public IdentityHelper(T value) {
        this.value = value;
    }

    @Override
    public int hashCode() {
        return System.identityHashCode(value);
    }

    @Override
    public boolean equals(Object o) {
        if (o instanceof IdentityHelper<?>) {
            return value == ((IdentityHelper<?>)o).value;
        }
        return value == o;
    }

    public static <T> IdentityHelper<T> of(T o) {
        return new IdentityHelper<>(o);
    }
}
