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

package tech.lamprism.lampray.security.crypto;

import org.bouncycastle.jce.provider.BouncyCastleProvider;
import tech.lamprism.lampray.security.crypto.key.DsaKeyDerivationStrategy;
import tech.lamprism.lampray.security.crypto.key.EcKeyDerivationStrategy;
import tech.lamprism.lampray.security.crypto.key.EdDsaKeyDerivationStrategy;
import tech.lamprism.lampray.security.crypto.key.KeyDerivationException;
import tech.lamprism.lampray.security.crypto.key.KeyDerivationStrategy;
import tech.lamprism.lampray.security.crypto.key.RsaKeyDerivationStrategy;

import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.Security;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Utility class for deriving public keys from private keys using various cryptographic algorithms.
 *
 * <p>This class uses the Strategy pattern to handle different key derivation algorithms
 * in a clean and extensible way. Supported algorithms include RSA, DSA, EC, Ed25519, and Ed448.</p>
 *
 * @author RollW
 */
public class KeyUtils {

    private static final Map<String, KeyDerivationStrategy> DERIVATION_STRATEGIES;

    static {
        Security.addProvider(new BouncyCastleProvider());

        // Initialize strategy registry using a cleaner approach
        List<KeyDerivationStrategy> strategies = List.of(
                new RsaKeyDerivationStrategy(),
                new DsaKeyDerivationStrategy(),
                new EcKeyDerivationStrategy(),
                new EdDsaKeyDerivationStrategy()
        );

        DERIVATION_STRATEGIES = strategies.stream()
                .flatMap(strategy -> getSupportedAlgorithms(strategy).stream()
                        .map(algorithm -> Map.entry(algorithm, strategy)))
                .collect(Collectors.toMap(
                        Map.Entry::getKey,
                        Map.Entry::getValue
                ));
    }

    /**
     * Derives a public key from the given private key.
     *
     * @param privateKey the private key to derive the public key from
     * @return the derived public key
     * @throws KeyDerivationException if the algorithm is unsupported or derivation fails
     */
    public static PublicKey derivePublicKey(PrivateKey privateKey) {
        if (privateKey == null) {
            throw new KeyDerivationException("Private key cannot be null");
        }

        String algorithm = privateKey.getAlgorithm();
        KeyDerivationStrategy strategy = DERIVATION_STRATEGIES.get(algorithm);

        if (strategy == null) {
            throw new KeyDerivationException("Unsupported algorithm: " + algorithm);
        }

        return strategy.derivePublicKey(privateKey);
    }

    /**
     * Gets all algorithms supported by a given strategy.
     */
    private static List<String> getSupportedAlgorithms(KeyDerivationStrategy strategy) {
        // Common algorithm names that strategies might support
        List<String> possibleAlgorithms = List.of("RSA", "DSA", "EC", "Ed25519", "Ed448");

        return possibleAlgorithms.stream()
                .filter(strategy::supportsAlgorithm)
                .toList();
    }

    /**
     * Private constructor to prevent instantiation of utility class.
     */
    private KeyUtils() {
        throw new UnsupportedOperationException("Utility class cannot be instantiated");
    }
}
