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

package tech.lamprism.lampray.security.authentication.login;

import com.google.common.io.ByteStreams;
import jakarta.mail.internet.MimeMessage;
import org.apache.commons.lang3.RandomStringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.mail.MailProperties;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.core.io.ClassPathResource;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMailMessage;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Component;
import space.lingu.NonNull;
import space.lingu.Nullable;
import tech.lamprism.lampray.mail.util.MimeMailMessageBuilder;
import tech.lamprism.lampray.user.AttributedUser;
import tech.lamprism.lampray.user.AttributedUserDetails;
import tech.rollw.common.web.AuthErrorCode;
import tech.rollw.common.web.ErrorCode;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.text.MessageFormat;
import java.util.Locale;

/**
 * 5-character random alphanumeric string, non case-sensitive.
 *
 * @author RollW
 */
@Component
public class EmailTokenLoginStrategy implements LoginStrategy {
    private static final Logger logger = LoggerFactory.getLogger(EmailTokenLoginStrategy.class);

    private static final String CACHE = "login-email-token";

    private final Cache cache;
    private final MailProperties mailProperties;
    private final JavaMailSender mailSender;

    public EmailTokenLoginStrategy(CacheManager cacheManager,
                                   MailProperties mailProperties,
                                   JavaMailSender mailSender) {
        this.cache = cacheManager.getCache(CACHE);
        this.mailProperties = mailProperties;
        this.mailSender = mailSender;
    }

    private static final String FULL_SEQUENCE = "ABCDEFGHIJKLMNOPQRSTUVWXYZ1234567890";

    @Override
    public LoginVerifiableToken createToken(AttributedUserDetails user) throws LoginTokenException {
        String token = RandomStringUtils.secure().next(5, FULL_SEQUENCE);
        long expireTime = System.currentTimeMillis() + 1000 * 5 * 60;
        // save to cache if it already has a token in it
        LoginConfirmToken oldToken =
                cache.get(user.getUserId(), LoginConfirmToken.class);
        if (oldToken != null) {
            throw new LoginTokenException(AuthErrorCode.ERROR_TOKEN_NOT_EXPIRED);
        }
        LoginConfirmToken confirmToken =
                LoginConfirmToken.emailToken(token, user.getUserId(), expireTime);
        cache.put(user.getUserId(), confirmToken);
        return confirmToken;
    }

    @NonNull
    @Override
    public ErrorCode verify(String token, @NonNull AttributedUserDetails user) {
        if (token == null) {
            return AuthErrorCode.ERROR_INVALID_TOKEN;
        }
        LoginConfirmToken confirmToken =
                cache.get(user.getUserId(), LoginConfirmToken.class);
        if (confirmToken == null) {
            return AuthErrorCode.ERROR_TOKEN_NOT_EXIST;
        }
        if (token.equalsIgnoreCase(confirmToken.token())) {
            cache.evictIfPresent(user.getUserId());
            return AuthErrorCode.SUCCESS;
        }
        return AuthErrorCode.ERROR_TOKEN_NOT_MATCH;
    }

    @Override
    public void sendToken(LoginVerifiableToken token, AttributedUserDetails user,
                          @Nullable Options requestInfo) throws LoginTokenException, IOException {
        if (!(token instanceof LoginConfirmToken confirmToken)) {
            throw new LoginTokenException(AuthErrorCode.ERROR_INVALID_TOKEN);
        }
        MimeMessage mimeMessage = mailSender.createMimeMessage();
        MimeMessageHelper helper = new MimeMessageHelper(mimeMessage);
        MimeMailMessageBuilder builder = new MimeMailMessageBuilder(helper);
        try {
            String text = getMailText(confirmToken.token(), user,
                    requestInfo != null ? requestInfo.getLocale() : null);
            builder.setText(text, true);
        } catch (FileNotFoundException e) {
            logger.error("Mail template not found", e);
            throw e;
        } catch (IOException e) {
            logger.error("Failed to read mail template", e);
            throw e;
        }
        MimeMailMessage message = builder
                .setTo(user.getEmail())
                .setSubject("[Lampray] Login token confirmation")
                .setFrom(mailProperties.getUsername())
                .build();
        mailSender.send(message.getMimeMessage());
    }

    @Override
    public LoginStrategyType getStrategyType() {
        return LoginStrategyType.EMAIL_TOKEN;
    }

    private String getMailText(String token, AttributedUser user, Locale locale) throws IOException {
        String path = getHtmlTemplatePath(locale);
        // TODO: allow set by user in the future

        // 1: web title,
        // 2: user name,
        // 3: token
        // 4: contact email address
        String text = read(path);
        return MessageFormat.format(text,
                "Lampray",
                user.getUsername(),
                token,
                mailProperties.getUsername());
    }

    private String read(String resourcePath) throws IOException {
        ClassPathResource resource = new ClassPathResource(resourcePath);
        if (resource.exists()) {
            byte[] data = ByteStreams.toByteArray(resource.getInputStream());
            return new String(data, StandardCharsets.UTF_8);
        }
        resource = new ClassPathResource("email_templates/email-login-code.html_template");
        if (!resource.exists()) {
            throw new FileNotFoundException("Default resource not found: email_templates/email-login-code.html_template");
        }
        byte[] data = ByteStreams.toByteArray(resource.getInputStream());
        return new String(data, StandardCharsets.UTF_8);
    }

    private String getHtmlTemplatePath(Locale locale) {
        if (locale == null) {
            return "email_templates/email-login-code.html_template";
        }
        return "email_templates/email-login-code_" + locale + ".html_template";
    }

}
