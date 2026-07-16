package io.mango.system.api;

import io.mango.common.result.R;
import io.mango.system.api.command.SaveSysTenantCommand;
import io.mango.system.api.vo.LoginTenantOptionVO;
import io.mango.system.api.vo.SysTenantVO;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import org.springframework.validation.annotation.Validated;

import java.util.List;

@Validated
public interface SysTenantApi {
    R<List<SysTenantVO>> list();
    R<List<LoginTenantOptionVO>> listLoginOptions();
    R<SysTenantVO> get(@NotNull Long id);
    R<Long> create(@Valid SaveSysTenantCommand command);
    R<Boolean> update(@Valid SaveSysTenantCommand command);
    R<Boolean> delete(@NotNull Long id);
    R<Boolean> updateStatus(@NotNull Long id, @NotNull Integer status);
}
