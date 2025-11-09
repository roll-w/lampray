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
 * Thrown when structural text is empty or no text node found.
 *
 * @author RollW
 */
public class StructuralTextEmptyException extends StructuralTextValidationException {
    public StructuralTextEmptyException() {
        super();
    }

    public StructuralTextEmptyException(String message) {
        super(message);
    }

    public StructuralTextEmptyException(Throwable cause) {
        super(cause);
    }

    public StructuralTextEmptyException(String message, Throwable cause) {
        super(message, cause);
    }
}

