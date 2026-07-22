package io.mango.file.core.service.impl;

import io.mango.common.result.Require;
import io.mango.file.api.command.ImportRemoteImageCommand;
import io.mango.file.api.command.SaveFileCommand;
import io.mango.file.api.enums.FileAccessLevel;
import io.mango.file.api.enums.FileCode;
import io.mango.file.api.vo.FileRecordVO;
import io.mango.file.core.service.IFileService;
import io.mango.file.core.service.IRemoteFileImportService;
import io.mango.file.core.service.remote.IRemoteImageFetcher;
import io.mango.file.core.service.remote.RemoteImageContent;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.io.ByteArrayInputStream;
import java.util.Locale;
import java.util.regex.Pattern;

/** Default remote image import orchestration. */
@Service
@RequiredArgsConstructor
public class RemoteFileImportService implements IRemoteFileImportService {

    private static final Pattern FORBIDDEN_PERSISTED_URL = Pattern.compile(
            "(?i)(?:https?://|data:|blob:)");

    private final IRemoteImageFetcher remoteImageFetcher;
    private final IFileService fileService;

    @Override
    public FileRecordVO importImage(ImportRemoteImageCommand command) {
        Require.notNull(command, FileCode.FILE_REMOTE_URL_INVALID);
        Require.notBlank(command.getSourceUrl(), FileCode.FILE_REMOTE_URL_INVALID);
        Require.isFalse(command.getBizMeta() != null
                        && FORBIDDEN_PERSISTED_URL.matcher(command.getBizMeta()).find(),
                FileCode.FILE_REMOTE_URL_INVALID);
        RemoteImageContent content = remoteImageFetcher.fetch(command.getSourceUrl());
        byte[] bytes = content.bytes();
        SaveFileCommand saveCommand = new SaveFileCommand();
        saveCommand.setInputStream(new ByteArrayInputStream(bytes));
        saveCommand.setFileName("remote-image." + content.extension().toLowerCase(Locale.ROOT));
        saveCommand.setFileSize((long) bytes.length);
        saveCommand.setContentType(content.contentType());
        saveCommand.setPurpose("image");
        saveCommand.setAccessLevel(FileAccessLevel.PRIVATE.name());
        saveCommand.setBizType(command.getBizType());
        saveCommand.setBizId(command.getBizId());
        saveCommand.setBizMeta(command.getBizMeta());
        saveCommand.setDirectoryId(command.getDirectoryId());
        return fileService.save(saveCommand);
    }
}
