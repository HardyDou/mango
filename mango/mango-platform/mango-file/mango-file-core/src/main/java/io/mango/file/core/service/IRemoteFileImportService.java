package io.mango.file.core.service;

import io.mango.file.api.command.ImportRemoteImageCommand;
import io.mango.file.api.vo.FileRecordVO;

/** Imports remote images through the managed file storage chain. */
public interface IRemoteFileImportService {

    /**
     * Imports one remote image for the current tenant.
     *
     * @param command source URL and optional business ownership
     * @return managed file record
     */
    FileRecordVO importImage(ImportRemoteImageCommand command);
}
