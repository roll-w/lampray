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

package tech.lamprism.lampray.security.token;

import org.bouncycastle.util.io.pem.PemReader;
import org.springframework.stereotype.Component;
import space.lingu.NonNull;
import tech.lamprism.lampray.setting.AttributedSettingSpecification;
import tech.lamprism.lampray.setting.ConfigReader;
import tech.lamprism.lampray.setting.SettingKey;
import tech.lamprism.lampray.setting.SettingSource;
import tech.lamprism.lampray.setting.SettingSpecificationBuilder;
import tech.lamprism.lampray.setting.SettingSpecificationSupplier;

import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import java.io.IOException;
import java.io.StringReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.Key;
import java.security.KeyFactory;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.SecureRandom;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.PKCS8EncodedKeySpec;
import java.util.Base64;
import java.util.List;
import java.util.Locale;

/**
 * Configuration keys for authorization token settings.
 *
 * @author RollW
 */
@Component
public class AuthorizationTokenConfigKeys implements SettingSpecificationSupplier {

    public static final AttributedSettingSpecification<String, String> TOKEN_ISSUER =
            new SettingSpecificationBuilder<>(SettingKey.ofString("token.authorization.issuer"))
                    .setDefaultValue("Lampray")
                    .setSupportedSources(SettingSource.VALUES)
                    .setTextDescription("The issuer name for JWT tokens. This identifies who issued the token.")
                    .setRequired(true)
                    .build();

    public static final AttributedSettingSpecification<Long, Long> TOKEN_EXPIRE_TIME =
            new SettingSpecificationBuilder<>(SettingKey.ofLong("token.authorization.expire-time"))
                    .setDefaultValue(3600L)
                    .setTextDescription("Token expiration time in seconds. Default is 3600 seconds (1 hour).")
                    .setSupportedSources(SettingSource.VALUES)
                    .setRequired(true)
                    .build();

    public static final String RANDOM = "[random]";

    public static final String SIGN_KEY_KEYPAIR = "keypair";
    public static final String SIGN_KEY_SECRET = "secret";

    public static final AttributedSettingSpecification<String, String> TOKEN_KEY_TYPE =
            new SettingSpecificationBuilder<>(SettingKey.ofString("token.authorization.sign-key.type"))
                    .setDefaultValue(SIGN_KEY_SECRET)
                    .setValueEntries(List.of(SIGN_KEY_KEYPAIR, SIGN_KEY_SECRET))
                    .setTextDescription("""
                            The cryptographic signing method for JWT tokens:
                            
                            - keypair: Uses RSA/ECDSA public/private key pair for asymmetric signing
                            - secret: Uses HMAC with a shared secret key for symmetric signing
                            
                            For keypair mode, set the private key in the secret-key field.
                            For secret mode, set the shared secret in the secret-key field.
                            """.trim())
                    .setSupportedSources(SettingSource.VALUES)
                    .setRequired(true)
                    .build();

    public static final AttributedSettingSpecification<String, String> TOKEN_SECRET_KEY =
            new SettingSpecificationBuilder<>(SettingKey.ofString("token.authorization.sign-key.secret-key"))
                    .setDefaultValue(RANDOM)
                    .setTextDescription("""
                            The signing key for JWT tokens. The format depends on the key type:
                            
                            - For 'secret' type: Base64-encoded shared secret
                            - For 'keypair' type: PEM-encoded private key
                            
                            Special values:
                            - '[random]': Generates a new random key on each startup (development only)
                            
                            Key source formats:
                            - Direct value: 'key-value:<base64-secret>' or 'key-value:<pem-private-key>'
                            - File reference: 'file:<path-to-key-file>'
                            
                            Note: Random keys are not suitable for production as they invalidate
                            existing tokens on restart.
                            """.trim())
                    .setSupportedSources(SettingSource.VALUES)
                    .setRequired(true)
                    .build();

    public static final AttributedSettingSpecification<String, String> TOKEN_KEY_ALGORITHM =
            new SettingSpecificationBuilder<>(SettingKey.ofString("token.authorization.sign-key.algorithm"))
                    .setDefaultValue("HmacSHA256")
                    .setTextDescription("The cryptographic algorithm for token signing. " +
                            "Commonly used: HmacSHA256, HmacSHA384, HmacSHA512 for secret keys. " +
                            "This setting is ignored when using keypair signing.")
                    .setSupportedSources(SettingSource.VALUES)
                    .setAllowAnyValue(true)
                    .setRequired(true)
                    .build();

