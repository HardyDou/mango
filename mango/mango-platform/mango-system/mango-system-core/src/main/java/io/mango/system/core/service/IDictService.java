package io.mango.system.core.service;

import io.mango.system.api.command.SaveDictDataCommand;
import io.mango.system.api.command.SaveDictTypeCommand;
import io.mango.system.api.vo.DictDataVO;
import io.mango.system.api.vo.DictOptionVO;
import io.mango.system.api.vo.DictTypeVO;

import java.util.List;

public interface IDictService {
    List<DictTypeVO> listTypes(String domainCode);
    DictTypeVO getType(Long id);
    Long createType(SaveDictTypeCommand command);
    Boolean updateType(SaveDictTypeCommand command);
    Boolean deleteType(Long id);
    List<DictDataVO> listData(Long typeId);
    DictDataVO getData(Long id);
    Long createData(SaveDictDataCommand command);
    Boolean updateData(SaveDictDataCommand command);
    Boolean deleteData(Long id);
    List<DictOptionVO> getOptions(String typeCode);
}
