package io.mango.file.core.service;

import io.mango.common.result.R;
import io.mango.file.api.command.SaveFileDirectoryCommand;
import io.mango.file.api.vo.FileDirectoryVO;
import io.mango.file.core.entity.FileDirectory;

import java.util.List;

/**
 * 文件逻辑目录服务。
 */
public interface IFileDirectoryService {

    R<List<FileDirectoryVO>> tree();

    R<Long> create(SaveFileDirectoryCommand command);

    R<Boolean> update(SaveFileDirectoryCommand command);

    R<Boolean> delete(Long id);

    FileDirectory selectVisible(Long directoryId);
}
