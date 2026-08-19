package io.mango.notice.core.integration;

import io.mango.file.api.FileApi;
import io.mango.file.api.FileImportApi;
import io.mango.file.api.command.FileDeleteCommand;
import io.mango.file.api.command.ImportRemoteImageCommand;
import io.mango.file.api.vo.FileRecordVO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;

/** Isolates Notice from file API transport envelopes. */
@Component
@RequiredArgsConstructor
public class NoticeFileGateway {

    private final FileImportApi fileImportApi;
    private final FileApi fileApi;

    public NoticeRemoteResult<FileRecordVO> importImage(ImportRemoteImageCommand command) {
        return NoticeRemoteResult.from(fileImportApi.importImage(command));
    }

    public NoticeRemoteResult<Boolean> delete(Long fileId) {
        FileDeleteCommand command = new FileDeleteCommand();
        command.setIds(List.of(fileId));
        return NoticeRemoteResult.from(fileApi.delete(command));
    }
}
