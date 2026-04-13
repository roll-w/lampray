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

package tech.lamprism.lampray.lock.impl;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import tech.lamprism.lampray.lock.LockService;
import tech.lamprism.lampray.lock.configuration.LockConfigKeys;
import tech.lamprism.lampray.lock.persistence.SharedLockEntity;
import tech.lamprism.lampray.lock.persistence.SharedLockRepository;
import tech.lamprism.lampray.setting.ConfigReader;

import java.time.OffsetDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.locks.ReentrantLock;

/**
 * Uses the shared database as the source of truth for distributed locks while keeping
 * local reentrancy cheap within a single JVM.
 *
 * @author RollW
 */
@Service
public class JpaLockService implements LockService {
    private final SharedLockRepository sharedLockRepository;
    private final TransactionTemplate transactionTemplate;
    private final ConcurrentMap<String, ReentrantLock> localLocks = new ConcurrentHashMap<>();
    private final ThreadLocal<Map<String, HeldLock>> heldLocks = ThreadLocal.withInitial(HashMap::new);
    private final String ownerPrefix = UUID.randomUUID().toString();
    private final long leaseSeconds;
    private final long retryIntervalMillis;

    public JpaLockService(SharedLockRepository sharedLockRepository,
                          ConfigReader configReader,
                          PlatformTransactionManager transactionManager) {
        this.sharedLockRepository = sharedLockRepository;
        this.transactionTemplate = new TransactionTemplate(transactionManager);
        this.leaseSeconds = positive(configReader.get(LockConfigKeys.LEASE_SECONDS, 300L), "lock lease seconds");
        this.retryIntervalMillis = positive(configReader.get(LockConfigKeys.RETRY_INTERVAL_MILLIS, 200L), "lock retry interval millis");
    }

    @Override
    public AcquiredLock acquire(String key) {
        String normalizedKey = normalizeKey(key);
        ReentrantLock localLock = localLocks.computeIfAbsent(normalizedKey, ignored -> new ReentrantLock());
        localLock.lock();
        Map<String, HeldLock> threadLocks = heldLocks.get();
        HeldLock existingHold = threadLocks.get(normalizedKey);
        if (existingHold != null) {
            existingHold.increment();
            return new AcquiredLockImpl(normalizedKey, localLock);
        }

        String ownerToken = ownerPrefix + ":" + UUID.randomUUID();
        boolean acquired = false;
        try {
            acquireClusterLock(normalizedKey, ownerToken);
            threadLocks.put(normalizedKey, new HeldLock(ownerToken));
            acquired = true;
            return new AcquiredLockImpl(normalizedKey, localLock);
        } finally {
            if (!acquired) {
                releaseLocalLock(normalizedKey, localLock);
            }
        }
    }

    private void acquireClusterLock(String key,
                                    String ownerToken) {
        while (true) {
            Boolean acquired = transactionTemplate.execute(status -> tryAcquireInTransaction(key, ownerToken));
            if (Boolean.TRUE.equals(acquired)) {
                return;
            }
            sleepBeforeRetry(key);
        }
    }

    private boolean tryAcquireInTransaction(String key,
                                            String ownerToken) {
        OffsetDateTime now = OffsetDateTime.now();
        OffsetDateTime expiresAt = now.plusSeconds(leaseSeconds);
        SharedLockEntity existing = sharedLockRepository.findLockedByLockKey(key).orElse(null);
        if (existing == null) {
            try {
                sharedLockRepository.save(new SharedLockEntity(key, ownerToken, expiresAt, now, now));
                sharedLockRepository.flush();
                return true;
            } catch (DataIntegrityViolationException exception) {
                return false;
            }
        }
        if (existing.getExpiresAt().isAfter(now) && !ownerToken.equals(existing.getOwnerToken())) {
            return false;
        }
        existing.setOwnerToken(ownerToken);
        existing.setExpiresAt(expiresAt);
        existing.setUpdateTime(now);
        sharedLockRepository.save(existing);
        sharedLockRepository.flush();
        return true;
    }

    private void sleepBeforeRetry(String key) {
        try {
            Thread.sleep(retryIntervalMillis);
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Interrupted while waiting for lock: " + key, exception);
        }
    }

    private void release(String key,
                         ReentrantLock localLock) {
        Map<String, HeldLock> threadLocks = heldLocks.get();
        HeldLock heldLock = threadLocks.get(key);
        if (heldLock == null) {
            releaseLocalLock(key, localLock);
            return;
        }

        String ownerToken = heldLock.ownerToken();
        int remaining = heldLock.decrement();
        try {
            if (remaining == 0) {
                threadLocks.remove(key);
                if (threadLocks.isEmpty()) {
                    heldLocks.remove();
                }
                releaseClusterLock(key, ownerToken);
            }
        } finally {
            releaseLocalLock(key, localLock);
        }
    }

    private void releaseClusterLock(String key,
                                    String ownerToken) {
        transactionTemplate.executeWithoutResult(status -> sharedLockRepository.findLockedByLockKey(key)
                .filter(existing -> ownerToken.equals(existing.getOwnerToken()))
                .ifPresent(existing -> {
                    sharedLockRepository.delete(existing);
                    sharedLockRepository.flush();
                }));
    }

    private void releaseLocalLock(String key,
                                  ReentrantLock localLock) {
        localLock.unlock();
        if (!localLock.isLocked() && !localLock.hasQueuedThreads()) {
            localLocks.remove(key, localLock);
        }
    }

    private String normalizeKey(String key) {
        String normalizedKey = Objects.requireNonNull(key, "key").trim();
        if (normalizedKey.isEmpty()) {
            throw new IllegalArgumentException("key must not be blank");
        }
        return normalizedKey;
    }

    private long positive(long value,
                          String name) {
        if (value <= 0) {
            throw new IllegalArgumentException(name + " must be positive");
        }
        return value;
    }

    private static final class HeldLock {
        private final String ownerToken;
        private int holdCount = 1;

        private HeldLock(String ownerToken) {
            this.ownerToken = ownerToken;
        }

        private String ownerToken() {
            return ownerToken;
        }

        private void increment() {
            holdCount++;
        }

        private int decrement() {
            holdCount--;
            return holdCount;
        }
    }

    private final class AcquiredLockImpl implements AcquiredLock {
        private final String key;
        private final ReentrantLock localLock;
        private boolean closed;

        private AcquiredLockImpl(String key,
                                 ReentrantLock localLock) {
            this.key = key;
            this.localLock = localLock;
        }

        @Override
        public void close() {
            if (closed) {
                return;
            }
            closed = true;
            release(key, localLock);
        }
    }
}
