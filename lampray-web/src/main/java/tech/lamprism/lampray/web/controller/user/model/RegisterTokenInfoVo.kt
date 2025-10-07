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

package tech.lamprism.lampray.web.controller.user.model

import tech.lamprism.lampray.security.authentication.VerifiableToken
import tech.lamprism.lampray.setting.SecretLevel
import tech.lamprism.lampray.user.UserIdentity

/**
 * @author RollW
 */
data class RegisterTokenInfoVo(
    val token: String,
    val maskedUsername: String,
    val maskedEmail: String,
) {

    companion object {
        @JvmStatic
        fun from(verifiableToken: VerifiableToken, userIdentity: UserIdentity): RegisterTokenInfoVo {
            return RegisterTokenInfoVo(
                token = verifiableToken.token(),
                maskedUsername = maskUsername(userIdentity.username),
                maskedEmail = maskEmail(userIdentity.email)
            )
        }

        private fun maskUsername(username: String): String {
            return SecretLevel.LOW.maskValue(username)
        }

        private fun maskEmail(email: String): String {
            val atIndex = email.indexOf('@')
            if (atIndex <= 0 || atIndex == email.length - 1) {
                return SecretLevel.LOW.maskValue(email)
            }
            val name = email.take(atIndex)
            val domainFull = email.substring(atIndex + 1)
            val dotIndex = domainFull.indexOf('.')
            val domain = if (dotIndex > 0) domainFull.take(dotIndex) else domainFull
            val tld = if (dotIndex > 0) domainFull.substring(dotIndex + 1) else ""
            val maskedName = if (name.length > 1) "${name.first()}***" else "***"
            val maskedDomain = if (domain.length > 1) "${domain.first()}***" else "***"
            return buildString {
                append(maskedName)
                append("@")
                append(maskedDomain)
                if (tld.isNotEmpty()) {
                    append(".")
                    append(tld)
                }
            }
        }
    }
}
