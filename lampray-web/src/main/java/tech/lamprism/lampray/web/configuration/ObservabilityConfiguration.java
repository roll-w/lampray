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

package tech.lamprism.lampray.web.configuration;

import io.micrometer.core.instrument.Meter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.config.MeterFilter;
import io.micrometer.core.instrument.distribution.DistributionStatisticConfig;
import io.micrometer.core.instrument.observation.DefaultMeterObservationHandler;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import io.micrometer.observation.ObservationRegistry;
import io.micrometer.prometheusmetrics.PrometheusConfig;
import io.micrometer.prometheusmetrics.PrometheusMeterRegistry;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import tech.lamprism.lampray.observability.CorrelationContextHolder;
import tech.lamprism.lampray.observability.MetricProvider;
import tech.lamprism.lampray.observability.Observations;
import tech.lamprism.lampray.observability.core.MicrometerMetricProvider;
import tech.lamprism.lampray.observability.core.MicrometerObservations;
import tech.lamprism.lampray.observability.core.MicrometerSystemMetrics;
import tech.lamprism.lampray.observability.core.ThreadLocalCorrelationContextHolder;
import tech.lamprism.lampray.setting.ConfigReader;
import tech.lamprism.lampray.web.common.keys.ObservabilityConfigKeys;
import tech.lamprism.lampray.web.configuration.filter.ApiContextInitializeFilter;
import tech.lamprism.lampray.web.configuration.filter.MetricsScrapeAuthenticationFilter;
import tech.lamprism.lampray.web.configuration.filter.RequestObservabilityFilter;

import java.util.ArrayList;
import java.util.List;
import java.time.Duration;

/**
 * @author RollW
 */
@Configuration
public class ObservabilityConfiguration {
    private final ConfigReader configReader;

    public ObservabilityConfiguration(ConfigReader configReader) {
        this.configReader = configReader;
    }

    @Bean
    public CorrelationContextHolder correlationContextHolder() {
        return new ThreadLocalCorrelationContextHolder();
    }

    @Bean
    public FilterRegistrationBean<RequestObservabilityFilter> requestObservabilityFilterRegistration(
            RequestObservabilityFilter requestObservabilityFilter) {
        return disableServletRegistration(requestObservabilityFilter);
    }

    @Bean
    public FilterRegistrationBean<ApiContextInitializeFilter> apiContextInitializeFilterRegistration(
            ApiContextInitializeFilter apiContextInitializeFilter) {
        return disableServletRegistration(apiContextInitializeFilter);
    }

    @Bean
    public FilterRegistrationBean<MetricsScrapeAuthenticationFilter> metricsScrapeAuthenticationFilterRegistration(
            MetricsScrapeAuthenticationFilter metricsScrapeAuthenticationFilter) {
        return disableServletRegistration(metricsScrapeAuthenticationFilter);
    }

    @Bean
    public MeterRegistry observabilityMeterRegistry() {
        MeterRegistry meterRegistry = isPrometheusEnabled()
                ? new PrometheusMeterRegistry(PrometheusConfig.DEFAULT)
                : new SimpleMeterRegistry();
        configureMeterRegistry(meterRegistry);
        MicrometerSystemMetrics.bindTo(meterRegistry);

        return meterRegistry;
    }

    @Bean
    public ObservationRegistry observationRegistry(MeterRegistry observabilityMeterRegistry) {
        ObservationRegistry observationRegistry = ObservationRegistry.create();
        observationRegistry.observationConfig()
                .observationHandler(new DefaultMeterObservationHandler(observabilityMeterRegistry));
        return observationRegistry;
    }

    @Bean
    public MetricProvider metricProvider(MeterRegistry observabilityMeterRegistry) {
        return new MicrometerMetricProvider(observabilityMeterRegistry);
    }

    @Bean
    public Observations observations(ObservationRegistry observationRegistry) {
        return new MicrometerObservations(observationRegistry);
    }

    private boolean isPrometheusEnabled() {
        return Boolean.TRUE.equals(configReader.get(ObservabilityConfigKeys.PROMETHEUS_ENABLED, true));
    }

    private void configureMeterRegistry(MeterRegistry meterRegistry) {
        List<String> commonTags = new ArrayList<>();
        appendCommonTag(commonTags, "application", configReader.get(
                ObservabilityConfigKeys.COMMON_TAG_APPLICATION,
                ObservabilityConfigKeys.DEFAULT_APPLICATION_TAG
        ));
        appendCommonTag(commonTags, "instance", configReader.get(
                ObservabilityConfigKeys.COMMON_TAG_INSTANCE,
                ObservabilityConfigKeys.DEFAULT_INSTANCE_TAG
        ));
        appendCommonTag(commonTags, "environment", configReader.get(
                ObservabilityConfigKeys.COMMON_TAG_ENVIRONMENT,
                ObservabilityConfigKeys.DEFAULT_ENVIRONMENT_TAG
        ));

        var config = meterRegistry.config();
        if (!commonTags.isEmpty()) {
            config.commonTags(commonTags.toArray(String[]::new));
        }
        config.meterFilter(MeterFilter.maximumAllowableMetrics(5000))
                .meterFilter(new MeterFilter() {
                    @Override
                    public DistributionStatisticConfig configure(Meter.Id id,
                                                                 DistributionStatisticConfig config) {
                        if (supportsDistributionStatistics(id)) {
                            return DistributionStatisticConfig.builder()
                                    .percentilesHistogram(true)
                                    .percentiles(0.5, 0.95, 0.99)
                                    .serviceLevelObjectives(
                                            Duration.ofMillis(100).toNanos(),
                                            Duration.ofMillis(300).toNanos(),
                                            Duration.ofSeconds(1).toNanos()
                                    )
                                    .build()
                                    .merge(config);
                        }
                        return config;
                    }
                });
    }

    private void appendCommonTag(List<String> tags,
                                 String key,
                                 String value) {
        if (value == null || value.trim().isEmpty()) {
            return;
        }
        tags.add(key);
        tags.add(value.trim());
    }

    private boolean supportsDistributionStatistics(Meter.Id id) {
        String name = id.getName();
        return name.startsWith("lampray.http.server.request")
                || name.startsWith("lampray.async.task");
    }

    private <T extends jakarta.servlet.Filter> FilterRegistrationBean<T> disableServletRegistration(T filter) {
        FilterRegistrationBean<T> registrationBean = new FilterRegistrationBean<>(filter);
        registrationBean.setEnabled(false);
        return registrationBean;
    }
}
