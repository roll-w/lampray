package tech.lamprism.lampray.system.database

import java.nio.charset.StandardCharsets
import java.nio.file.Files
import java.nio.file.Path
import java.security.KeyStore

object DatabaseSslSupport {
    fun materializePemFile(prefix: String, material: DatabaseSslMaterial): DatabaseSslFileArtifact {
        return when (material.source) {
            DatabaseSslMaterialSource.FILE -> DatabaseSslFileArtifact(Path.of(material.value))
            DatabaseSslMaterialSource.VALUE -> {
                val path = createTempFile(prefix, ".pem")
                try {
                    Files.writeString(path, material.value, StandardCharsets.UTF_8)
                } catch (e: Exception) {
                    cleanupTempPath(path, e)
                    throw e
                }
                DatabaseSslFileArtifact(path, listOf(TemporaryPathResource(path)))
            }
        }
    }

    fun materializeTrustStore(
        prefix: String,
        material: DatabaseSslMaterial,
        type: String = "PKCS12"
    ): DatabaseSslKeyStoreArtifact {
        val storePassword = newPassword()
        val certificates = readCertificates(material)
        val keyStore = KeyStore.getInstance(type)
        keyStore.load(null, storePassword.toCharArray())
        certificates.forEachIndexed { index, certificate ->
            keyStore.setCertificateEntry("$prefix-ca-$index", certificate)
        }
        return writeKeyStore(prefix, keyStore, storePassword, type)
    }

    fun materializeKeyStore(
        prefix: String,
        certificate: DatabaseSslMaterial,
        key: DatabaseSslMaterial,
        type: String = "PKCS12"
    ): DatabaseSslKeyStoreArtifact {
        val storePassword = newPassword()
        val certificates = readCertificates(certificate)
        val privateKey = readPrivateKey(key)
        val keyStore = KeyStore.getInstance(type)
        keyStore.load(null, storePassword.toCharArray())
        keyStore.setKeyEntry("$prefix-client", privateKey, storePassword.toCharArray(), certificates)
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
            cleanupTempPath(path, e)
            throw e
        }
        return DatabaseSslKeyStoreArtifact(path, password, type, listOf(TemporaryPathResource(path)))
    }
}
