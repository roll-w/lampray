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

package tech.lamprism.lampray.storage.support;

import org.springframework.stereotype.Service;

import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.locks.ReentrantLock;

@Service
public class StorageBlobLifecycleLockManager {
    private final ConcurrentMap<String, ReentrantLock> locks = new ConcurrentHashMap<>();

    public LockedKey acquire(String key) {
        String normalizedKey = Objects.requireNonNull(key, "key").trim();
        if (normalizedKey.isEmpty()) {
            throw new IllegalArgumentException("key must not be blank");
        }
        ReentrantLock lock = locks.computeIfAbsent(normalizedKey, ignored -> new ReentrantLock());
        lock.lock();
        return new LockedKey(normalizedKey, lock);
    }

    public final class LockedKey implements AutoCloseable {
        private final String key;
        private final ReentrantLock lock;

        private LockedKey(String key,
                          ReentrantLock lock) {
            this.key = key;
            this.lock = lock;
        }

        @Override
        public void close() {
            lock.unlock();
            if (!lock.isLocked() && !lock.hasQueuedThreads()) {
                locks.remove(key, lock);
            }
        }
    }
}
