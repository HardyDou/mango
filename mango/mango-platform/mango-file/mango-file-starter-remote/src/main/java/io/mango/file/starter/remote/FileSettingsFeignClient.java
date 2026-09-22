package io.mango.file.starter.remote;

import io.mango.common.result.R;
import io.mango.file.api.FileSettingsApi;
import io.mango.file.api.command.SaveFileSettingsCommand;
import io.mango.file.api.vo.FileSettingsVO;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;

/** 文件中心运行时配置远程调用。 */
@FeignClient(name = "mango-file", contextId = "fileSettingsFeignClient", path = "/file/settings")
public interface FileSettingsFeignClient extends FileSettingsApi {

    @Override
    @GetMapping
    R<FileSettingsVO> get();

    @Override
    @PutMapping
    R<Boolean> save(@RequestBody SaveFileSettingsCommand command);
}
