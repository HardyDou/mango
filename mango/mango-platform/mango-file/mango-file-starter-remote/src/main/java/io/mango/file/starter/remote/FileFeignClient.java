package io.mango.file.starter.remote;

import io.mango.common.result.R;
import io.mango.common.vo.PageResult;
import io.mango.file.api.FileApi;
import io.mango.file.api.command.FileArchiveCommand;
import io.mango.file.api.command.SaveFileCommand;
import io.mango.file.api.query.FileRecordPageQuery;
import io.mango.file.api.vo.FileDownloadVO;
import io.mango.file.api.vo.FilePreviewVO;
import io.mango.file.api.vo.FileRecordVO;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;

/**
 * 文件服务 Feign 适配器。
 */
@FeignClient(name = "mango-file", contextId = "fileFeignClient", path = "/file/files")
public interface FileFeignClient extends FileApi {

    @Override
    default R<FileRecordVO> save(SaveFileCommand command) {
        if (command == null) {
            return R.fail("文件保存命令不能为空");
        }
        return upload(new CommandMultipartFile(command),
                command.getPurpose(),
                command.getAccessLevel(),
                command.getBizType(),
                command.getBizId(),
                command.getBizMeta(),
                command.getDirectoryId());
    }

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    R<FileRecordVO> upload(@RequestPart("file") MultipartFile file,
                           @RequestParam(value = "purpose", required = false) String purpose,
                           @RequestParam(value = "accessLevel", required = false) String accessLevel,
                           @RequestParam(value = "bizType", required = false) String bizType,
                           @RequestParam(value = "bizId", required = false) String bizId,
                           @RequestParam(value = "bizMeta", required = false) String bizMeta,
                           @RequestParam(value = "directoryId", required = false) Long directoryId);

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
    default FileDownloadVO download(Long id) {
        return FileRemoteDownloadConverter.toFileDownload(downloadResponse(id));
    }

    @Override
    default FileDownloadVO downloadForService(Long id) {
        return download(id);
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

    final class CommandMultipartFile implements MultipartFile {

        private final SaveFileCommand command;
        private final byte[] content;

        private CommandMultipartFile(SaveFileCommand command) {
            this.command = command;
            try (InputStream inputStream = command.getInputStream()) {
                this.content = inputStream == null ? new byte[0] : inputStream.readAllBytes();
            } catch (IOException ex) {
                throw new IllegalStateException("读取文件保存命令内容失败", ex);
            }
        }

        @Override
        public String getName() {
            return "file";
        }

        @Override
        public String getOriginalFilename() {
            return command.getFileName();
        }

        @Override
        public String getContentType() {
            return command.getContentType();
        }

        @Override
        public boolean isEmpty() {
            return content.length == 0;
        }

        @Override
        public long getSize() {
            return content.length;
        }

        @Override
        public byte[] getBytes() {
            return content.clone();
        }

        @Override
        public InputStream getInputStream() {
            return new ByteArrayInputStream(content);
        }

        @Override
        public void transferTo(java.io.File dest) throws IOException, IllegalStateException {
            try (InputStream inputStream = getInputStream();
                 java.io.OutputStream outputStream = java.nio.file.Files.newOutputStream(dest.toPath())) {
                inputStream.transferTo(outputStream);
            }
        }
    }
}
