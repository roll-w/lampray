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

import tech.lamprism.lampray.user.UserIdentity;

import java.util.List;

/**
 * @author RollW
 */
public interface FavoriteProvider {
    List<FavoriteGroup> getFavoriteGroups();

    List<FavoriteGroup> getFavoriteGroups(UserIdentity userIdentity);

    List<FavoriteItem> getFavoriteItems(long favoriteGroupId);

    FavoriteGroup getFavoriteGroup(long favoriteGroupId);

    FavoriteItem getFavoriteItem(long favoriteItemId);
}
