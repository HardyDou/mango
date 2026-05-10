package io.mango.file.api;

import io.mango.common.result.R;
import io.mango.common.vo.PageResult;
import io.mango.file.api.command.FileArchiveCommand;
import io.mango.file.api.query.FileRecordPageQuery;
import io.mango.file.api.vo.FilePreviewVO;
import io.mango.file.api.vo.FileRecordVO;

/**
 * 文件能力 API 契约。
 */
public interface FileApi {

    /** 分页查询文件记录。 */
    R<PageResult<FileRecordVO>> page(FileRecordPageQuery query);

    /** 查询文件详情。 */
    R<FileRecordVO> get(Long id);

    /** 查询文件预览元数据。 */
    R<FilePreviewVO> preview(Long id);

    /** 归档文件记录。 */
    R<Boolean> archive(FileArchiveCommand command);
}
