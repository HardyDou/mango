package io.mango.system.core.service;

import io.mango.system.api.command.SaveAdminBrandingCommand;
import io.mango.system.api.vo.AdminBrandingVO;

public interface IAdminBrandingService {
    AdminBrandingVO get();
    Boolean save(SaveAdminBrandingCommand command);
}
