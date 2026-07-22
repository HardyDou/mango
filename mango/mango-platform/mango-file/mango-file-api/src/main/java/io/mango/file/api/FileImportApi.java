package io.mango.file.api;

import io.mango.common.result.R;
import io.mango.file.api.command.ImportRemoteImageCommand;
import io.mango.file.api.vo.FileRecordVO;
import jakarta.validation.Valid;
import org.springframework.validation.annotation.Validated;

/** Remote file import contract. */
@Validated
public interface FileImportApi {

    /**
     * Imports a public remote image into the current tenant file storage.
     *
     * @param command remote image source and optional business ownership
     * @return the managed file record
     */
    R<FileRecordVO> importImage(@Valid ImportRemoteImageCommand command);
}
