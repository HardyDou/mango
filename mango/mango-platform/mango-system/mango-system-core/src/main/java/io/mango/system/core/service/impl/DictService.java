package io.mango.system.core.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import io.mango.common.result.Require;
import io.mango.system.api.enums.DictShapeEnum;
import io.mango.system.api.enums.SystemCode;
import io.mango.system.api.command.SaveDictDataCommand;
import io.mango.system.api.command.SaveDictTypeCommand;
import io.mango.system.api.vo.DictDataVO;
import io.mango.system.api.vo.DictOptionVO;
import io.mango.system.api.vo.DictTreeNodeVO;
import io.mango.system.api.vo.DictTypeVO;
import io.mango.system.core.entity.DictDataEntity;
import io.mango.system.core.entity.DictTypeEntity;
import io.mango.system.core.mapper.DictDataMapper;
import io.mango.system.core.mapper.DictTypeMapper;
import io.mango.system.core.service.IDictService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class DictService implements IDictService {

    private static final int MAX_GROUP_LENGTH = 32;
    private static final int MAX_GROUP_COUNT = 20;
    private static final int MAX_GROUPS_LENGTH = 255;
    private static final String GROUP_VALUE_PATTERN = "[A-Za-z0-9_-]+";

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
        entity.setStatus(defaultInteger(command.getStatus(), 1));
        dictTypeMapper.insert(entity);
        return entity.getId();
    }

    @Override
    public Boolean updateType(SaveDictTypeCommand command) {
        Require.notNull(command, SystemCode.SYSTEM_INVALID);
        Require.notNull(command.getId(), SystemCode.SYSTEM_INVALID, "ID不能为空");
        DictTypeEntity current = requireType(command.getId());
        String shape = normalizeShape(command.getShape(), current.getShape());
        if (!shape.equals(normalizeShape(current.getShape(), null))) {
            long dataCount = dictDataMapper.selectCount(new LambdaQueryWrapper<DictDataEntity>()
                    .eq(DictDataEntity::getDictType, current.getDictType()));
            Require.isTrue(dataCount == 0, SystemCode.SYSTEM_INVALID, "字典已有数据时不能修改字典形态");
        }
        DictTypeEntity entity = new DictTypeEntity();
        entity.setId(command.getId());
        entity.setShape(shape);
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
        DictTypeEntity type = requireTypeByCode(command.getDictType());
        validateData(type, null, command.getDictValue(), command.getParentValue());
        DictDataEntity entity = new DictDataEntity();
        copy(command, entity);
        entity.setSort(defaultInteger(command.getSort(), 0));
        entity.setStatus(defaultInteger(command.getStatus(), 1));
        dictDataMapper.insert(entity);
        return entity.getId();
    }

    @Override
    public Boolean updateData(SaveDictDataCommand command) {
        Require.notNull(command, SystemCode.SYSTEM_INVALID);
        Require.notNull(command.getId(), SystemCode.SYSTEM_INVALID, "ID不能为空");
        DictDataEntity current = requireData(command.getId());
        DictTypeEntity type = requireTypeByCode(command.getDictType());
        if (!current.getDictValue().equals(command.getDictValue())) {
            Require.isTrue(!hasChildren(current.getDictType(), current.getDictValue()),
                    SystemCode.SYSTEM_INVALID, "存在子节点时不能修改字典值");
        }
        validateData(type, command.getId(), command.getDictValue(), command.getParentValue());
        DictDataEntity entity = new DictDataEntity();
        entity.setId(command.getId());
        copy(command, entity);
        return dictDataMapper.updateById(entity) > 0;
    }

    @Override
    public Boolean deleteData(Long id) {
        Require.notNull(id, SystemCode.SYSTEM_INVALID, "字典数据 ID 不能为空");
        DictDataEntity data = requireData(id);
        Require.isTrue(!hasChildren(data.getDictType(), data.getDictValue()),
                SystemCode.SYSTEM_INVALID, "请先删除该节点下的子节点");
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

    @Override
    public List<DictTreeNodeVO> getTreeOptions(String typeCode) {
        DictTypeEntity type = requireTypeByCode(typeCode);
        Require.isTrue(DictShapeEnum.TREE.getCode().equals(normalizeShape(type.getShape(), null)),
                SystemCode.SYSTEM_INVALID, "当前字典不是 TREE 形态");
        List<DictDataEntity> data = dictDataMapper.selectList(new LambdaQueryWrapper<DictDataEntity>()
                .eq(DictDataEntity::getDictType, typeCode)
                .eq(DictDataEntity::getStatus, 1)
                .orderByAsc(DictDataEntity::getSort));
        return buildTree(data);
    }

    private DictTypeEntity requireType(Long id) {
        DictTypeEntity entity = dictTypeMapper.selectById(id);
        Require.notNull(entity, SystemCode.DICT_TYPE_NOT_FOUND);
        return entity;
    }

    private DictTypeEntity requireTypeByCode(String typeCode) {
        DictTypeEntity entity = dictTypeMapper.selectOne(new LambdaQueryWrapper<DictTypeEntity>()
                .eq(DictTypeEntity::getDictType, typeCode)
                .last("limit 1"));
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
        entity.setShape(normalizeShape(command.getShape(),
                entity.getShape() == null ? DictShapeEnum.FLAT.getCode() : entity.getShape()));
        String domainCode = "COMMON";
        if (StringUtils.hasText(command.getDomainCode())) {
            domainCode = command.getDomainCode().trim();
        }
        entity.setDomainCode(domainCode);
        entity.setStatus(command.getStatus());
        entity.setRemark(command.getRemark());
    }

    private void copy(SaveDictDataCommand command, DictDataEntity entity) {
        entity.setDictType(command.getDictType());
        entity.setDictLabel(command.getDictLabel());
        entity.setDictValue(command.getDictValue());
        entity.setParentValue(trim(command.getParentValue()));
        entity.setDictGroups(normalizeGroups(command.getGroups()));
        entity.setSort(command.getSort());
        entity.setStatus(command.getStatus());
        entity.setRemark(command.getRemark());
    }

    private DictTypeVO toTypeVO(DictTypeEntity entity) {
        DictTypeVO vo = new DictTypeVO();
        vo.setId(entity.getId());
        vo.setDictType(entity.getDictType());
        vo.setDictName(entity.getDictName());
        vo.setShape(normalizeShape(entity.getShape(), null));
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
        vo.setParentValue(entity.getParentValue());
        vo.setGroups(splitGroups(entity.getDictGroups()));
        vo.setSort(entity.getSort());
        vo.setStatus(entity.getStatus());
        vo.setRemark(entity.getRemark());
        return vo;
    }

    private DictOptionVO toOptionVO(DictDataEntity entity) {
        DictOptionVO vo = new DictOptionVO();
        vo.setLabel(entity.getDictLabel());
        vo.setValue(entity.getDictValue());
        vo.setGroups(splitGroups(entity.getDictGroups()));
        return vo;
    }

    private List<DictTreeNodeVO> buildTree(List<DictDataEntity> data) {
        Map<String, DictTreeNodeVO> nodes = new HashMap<>();
        for (DictDataEntity entity : data) {
            nodes.put(entity.getDictValue(), toTreeNode(entity));
        }
        List<DictTreeNodeVO> roots = new ArrayList<>();
        for (DictDataEntity entity : data) {
            DictTreeNodeVO node = nodes.get(entity.getDictValue());
            if (!StringUtils.hasText(entity.getParentValue())) {
                roots.add(node);
            } else {
                DictTreeNodeVO parent = nodes.get(entity.getParentValue());
                if (parent != null) {
                    parent.addChild(node);
                }
            }
        }
        return roots;
    }

    private DictTreeNodeVO toTreeNode(DictDataEntity entity) {
        DictTreeNodeVO vo = new DictTreeNodeVO();
        vo.setId(entity.getId());
        vo.setDictType(entity.getDictType());
        vo.setDictLabel(entity.getDictLabel());
        vo.setDictValue(entity.getDictValue());
        vo.setParentValue(entity.getParentValue());
        vo.setGroups(splitGroups(entity.getDictGroups()));
        vo.setSort(entity.getSort());
        vo.setStatus(entity.getStatus());
        vo.setRemark(entity.getRemark());
        return vo;
    }

    private void validateData(DictTypeEntity type, Long id, String value, String parentValue) {
        String normalizedValue = trim(value);
        String normalizedParent = trim(parentValue);
        Require.isTrue(StringUtils.hasText(normalizedValue), SystemCode.SYSTEM_INVALID, "字典值不能为空");
        LambdaQueryWrapper<DictDataEntity> duplicate = new LambdaQueryWrapper<DictDataEntity>()
                .eq(DictDataEntity::getDictType, type.getDictType())
                .eq(DictDataEntity::getDictValue, normalizedValue);
        if (id != null) {
            duplicate.ne(DictDataEntity::getId, id);
        }
        Require.isTrue(dictDataMapper.selectCount(duplicate) == 0, SystemCode.SYSTEM_INVALID, "同一字典内的 value 不能重复");

        DictShapeEnum shape = DictShapeEnum.fromCode(normalizeShape(type.getShape(), null));
        if (shape == DictShapeEnum.FLAT) {
            Require.isTrue(!StringUtils.hasText(normalizedParent), SystemCode.SYSTEM_INVALID,
                    "FLAT 字典不能设置父节点");
            return;
        }
        if (!StringUtils.hasText(normalizedParent)) {
            return;
        }
        Require.isTrue(!normalizedValue.equals(normalizedParent), SystemCode.SYSTEM_INVALID, "字典节点不能指向自身");
        Set<String> visited = new HashSet<>();
        String cursor = normalizedParent;
        while (StringUtils.hasText(cursor)) {
            Require.isTrue(visited.add(cursor), SystemCode.SYSTEM_INVALID, "字典节点存在循环引用");
            Require.isTrue(!normalizedValue.equals(cursor), SystemCode.SYSTEM_INVALID, "字典节点存在循环引用");
            DictDataEntity parent = findDataByValue(type.getDictType(), cursor);
            Require.notNull(parent, SystemCode.SYSTEM_INVALID, "父节点不存在");
            cursor = trim(parent.getParentValue());
        }
    }

    private DictDataEntity findDataByValue(String dictType, String value) {
        return dictDataMapper.selectOne(new LambdaQueryWrapper<DictDataEntity>()
                .eq(DictDataEntity::getDictType, dictType)
                .eq(DictDataEntity::getDictValue, value)
                .last("limit 1"));
    }

    private boolean hasChildren(String dictType, String value) {
        return dictDataMapper.selectCount(new LambdaQueryWrapper<DictDataEntity>()
                .eq(DictDataEntity::getDictType, dictType)
                .eq(DictDataEntity::getParentValue, value)) > 0;
    }

    /**
     * 归一化分组标记：去空白、去重并保持声明顺序；空集合写入 null，保持旧数据语义不变。
     */
    private String normalizeGroups(List<String> groups) {
        if (groups == null || groups.isEmpty()) {
            return null;
        }
        Set<String> normalized = new java.util.LinkedHashSet<>();
        for (String group : groups) {
            String value = trim(group);
            if (value == null) {
                continue;
            }
            Require.isTrue(value.length() <= MAX_GROUP_LENGTH, SystemCode.SYSTEM_INVALID, "分组标记长度不能超过32");
            Require.isTrue(value.matches(GROUP_VALUE_PATTERN), SystemCode.SYSTEM_INVALID,
                    "分组标记只能包含字母、数字、下划线和短横线");
            normalized.add(value);
        }
        if (normalized.isEmpty()) {
            return null;
        }
        Require.isTrue(normalized.size() <= MAX_GROUP_COUNT, SystemCode.SYSTEM_INVALID, "分组数量不能超过20");
        String joined = String.join(",", normalized);
        Require.isTrue(joined.length() <= MAX_GROUPS_LENGTH, SystemCode.SYSTEM_INVALID, "分组标记总长度不能超过255");
        return joined;
    }

    private List<String> splitGroups(String groups) {
        String normalized = trim(groups);
        if (normalized == null) {
            return List.of();
        }
        return List.of(normalized.split(","));
    }

    private String normalizeShape(String shape, String defaultShape) {
        String value = StringUtils.hasText(shape) ? shape : defaultShape;
        DictShapeEnum normalized = DictShapeEnum.fromCode(value);
        Require.notNull(normalized, SystemCode.SYSTEM_INVALID, "字典形态只能是 FLAT 或 TREE");
        return normalized.getCode();
    }

    private String trim(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private Integer defaultInteger(Integer value, int defaultValue) {
        if (value == null) {
            return Integer.valueOf(defaultValue);
        }
        return value;
    }
}
