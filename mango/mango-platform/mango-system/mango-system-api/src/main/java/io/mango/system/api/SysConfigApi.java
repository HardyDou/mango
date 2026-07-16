package io.mango.system.api;

import io.mango.common.result.R;
import io.mango.system.api.command.SaveSysConfigCommand;
import io.mango.system.api.enums.ConfigTypeEnum;
import io.mango.system.api.vo.SysConfigVO;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import org.springframework.validation.annotation.Validated;

import java.util.List;

@Validated
public interface SysConfigApi {
    R<List<SysConfigVO>> list(@Size(max = 20) String type, @Size(max = 64) String domainCode);
    R<SysConfigVO> get(@NotNull Long id);
    R<Long> create(@Valid SaveSysConfigCommand command);
    R<Boolean> update(@Valid SaveSysConfigCommand command);
    R<Boolean> delete(@NotNull Long id);
    R<Boolean> updateValue(@NotNull Long id, @Size(max = 65535) String value);
    R<List<SysConfigVO>> listByType(@NotNull ConfigTypeEnum type, @Size(max = 64) String domainCode);
    R<List<String>> groups();
    R<List<String>> valueTypes();
}
