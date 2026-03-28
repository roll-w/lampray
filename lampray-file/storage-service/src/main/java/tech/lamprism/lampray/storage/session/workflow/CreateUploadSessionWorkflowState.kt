package tech.lamprism.lampray.storage.session.workflow

import tech.lamprism.lampray.storage.FileType
import tech.lamprism.lampray.storage.StorageAccessRequest
import tech.lamprism.lampray.storage.StorageUploadMode
import tech.lamprism.lampray.storage.StorageUploadSession
import tech.lamprism.lampray.storage.configuration.StorageGroupConfig
import tech.lamprism.lampray.storage.persistence.StorageUploadSessionEntity
import tech.lamprism.lampray.storage.routing.StorageWritePlan
import tech.lamprism.lampray.storage.store.BlobStore
import java.time.OffsetDateTime

data class CreateUploadSessionWorkflowState(
    var groupName: String? = null,
    var writePlan: StorageWritePlan? = null,
    var groupSettings: StorageGroupConfig? = null,
    var fileName: String? = null,
    var mimeType: String? = null,
    var fileType: FileType? = null,
    var checksum: String? = null,
    var uploadId: String? = null,
    var fileId: String? = null,
    var primaryBackend: String? = null,
    var primaryBlobStore: BlobStore? = null,
    var uploadMode: StorageUploadMode? = null,
    var now: OffsetDateTime? = null,
    var expiresAt: OffsetDateTime? = null,
    var directRequest: StorageAccessRequest? = null,
    var objectKey: String? = null,
    var uploadSessionEntity: StorageUploadSessionEntity? = null,
    var result: StorageUploadSession? = null,
)
