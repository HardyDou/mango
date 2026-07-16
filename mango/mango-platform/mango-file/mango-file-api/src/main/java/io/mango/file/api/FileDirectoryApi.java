package io.mango.file.api;

import io.mango.common.result.R;
import io.mango.file.api.command.SaveFileDirectoryCommand;
import io.mango.file.api.vo.FileDirectoryVO;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import org.springframework.validation.annotation.Validated;

import java.util.List;

/** File directory management contract. */
@Validated
public interface FileDirectoryApi {

    R<List<FileDirectoryVO>> tree();

    R<Long> create(@Valid SaveFileDirectoryCommand command);

    R<Boolean> update(@Valid SaveFileDirectoryCommand command);

    R<Boolean> delete(@NotNull @Positive Long id);
}
