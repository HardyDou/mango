package io.mango.system.core.service;

import io.mango.common.result.R;
import io.mango.system.api.po.DictTypePo;
import io.mango.system.api.po.DictDataPo;
import io.mango.system.api.vo.DictTypeVO;
import io.mango.system.api.vo.DictDataVO;
import io.mango.system.api.vo.DictOptionVO;

import java.util.List;

public interface IDictService {

    R<List<DictTypeVO>> listTypes();

    R<DictTypeVO> getType(Long id);

    R<Long> createType(DictTypePo po);

    R<Boolean> updateType(DictTypePo po);

    R<Boolean> deleteType(Long id);

    R<List<DictDataVO>> listData(Long typeId);

    R<DictDataVO> getData(Long id);

    R<Long> createData(DictDataPo po);

    R<Boolean> updateData(DictDataPo po);

    R<Boolean> deleteData(Long id);

    R<List<DictOptionVO>> getOptions(String typeCode);
}
