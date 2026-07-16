package io.mango.file.core.service;

import io.mango.file.api.command.SaveFileSettingsCommand;
import io.mango.file.api.vo.FileSettingsVO;

/**
 * 文件中心运行时配置服务。
 */
public interface IFileSettingsService {

    FileSettingsVO get();

    Boolean save(SaveFileSettingsCommand command);

    FileSettingsVO current();
}
