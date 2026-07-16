package io.mango.system.core.service;

import io.mango.system.api.command.SaveSysTenantCommand;
import io.mango.system.api.vo.LoginTenantOptionVO;
import io.mango.system.api.vo.SysTenantVO;

import java.util.List;

public interface ISysTenantService {
    List<SysTenantVO> list();
    List<LoginTenantOptionVO> listLoginOptions();
    SysTenantVO get(Long id);
    Long create(SaveSysTenantCommand command);
    Boolean update(SaveSysTenantCommand command);
    Boolean delete(Long id);
    Boolean updateStatus(Long id, Integer status);
}
