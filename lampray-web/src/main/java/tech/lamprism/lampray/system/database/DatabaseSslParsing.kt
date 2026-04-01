package tech.lamprism.lampray.system.database

import org.bouncycastle.asn1.ASN1Sequence
import org.bouncycastle.asn1.DERNull
import org.bouncycastle.asn1.pkcs.PrivateKeyInfo
import org.bouncycastle.asn1.pkcs.PKCSObjectIdentifiers
import org.bouncycastle.asn1.x509.AlgorithmIdentifier
import org.bouncycastle.asn1.x9.X9ObjectIdentifiers
import org.bouncycastle.cert.X509CertificateHolder
import org.bouncycastle.cert.jcajce.JcaX509CertificateConverter
import org.bouncycastle.jce.provider.BouncyCastleProvider
import org.bouncycastle.openssl.PEMEncryptedKeyPair
import org.bouncycastle.openssl.PEMKeyPair
import org.bouncycastle.openssl.PEMParser
import org.bouncycastle.openssl.X509TrustedCertificateBlock
import org.bouncycastle.openssl.jcajce.JcaPEMKeyConverter
import org.bouncycastle.pkcs.PKCS8EncryptedPrivateKeyInfo
import java.io.StringReader
import java.nio.charset.StandardCharsets
import java.nio.file.Files
import java.nio.file.Path
import java.security.PrivateKey
import java.security.Security
import java.security.cert.X509Certificate
import java.util.Base64

private val bouncyCastleProviderName: String by lazy {
    if (Security.getProvider(BouncyCastleProvider.PROVIDER_NAME) == null) {
        Security.addProvider(BouncyCastleProvider())
    }
    BouncyCastleProvider.PROVIDER_NAME
}

internal fun readCertificates(material: DatabaseSslMaterial): Array<X509Certificate> {
    val bytes = readBytes(material)
    return if (looksLikePem(bytes)) {
        readCertificatesFromPem(String(bytes, StandardCharsets.UTF_8))
    } else {
        arrayOf(readCertificateFromDer(material, bytes))
    }
}

private fun readCertificatesFromPem(content: String): Array<X509Certificate> {
    val certificates = mutableListOf<X509Certificate>()
    val ignoredObjectTypes = mutableListOf<String>()
    val converter = certificateConverter()

    pemParser(content).use { parser ->
        while (true) {
            val parsedObject = parser.readObject() ?: break
            when (parsedObject) {
                is X509CertificateHolder -> certificates += converter.getCertificate(parsedObject)
                is X509TrustedCertificateBlock -> certificates += converter.getCertificate(parsedObject.certificateHolder)
                else -> ignoredObjectTypes += parsedObject.typeName()
            }
        }
    }

    require(certificates.isNotEmpty()) {
        buildString {
            append("No X.509 certificate could be loaded from database SSL material.")
            if (ignoredObjectTypes.isNotEmpty()) {
                append(" Encountered PEM objects: ")
                append(ignoredObjectTypes.joinToString(", "))
            }
        }
    }

    return certificates.toTypedArray()
}

internal fun readPrivateKey(material: DatabaseSslMaterial): PrivateKey {
    val bytes = readBytes(material)
    return if (looksLikePem(bytes)) {
        readPrivateKeyFromPem(String(bytes, StandardCharsets.UTF_8))
    } else {
        readPrivateKeyFromDer(material, bytes)
    }
}

private fun readPrivateKeyFromPem(content: String): PrivateKey {
    val ignoredObjectTypes = mutableListOf<String>()
    val converter = keyConverter()

    pemParser(content).use { parser ->
        while (true) {
            val parsedObject = parser.readObject() ?: break
            when (parsedObject) {
                is PEMKeyPair -> return converter.getKeyPair(parsedObject).getPrivate()
                is PrivateKeyInfo -> return converter.getPrivateKey(parsedObject)
                is PEMEncryptedKeyPair,
                is PKCS8EncryptedPrivateKeyInfo -> throw IllegalArgumentException(
                    "Encrypted private keys are not supported for managed database SSL material."
                )

                else -> ignoredObjectTypes += parsedObject.typeName()
            }
        }
    }

    throw IllegalArgumentException(
        buildString {
            append("Unsupported private key format for database SSL key material.")
            if (ignoredObjectTypes.isNotEmpty()) {
                append(" Encountered PEM objects: ")
                append(ignoredObjectTypes.joinToString(", "))
            }
        }
    )
}

private fun readCertificateFromDer(material: DatabaseSslMaterial, bytes: ByteArray): X509Certificate {
    return parseBinaryOrBase64Material(material, bytes) { derBytes ->
        certificateConverter().getCertificate(X509CertificateHolder(derBytes))
    }
}

