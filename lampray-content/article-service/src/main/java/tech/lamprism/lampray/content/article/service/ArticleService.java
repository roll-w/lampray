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

package tech.lamprism.lampray.content.article.service;

import org.apache.commons.lang3.Validate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import space.lingu.NonNull;
import tech.lamprism.lampray.content.ContentDetails;
import tech.lamprism.lampray.content.ContentPublisher;
import tech.lamprism.lampray.content.ContentType;
import tech.lamprism.lampray.content.UncreatedContent;
import tech.lamprism.lampray.content.article.Article;
import tech.lamprism.lampray.content.article.persistence.ArticleDo;
import tech.lamprism.lampray.content.article.persistence.ArticleRepository;
import tech.lamprism.lampray.content.collection.ContentCollectionIdentity;
import tech.lamprism.lampray.content.collection.ContentCollectionProvider;
import tech.lamprism.lampray.content.collection.ContentCollectionType;
import tech.lamprism.lampray.content.common.ContentErrorCode;
import tech.lamprism.lampray.content.common.ContentException;
import tech.lamprism.lampray.content.structuraltext.StructuralText;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Objects;

/**
 * @author RollW
 */
@Service
public class ArticleService implements ContentPublisher, ContentCollectionProvider {
    private static final Logger logger = LoggerFactory.getLogger(ArticleService.class);

    private final ArticleRepository articleRepository;

    public ArticleService(ArticleRepository articleRepository) {
        this.articleRepository = articleRepository;
    }

    @Override
    public ContentDetails publish(@NonNull UncreatedContent uncreatedContent,
                                  OffsetDateTime timestamp)
            throws ContentException {
        if (uncreatedContent.getContentType() != ContentType.ARTICLE) {
            throw new IllegalArgumentException("Content type not supported: " +
                    uncreatedContent.getContentType());
        }
        String title = Validate.notEmpty(uncreatedContent.getTitle());
        StructuralText content = Objects.requireNonNull(uncreatedContent.getContent());

        long userId = uncreatedContent.getOperator().getUserId();

        if (articleRepository.findByTitle(title, userId).isPresent()) {
            throw new ContentException(ContentErrorCode.ERROR_CONTENT_EXISTED);
        }

        ArticleDo article = ArticleDo.builder()
                .setUserId(userId)
                .setTitle(title)
                .setContent(content)
                .setCover("user-cover")
                .setCreateTime(timestamp)
                .setUpdateTime(timestamp)
                .build();
        ArticleDo created = articleRepository.save(article);
        logger.trace("Article({}) title={} created by user({})",
                created.getId(), created.getTitle(), created.getUserId());
        return created;
    }

    @Override
    public boolean supports(@NonNull ContentType contentType) {
        return contentType == ContentType.ARTICLE;
    }

    private List<Article> getUserArticles(long id) {
        return articleRepository.findAllByUserId(id)
                .stream()
                .map(ArticleDo::lock)
                .toList();
    }

    private List<Article> getArticles() {
        return articleRepository.findAll()
                .stream()
                .map(ArticleDo::lock)
                .toList();
    }

    @Override
    public boolean supportsCollection(ContentCollectionType contentCollectionType) {
        return switch (contentCollectionType) {
            case ARTICLES, USER_ARTICLES -> true;
            default -> false;
        };
    }

    @NonNull
    @Override
    public List<? extends ContentDetails> getContents(
            ContentCollectionIdentity contentCollectionIdentity) {
        return switch (contentCollectionIdentity.getContentCollectionType()) {
            case ARTICLES -> getArticles();
            case USER_ARTICLES -> getUserArticles(
                    contentCollectionIdentity.getContentCollectionId()
            );
            default -> throw new UnsupportedOperationException("Unsupported collection type: " +
                    contentCollectionIdentity.getContentCollectionType());
        };
    }
}
