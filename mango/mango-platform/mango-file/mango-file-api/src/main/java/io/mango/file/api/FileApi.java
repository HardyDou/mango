package io.mango.file.api;

import io.mango.common.result.R;
import io.mango.common.vo.PageResult;
import io.mango.file.api.command.FileDeleteCommand;
import io.mango.file.api.command.FileMergePdfCommand;
import io.mango.file.api.command.FilePackageCommand;
import io.mango.file.api.query.FileRecordPageQuery;
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

    R<FileRecordVO> mergeToPdf(@Valid FileMergePdfCommand command);

    R<Boolean> archive(
            @NotNull @Positive Long id,
            @Size(max = 500, message = "归档原因不能超过500个字符") String reason);

    R<Boolean> delete(@Valid FileDeleteCommand command);
}
