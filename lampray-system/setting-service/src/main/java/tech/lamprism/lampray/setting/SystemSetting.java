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

package tech.lamprism.lampray.setting;

import space.lingu.NonNull;
import tech.lamprism.lampray.DataEntity;
import tech.lamprism.lampray.EntityBuilder;
import tech.rollw.common.web.system.SystemResourceKind;

import java.time.OffsetDateTime;
import java.util.Objects;

/**
 * @author RollW
 */
@SuppressWarnings("all")
public class SystemSetting implements DataEntity<Long> {
    private final Long id;
    private final String key;
    private final String value;

    public SystemSetting(Long id, String key, String value) {
        this.id = id;
        this.key = key;
        this.value = value;
    }

    public String getKey() {
        return key;
    }

    public String getValue() {
        return value;
    }

    @Override
    public Long getEntityId() {
        return id;
    }

    @NonNull
    @Override
    public OffsetDateTime getCreateTime() {
        return NONE_TIME;
    }

    @NonNull
    @Override
    public OffsetDateTime getUpdateTime() {
        return NONE_TIME;
    }

    public Builder toBuilder() {
        return new Builder(this);
    }

    @Override
    public SystemResourceKind getSystemResourceKind() {
        return SystemSettingResourceKind.INSTANCE;
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, key, value);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof SystemSetting)) return false;
        SystemSetting that = (SystemSetting) o;
        return Objects.equals(id, that.id) && Objects.equals(key, that.key) && Objects.equals(value, that.value);
    }

    @Override
    public String toString() {
        return "SystemSetting{" +
                "id=" + id +
                ", key='" + key + '\'' +
                ", value='" + value + '\''
                + '}';
    }

    public static final class Builder implements EntityBuilder<SystemSetting, Long> {
        private Long id;
        private String key;
        private String value;

        public Builder() {
        }

        public Builder(SystemSetting systemSetting) {
            this.id = systemSetting.getEntityId();
            this.key = systemSetting.getKey();
            this.value = systemSetting.getValue();
        }

        public Builder setKey(String key) {
            this.key = key;
            return this;
        }

        public Builder setValue(String value) {
            this.value = value;
            return this;
        }

        @Override
        public SystemSetting build() {
            return new SystemSetting(id, key, value);
        }

        @Override
        public Builder setEntityId(Long id) {
            this.id = id;
            return this;
        }
    }
}
