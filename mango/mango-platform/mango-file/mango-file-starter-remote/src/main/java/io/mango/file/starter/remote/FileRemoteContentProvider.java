package io.mango.file.starter.remote;

import io.mango.common.exception.BizException;
import io.mango.common.result.R;
import io.mango.file.api.IFileContentProvider;
import io.mango.file.api.command.SaveFileCommand;
import io.mango.file.api.enums.FileCode;
import io.mango.file.api.vo.FileDownloadVO;
import io.mango.file.api.vo.FileRecordVO;
import lombok.RequiredArgsConstructor;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;

/** Remote implementation of the generated file content provider. */
@RequiredArgsConstructor
public class FileRemoteContentProvider implements IFileContentProvider {

    private final FileBinaryFeignClient fileBinaryFeignClient;

    @Override
    public FileRecordVO save(SaveFileCommand command) {
        if (command == null) {
            throw new BizException(FileCode.FILE_EMPTY.getCode(), FileCode.FILE_EMPTY.getMessage());
        }
        R<FileRecordVO> response = fileBinaryFeignClient.upload(
                new CommandMultipartFile(command),
                command.getPurpose(),
                command.getAccessLevel(),
                command.getBizType(),
                command.getBizId(),
                command.getBizMeta(),
                command.getDirectoryId());
        if (response == null || !response.isSuccess() || response.getData() == null) {
            String message = response == null ? FileCode.FILE_STORE_FAILED.getMessage() : response.getMsg();
            throw new BizException(FileCode.FILE_STORE_FAILED.getCode(), message);
        }
        return response.getData();
    }

    @Override
    public FileDownloadVO download(Long id) {
        return FileRemoteDownloadConverter.toFileDownload(fileBinaryFeignClient.download(id));
    }

    @Override
    public FileDownloadVO downloadForService(Long id) {
        return download(id);
    }

    private static final class CommandMultipartFile implements MultipartFile {

        private final SaveFileCommand command;
        private final byte[] content;

        private CommandMultipartFile(SaveFileCommand command) {
            this.command = command;
            try (InputStream inputStream = command.getInputStream()) {
                this.content = inputStream == null ? new byte[0] : inputStream.readAllBytes();
            } catch (IOException ex) {
                throw new BizException(FileCode.FILE_READ_FAILED.getCode(), FileCode.FILE_READ_FAILED.getMessage(), ex);
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
        public void transferTo(java.io.File destination) throws IOException {
            try (InputStream inputStream = getInputStream();
                    java.io.OutputStream outputStream = java.nio.file.Files.newOutputStream(destination.toPath())) {
                inputStream.transferTo(outputStream);
            }
        }
    }
}
