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

package tech.lamprism.lampray.storage.awss3;

import org.junit.jupiter.api.Test;
import tech.lamprism.lampray.storage.store.BlobDownloadRequest;
import tech.lamprism.lampray.storage.store.BlobStoreCapability;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * @author RollW
 */
class S3BlobStorePublicUrlTest {
    @Test
    void buildsPathStylePublicUrl() throws Exception {
        try (S3BlobStore blobStore = new S3BlobStore(
                "s3",
                "https://internal-s3.example.com",
                "https://objects.example.com",
                "us-east-1",
                "lampray-assets",
                "root-prefix",
                true,
                "access-key",
                "secret-key"
        )) {
            String url = blobStore.createPublicDownloadUrl(
                    new BlobDownloadRequest("folder/avatar image.png", null, "image/png")
            );

            assertEquals(
                    "https://objects.example.com/lampray-assets/root-prefix/folder/avatar%20image.png",
                    url
            );
            assertTrue(blobStore.supports(BlobStoreCapability.PUBLIC_DOWNLOAD_URL));
        }
    }

    @Test
    void buildsVirtualHostPublicUrl() throws Exception {
        try (S3BlobStore blobStore = new S3BlobStore(
                "s3",
                "https://internal-s3.example.com",
                "https://s3.example.com",
                "us-east-1",
                "lampray-assets",
                "root-prefix",
                false,
                "access-key",
                "secret-key"
        )) {
            String url = blobStore.createPublicDownloadUrl(
                    new BlobDownloadRequest("folder/avatar.png", null, "image/png")
            );

            assertEquals(
                    "https://lampray-assets.s3.example.com/root-prefix/folder/avatar.png",
                    url
            );
        }
    }

    @Test
    void keepsExplicitBucketHostOrPathPrefix() throws Exception {
        try (S3BlobStore blobStore = new S3BlobStore(
                "s3",
                "https://internal-s3.example.com",
                "https://lampray-assets.cdn.example.com/public",
                "us-east-1",
                "lampray-assets",
                "root-prefix",
                false,
                "access-key",
                "secret-key"
        )) {
            String url = blobStore.createPublicDownloadUrl(
                    new BlobDownloadRequest("logo.svg", null, "image/svg+xml")
            );

            assertEquals(
                    "https://lampray-assets.cdn.example.com/public/root-prefix/logo.svg",
                    url
            );
        }
    }
}
