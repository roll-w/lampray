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

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import io.micrometer.observation.ObservationRegistry;
import io.micrometer.prometheusmetrics.PrometheusConfig;
import io.micrometer.prometheusmetrics.PrometheusMeterRegistry;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import tech.lamprism.lampray.observability.CorrelationContextHolder;
import tech.lamprism.lampray.observability.HealthContributor;
import tech.lamprism.lampray.observability.MetricProvider;
import tech.lamprism.lampray.observability.MeterRegistryContributor;
import tech.lamprism.lampray.observability.ObservationRegistryContributor;
import tech.lamprism.lampray.observability.Observations;
import tech.lamprism.lampray.observability.StatusAggregator;
import tech.lamprism.lampray.observability.core.CompositeHealthService;
import tech.lamprism.lampray.observability.core.DefaultMeterObservationRegistryContributor;
import tech.lamprism.lampray.observability.core.DefaultStatusAggregator;
import tech.lamprism.lampray.observability.core.MeterRegistryContributors;
import tech.lamprism.lampray.observability.core.MicrometerMetricProvider;
import tech.lamprism.lampray.observability.core.MicrometerObservations;
import tech.lamprism.lampray.observability.core.MicrometerSystemMetricsContributor;
import tech.lamprism.lampray.observability.core.ObservationRegistryContributors;
import tech.lamprism.lampray.observability.core.ThreadLocalCorrelationContextHolder;
import tech.lamprism.lampray.web.configuration.filter.ApiContextInitializeFilter;
import tech.lamprism.lampray.web.configuration.filter.MetricsScrapeAuthenticationFilter;
import tech.lamprism.lampray.web.configuration.filter.RequestObservabilityFilter;
import tech.lamprism.lampray.web.observability.management.ManagementEndpointIds;
import tech.lamprism.lampray.web.observability.management.ManagementExposurePolicy;

import java.util.List;

/**
 * @author RollW
 */
@Configuration
public class ObservabilityConfiguration {
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
    public MeterRegistryContributor micrometerSystemMetricsContributor() {
        return new MicrometerSystemMetricsContributor();
    }

    @Bean
    public StatusAggregator statusAggregator() {
        return new DefaultStatusAggregator();
    }

    @Bean
    public CompositeHealthService compositeHealthService(List<HealthContributor> contributors,
                                                        StatusAggregator statusAggregator) {
        return new CompositeHealthService(contributors, statusAggregator);
    }

    @Bean
    public MeterRegistry observabilityMeterRegistry(List<MeterRegistryContributor> contributors,
                                                    ManagementExposurePolicy exposurePolicy) {
        MeterRegistry meterRegistry = exposurePolicy.isExposed(ManagementEndpointIds.PROMETHEUS)
                ? new PrometheusMeterRegistry(PrometheusConfig.DEFAULT)
                : new SimpleMeterRegistry();
        new MeterRegistryContributors(contributors).contribute(meterRegistry);
        return meterRegistry;
    }

    @Bean
    public ObservationRegistryContributor defaultMeterObservationRegistryContributor(
            MeterRegistry observabilityMeterRegistry) {
        return new DefaultMeterObservationRegistryContributor(observabilityMeterRegistry);
    }

    @Bean
    public ObservationRegistry observationRegistry(List<ObservationRegistryContributor> contributors) {
        ObservationRegistry observationRegistry = ObservationRegistry.create();
        new ObservationRegistryContributors(contributors).contribute(observationRegistry);
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

    private <T extends jakarta.servlet.Filter> FilterRegistrationBean<T> disableServletRegistration(T filter) {
        FilterRegistrationBean<T> registrationBean = new FilterRegistrationBean<>(filter);
        registrationBean.setEnabled(false);
        return registrationBean;
    }
}
