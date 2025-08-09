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

import org.apache.commons.lang3.StringUtils;

import java.util.List;

/**
 * @author RollW
 */
public interface CommandTree extends CommandSpecification {
    List<CommandTree> getChildren();

    CommandTree getParent();

    /**
     * Finds a child command by its full name recursively.
     *
     * @param name the full name of the child command
     * @return the matching child CommandTree, or null if not found
     */
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

    /**
     * Gets the full name of this command, which is the concatenation of
     * the names of this command and all its parent commands.
     *
     * @return the full name of the command
     */
    default String getFullName() {
        CommandTree commandTree = this;
        StringBuilder fullName = new StringBuilder();
        while (commandTree != null) {
            String name = commandTree.getName();
            if (StringUtils.isBlank(name)) {
                commandTree = commandTree.getParent();
                continue;
            }
            if (fullName.length() > 0) {
                fullName.insert(0, " ");
            }
            fullName.insert(0, name);
            commandTree = commandTree.getParent();
        }
        return fullName.toString();
    }

    default boolean isRoot() {
        return getParent() == null;
    }

    boolean isHidden();

    List<Option> getOptions();
}
