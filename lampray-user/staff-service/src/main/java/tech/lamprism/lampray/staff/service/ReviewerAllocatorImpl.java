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

package tech.lamprism.lampray.staff.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import tech.lamprism.lampray.content.ContentIdentity;
import tech.lamprism.lampray.content.ContentType;
import tech.lamprism.lampray.content.review.ReviewerAllocator;
import tech.lamprism.lampray.content.review.persistence.ReviewJobEntity;
import tech.lamprism.lampray.content.review.persistence.ReviewJobRepository;
import tech.lamprism.lampray.content.review.persistence.ReviewTaskEntity;
import tech.lamprism.lampray.content.review.persistence.ReviewTaskRepository;
import tech.lamprism.lampray.staff.AttributedStaff;
import tech.lamprism.lampray.staff.OnStaffEventListener;
import tech.lamprism.lampray.staff.Staff;
import tech.lamprism.lampray.staff.StaffType;
import tech.lamprism.lampray.staff.persistence.StaffRepository;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.concurrent.Executor;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

/**
 * @author RollW
 */
@Service
public class ReviewerAllocatorImpl implements ReviewerAllocator, OnStaffEventListener {
    private static final Logger logger = LoggerFactory.getLogger(ReviewerAllocatorImpl.class);

    private final ReviewTaskRepository reviewTaskRepository;
    private final ReviewJobRepository reviewJobRepository;
    private final StaffRepository staffRepository;

    private final Map<Long, Integer> weights = new HashMap<>();
    private final TreeMap<Integer, Deque<Long>> staffReviewingCount = new TreeMap<>();
    private final ReadWriteLock lock = new ReentrantReadWriteLock();
    private final Executor executor;

    public ReviewerAllocatorImpl(ReviewTaskRepository reviewTaskRepository,
                                 ReviewJobRepository reviewJobRepository,
                                 StaffRepository staffRepository,
                                 @Qualifier("mainScheduledExecutorService") Executor executor) {
        this.reviewTaskRepository = reviewTaskRepository;
        this.reviewJobRepository = reviewJobRepository;
        this.staffRepository = staffRepository;
        this.executor = executor;
        startAsyncLoad();
    }

    private void startAsyncLoad() {
        executor.execute(this::loadStaffReviewingCount);
    }


    private void loadStaffReviewingCount() {
        lock.writeLock().lock();
        try {
            weights.clear();
            staffReviewingCount.clear();

            final int batchSize = 200;
            int offset = 0;
            long totalTasks = reviewTaskRepository.countPendingTasks();

            while (offset < totalTasks) {
                List<ReviewTaskEntity> pendingTasks = reviewTaskRepository.findPendingTasksBatch(offset, batchSize);
                if (pendingTasks.isEmpty()) {
                    break;
                }

                List<String> jobIds = pendingTasks.stream()
                        .map(ReviewTaskEntity::getReviewJobId)
                        .distinct()
                        .toList();

                List<ReviewJobEntity> jobs = reviewJobRepository.findAllById(jobIds);
                Map<String, ReviewJobEntity> jobMap = new HashMap<>();
                for (ReviewJobEntity job : jobs) {
                    jobMap.put(job.getResourceId(), job);
                }

                for (ReviewTaskEntity task : pendingTasks) {
                    ReviewJobEntity job = jobMap.get(task.getReviewJobId());
                    if (job != null) {
                        long reviewerId = task.getReviewerId();
                        int weight = job.getReviewContentType().getWeight();
                        weights.put(reviewerId, weights.getOrDefault(reviewerId, 0) + weight);
                    }
                }

                offset += batchSize;
            }

            List<? extends AttributedStaff> staffs = loadStaffs();
            for (AttributedStaff staff : staffs) {
                if (staff != null) {
                    weights.putIfAbsent(staff.getUserId(), 0);
                }
            }

            for (Map.Entry<Long, Integer> entry : weights.entrySet()) {
                long staffId = entry.getKey();
                int weight = entry.getValue();
                Deque<Long> deque = staffReviewingCount.computeIfAbsent(weight, k -> new ArrayDeque<>());
                deque.addLast(staffId);
            }

            logger.info("Load ReviewerAllocator staffs: {}, weights: {}",
                    weights.size(), staffReviewingCount.size());
        } finally {
            lock.writeLock().unlock();
        }
    }

