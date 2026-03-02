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

package tech.lamprism.lampray.web.controller.article;

import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import tech.lamprism.lampray.content.ContentDetails;
import tech.lamprism.lampray.content.ContentPublishProvider;
import tech.lamprism.lampray.content.UncreatedContent;
import tech.lamprism.lampray.content.article.ArticleDetailsMetadata;
import tech.lamprism.lampray.content.common.ContentException;
import tech.lamprism.lampray.user.UserIdentity;
import tech.lamprism.lampray.web.common.ApiContext;
import tech.lamprism.lampray.web.controller.Api;
import tech.lamprism.lampray.web.controller.article.model.ArticleCreateRequest;
import tech.lamprism.lampray.web.controller.article.model.ArticleInfoView;
import tech.rollw.common.web.HttpResponseEntity;
import tech.rollw.common.web.UserErrorCode;
import tech.rollw.common.web.system.ContextThread;
import tech.rollw.common.web.system.ContextThreadAware;

/**
 * @author RollW
 */
@Api
public class ArticleController {
    private final ContentPublishProvider contentPublishProvider;
    private final ContextThreadAware<ApiContext> apiContextThreadAware;

    public ArticleController(ContentPublishProvider contentPublishProvider,
                             ContextThreadAware<ApiContext> apiContextThreadAware) {
        this.contentPublishProvider = contentPublishProvider;
        this.apiContextThreadAware = apiContextThreadAware;
    }

    @PostMapping("/articles")
    public HttpResponseEntity<ArticleInfoView> createArticle(
            @RequestBody ArticleCreateRequest articleCreateRequest) {
        ContextThread<ApiContext> apiContextThread = apiContextThreadAware.getContextThread();
        ApiContext apiContext = apiContextThread.getContext();
        if (!apiContext.hasUser()) {
            throw new ContentException(UserErrorCode.ERROR_USER_NOT_LOGIN);
        }
        UserIdentity user = apiContext.getUser();
        UncreatedContent uncreatedContent = articleCreateRequest.toUncreatedContent(
                user,
                ArticleDetailsMetadata.EMPTY
        );
        ContentDetails articleDetails =
                contentPublishProvider.publishContent(uncreatedContent);
        ArticleInfoView articleInfoView = ArticleInfoView.from(articleDetails);
        return HttpResponseEntity.success(articleInfoView);
    }
}
