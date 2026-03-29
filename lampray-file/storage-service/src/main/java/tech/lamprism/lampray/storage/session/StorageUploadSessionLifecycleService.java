package tech.lamprism.lampray.storage.session;

import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;
import tech.lamprism.lampray.storage.persistence.StorageUploadSessionEntity;
import tech.lamprism.lampray.storage.persistence.StorageUploadSessionRepository;
import tech.lamprism.lampray.storage.persistence.UploadSessionStatus;

import java.time.OffsetDateTime;

/**
 * @author RollW
 */
@Service
public class StorageUploadSessionLifecycleService {
    private final StorageUploadSessionRepository storageUploadSessionRepository;
    private final TransactionTemplate transactionTemplate;

    public StorageUploadSessionLifecycleService(StorageUploadSessionRepository storageUploadSessionRepository,
                                               PlatformTransactionManager transactionManager) {
        this.storageUploadSessionRepository = storageUploadSessionRepository;
        this.transactionTemplate = new TransactionTemplate(transactionManager);
    }

    public void expirePendingUploadSession(String uploadId) {
        transactionTemplate.executeWithoutResult(status -> storageUploadSessionRepository.findById(uploadId)
                .filter(uploadSession -> uploadSession.getStatus() == UploadSessionStatus.PENDING)
                .ifPresent(uploadSession -> expire(uploadSession, OffsetDateTime.now())));
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void expire(StorageUploadSessionEntity uploadSession,
                       OffsetDateTime now) {
        uploadSession.setStatus(UploadSessionStatus.EXPIRED);
        uploadSession.setUpdateTime(now);
        storageUploadSessionRepository.save(uploadSession);
    }

    public void markCompleted(StorageUploadSessionEntity uploadSession,
                              OffsetDateTime now) {
        uploadSession.setStatus(UploadSessionStatus.COMPLETED);
        uploadSession.setUpdateTime(now);
        storageUploadSessionRepository.save(uploadSession);
    }
}
