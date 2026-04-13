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

package tech.lamprism.lampray.lock.configuration;

import org.springframework.stereotype.Component;
import space.lingu.NonNull;
import tech.lamprism.lampray.setting.AttributedSettingSpecification;
import tech.lamprism.lampray.setting.SettingKey;
import tech.lamprism.lampray.setting.SettingSource;
import tech.lamprism.lampray.setting.SettingSpecificationBuilder;
import tech.lamprism.lampray.setting.SettingSpecificationSupplier;

import java.util.List;

/**
 * @author RollW
 */
@Component
public class LockConfigKeys implements SettingSpecificationSupplier {
    public static final String PREFIX = "lock.";

    public static final AttributedSettingSpecification<Long, Long> LEASE_SECONDS =
            new SettingSpecificationBuilder<>(SettingKey.ofLong(PREFIX + "lease-seconds"))
                    .setTextDescription("Lease duration in seconds for shared locks.")
                    .setDefaultValue(300L)
                    .setRequired(true)
                    .setSupportedSources(SettingSource.VALUES)
                    .build();

    public static final AttributedSettingSpecification<Long, Long> RETRY_INTERVAL_MILLIS =
            new SettingSpecificationBuilder<>(SettingKey.ofLong(PREFIX + "retry-interval-millis"))
                    .setTextDescription("Retry interval in milliseconds while waiting for shared locks.")
                    .setDefaultValue(200L)
                    .setRequired(true)
                    .setSupportedSources(SettingSource.VALUES)
                    .build();

    public static final LockConfigKeys INSTANCE = new LockConfigKeys();

    private static final List<AttributedSettingSpecification<?, ?>> SPECIFICATIONS = List.of(
            LEASE_SECONDS,
            RETRY_INTERVAL_MILLIS
    );

    @NonNull
    @Override
    public List<AttributedSettingSpecification<?, ?>> getSpecifications() {
        return SPECIFICATIONS;
    }
}
