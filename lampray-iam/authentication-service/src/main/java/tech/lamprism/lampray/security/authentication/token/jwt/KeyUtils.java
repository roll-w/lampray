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

import org.bouncycastle.asn1.x509.SubjectPublicKeyInfo;
import org.bouncycastle.crypto.params.AsymmetricKeyParameter;
import org.bouncycastle.crypto.params.DSAPrivateKeyParameters;
import org.bouncycastle.crypto.params.DSAPublicKeyParameters;
import org.bouncycastle.crypto.params.ECPrivateKeyParameters;
import org.bouncycastle.crypto.params.ECPublicKeyParameters;
import org.bouncycastle.crypto.params.Ed25519PrivateKeyParameters;
import org.bouncycastle.crypto.params.Ed448PrivateKeyParameters;
import org.bouncycastle.crypto.params.RSAKeyParameters;
import org.bouncycastle.crypto.params.RSAPrivateCrtKeyParameters;
import org.bouncycastle.crypto.util.PrivateKeyFactory;
import org.bouncycastle.crypto.util.PublicKeyFactory;
import org.bouncycastle.crypto.util.SubjectPublicKeyInfoFactory;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.bouncycastle.math.ec.ECPoint;
import org.bouncycastle.openssl.jcajce.JcaPEMKeyConverter;

import java.math.BigInteger;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.Security;

/**
 * @author RollW
 */
public class KeyUtils {
    // TODO: improve code

    static {
        Security.addProvider(new BouncyCastleProvider());
    }

    public static PublicKey derivePublicKey(PrivateKey privateKey) throws Exception {
        String alg = privateKey.getAlgorithm();

        return switch (alg) {
            case "RSA" -> deriveRsaPublicKey(privateKey);
            case "DSA" -> deriveDsaPublicKey(privateKey);
            case "EC" -> deriveEcPublicKey(privateKey);
            case "Ed25519", "Ed448" -> deriveEdKey(privateKey);
            default -> throw new UnsupportedOperationException("Unsupported algorithm: " + alg);
        };
    }

    private static PublicKey deriveRsaPublicKey(PrivateKey privateKey) throws Exception {
        AsymmetricKeyParameter privateKeyParam = PrivateKeyFactory.createKey(privateKey.getEncoded());
        if (!(privateKeyParam instanceof RSAKeyParameters)) {
            throw new IllegalArgumentException("Not RSA private key");
        }

        RSAPrivateCrtKeyParameters priv = (RSAPrivateCrtKeyParameters) privateKeyParam;
        RSAKeyParameters pub = new RSAKeyParameters(false, priv.getModulus(), priv.getPublicExponent());

        return toJcaPublicKey(PublicKeyFactory.createKey(SubjectPublicKeyInfoFactory.createSubjectPublicKeyInfo(pub)));
    }

    private static PublicKey deriveDsaPublicKey(PrivateKey privateKey) throws Exception {
        AsymmetricKeyParameter privateKeyParam = PrivateKeyFactory.createKey(privateKey.getEncoded());
        if (!(privateKeyParam instanceof DSAPrivateKeyParameters priv)) {
            throw new IllegalArgumentException("Not DSA private key");
        }

        // y = g^x mod p
        BigInteger y = priv.getParameters().getG().modPow(priv.getX(), priv.getParameters().getP());

        DSAPublicKeyParameters pub = new DSAPublicKeyParameters(y, priv.getParameters());
        return toJcaPublicKey(PublicKeyFactory.createKey(SubjectPublicKeyInfoFactory.createSubjectPublicKeyInfo(pub)));
    }

    private static PublicKey deriveEcPublicKey(PrivateKey privateKey) throws Exception {
        AsymmetricKeyParameter privateKeyParam = PrivateKeyFactory.createKey(privateKey.getEncoded());
        if (!(privateKeyParam instanceof ECPrivateKeyParameters priv)) {
            throw new IllegalArgumentException("Not EC private key");
        }

        ECPoint q = priv.getParameters().getG().multiply(priv.getD());

        ECPublicKeyParameters pub = new ECPublicKeyParameters(q, priv.getParameters());
        return toJcaPublicKey(PublicKeyFactory.createKey(SubjectPublicKeyInfoFactory.createSubjectPublicKeyInfo(pub)));
    }

    private static PublicKey deriveEdKey(PrivateKey privateKey) throws Exception {
        AsymmetricKeyParameter privateKeyParam = PrivateKeyFactory.createKey(privateKey.getEncoded());
        if (!(privateKeyParam instanceof Ed25519PrivateKeyParameters) &&
                !(privateKeyParam instanceof Ed448PrivateKeyParameters)) {
            throw new IllegalArgumentException("Not EdDSA private key");
        }

        AsymmetricKeyParameter pub;
        if (privateKeyParam instanceof Ed25519PrivateKeyParameters) {
            pub = ((Ed25519PrivateKeyParameters) privateKeyParam).generatePublicKey();
        } else {
            pub = ((Ed448PrivateKeyParameters) privateKeyParam).generatePublicKey();
        }

        return toJcaPublicKey(PublicKeyFactory.createKey(SubjectPublicKeyInfoFactory.createSubjectPublicKeyInfo(pub)));
    }

    private static PublicKey toJcaPublicKey(AsymmetricKeyParameter pubParams) throws Exception {
        SubjectPublicKeyInfo spki = SubjectPublicKeyInfoFactory.createSubjectPublicKeyInfo(pubParams);
        return new JcaPEMKeyConverter()
                .setProvider(BouncyCastleProvider.PROVIDER_NAME)
                .getPublicKey(spki);
    }

}
