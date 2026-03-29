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

package tech.lamprism.lampray.web.controller.storage;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpRange;

import java.util.List;

/**
 * @author RollW
 */
public final class HttpRangeUtils {

    public static List<HttpRange> tryGetsRange(HttpServletRequest request) {
        String range = request.getHeader("Range");
        if (range == null) {
            return List.of();
        }
        return HttpRange.parseRanges(range);
    }

    private HttpRangeUtils() {
    }
}
