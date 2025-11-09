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

package tech.lamprism.lampray.security.crypto.key;

import org.bouncycastle.asn1.x509.SubjectPublicKeyInfo;
import org.bouncycastle.crypto.params.AsymmetricKeyParameter;
import org.bouncycastle.crypto.util.SubjectPublicKeyInfoFactory;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.bouncycastle.openssl.jcajce.JcaPEMKeyConverter;
import space.lingu.NonNull;

import java.security.PrivateKey;
import java.security.PublicKey;

/**
 * @author RollW
 */
public abstract class AbstractKeyDerivationStrategy implements KeyDerivationStrategy {
    private static PublicKey convertToJcaPublicKey(AsymmetricKeyParameter publicKeyParameter) {
        try {
            SubjectPublicKeyInfo subjectPublicKeyInfo =
                    SubjectPublicKeyInfoFactory.createSubjectPublicKeyInfo(publicKeyParameter);
            return new JcaPEMKeyConverter()
                    .setProvider(BouncyCastleProvider.PROVIDER_NAME)
                    .getPublicKey(subjectPublicKeyInfo);
        } catch (Exception e) {
            throw new KeyDerivationException("Failed to convert to JCA public key", e);
        }
    }

    @Override
    @NonNull
    public final PublicKey derivePublicKey(@NonNull PrivateKey privateKey) {
        return convertToJcaPublicKey(derivePublicKeyParameter(privateKey));
    }

    @NonNull
    protected abstract AsymmetricKeyParameter derivePublicKeyParameter(@NonNull PrivateKey privateKey);
}
