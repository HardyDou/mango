package io.mango.file.starter.remote;

import io.mango.common.result.R;
import io.mango.file.api.FileImportApi;
import io.mango.file.api.command.ImportRemoteImageCommand;
import io.mango.file.api.vo.FileRecordVO;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

/** Remote adapter for controlled image import. */
@FeignClient(name = "mango-file", contextId = "fileImportFeignClient", path = "/file/files")
public interface FileImportFeignClient extends FileImportApi {

    @Override
    @PostMapping("/import-image")
    R<FileRecordVO> importImage(@RequestBody ImportRemoteImageCommand command);
}
