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

package tech.lamprism.lampray.security.firewall

import jakarta.servlet.FilterChain
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.springframework.web.filter.OncePerRequestFilter
import tech.lamprism.lampray.web.common.ApiContext
import tech.lamprism.lampray.web.common.ApiContextAware
import tech.rollw.common.web.system.ContextThreadAware

/**
 * @author RollW
 */
class FirewallFilter(
   private val firewallRegistry: FirewallRegistry,
) : OncePerRequestFilter(), ApiContextAware {

    override fun getFilterName() = "FirewallFilter"

    override fun doFilterInternal(
        request: HttpServletRequest,
        response: HttpServletResponse,
        filterChain: FilterChain
    ) {
        val user = apiContextAware.contextThread.context.user
        val firewallAccessRequest = HttpFirewallAccessRequest(request, user)

        firewallRegistry.filter(firewallAccessRequest)
        filterChain.doFilter(request, response)
    }

    private lateinit var apiContextAware: ContextThreadAware<ApiContext>

    override fun setApiContext(contextAware: ContextThreadAware<ApiContext>) {
        this.apiContextAware = contextAware
    }
}