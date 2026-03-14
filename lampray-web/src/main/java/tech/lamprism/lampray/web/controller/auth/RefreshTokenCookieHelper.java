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

package tech.lamprism.lampray.web.controller.auth;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;

import java.time.Duration;

/**
 * Helper for keeping refresh tokens out of JavaScript-accessible storage.
 *
 * @author RollW
 */
public final class RefreshTokenCookieHelper {
    public static final String COOKIE_NAME = "lampray_refresh_token";
    public static final String COOKIE_PATH = "/api/v1/auth";
    private static final String SAME_SITE = "Strict";

    private RefreshTokenCookieHelper() {
    }

    public static void writeRefreshTokenCookie(HttpServletRequest request,
                                               HttpServletResponse response,
                                               String refreshToken,
                                               boolean rememberMe,
                                               Duration maxAge) {
        ResponseCookie.ResponseCookieBuilder builder = newCookieBuilder(request, refreshToken);
        if (rememberMe) {
            builder.maxAge(maxAge);
        }
        response.addHeader(HttpHeaders.SET_COOKIE, builder.build().toString());
    }

    public static void clearRefreshTokenCookie(HttpServletRequest request,
                                               HttpServletResponse response) {
        response.addHeader(HttpHeaders.SET_COOKIE, newCookieBuilder(request, "")
                .maxAge(Duration.ZERO)
                .build()
                .toString());
    }

    public static String resolveRefreshToken(HttpServletRequest request) {
        Cookie[] cookies = request.getCookies();
        if (ArrayUtils.isEmpty(cookies)) {
            return null;
        }

        for (Cookie cookie : cookies) {
            if (StringUtils.equals(cookie.getName(), COOKIE_NAME) && StringUtils.isNotBlank(cookie.getValue())) {
                return cookie.getValue();
            }
        }
        return null;
    }

    private static ResponseCookie.ResponseCookieBuilder newCookieBuilder(HttpServletRequest request,
                                                                         String value) {
        return ResponseCookie.from(COOKIE_NAME, value)
                .httpOnly(true)
                .secure(request.isSecure())
                .path(COOKIE_PATH)
                .sameSite(SAME_SITE);
    }
}
