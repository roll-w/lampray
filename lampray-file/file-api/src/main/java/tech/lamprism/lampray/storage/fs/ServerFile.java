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

package tech.lamprism.lampray.storage.fs;

/**
 * A file in the file server. It is a file that can be read and written.
 *
 * @author RollW
 */
public interface ServerFile {
    /**
     * A qualified name is a unique identifier for a file.
     * It is used to identify a file in the file server.
     *
     * @return qualified name of the file
     */
    String getQualifiedName();

    /**
     * Get the size of the file.
     *
     * @return size of the file
     */
    long getSize();

    String getServerName();
}