    private static final List<AttributedSettingSpecification<?, ?>> SPECIFICATIONS =
            List.of(TOKEN_ISSUER, TOKEN_EXPIRE_TIME, TOKEN_KEY_TYPE, TOKEN_SECRET_KEY, TOKEN_KEY_ALGORITHM);

    @Override
    @NonNull
    public List<AttributedSettingSpecification<?, ?>> getSpecifications() {
        return SPECIFICATIONS;
    }

    /**
     * Parses the secret key from the configuration reader.
     * If the key is set to "[random]", generates a new random key.
     */
    @NonNull
    public static Key parseSecretKey(@NonNull ConfigReader configReader) {
        String keyValue = configReader.get(TOKEN_SECRET_KEY);
        if (RANDOM.equalsIgnoreCase(keyValue)) {
            return generateRandomKey(configReader);
        }

        String type = configReader.get(TOKEN_KEY_TYPE);
        if (type == null) {
            throw new IllegalArgumentException("Token key type cannot be null");
        }

        String actualKeyValue = readValue(keyValue);
        if (actualKeyValue == null || actualKeyValue.isBlank()) {
            throw new IllegalArgumentException("Key value cannot be empty");
        }

        return switch (type.toLowerCase(Locale.ROOT)) {
            case SIGN_KEY_KEYPAIR -> parsePrivateKey(actualKeyValue);
            case SIGN_KEY_SECRET -> parseSecretKeyFromString(actualKeyValue, configReader);
            default -> throw new IllegalArgumentException("Unknown token sign key type: " + type);
        };
    }

    /**
     * Generates a random secret key for HMAC signing.
     */
    private static SecretKey generateRandomKey(@NonNull ConfigReader configReader) {
        try {
            String algorithmConfig = configReader.get(TOKEN_KEY_ALGORITHM);
            String algorithm = extractHmacAlgorithm(algorithmConfig != null ? algorithmConfig : "HmacSHA256");
            KeyGenerator keyGenerator = KeyGenerator.getInstance(algorithm);
            keyGenerator.init(256, new SecureRandom());
            return keyGenerator.generateKey();
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalArgumentException("Unsupported algorithm for random key generation", e);
        }
    }

    /**
     * Parses a private key from PEM format string.
     */
    private static PrivateKey parsePrivateKey(String keyContent) {
        try {
            PemReader pemReader = new PemReader(new StringReader(keyContent));
            byte[] keyBytes = pemReader.readPemObject().getContent();
            PKCS8EncodedKeySpec keySpec = new PKCS8EncodedKeySpec(keyBytes);

            // Try RSA first, then EC
            try {
                KeyFactory keyFactory = KeyFactory.getInstance("RSA");
                return keyFactory.generatePrivate(keySpec);
            } catch (InvalidKeySpecException e) {
                KeyFactory keyFactory = KeyFactory.getInstance("EC");
                return keyFactory.generatePrivate(keySpec);
            }
        } catch (Exception e) {
            throw new IllegalArgumentException("Failed to parse private key from PEM format", e);
        }
    }

    /**
     * Parses a secret key from base64 encoded string.
     */
    private static SecretKey parseSecretKeyFromString(String keyContent, @NonNull ConfigReader configReader) {
        try {
            byte[] keyBytes = Base64.getDecoder().decode(keyContent.trim());
            String algorithmConfig = configReader.get(TOKEN_KEY_ALGORITHM);
            String algorithm = extractHmacAlgorithm(algorithmConfig != null ? algorithmConfig : "HmacSHA256");
            return new SecretKeySpec(keyBytes, algorithm);
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Failed to parse secret key from base64 format", e);
        }
    }

    /**
     * Extracts the HMAC algorithm name from the full algorithm string.
     */
    private static String extractHmacAlgorithm(String algorithm) {
        return algorithm.startsWith("Hmac") ? algorithm : "Hmac" + algorithm;
    }

    /**
     * Reads the actual key value from various sources.
     */
    private static String readValue(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }

        if (value.startsWith("file:")) {
            return readFromFile(value.substring(5).trim());
        }

        if (value.startsWith("key-value:")) {
            return value.substring(10).trim();
        }

        // Direct value
        return value.trim();
    }

    /**
     * Reads key content from a file.
     */
    private static String readFromFile(String filePath) {
        try {
            Path path = Path.of(filePath);
            if (!Files.exists(path)) {
                throw new IllegalArgumentException("Key file not found: " + filePath);
            }
            return Files.readString(path).trim();
        } catch (IOException e) {
            throw new IllegalArgumentException("Failed to read key file: " + filePath, e);
        }
    }
}
