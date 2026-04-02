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

package tech.lamprism.lampray.web.common.keys

import org.springframework.stereotype.Component
import tech.lamprism.lampray.setting.AttributedSettingSpecification
import tech.lamprism.lampray.setting.SettingKey
import tech.lamprism.lampray.setting.SettingSource
import tech.lamprism.lampray.setting.SettingSpecificationBuilder
import tech.lamprism.lampray.setting.SettingSpecificationSupplier

/**
 * @author RollW
 */
@Component
object ObservabilityConfigKeys : SettingSpecificationSupplier {
    const val DEFAULT_REQUEST_ID_HEADER = "X-Request-Id"
    const val DEFAULT_APPLICATION_TAG = "lampray"
    const val DEFAULT_INSTANCE_TAG = "local"
    const val DEFAULT_ENVIRONMENT_TAG = "default"
    const val DEFAULT_METRICS_SCRAPE_TOKEN = ""
    private val DEFAULT_EXPOSED_ENDPOINTS = listOf("info", "health", "metrics")
    private val DEFAULT_LIVENESS_CONTRIBUTORS = listOf("application", "observationRegistry", "meterRegistry")
    private val DEFAULT_READINESS_CONTRIBUTORS = listOf("observability", "database", "prometheus")

    @JvmField
    val REQUEST_ID_HEADER =
        SettingSpecificationBuilder(SettingKey.ofString("observability.correlation.request-id-header"))
            .setTextDescription("Header name used for request correlation.")
            .setDefaultValue(DEFAULT_REQUEST_ID_HEADER)
            .setSupportedSources(SettingSource.VALUES)
            .setRequired(false)
            .build()

    @JvmField
    val MANAGEMENT_EXPOSED_ENDPOINTS =
        SettingSpecificationBuilder(SettingKey.ofStringSet("observability.management.endpoints.exposed"))
            .setTextDescription("Exposed observability management endpoints.")
            .setValueEntries(DEFAULT_EXPOSED_ENDPOINTS)
            .setDefaults(DEFAULT_EXPOSED_ENDPOINTS.indices.toList())
            .setSupportedSources(SettingSource.VALUES)
            .setRequired(false)
            .build()

    @JvmField
    val HEALTH_LIVENESS_CONTRIBUTORS =
        SettingSpecificationBuilder(SettingKey.ofStringSet("observability.health.liveness.include"))
            .setTextDescription("Health contributors included in the liveness group.")
            .setValueEntries(DEFAULT_LIVENESS_CONTRIBUTORS)
            .setDefaults(DEFAULT_LIVENESS_CONTRIBUTORS.indices.toList())
            .setSupportedSources(SettingSource.VALUES)
            .setRequired(false)
            .build()

    @JvmField
    val HEALTH_READINESS_CONTRIBUTORS =
        SettingSpecificationBuilder(SettingKey.ofStringSet("observability.health.readiness.include"))
            .setTextDescription("Health contributors included in the readiness group.")
            .setValueEntries(DEFAULT_READINESS_CONTRIBUTORS)
            .setDefaults(DEFAULT_READINESS_CONTRIBUTORS.indices.toList())
            .setSupportedSources(SettingSource.VALUES)
            .setRequired(false)
            .build()

    @JvmField
    val METRICS_SCRAPE_TOKEN =
        SettingSpecificationBuilder(SettingKey.ofString("observability.prometheus.scrape-token"))
            .setTextDescription("Bearer token required to access the Prometheus scrape alias.")
            .setDefaultValue(DEFAULT_METRICS_SCRAPE_TOKEN)
            .setSupportedSources(SettingSource.VALUES)
            .setRequired(false)
            .build()

    @JvmField
    val COMMON_TAG_APPLICATION =
        SettingSpecificationBuilder(SettingKey.ofString("observability.common-tags.application"))
            .setTextDescription("Common application tag attached to exported meters.")
            .setDefaultValue(DEFAULT_APPLICATION_TAG)
            .setSupportedSources(SettingSource.VALUES)
            .setRequired(false)
            .build()

    @JvmField
    val COMMON_TAG_INSTANCE =
        SettingSpecificationBuilder(SettingKey.ofString("observability.common-tags.instance"))
            .setTextDescription("Common instance tag attached to exported meters.")
            .setDefaultValue(DEFAULT_INSTANCE_TAG)
            .setSupportedSources(SettingSource.VALUES)
            .setRequired(false)
            .build()

    @JvmField
    val COMMON_TAG_ENVIRONMENT =
        SettingSpecificationBuilder(SettingKey.ofString("observability.common-tags.environment"))
            .setTextDescription("Common environment tag attached to exported meters.")
            .setDefaultValue(DEFAULT_ENVIRONMENT_TAG)
            .setSupportedSources(SettingSource.VALUES)
            .setRequired(false)
            .build()

    private val keys = listOf(
        REQUEST_ID_HEADER,
        MANAGEMENT_EXPOSED_ENDPOINTS,
        HEALTH_LIVENESS_CONTRIBUTORS,
        HEALTH_READINESS_CONTRIBUTORS,
        METRICS_SCRAPE_TOKEN,
        COMMON_TAG_APPLICATION,
        COMMON_TAG_INSTANCE,
        COMMON_TAG_ENVIRONMENT
    )

    override val specifications: List<AttributedSettingSpecification<*, *>>
        get() = keys
}
