package io.mango.file.preview.core.task;

import io.mango.file.preview.api.vo.FilePreviewTaskVO;

/** 预览产物任务编排。 */
public interface IFilePreviewTaskService {

    default FilePreviewTaskVO submit(Long fileId) {
        return submit(fileId, false);
    }

    FilePreviewTaskVO submit(Long fileId, boolean allowLargeFile);

    default FilePreviewTaskVO status(Long fileId) {
        return status(fileId, false);
    }

    FilePreviewTaskVO status(Long fileId, boolean allowLargeFile);
}
