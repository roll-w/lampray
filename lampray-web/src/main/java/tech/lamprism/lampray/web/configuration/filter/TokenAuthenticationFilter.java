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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;
import space.lingu.NonNull;
import tech.lamprism.lampray.security.authentication.adapter.TokenBasedAuthenticationToken;

import java.io.IOException;

/**
 * @author RollW
 */
public class TokenAuthenticationFilter extends OncePerRequestFilter {
    private static final Logger logger = LoggerFactory.getLogger(TokenAuthenticationFilter.class);

    private final AuthenticationManager authenticationManager;

    public TokenAuthenticationFilter(AuthenticationManager authenticationManager) {
        this.authenticationManager = authenticationManager;
    }

    @Override
    protected void doFilterInternal(@NonNull HttpServletRequest request,
                                    @NonNull HttpServletResponse response,
                                    @NonNull FilterChain filterChain) throws ServletException, IOException {
        String loadedToken = loadToken(request);
        if (loadedToken == null) {
            filterChain.doFilter(request, response);
            return;
        }

        Authentication existAuthentication = SecurityContextHolder.getContext().getAuthentication();
        if (existAuthentication != null && existAuthentication.isAuthenticated()) {
            logger.debug("User '{}' is already authenticated", existAuthentication.getName());
            filterChain.doFilter(request, response);
            return;
        }

        Authentication authentication = authenticationManager
                .authenticate(new TokenBasedAuthenticationToken(loadedToken));
        logger.debug("User '{}' authenticated", authentication.getName());
        SecurityContextHolder.getContext().setAuthentication(authentication);
        filterChain.doFilter(request, response);
    }

    private String loadToken(HttpServletRequest request) {
        return request.getHeader("Authorization");
    }
}
