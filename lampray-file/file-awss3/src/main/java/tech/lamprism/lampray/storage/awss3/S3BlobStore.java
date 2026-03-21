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

import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.AwsCredentialsProvider;
import software.amazon.awssdk.auth.credentials.DefaultCredentialsProvider;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.core.ResponseInputStream;
import software.amazon.awssdk.core.exception.SdkException;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.S3ClientBuilder;
import software.amazon.awssdk.services.s3.S3Configuration;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.ChecksumAlgorithm;
import software.amazon.awssdk.services.s3.model.ChecksumMode;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectResponse;
import software.amazon.awssdk.services.s3.model.HeadObjectRequest;
import software.amazon.awssdk.services.s3.model.HeadObjectResponse;
import software.amazon.awssdk.services.s3.model.NoSuchKeyException;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.model.S3Exception;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.GetObjectPresignRequest;
import software.amazon.awssdk.services.s3.presigner.model.PresignedGetObjectRequest;
import software.amazon.awssdk.services.s3.presigner.model.PresignedPutObjectRequest;
import software.amazon.awssdk.services.s3.presigner.model.PutObjectPresignRequest;
import tech.lamprism.lampray.storage.StorageAccessRequest;
import tech.lamprism.lampray.storage.StorageByteRange;
import tech.lamprism.lampray.storage.StorageDownloadSource;
import tech.lamprism.lampray.storage.source.InputStreamDownloadSource;
import tech.lamprism.lampray.storage.store.BlobDownloadRequest;
import tech.lamprism.lampray.storage.store.BlobObject;
import tech.lamprism.lampray.storage.store.BlobStore;
import tech.lamprism.lampray.storage.store.BlobStoreCapability;
import tech.lamprism.lampray.storage.store.BlobWriteRequest;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Base64;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/**
 * @author RollW
 */
public class S3BlobStore implements BlobStore, AutoCloseable {
    private static final String DEFAULT_CONTENT_TYPE = "application/octet-stream";
    private static final Duration DEFAULT_ACCESS_DURATION = Duration.ofMinutes(5);
    private static final Duration MAX_ACCESS_DURATION = Duration.ofDays(7);

    private final String backendName;
    private final String bucket;
    private final String rootPrefix;
    private final URI publicEndpoint;
    private final boolean pathStyleAccess;
    private final boolean nativeChecksumEnabled;
    private final S3Client s3Client;
    private final S3Presigner s3Presigner;

    public S3BlobStore(String backendName,
                       String endpoint,
                       String publicEndpoint,
                       boolean nativeChecksumEnabled,
                       String region,
                       String bucket,
                       String rootPrefix,
                       boolean pathStyleAccess,
                       String accessKey,
                       String secretKey) {
        this.backendName = requireText(backendName, "backendName");
        this.bucket = requireText(bucket, "bucket");
        this.rootPrefix = normalizeRootPrefix(rootPrefix);
        this.publicEndpoint = resolveEndpoint(publicEndpoint);
        this.pathStyleAccess = pathStyleAccess;
        this.nativeChecksumEnabled = nativeChecksumEnabled;

        AwsCredentialsProvider credentialsProvider = resolveCredentialsProvider(accessKey, secretKey);
        Region awsRegion = Region.of(normalizeRegion(region));
        URI endpointOverride = resolveEndpoint(endpoint);
        S3Configuration serviceConfiguration = S3Configuration.builder()
                .pathStyleAccessEnabled(pathStyleAccess)
                .build();

        S3ClientBuilder clientBuilder = S3Client.builder()
                .region(awsRegion)
                .serviceConfiguration(serviceConfiguration)
                .credentialsProvider(credentialsProvider);

        S3Presigner.Builder presignerBuilder = S3Presigner.builder()
                .region(awsRegion)
                .serviceConfiguration(serviceConfiguration)
                .credentialsProvider(credentialsProvider);

        if (endpointOverride != null) {
            clientBuilder.endpointOverride(endpointOverride);
            presignerBuilder.endpointOverride(endpointOverride);
        }

        this.s3Client = clientBuilder.build();
        this.s3Presigner = presignerBuilder.build();
    }

