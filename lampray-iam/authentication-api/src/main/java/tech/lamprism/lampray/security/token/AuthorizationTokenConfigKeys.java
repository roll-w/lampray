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

import org.springframework.stereotype.Component;
import space.lingu.NonNull;
import tech.lamprism.lampray.setting.AttributedSettingSpecification;
import tech.lamprism.lampray.setting.SettingKey;
import tech.lamprism.lampray.setting.SettingSource;
import tech.lamprism.lampray.setting.SettingSpecificationBuilder;
import tech.lamprism.lampray.setting.SettingSpecificationSupplier;

import java.util.List;

/**
 * @author RollW
 */
@Component
public class AuthorizationTokenConfigKeys implements SettingSpecificationSupplier {

    public static final AttributedSettingSpecification<String, String> TOKEN_ISSUER =
            new SettingSpecificationBuilder<>(SettingKey.ofString("token.authorization.issuer"))
                    .setDefaultValue("Lampray")
                    .setSupportedSources(SettingSource.VALUES)
                    .setTextDescription("Token issuer.")
                    .setRequired(true)
                    .build();

    public static final AttributedSettingSpecification<Long, Long> TOKEN_EXPIRE_TIME =
            new SettingSpecificationBuilder<>(SettingKey.ofLong("token.authorization.expire-time"))
                    .setDefaultValue(3600L)
                    .setTextDescription("Token expiration time in seconds.")
                    .setSupportedSources(SettingSource.VALUES)
                    .setRequired(true)
                    .build();

    public static final String RANDOM = "[random]";

    public static final String SIGN_KEY_KEYPAIR = "keypair";
    public static final String SIGN_KEY_SECRET = "secret";

    public static final AttributedSettingSpecification<String, String> TOKEN_KEY_TYPE =
            new SettingSpecificationBuilder<>(SettingKey.ofString("token.authorization.sign-key.type"))
                    .setDefault(0)
                    .setValueEntries(List.of(SIGN_KEY_KEYPAIR, SIGN_KEY_SECRET))
                    .setTextDescription("""
                            Sign key type used to sign token.
                            
                            - keypair: use public/private key pair to sign token.
                            - secret: use secret key to sign token.
                            
                            When using secret key, you need to set the secret key in the private key field.
                            """.trim())
                    .setSupportedSources(SettingSource.VALUES)
                    .setRequired(true)
                    .build();

    public static final AttributedSettingSpecification<String, String> TOKEN_SECRET_KEY =
            new SettingSpecificationBuilder<>(SettingKey.ofString("token.authorization.sign-key.secret-key"))
                    .setDefaultValue(RANDOM)
                    .setTextDescription("""
                            Secret key used to sign token. Used when sign key type is 'keypair' or 'secret'. \
                            If the sign key type is 'keypair', you should set the private key in this field, \
                            and the public key in the public key field.
                            
                            Set to '[random]' to generate a random key.
                            
                            To refer to a file, use the syntax 'file:<path>', \
                            where '<path>' is the path to the file containing the key.
                            For key value referencing, you can use the optional syntax 'key:<value>',\
                            where '<value>' is the key value to reference.
                            """.trim())
                    .setSupportedSources(SettingSource.VALUES)
                    .setRequired(true)
                    .build();

    private static final List<AttributedSettingSpecification<?, ?>> SPECIFICATIONS =
            List.of(TOKEN_ISSUER, TOKEN_EXPIRE_TIME, TOKEN_KEY_TYPE, TOKEN_SECRET_KEY);

    @Override
    @NonNull
    public List<AttributedSettingSpecification<?, ?>> getSpecifications() {
        return SPECIFICATIONS;
    }
}
