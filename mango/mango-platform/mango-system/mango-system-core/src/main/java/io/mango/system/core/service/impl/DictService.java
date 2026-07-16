package io.mango.system.core.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import io.mango.common.result.Require;
import io.mango.system.api.enums.SystemCode;
import io.mango.system.api.command.SaveDictDataCommand;
import io.mango.system.api.command.SaveDictTypeCommand;
import io.mango.system.api.vo.DictDataVO;
import io.mango.system.api.vo.DictOptionVO;
import io.mango.system.api.vo.DictTypeVO;
import io.mango.system.core.entity.DictDataEntity;
import io.mango.system.core.entity.DictTypeEntity;
import io.mango.system.core.mapper.DictDataMapper;
import io.mango.system.core.mapper.DictTypeMapper;
import io.mango.system.core.service.IDictService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.List;

@Service
@RequiredArgsConstructor
public class DictService implements IDictService {

    private final DictTypeMapper dictTypeMapper;
    private final DictDataMapper dictDataMapper;

    @Override
    public List<DictTypeVO> listTypes(String domainCode) {
        LambdaQueryWrapper<DictTypeEntity> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(StringUtils.hasText(domainCode), DictTypeEntity::getDomainCode, trim(domainCode));
        return dictTypeMapper.selectList(wrapper).stream().map(this::toTypeVO).toList();
    }

    @Override
    public DictTypeVO getType(Long id) {
        return toTypeVO(requireType(id));
    }

    @Override
    public Long createType(SaveDictTypeCommand command) {
        Require.notNull(command, SystemCode.SYSTEM_INVALID);
        DictTypeEntity entity = new DictTypeEntity();
        copy(command, entity);
        entity.setStatus(command.getStatus() == null ? 1 : command.getStatus());
        dictTypeMapper.insert(entity);
        return entity.getId();
    }

    @Override
    public Boolean updateType(SaveDictTypeCommand command) {
        Require.notNull(command, SystemCode.SYSTEM_INVALID);
        Require.notNull(command.getId(), SystemCode.SYSTEM_INVALID, "ID不能为空");
        requireType(command.getId());
        DictTypeEntity entity = new DictTypeEntity();
        entity.setId(command.getId());
        copy(command, entity);
        return dictTypeMapper.updateById(entity) > 0;
    }

    @Override
    public Boolean deleteType(Long id) {
        DictTypeEntity type = requireType(id);
        long dataCount = dictDataMapper.selectCount(new LambdaQueryWrapper<DictDataEntity>()
                .eq(DictDataEntity::getDictType, type.getDictType()));
        Require.isTrue(dataCount == 0, SystemCode.DICT_TYPE_IN_USE);
        return dictTypeMapper.deleteById(id) > 0;
    }

    @Override
    public List<DictDataVO> listData(Long typeId) {
        LambdaQueryWrapper<DictDataEntity> wrapper = new LambdaQueryWrapper<>();
        if (typeId != null) {
            wrapper.eq(DictDataEntity::getDictType, requireType(typeId).getDictType());
        }
        wrapper.orderByAsc(DictDataEntity::getSort);
        return dictDataMapper.selectList(wrapper).stream().map(this::toDataVO).toList();
    }

    @Override
    public DictDataVO getData(Long id) {
        return toDataVO(requireData(id));
    }

    @Override
    public Long createData(SaveDictDataCommand command) {
        Require.notNull(command, SystemCode.SYSTEM_INVALID);
        DictDataEntity entity = new DictDataEntity();
        copy(command, entity);
        entity.setSort(command.getSort() == null ? 0 : command.getSort());
        entity.setStatus(command.getStatus() == null ? 1 : command.getStatus());
        dictDataMapper.insert(entity);
        return entity.getId();
    }

    @Override
    public Boolean updateData(SaveDictDataCommand command) {
        Require.notNull(command, SystemCode.SYSTEM_INVALID);
        Require.notNull(command.getId(), SystemCode.SYSTEM_INVALID, "ID不能为空");
        requireData(command.getId());
        DictDataEntity entity = new DictDataEntity();
        entity.setId(command.getId());
        copy(command, entity);
        return dictDataMapper.updateById(entity) > 0;
    }

    @Override
    public Boolean deleteData(Long id) {
        Require.notNull(id, SystemCode.SYSTEM_INVALID, "字典数据 ID 不能为空");
        requireData(id);
        return dictDataMapper.deleteById(id) > 0;
    }

    @Override
    public List<DictOptionVO> getOptions(String typeCode) {
        return dictDataMapper.selectList(new LambdaQueryWrapper<DictDataEntity>()
                        .eq(DictDataEntity::getDictType, typeCode)
                        .eq(DictDataEntity::getStatus, 1)
                        .orderByAsc(DictDataEntity::getSort))
                .stream().map(this::toOptionVO).toList();
    }

    private DictTypeEntity requireType(Long id) {
        DictTypeEntity entity = dictTypeMapper.selectById(id);
        Require.notNull(entity, SystemCode.DICT_TYPE_NOT_FOUND);
        return entity;
    }

    private DictDataEntity requireData(Long id) {
        DictDataEntity entity = dictDataMapper.selectById(id);
        Require.notNull(entity, SystemCode.DICT_DATA_NOT_FOUND);
        return entity;
    }

    private void copy(SaveDictTypeCommand command, DictTypeEntity entity) {
        entity.setDictType(command.getDictType());
        entity.setDictName(command.getDictName());
        entity.setDomainCode(StringUtils.hasText(command.getDomainCode()) ? command.getDomainCode().trim() : "COMMON");
        entity.setStatus(command.getStatus());
        entity.setRemark(command.getRemark());
    }

    private void copy(SaveDictDataCommand command, DictDataEntity entity) {
        entity.setDictType(command.getDictType());
        entity.setDictLabel(command.getDictLabel());
        entity.setDictValue(command.getDictValue());
        entity.setSort(command.getSort());
        entity.setStatus(command.getStatus());
        entity.setRemark(command.getRemark());
    }

    private DictTypeVO toTypeVO(DictTypeEntity entity) {
        DictTypeVO vo = new DictTypeVO();
        vo.setId(entity.getId());
        vo.setDictType(entity.getDictType());
        vo.setDictName(entity.getDictName());
        vo.setDomainCode(entity.getDomainCode());
        vo.setStatus(entity.getStatus());
        vo.setRemark(entity.getRemark());
        return vo;
    }

    private DictDataVO toDataVO(DictDataEntity entity) {
        DictDataVO vo = new DictDataVO();
        vo.setId(entity.getId());
        vo.setDictType(entity.getDictType());
        vo.setDictLabel(entity.getDictLabel());
        vo.setDictValue(entity.getDictValue());
        vo.setSort(entity.getSort());
        vo.setStatus(entity.getStatus());
        return vo;
    }

    private DictOptionVO toOptionVO(DictDataEntity entity) {
        DictOptionVO vo = new DictOptionVO();
        vo.setLabel(entity.getDictLabel());
        vo.setValue(entity.getDictValue());
        return vo;
    }

    private String trim(String value) {
        return value == null ? null : value.trim();
    }
}
