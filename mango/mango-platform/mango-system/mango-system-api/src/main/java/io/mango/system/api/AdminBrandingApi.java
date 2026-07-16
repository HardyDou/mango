package io.mango.system.api;

import io.mango.common.result.R;
import io.mango.system.api.command.SaveAdminBrandingCommand;
import io.mango.system.api.vo.AdminBrandingVO;
import jakarta.validation.Valid;
import org.springframework.validation.annotation.Validated;

@Validated
public interface AdminBrandingApi {
    R<AdminBrandingVO> get();
    R<AdminBrandingVO> publicConfig();
    R<Boolean> save(@Valid SaveAdminBrandingCommand command);
}
