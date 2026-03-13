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

package tech.lamprism.lampray.web.controller.user;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import tech.lamprism.lampray.RequestMetadata;
import tech.lamprism.lampray.security.authentication.UserInfoSignature;
import tech.lamprism.lampray.security.authentication.VerifiableToken;
import tech.lamprism.lampray.security.authentication.login.LoginProvider;
import tech.lamprism.lampray.security.authentication.login.LoginStrategyType;
import tech.lamprism.lampray.security.authentication.registration.RegisterProvider;
import tech.lamprism.lampray.security.authentication.registration.SimpleRegistration;
import tech.lamprism.lampray.security.token.AuthorizationTokenConfigKeys;
import tech.lamprism.lampray.security.token.AuthorizationTokenManager;
import tech.lamprism.lampray.security.token.InheritedAuthorizationScope;
import tech.lamprism.lampray.security.token.MetadataAuthorizationToken;
import tech.lamprism.lampray.security.token.SimpleAuthorizationToken;
import tech.lamprism.lampray.security.token.TokenFormat;
import tech.lamprism.lampray.security.token.TokenSubjectSignKeyProvider;
import tech.lamprism.lampray.security.token.TokenType;
import tech.lamprism.lampray.security.token.UserTokenSubject;
import tech.lamprism.lampray.setting.ConfigReader;
import tech.lamprism.lampray.user.AttributedUser;
import tech.lamprism.lampray.user.AttributedUserDetails;
import tech.lamprism.lampray.user.UserProvider;
import tech.lamprism.lampray.web.common.ParamValidate;
import tech.lamprism.lampray.web.controller.auth.RefreshTokenCookieHelper;
import tech.lamprism.lampray.web.controller.user.model.LoginResponse;
import tech.lamprism.lampray.web.controller.user.model.LoginTokenSendRequest;
import tech.lamprism.lampray.web.controller.user.model.RegisterTokenInfoVo;
import tech.lamprism.lampray.web.controller.user.model.ResendRegisterTokenRequest;
import tech.lamprism.lampray.web.controller.user.model.UserLoginRequest;
import tech.lamprism.lampray.web.controller.user.model.UserRegisterRequest;
import tech.rollw.common.web.HttpResponseEntity;
import tech.rollw.common.web.ParameterFailedException;

import javax.crypto.spec.SecretKeySpec;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.List;
import java.util.Objects;

/**
 * @author RollW
 */
@UserApi
public class LoginRegisterController {
    private static final Logger logger = LoggerFactory.getLogger(LoginRegisterController.class);

    private final LoginProvider loginProvider;
    private final RegisterProvider registerProvider;
    private final AuthorizationTokenManager authorizationTokenManager;
    private final TokenSubjectSignKeyProvider tokenSubjectSignKeyProvider;
    private final ConfigReader configReader;
    private final UserProvider userProvider;

    public LoginRegisterController(LoginProvider loginProvider,
                                   RegisterProvider registerProvider,
                                   AuthorizationTokenManager authorizationTokenManager,
                                   TokenSubjectSignKeyProvider tokenSubjectSignKeyProvider,
                                   ConfigReader configReader,
                                   UserProvider userProvider) {
        this.loginProvider = loginProvider;
        this.registerProvider = registerProvider;
        this.authorizationTokenManager = authorizationTokenManager;
        this.tokenSubjectSignKeyProvider = tokenSubjectSignKeyProvider;
        this.configReader = configReader;
        this.userProvider = userProvider;
    }

    @PostMapping("/login/password")
    public HttpResponseEntity<LoginResponse> loginByPassword(
            RequestMetadata requestMetadata,
            HttpServletRequest request,
            HttpServletResponse response,
            @RequestBody UserLoginRequest loginRequest) {
        // account login, account maybe the username or email
        // needs to check the account type and get the user id
        ParamValidate.notEmpty(loginRequest.identity(), "identity cannot be null or empty.");
        ParamValidate.notEmpty(loginRequest.token(), "token cannot be null or empty.");

        UserInfoSignature userInfoSignature = loginProvider.login(
                loginRequest.identity(),
                loginRequest.token(),
                LoginStrategyType.PASSWORD,
                requestMetadata);
        return loginResponse(request, response, userInfoSignature, loginRequest.rememberMe());
    }

    // TODO: login by email token
    @PostMapping("/login/token/email")
    public HttpResponseEntity<LoginResponse> loginByEmailToken(
            RequestMetadata requestMetadata,
            HttpServletRequest request,
            HttpServletResponse response,
            @RequestBody UserLoginRequest loginRequest) {
        UserInfoSignature signature = loginProvider.login(
                loginRequest.identity(),
                loginRequest.token(),
                LoginStrategyType.EMAIL_TOKEN,
                requestMetadata);
        return loginResponse(request, response, signature, loginRequest.rememberMe());
    }

    @PostMapping("/login/token")
    public HttpResponseEntity<Void> sendEmailLoginToken(
            HttpServletRequest request,
            @RequestBody LoginTokenSendRequest loginTokenSendRequest) throws IOException {
        ParamValidate.notEmpty(loginTokenSendRequest.identity(), "identity cannot be null or empty.");
        loginProvider.sendToken(
                loginTokenSendRequest.identity(),
                LoginStrategyType.EMAIL_TOKEN,
                null
        );
        return HttpResponseEntity.success();
    }