private fun readPrivateKeyFromDer(material: DatabaseSslMaterial, bytes: ByteArray): PrivateKey {
    return parseBinaryOrBase64Material(material, bytes) { derBytes ->
        readPrivateKeyFromDerBytes(derBytes)
    }
}

private fun readPrivateKeyFromDerBytes(keyBytes: ByteArray): PrivateKey {
    return try {
        keyConverter().getPrivateKey(PrivateKeyInfo.getInstance(keyBytes))
    } catch (_: Exception) {
        val privateKeyInfo = createPrivateKeyInfoFromLegacyDer(keyBytes)
        keyConverter().getPrivateKey(privateKeyInfo)
    }
}

private fun createPrivateKeyInfoFromLegacyDer(keyBytes: ByteArray): PrivateKeyInfo {
    val sequence = ASN1Sequence.getInstance(keyBytes)

    runCatching {
        val rsaPrivateKey = org.bouncycastle.asn1.pkcs.RSAPrivateKey.getInstance(sequence)
        PrivateKeyInfo(
            AlgorithmIdentifier(PKCSObjectIdentifiers.rsaEncryption, DERNull.INSTANCE),
            rsaPrivateKey
        )
    }.getOrNull()?.let { return it }

    runCatching {
        val ecPrivateKey = org.bouncycastle.asn1.sec.ECPrivateKey.getInstance(sequence)
        val parameters = ecPrivateKey.parametersObject
            ?: throw IllegalArgumentException("EC private key is missing named curve parameters.")
        PrivateKeyInfo(
            AlgorithmIdentifier(X9ObjectIdentifiers.id_ecPublicKey, parameters),
            ecPrivateKey
        )
    }.getOrNull()?.let { return it }

    throw IllegalArgumentException("Unsupported private key format for database SSL key material.")
}

private fun pemParser(content: String): PEMParser = PEMParser(StringReader(content))

private fun keyConverter(): JcaPEMKeyConverter {
    return JcaPEMKeyConverter().setProvider(bouncyCastleProviderName)
}

private fun certificateConverter(): JcaX509CertificateConverter {
    return JcaX509CertificateConverter().setProvider(bouncyCastleProviderName)
}

private fun looksLikePem(bytes: ByteArray): Boolean {
    return String(bytes, StandardCharsets.UTF_8).contains("-----BEGIN")
}

private fun <T> parseBinaryOrBase64Material(
    material: DatabaseSslMaterial,
    bytes: ByteArray,
    parser: (ByteArray) -> T
): T {
    return try {
        parser(bytes)
    } catch (rawFailure: Exception) {
        val base64Bytes = decodeBase64Material(material, bytes) ?: throw rawFailure
        try {
            parser(base64Bytes)
        } catch (base64Failure: Exception) {
            base64Failure.addSuppressed(rawFailure)
            throw base64Failure
        }
    }
}

private fun decodeBase64Material(material: DatabaseSslMaterial, bytes: ByteArray): ByteArray? {
    return when (material.source) {
        DatabaseSslMaterialSource.VALUE -> decodeBase64Bytes(String(bytes, StandardCharsets.UTF_8).trim())
        DatabaseSslMaterialSource.FILE -> {
            val text = String(bytes, StandardCharsets.UTF_8).trim()
            if (looksLikeBase64Text(text)) decodeBase64Bytes(text) else null
        }
    }
}

private fun decodeBase64Bytes(text: String): ByteArray? {
    if (text.isEmpty()) {
        return null
    }
    return try {
        Base64.getMimeDecoder().decode(text)
    } catch (_: IllegalArgumentException) {
        null
    }
}

private fun looksLikeBase64Text(text: String): Boolean {
    if (text.isEmpty()) {
        return false
    }
    var hasData = false
    for (character in text) {
        if (character.isWhitespace()) {
            continue
        }
        val isBase64Char = character in 'A'..'Z' ||
            character in 'a'..'z' ||
            character in '0'..'9' ||
            character == '+' ||
            character == '/' ||
            character == '='
        if (!isBase64Char) {
            return false
        }
        hasData = true
    }
    return hasData
}

private fun Any.typeName(): String {
    return this::class.java.simpleName.ifBlank { this::class.java.name }
}

private fun readBytes(material: DatabaseSslMaterial): ByteArray {
    return when (material.source) {
        DatabaseSslMaterialSource.FILE -> Files.readAllBytes(Path.of(material.value))
        DatabaseSslMaterialSource.VALUE -> material.value.toByteArray(StandardCharsets.UTF_8)
    }
}
