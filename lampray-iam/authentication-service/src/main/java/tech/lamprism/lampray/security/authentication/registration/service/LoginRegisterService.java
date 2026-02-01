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

package tech.lamprism.lampray.security.authentication.registration.service;

import com.google.common.base.Preconditions;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import space.lingu.NonNull;
import tech.lamprism.lampray.LampException;
import tech.lamprism.lampray.RequestMetadata;
import tech.lamprism.lampray.security.authentication.UserInfoSignature;
import tech.lamprism.lampray.security.authentication.VerifiableToken;
import tech.lamprism.lampray.security.authentication.adapter.PreUserAuthenticationToken;
import tech.lamprism.lampray.security.authentication.login.LoginProvider;
import tech.lamprism.lampray.security.authentication.login.LoginStrategy;
import tech.lamprism.lampray.security.authentication.login.LoginStrategyType;
import tech.lamprism.lampray.security.authentication.login.LoginVerifiableToken;
import tech.lamprism.lampray.security.authentication.login.OnUserLoginEvent;
import tech.lamprism.lampray.security.authentication.registration.OnUserRegistrationEvent;
import tech.lamprism.lampray.security.authentication.registration.RegisterProvider;
import tech.lamprism.lampray.security.authentication.registration.RegisterTokenProvider;
import tech.lamprism.lampray.security.authentication.registration.RegisterVerificationToken;
import tech.lamprism.lampray.security.authentication.registration.Registration;
import tech.lamprism.lampray.security.authentication.registration.RegistrationInterceptor;
import tech.lamprism.lampray.security.authentication.registration.repository.RegisterTokenDo;
import tech.lamprism.lampray.security.authentication.registration.repository.RegisterTokenRepository;
import tech.lamprism.lampray.security.firewall.FirewallAccessRequest;
import tech.lamprism.lampray.security.firewall.FirewallRegistry;
import tech.lamprism.lampray.user.AttributedUser;
import tech.lamprism.lampray.user.AttributedUserDetails;
import tech.lamprism.lampray.user.UserIdentity;
import tech.lamprism.lampray.user.UserManageService;
import tech.lamprism.lampray.user.UserOperator;
import tech.lamprism.lampray.user.UserProvider;
import tech.lamprism.lampray.user.UserSignatureProvider;
import tech.lamprism.lampray.user.UserTrait;
import tech.lamprism.lampray.user.UserViewException;
import tech.rollw.common.web.AuthErrorCode;
import tech.rollw.common.web.ErrorCode;
import tech.rollw.common.web.IoErrorCode;
import tech.rollw.common.web.UserErrorCode;
import tech.rollw.common.web.system.AuthenticationException;
import tech.rollw.common.web.system.SystemResourceOperatorProvider;

import java.io.IOException;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * @author RollW
 */
@Service
public class LoginRegisterService implements LoginProvider, RegisterTokenProvider, RegisterProvider {
    private static final Logger logger = LoggerFactory.getLogger(LoginRegisterService.class);

    private final RegisterTokenRepository registerTokenRepository;
    private final FirewallRegistry firewallRegistry;
    private final SystemResourceOperatorProvider<Long> systemResourceOperatorProvider;
    private final UserProvider userProvider;
    private final UserManageService userManageService;
    private final ApplicationEventPublisher eventPublisher;
    private final AuthenticationManager authenticationManager;
    private final UserSignatureProvider userSignatureProvider;
    private final List<RegistrationInterceptor> registrationInterceptors;
    private final Map<LoginStrategyType, LoginStrategy> loginStrategyMap =
            new EnumMap<>(LoginStrategyType.class);

