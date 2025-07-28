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

package tech.lamprism.lampray.web.configuration.database.ssl

import space.lingu.Experimental
import tech.lamprism.lampray.web.configuration.database.CertificateValue
import java.io.ByteArrayInputStream
import java.security.KeyFactory
import java.security.KeyStore
import java.security.PrivateKey
import java.security.cert.CertificateFactory
import java.security.cert.X509Certificate
import java.security.spec.PKCS8EncodedKeySpec
import java.util.Base64
import javax.net.ssl.KeyManager
import javax.net.ssl.KeyManagerFactory
import javax.net.ssl.SSLContext
import javax.net.ssl.TrustManager
import javax.net.ssl.TrustManagerFactory
import javax.net.ssl.X509TrustManager

/**
 * Utility class for handling SSL/TLS certificates in database connections.
 * Provides methods to load certificates from various sources and configure SSL contexts.
 *
 * @author RollW
 */
@Experimental(info = "Internal use only, may change in future versions.")
object SslCertificateUtils {

    /**
     * Creates an SSL context from the provided certificate configuration.
     *
     * @param clientCert Client certificate (optional)
     * @param clientKey Client private key (optional)
     * @param caCert CA certificate for server verification (optional)
     * @param allowSelfSigned Whether to allow self-signed certificates
     * @return Configured SSL context
     */
    fun createSslContext(
        clientCert: CertificateValue? = null,
        clientKey: CertificateValue? = null,
        caCert: CertificateValue? = null,
        allowSelfSigned: Boolean = false
    ): SSLContext {
        val sslContext = SSLContext.getInstance("TLS")

        // Create key manager if client certificate is provided
        val keyManagers = if (clientCert != null && clientKey != null) {
            createKeyManagers(clientCert, clientKey)
        } else null

        // Create trust manager if CA certificate is provided
        val trustManagers = if (caCert != null || allowSelfSigned) {
            createTrustManagers(caCert, allowSelfSigned)
        } else null

        sslContext.init(keyManagers, trustManagers, null)
        return sslContext
    }

    /**
     * Creates key managers from client certificate and private key.
     */
    private fun createKeyManagers(
        clientCert: CertificateValue,
        clientKey: CertificateValue
    ): Array<KeyManager> {
        val keyStore = KeyStore.getInstance(KeyStore.getDefaultType())
        keyStore.load(null, null)

        // Load client certificate
        val certificate = loadCertificate(clientCert)

        // Load private key
        val privateKey = loadPrivateKey(clientKey)

        // Add certificate and key to keystore
        keyStore.setKeyEntry("client", privateKey, "".toCharArray(), arrayOf(certificate))

        val keyManagerFactory = KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm())
        keyManagerFactory.init(keyStore, "".toCharArray())

        return keyManagerFactory.keyManagers
    }

    /**
     * Creates trust managers from CA certificate or allows all if self-signed is enabled.
     */
    private fun createTrustManagers(
        caCert: CertificateValue?,
        allowSelfSigned: Boolean
    ): Array<TrustManager> {
        return if (allowSelfSigned) {
            // Create trust manager that accepts all certificates
            arrayOf(object : X509TrustManager {
                override fun checkClientTrusted(chain: Array<X509Certificate>, authType: String) {}
                override fun checkServerTrusted(chain: Array<X509Certificate>, authType: String) {}
                override fun getAcceptedIssuers(): Array<X509Certificate> = arrayOf()
            })
        } else if (caCert != null) {
            // Create trust manager with specific CA certificate
            val trustStore = KeyStore.getInstance(KeyStore.getDefaultType())
            trustStore.load(null, null)

            val certificate = loadCertificate(caCert)
            trustStore.setCertificateEntry("ca", certificate)

            val trustManagerFactory = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm())
            trustManagerFactory.init(trustStore)

            trustManagerFactory.trustManagers
        } else {
            // Use default trust managers
            val trustManagerFactory = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm())
            trustManagerFactory.init(null as KeyStore?)
            trustManagerFactory.trustManagers
        }
    }

    /**
     * Loads a certificate from CertificateValue.
     */
    private fun loadCertificate(certValue: CertificateValue): X509Certificate {
        val certContent = certValue.getContent()
        val cleanedContent = cleanPemContent(certContent, "CERTIFICATE")

        val certBytes = Base64.getDecoder().decode(cleanedContent)
        val certFactory = CertificateFactory.getInstance("X.509")

        return certFactory.generateCertificate(ByteArrayInputStream(certBytes)) as X509Certificate
    }

    /**
     * Loads a private key from CertificateValue.
     */
    private fun loadPrivateKey(keyValue: CertificateValue): PrivateKey {
        val keyContent = keyValue.getContent()
        val cleanedContent = cleanPemContent(keyContent, "PRIVATE KEY")

        val keyBytes = Base64.getDecoder().decode(cleanedContent)
        val keySpec = PKCS8EncodedKeySpec(keyBytes)
        val keyFactory = KeyFactory.getInstance("RSA")

        return try {
            keyFactory.generatePrivate(keySpec)
        } catch (_: Exception) {
            // Try with EC algorithm if RSA fails
            val ecKeyFactory = KeyFactory.getInstance("EC")
            ecKeyFactory.generatePrivate(keySpec)
        }
    }

    /**
     * Cleans PEM content by removing headers, footers, and whitespace.
     */
    private fun cleanPemContent(pemContent: String, type: String): String {
        return pemContent
            .replace("-----BEGIN $type-----", "")
            .replace("-----END $type-----", "")
            .replace("-----BEGIN CERTIFICATE-----", "")
            .replace("-----END CERTIFICATE-----", "")
            .replace("-----BEGIN PRIVATE KEY-----", "")
            .replace("-----END PRIVATE KEY-----", "")
            .replace("-----BEGIN RSA PRIVATE KEY-----", "")
            .replace("-----END RSA PRIVATE KEY-----", "")
            .replace("-----BEGIN EC PRIVATE KEY-----", "")
            .replace("-----END EC PRIVATE KEY-----", "")
            .replace("\n", "")
            .replace("\r", "")
            .replace(" ", "")
            .trim()
    }
}
