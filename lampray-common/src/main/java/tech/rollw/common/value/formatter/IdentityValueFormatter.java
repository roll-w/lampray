/*
 * Copyright (C) 2023-2025 RollW
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

package tech.rollw.common.value.formatter;

/**
 * @author RollW
 */
public class IdentityValueFormatter<T> implements ValueFormatter<T, T> {
    private static final IdentityValueFormatter<?> INSTANCE = new IdentityValueFormatter<>();

    private IdentityValueFormatter() {
    }

    @Override
    public T format(T value) {
        return value;
    }

    @SuppressWarnings("unchecked")
    public static <T> IdentityValueFormatter<T> getInstance() {
        return (IdentityValueFormatter<T>) INSTANCE;
    }
}
