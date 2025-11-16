/*
 * Copyright (C) 2023 RollW
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

package tech.lamprism.lampray.setting.event;

import org.springframework.context.ApplicationEvent;
import tech.lamprism.lampray.setting.SettingSpecification;

/**
 * @author RollW
 */
public class SettingValueChangedEvent<T> extends ApplicationEvent {
    private final SettingSpecification<T> specification;
    private final T value;

    public SettingValueChangedEvent(SettingSpecification<T> specification, T value) {
        super(specification);
        this.specification = specification;
        this.value = value;
    }

    public SettingSpecification<T> getSpecification() {
        return specification;
    }

    public T getValue() {
        return value;
    }
}
