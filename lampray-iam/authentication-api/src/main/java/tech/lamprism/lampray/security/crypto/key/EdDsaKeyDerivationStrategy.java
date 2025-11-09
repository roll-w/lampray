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

import org.bouncycastle.crypto.params.AsymmetricKeyParameter;
import org.bouncycastle.crypto.params.Ed25519PrivateKeyParameters;
import org.bouncycastle.crypto.params.Ed448PrivateKeyParameters;
import org.bouncycastle.crypto.util.PrivateKeyFactory;
import org.bouncycastle.crypto.util.PublicKeyFactory;
import org.bouncycastle.crypto.util.SubjectPublicKeyInfoFactory;
import space.lingu.NonNull;

import java.security.PrivateKey;

/**
 * EdDSA (Ed25519/Ed448) key derivation strategy implementation.
 *
 * @author RollW
 */
public class EdDsaKeyDerivationStrategy extends AbstractKeyDerivationStrategy {
    @NonNull
    @Override
    protected AsymmetricKeyParameter derivePublicKeyParameter(@NonNull PrivateKey privateKey) {
        try {
            AsymmetricKeyParameter privateKeyParam = PrivateKeyFactory.createKey(privateKey.getEncoded());

            AsymmetricKeyParameter publicKeyParam;
            if (privateKeyParam instanceof Ed25519PrivateKeyParameters ed25519PrivateKey) {
                publicKeyParam = ed25519PrivateKey.generatePublicKey();
            } else if (privateKeyParam instanceof Ed448PrivateKeyParameters ed448PrivateKey) {
                publicKeyParam = ed448PrivateKey.generatePublicKey();
            } else {
                throw new KeyDerivationException("Invalid EdDSA private key format");
            }

            return PublicKeyFactory.createKey(
                    SubjectPublicKeyInfoFactory.createSubjectPublicKeyInfo(publicKeyParam)
            );
        } catch (Exception e) {
            throw new KeyDerivationException("Failed to derive EdDSA public key", e);
        }
    }

    @Override
    public boolean supportsAlgorithm(String algorithm) {
        return "Ed25519".equals(algorithm) || "Ed448".equals(algorithm);
    }
}
