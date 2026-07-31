package io.mango.file.starter.remote;

import io.mango.common.result.R;
import io.mango.common.vo.PageResult;
import io.mango.file.api.FileApi;
import io.mango.file.api.command.FileDeleteCommand;
import io.mango.file.api.command.FileMergePdfCommand;
import io.mango.file.api.command.FilePackageCommand;
import io.mango.file.api.command.FilePackageSizeControlCommand;
import io.mango.file.api.query.FileRecordPageQuery;
import io.mango.file.api.vo.FilePreviewVO;
import io.mango.file.api.vo.FilePackageResultVO;
import io.mango.file.api.vo.FileRecordVO;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.cloud.openfeign.SpringQueryMap;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;

/** Remote adapter for file metadata and lifecycle operations. */
@FeignClient(name = "mango-file", contextId = "fileFeignClient", path = "/file/files")
public interface FileFeignClient extends FileApi {

    @Override
    @GetMapping("/page")
    R<PageResult<FileRecordVO>> page(@SpringQueryMap FileRecordPageQuery query);

    @Override
    @GetMapping("/detail")
    R<FileRecordVO> get(@RequestParam("id") Long id);

    @Override
    @GetMapping("/preview")
    R<FilePreviewVO> preview(@RequestParam("id") Long id);

    @Override
    @PostMapping("/package")
    R<FileRecordVO> packageFiles(@RequestBody FilePackageCommand command);

    @Override
    @PostMapping("/package-size-control")
    R<FilePackageResultVO> packageFilesWithSizeControl(@RequestBody FilePackageSizeControlCommand command);

    @Override
    @PostMapping("/merge-pdf")
    R<FileRecordVO> mergeToPdf(@RequestBody FileMergePdfCommand command);

    @Override
    @DeleteMapping
    R<Boolean> archive(
            @RequestParam("id") Long id,
            @RequestParam(value = "reason", required = false) String reason);

    @Override
    @PostMapping("/delete")
    R<Boolean> delete(@RequestBody FileDeleteCommand command);
}
