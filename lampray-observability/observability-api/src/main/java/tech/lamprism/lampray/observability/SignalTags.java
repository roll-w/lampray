/*
 * Copyright (C) 2023-2026 RollW
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package tech.lamprism.lampray.observability;

import space.lingu.NonNull;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Objects;
import java.util.Set;

/**
 * @author RollW
 */
public final class SignalTags implements Iterable<SignalTag> {
    private static final SignalTags EMPTY = new SignalTags(List.of());

    private final List<SignalTag> tags;

    private SignalTags(List<SignalTag> tags) {
        this.tags = List.copyOf(tags);
        validateUniqueKeys(this.tags);
    }

    public static SignalTags empty() {
        return EMPTY;
    }

    public static SignalTags of(String key,
                                String value) {
        return new SignalTags(List.of(SignalTag.of(key, value)));
    }

    public static SignalTags of(String firstKey,
                                String firstValue,
                                String secondKey,
                                String secondValue) {
        return new SignalTags(List.of(
                SignalTag.of(firstKey, firstValue),
                SignalTag.of(secondKey, secondValue)
        ));
    }

    public static SignalTags of(List<SignalTag> tags) {
        Objects.requireNonNull(tags, "tags cannot be null");
        return tags.isEmpty() ? EMPTY : new SignalTags(tags);
    }

    public SignalTags and(String key,
                          String value) {
        ArrayList<SignalTag> updated = new ArrayList<>(tags.size() + 1);
        updated.addAll(tags);
        updated.add(SignalTag.of(key, value));
        return new SignalTags(updated);
    }

    public boolean isEmpty() {
        return tags.isEmpty();
    }

    public int size() {
        return tags.size();
    }

    public String getValue(String key) {
        String requiredKey = SpecificationSupport.requireText(key, "key");
        for (SignalTag tag : tags) {
            if (tag.getKey().equals(requiredKey)) {
                return tag.getValue();
            }
        }
        return null;
    }

    public List<SignalTag> asList() {
        return tags;
    }

    @NonNull
    @Override
    public Iterator<SignalTag> iterator() {
        return tags.iterator();
    }

    @Override
    public boolean equals(Object object) {
        if (this == object) {
            return true;
        }
        if (!(object instanceof SignalTags that)) {
            return false;
        }
        return tags.equals(that.tags);
    }

    @Override
    public int hashCode() {
        return tags.hashCode();
    }

    @Override
    public String toString() {
        return tags.toString();
    }

    private static void validateUniqueKeys(List<SignalTag> tags) {
        Set<String> keys = new HashSet<>();
        for (SignalTag tag : tags) {
            if (!keys.add(tag.getKey())) {
                throw new IllegalArgumentException("Duplicate tag key: " + tag.getKey());
            }
        }
    }
}