    public LoginRegisterService(List<LoginStrategy> strategies,
                                RegisterTokenRepository registerTokenRepository,
                                FirewallRegistry firewallRegistry,
                                SystemResourceOperatorProvider<Long> systemResourceOperatorProvider,
                                UserProvider userProvider,
                                UserManageService userManageService,
                                ApplicationEventPublisher eventPublisher,
                                AuthenticationManager authenticationManager,
                                UserSignatureProvider userSignatureProvider,
                                List<RegistrationInterceptor> registrationInterceptors) {
        this.registerTokenRepository = registerTokenRepository;
        this.firewallRegistry = firewallRegistry;
        this.systemResourceOperatorProvider = systemResourceOperatorProvider;
        this.userProvider = userProvider;
        this.userManageService = userManageService;
        this.eventPublisher = eventPublisher;
        this.authenticationManager = authenticationManager;
        this.userSignatureProvider = userSignatureProvider;
        this.registrationInterceptors = registrationInterceptors;
        strategies.forEach(strategy ->
                loginStrategyMap.put(strategy.getStrategyType(), strategy));
    }

    @NonNull
    public LoginStrategy getLoginStrategy(@NonNull LoginStrategyType type) {
        return loginStrategyMap.get(type);
    }

    public void sendToken(long userId,
                          LoginStrategyType type,
                          RequestMetadata requestMetadata) throws IOException {
        LoginStrategy strategy = getLoginStrategy(type);
        AttributedUserDetails user = userProvider.getUser(userId);
        LoginVerifiableToken token = strategy.createToken(user);
        LoginStrategy.Options requestInfo = new LoginStrategy.Options(LocaleContextHolder.getLocale());
        sendToken(strategy, token, user, requestInfo);
    }

    @Override
    public void sendToken(@NonNull String identity,
                          @NonNull LoginStrategyType type,
                          RequestMetadata requestMetadata) {
        LoginStrategy strategy = getLoginStrategy(type);
        AttributedUserDetails user = tryGetUser(identity);
        LoginVerifiableToken token = strategy.createToken(user);
        LoginStrategy.Options requestInfo = new LoginStrategy.Options(LocaleContextHolder.getLocale());
        sendToken(strategy, token, user, requestInfo);
    }

    private void sendToken(LoginStrategy strategy,
                           LoginVerifiableToken token,
                           AttributedUserDetails user,
                           LoginStrategy.Options requestInfo) {
        try {
            strategy.sendToken(token, user, requestInfo);
        } catch (IOException e) {
            logger.error("Failed to send token to user: {}@{}, due to: {}",
                    user.getUserId(), user.getUsername(), e.getMessage(), e);
            throw new LampException(IoErrorCode.ERROR_IO);
        }
    }

    private ErrorCode verifyToken(String token,
                                  AttributedUserDetails user,
                                  LoginStrategyType type) {
        LoginStrategy strategy = getLoginStrategy(type);
        return strategy.verify(token, user);
    }

    private AttributedUserDetails tryGetUser(String identity) {
        if (identity.contains("@")) {
            return userProvider.getUserByEmail(identity);
        }
        return userProvider.getUser(identity);
    }

    @Override
    @NonNull
    public UserInfoSignature login(@NonNull String identity,
                                   @NonNull String token,
                                   @NonNull LoginStrategyType type,
                                   RequestMetadata metadata) {
        Preconditions.checkNotNull(identity, "identity cannot be null");
        Preconditions.checkNotNull(token, "token cannot be null");

        AttributedUserDetails user = tryGetUser(identity);
        if (user == null) {
            throw new UserViewException(UserErrorCode.ERROR_USER_NOT_EXIST);
        }
        ErrorCode code = verifyToken(token, user, type);
        if (code.failed()) {
            throw new UserViewException(code);
        }

        firewallFilter(user);

        PreUserAuthenticationToken authenticationToken = new PreUserAuthenticationToken(user);
        Authentication authenticatedToken = authenticationManager.authenticate(authenticationToken);
        SecurityContextHolder.getContext().setAuthentication(authenticatedToken);

        OnUserLoginEvent onUserLoginEvent = new OnUserLoginEvent(user, metadata);
        eventPublisher.publishEvent(onUserLoginEvent);
        String signature = userSignatureProvider.getSignature(user.getUserId());
        return UserInfoSignature.from(user, signature);
    }


