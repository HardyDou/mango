package io.mango.file.core.service;

import io.mango.file.api.command.SaveFileDirectoryCommand;
import io.mango.file.api.vo.FileDirectoryVO;
import io.mango.file.core.entity.FileDirectoryEntity;

import java.util.List;

/**
 * 文件逻辑目录服务。
 */
public interface IFileDirectoryService {

    List<FileDirectoryVO> tree();

    Long create(SaveFileDirectoryCommand command);

    Boolean update(SaveFileDirectoryCommand command);

    Boolean delete(Long id);

    FileDirectoryEntity selectVisible(Long directoryId);
}
