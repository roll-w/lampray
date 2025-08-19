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
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.ProviderManager;
import org.springframework.security.config.annotation.authentication.configuration.EnableGlobalAuthentication;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityCustomizer;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.core.GrantedAuthorityDefaults;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.web.context.SecurityContextHolderFilter;
import org.springframework.security.web.context.SecurityContextRepository;
import org.springframework.security.web.firewall.ExceptionResolveRequestRejectedHandler;
import org.springframework.security.web.firewall.RequestRejectedHandler;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.springframework.web.servlet.HandlerExceptionResolver;
import tech.lamprism.lampray.security.authentication.adapter.PreUserAuthenticationProvider;
import tech.lamprism.lampray.security.authentication.adapter.TokenBasedAuthenticationProvider;
import tech.lamprism.lampray.security.authorization.PrivilegedUserProvider;
import tech.lamprism.lampray.security.firewall.FirewallFilter;
import tech.lamprism.lampray.security.token.AuthorizationTokenManager;
import tech.lamprism.lampray.security.token.TokenSignKeyProvider;
import tech.lamprism.lampray.web.configuration.compenent.ForwardedHeaderDelegateFilter;
import tech.lamprism.lampray.web.configuration.compenent.WebDelegateSecurityHandler;
import tech.lamprism.lampray.web.configuration.filter.ApiContextInitializeFilter;
import tech.lamprism.lampray.web.configuration.filter.CorsConfigFilter;
import tech.lamprism.lampray.web.configuration.filter.TokenAuthenticationFilter;

import java.time.Duration;
import java.time.temporal.ChronoUnit;
import java.util.Arrays;
import java.util.List;

/**
 * @author RollW
 */
@Configuration
@EnableWebSecurity
@EnableGlobalAuthentication
@EnableMethodSecurity
public class WebSecurityConfiguration {

    public WebSecurityConfiguration() {
    }

    @Bean
    public WebSecurityCustomizer webSecurityCustomizer(RequestRejectedHandler requestRejectedHandler) {
        // TODO: remove this in production
        return (web) -> web
                .debug(false)
                .requestRejectedHandler(requestRejectedHandler);
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity security,
                                                   SecurityContextRepository securityContextRepository,
                                                   CorsConfigFilter corsConfigFilter,
                                                   TokenAuthenticationFilter tokenAuthenticationFilter,
                                                   ApiContextInitializeFilter apiContextInitializeFilter,
                                                   FirewallFilter firewallFilter,
                                                   ForwardedHeaderDelegateFilter forwardedHeaderFilter,
                                                   AuthenticationEntryPoint authenticationEntryPoint,
                                                   AccessDeniedHandler accessDeniedHandler) throws Exception {
        security.csrf(AbstractHttpConfigurer::disable);
        security.cors(configurer -> configurer
                .configurationSource(corsConfigurationSource())
        );
        security.securityContext(configurer ->
                configurer.securityContextRepository(securityContextRepository));
        security.authorizeHttpRequests(configurer -> configurer
                .requestMatchers(HttpMethod.OPTIONS).permitAll()
                .requestMatchers("/api/{version}/auth/token/**").permitAll()
                .requestMatchers("/api/{version}/admin/**").hasAnyAuthority("role:ADMIN")
                .requestMatchers("/api/{version}/message/**").hasAnyAuthority("role:USER")
                .requestMatchers("/api/{version}/{userId}/review/**").hasAnyAuthority("role:ADMIN", "role:REVIEWER")
                .requestMatchers("/api/{version}/common/**").permitAll()
                .requestMatchers(HttpMethod.GET, "/api/{version}/storages/{id}").permitAll()
                .requestMatchers(HttpMethod.GET, "/api/{version}/admin/**").hasAnyAuthority("role:ADMIN")
                .requestMatchers(HttpMethod.GET, "/api/{version}/{userId}/review").hasAnyAuthority("role:ADMIN", "role:REVIEWER")
                .requestMatchers(HttpMethod.GET).permitAll()
                .requestMatchers("/api/{version}/user/login/**").permitAll()
                .requestMatchers("/api/{version}/user/register/**").permitAll()
                .requestMatchers("/api/{version}/user/logout/**").permitAll()
                .requestMatchers("/api/**").hasAnyAuthority("USER", "role:USER")
                .requestMatchers("/static/images/**").permitAll()
                .requestMatchers(HttpMethod.GET, "/**").permitAll()
                .anyRequest().hasAnyAuthority("USER", "role:USER")
        );

        security.exceptionHandling(configurer -> configurer
                .authenticationEntryPoint(authenticationEntryPoint)
                .accessDeniedHandler(accessDeniedHandler)
        );

        security.sessionManagement(configurer -> {
            configurer.sessionCreationPolicy(SessionCreationPolicy.STATELESS);
        });
        security.addFilterBefore(tokenAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);
        security.addFilterAfter(apiContextInitializeFilter, TokenAuthenticationFilter.class);
        security.addFilterAfter(firewallFilter, ApiContextInitializeFilter.class);
        security.addFilterBefore(corsConfigFilter, SecurityContextHolderFilter.class);
        security.addFilterBefore(forwardedHeaderFilter, CorsConfigFilter.class);
        return security.build();
    }

    @Bean
    public AuthenticationManager authenticationManager(
            List<AuthenticationProvider> authenticationProviders) {
        return new ProviderManager(authenticationProviders);
    }

    @Bean
    public CorsConfigFilter corsConfigFilter(
            @Qualifier("handlerExceptionResolver") HandlerExceptionResolver resolver) {
        return new CorsConfigFilter(resolver);
    }

    @Bean
    public TokenAuthenticationFilter tokenAuthenticationFilter(
            AuthenticationManager authenticationManager
    ) {
        return new TokenAuthenticationFilter(authenticationManager);
    }

    @Bean
    public TokenBasedAuthenticationProvider tokenBasedAuthenticationProvider(
            AuthorizationTokenManager authorizationTokenManager,
            PrivilegedUserProvider privilegedUserProvider,
            TokenSignKeyProvider tokenSignKeyProvider) {
        return new TokenBasedAuthenticationProvider(
                authorizationTokenManager,
                privilegedUserProvider,
                tokenSignKeyProvider
        );
    }

    @Bean
    public PreUserAuthenticationProvider preUserAuthenticationProvider(
            PrivilegedUserProvider privilegedUserProvider) {
        return new PreUserAuthenticationProvider(privilegedUserProvider);
    }

    @Bean
    public WebDelegateSecurityHandler webDelegateSecurityHandler(
            @Qualifier("handlerExceptionResolver") HandlerExceptionResolver resolver) {
        return new WebDelegateSecurityHandler(resolver);
    }

    @Bean
    public RequestRejectedHandler requestRejectedHandler(
            @Qualifier("handlerExceptionResolver") HandlerExceptionResolver resolver) {
        return new ExceptionResolveRequestRejectedHandler(resolver);
    }

    @Bean
    public GrantedAuthorityDefaults grantedAuthorityDefaults() {
        return new GrantedAuthorityDefaults(""); // removes the "ROLE_" prefix
    }

    private CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();

        configuration.addAllowedOriginPattern("*");
        configuration.setAllowedMethods(Arrays.stream(HttpMethod.values())
                .map(HttpMethod::name)
                .toList()
        );
        configuration.setAllowedHeaders(List.of("*"));
        configuration.setExposedHeaders(List.of("*"));
        configuration.setAllowCredentials(true);
        configuration.setMaxAge(Duration.of(1, ChronoUnit.MINUTES));

        final UrlBasedCorsConfigurationSource source =
                new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);

        return source;
    }

}
