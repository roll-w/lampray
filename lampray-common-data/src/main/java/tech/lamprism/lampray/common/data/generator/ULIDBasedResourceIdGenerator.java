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

package tech.lamprism.lampray.common.data.generator;

import com.github.f4b6a3.ulid.Ulid;
import com.github.f4b6a3.ulid.UlidCreator;
import org.springframework.stereotype.Service;
import tech.lamprism.lampray.common.data.ResourceIdGenerator;
import tech.rollw.common.web.system.SystemResourceKind;

/**
 * @author RollW
 */
@Service// TODO: move to configuration class
public class ULIDBasedResourceIdGenerator implements ResourceIdGenerator {
    @Override
    public String nextId(SystemResourceKind systemResourceKind) {
        Ulid ulid = UlidCreator.getUlid();
        return ulid.toString();
    }
}
