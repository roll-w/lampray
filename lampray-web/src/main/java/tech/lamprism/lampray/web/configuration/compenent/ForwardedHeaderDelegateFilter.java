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

package tech.lamprism.lampray.web.configuration.compenent;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.web.filter.ForwardedHeaderFilter;
import org.springframework.web.filter.OncePerRequestFilter;
import space.lingu.NonNull;

import java.io.IOException;

/**
 * @author RollW
 */
public class ForwardedHeaderDelegateFilter extends OncePerRequestFilter {
    private final ForwardedHeaderFilter forwardedHeaderFilter = new ForwardedHeaderFilter();

    private boolean enabled = true;

    public ForwardedHeaderDelegateFilter() {
        // Default constructor
    }

    public ForwardedHeaderDelegateFilter(boolean enabled) {
        this.enabled = enabled;
    }

    @Override
    protected void doFilterInternal(@NonNull HttpServletRequest request, @NonNull HttpServletResponse response,
                                    @NonNull FilterChain filterChain) throws ServletException, IOException {
        if (enabled) {
            forwardedHeaderFilter.doFilter(request, response, filterChain);
        } else {
            filterChain.doFilter(request, response);
        }
    }
}
