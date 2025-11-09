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

package tech.lamprism.lampray.content.structuraltext.validation;

/**
 * @author RollW
 */
public class StructuralTextTooLongException extends StructuralTextValidationException {
    public StructuralTextTooLongException() {
        super();
    }

    public StructuralTextTooLongException(String message) {
        super(message);
    }

    public StructuralTextTooLongException(Throwable cause) {
        super(cause);
    }

    public StructuralTextTooLongException(String message, Throwable cause) {
        super(message, cause);
    }
}
