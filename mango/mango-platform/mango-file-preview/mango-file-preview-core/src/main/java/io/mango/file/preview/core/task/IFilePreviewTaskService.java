package io.mango.file.preview.core.task;

import io.mango.file.preview.api.vo.FilePreviewTaskVO;

/** 预览产物任务编排。 */
public interface IFilePreviewTaskService {

    FilePreviewTaskVO submit(Long fileId, boolean allowLargeFile);

    FilePreviewTaskVO status(Long fileId, boolean allowLargeFile);
}
