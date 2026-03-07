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

package tech.lamprism.lampray.content.article.persistence

import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import tech.lamprism.lampray.common.data.CommonDao
import tech.lamprism.lampray.common.data.Dao

/**
 * @author RollW
 */
@Dao
interface ArticleDao : CommonDao<ArticleDo, String> {
    @Query("FROM ArticleDo a WHERE a.userId = :userId")
    fun findAllByUserId(@Param("userId") userId: Long): List<ArticleDo>

    @Query("FROM ArticleDo a WHERE a.id = :id")
    fun findById(@Param("id") id: Long): ArticleDo?

    @Query("FROM ArticleDo a WHERE a.id in :ids")
    fun findAllByIdIn(@Param("ids") ids: List<Long>): List<ArticleDo>
}
