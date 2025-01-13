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

package tech.lamprism.lampray.web.configuration.filter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.http.HttpMethod;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import space.lingu.NonNull;
import tech.lamprism.lampray.user.AttributedUser;
import tech.lamprism.lampray.web.common.ApiContext;
import tech.lamprism.lampray.web.util.RequestUtils;
import tech.rollw.common.web.system.ContextThread;
import tech.rollw.common.web.system.ContextThreadAware;

import java.io.IOException;
import java.time.OffsetDateTime;

/**
 * @author RollW
 */
@Component
public class ApiContextInitializeFilter extends OncePerRequestFilter {
    private final ContextThreadAware<ApiContext> apiContextThreadAware;

    public ApiContextInitializeFilter(ContextThreadAware<ApiContext> apiContextThreadAware) {
        this.apiContextThreadAware = apiContextThreadAware;
    }

    @Override
    protected void doFilterInternal(@NonNull HttpServletRequest request,
                                    @NonNull HttpServletResponse response,
                                    @NonNull FilterChain filterChain) throws ServletException, IOException {
        ContextThread<ApiContext> contextThread =
                apiContextThreadAware.assambleContextThread(null);
        ApiContext apiContext = createApiContext(request, tryGetUser());
        contextThread.setContext(apiContext);
        try {
            filterChain.doFilter(request, response);
        } finally {
            contextThread.clearContext();
        }
    }

    private AttributedUser tryGetUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null) {
            return null;
        }
        if (!(authentication.getPrincipal() instanceof AttributedUser user)) {
            return null;
        }
        return user;
    }

    private ApiContext createApiContext(HttpServletRequest request, AttributedUser user) {
        String requestUri = request.getRequestURI();
        HttpMethod method = HttpMethod.valueOf(request.getMethod());
        String ip = RequestUtils.getRemoteIpAddress(request);
        OffsetDateTime timestamp = OffsetDateTime.now();
        return new ApiContext(
                ip,
                LocaleContextHolder.getLocale(),
                method, user, timestamp,
                requestUri
        );
    }
}
