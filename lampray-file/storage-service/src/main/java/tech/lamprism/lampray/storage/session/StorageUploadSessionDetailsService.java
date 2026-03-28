package tech.lamprism.lampray.storage.session;

import org.springframework.stereotype.Service;
import tech.lamprism.lampray.storage.FileStorage;
import tech.lamprism.lampray.storage.StorageException;
import tech.lamprism.lampray.storage.StorageUploadSessionDetails;
import tech.lamprism.lampray.storage.StorageUploadSessionState;
import tech.lamprism.lampray.storage.persistence.StorageFileEntity;
import tech.lamprism.lampray.storage.persistence.StorageFileRepository;
import tech.lamprism.lampray.storage.persistence.StorageUploadSessionEntity;
import tech.rollw.common.web.DataErrorCode;

import java.time.OffsetDateTime;

@Service
public class StorageUploadSessionDetailsService {
    private final StorageUploadSessionLookupService storageUploadSessionLookupService;
    private final UploadSessionAuthorizationValidator uploadSessionAuthorizationValidator;
    private final StorageFileRepository storageFileRepository;

    public StorageUploadSessionDetailsService(StorageUploadSessionLookupService storageUploadSessionLookupService,
                                             UploadSessionAuthorizationValidator uploadSessionAuthorizationValidator,
                                             StorageFileRepository storageFileRepository) {
        this.storageUploadSessionLookupService = storageUploadSessionLookupService;
        this.uploadSessionAuthorizationValidator = uploadSessionAuthorizationValidator;
        this.storageFileRepository = storageFileRepository;
    }

    public StorageUploadSessionDetails getUploadSession(String uploadId,
                                                        Long userId) {
        StorageUploadSessionEntity uploadSession = storageUploadSessionLookupService.requireUploadSession(uploadId);
        uploadSessionAuthorizationValidator.ensureQueryable(uploadSession, userId);
        StorageUploadSessionState sessionState = StorageUploadSessionStates.resolveTrackedState(uploadSession, OffsetDateTime.now());
        FileStorage fileStorage = sessionState == StorageUploadSessionState.COMPLETED
                ? requireStoredFile(uploadSession.getFileId()).lock()
                : null;
        return new StorageUploadSessionDetails(
                uploadSession.getUploadId(),
                uploadSession.getUploadMode(),
                uploadSession.getFileName(),
                uploadSession.getGroupName(),
                uploadSession.getFileId(),
                sessionState,
                uploadSession.getExpiresAt(),
                uploadSession.getCreateTime(),
                uploadSession.getUpdateTime(),
                fileStorage
        );
    }

    private StorageFileEntity requireStoredFile(String fileId) {
        return storageFileRepository.findById(fileId)
                .orElseThrow(() -> new StorageException(
                        DataErrorCode.ERROR_DATA_NOT_EXIST,
                        "File not found: " + fileId
                ));
    }
}