    public S3BlobStore(String backendName,
                       String bucket,
                       String rootPrefix,
                       S3Client s3Client,
                       S3Presigner s3Presigner) {
        this.backendName = requireText(backendName, "backendName");
        this.bucket = requireText(bucket, "bucket");
        this.rootPrefix = normalizeRootPrefix(rootPrefix);
        this.publicEndpoint = null;
        this.pathStyleAccess = false;
        this.nativeChecksumEnabled = true;
        this.s3Client = Objects.requireNonNull(s3Client, "s3Client must not be null");
        this.s3Presigner = Objects.requireNonNull(s3Presigner, "s3Presigner must not be null");
    }

    @Override
    public String getBackendName() {
        return backendName;
    }

    @Override
    public Set<BlobStoreCapability> getCapabilities() {
        Set<BlobStoreCapability> capabilities = new LinkedHashSet<>();
        capabilities.add(BlobStoreCapability.DIRECT_UPLOAD);
        capabilities.add(BlobStoreCapability.DIRECT_DOWNLOAD);
        if (publicEndpoint != null) {
            capabilities.add(BlobStoreCapability.PUBLIC_DOWNLOAD_URL);
        }
        return Set.copyOf(capabilities);
    }

    @Override
    public BlobObject store(BlobWriteRequest request,
                            InputStream inputStream) throws IOException {
        Objects.requireNonNull(inputStream, "inputStream must not be null");
        validateContentLength(request.getSize());

        String normalizedKey = normalizeObjectKey(request.getKey());
        PutObjectRequest objectRequest = buildPutObjectRequest(normalizedKey, request);

        try {
            s3Client.putObject(objectRequest, RequestBody.fromInputStream(inputStream, request.getSize()));
            return describe(normalizedKey);
        } catch (SdkException exception) {
            throw toIOException("put", normalizedKey, exception);
        }
    }

    @Override
    public StorageDownloadSource openDownload(String key) throws IOException {
        String normalizedKey = normalizeObjectKey(key);
        return new InputStreamDownloadSource(
                () -> openObjectStream(normalizedKey, null),
                range -> {
                    validateRange(range);
                    return openObjectStream(normalizedKey, "bytes=" + range.startBytes() + "-" + range.endBytes());
                }
        );
    }

    @Override
    public BlobObject describe(String key) throws IOException {
        String normalizedKey = normalizeObjectKey(key);
        try {
            HeadObjectResponse response = headObject(normalizedKey, nativeChecksumEnabled);
            return toBlobObject(normalizedKey, response);
        } catch (NoSuchKeyException exception) {
            throw new IOException("S3 object does not exist: " + normalizedKey, exception);
        } catch (S3Exception exception) {
            if (isObjectNotFound(exception)) {
                throw new IOException("S3 object does not exist: " + normalizedKey, exception);
            }
            if (!nativeChecksumEnabled) {
                throw toIOException("stat", normalizedKey, exception);
            }
            try {
                return toBlobObject(normalizedKey, headObject(normalizedKey, false));
            } catch (NoSuchKeyException fallbackException) {
                throw new IOException("S3 object does not exist: " + normalizedKey, fallbackException);
            } catch (S3Exception fallbackException) {
                if (isObjectNotFound(fallbackException)) {
                    throw new IOException("S3 object does not exist: " + normalizedKey, fallbackException);
                }
                throw toIOException("stat", normalizedKey, exception);
            } catch (SdkException fallbackException) {
                throw toIOException("stat", normalizedKey, exception);
            }
        } catch (SdkException exception) {
            if (!nativeChecksumEnabled) {
                throw toIOException("stat", normalizedKey, exception);
            }
            try {
                return toBlobObject(normalizedKey, headObject(normalizedKey, false));
            } catch (NoSuchKeyException fallbackException) {
                throw new IOException("S3 object does not exist: " + normalizedKey, fallbackException);
            } catch (S3Exception fallbackException) {
                if (isObjectNotFound(fallbackException)) {
                    throw new IOException("S3 object does not exist: " + normalizedKey, fallbackException);
                }
                throw toIOException("stat", normalizedKey, exception);
            } catch (SdkException fallbackException) {
                throw toIOException("stat", normalizedKey, exception);
            }
        }
    }

