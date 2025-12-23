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

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.luben.zstd.Zstd;
import com.github.luben.zstd.ZstdException;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;
import org.apache.commons.lang3.ArrayUtils;
import org.msgpack.jackson.dataformat.MessagePackFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import tech.lamprism.lampray.content.structuraltext.StructuralText;
import tech.lamprism.lampray.content.structuraltext.element.Text;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Objects;

/**
 * @author RollW
 */
@Converter(autoApply = true)
public class StructuralTextAttributeConverter implements AttributeConverter<StructuralText, byte[]> {
    private static final Logger logger = LoggerFactory.getLogger(StructuralTextAttributeConverter.class);

    private final ObjectMapper objectMapper;

    public StructuralTextAttributeConverter(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper.copyWith(new MessagePackFactory());
        this.objectMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        this.objectMapper.setSerializationInclusion(JsonInclude.Include.NON_NULL);
    }

    @Override
    public byte[] convertToDatabaseColumn(StructuralText attribute) {
        if (attribute == null || StructuralText.EMPTY.equals(attribute)) {
            return null;
        }
        try {
            byte[] valueBytes = objectMapper.writeValueAsBytes(attribute);
            byte[] compressed = Zstd.compress(valueBytes);
            return ArrayUtils.addAll(Version.V1.header, compressed);
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Error converting StructuralText to JSON string", e);
        }
    }

    @Override
    public StructuralText convertToEntityAttribute(byte[] dbData) {
        if (dbData == null || dbData.length == 0) {
            return StructuralText.EMPTY;
        }
        try {
            byte[] header = ArrayUtils.subarray(dbData, 0, HEADER_LENGTH);
            if (header.length < HEADER_LENGTH) {
                return new Text(new String(dbData, StandardCharsets.UTF_8));
            }
            Version version = null;
            for (Version v : Version.values()) {
                if (Objects.deepEquals(v.header, header)) {
                    version = v;
                    break;
                }
            }
            if (version == null) {
                return new Text(new String(dbData, StandardCharsets.UTF_8));
            }
            byte[] compressedData = ArrayUtils.subarray(dbData, HEADER_LENGTH, dbData.length);
            byte[] decompressedData = decompress(compressedData, version);
            if (decompressedData == null) {
                return new Text(new String(dbData, StandardCharsets.UTF_8));
            }
            try {
                return objectMapper.readValue(decompressedData, StructuralText.class);
            } catch (JsonProcessingException e) {
                // With version header, decompression succeeded, but JSON parsing failed
                logger.error("Failed to parse StructuralText JSON data, version: {}, data in string: {}",
                        version, new String(decompressedData, StandardCharsets.UTF_8), e);
                return new Text(new String(decompressedData, StandardCharsets.UTF_8));
            }
        } catch (IOException e) {
            throw new RuntimeException("Error converting stored data to StructuralText", e);
        }
    }

    private byte[] decompress(byte[] compressedData, Version version) {
        try {
            return Zstd.decompress(compressedData);
        } catch (ZstdException e) {
            // With version header, but decompression failed, maybe corrupted data
            logger.error("Failed to decompress StructuralText data, version: {}", version, e);
            return null;
        }
    }

    private static final int HEADER_LENGTH = 4;

    public enum Version {
        V1(new byte[]{0x00, 0x00, 0x00, 0x01});

        private final byte[] header;

        Version(byte[] header) {
            this.header = header;
        }
    }
}
