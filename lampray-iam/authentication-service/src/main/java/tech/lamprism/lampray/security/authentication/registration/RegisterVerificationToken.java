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

package tech.lamprism.lampray.security.authentication.registration;

import space.lingu.NonNull;
import tech.lamprism.lampray.DataEntity;
import tech.lamprism.lampray.LongEntityBuilder;
import tech.lamprism.lampray.security.authentication.VerifiableToken;
import tech.rollw.common.web.system.SystemResourceKind;

import java.time.OffsetDateTime;
import java.util.Date;

/**
 * Register Verification Token.
 *
 * @author RollW
 */
public record RegisterVerificationToken(
        Long id,
        String token,
        long userId,
        long expiryTime,// timestamp
        boolean used) implements VerifiableToken, DataEntity<Long> {

    public boolean isExpired() {
        return System.currentTimeMillis() > expiryTime;
    }

    private static final int EXPIRATION = 60 * 24;// min

    public static long calculateExpiryDate(int expiryTimeInMinutes) {
        long now = System.currentTimeMillis();
        long millis = expiryTimeInMinutes * 60 * 1000L;
        return now + millis;
    }

    public static long calculateExpiryDate() {
        return calculateExpiryDate(EXPIRATION);
    }

    public Date toDate() {
        return new Date(expiryTime);
    }

    @Override
    public Long getId() {
        return id;
    }

    @NonNull
    @Override
    public OffsetDateTime getCreateTime() {
        return NONE_TIME;
    }

    @NonNull
    @Override
    public OffsetDateTime getUpdateTime() {
        return NONE_TIME;
    }

    public Builder toBuilder() {
        return new Builder(this);
    }

    @NonNull
    @Override
    public SystemResourceKind getSystemResourceKind() {
        return RegisterTokenResourceKind.INSTANCE;
    }

    public static Builder builder() {
        return new Builder();
    }

    public RegisterVerificationToken markVerified() {
        return toBuilder().setUsed(true).build();
    }

    @Override
    public boolean isUsable() {
        return !used && !isExpired();
    }

    public static final class Builder implements LongEntityBuilder<RegisterVerificationToken> {
        private Long id;
        private String token;
        private long userId;
        private long expiryTime;
        private boolean used;

        public Builder() {
        }

        public Builder(RegisterVerificationToken source) {
            this.id = source.id;
            this.token = source.token;
            this.userId = source.userId;
            this.expiryTime = source.expiryTime;
            this.used = source.used;
        }

        @Override
        public Builder setId(Long id) {
            this.id = id;
            return this;
        }

        public Builder setToken(String token) {
            this.token = token;
            return this;
        }

        public Builder setUserId(long userId) {
            this.userId = userId;
            return this;
        }

        public Builder setExpiryTime(long expiryTime) {
            this.expiryTime = expiryTime;
            return this;
        }

        public Builder setUsed(boolean used) {
            this.used = used;
            return this;
        }

        @Override
        public RegisterVerificationToken build() {
            return new RegisterVerificationToken(id, token, userId, expiryTime, used);
        }
    }
}
