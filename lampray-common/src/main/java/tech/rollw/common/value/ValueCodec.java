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

package tech.rollw.common.value;

import tech.rollw.common.value.formatter.ValueFormatter;
import tech.rollw.common.value.parser.ValueParser;

/**
 * @author RollW
 */
public interface ValueCodec<R, T> extends ValueParser<R, T>, ValueFormatter<T, R> {

    class SimpleValueCodec<R, T> implements ValueCodec<R, T> {
        private final ValueParser<R, T> parser;
        private final ValueFormatter<T, R> formatter;

        public SimpleValueCodec(ValueParser<R, T> parser,
                                ValueFormatter<T, R> formatter) {
            this.parser = parser;
            this.formatter = formatter;
        }

        @Override
        public T parse(R value) {
            return parser.parse(value);
        }

        @Override
        public R format(T value) {
            return formatter.format(value);
        }
    }

    static <R, T> ValueCodec<R, T> of(ValueParser<R, T> parser, ValueFormatter<T, R> formatter) {
        return new SimpleValueCodec<>(parser, formatter);
    }
}
