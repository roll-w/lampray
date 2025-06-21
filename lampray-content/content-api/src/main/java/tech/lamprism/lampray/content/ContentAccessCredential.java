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

package tech.lamprism.lampray.content;

import com.google.common.base.Preconditions;
import space.lingu.NonNull;
import space.lingu.Nullable;
import tech.lamprism.lampray.user.UserTrait;

import java.util.Arrays;

/**
 * Content access credential.
 *
 * @author RollW
 */
public class ContentAccessCredential {
    @NonNull
    private final Type type;
    private final Object data;

    public ContentAccessCredential(@NonNull Type type, Object data) {
        Preconditions.checkNotNull(type);
        this.type = type;
        this.data = data;
        Type.checkTypeMatches(type, data);
    }

    @NonNull
    public Type getType() {
        return type;
    }

    @Nullable
    public Object getRawData() {
        return data;
    }

    public enum Type {
        /**
         * No credential, used for admin access or user access their own contents.
         * <p>
         * If this type's credential is contained, will never check the other credentials
         * and always allow access.
         */
        NO_LIMIT,

        /**
         * Provides current access user.
         */
        USER(Long.class, UserTrait.class),

        /**
         * Provides the current user group that the current access user belongs to.
         */
        USER_GROUP(Long.class, UserTrait.class),

        /**
         * Provides the password for the target content.
         */
        PASSWORD(String.class);

        private final Class<?>[] types;

        Type(Class<?>... types) {
            this.types = types;
        }

        public boolean needsCheck() {
            return types.length > 0;
        }

        /**
         * Get the types of data that can be used to authenticate.
         *
         * @return supported types.
         */
        public Class<?>[] getTypes() {
            return types;
        }

        public static void checkTypeMatches(Type type, Object data) {
            if (data == null || !type.needsCheck()) {
                return;
            }
            for (Class<?> typeType : type.getTypes()) {
                if (typeType.isAssignableFrom(data.getClass())) {
                    return;
                }
            }
            throw new IllegalArgumentException("Data type is not assignable to any of the types in ContentAccessCredential.Type: " + type +
                    ", supports: " + Arrays.toString(type.getTypes()) + ", data type: " + data.getClass());
        }
    }
}
