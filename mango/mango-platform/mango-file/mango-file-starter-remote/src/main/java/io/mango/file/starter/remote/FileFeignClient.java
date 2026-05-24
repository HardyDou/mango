package io.mango.file.starter.remote;

import io.mango.common.result.R;
import io.mango.common.vo.PageResult;
import io.mango.file.api.FileApi;
import io.mango.file.api.command.FileArchiveCommand;
import io.mango.file.api.command.SaveGeneratedFileCommand;
import io.mango.file.api.query.FileRecordPageQuery;
import io.mango.file.api.vo.FileDownloadVO;
import io.mango.file.api.vo.FilePreviewVO;
import io.mango.file.api.vo.FileRecordVO;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;

import java.io.IOException;

/**
 * 文件服务 Feign 适配器。
 */
@FeignClient(name = "mango-file", path = "/file/files")
public interface FileFeignClient extends FileApi {

    @Override
    @GetMapping("/page")
    R<PageResult<FileRecordVO>> page(FileRecordPageQuery query);

    @Override
    @GetMapping("/detail")
    R<FileRecordVO> get(@RequestParam("id") Long id);

    @Override
    @GetMapping("/preview")
    R<FilePreviewVO> preview(@RequestParam("id") Long id);

    @Override
    default R<FileRecordVO> saveGenerated(SaveGeneratedFileCommand command) {
        if (command == null || command.getInputStream() == null) {
            return R.fail("文件输入流不能为空");
        }
        try {
            byte[] content = command.getInputStream().readAllBytes();
            Resource file = new NamedByteArrayResource(content, command.getFileName());
            return saveGenerated(file, command.getPurpose(), command.getBizType(), command.getBizId());
        } catch (IOException ex) {
            return R.fail("文件读取失败");
        }
    }

    @PostMapping(path = "/generated", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    R<FileRecordVO> saveGenerated(@RequestPart("file") Resource file,
                                  @RequestParam(value = "purpose", required = false) String purpose,
                                  @RequestParam(value = "bizType", required = false) String bizType,
                                  @RequestParam(value = "bizId", required = false) String bizId);

    @Override
    default FileDownloadVO download(Long id) {
        return FileRemoteDownloadConverter.toFileDownload(downloadResponse(id));
    }

    @GetMapping("/download")
    ResponseEntity<byte[]> downloadResponse(@RequestParam("id") Long id);

    @Override
    @DeleteMapping
    default R<Boolean> archive(FileArchiveCommand command) {
        if (command == null) {
            return R.fail("文件归档命令不能为空");
        }
        return archive(command.getId(), command.getReason());
    }

    @DeleteMapping
    R<Boolean> archive(@RequestParam("id") Long id, @RequestParam(value = "reason", required = false) String reason);

    class NamedByteArrayResource extends ByteArrayResource {

        private final String filename;

        NamedByteArrayResource(byte[] byteArray, String filename) {
            super(byteArray);
            this.filename = filename;
        }

        @Override
        public String getFilename() {
            return filename;
        }
    }
}
