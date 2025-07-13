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

package tech.lamprism.lampray.storage.fs;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

/**
 * @author RollW
 */
public class IOUtils {
    public static void transferTo(InputStream inputStream, OutputStream outputStream, PositionMark positionMark) throws IOException {
        long skipped = inputStream.skip(positionMark.getOffset());
        if (skipped < positionMark.getOffset()) {
            throw new FileStoreException("Failed to skip to the specified offset: " + positionMark.getOffset());
        }
        long bytesToRead = positionMark.getLength();
        byte[] buffer = new byte[8192];
        int bytesRead;
        while (bytesToRead > 0 && (bytesRead = inputStream.read(buffer, 0, (int) Math.min(buffer.length, bytesToRead))) != -1) {
            outputStream.write(buffer, 0, bytesRead);
            bytesToRead -= bytesRead;
        }
    }

    public static void transferTo(InputStream inputStream, OutputStream outputStream) throws IOException {
        inputStream.transferTo(outputStream);
    }
}
