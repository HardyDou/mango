package io.mango.system.core.service;

import io.mango.system.api.command.SaveSysConfigCommand;
import io.mango.system.api.enums.ConfigTypeEnum;
import io.mango.system.api.vo.SysConfigVO;

import java.util.List;

public interface ISysConfigService {
    List<SysConfigVO> list(String type, String domainCode);
    List<SysConfigVO> listByType(ConfigTypeEnum type, String domainCode);
    SysConfigVO get(Long id);
    Long create(SaveSysConfigCommand command);
    Boolean update(SaveSysConfigCommand command);
    Boolean delete(Long id);
    Boolean updateValue(Long id, String value);
    List<String> listTypes();
    List<String> listValueTypes();
}
