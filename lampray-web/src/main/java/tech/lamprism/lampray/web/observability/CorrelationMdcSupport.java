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

package tech.lamprism.lampray.web.observability;

import org.slf4j.MDC;
import tech.lamprism.lampray.observability.CorrelationContext;

/**
 * @author RollW
 */
public final class CorrelationMdcSupport {
    public static final String REQUEST_ID = "requestId";
    public static final String TRACE_ID = "traceId";
    public static final String SPAN_ID = "spanId";

    private CorrelationMdcSupport() {
    }

    public static void replace(CorrelationContext context) {
        clear();
        if (context == null) {
            return;
        }

        putIfPresent(REQUEST_ID, context.getRequestId());
        putIfPresent(TRACE_ID, context.getTraceId());
        putIfPresent(SPAN_ID, context.getSpanId());
    }

    public static void clear() {
        MDC.remove(REQUEST_ID);
        MDC.remove(TRACE_ID);
        MDC.remove(SPAN_ID);
    }

    private static void putIfPresent(String key, String value) {
        if (value == null || value.trim().isEmpty()) {
            return;
        }
        MDC.put(key, value);
    }
}
