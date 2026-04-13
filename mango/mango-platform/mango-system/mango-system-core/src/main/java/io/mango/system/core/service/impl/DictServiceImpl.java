package io.mango.system.core.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import io.mango.common.result.R;
import io.mango.system.api.po.DictTypePo;
import io.mango.system.api.po.DictDataPo;
import io.mango.system.api.vo.DictTypeVO;
import io.mango.system.api.vo.DictDataVO;
import io.mango.system.api.vo.DictOptionVO;
import io.mango.system.core.entity.DictType;
import io.mango.system.core.entity.DictData;
import io.mango.system.core.mapper.DictTypeMapper;
import io.mango.system.core.mapper.DictDataMapper;
import io.mango.system.core.service.IDictService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class DictServiceImpl implements IDictService {

    private final DictTypeMapper dictTypeMapper;
    private final DictDataMapper dictDataMapper;

    @Override
    public R<List<DictTypeVO>> listTypes() {
        List<DictType> list = dictTypeMapper.selectList(null);
        List<DictTypeVO> voList = list.stream().map(this::convertToTypeVO).collect(Collectors.toList());
        return R.ok(voList);
    }

    @Override
    public R<DictTypeVO> getType(Long id) {
        DictType entity = dictTypeMapper.selectById(id);
        if (entity == null) {
            return R.fail("字典类型不存在");
        }
        return R.ok(convertToTypeVO(entity));
    }

    @Override
    public R<Long> createType(DictTypePo po) {
        DictType entity = new DictType();
        entity.setDictType(po.getDictType());
        entity.setDictName(po.getDictName());
        entity.setStatus(po.getStatus());
        entity.setRemark(po.getRemark());
        dictTypeMapper.insert(entity);
        return R.ok(entity.getId());
    }

    @Override
    public R<Boolean> updateType(DictTypePo po) {
        if (po.getId() == null) {
            return R.fail("ID不能为空");
        }
        DictType entity = new DictType();
        entity.setId(po.getId());
        entity.setDictType(po.getDictType());
        entity.setDictName(po.getDictName());
        entity.setStatus(po.getStatus());
        entity.setRemark(po.getRemark());
        return R.ok(dictTypeMapper.updateById(entity) > 0);
    }

    @Override
    public R<Boolean> deleteType(Long id) {
        return R.ok(dictTypeMapper.deleteById(id) > 0);
    }

    @Override
    public R<List<DictDataVO>> listData(Long typeId) {
        LambdaQueryWrapper<DictData> wrapper = new LambdaQueryWrapper<>();
        if (typeId != null) {
            DictType dictType = dictTypeMapper.selectById(typeId);
            if (dictType != null) {
                wrapper.eq(DictData::getDictType, dictType.getDictType());
            }
        }
        wrapper.orderByAsc(DictData::getSort);
        List<DictData> list = dictDataMapper.selectList(wrapper);
        List<DictDataVO> voList = list.stream().map(this::convertToDataVO).collect(Collectors.toList());
        return R.ok(voList);
    }

    @Override
    public R<DictDataVO> getData(Long id) {
        DictData entity = dictDataMapper.selectById(id);
        if (entity == null) {
            return R.fail("字典数据不存在");
        }
        return R.ok(convertToDataVO(entity));
    }

    @Override
    public R<Long> createData(DictDataPo po) {
        DictData entity = new DictData();
        entity.setDictType(po.getDictType());
        entity.setDictLabel(po.getDictLabel());
        entity.setDictValue(po.getDictValue());
        entity.setSort(po.getSort());
        entity.setStatus(po.getStatus());
        dictDataMapper.insert(entity);
        return R.ok(entity.getId());
    }

    @Override
    public R<Boolean> updateData(DictDataPo po) {
        if (po.getId() == null) {
            return R.fail("ID不能为空");
        }
        DictData entity = new DictData();
        entity.setId(po.getId());
        entity.setDictType(po.getDictType());
        entity.setDictLabel(po.getDictLabel());
        entity.setDictValue(po.getDictValue());
        entity.setSort(po.getSort());
        entity.setStatus(po.getStatus());
        return R.ok(dictDataMapper.updateById(entity) > 0);
    }

    @Override
    public R<Boolean> deleteData(Long id) {
        return R.ok(dictDataMapper.deleteById(id) > 0);
    }

    @Override
    public R<List<DictOptionVO>> getOptions(String typeCode) {
        LambdaQueryWrapper<DictData> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(DictData::getDictType, typeCode)
               .eq(DictData::getStatus, 1)
               .orderByAsc(DictData::getSort);
        List<DictData> list = dictDataMapper.selectList(wrapper);
        List<DictOptionVO> options = list.stream().map(d -> {
            DictOptionVO vo = new DictOptionVO();
            vo.setLabel(d.getDictLabel());
            vo.setValue(d.getDictValue());
            return vo;
        }).collect(Collectors.toList());
        return R.ok(options);
    }

    private DictTypeVO convertToTypeVO(DictType entity) {
        DictTypeVO vo = new DictTypeVO();
        vo.setId(entity.getId());
        vo.setDictType(entity.getDictType());
        vo.setDictName(entity.getDictName());
        vo.setStatus(entity.getStatus());
        vo.setRemark(entity.getRemark());
        return vo;
    }

    private DictDataVO convertToDataVO(DictData entity) {
        DictDataVO vo = new DictDataVO();
        vo.setId(entity.getId());
        vo.setDictType(entity.getDictType());
        vo.setDictLabel(entity.getDictLabel());
        vo.setDictValue(entity.getDictValue());
        vo.setSort(entity.getSort());
        vo.setStatus(entity.getStatus());
        return vo;
    }
}
