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

package tech.lamprism.lampray.content.favorite;

import space.lingu.NonNull;
import tech.lamprism.lampray.content.ContentTrait;
import tech.rollw.common.web.CommonRuntimeException;
import tech.rollw.common.web.system.SystemResource;
import tech.rollw.common.web.system.SystemResourceKind;
import tech.rollw.common.web.system.SystemResourceOperator;

/**
 * @author RollW
 */
public interface FavoriteGroupOperator extends SystemResourceOperator<Long>, SystemResource<Long> {
    @Override
    void setCheckDeleted(boolean checkDeleted);

    @Override
    boolean isCheckDeleted();

    @Override
    FavoriteGroupOperator update() throws CommonRuntimeException;

    @Override
    FavoriteGroupOperator delete() throws CommonRuntimeException;

    @Override
    FavoriteGroupOperator rename(String newName) throws CommonRuntimeException;

    @Override
    FavoriteGroupOperator getSystemResource();

    FavoriteGroupOperator setVisibility(boolean publicVisible);

    FavoriteGroupOperator addFavorite(ContentTrait contentTrait);

    FavoriteGroupOperator removeFavorite(ContentTrait contentTrait);

    FavoriteGroupOperator removeFavorite(long favoriteItemId);

    FavoriteGroup getFavoriteGroup();

    @Override
    default Long getResourceId() {
        return getFavoriteGroup().getId();
    }

    @NonNull
    @Override
    default SystemResourceKind getSystemResourceKind() {
        return FavoriteGroupResourceKind.INSTANCE;
    }
}
