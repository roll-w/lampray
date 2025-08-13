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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import tech.lamprism.lampray.RequestMetadata;
import tech.lamprism.lampray.authentication.SecurityConfigKeys;
import tech.lamprism.lampray.authentication.UserInfoSignature;
import tech.lamprism.lampray.authentication.login.LoginProvider;
import tech.lamprism.lampray.authentication.login.LoginStrategyType;
import tech.lamprism.lampray.security.authentication.registration.RegisterProvider;
import tech.lamprism.lampray.security.authentication.registration.SimpleRegistration;
import tech.lamprism.lampray.security.token.AuthorizationToken;
import tech.lamprism.lampray.security.token.AuthorizationTokenProvider;
import tech.lamprism.lampray.security.token.AuthorizationTokenUtils;
import tech.lamprism.lampray.setting.ConfigReader;
import tech.lamprism.lampray.user.AttributedUser;
import tech.lamprism.lampray.user.AttributedUserDetails;
import tech.lamprism.lampray.user.UserProvider;
import tech.lamprism.lampray.web.common.ParamValidate;
import tech.lamprism.lampray.web.controller.user.model.LoginResponse;
import tech.lamprism.lampray.web.controller.user.model.LoginTokenSendRequest;
import tech.lamprism.lampray.web.controller.user.model.UserLoginRequest;
import tech.lamprism.lampray.web.controller.user.model.UserRegisterRequest;
import tech.rollw.common.web.HttpResponseEntity;

import java.io.IOException;
import java.time.Duration;
import java.util.List;

/**
 * @author RollW
 */
@UserApi
public class LoginRegisterController {
    private static final Logger logger = LoggerFactory.getLogger(LoginRegisterController.class);

    private final LoginProvider loginProvider;
    private final RegisterProvider registerProvider;
    private final AuthorizationTokenProvider authorizationTokenProvider;
    private final ConfigReader configReader;
    private final UserProvider userProvider;

    public LoginRegisterController(LoginProvider loginProvider,
                                   RegisterProvider registerProvider,
                                   AuthorizationTokenProvider authorizationTokenProvider,
                                   ConfigReader configReader,
                                   UserProvider userProvider) {
        this.loginProvider = loginProvider;
        this.registerProvider = registerProvider;
        this.configReader = configReader;
        this.userProvider = userProvider;
        this.authorizationTokenProvider = authorizationTokenProvider;
    }

    @PostMapping("/login/password")
    public HttpResponseEntity<LoginResponse> loginByPassword(
            RequestMetadata requestMetadata,
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
        return loginResponse(userInfoSignature);
    }

    // TODO: login by email token
    @PostMapping("/login/token/email")
    public HttpResponseEntity<LoginResponse> loginByEmailToken(
            RequestMetadata requestMetadata,
            @RequestBody UserLoginRequest loginRequest) {
        UserInfoSignature signature = loginProvider.login(
                loginRequest.identity(),
                loginRequest.token(),
                LoginStrategyType.EMAIL_TOKEN,
                requestMetadata);
        return loginResponse(signature);
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

    private HttpResponseEntity<LoginResponse> loginResponse(UserInfoSignature userInfoSignature) {
        Long expireTime = configReader.get(SecurityConfigKeys.TOKEN_EXPIRE_TIME);
        AuthorizationToken token = authorizationTokenProvider.createToken(
                userInfoSignature,
                userId -> userInfoSignature.signature(),
                Duration.ofSeconds(expireTime),
                List.of()
        );
        LoginResponse response = new LoginResponse(
                AuthorizationTokenUtils.toHeaderValue(token),
                userInfoSignature
        );
        return HttpResponseEntity.success(response);
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
            @RequestParam("username") String username) {
        ParamValidate.notEmpty(username,
                "Username cannot be null or empty.");

        AttributedUserDetails user = userProvider.getUser(username);
        registerProvider.resendRegisterToken(user);

        return HttpResponseEntity.success();
    }

    @PostMapping("/logout")
    public HttpResponseEntity<Void> logout(HttpServletRequest request) {
        return HttpResponseEntity.success();
    }
}
