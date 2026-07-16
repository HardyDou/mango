package io.mango.file.core.service;

import io.mango.common.vo.PageResult;
import io.mango.file.api.command.SaveFileStorageConfigCommand;
import io.mango.file.api.command.TestFileStorageConfigCommand;
import io.mango.file.api.query.FileStorageConfigPageQuery;
import io.mango.file.api.vo.FileStorageConfigTestVO;
import io.mango.file.api.vo.FileStorageConfigVO;
import io.mango.file.core.entity.FileStorageConfigEntity;
import io.mango.file.core.service.model.EnabledFileStorageKey;

/**
 * 文件存储配置服务。
 */
public interface IFileStorageConfigService {

    PageResult<FileStorageConfigVO> page(FileStorageConfigPageQuery query);

    FileStorageConfigVO get(Long id);

    Long create(SaveFileStorageConfigCommand command);

    Boolean update(SaveFileStorageConfigCommand command);

    Boolean delete(Long id);

    Boolean activate(Long id);

    FileStorageConfigTestVO test(TestFileStorageConfigCommand command);

    FileStorageConfigEntity activeConfig();

    FileStorageConfigEntity getEnabledConfig(EnabledFileStorageKey key);
}
