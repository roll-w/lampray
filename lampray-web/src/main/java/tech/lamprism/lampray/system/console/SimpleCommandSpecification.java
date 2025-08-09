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

import org.apache.commons.lang3.ObjectUtils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;

/**
 * @author RollW
 */
public class SimpleCommandSpecification implements CommandSpecification {
    private final List<String> names;
    private final String description;
    private final String header;
    private final String group;
    private final List<String> aliases;
    private final boolean hidden;
    private final List<Option> options;

    public SimpleCommandSpecification(List<String> names, String description,
                                      String header, String group,
                                      List<String> aliases,
                                      boolean hidden,
                                      List<Option> options) {
        this.names = Objects.requireNonNull(names, "names cannot be null");
        this.description = description;
        this.header = header;
        this.group = group;
        this.aliases = aliases;
        this.hidden = hidden;
        this.options = ObjectUtils.defaultIfNull(options, new ArrayList<>());
    }


    @Override
    public String getName() {
        return names.get(names.size() - 1);
    }

    @Override
    public String getDescription() {
        return description;
    }

    @Override
    public String getHeader() {
        return header;
    }

    @Override
    public String getGroup() {
        return group;
    }

    @Override
    public List<String> getAliases() {
        return aliases;
    }

    @Override
    public String getFullName() {
        return String.join(" ", names);
    }

    @Override
    public boolean isHidden() {
        return hidden;
    }

    @Override
    public List<Option> getOptions() {
        return options;
    }

    public Builder toBuilder() {
        return new Builder(this);
    }

    public static Builder builder() {
        return new Builder();
    }

    @Override
    public boolean equals(Object o) {
        if (!(o instanceof SimpleCommandSpecification that)) return false;
        return hidden == that.hidden && Objects.equals(names, that.names) && Objects.equals(description, that.description) && Objects.equals(header, that.header) && Objects.equals(group, that.group) && Objects.equals(aliases, that.aliases) && Objects.equals(options, that.options);
    }

    @Override
    public int hashCode() {
        return Objects.hash(names, description, header, group, aliases, hidden, options);
    }

    @Override
    public String toString() {
        return "SimpleCommandSpecification{" +
                "names=" + names +
                ", description='" + description + '\'' +
                ", header='" + header + '\'' +
                ", group='" + group + '\'' +
                ", aliases=" + aliases +
                ", hidden=" + hidden +
                ", options=" + options +
                '}';
    }

    public static final class Builder {
        private List<String> names;
        private String description;
        private String header;
        private String group;
        private List<String> aliases;
        private boolean hidden;
        private List<Option> options;

        public Builder() {
        }

        public Builder(SimpleCommandSpecification other) {
            this.names = other.names;
            this.description = other.description;
            this.header = other.header;
            this.group = other.group;
            this.aliases = other.aliases;
            this.hidden = other.hidden;
            this.options = other.options;
        }

        public Builder setNames(List<String> names) {
            this.names = names;
            return this;
        }

        public Builder setNames(String... names) {
            this.names = Arrays.asList(names);
            return this;
        }

        public Builder setName(String name) {
            this.names = Arrays.asList(name.split(" "));
            return this;
        }

        public Builder setDescription(String description) {
            this.description = description;
            return this;
        }

        public Builder setHeader(String header) {
            this.header = header;
            return this;
        }

        public Builder setGroup(String group) {
            this.group = group;
            return this;
        }

        public Builder setAliases(List<String> aliases) {
            this.aliases = aliases;
            return this;
        }

        public Builder setHidden(boolean hidden) {
            this.hidden = hidden;
            return this;
        }

        public Builder setOptions(List<Option> options) {
            this.options = new ArrayList<>(options);
            return this;
        }

        public Builder addOption(Option option) {
            if (this.options == null) {
                this.options = new ArrayList<>();
            }
            this.options.add(option);
            return this;
        }

        public SimpleCommandSpecification build() {
            return new SimpleCommandSpecification(names, description,
                    header, group, aliases, hidden, options);
        }
    }
}