    @Override
    public boolean exists(String key) throws IOException {
        String normalizedKey = normalizeObjectKey(key);

        try {
            s3Client.headObject(HeadObjectRequest.builder()
                    .bucket(bucket)
                    .key(toStorageKey(normalizedKey))
                    .build());
            return true;
        } catch (NoSuchKeyException exception) {
            return false;
        } catch (S3Exception exception) {
            if (isObjectNotFound(exception)) {
                return false;
            }
            throw toIOException("check existence", normalizedKey, exception);
        } catch (SdkException exception) {
            throw toIOException("check existence", normalizedKey, exception);
        }
    }

    @Override
    public boolean delete(String key) throws IOException {
        String normalizedKey = normalizeObjectKey(key);
        if (!exists(normalizedKey)) {
            return false;
        }

        try {
            s3Client.deleteObject(DeleteObjectRequest.builder()
                    .bucket(bucket)
                    .key(toStorageKey(normalizedKey))
                    .build());
            return true;
        } catch (S3Exception exception) {
            if (isObjectNotFound(exception)) {
                return false;
            }
            throw toIOException("delete", normalizedKey, exception);
        } catch (SdkException exception) {
            throw toIOException("delete", normalizedKey, exception);
        }
    }

    @Override
    public StorageAccessRequest createDirectUpload(BlobWriteRequest request,
                                                   Duration duration) throws IOException {
        validateContentLength(request.getSize());
        String normalizedKey = normalizeObjectKey(request.getKey());
        PutObjectRequest objectRequest = buildPutObjectRequest(normalizedKey, request);

        PutObjectPresignRequest putRequestToSign = PutObjectPresignRequest.builder()
                .signatureDuration(normalizeAccessDuration(duration))
                .putObjectRequest(objectRequest)
                .build();

        try {
            PresignedPutObjectRequest generatedRequest = s3Presigner.presignPutObject(putRequestToSign);
            return new StorageAccessRequest(
                    generatedRequest.httpRequest().method().name(),
                    generatedRequest.url().toString(),
                    flattenHeaders(generatedRequest.httpRequest().headers()),
                    OffsetDateTime.ofInstant(generatedRequest.expiration(), ZoneOffset.UTC)
            );
        } catch (SdkException exception) {
            throw toIOException("create direct upload", normalizedKey, exception);
        }
    }

    @Override
    public StorageAccessRequest createDirectDownload(BlobDownloadRequest request,
                                                     Duration duration) throws IOException {
        String normalizedKey = normalizeObjectKey(request.getKey());

        GetObjectRequest.Builder objectRequestBuilder = GetObjectRequest.builder()
                .bucket(bucket)
                .key(toStorageKey(normalizedKey));

        if (hasText(request.getContentType())) {
            objectRequestBuilder.responseContentType(request.getContentType().trim());
        }

        if (hasText(request.getFileName())) {
            objectRequestBuilder.responseContentDisposition(buildContentDisposition(request.getFileName()));
        }

        GetObjectPresignRequest getRequestToSign = GetObjectPresignRequest.builder()
                .signatureDuration(normalizeAccessDuration(duration))
                .getObjectRequest(objectRequestBuilder.build())
                .build();

        try {
            PresignedGetObjectRequest generatedRequest = s3Presigner.presignGetObject(getRequestToSign);
            return new StorageAccessRequest(
                    generatedRequest.httpRequest().method().name(),
                    generatedRequest.url().toString(),
                    flattenHeaders(generatedRequest.httpRequest().headers()),
                    OffsetDateTime.ofInstant(generatedRequest.expiration(), ZoneOffset.UTC)
            );
        } catch (SdkException exception) {
            throw toIOException("create direct download", normalizedKey, exception);
        }
    }

    @Override
    public String createPublicDownloadUrl(BlobDownloadRequest request) {
        if (publicEndpoint == null) {
            throw new UnsupportedOperationException(
                    "Blob store backend does not support public download urls: " + getBackendName()
            );
        }
        return buildPublicObjectUrl(normalizeObjectKey(request.getKey()));
    }

    @Override
    public void close() {
        s3Client.close();
        s3Presigner.close();
    }

    private ResponseInputStream<GetObjectResponse> openObjectStream(String key,
                                                                    String range) throws IOException {
        GetObjectRequest.Builder requestBuilder = GetObjectRequest.builder()
                .bucket(bucket)
                .key(toStorageKey(key));
        if (hasText(range)) {
            requestBuilder.range(range);
        }
        try {
            return s3Client.getObject(requestBuilder.build());
        } catch (NoSuchKeyException exception) {
            throw new IOException("S3 object does not exist: " + key, exception);
        } catch (S3Exception exception) {
            if (isObjectNotFound(exception)) {
                throw new IOException("S3 object does not exist: " + key, exception);
            }
            throw toIOException("read", key, exception);
        } catch (SdkException exception) {
            throw toIOException("read", key, exception);
        }
    }

