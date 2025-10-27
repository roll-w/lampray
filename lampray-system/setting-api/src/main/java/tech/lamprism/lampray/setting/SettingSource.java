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

import java.util.List;

/**
 * Setting source.
 * <p>
 * Load order: LOCAL -> PROPERTIES -> ENVIRONMENT -> DATABASE
 * <p>
 * The next one can override the previous one.
 *
 * @author RollW
 */
public enum SettingSource {
    /**
     * No setting.
     */
    NONE,

    /**
     * Local setting, load from the conf file.
     */
    LOCAL,

    /**
     * Setting from system properties. System properties are passed
     * to the JVM when it starts.
     */
    PROPERTIES,

    /**
     * Setting from environment variables.
     */
    ENVIRONMENT,

    /**
     * Setting from command line arguments.
     */
    COMMAND_LINE,


    /**
     * Setting from the database.
     */
    DATABASE;

    public static final List<SettingSource> VALUES = List.of(LOCAL, PROPERTIES, ENVIRONMENT, COMMAND_LINE, DATABASE);

    public static final List<SettingSource> LOCAL_ONLY = List.of(LOCAL, PROPERTIES, ENVIRONMENT, COMMAND_LINE);
}
