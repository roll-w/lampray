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

package tech.lamprism.lampray.system.console;

import java.util.Objects;
import java.util.Set;

/**
 * @author RollW
 */
public class SimpleCommandOption implements CommandSpecification.Option {
    private final Set<String> names;
    private final String label;
    private final String description;
    private final String defaultValue;
    private final boolean required;
    private final boolean hidden;
    private final boolean global;
    private final Class<?> type;

    public SimpleCommandOption(Set<String> names, String label, String description,
                               String defaultValue, boolean required,
                               boolean hidden, boolean global, Class<?> type) {
        this.names = Objects.requireNonNull(names, "names cannot be null");
        this.label = label;
        this.description = description;
        this.defaultValue = defaultValue;
        this.required = required;
        this.hidden = hidden;
        this.global = global;
        this.type = type;
    }

    @Override
    public Set<String> getNames() {
        return names;
    }

    @Override
    public String getLabel() {
        return label;
    }

    @Override
    public String getDescription() {
        return description;
    }

    @Override
    public boolean isRequired() {
        return required;
    }

    @Override
    public boolean isHidden() {
        return hidden;
    }

    @Override
    public String getDefaultValue() {
        return defaultValue;
    }

    @Override
    public boolean isGlobal() {
        return global;
    }

    @Override
    public Class<?> getType() {
        return type;
    }

    public Builder toBuilder() {
        return new Builder(this);
    }

    public static Builder builder() {
        return new Builder();
    }

    @Override
    public boolean equals(Object o) {
        if (!(o instanceof SimpleCommandOption that)) return false;
        return required == that.required && hidden == that.hidden && global == that.global && Objects.equals(names, that.names) &&
                Objects.equals(label, that.label) && Objects.equals(description, that.description) &&
                Objects.equals(defaultValue, that.defaultValue) && Objects.equals(type, that.type);
    }

    @Override
    public int hashCode() {
        return Objects.hash(names, label, description, defaultValue, required, hidden, global, type);
    }

    @Override
    public String toString() {
        return "SimpleCommandOption{" +
                "names=" + names +
                ", label='" + label + '\'' +
                ", description='" + description + '\'' +
                ", defaultValue='" + defaultValue + '\'' +
                ", required=" + required +
                ", hidden=" + hidden +
                ", global=" + global +
                ", type=" + type +
                '}';
    }

    public static final class Builder {
        private Set<String> names;
        private String label;
        private String description;
        private String defaultValue;
        private boolean required;
        private boolean hidden;
        private boolean global;
        private Class<?> type;// Null means no argument expected, e.g. a flag

        public Builder() {
        }

        public Builder(SimpleCommandOption other) {
            this.names = other.names;
            this.label = other.label;
            this.description = other.description;
            this.defaultValue = other.defaultValue;
            this.required = other.required;
            this.hidden = other.hidden;
            this.global = other.global;
        }

        public static Builder aSimpleCommandOption() {
            return new Builder();
        }

        public Builder setNames(Set<String> names) {
            this.names = names;
            return this;
        }

        public Builder setNames(String... names) {
            this.names = Set.of(names);
            return this;
        }

        public Builder setLabel(String label) {
            this.label = label;
            return this;
        }

        public Builder setDescription(String description) {
            this.description = description;
            return this;
        }

        public Builder setDefaultValue(String defaultValue) {
            this.defaultValue = defaultValue;
            return this;
        }

        public Builder setRequired(boolean required) {
            this.required = required;
            return this;
        }

        public Builder setHidden(boolean hidden) {
            this.hidden = hidden;
            return this;
        }

        public Builder setGlobal(boolean global) {
            this.global = global;
            return this;
        }

        public Builder setType(Class<?> type) {
            this.type = type;
            return this;
        }

        public SimpleCommandOption build() {
            return new SimpleCommandOption(names, label, description,
                    defaultValue, required, hidden, global, type);
        }
    }
}
