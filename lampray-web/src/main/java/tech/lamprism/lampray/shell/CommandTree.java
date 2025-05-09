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

package tech.lamprism.lampray.shell;

import java.util.List;
import java.util.Set;

/**
 * @author RollW
 */
public interface CommandTree {
    List<CommandTree> getChildren();

    CommandTree getParent();

    String getName();

    String getDescription();

    String getHeader();

    String getGroup();

    List<String> getAliases();

    default CommandTree findChildByFullName(String name) {
        for (CommandTree child : getChildren()) {
            String fullName = child.getFullName();
            if (fullName.equals(name)) {
                return child;
            }
            if (name.startsWith(fullName)) {
                return child.findChildByFullName(name);
            }
        }
        return null;
    }

    default String getFullName() {
        CommandTree commandTree = this;
        StringBuilder name = new StringBuilder();
        while (commandTree != null) {
            if (name.length() > 0) {
                name.insert(0, " ");
            }
            name.insert(0, commandTree.getName());
            commandTree = commandTree.getParent();
        }
        return name.toString();
    }

    default boolean isRoot() {
        return getParent() == null;
    }

    boolean isHidden();

    List<Option> getOptions();

    interface Option {
        Set<String> getNames();

        String getLabel();

        String getDescription();

        boolean isRequired();

        boolean isHidden();

        String getDefaultValue();

        boolean isGlobal();
    }
}
