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

package tech.lamprism.lampray.server.sshd

import org.apache.sshd.common.AttributeRepository
import org.apache.sshd.server.auth.password.PasswordAuthenticator
import org.apache.sshd.server.session.ServerSession
import org.slf4j.info
import org.slf4j.logger
import org.springframework.security.core.Authentication
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.stereotype.Service
import tech.lamprism.lampray.authentication.login.LoginProvider
import tech.lamprism.lampray.authentication.login.LoginStrategyType
import tech.rollw.common.web.CommonRuntimeException

/**
 * @author RollW
 */
@Service
class SshdPasswordAuthenticator(
    private val loginProvider: LoginProvider
) : PasswordAuthenticator {
    override fun authenticate(
        username: String,
        password: String,
        session: ServerSession
    ): Boolean {
        return try {
            val user = loginProvider.login(
                username, password, LoginStrategyType.PASSWORD
            )
            if (!user.role.hasPrivilege()) {
                return false
            }
            session.setAttribute(
                AUTHENTICATION_KEY,
                SecurityContextHolder.getContext().authentication
            )
            logger.info {
                "User '${user.username}' login successfully through SSH connection, remote address: " +
                        "${session.clientAddress} using ${session.clientVersion}"
            }
            true
        } catch (e: CommonRuntimeException) {
            false
        } finally {
            SecurityContextHolder.clearContext()
        }
    }

    companion object {
        private val logger = logger<SshdPasswordAuthenticator>()

        @JvmField
        val AUTHENTICATION_KEY = AttributeRepository.AttributeKey<Authentication>()
    }
}