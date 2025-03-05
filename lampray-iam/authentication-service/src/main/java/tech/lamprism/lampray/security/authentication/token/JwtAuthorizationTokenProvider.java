/*
 * Copyright (C) 2023 RollW
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

package tech.lamprism.lampray.security.authentication.token;

import com.google.common.hash.Hasher;
import com.google.common.hash.Hashing;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import io.jsonwebtoken.security.SecurityException;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import space.lingu.NonNull;
import tech.lamprism.lampray.authentication.SecurityConfigKeys;
import tech.lamprism.lampray.security.authorization.AuthorizationScope;
import tech.lamprism.lampray.security.token.AuthorizationToken;
import tech.lamprism.lampray.security.token.AuthorizationTokenProvider;
import tech.lamprism.lampray.security.token.BearerAuthorizationToken;
import tech.lamprism.lampray.setting.ConfigReader;
import tech.lamprism.lampray.user.UserIdentity;
import tech.lamprism.lampray.user.UserProvider;
import tech.lamprism.lampray.user.UserSignatureProvider;
import tech.rollw.common.web.AuthErrorCode;
import tech.rollw.common.web.CommonRuntimeException;
import tech.rollw.common.web.system.AuthenticationException;

import java.nio.charset.StandardCharsets;
import java.security.Key;
import java.time.Duration;
import java.util.Collection;
import java.util.Date;

/**
 * @author RollW
 */
@Service
public class JwtAuthorizationTokenProvider implements AuthorizationTokenProvider {
    private static final String TOKEN_HEAD = "Bearer";
    private static final String SIGN_FIELD = "sign";
    private static final Logger logger = LoggerFactory.getLogger(JwtAuthorizationTokenProvider.class);

    private final ConfigReader configReader;
    private final UserProvider userProvider;

    private final Key signKey;

    // TODO: support keypair for sign
    // private final KeyPair signKeyPair = Keys.keyPairFor(SignatureAlgorithm.PS512);

    public JwtAuthorizationTokenProvider(ConfigReader configReader,
                                         UserProvider userProvider) {
        this.configReader = configReader;
        this.userProvider = userProvider;
        this.signKey = createSignKey(configReader);
    }

    private Key createSignKey(ConfigReader configReader) {
        String signKey = configReader.get(SecurityConfigKeys.TOKEN_SIGN_KEY);
        if (StringUtils.equalsIgnoreCase("[random]", signKey)) {
            return Keys.secretKeyFor(SignatureAlgorithm.HS512);
        }
        return Keys.hmacShaKeyFor(signKey.getBytes(StandardCharsets.UTF_8));
    }

    private Date getExpirationDateFromNow(Duration duration) {
        return new Date(System.currentTimeMillis() + duration.toMillis());
    }

    @Override
    @NonNull
    public AuthorizationToken createToken(@NonNull UserIdentity user,
                                          @NonNull UserSignatureProvider signatureProvider,
                                          @NonNull Duration expiryDuration,
                                          @NonNull Collection<? extends AuthorizationScope> authorizedScopes) {
        String issuer = configReader.get(SecurityConfigKeys.TOKEN_ISSUER);
        String subject = String.valueOf(user.getUserId());
        String signature = signatureProvider.getSignature(user.getUserId());
        String rawToken = Jwts.builder()
                .setSubject(subject)
                .setExpiration(getExpirationDateFromNow(expiryDuration))
                .claim(SIGN_FIELD, signForToken(signature, subject))
                .setIssuer(issuer)
                .signWith(signKey)
                .compact();
        // TODO: add authorized scopes
        return new BearerAuthorizationToken(rawToken);
    }

    @SuppressWarnings("UnstableApiUsage")
    private Hasher getHasher(String signature) {
        return Hashing.hmacSha512(signature.getBytes(StandardCharsets.UTF_8))
                .newHasher();
    }

    @SuppressWarnings("UnstableApiUsage")
    private String signForToken(String signature, String subject) {
        Hasher hasher = getHasher(signature);
        hasher.putString(subject, StandardCharsets.UTF_8);
        return hasher.hash().toString();
    }

    @SuppressWarnings("UnstableApiUsage")
    private boolean validateSign(String signature,
                                 String subject,
                                 String sign) {
        if (StringUtils.isEmpty(signature) || StringUtils.isEmpty(sign)) {
            return false;
        }
        Hasher hasher = getHasher(signature);
        hasher.putString(subject, StandardCharsets.UTF_8);
        return hasher.hash().toString().equals(sign);
    }

    @Override
    @NonNull
    public UserIdentity parseToken(@NonNull AuthorizationToken token,
                                   @NonNull UserSignatureProvider signatureProvider) {
        String rawToken = token.getToken();
        try {
            Claims claims = Jwts.parserBuilder()
                    .setSigningKey(signKey)
                    .build()
                    .parseClaimsJws(rawToken)
                    .getBody();
            long userId = Long.parseLong(claims.getSubject());
            String userSignature = signatureProvider.getSignature(userId);
            String sign = claims.get(SIGN_FIELD, String.class);

            // Two steps validation, when a user changes the password,
            // the signature associated with the user will be changed,
            // so the previous token will be invalid.
            if (!validateSign(userSignature, claims.getSubject(), sign)) {
                throw new AuthenticationException(AuthErrorCode.ERROR_INVALID_TOKEN);
            }
            return userProvider.getUser(userId);
        } catch (ExpiredJwtException e) {
            throw new AuthenticationException(AuthErrorCode.ERROR_TOKEN_EXPIRED);
        } catch (SecurityException e) {
            logger.warn("Invalid jwt due to security exception: {}", e.getMessage());
            throw new AuthenticationException(AuthErrorCode.ERROR_INVALID_TOKEN);
        } catch (NumberFormatException e) {
            logger.debug("Invalid jwt token number format: {}", rawToken);
            throw new AuthenticationException(AuthErrorCode.ERROR_INVALID_TOKEN);
        } catch (Exception e) {
            if (e instanceof CommonRuntimeException cre) {
                throw cre;
            }
            logger.warn("Unexpected exception when parsing jwt token: {}", e.getMessage(), e);
            throw new AuthenticationException(AuthErrorCode.ERROR_INVALID_TOKEN);
        }
    }

    @Override
    public boolean supports(@NonNull String tokenType) {
        return TOKEN_HEAD.equalsIgnoreCase(tokenType);
    }
}
