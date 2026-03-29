/*
 * Copyright (C) 2023-2026 RollW
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

package tech.lamprism.lampray.storage.routing;

import org.springframework.stereotype.Component;
import tech.lamprism.lampray.storage.StorageException;
import tech.rollw.common.web.CommonErrorCode;
import tech.rollw.common.web.DataErrorCode;

/**
 * @author RollW
 */
@Component
public class StorageWritePlanResolver {
    private final StorageGroupRouter storageGroupRouter;

    public StorageWritePlanResolver(StorageGroupRouter storageGroupRouter) {
        this.storageGroupRouter = storageGroupRouter;
    }

    public StorageWritePlan select(String groupName) {
        try {
            return storageGroupRouter.selectWritePlan(groupName);
        } catch (IllegalArgumentException exception) {
            throw new StorageException(CommonErrorCode.ERROR_ILLEGAL_ARGUMENT, exception.getMessage());
        } catch (IllegalStateException exception) {
            throw new StorageException(DataErrorCode.ERROR_DATA_NOT_EXIST, exception.getMessage());
        }
    }

    public StorageWritePlan restore(String groupName,
                                    String primaryBackend) {
        try {
            return storageGroupRouter.restoreWritePlan(groupName, primaryBackend);
        } catch (IllegalArgumentException exception) {
            throw new StorageException(CommonErrorCode.ERROR_ILLEGAL_ARGUMENT, exception.getMessage());
        } catch (IllegalStateException exception) {
            throw new StorageException(DataErrorCode.ERROR_DATA_NOT_EXIST, exception.getMessage());
        }
    }
}
