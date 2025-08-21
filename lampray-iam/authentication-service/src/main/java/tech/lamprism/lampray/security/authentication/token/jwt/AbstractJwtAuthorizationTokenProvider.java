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

package tech.lamprism.lampray.security.authentication.token.jwt;

import com.google.common.hash.HashCode;
import com.google.common.hash.Hasher;
import com.google.common.hash.Hashing;
import com.google.common.io.BaseEncoding;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.Jws;
import io.jsonwebtoken.JwtBuilder;
import io.jsonwebtoken.JwtParserBuilder;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.SecurityException;
import kotlin.Pair;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import space.lingu.NonNull;
import tech.lamprism.lampray.authentication.SecurityConfigKeys;
import tech.lamprism.lampray.security.authorization.AuthorizationScope;
import tech.lamprism.lampray.security.authorization.AuthorizationScopeProvider;
import tech.lamprism.lampray.security.token.AuthorizationToken;
import tech.lamprism.lampray.security.token.AuthorizationTokenConfigKeys;
import tech.lamprism.lampray.security.token.AuthorizationTokenProvider;
import tech.lamprism.lampray.security.token.MetadataAuthorizationToken;
import tech.lamprism.lampray.security.token.SimpleMetadataAuthorizationToken;
import tech.lamprism.lampray.security.token.SubjectType;
import tech.lamprism.lampray.security.token.TokenFormat;
import tech.lamprism.lampray.security.token.TokenSignKeyProvider;
import tech.lamprism.lampray.security.token.TokenSubject;
import tech.lamprism.lampray.security.token.TokenSubjectProvider;
import tech.lamprism.lampray.security.token.TokenType;
import tech.lamprism.lampray.setting.ConfigReader;
import tech.rollw.common.web.AuthErrorCode;
import tech.rollw.common.web.CommonRuntimeException;
import tech.rollw.common.web.system.AuthenticationException;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.security.Key;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.time.Duration;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Collection;
import java.util.Date;
import java.util.List;

/**
 * @author RollW
 */
public abstract class AbstractJwtAuthorizationTokenProvider implements AuthorizationTokenProvider {
    public static final String SIGN_FIELD = "sign";
    public static final String SCOPES_FIELD = "scopes";
    public static final String TOKEN_ID_FIELD = "jti";
    public static final String TOKEN_TYPE_FIELD = "token_type";

    private static final Logger logger = LoggerFactory.getLogger(AbstractJwtAuthorizationTokenProvider.class);

    protected final ConfigReader configReader;
    protected final AuthorizationScopeProvider authorizationScopeProvider;
    protected final TokenSubjectProvider tokenSubjectProvider;

    protected final Key signKey;

    public AbstractJwtAuthorizationTokenProvider(ConfigReader configReader,
                                                 AuthorizationScopeProvider authorizationScopeProvider,
                                                 TokenSubjectProvider tokenSubjectProvider) {
        this.configReader = configReader;
        this.signKey = createSignKey(configReader);
        this.authorizationScopeProvider = authorizationScopeProvider;
        this.tokenSubjectProvider = tokenSubjectProvider;
    }

    private Key createSignKey(ConfigReader configReader) {
        return AuthorizationTokenConfigKeys.parseSecretKey(configReader);
    }