    private List<? extends AttributedStaff> loadStaffs() {
        return staffRepository.findByTypes(Set.of(StaffType.ADMIN, StaffType.REVIEWER));
    }

    private void remappingReviewer(long reviewer, int original, int newWeight) {
        lock.writeLock().lock();
        try {
            weights.put(reviewer, newWeight);

            Deque<Long> originalDeque = staffReviewingCount.get(original);
            if (originalDeque != null) {
                originalDeque.removeFirstOccurrence(reviewer);
                if (originalDeque.isEmpty()) {
                    staffReviewingCount.remove(original);
                }
            }

            Deque<Long> newDeque = staffReviewingCount.getOrDefault(newWeight, new ArrayDeque<>());
            if (!newDeque.contains(reviewer)) {
                newDeque.addLast(reviewer);
            }
            staffReviewingCount.put(newWeight, newDeque);
        } finally {
            lock.writeLock().unlock();
        }
    }

    @Override
    public long allocateReviewer(ContentIdentity contentIdentity, boolean allowAutoReviewer) {
        if (contentIdentity == null) {
            return AUTO_REVIEWER;
        }

        if (canAutoReview(contentIdentity.getContentType()) && allowAutoReviewer) {
            return AUTO_REVIEWER;
        }

        lock.writeLock().lock();
        try {
            Map.Entry<Integer, Deque<Long>> entry = staffReviewingCount.firstEntry();
            if (entry == null) {
                return AUTO_REVIEWER;
            }

            Deque<Long> ids = entry.getValue();
            if (ids == null || ids.isEmpty()) {
                staffReviewingCount.remove(entry.getKey());
                Map.Entry<Integer, Deque<Long>> next = staffReviewingCount.firstEntry();
                if (next == null) {
                    return AUTO_REVIEWER;
                }
                ids = next.getValue();
                if (ids == null || ids.isEmpty()) {
                    return AUTO_REVIEWER;
                }
                entry = next;
            }

            // Pick the first reviewer from the deque (round-robin among same-weight)
            long reviewerId = ids.removeFirst();
            if (ids.isEmpty()) {
                staffReviewingCount.remove(entry.getKey());
            } else {
                staffReviewingCount.put(entry.getKey(), ids);
            }

            int addedWeight = contentIdentity.getContentType().getWeight();
            // Put reviewer into new weight bucket and update weights map
            int newWeight = entry.getKey() + addedWeight;
            weights.put(reviewerId, newWeight);
            Deque<Long> newDeque = staffReviewingCount.getOrDefault(newWeight, new ArrayDeque<>());
            newDeque.addLast(reviewerId);
            staffReviewingCount.put(newWeight, newDeque);

            return reviewerId;
        } finally {
            lock.writeLock().unlock();
        }
    }

    @Override
    public void releaseReviewer(long reviewerId, ContentIdentity contentIdentity) {
        if (reviewerId == AUTO_REVIEWER) {
            return;
        }
        if (contentIdentity == null) return;

        lock.writeLock().lock();
        try {
            Integer weight = weights.get(reviewerId);
            if (weight == null) {
                return;
            }
            int decrement = (contentIdentity.getContentType() == null) ? 0 : contentIdentity.getContentType().getWeight();
            int newWeight = Math.max(0, weight - decrement);
            remappingReviewer(reviewerId, weight, newWeight);
        } finally {
            lock.writeLock().unlock();
        }
    }

    private boolean canAutoReview(ContentType contentType) {
        return contentType.getWeight() == 0;
    }

    @Override
    public void onStaffCreated(Staff staff) {
        if (staff == null) {
            return;
        }
        lock.writeLock().lock();
        try {
            long userId = staff.getUserId();
            weights.putIfAbsent(userId, 0);
            Deque<Long> deque = staffReviewingCount.getOrDefault(0, new ArrayDeque<>());
            if (!deque.contains(userId)) {
                deque.addLast(userId);
            }
            staffReviewingCount.put(0, deque);
        } finally {
            lock.writeLock().unlock();
        }
    }
}
