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

package tech.lamprism.lampray.web;

/**
 * Internal interface for inverting the dependency of the external endpoint
 * provider. Since some components need to access the external endpoint
 * without being aware of the web module.
 *
 * @author RollW
 */
public interface ExternalEndpointProvider {
    /**
     * Get the external web endpoint, which is used for accessing the web
     * interface of the system.
     * <p>
     * See also: {@code ServerConfigKeys#HTTP_EXTERNAL_WEB_ADDRESS}
     *
     * @return the external web endpoint URL
     */
    String getExternalWebEndpoint();

    /**
     * Get the external API endpoint, which is used for accessing the API of
     * the system.
     * <p>
     * See also: {@code ServerConfigKeys#HTTP_EXTERNAL_API_ADDRESS}
     *
     * @return the external API endpoint URL
     */
    String getExternalApiEndpoint();
}
