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
package tech.lamprism.lampray.content.permit

import space.lingu.NonNull
import tech.lamprism.lampray.content.Content
import tech.lamprism.lampray.content.ContentAccessAuthType
import tech.lamprism.lampray.content.ContentAccessCredential.Type
import tech.lamprism.lampray.content.ContentAccessCredentials

/**
 * @author RollW
 */
class ContentPermitCheckerChain(
    private val providers: List<ContentPermitCheckProvider>
) : ContentPermitChecker {

    @NonNull
    override fun checkAccessPermit(
        @NonNull content: Content,
        @NonNull contentAccessAuthType: ContentAccessAuthType,
        @NonNull credentials: ContentAccessCredentials
    ): ContentPermitResult {
        if (!contentAccessAuthType.needsAuth()) {
            // no need to check.
            return ContentPermitResult.permit()
        }
        if (credentials.hasType(Type.NO_LIMIT)) {
            return ContentPermitResult.permit()
        }

        for (provider in providers) {
            if (!provider.supports(contentAccessAuthType)) {
                continue
            }
            val result = provider.checkAccessPermit(
                content,
                contentAccessAuthType,
                credentials
            )
            if (result.failed()) {
                return ContentPermitResult.deny(result)
            }
        }

        return ContentPermitResult.permit()
    }

    private class EmptyChecker : ContentPermitChecker {
        override fun checkAccessPermit(
            content: Content,
            contentAccessAuthType: ContentAccessAuthType,
            credentials: ContentAccessCredentials
        ): ContentPermitResult {
            return ContentPermitResult.permit()
        }

        companion object {
            val INSTANCE: EmptyChecker = EmptyChecker()
        }
    }

    companion object {
        @JvmStatic
        fun of(vararg providers: ContentPermitCheckProvider): ContentPermitChecker {
            return of(providers.toList())
        }

        @JvmStatic
        fun of(providers: List<ContentPermitCheckProvider>): ContentPermitChecker {
            if (providers.isEmpty()) {
                return EmptyChecker.INSTANCE
            }
            return ContentPermitCheckerChain(providers)
        }
    }
}
