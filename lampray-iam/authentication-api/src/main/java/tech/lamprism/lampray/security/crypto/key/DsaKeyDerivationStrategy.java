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
import org.bouncycastle.crypto.params.DSAPrivateKeyParameters;
import org.bouncycastle.crypto.params.DSAPublicKeyParameters;
import org.bouncycastle.crypto.util.PrivateKeyFactory;
import org.bouncycastle.crypto.util.PublicKeyFactory;
import org.bouncycastle.crypto.util.SubjectPublicKeyInfoFactory;
import space.lingu.NonNull;

import java.math.BigInteger;
import java.security.PrivateKey;

/**
 * DSA key derivation strategy implementation.
 *
 * @author RollW
 */
public class DsaKeyDerivationStrategy extends AbstractKeyDerivationStrategy {

    @NonNull
    @Override
    protected AsymmetricKeyParameter derivePublicKeyParameter(@NonNull PrivateKey privateKey) {
        try {
            AsymmetricKeyParameter privateKeyParam = PrivateKeyFactory.createKey(privateKey.getEncoded());
            if (!(privateKeyParam instanceof DSAPrivateKeyParameters dsaPrivateKey)) {
                throw new KeyDerivationException("Invalid DSA private key format");
            }

            // Calculate public key: y = g^x mod p
            BigInteger publicKeyValue = dsaPrivateKey.getParameters().getG()
                    .modPow(dsaPrivateKey.getX(), dsaPrivateKey.getParameters().getP());

            DSAPublicKeyParameters dsaPublicKey = new DSAPublicKeyParameters(
                    publicKeyValue,
                    dsaPrivateKey.getParameters()
            );

            return PublicKeyFactory.createKey(
                    SubjectPublicKeyInfoFactory.createSubjectPublicKeyInfo(dsaPublicKey)
            );
        } catch (Exception e) {
            throw new KeyDerivationException("Failed to derive DSA public key", e);
        }
    }

    @Override
    public boolean supportsAlgorithm(String algorithm) {
        return "DSA".equals(algorithm);
    }
}
