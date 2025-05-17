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

package tech.lamprism.lampray.web.controller.storage;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import tech.lamprism.lampray.storage.FileStorage;
import tech.lamprism.lampray.storage.StorageProvider;
import tech.lamprism.lampray.web.controller.Api;

import java.io.IOException;

/**
 * @author RollW
 */
@Api
public class StorageController {
    private final StorageProvider storageProvider;

    public StorageController(StorageProvider storageProvider) {
        this.storageProvider = storageProvider;
    }

    // TODO: rewrite this method
    @GetMapping("/storages/{id}")
    public void getStorage(@PathVariable("id") String id,
                           HttpServletRequest request,
                           HttpServletResponse response) throws IOException {
        FileStorage fileStorage = storageProvider.getFileStorage(id);
        try {
            DownloadHelper.downloadFile(
                    fileStorage, id, request,
                    response, storageProvider
            );
        } catch (IOException e) {
            response.reset();
            throw e;
        }
    }
}
