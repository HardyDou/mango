package io.mango.area.core.service;

import io.mango.area.api.command.SaveAreaCommand;
import io.mango.area.api.vo.SysAreaTreeNodeVO;
import io.mango.area.api.vo.SysAreaVO;

import java.util.List;

public interface ISysAreaService {
    List<SysAreaTreeNodeVO> tree(Integer type);
    List<SysAreaVO> listByPid(Long parentId);
    SysAreaVO getById(Long id);
    SysAreaVO getByAdcode(Long adcode);
    Void create(SaveAreaCommand command);
    Void update(SaveAreaCommand command);
    Void delete(Long id);
    List<SysAreaVO> listActive();
}
