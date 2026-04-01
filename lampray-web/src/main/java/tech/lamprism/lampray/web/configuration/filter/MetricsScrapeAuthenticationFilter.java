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
import space.lingu.NonNull;
import tech.lamprism.lampray.setting.ConfigReader;
import tech.lamprism.lampray.web.common.keys.ObservabilityConfigKeys;

import java.io.IOException;
import java.util.Objects;

/**
 * @author RollW
 */
@Component
public class MetricsScrapeAuthenticationFilter extends OncePerRequestFilter {
    private static final String BEARER_PREFIX = "Bearer ";

    private final ConfigReader configReader;

    public MetricsScrapeAuthenticationFilter(ConfigReader configReader) {
        this.configReader = configReader;
    }

    @Override
    protected boolean shouldNotFilter(@NonNull HttpServletRequest request) {
        return !"/metrics".equals(request.getServletPath());
    }

    @Override
    protected void doFilterInternal(@NonNull HttpServletRequest request,
                                    @NonNull HttpServletResponse response,
                                    @NonNull FilterChain filterChain) throws ServletException, IOException {
        String configuredToken = configuredToken();
        if (configuredToken.isEmpty()) {
            response.sendError(HttpServletResponse.SC_NOT_FOUND);
            return;
        }

        String authorization = request.getHeader("Authorization");
        if (authorization == null || !authorization.startsWith(BEARER_PREFIX)) {
            reject(response);
            return;
        }

        String providedToken = authorization.substring(BEARER_PREFIX.length()).trim();
        if (!Objects.equals(configuredToken, providedToken)) {
            reject(response);
            return;
        }

        filterChain.doFilter(request, response);
    }

    private String configuredToken() {
        String token = configReader.get(
                ObservabilityConfigKeys.METRICS_SCRAPE_TOKEN,
                ObservabilityConfigKeys.DEFAULT_METRICS_SCRAPE_TOKEN
        );
        return token == null ? "" : token.trim();
    }

    private void reject(HttpServletResponse response) throws IOException {
        response.setHeader("WWW-Authenticate", "Bearer realm=\"metrics\"");
        response.sendError(HttpServletResponse.SC_UNAUTHORIZED);
    }
}
