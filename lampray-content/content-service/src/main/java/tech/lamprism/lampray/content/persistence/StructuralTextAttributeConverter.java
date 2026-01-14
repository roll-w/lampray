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

package tech.lamprism.lampray.content.persistence;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.persistence.Converter;
import tech.lamprism.lampray.common.data.BinaryObjectAttributeConverter;
import tech.lamprism.lampray.content.structuraltext.StructuralText;
import tech.lamprism.lampray.content.structuraltext.element.Text;

import java.nio.charset.StandardCharsets;

/**
 * @author RollW
 */
@Converter(autoApply = true)
public class StructuralTextAttributeConverter extends BinaryObjectAttributeConverter<StructuralText> {
    public StructuralTextAttributeConverter(ObjectMapper objectMapper) {
        super(objectMapper);
    }

    @Override
    public byte[] convertToDatabaseColumn(StructuralText attribute) {
        if (attribute == null || StructuralText.EMPTY.equals(attribute)) {
            return null;
        }
        return super.convertToDatabaseColumn(attribute);
    }

    @Override
    public StructuralText convertToEntityAttribute(byte[] dbData) {
        return super.convertToEntityAttribute(dbData);
    }

    @Override
    protected Class<StructuralText> getValueType() {
        return StructuralText.class;
    }

    @Override
    protected StructuralText getEmptyValue() {
        return StructuralText.EMPTY;
    }

    @Override
    protected StructuralText fallbackConvert(byte[] dbData) {
        // Fallback: treat as plain text when version header is not recognized
        return new Text(new String(dbData, StandardCharsets.UTF_8));
    }

    @Override
    protected StructuralText onDeserializationError(byte[] decompressedData) {
        // On deserialization error: treat as plain text
        return new Text(new String(decompressedData, StandardCharsets.UTF_8));
    }
}
