package tech.lamprism.lampray.storage.session;

import org.springframework.stereotype.Service;
import tech.lamprism.lampray.storage.StorageException;
import tech.lamprism.lampray.storage.persistence.StorageUploadSessionEntity;
import tech.rollw.common.web.AuthErrorCode;

/**
 * @author RollW
 */
@Service
public class UploadSessionAuthorizationValidator {
    public void ensureAuthorized(StorageUploadSessionEntity uploadSession,
                                 Long userId) {
        if (uploadSession.getOwnerUserId() != null && !uploadSession.getOwnerUserId().equals(userId)) {
            throw new StorageException(AuthErrorCode.ERROR_UNAUTHORIZED_USE,
                    "You are not allowed to use this upload session.");
        }
    }

    public void ensureQueryable(StorageUploadSessionEntity uploadSession,
                                Long userId) {
        if (userId == null || uploadSession.getOwnerUserId() == null || !uploadSession.getOwnerUserId().equals(userId)) {
            throw new StorageException(AuthErrorCode.ERROR_UNAUTHORIZED_USE,
                    "You are not allowed to query this upload session.");
        }
    }
}
