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

package tech.lamprism.lampray.web.configuration;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.context.RequestAttributeSecurityContextRepository;
import org.springframework.security.web.context.SecurityContextRepository;
import tech.lamprism.lampray.setting.ConfigReader;
import tech.lamprism.lampray.web.common.keys.ServerConfigKeys;
import tech.lamprism.lampray.web.configuration.compenent.ForwardedHeaderDelegateFilter;

/**
 * @author RollW
 */
@Configuration
public class SecurityComponentConfiguration {
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public SecurityContextRepository securityContextRepository() {
        return new RequestAttributeSecurityContextRepository();
    }

    @Bean
    public ForwardedHeaderDelegateFilter forwardedHeaderFilter(
            @Qualifier(LocalConfigConfiguration.LOCAL_CONFIG_PROVIDER)
            ConfigReader configReader
    ) {
        Boolean enabled = configReader.get(ServerConfigKeys.PROCESS_PROXY_HEADERS);
        return new ForwardedHeaderDelegateFilter(enabled != null && enabled);
    }
}
