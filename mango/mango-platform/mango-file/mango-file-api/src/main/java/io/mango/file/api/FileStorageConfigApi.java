package io.mango.file.api;

import io.mango.common.result.R;
import io.mango.common.vo.PageResult;
import io.mango.file.api.command.SaveFileStorageConfigCommand;
import io.mango.file.api.command.TestFileStorageConfigCommand;
import io.mango.file.api.query.FileStorageConfigPageQuery;
import io.mango.file.api.vo.FileStorageConfigTestVO;
import io.mango.file.api.vo.FileStorageConfigVO;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import org.springframework.validation.annotation.Validated;

/** File storage configuration management contract. */
@Validated
public interface FileStorageConfigApi {

    R<PageResult<FileStorageConfigVO>> page(@Valid FileStorageConfigPageQuery query);

    R<FileStorageConfigVO> get(@NotNull @Positive Long id);

    R<Long> create(@Valid SaveFileStorageConfigCommand command);

    R<Boolean> update(@Valid SaveFileStorageConfigCommand command);

    R<Boolean> delete(@NotNull @Positive Long id);

    R<Boolean> activate(@NotNull @Positive Long id);

    R<FileStorageConfigTestVO> test(@Valid TestFileStorageConfigCommand command);
}
