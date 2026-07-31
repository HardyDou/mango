package io.mango.file.api;

import io.mango.common.result.R;
import io.mango.common.vo.PageResult;
import io.mango.file.api.command.FileDeleteCommand;
import io.mango.file.api.command.FileMergePdfCommand;
import io.mango.file.api.command.FilePackageCommand;
import io.mango.file.api.command.FilePackageSizeControlCommand;
import io.mango.file.api.query.FileRecordPageQuery;
import io.mango.file.api.vo.FilePackageResultVO;
import io.mango.file.api.vo.FilePreviewVO;
import io.mango.file.api.vo.FileRecordVO;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import org.springframework.validation.annotation.Validated;

/** File metadata and lifecycle HTTP contract. */
@Validated
public interface FileApi {

    R<PageResult<FileRecordVO>> page(@Valid FileRecordPageQuery query);

    R<FileRecordVO> get(@NotNull @Positive Long id);

    R<FilePreviewVO> preview(@NotNull @Positive Long id);

    R<FileRecordVO> packageFiles(@Valid FilePackageCommand command);

    /**
     * 生成单个 ZIP，并按自动或手动模式控制可压缩条目的大小。
     * 无法达到目标大小时仍返回已保存的 ZIP 和实际处理结果。
     *
     * @param command 大小控制打包命令
     * @return ZIP 文件记录和大小控制摘要
     */
    R<FilePackageResultVO> packageFilesWithSizeControl(@Valid FilePackageSizeControlCommand command);

    R<FileRecordVO> mergeToPdf(@Valid FileMergePdfCommand command);

    R<Boolean> archive(
            @NotNull @Positive Long id,
            @Size(max = 500, message = "归档原因不能超过500个字符") String reason);

    R<Boolean> delete(@Valid FileDeleteCommand command);
}
