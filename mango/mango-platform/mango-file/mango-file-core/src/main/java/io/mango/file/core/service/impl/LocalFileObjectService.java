package io.mango.file.core.service.impl;

import io.mango.file.core.entity.FileStorageConfigEntity;
import io.mango.file.core.service.ILocalFileObjectService;
import io.mango.file.core.storage.FileObject;
import io.mango.file.core.storage.FileStorageRouter;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

/** Local file object reader. */
@Service
@RequiredArgsConstructor
public class LocalFileObjectService implements ILocalFileObjectService {

    private final FileStorageRouter fileStorageRouter;

    @Override
    public FileObject get(String bucket, String objectName) {
        FileStorageConfigEntity config = new FileStorageConfigEntity();
        config.setStorageType("LOCAL");
        config.setBucketName(bucket);
        return fileStorageRouter.getObject(config, objectName);
    }
}
