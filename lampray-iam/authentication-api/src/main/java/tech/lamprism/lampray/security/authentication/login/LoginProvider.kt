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
package tech.lamprism.lampray.security.authentication.login

import tech.lamprism.lampray.RequestMetadata
import tech.lamprism.lampray.security.authentication.UserInfoSignature

/**
 * @author RollW
 */
@JvmDefaultWithoutCompatibility
interface LoginProvider {
    fun getLoginStrategy(type: LoginStrategyType): LoginStrategy

    fun login(
        identity: String, token: String,
        type: LoginStrategyType,
        requestMetadata: RequestMetadata? = null
    ): UserInfoSignature

    fun sendToken(
        identity: String,
        type: LoginStrategyType,
        requestMetadata: RequestMetadata? = null
    )

    fun logout()
}
