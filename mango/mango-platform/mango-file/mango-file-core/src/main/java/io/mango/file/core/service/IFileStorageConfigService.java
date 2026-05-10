package io.mango.file.core.service;

import io.mango.common.result.R;
import io.mango.common.vo.PageResult;
import io.mango.file.api.command.SaveFileStorageConfigCommand;
import io.mango.file.api.command.TestFileStorageConfigCommand;
import io.mango.file.api.query.FileStorageConfigPageQuery;
import io.mango.file.api.vo.FileStorageConfigTestVO;
import io.mango.file.api.vo.FileStorageConfigVO;
import io.mango.file.core.entity.FileStorageConfig;

/**
 * 文件存储配置服务。
 */
public interface IFileStorageConfigService {

    R<PageResult<FileStorageConfigVO>> page(FileStorageConfigPageQuery query);

    R<FileStorageConfigVO> get(Long id);

    R<Long> create(SaveFileStorageConfigCommand command);

    R<Boolean> update(SaveFileStorageConfigCommand command);

    R<Boolean> delete(Long id);

    R<Boolean> activate(Long id);

    R<FileStorageConfigTestVO> test(TestFileStorageConfigCommand command);

    FileStorageConfig activeConfig();

    FileStorageConfig getEnabledConfig(Long id, String storageType, String bucketName);
}