    private BlobObject toBlobObject(String key, HeadObjectResponse response) {
        String contentType = hasText(response.contentType()) ? response.contentType() : DEFAULT_CONTENT_TYPE;
        long size = response.contentLength() != null ? response.contentLength() : 0L;
        OffsetDateTime lastModified = response.lastModified() != null
                ? OffsetDateTime.ofInstant(response.lastModified(), ZoneOffset.UTC)
                : OffsetDateTime.now(ZoneOffset.UTC);

        return new BlobObject(
                backendName,
                key,
                size,
                contentType,
                response.eTag(),
                resolveChecksumSha256(response),
                lastModified,
                normalizeMetadata(response.metadata())
        );
    }

    private HeadObjectResponse headObject(String normalizedKey,
                                          boolean includeChecksumMode) {
        HeadObjectRequest.Builder requestBuilder = HeadObjectRequest.builder()
                .bucket(bucket)
                .key(toStorageKey(normalizedKey));
        if (includeChecksumMode) {
            requestBuilder.checksumMode(ChecksumMode.ENABLED);
        }
        return s3Client.headObject(requestBuilder.build());
    }

    private PutObjectRequest buildPutObjectRequest(String normalizedKey,
                                                   BlobWriteRequest request) {
        PutObjectRequest.Builder requestBuilder = PutObjectRequest.builder()
                .bucket(bucket)
                .key(toStorageKey(normalizedKey))
                .contentLength(request.getSize())
                .metadata(normalizeMetadata(request.getMetadata()));
        if (hasText(request.getContentType())) {
            requestBuilder.contentType(request.getContentType().trim());
        }
        if (nativeChecksumEnabled && hasText(request.getChecksumSha256())) {
            requestBuilder.checksumAlgorithm(ChecksumAlgorithm.SHA256);
            requestBuilder.checksumSHA256(hexToBase64Checksum(request.getChecksumSha256().trim()));
        }
        return requestBuilder.build();
    }

    private String resolveChecksumSha256(HeadObjectResponse response) {
        if (hasText(response.checksumSHA256())) {
            return base64ToHexChecksum(response.checksumSHA256());
        }
        Map<String, String> metadata = normalizeMetadata(response.metadata());
        String metadataChecksum = metadata.get("checksum-sha256");
        return hasText(metadataChecksum) ? metadataChecksum.trim().toLowerCase(java.util.Locale.ROOT) : null;
    }

    private String hexToBase64Checksum(String checksumSha256) {
        return Base64.getEncoder().encodeToString(hexToBytes(checksumSha256));
    }

    private String base64ToHexChecksum(String base64Checksum) {
        return toHex(Base64.getDecoder().decode(base64Checksum));
    }

    private byte[] hexToBytes(String hexValue) {
        if (hexValue.length() % 2 != 0) {
            throw new IllegalArgumentException("Checksum must have an even number of characters.");
        }
        byte[] bytes = new byte[hexValue.length() / 2];
        for (int i = 0; i < hexValue.length(); i += 2) {
            int high = Character.digit(hexValue.charAt(i), 16);
            int low = Character.digit(hexValue.charAt(i + 1), 16);
            if (high < 0 || low < 0) {
                throw new IllegalArgumentException("Checksum must be a hexadecimal string.");
            }
            bytes[i / 2] = (byte) ((high << 4) + low);
        }
        return bytes;
    }

    private String toHex(byte[] bytes) {
        StringBuilder builder = new StringBuilder(bytes.length * 2);
        for (byte current : bytes) {
            builder.append(Character.forDigit((current >> 4) & 0xF, 16));
            builder.append(Character.forDigit(current & 0xF, 16));
        }
        return builder.toString();
    }

    private static AwsCredentialsProvider resolveCredentialsProvider(String accessKey, String secretKey) {
        boolean hasAccessKey = hasText(accessKey);
        boolean hasSecretKey = hasText(secretKey);
        if (hasAccessKey != hasSecretKey) {
            throw new IllegalArgumentException("accessKey and secretKey must be provided together");
        }

        if (!hasAccessKey) {
            return DefaultCredentialsProvider.create();
        }

        return StaticCredentialsProvider.create(AwsBasicCredentials.create(accessKey.trim(), secretKey.trim()));
    }