    private HttpResponseEntity<LoginResponse> loginResponse(HttpServletRequest request,
                                                            HttpServletResponse response,
                                                            UserInfoSignature userInfoSignature,
                                                            boolean rememberMe) {
        long accessTokenExpireTime = Objects.requireNonNull(configReader.get(AuthorizationTokenConfigKeys.ACCESS_TOKEN_EXPIRE_TIME));
        long refreshTokenExpireTime = Objects.requireNonNull(configReader.get(AuthorizationTokenConfigKeys.REFRESH_TOKEN_EXPIRE_TIME));

        UserTokenSubject tokenSubject = new UserTokenSubject(userInfoSignature);
        SecretKeySpec signKey = new SecretKeySpec(userInfoSignature.signature().getBytes(StandardCharsets.UTF_8), "HmacSHA256");
        MetadataAuthorizationToken refreshToken = authorizationTokenManager.createToken(
                tokenSubject,
                subject -> signKey,
                TokenType.REFRESH,
                Duration.ofSeconds(refreshTokenExpireTime),
                List.of(InheritedAuthorizationScope.fromSubject(tokenSubject))
        );
        MetadataAuthorizationToken accessToken = authorizationTokenManager.exchangeToken(refreshToken,
                subject -> signKey,
                TokenType.ACCESS,
                Duration.ofSeconds(accessTokenExpireTime),
                List.of(InheritedAuthorizationScope.fromSubject(tokenSubject))
        );

        RefreshTokenCookieHelper.writeRefreshTokenCookie(
                request,
                response,
                refreshToken.getToken(),
                rememberMe,
                Duration.ofSeconds(refreshTokenExpireTime)
        );

        LoginResponse loginResponse = new LoginResponse(
                accessToken, refreshToken,
                userInfoSignature
        );
        return HttpResponseEntity.success(loginResponse);
    }

    @PostMapping("/register")
    public HttpResponseEntity<Void> registerUser(@RequestBody UserRegisterRequest request) {
        AttributedUser user = registerProvider.register(
                new SimpleRegistration(
                        request.username(),
                        request.password(),
                        request.email()
                )
        );
        return HttpResponseEntity.success();
    }

    @PostMapping("/register/token/{token}")
    public HttpResponseEntity<Void> activateUser(@PathVariable String token) {
        ParamValidate.notEmpty(token, "Token cannot be null or empty.");
        registerProvider.verifyRegisterToken(token);
        return HttpResponseEntity.success();
    }

    @PostMapping("/register/token")
    public HttpResponseEntity<Void> resendRegisterToken(
            @Valid @RequestBody ResendRegisterTokenRequest request) {
        AttributedUserDetails user = userProvider.getUser(request.getUsername());
        if (!StringUtils.equalsIgnoreCase(user.getEmail(), request.getEmail())) {
            throw new ParameterFailedException("Email does not match the user.");
        }

        registerProvider.resendRegisterToken(user);
        return HttpResponseEntity.success();
    }

    @GetMapping("/register/token/{token}")
    public HttpResponseEntity<RegisterTokenInfoVo> getRegisterTokenInfo(
            @PathVariable("token") String token) {
        ParamValidate.notEmpty(token, "Token cannot be null or empty.");
        VerifiableToken registerToken = registerProvider.getRegisterToken(token);
        long userId = registerToken.getUserId();
        AttributedUserDetails user = userProvider.getUser(userId);
        if (user == null) {
            throw new ParameterFailedException("Token not valid.");
        }
        return HttpResponseEntity.success(RegisterTokenInfoVo.from(registerToken, user));
    }

    @PostMapping("/logout")
    public HttpResponseEntity<Void> logout(HttpServletRequest request,
                                           HttpServletResponse response) {
        revokeAuthenticatedToken(request);
        RefreshTokenCookieHelper.clearRefreshTokenCookie(request, response);
        return HttpResponseEntity.success();
    }

    private void revokeAuthenticatedToken(HttpServletRequest request) {
        String accessToken = resolveAccessToken(request);
        if (accessToken == null) {
            return;
        }

        try {
            MetadataAuthorizationToken parsedToken = authorizationTokenManager.parseToken(
                    new SimpleAuthorizationToken(accessToken, TokenType.ACCESS),
                    tokenSubjectSignKeyProvider
            );
            authorizationTokenManager.revokeToken(parsedToken);
        } catch (RuntimeException e) {
            logger.warn("Failed to revoke token during logout.", e);
        }
    }

    private String resolveAccessToken(HttpServletRequest request) {
        String authorizationHeader = request.getHeader("Authorization");
        if (StringUtils.isBlank(authorizationHeader)) {
            return null;
        }

        int separatorIndex = authorizationHeader.indexOf(' ');
        if (separatorIndex <= 0 || separatorIndex == authorizationHeader.length() - 1) {
            return null;
        }

        String tokenFormat = authorizationHeader.substring(0, separatorIndex);
        if (!StringUtils.equals(tokenFormat, TokenFormat.BEARER.getValue())) {
            return null;
        }

        return authorizationHeader.substring(separatorIndex + 1);
    }
}
