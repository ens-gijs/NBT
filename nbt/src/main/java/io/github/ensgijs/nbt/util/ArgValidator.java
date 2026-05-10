package io.github.ensgijs.nbt.util;

public class ArgValidator {
    private ArgValidator() { }

    public static <T> T requireValue(T value) {
        if (value == null) {
            throw new IllegalArgumentException();
        }
        return value;
    }

    public static String requireNotEmpty(String value) {
        if (value == null || value.isEmpty()) {
            throw new IllegalArgumentException();
        }
        return value;
    }

    public static <T> T requireValue(T value, String name) {
        if (value == null) {
            throw new IllegalArgumentException(name + " must not be null");
        }
        return value;
    }

    public static String requireNotEmpty(String value, String name) {
        if (value == null || value.isEmpty()) {
            throw new IllegalArgumentException(name + " must not be null or empty");
        }
        return value;
    }

    public static void check(boolean condition) {
        if (!condition) {
            throw new IllegalArgumentException();
        }
    }

    public static void check(boolean condition, String description) {
        if (!condition) {
            throw new IllegalArgumentException(description);
        }
    }


    public static <T> T check(T value, boolean condition) {
        if (!condition) {
            throw new IllegalArgumentException();
        }
        return value;
    }

    public static <T> T check(T value, boolean condition, String description) {
        if (!condition) {
            throw new IllegalArgumentException(description);
        }
        return value;
    }

    public static void checkState(boolean expression, Object errorMessage) {
        if (!expression) {
            throw new IllegalStateException(String.valueOf(errorMessage));
        }
    }
}
