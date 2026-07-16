package io.mango.file.starter.remote;

import io.mango.common.result.R;
import io.mango.file.api.vo.FileRecordVO;
import io.mango.common.contract.BinaryHttpAdapter;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.multipart.MultipartFile;

/** Remote binary transport used by {@link FileRemoteContentProvider}. */
@BinaryHttpAdapter
@FeignClient(name = "mango-file", contextId = "fileBinaryFeignClient", path = "/file/files")
public interface FileBinaryFeignClient {

    @PostMapping
    R<FileRecordVO> upload(
            @RequestPart("file") MultipartFile file,
            @RequestParam(value = "purpose", required = false) String purpose,
            @RequestParam(value = "accessLevel", required = false) String accessLevel,
            @RequestParam(value = "bizType", required = false) String bizType,
            @RequestParam(value = "bizId", required = false) String bizId,
            @RequestParam(value = "bizMeta", required = false) String bizMeta,
            @RequestParam(value = "directoryId", required = false) Long directoryId);

    @GetMapping("/download")
    ResponseEntity<byte[]> download(@RequestParam("id") Long id);
}