    private PublicKey getPublicKey(Key secretKey) {
        if (secretKey instanceof SecretKey) {
            throw new IllegalArgumentException("SecretKey cannot be used as a public key");
        }
        if (!(secretKey instanceof PrivateKey privateKey)) {
            throw new IllegalArgumentException("Key must be a PrivateKey to derive a public key");
        }
        try {
            return KeyUtils.derivePublicKey(privateKey);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private JwtParserBuilder verifyWith(JwtParserBuilder jwtParserBuilder, Key signKey) {
        if (signKey instanceof SecretKey secretKey) {
            return jwtParserBuilder.verifyWith(secretKey);
        }
        return jwtParserBuilder.verifyWith(getPublicKey(signKey));
    }

    private OffsetDateTime getExpirationDateFromNow(Duration duration) {
        return OffsetDateTime.now().plus(duration);
    }

    @SuppressWarnings("UnstableApiUsage")
    private static Hasher getHasher(Key tokenSignKey) {
        return Hashing.hmacSha512(tokenSignKey).newHasher();
    }

    @SuppressWarnings("UnstableApiUsage")
    private static String signForToken(Key tokenSignKey, String payload) {
        Hasher hasher = getHasher(tokenSignKey);
        hasher.putString(payload, StandardCharsets.UTF_8);
        return toBase64String(hasher.hash());
    }

    @SuppressWarnings("UnstableApiUsage")
    private boolean validateSign(Key signKey,
                                 String payload,
                                 String sign) {
        if (signKey == null || StringUtils.isEmpty(sign)) {
            return false;
        }
        Hasher hasher = getHasher(signKey);
        hasher.putString(payload, StandardCharsets.UTF_8);
        return toBase64String(hasher.hash()).equals(sign);
    }

    private static String toBase64String(HashCode hashCode) {
        return BaseEncoding.base64().omitPadding().encode(hashCode.asBytes());
    }

    @Override
    @NonNull
    public final MetadataAuthorizationToken createToken(@NonNull TokenSubject subject,
                                                        @NonNull TokenSignKeyProvider tokenSignKeyProvider,
                                                        @NonNull String tokenId,
                                                        @NonNull TokenType tokenType,
                                                        @NonNull Duration expiryDuration,
                                                        @NonNull Collection<? extends AuthorizationScope> authorizedScopes,
                                                        @NonNull TokenFormat tokenFormat) {
        String issuer = configReader.get(SecurityConfigKeys.TOKEN_ISSUER);
        String jwtSubject = toSubject(subject);

        Key tokenSignKey = tokenSignKeyProvider.getSignKey(subject);
        List<String> scopes = authorizedScopes
                .stream()
                .map(AuthorizationScope::getScope)
                .toList();

        OffsetDateTime expirationDate = getExpirationDateFromNow(expiryDuration);
        JwtBuilder jwtBuilder = Jwts.builder()
                .subject(jwtSubject)
                .expiration(Date.from(expirationDate.toInstant()))
                .issuer(issuer)
                .claim(SIGN_FIELD, signForToken(tokenSignKey, toSignPayload(subject, tokenId)))
                .claim(SCOPES_FIELD, scopes)
                .claim(TOKEN_ID_FIELD, tokenId)
                .claim(TOKEN_TYPE_FIELD, tokenType.getValue());
        buildJwt(subject, tokenSignKeyProvider, tokenId, tokenType, expiryDuration, authorizedScopes, tokenFormat, jwtBuilder);
        String token = jwtBuilder.signWith(signKey).compact();
        return new SimpleMetadataAuthorizationToken(
                token, tokenType, subject, tokenId,
                List.copyOf(authorizedScopes), expirationDate,
                TokenFormat.BEARER
        );
    }

    protected void buildJwt(@NonNull TokenSubject subject,
                            @NonNull TokenSignKeyProvider tokenSignKeyProvider,
                            @NonNull String tokenId,
                            @NonNull TokenType tokenType,
                            @NonNull Duration expiryDuration,
                            @NonNull Collection<? extends AuthorizationScope> authorizedScopes,
                            @NonNull TokenFormat tokenFormat,
                            @NonNull JwtBuilder builder) {
        // Default implementation does nothing, subclasses can override to add custom claims
        // or other properties to the JWT.
    }

    @Override
    @NonNull
    public MetadataAuthorizationToken parseToken(@NonNull AuthorizationToken token,
                                                 @NonNull TokenSignKeyProvider tokenSignKeyProvider) {
        if (token instanceof MetadataAuthorizationToken metadataAuthorizationToken) {
            // Already parsed
            return metadataAuthorizationToken;
        }

        String rawToken = token.getToken();
        try {
            JwtParserBuilder parserBuilder = Jwts.parser();
            Jws<Claims> jws = verifyWith(parserBuilder, signKey)
                    .build()
                    .parseSignedClaims(rawToken);
            Claims claims = jws.getPayload();
            String subject = claims.getSubject();
            Pair<String, SubjectType> parsedSubject = parseSubject(subject);
            TokenSubject tokenSubject = tokenSubjectProvider.getTokenSubject(parsedSubject.getFirst(), parsedSubject.getSecond());
            Key tokenSignKey = tokenSignKeyProvider.getSignKey(tokenSubject);
            String sign = claims.get(SIGN_FIELD, String.class);
            String tokenId = claims.get(TOKEN_ID_FIELD, String.class);

            // Two steps validation, when a user changes the password,
            // the signature associated with the user will be changed,
            // so the previous token will be invalid.
            if (!validateSign(tokenSignKey, toSignPayload(tokenSubject, tokenId), sign)) {
                throw new AuthenticationException(AuthErrorCode.ERROR_INVALID_TOKEN, "invalid sign");
            }
            TokenType tokenType = TokenType.fromValue(claims.get(TOKEN_TYPE_FIELD, String.class));
            if (tokenType == null) {
                throw new AuthenticationException(AuthErrorCode.ERROR_INVALID_TOKEN, "invalid token type");
            }
            List<String> scopes = claims.get(SCOPES_FIELD, List.class);
            List<AuthorizationScope> authorizationScopes = authorizationScopeProvider.findScopes(scopes);
            OffsetDateTime expirationTime = OffsetDateTime.ofInstant(claims.getExpiration().toInstant(), ZoneOffset.UTC);
            return constructMetadataAuthorizationToken(token, tokenType, tokenSubject, tokenId, authorizationScopes, expirationTime, jws);
        } catch (ExpiredJwtException e) {
            throw new AuthenticationException(AuthErrorCode.ERROR_TOKEN_EXPIRED);
        } catch (SecurityException e) {
            logger.warn("Invalid jwt due to security exception: {}", e.getMessage());
            throw new AuthenticationException(AuthErrorCode.ERROR_INVALID_TOKEN);
        } catch (NumberFormatException e) {
            logger.debug("Invalid jwt token number format: {}", rawToken);
            throw new AuthenticationException(AuthErrorCode.ERROR_INVALID_TOKEN);
        } catch (CommonRuntimeException e) {
            throw e;
        } catch (Exception e) {
            logger.warn("Unexpected exception when parsing jwt token: {}", e.getMessage(), e);
            throw new AuthenticationException(AuthErrorCode.ERROR_INVALID_TOKEN);
        }
    }

    protected MetadataAuthorizationToken constructMetadataAuthorizationToken(
            @NonNull AuthorizationToken token,
            @NonNull TokenType tokenType,
            @NonNull TokenSubject subject,
            @NonNull String tokenId,
            @NonNull List<? extends AuthorizationScope> authorizedScopes,
            @NonNull OffsetDateTime expirationTime,
            @NonNull Jws<Claims> jws) {
        return new SimpleMetadataAuthorizationToken(token, subject, tokenId, authorizedScopes, expirationTime);
    }

    private String toSubject(TokenSubject tokenSubject) {
        return tokenSubject.getType().getValue() + ":" + tokenSubject.getId();
    }

    private String toSignPayload(TokenSubject tokenSubject, String tokenId) {
        return toSubject(tokenSubject) + ":" + tokenId;
    }

    private Pair<String, SubjectType> parseSubject(String subject) {
        String[] parts = subject.split(":");
        if (parts.length != 2) {
            throw new AuthenticationException(AuthErrorCode.ERROR_INVALID_TOKEN);
        }
        SubjectType type = SubjectType.fromValue(parts[0].trim());
        if (type == null) {
            throw new AuthenticationException(AuthErrorCode.ERROR_INVALID_TOKEN);
        }
        return new Pair<>(parts[1], type);
    }
}
