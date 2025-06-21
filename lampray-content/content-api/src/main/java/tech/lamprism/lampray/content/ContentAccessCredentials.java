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

import space.lingu.NonNull;
import tech.rollw.common.web.system.SystemAuthenticateCredentials;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Utility class for {@link ContentAccessCredential} collection.
 *
 * @author RollW
 */
public final class ContentAccessCredentials implements SystemAuthenticateCredentials {
    private final List<ContentAccessCredential> contentAccessCredentials;

    public ContentAccessCredentials(List<ContentAccessCredential> contentAccessCredentials) {
        this.contentAccessCredentials = contentAccessCredentials;
    }

    public ContentAccessCredentials(ContentAccessCredential... contentAccessCredentials) {
        this(List.of(contentAccessCredentials));
    }

    public List<ContentAccessCredential> getContentAccessCredentials() {
        return Collections.unmodifiableList(contentAccessCredentials);
    }

    public boolean hasType(ContentAccessCredential.Type type) {
        for (ContentAccessCredential credential : contentAccessCredentials) {
            if (credential == null) {
                continue;
            }
            if (credential.getType() == type) {
                return true;
            }
        }
        return false;
    }

    public List<ContentAccessCredential> getCredentials(ContentAccessCredential.Type type) {
        List<ContentAccessCredential> result = new ArrayList<>();
        for (ContentAccessCredential credential : contentAccessCredentials) {
            if (credential == null) {
                continue;
            }
            if (credential.getType() == type) {
                result.add(credential);
            }
        }
        return result;
    }

    public ContentAccessCredential getCredential(ContentAccessCredential.Type type) {
        for (ContentAccessCredential credential : contentAccessCredentials) {
            if (credential == null) {
                continue;
            }

            if (credential.getType() == type) {
                return credential;
            }
        }
        return null;
    }

    public static ContentAccessCredentials empty() {
        return new ContentAccessCredentials(Collections.emptyList());
    }

    public static ContentAccessCredentials of(ContentAccessCredential... credentials) {
        return new ContentAccessCredentials(credentials);
    }

    public static ContentAccessCredentials of(List<ContentAccessCredential> credentials) {
        return new ContentAccessCredentials(credentials);
    }

    public static ContentAccessCredentials of(@NonNull ContentAccessCredential.Type type, Object data) {
        return new ContentAccessCredentials(new ContentAccessCredential(type, data));
    }


    public static ContentAccessCredentials of(@NonNull ContentAccessCredential.Type type1, Object data1,
                                              @NonNull ContentAccessCredential.Type type2, Object data2) {
        return new ContentAccessCredentials(
                new ContentAccessCredential(type1, data1),
                new ContentAccessCredential(type2, data2)
        );
    }

    public static ContentAccessCredentials of(@NonNull ContentAccessCredential.Type type1, Object data1,
                                              @NonNull ContentAccessCredential.Type type2, Object data2,
                                              @NonNull ContentAccessCredential.Type type3, Object data3) {
        return new ContentAccessCredentials(
                new ContentAccessCredential(type1, data1),
                new ContentAccessCredential(type2, data2),
                new ContentAccessCredential(type3, data3)
        );
    }

    public static final ContentAccessCredentials NO_LIMIT = new ContentAccessCredentials(
            new ContentAccessCredential(ContentAccessCredential.Type.NO_LIMIT, null)
    );

    public static final ContentAccessCredentials ANONYMOUS = new ContentAccessCredentials();
}