    @Override
    @NonNull
    public AttributedUser register(@NonNull final Registration registration) {
        Registration iter = registration;
        for (RegistrationInterceptor interceptor : registrationInterceptors) {
            iter = interceptor.preRegistration(registration);
        }

        AttributedUser user = userManageService.createUser(
                iter.getUsername(), iter.getPassword(),
                iter.getEmail(), iter.getRole(), iter.getEnabled()
        );

        // TODO: replace with UserNotifier to send email
        OnUserRegistrationEvent event = new OnUserRegistrationEvent(user, LocaleContextHolder.getLocale());
        eventPublisher.publishEvent(event);

        logger.info("Register username: {}, email: {}, role: {}, id: {}",
                user.getUsername(), user.getEmail(),
                user.getRole(),
                user.getUserId()
        );
        return user;
    }

    private void firewallFilter(AttributedUser user) {
        // Filter by ip or path has been executed before in FirewallFilter,
        // here we just check if the user can access the system.
        FirewallAccessRequest request = new UserLoginFirewallAccessRequest(user);
        firewallRegistry.filter(request);
    }

    @Override
    public void logout() {
        SecurityContextHolder.clearContext();
    }

    @Override
    public VerifiableToken createRegisterToken(UserIdentity userIdentity) {
        UUID uuid = UUID.randomUUID();
        String token = uuid.toString();
        long expiryTime = RegisterVerificationToken.calculateExpiryDate();
        RegisterTokenDo registerVerificationToken = new RegisterTokenDo(
                null, token, userIdentity.getUserId(), expiryTime, false
        );
        registerVerificationToken = registerTokenRepository.save(registerVerificationToken);
        return registerVerificationToken.lock();
    }

    @Override
    public RegisterVerificationToken getRegisterToken(String token) {
        RegisterTokenDo registerTokenDo = registerTokenRepository.findByToken(token)
                .orElseThrow(() -> new AuthenticationException(AuthErrorCode.ERROR_TOKEN_NOT_EXIST));
        if (registerTokenDo.isExpired() || registerTokenDo.getUsed()) {
            throw new AuthenticationException(AuthErrorCode.ERROR_TOKEN_NOT_EXIST);
        }
        return registerTokenDo.lock();
    }

    @Override
    public void resendRegisterToken(UserIdentity user) {
        AttributedUser attributedUser = retrieveUser(user);
        OnUserRegistrationEvent event = new OnUserRegistrationEvent(
                attributedUser, LocaleContextHolder.getLocale()
        );
        eventPublisher.publishEvent(event);
    }

    private AttributedUser retrieveUser(UserTrait user) {
        if (user instanceof AttributedUser attributedUser) {
            return attributedUser;
        }
        return userProvider.getUser(user);
    }

    @Override
    public void verifyRegisterToken(String token) {
        RegisterTokenDo registerTokenDo = registerTokenRepository.findByToken(token)
                .orElseThrow(() -> new AuthenticationException(AuthErrorCode.ERROR_TOKEN_NOT_EXIST));
        if (registerTokenDo.getUsed()) {
            throw new AuthenticationException(AuthErrorCode.ERROR_TOKEN_USED);
        }
        if (registerTokenDo.isExpired()) {
            throw new AuthenticationException(AuthErrorCode.ERROR_TOKEN_EXPIRED);
        }
        registerTokenDo.markVerified();
        registerTokenRepository.save(registerTokenDo);
        UserOperator userOperator = systemResourceOperatorProvider.getSystemResourceOperator(
                        UserTrait.of(registerTokenDo.getUserId()),
                        true)
                .cast(UserOperator.class);
        if (userOperator.isCanceled()) {
            throw new AuthenticationException(UserErrorCode.ERROR_USER_CANCELED);
        }
        if (userOperator.isEnabled()) {
            throw new AuthenticationException(UserErrorCode.ERROR_USER_ALREADY_ACTIVATED);
        }
        userOperator.disableAutoUpdate()
                .setEnabled(true)
                .update();
    }
}