    private static URI resolveEndpoint(String endpoint) {
        if (!hasText(endpoint)) {
            return null;
        }

        try {
            URI uri = URI.create(endpoint.trim());
            if (!uri.isAbsolute() || !hasText(uri.getScheme()) || !hasText(uri.getHost())) {
                throw new IllegalArgumentException("Endpoint URI must be an absolute http or https URL: " + endpoint);
            }
            String scheme = uri.getScheme().trim().toLowerCase(java.util.Locale.ROOT);
            if (!"http".equals(scheme) && !"https".equals(scheme)) {
                throw new IllegalArgumentException("Endpoint URI scheme must be http or https: " + endpoint);
            }
            return uri;
        } catch (IllegalArgumentException exception) {
            throw new IllegalArgumentException("Invalid endpoint URI: " + endpoint, exception);
        }
    }

    private static String normalizeRegion(String region) {
        if (!hasText(region)) {
            return "us-east-1";
        }
        return region.trim();
    }

    private static Map<String, String> flattenHeaders(Map<String, List<String>> headers) {
        if (headers == null || headers.isEmpty()) {
            return Map.of();
        }

        Map<String, String> flattened = new LinkedHashMap<>();
        for (Map.Entry<String, List<String>> entry : headers.entrySet()) {
            String name = entry.getKey();
            List<String> values = entry.getValue();
            if (!hasText(name) || values == null || values.isEmpty() || "host".equalsIgnoreCase(name)) {
                continue;
            }
            flattened.put(name, values.get(0));
        }
        return flattened.isEmpty() ? Map.of() : Map.copyOf(flattened);
    }

    private static String normalizeObjectKey(String key) {
        String normalized = requireText(key, "key");
        while (normalized.startsWith("/")) {
            normalized = normalized.substring(1);
        }
        if (normalized.isEmpty()) {
            throw new IllegalArgumentException("key must not be blank");
        }
        return normalized;
    }

    private String buildPublicObjectUrl(String key) {
        String host = publicEndpoint.getHost();
        if (shouldPrefixBucketHost()) {
            host = bucket + "." + host;
        }

        String path = buildPublicPath(toStorageKey(key));
        try {
            return new URI(
                    publicEndpoint.getScheme(),
                    publicEndpoint.getUserInfo(),
                    host,
                    publicEndpoint.getPort(),
                    path,
                    null,
                    null
            ).toASCIIString();
        } catch (Exception exception) {
            throw new IllegalStateException("Failed to build public S3 object url.", exception);
        }
    }

    private String buildPublicPath(String storageKey) {
        List<String> segments = new ArrayList<>();
        appendPathSegments(segments, publicEndpoint.getPath());
        if (pathStyleAccess) {
            segments.add(bucket);
        }
        appendPathSegments(segments, storageKey);
        if (segments.isEmpty()) {
            return "/";
        }
        return "/" + String.join("/", segments.stream().map(S3BlobStore::encodePathSegment).toList());
    }

    private boolean shouldPrefixBucketHost() {
        if (pathStyleAccess || publicEndpoint == null || !hasText(publicEndpoint.getHost())) {
            return false;
        }
        if (hasText(publicEndpoint.getPath()) && !"/".equals(publicEndpoint.getPath().trim())) {
            return false;
        }
        String normalizedHost = publicEndpoint.getHost().trim().toLowerCase(java.util.Locale.ROOT);
        String bucketPrefix = bucket.toLowerCase(java.util.Locale.ROOT) + ".";
        return !normalizedHost.equals(bucket.toLowerCase(java.util.Locale.ROOT))
                && !normalizedHost.startsWith(bucketPrefix);
    }

    private static void appendPathSegments(List<String> segments,
                                           String path) {
        if (!hasText(path)) {
            return;
        }

        int start = 0;
        while (start < path.length()) {
            while (start < path.length() && path.charAt(start) == '/') {
                start++;
            }
            if (start >= path.length()) {
                return;
            }
            int end = start;
            while (end < path.length() && path.charAt(end) != '/') {
                end++;
            }
            String segment = path.substring(start, end).trim();
            if (!segment.isEmpty()) {
                segments.add(segment);
            }
            start = end + 1;
        }
    }

