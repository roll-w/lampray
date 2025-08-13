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

package tech.lamprism.lampray.authentication;

import tech.lamprism.lampray.setting.AttributedSettingSpecification;
import tech.lamprism.lampray.setting.SettingKey;
import tech.lamprism.lampray.setting.SettingSource;
import tech.lamprism.lampray.setting.SettingSpecificationBuilder;
import tech.lamprism.lampray.setting.SettingSpecificationSupplier;

import java.util.List;

/**
 * @author RollW
 */
public class SecurityConfigKeys implements SettingSpecificationSupplier {
    public static final AttributedSettingSpecification<String, String> TOKEN_ISSUER =
            new SettingSpecificationBuilder<>(SettingKey.ofString("security.token.issuer"))
                    .setDefaultValue("Lampray")
                    .setSupportedSources(SettingSource.VALUES)
                    .setTextDescription("Token issuer.")
                    .setRequired(true)
                    .build();

    public static final AttributedSettingSpecification<Long, Long> TOKEN_EXPIRE_TIME =
            new SettingSpecificationBuilder<>(SettingKey.ofLong("security.token.expire-time"))
                    .setDefaultValue(3600L)
                    .setTextDescription("Token expiration time in seconds.")
                    .setSupportedSources(SettingSource.VALUES)
                    .setRequired(true)
                    .build();

    public static final String RANDOM = "[random]";

    public static final AttributedSettingSpecification<String, String> TOKEN_SIGN_KEY =
            new SettingSpecificationBuilder<>(SettingKey.ofString("security.token.sign-key"))
                    .setDefaultValue(RANDOM)
                    .setTextDescription("Key used to sign and verify token.")
                    .setSupportedSources(SettingSource.VALUES)
                    .setRequired(true)
                    .build();

    public static final String PREFIX = "security.";

    private static final List<AttributedSettingSpecification<?, ?>> KEYS = List.of(
            TOKEN_ISSUER,
            TOKEN_EXPIRE_TIME,
            TOKEN_SIGN_KEY
    );

    public static final SecurityConfigKeys INSTANCE = new SecurityConfigKeys();

    @Override
    public List<AttributedSettingSpecification<?, ?>> getSpecifications() {
        return KEYS;
    }

    private SecurityConfigKeys() {
    }
}
