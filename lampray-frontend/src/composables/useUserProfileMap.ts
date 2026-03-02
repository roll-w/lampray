/*
 * Copyright (C) 2023-2026 RollW
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

import {ref} from "vue";
import {useAxios} from "@/composables/useAxios.ts";
import {userService} from "@/services/user/user.service.ts";
import type {UserCommonDetailsVo} from "@/services/user/user.type.ts";

const profileCache = new Map<number, UserCommonDetailsVo | null>();

function normalizeIds(userIds: number[]): number[] {
    const deduplicated = new Set<number>();
    for (const userId of userIds) {
        if (Number.isFinite(userId) && userId > 0) {
            deduplicated.add(userId);
        }
    }
    return [...deduplicated];
}

export function useUserProfileMap() {
    const axios = useAxios();
    const loading = ref(false);
    const profiles = ref<Map<number, UserCommonDetailsVo | null>>(new Map());

    async function resolveProfiles(userIds: number[]): Promise<Map<number, UserCommonDetailsVo | null>> {
        const normalizedIds = normalizeIds(userIds);
        const missingIds = normalizedIds.filter(userId => !profileCache.has(userId));

        if (missingIds.length > 0) {
            loading.value = true;
            try {
                await Promise.all(missingIds.map(async userId => {
                    try {
                        const response = await userService(axios).getUserInfo(userId);
                        profileCache.set(userId, response.data.data ?? null);
                    } catch {
                        profileCache.set(userId, null);
                    }
                }));
            } finally {
                loading.value = false;
            }
        }

        const next = new Map<number, UserCommonDetailsVo | null>();
        for (const userId of normalizedIds) {
            next.set(userId, profileCache.get(userId) ?? null);
        }
        profiles.value = next;
        return next;
    }

    function getProfile(userId: number): UserCommonDetailsVo | null {
        return profiles.value.get(userId) ?? profileCache.get(userId) ?? null;
    }

    return {
        loading,
        profiles,
        resolveProfiles,
        getProfile,
    };
}
