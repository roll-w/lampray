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

package tech.lamprism.lampray.web.observability.management;

import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;

import java.util.Objects;

/**
 * @author RollW
 */
public final class ManagementResponse {
    private final HttpStatus status;
    private final MediaType mediaType;
    private final Object body;

    private ManagementResponse(HttpStatus status,
                               MediaType mediaType,
                               Object body) {
        this.status = Objects.requireNonNull(status, "status cannot be null");
        this.mediaType = Objects.requireNonNull(mediaType, "mediaType cannot be null");
        this.body = body;
    }

    public static ManagementResponse okJson(Object body) {
        return new ManagementResponse(HttpStatus.OK, MediaType.APPLICATION_JSON, body);
    }

    public static ManagementResponse okText(String body) {
        return new ManagementResponse(HttpStatus.OK, MediaType.TEXT_PLAIN, body);
    }

    public static ManagementResponse status(HttpStatus status,
                                            MediaType mediaType,
                                            Object body) {
        return new ManagementResponse(status, mediaType, body);
    }

    public HttpStatus getStatus() {
        return status;
    }

    public MediaType getMediaType() {
        return mediaType;
    }

    public Object getBody() {
        return body;
    }
}
