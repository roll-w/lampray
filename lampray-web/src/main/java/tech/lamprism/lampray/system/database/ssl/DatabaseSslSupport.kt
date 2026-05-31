/*
 * Copyright (C) 2023-2026 RollW
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

package tech.lamprism.lampray.system.database.ssl

import org.apache.commons.lang3.RandomStringUtils
import org.slf4j.LoggerFactory
import java.io.IOException
import java.nio.charset.StandardCharsets
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.attribute.PosixFilePermission
import java.security.KeyStore

/**
 * Provides methods to materialize SSL certificate material into files and keystores.
 *
 * Supports both file-based and inline PEM/DER material, creating temporary files
 * as needed with secure permissions.
 *
 * @author RollW
 */
object DatabaseSslSupport {

    private val logger = LoggerFactory.getLogger(DatabaseSslSupport::class.java)

    /**
     * Materializes PEM content to a file. If the material source is a file,
     * returns a reference to that file. If inline content, writes to a temp file.
     *
     * @param prefix the filename prefix for temp files
     * @param material the SSL material to materialize
     * @return a file artifact with the path and cleanup resources
     */
    fun materializePemFile(prefix: String, material: DatabaseSslMaterial): DatabaseSslFileArtifact {
        return when (material.source) {
            DatabaseSslMaterial.Source.FILE -> {
                DatabaseSslFileArtifact(Path.of(material.value))
            }

            DatabaseSslMaterial.Source.VALUE -> {
                val path = createTempFile(prefix, ".pem")
                try {
                    Files.writeString(path, material.value, StandardCharsets.UTF_8)
                } catch (e: Exception) {
                    Files.deleteIfExists(path)
                    throw e
                }
                DatabaseSslFileArtifact(path, createCleanupResource(path))
            }
        }
    }

    /**
     * Materializes CA certificates into a PKCS12 trust store.
     *
     * @param prefix the filename prefix for the trust store file
     * @param material the CA certificate material
     * @param type the keystore type (default: PKCS12)
     * @return a keystore artifact with the path, password, and cleanup resources
     */
    fun materializeTrustStore(
        prefix: String,
        material: DatabaseSslMaterial,
        type: String = DEFAULT_KEYSTORE_TYPE
    ): DatabaseSslKeyStoreArtifact {
        val storePassword = generatePassword()
        val certificates = DatabaseSslParsing.readCertificates(material)
        val keyStore = KeyStore.getInstance(type)
        keyStore.load(null, storePassword.toCharArray())

        certificates.forEachIndexed { index, certificate ->
            keyStore.setCertificateEntry("$prefix-ca-$index", certificate)
        }

        return writeKeyStore(prefix, keyStore, storePassword, type)
    }

    /**
     * Materializes client certificate and private key into a PKCS12 key store.
     *
     * @param prefix the filename prefix for the key store file
     * @param certificate the client certificate material
     * @param key the private key material
     * @param type the keystore type (default: PKCS12)
     * @return a keystore artifact with the path, password, and cleanup resources
     */
    fun materializeKeyStore(
        prefix: String,
        certificate: DatabaseSslMaterial,
        key: DatabaseSslMaterial,
        type: String = DEFAULT_KEYSTORE_TYPE
    ): DatabaseSslKeyStoreArtifact {
        val storePassword = generatePassword()
        val certificates = DatabaseSslParsing.readCertificates(certificate)
        val privateKey = DatabaseSslParsing.readPrivateKey(key)
        val keyStore = KeyStore.getInstance(type)
        keyStore.load(null, storePassword.toCharArray())
        keyStore.setKeyEntry(
            "$prefix-client",
            privateKey,
            storePassword.toCharArray(),
            certificates
        )

        return writeKeyStore(prefix, keyStore, storePassword, type)
    }

    private fun writeKeyStore(
        prefix: String,
        keyStore: KeyStore,
        password: String,
        type: String
    ): DatabaseSslKeyStoreArtifact {
        val extension = if (type.equals("JKS", ignoreCase = true)) ".jks" else ".p12"
        val path = createTempFile(prefix, extension)
        try {
            Files.newOutputStream(path).use { outputStream ->
                keyStore.store(outputStream, password.toCharArray())
            }
        } catch (e: Exception) {
            Files.deleteIfExists(path)
            throw e
        }
        return DatabaseSslKeyStoreArtifact(
            path,
            password,
            type,
            createCleanupResource(path)
        )
    }

    private fun createTempFile(prefix: String, suffix: String): Path {
        val path = Files.createTempFile(TEMP_FILE_PREFIX + prefix, suffix)
        setOwnerOnlyPermissions(path)
        return path
    }

    private fun setOwnerOnlyPermissions(path: Path) {
        try {
            val permissions = setOf(
                PosixFilePermission.OWNER_READ,
                PosixFilePermission.OWNER_WRITE
            )
            Files.setPosixFilePermissions(path, permissions)
        } catch (_: UnsupportedOperationException) {
            logger.debug("POSIX permissions not supported, falling back to File API for: {}", path)
            trySetOwnerOnlyPermissionsWithFileApi(path)
        } catch (e: IOException) {
            logger.warn("Failed to restrict file permissions due to IO error: {}", path, e)
        } catch (e: SecurityException) {
            logger.warn("Failed to restrict file permissions due to security policy: {}", path, e)
        }
    }

    private fun trySetOwnerOnlyPermissionsWithFileApi(path: Path) {
        try {
            val file = path.toFile()
            file.setReadable(false, false)
            file.setWritable(false, false)
            file.setExecutable(false, false)
            val ownerRead = file.setReadable(true, true)
            val ownerWrite = file.setWritable(true, true)
            if (!ownerRead || !ownerWrite) {
                logger.warn(
                    "Failed to restrict database SSL temporary file permissions with the file API: {}",
                    path
                )
            }
        } catch (e: SecurityException) {
            logger.warn("Failed to restrict file permissions with File API: {}", path, e)
        }
    }

    private fun generatePassword(): String {
        return RandomStringUtils.secure().nextAlphanumeric(PASSWORD_LENGTH)
    }

    private fun createCleanupResource(path: Path): List<AutoCloseable> {
        return listOf(AutoCloseable { Files.deleteIfExists(path) })
    }

    private const val TEMP_FILE_PREFIX = "lampray-db-"
    private const val DEFAULT_KEYSTORE_TYPE = "PKCS12"
    private const val PASSWORD_LENGTH = 32
}
