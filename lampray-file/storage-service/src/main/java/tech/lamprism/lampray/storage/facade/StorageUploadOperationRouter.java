package tech.lamprism.lampray.storage.facade;

import com.google.common.collect.Maps;
import org.springframework.stereotype.Service;
import tech.lamprism.lampray.storage.FileStorage;
import tech.lamprism.lampray.storage.StorageException;
import tech.lamprism.lampray.storage.StorageUploadMode;
import tech.lamprism.lampray.storage.persistence.StorageUploadSessionEntity;
import tech.rollw.common.web.CommonErrorCode;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.Map;

/**
 * @author RollW
 */
@Service
public class StorageUploadOperationRouter {
    private final Map<StorageUploadMode, StorageUploadStrategy> uploadStrategies;

    public StorageUploadOperationRouter(List<StorageUploadStrategy> uploadStrategies) {
        this.uploadStrategies = Maps.uniqueIndex(uploadStrategies, StorageUploadStrategy::mode);
    }

    public FileStorage uploadContent(StorageUploadSessionEntity uploadSession,
                                     InputStream inputStream) throws IOException {
        if (uploadSession.getUploadMode() != StorageUploadMode.PROXY) {
            throw new StorageException(CommonErrorCode.ERROR_ILLEGAL_ARGUMENT,
                    "Upload session requires completion after direct upload: " + uploadSession.getUploadId());
        }
        return requireStrategy(uploadSession.getUploadMode()).uploadContent(uploadSession, inputStream);
    }

    public FileStorage completeUpload(StorageUploadSessionEntity uploadSession) throws IOException {
        if (uploadSession.getUploadMode() != StorageUploadMode.DIRECT) {
            throw new StorageException(CommonErrorCode.ERROR_ILLEGAL_ARGUMENT,
                    "Upload session requires proxy upload: " + uploadSession.getUploadId());
        }
        return requireStrategy(uploadSession.getUploadMode()).completeUpload(uploadSession);
    }

    private StorageUploadStrategy requireStrategy(StorageUploadMode mode) {
        StorageUploadStrategy strategy = uploadStrategies.get(mode);
        if (strategy == null) {
            throw new StorageException(CommonErrorCode.ERROR_ILLEGAL_ARGUMENT,
                    "Unsupported upload mode: " + mode);
        }
        return strategy;
    }
}
