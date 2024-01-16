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

package space.lingu.lamp.web.common;

import tech.rollw.common.web.BusinessRuntimeException;
import tech.rollw.common.web.WebCommonErrorCode;

/**
 * @author RollW
 */
@Deprecated
public class ParameterFailedException extends BusinessRuntimeException {
    private static final String DEFAULT_TEMPLATE = "Parameter '{}' validate failed.";

    public ParameterFailedException() {
        super(WebCommonErrorCode.ERROR_PARAM_FAILED);
    }

    public ParameterFailedException(String message, Object... args) {
        super(WebCommonErrorCode.ERROR_PARAM_FAILED, message, args);
    }

    public ParameterFailedException(String parameterName) {
        this(DEFAULT_TEMPLATE, parameterName);
    }
}
