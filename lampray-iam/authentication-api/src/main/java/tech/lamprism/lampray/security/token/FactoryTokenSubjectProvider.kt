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
package tech.lamprism.lampray.security.token

/**
 * @author RollW
 */
class FactoryTokenSubjectProvider(
    private val factories: List<TokenSubject.Factory>
) : TokenSubjectProvider {
    override fun getTokenSubject(id: String, subjectType: SubjectType): TokenSubject {
        val factory = factories.find { it.supports(subjectType) }
            ?: throw IllegalArgumentException("No factory registered for subject type: $subjectType")
        return factory.fromSubject(id, subjectType)
    }
}
