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

package tech.lamprism.lampray.web.controller.content;

import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import tech.lamprism.lampray.content.ContentAccessCredential;
import tech.lamprism.lampray.content.ContentAccessCredentials;
import tech.lamprism.lampray.content.ContentAccessService;
import tech.lamprism.lampray.content.ContentDetails;
import tech.lamprism.lampray.content.ContentIdentity;
import tech.lamprism.lampray.content.ContentMetadataDetails;
import tech.lamprism.lampray.content.ContentOperator;
import tech.lamprism.lampray.content.ContentPublishProvider;
import tech.lamprism.lampray.content.collection.ContentCollectionIdentity;
import tech.lamprism.lampray.content.collection.ContentCollectionProviderFactory;
import tech.lamprism.lampray.content.collection.ContentCollectionType;
import tech.lamprism.lampray.content.common.ContentErrorCode;
import tech.lamprism.lampray.content.common.ContentException;
import tech.lamprism.lampray.user.AttributedUser;
import tech.lamprism.lampray.web.common.ApiContext;
import tech.lamprism.lampray.web.controller.Api;
import tech.lamprism.lampray.web.controller.content.vo.ContentVo;
import tech.lamprism.lampray.web.controller.content.vo.UrlContentType;
import tech.rollw.common.web.CommonErrorCode;
import tech.rollw.common.web.HttpResponseEntity;
import tech.rollw.common.web.system.ContextThread;
import tech.rollw.common.web.system.ContextThreadAware;
import tech.rollw.common.web.system.SimpleSystemResource;
import tech.rollw.common.web.system.SystemResource;
import tech.rollw.common.web.system.SystemResourceOperatorProvider;

import java.util.List;

/**
 * @author RollW
 */
@Api
public class ContentController {
    private final ContentPublishProvider contentPublishProvider;
    private final ContentAccessService contentAccessService;
    private final SystemResourceOperatorProvider<Long> systemResourceOperatorProvider;
    private final ContentCollectionProviderFactory contentCollectionProviderFactory;
    private final ContextThreadAware<ApiContext> apiContextThreadAware;

    public ContentController(ContentPublishProvider contentPublishProvider,
                             ContentAccessService contentAccessService,
                             SystemResourceOperatorProvider<Long> systemResourceOperatorProvider,
                             ContentCollectionProviderFactory contentCollectionProviderFactory,
                             ContextThreadAware<ApiContext> apiContextThreadAware) {
        this.contentPublishProvider = contentPublishProvider;
        this.contentAccessService = contentAccessService;
        this.systemResourceOperatorProvider = systemResourceOperatorProvider;
        this.contentCollectionProviderFactory = contentCollectionProviderFactory;
        this.apiContextThreadAware = apiContextThreadAware;
    }

    @GetMapping("/users/{userId}/{contentType}/{contentId}")
    public HttpResponseEntity<ContentVo> getContent(
            @PathVariable("userId") Long userId,
            @PathVariable("contentType") UrlContentType contentType,
            @PathVariable("contentId") Long contentId) {
        ContextThread<ApiContext> apiContextThread = apiContextThreadAware.getContextThread();
        ApiContext apiContext = apiContextThread.getContext();
        ContentDetails details = contentAccessService.openContent(
                ContentIdentity.of(contentId, contentType.getContentType()),
                ContentAccessCredentials.of(
                        ContentAccessCredential.Type.USER, apiContext.getUser()
                )
        );
        if (details.getUserId() != userId) {
            return HttpResponseEntity.of(ContentErrorCode.ERROR_CONTENT_NOT_FOUND);
        }

        return HttpResponseEntity.success(contentVoConvert(details));
    }


    @GetMapping("/{contentType}")
    public HttpResponseEntity<List<ContentVo>> getContents(
            @PathVariable("contentType") UrlContentType contentType) {
        ApiContext context = apiContextThreadAware.getContextThread()
                .getContext();
        AttributedUser user = context.getUser();
        if (user == null) {
            return HttpResponseEntity.of(CommonErrorCode.ERROR_NOT_FOUND);
        }

        ContentCollectionType userCollectionType = contentType.getUserCollectionType();
        ContentAccessCredentials contentAccessCredentials = ContentAccessCredentials.of(
                ContentAccessCredential.Type.USER,
                user
        );
        List<ContentMetadataDetails<?>> contents = contentCollectionProviderFactory.getContents(
                ContentCollectionIdentity.of(
                        user.getUserId(),
                        userCollectionType
                ),
                contentAccessCredentials
        );

        return HttpResponseEntity.success(
                contents.stream().map(this::contentVoConvert).toList()
        );
    }

    @GetMapping("/users/{userId}/{contentType}")
    public HttpResponseEntity<List<ContentVo>> getUserContents(
            @PathVariable("userId") Long userId,
            @PathVariable("contentType") UrlContentType contentType) {
        ContextThread<ApiContext> apiContextThread =
                apiContextThreadAware.getContextThread();
        ApiContext apiContext = apiContextThread.getContext();
        // TODO: check if the user is the same as the current user
        ContentAccessCredentials contentAccessCredentials = ContentAccessCredentials.of(
                ContentAccessCredential.Type.USER,
                apiContext.getUser()
        );

        List<ContentMetadataDetails<?>> contents =
                contentCollectionProviderFactory.getContents(
                        ContentCollectionIdentity.of(
                                userId,
                                contentType.getUserCollectionType()
                        ),
                        contentAccessCredentials
                );
        return HttpResponseEntity.success(
                contents.stream().map(this::contentVoConvert).toList()
        );
    }

    @DeleteMapping("/users/{userId}/{contentType}/{contentId}")
    public HttpResponseEntity<Void> deleteContent(
            @PathVariable("userId") Long userId,
            @PathVariable("contentType") UrlContentType contentType,
            @PathVariable("contentId") Long contentId) {
        ContentOperator contentOperator =
                systemResourceOperatorProvider.getSystemResourceOperator(
                        getSystemResource(contentId, contentType), true
                ).cast(ContentOperator.class);
        if (userId != contentOperator.getUserId()) {
            throw new ContentException(ContentErrorCode.ERROR_CONTENT_NOT_FOUND);
        }
        contentOperator.enableAutoUpdate()
                .delete();
        return HttpResponseEntity.success();
    }

    @PutMapping("/users/{userId}/{contentType}/{contentId}")
    public HttpResponseEntity<Void> updateContent(
            @PathVariable("userId") Long userId,
            @PathVariable("contentType") UrlContentType contentType,
            @PathVariable("contentId") Long contentId) {
        return HttpResponseEntity.success();
    }

    private SystemResource<Long> getSystemResource(Long contentId,
                                                   UrlContentType contentType) {
        return new SimpleSystemResource<>(
                contentId,
                contentType.getContentType().getSystemResourceKind()
        );
    }

    private ContentVo contentVoConvert(ContentDetails details) {
       return ContentViewHelper.toContentView(details);
    }
}
