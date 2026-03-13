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

package tech.lamprism.lampray.security.authentication;

import space.lingu.NonNull;
import tech.lamprism.lampray.setting.AttributedSettingSpecification;
import tech.lamprism.lampray.setting.SettingKey;
import tech.lamprism.lampray.setting.SettingSource;
import tech.lamprism.lampray.setting.SettingSpecificationBuilder;
import tech.lamprism.lampray.setting.SettingSpecificationSupplier;

import java.util.List;
import java.util.Set;

/**
 * @author RollW
 */
public class SecurityConfigKeys implements SettingSpecificationSupplier {

    public static final String PREFIX = "security.";

    public static final AttributedSettingSpecification<Boolean, Boolean> CORS_ALLOW_ALL_ORIGINS =
            new SettingSpecificationBuilder<>(SettingKey.ofBoolean(PREFIX + "cors.allow-all-origins"))
                    .setTextDescription("Whether to allow cross-origin requests from any origin.")
                    .setValueEntries(List.of(false, true))
                    .setDefaultValue(true)
                    .setRequired(false)
                    .setSupportedSources(SettingSource.VALUES)
                    .build();

    public static final AttributedSettingSpecification<Set<String>, String> CORS_ALLOWED_ORIGINS =
            new SettingSpecificationBuilder<>(SettingKey.ofStringSet(PREFIX + "cors.allowed-origins"))
                    .setTextDescription("Allowed CORS origins when allow-all-origins is disabled.")
                    .setRequired(false)
                    .setSupportedSources(SettingSource.VALUES)
                    .build();

    private static final List<AttributedSettingSpecification<?, ?>> KEYS = List.of(
            CORS_ALLOW_ALL_ORIGINS,
            CORS_ALLOWED_ORIGINS
    );

    public static final SecurityConfigKeys INSTANCE = new SecurityConfigKeys();

    @NonNull
    @Override
    public List<AttributedSettingSpecification<?, ?>> getSpecifications() {
        return KEYS;
    }

    private SecurityConfigKeys() {
    }
}
