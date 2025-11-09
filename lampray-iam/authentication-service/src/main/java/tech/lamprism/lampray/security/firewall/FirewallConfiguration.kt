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

import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import tech.lamprism.lampray.security.firewall.filtertable.FileFilterTable
import tech.lamprism.lampray.security.firewall.filtertable.FilterTable
import tech.lamprism.lampray.security.firewall.filtertable.FilterTableFirewall

/**
 * @author RollW
 */
@Configuration
class FirewallConfiguration {

    @Bean
    fun firewallRegistry(firewalls: List<Firewall>): FirewallRegistry {
        return SimpleFirewallRegistry(firewalls)
    }

    @Bean
    fun firewallFilter(firewallRegistry: FirewallRegistry): FirewallFilter {
        return FirewallFilter(firewallRegistry)
    }

    @Bean
    fun filterTableFirewall(filterTable: FilterTable): Firewall {
        return FilterTableFirewall(filterTable)
    }

    @Bean
    fun filterTable(): FilterTable {
        // TODO: Make this configurable
        return FileFilterTable("conf/filtertable")
    }
}