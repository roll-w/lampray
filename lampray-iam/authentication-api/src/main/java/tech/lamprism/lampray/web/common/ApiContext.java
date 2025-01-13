/*
 * Copyright (C) 2023 RollW
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

package tech.lamprism.lampray.web.common;

import org.springframework.http.HttpMethod;
import space.lingu.NonNull;
import space.lingu.Nullable;
import tech.lamprism.lampray.user.AttributedUser;
import tech.rollw.common.web.system.SystemContext;

import java.time.OffsetDateTime;
import java.util.Locale;

/**
 * @author RollW
 */
public class ApiContext implements SystemContext {
    private final String ip;
    private final Locale locale;
    private final HttpMethod method;
    private final AttributedUser user;
    private final OffsetDateTime timestamp;
    private final String requestUri;

    public ApiContext(String ip, Locale locale,
                      HttpMethod method, AttributedUser user,
                      OffsetDateTime timestamp, String requestUri) {
        this.ip = ip;
        this.locale = locale;
        this.method = method;
        this.user = user;
        this.timestamp = timestamp;
        this.requestUri = requestUri;
    }

    @Override
    public Object getObject(@NonNull String key) {
        return switch (key) {
            case "ip" -> ip;
            case "locale" -> locale;
            case "method" -> method;
            case "user" -> user;
            default -> null;
        };
    }

    public boolean hasUser() {
        return user != null;
    }

    public String getIp() {
        return ip;
    }

    public Locale getLocale() {
        return locale;
    }

    public HttpMethod getMethod() {
        return method;
    }

    @Nullable
    public AttributedUser getUser() {
        return user;
    }

    public OffsetDateTime getTimestamp() {
        return timestamp;
    }

    public String getRequestUri() {
        return requestUri;
    }

    public ApiContext fork(AttributedUser user) {
        if (user == this.user) {
            return this;
        }
        return new ApiContext(ip, locale, method, user, timestamp, requestUri);
    }


    @Override
    public String toString() {
        return "ApiContext{" +
                ", ip='" + ip + '\'' +
                ", locale=" + locale +
                ", method=" + method +
                ", user=" + user +
                ", timestamp=" + timestamp +
                ", requestUri='" + requestUri + '\'' +
                '}';
    }
}