    private static String encodePathSegment(String segment) {
        return URLEncoder.encode(segment, StandardCharsets.UTF_8).replace("+", "%20");
    }

    private String toStorageKey(String key) {
        if (rootPrefix.isEmpty()) {
            return key;
        }
        return rootPrefix + "/" + key;
    }

    private static String normalizeRootPrefix(String rootPrefix) {
        if (!hasText(rootPrefix)) {
            return "";
        }

        String normalized = rootPrefix.trim();
        while (normalized.startsWith("/")) {
            normalized = normalized.substring(1);
        }
        while (normalized.endsWith("/")) {
            normalized = normalized.substring(0, normalized.length() - 1);
        }
        return normalized;
    }

    private static void validateContentLength(long size) {
        if (size < 0) {
            throw new IllegalArgumentException("size must be non-negative");
        }
    }

    private static void validateRange(StorageByteRange range) {
        Objects.requireNonNull(range, "range must not be null");
    }

    private static Duration normalizeAccessDuration(Duration duration) {
        Duration normalized = duration == null ? DEFAULT_ACCESS_DURATION : duration;
        if (normalized.isNegative() || normalized.isZero()) {
            throw new IllegalArgumentException("duration must be positive");
        }
        if (normalized.compareTo(MAX_ACCESS_DURATION) > 0) {
            throw new IllegalArgumentException("duration exceeds maximum of 7 days");
        }
        return normalized;
    }

    private static String buildContentDisposition(String fileName) {
        String normalized = requireText(fileName, "fileName");
        String fallbackName = toAsciiFileName(normalized);

        String encoded = URLEncoder.encode(normalized, StandardCharsets.UTF_8).replace("+", "%20");
        return "attachment; filename=\"" + fallbackName + "\"; filename*=UTF-8''" + encoded;
    }

    private static String toAsciiFileName(String fileName) {
        StringBuilder builder = new StringBuilder(fileName.length());
        for (int i = 0; i < fileName.length(); i++) {
            char current = fileName.charAt(i);
            if (current <= 31 || current == '"' || current == '\\' || current == '/'
                    || current == ';' || current > 126) {
                builder.append('_');
                continue;
            }
            builder.append(current);
        }
        String fallback = builder.toString().trim();
        return fallback.isEmpty() ? "download" : fallback;
    }

    private static Map<String, String> normalizeMetadata(Map<String, String> metadata) {
        if (metadata == null || metadata.isEmpty()) {
            return Map.of();
        }

        Map<String, String> normalized = new LinkedHashMap<>();
        for (Map.Entry<String, String> entry : metadata.entrySet()) {
            String key = entry.getKey();
            String value = entry.getValue();
            if (!hasText(key) || value == null) {
                continue;
            }
            normalized.put(key.trim(), value);
        }
        return normalized.isEmpty() ? Map.of() : Map.copyOf(normalized);
    }

    private boolean isObjectNotFound(S3Exception exception) {
        if (exception.statusCode() == 404) {
            return true;
        }

        if (exception.awsErrorDetails() == null) {
            return false;
        }

        String errorCode = exception.awsErrorDetails().errorCode();
        return "NotFound".equals(errorCode) || "NoSuchKey".equals(errorCode);
    }

    private IOException toIOException(String operation, String key, Exception exception) {
        StringBuilder message = new StringBuilder("Failed to ")
                .append(operation)
                .append(" S3 object '")
                .append(key)
                .append("' in backend '")
                .append(backendName)
                .append("'");

        if (exception instanceof S3Exception s3Exception && s3Exception.awsErrorDetails() != null) {
            String errorCode = s3Exception.awsErrorDetails().errorCode();
            String errorMessage = s3Exception.awsErrorDetails().errorMessage();
            if (hasText(errorCode)) {
                message.append(" (").append(errorCode).append(')');
            }
            if (hasText(errorMessage)) {
                message.append(": ").append(errorMessage);
            }
        }

        return new IOException(message.toString(), exception);
    }

    private static String requireText(String value, String fieldName) {
        if (!hasText(value)) {
            throw new IllegalArgumentException(fieldName + " must not be blank");
        }
        return value.trim();
    }

    private static boolean hasText(String value) {
        return value != null && !value.trim().isEmpty();
    }
}
