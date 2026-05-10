package io.github.ensgijs.nbt.query.evaluator;

import io.github.ensgijs.nbt.tag.Tag;

@FunctionalInterface
public interface Evaluator {
    Object eval(Tag<?> tag);
}
