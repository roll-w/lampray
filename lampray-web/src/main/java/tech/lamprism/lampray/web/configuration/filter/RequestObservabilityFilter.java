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

package tech.lamprism.lampray.web.configuration.filter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.web.servlet.HandlerMapping;
import space.lingu.NonNull;
import tech.lamprism.lampray.observability.CorrelationContext;
import tech.lamprism.lampray.observability.CorrelationContextHolder;
import tech.lamprism.lampray.observability.ObservationDefinition;
import tech.lamprism.lampray.observability.ObservationScope;
import tech.lamprism.lampray.observability.Observability;
import tech.lamprism.lampray.setting.ConfigReader;
import tech.lamprism.lampray.web.common.keys.ObservabilityConfigKeys;
import tech.lamprism.lampray.web.observability.CorrelationMdcSupport;

import java.io.IOException;
import java.util.UUID;

/**
 * @author RollW
 */
@Component
public class RequestObservabilityFilter extends OncePerRequestFilter {
    private final Observability observability;
    private final CorrelationContextHolder correlationContextHolder;
    private final ConfigReader configReader;

    public RequestObservabilityFilter(Observability observability,
                                      CorrelationContextHolder correlationContextHolder,
                                      ConfigReader configReader) {
        this.observability = observability;
        this.correlationContextHolder = correlationContextHolder;
        this.configReader = configReader;
    }

    @Override
    protected void doFilterInternal(@NonNull HttpServletRequest request,
                                    @NonNull HttpServletResponse response,
                                    @NonNull FilterChain filterChain) throws ServletException, IOException {
        CorrelationContext correlationContext = createCorrelationContext(request);
        CorrelationContext previousCorrelationContext = correlationContextHolder.swap(correlationContext);
        CorrelationMdcSupport.replace(correlationContext);
        response.setHeader(resolveRequestIdHeader(), correlationContext.getRequestId());

        ObservationScope scope = observability.openScope(
                ObservationDefinition.system("lampray.http.server.request")
                        .withLowCardinalityTag("method", request.getMethod())
        );
        try {
            filterChain.doFilter(request, response);
            scope.lowCardinalityTag("uri", resolveUriPattern(request));
            scope.lowCardinalityTag("status", Integer.toString(response.getStatus()));
            scope.lowCardinalityTag("result", resolveHttpResult(response.getStatus()));
        } catch (Throwable ex) {
            scope.lowCardinalityTag("uri", resolveUriPattern(request));
            scope.lowCardinalityTag("status", Integer.toString(response.getStatus()));
            scope.lowCardinalityTag("result", "error");
            scope.error(ex);
            if (ex instanceof ServletException servletException) {
                throw servletException;
            }
            if (ex instanceof IOException ioException) {
                throw ioException;
            }
            if (ex instanceof RuntimeException runtimeException) {
                throw runtimeException;
            }
            if (ex instanceof Error error) {
                throw error;
            }
            throw new IllegalStateException(ex);
        } finally {
            scope.close();
            correlationContextHolder.restore(previousCorrelationContext);
            CorrelationMdcSupport.replace(previousCorrelationContext);
        }
    }

    private CorrelationContext createCorrelationContext(HttpServletRequest request) {
        String requestId = request.getHeader(resolveRequestIdHeader());
        if (requestId == null || requestId.trim().isEmpty()) {
            requestId = UUID.randomUUID().toString();
        }
        return CorrelationContext.of(requestId);
    }

    private String resolveRequestIdHeader() {
        return configReader.get(
                ObservabilityConfigKeys.REQUEST_ID_HEADER,
                ObservabilityConfigKeys.DEFAULT_REQUEST_ID_HEADER
        );
    }

    private String resolveHttpResult(int status) {
        if (status >= 500) {
            return "server_error";
        }
        if (status >= 400) {
            return "client_error";
        }
        if (status >= 300) {
            return "redirect";
        }
        return "success";
    }

    private String resolveUriPattern(HttpServletRequest request) {
        Object pattern = request.getAttribute(HandlerMapping.BEST_MATCHING_PATTERN_ATTRIBUTE);
        if (pattern instanceof String value && !value.trim().isEmpty()) {
            return value;
        }
        return "UNKNOWN";
    }
}
