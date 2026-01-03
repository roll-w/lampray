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

package tech.lamprism.lampray.common.data;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.luben.zstd.Zstd;
import com.github.luben.zstd.ZstdException;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;
import jakarta.persistence.PersistenceException;
import org.apache.commons.lang3.ArrayUtils;
import org.msgpack.jackson.dataformat.MessagePackFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Objects;

/**
 * Abstract base class for binary compact attribute converters using MessagePack
 * serialization and Zstd compression.
 *
 * @author RollW
 */
@Converter
public abstract class BinaryObjectAttributeConverter<X> implements AttributeConverter<X, byte[]> {
    protected static final Logger logger = LoggerFactory.getLogger(BinaryObjectAttributeConverter.class);
    protected static final int HEADER_LENGTH = 4;

    protected final ObjectMapper objectMapper;

    protected BinaryObjectAttributeConverter(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper.copyWith(new MessagePackFactory());
        this.objectMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        this.objectMapper.setSerializationInclusion(JsonInclude.Include.NON_NULL);
    }

    @Override
    public byte[] convertToDatabaseColumn(X attribute) {
        if (attribute == null) {
            return null;
        }
        try {
            byte[] valueBytes = objectMapper.writeValueAsBytes(attribute);
            byte[] compressed = Zstd.compress(valueBytes);
            return ArrayUtils.addAll(Version.V1.header, compressed);
        } catch (JsonProcessingException e) {
            throw new PersistenceException("Error converting to JSON bytes", e);
        }
    }

    @Override
    public X convertToEntityAttribute(byte[] dbData) {
        if (dbData == null || dbData.length == 0) {
            return getEmptyValue();
        }
        try {
            byte[] header = ArrayUtils.subarray(dbData, 0, HEADER_LENGTH);
            if (header.length < HEADER_LENGTH) {
                return fallbackConvert(dbData);
            }
            Version version = null;
            for (Version v : Version.values()) {
                if (Objects.deepEquals(v.header, header)) {
                    version = v;
                    break;
                }
            }
            if (version == null) {
                return fallbackConvert(dbData);
            }
            byte[] compressedData = ArrayUtils.subarray(dbData, HEADER_LENGTH, dbData.length);
            byte[] decompressedData = decompress(compressedData, version);
            if (decompressedData == null) {
                return fallbackConvert(dbData);
            }
            try {
                return objectMapper.readValue(decompressedData, getValueType());
            } catch (JsonProcessingException e) {
                logger.error("Failed to parse data from JSON, version: {}, data: {}",
                        version, new String(decompressedData, StandardCharsets.UTF_8), e);
                return onDeserializationError(decompressedData);
            }
        } catch (IOException e) {
            throw new PersistenceException("Error converting stored data", e);
        }
    }

    protected byte[] decompress(byte[] compressedData, Version version) {
        try {
            return Zstd.decompress(compressedData);
        } catch (ZstdException e) {
            logger.error("Failed to decompress data, version: {}", version, e);
            return null;
        }
    }

    /**
     * Get the target class type for deserialization.
     */
    protected abstract Class<X> getValueType();

    /**
     * Handle fallback conversion when version header is not recognized.
     * Default implementation returns null.
     */
    protected X fallbackConvert(byte[] dbData) {
        return null;
    }

    /**
     * Get the empty/null value for this type. Default implementation returns null.
     */
    protected X getEmptyValue() {
        return null;
    }

    /**
     * Handle deserialization errors. Default implementation returns null.
     */
    protected X onDeserializationError(byte[] decompressedData) {
        return null;
    }

    public enum Version {
        V1(new byte[]{0x00, 0x00, 0x00, 0x01});

        private final byte[] header;

        Version(byte[] header) {
            this.header = header;
        }
    }
}

