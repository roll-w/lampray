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

import java.util.List;
import java.util.Set;

/**
 * Represents a command specification for the console system.
 * Provides metadata and configuration for a command.
 */
public interface CommandSpecification {
    /**
     * Gets the primary name of the command.
     */
    String getName();

    /**
     * Gets the description of the command.
     */
    String getDescription();

    /**
     * Gets the header information for the command.
     */
    String getHeader();

    /**
     * Gets the group to which the command belongs.
     */
    String getGroup();

    /**
     * Gets the list of aliases for the command.
     */
    List<String> getAliases();

    /**
     * Gets the full name of the command, including group or namespace if applicable.
     */
    String getFullName();

    /**
     * Indicates whether the command is hidden from help or listing.
     */
    boolean isHidden();

    /**
     * Gets the list of options available for the command.
     */
    List<Option> getOptions();

    /**
     * Represents an option for a command, including its metadata and configuration.
     */
    interface Option {
        /**
         * Gets all names (including aliases) for this option.
         */
        Set<String> getNames();

        /**
         * Gets the label for this option.
         */
        String getLabel();

        /**
         * Gets the description of this option.
         */
        String getDescription();

        /**
         * Indicates whether this option is required.
         */
        boolean isRequired();

        /**
         * Indicates whether this option is hidden from help or listing.
         */
        boolean isHidden();

        /**
         * Gets the default value for this option, if any.
         */
        String getDefaultValue();

        /**
         * Indicates whether this option is global (applies to all commands).
         */
        boolean isGlobal();

        /**
         * Gets the type of value expected for this option.
         */
        Class<?> getType();
    }
}
