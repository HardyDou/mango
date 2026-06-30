package io.mango.system.core.service;

import io.mango.common.result.R;
import io.mango.system.api.command.SaveAdminBrandingCommand;
import io.mango.system.api.vo.AdminBrandingVO;

public interface IAdminBrandingService {

    R<AdminBrandingVO> get();

    R<Boolean> save(SaveAdminBrandingCommand command);
}
