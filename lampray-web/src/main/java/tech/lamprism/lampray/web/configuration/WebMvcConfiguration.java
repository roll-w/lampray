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

package tech.lamprism.lampray.web.configuration;

import com.fasterxml.jackson.databind.MapperFeature;
import com.fasterxml.jackson.databind.SerializationFeature;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.converter.json.Jackson2ObjectMapperBuilder;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import tech.lamprism.lampray.content.structuraltext.StructuralText;
import tech.lamprism.lampray.content.structuraltext.jackson.StructuralTextDeserializer;
import tech.lamprism.lampray.web.configuration.json.ErrorCodeDeserializer;
import tech.lamprism.lampray.web.configuration.json.ErrorCodeSerializer;
import tech.rollw.common.web.ErrorCode;
import tech.rollw.common.web.ErrorCodeFinderChain;

/**
 * @author RollW
 */
@Configuration
public class WebMvcConfiguration implements WebMvcConfigurer {
    @Bean
    public Jackson2ObjectMapperBuilder jackson2ObjectMapperBuilder(ErrorCodeFinderChain finderChain) {
        ErrorCodeDeserializer errorCodeDeserializer = new ErrorCodeDeserializer(finderChain);
        ErrorCodeSerializer errorCodeSerializer = new ErrorCodeSerializer();
        StructuralTextDeserializer structuralTextDeserializer = new StructuralTextDeserializer();

        return Jackson2ObjectMapperBuilder
                .json()
                .featuresToEnable(MapperFeature.ACCEPT_CASE_INSENSITIVE_ENUMS)
                .featuresToDisable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
                .serializerByType(ErrorCode.class, errorCodeSerializer)
                .deserializerByType(ErrorCode.class, errorCodeDeserializer)
                .deserializerByType(StructuralText.class, structuralTextDeserializer);
    }

}
