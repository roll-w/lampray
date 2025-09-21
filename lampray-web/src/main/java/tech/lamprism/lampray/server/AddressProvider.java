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

package tech.lamprism.lampray.server;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import jakarta.servlet.http.HttpServletRequest;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import tech.lamprism.lampray.setting.ConfigReader;
import tech.lamprism.lampray.setting.SettingSpecification;
import tech.lamprism.lampray.web.ExternalEndpointProvider;
import tech.lamprism.lampray.web.common.keys.ResourceConfigKeys;
import tech.lamprism.lampray.web.common.keys.ServerConfigKeys;

import java.time.Duration;

/**
 * Elegant address provider with automatic async support.
 * Handles both sync and async scenarios transparently.
 *
 * @author RollW
 */
@Component
public class AddressProvider implements ExternalEndpointProvider {
    private static final Logger logger = LoggerFactory.getLogger(AddressProvider.class);

    private final ConfigReader configReader;

    // Thread-local storage for async context
    private static final ThreadLocal<AsyncContext> asyncContext = new ThreadLocal<>();

    // Simple cache for resolved addresses
    private final Cache<String, String> cache = Caffeine.newBuilder()
            .expireAfterAccess(Duration.ofMinutes(10))
            .maximumSize(100)
            .build();

    public AddressProvider(ConfigReader configReader) {
        this.configReader = configReader;
    }

    @Override
    public String getExternalApiEndpoint() {
        String configured = configReader.get(ServerConfigKeys.HTTP_EXTERNAL_API_ADDRESS);
        if (StringUtils.isBlank(configured)) {
            logger.debug("API external address not configured, using inherited.");
            return resolveAddress(ServerConfigKeys.HTTP_EXTERNAL_API_ADDRESS, ServerConfigKeys.HTTP_EXTERNAL_ADDRESS_INHERITED);
        }
        return resolveAddress(ServerConfigKeys.HTTP_EXTERNAL_API_ADDRESS, configured);
    }

    @Override
    public String getExternalWebEndpoint() {
        if (Boolean.TRUE.equals(configReader.get(ResourceConfigKeys.FRONTEND_ENABLED))) {
            // If frontend is enabled, web endpoint is same as API endpoint
            return getExternalApiEndpoint();
        }
        String configured = configReader.get(ServerConfigKeys.HTTP_EXTERNAL_WEB_ADDRESS);
        if (StringUtils.isEmpty(configured)) {
            return getExternalApiEndpoint();
        }
        return resolveAddress(ServerConfigKeys.HTTP_EXTERNAL_WEB_ADDRESS, configured);
    }

    /**
     * Resolve address with intelligent fallback and caching.
     */
    private String resolveAddress(SettingSpecification<String, String> configKey, String configured) {

        // Use explicit configuration if provided
        if (!ServerConfigKeys.HTTP_EXTERNAL_ADDRESS_INHERITED.equals(configured)) {
            return normalizeAddress(configured);
        }

        // Try to resolve from context with caching
        String cacheKey = configKey.getKey().getName() + "_" + getContextSignature();
        String cached = cache.getIfPresent(cacheKey);
        if (cached != null) {
            return cached;
        }

        String resolved = resolveFromCurrentContext();
        if (resolved == null) {
            resolved = buildFallbackAddress();
        }

        cache.put(cacheKey, resolved);
        return resolved;
    }

    /**
     * Resolve address from current context (sync or async).
     */
    private String resolveFromCurrentContext() {
        // Try current request context first (sync thread)
        HttpServletRequest request = getCurrentRequest();
        if (request != null) {
            return buildAddress(request.getScheme(), request.getServerName(), request.getServerPort());
        }

        // Fallback to async context
        AsyncContext context = asyncContext.get();
        if (context != null) {
            return buildAddress(context.scheme, context.host, context.port);
        }

        return null;
    }

    /**
     * Build address from components.
     */
    private String buildAddress(String scheme, String host, int port) {
        if (scheme == null || host == null) {
            return null;
        }

        boolean isDefaultPort = ("http".equals(scheme) && port == 80) ||
                ("https".equals(scheme) && port == 443);

        return isDefaultPort ?
                scheme + "://" + host :
                scheme + "://" + host + ":" + port;
    }

    /**
     * Build fallback address from configuration.
     */
    private String buildFallbackAddress() {
        String host = configReader.get(ServerConfigKeys.HTTP_HOST);
        Integer port = configReader.get(ServerConfigKeys.HTTP_PORT);

        String scheme = "http";
        String hostname = StringUtils.isNotEmpty(host) ? host : "localhost";
        int actualPort = (port != null && port > 0) ? port : 5100;

        return buildAddress(scheme, hostname, actualPort);
    }

    /**
     * Get current HTTP request from Spring context.
     */
    private HttpServletRequest getCurrentRequest() {
        try {
            ServletRequestAttributes attrs = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
            return attrs != null ? attrs.getRequest() : null;
        } catch (IllegalStateException e) {
            return null;
        }
    }

    /**
     * Generate context signature for caching.
     */
    private String getContextSignature() {
        HttpServletRequest request = getCurrentRequest();
        if (request != null) {
            return request.getScheme() + "_" + request.getServerName() + "_" + request.getServerPort();
        }

        AsyncContext context = asyncContext.get();
        if (context != null) {
            return context.scheme + "_" + context.host + "_" + context.port;
        }

        return "default";
    }

    /**
     * Normalize address by removing trailing slash.
     */
    private String normalizeAddress(String address) {
        if (address == null || address.isEmpty()) {
            return "";
        }
        return address.endsWith("/") ? address.substring(0, address.length() - 1) : address;
    }

    /**
     * Capture current request context for async use.
     */
    public AsyncContext captureContext() {
        HttpServletRequest request = getCurrentRequest();
        if (request != null) {
            return new AsyncContext(
                    request.getScheme(),
                    request.getServerName(),
                    request.getServerPort()
            );
        }
        return null;
    }

    /**
     * Set async context in current thread.
     */
    public void setAsyncContext(AsyncContext context) {
        asyncContext.set(context);
    }

    /**
     * Clear async context from current thread.
     */
    public void clearAsyncContext() {
        asyncContext.remove();
    }

    /**
     * Async context holder.
     */
    public static class AsyncContext {
        final String scheme;
        final String host;
        final int port;

        public AsyncContext(String scheme, String host, int port) {
            this.scheme = scheme;
            this.host = host;
            this.port = port;
        }

        @Override
        public String toString() {
            return scheme + "://" + host + ":" + port;
        }
    }
}
