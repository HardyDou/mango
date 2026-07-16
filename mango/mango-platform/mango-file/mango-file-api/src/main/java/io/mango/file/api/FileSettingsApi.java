package io.mango.file.api;

import io.mango.common.result.R;
import io.mango.file.api.command.SaveFileSettingsCommand;
import io.mango.file.api.vo.FileSettingsVO;
import jakarta.validation.Valid;
import org.springframework.validation.annotation.Validated;

/** File runtime settings contract. */
@Validated
public interface FileSettingsApi {

    R<FileSettingsVO> get();

    R<Boolean> save(@Valid SaveFileSettingsCommand command);
}
