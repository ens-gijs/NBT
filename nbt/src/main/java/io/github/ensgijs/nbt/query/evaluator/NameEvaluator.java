package io.github.ensgijs.nbt.query.evaluator;

import io.github.ensgijs.nbt.tag.CompoundTag;
import io.github.ensgijs.nbt.tag.Tag;

public class NameEvaluator implements Evaluator {

    private final String key;

    public String key() {
        return key;
    }

    public NameEvaluator(String key) {
        this.key = key;
    }

    public Object eval(Tag<?> tag) {
        if (tag instanceof CompoundTag) {
            return ((CompoundTag) tag).get(key);
        }
        return null;
    }

    @Override
    public String toString() {
        return key;
    }
}
