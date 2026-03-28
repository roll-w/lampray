package tech.lamprism.lampray.storage.persistence.file;

import org.springframework.stereotype.Service;
import tech.lamprism.lampray.storage.StorageException;
import tech.lamprism.lampray.storage.persistence.StorageFileEntity;
import tech.lamprism.lampray.storage.persistence.StorageFileRepository;
import tech.rollw.common.web.DataErrorCode;

@Service
public class StorageFileLookupService {
    private final StorageFileRepository storageFileRepository;

    public StorageFileLookupService(StorageFileRepository storageFileRepository) {
        this.storageFileRepository = storageFileRepository;
    }

    public StorageFileEntity requireFileEntity(String fileId) {
        return storageFileRepository.findById(fileId)
                .orElseThrow(() -> new StorageException(DataErrorCode.ERROR_DATA_NOT_EXIST,
                        "File not found: " + fileId));
    }
}
