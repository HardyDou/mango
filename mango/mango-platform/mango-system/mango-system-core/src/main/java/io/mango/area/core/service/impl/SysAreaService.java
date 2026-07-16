package io.mango.area.core.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import io.mango.area.api.command.SaveAreaCommand;
import io.mango.area.api.vo.SysAreaTreeNodeVO;
import io.mango.area.api.vo.SysAreaVO;
import io.mango.area.core.entity.SysAreaEntity;
import io.mango.area.core.mapper.SysAreaMapper;
import io.mango.area.core.service.ISysAreaService;
import io.mango.common.result.Require;
import io.mango.area.api.enums.AreaCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class SysAreaService implements ISysAreaService {

    private static final int DEFAULT_TREE_LEVEL = 1;
    private static final int MAX_TREE_LEVEL = 4;

    private final SysAreaMapper areaMapper;

    @Override
    public List<SysAreaTreeNodeVO> tree(Integer type) {
        int maxLevel = normalizeTreeLevel(type);
        List<SysAreaEntity> areas = areaMapper.selectList(new LambdaQueryWrapper<SysAreaEntity>()
                .eq(SysAreaEntity::getAreaStatus, "1")
                .le(SysAreaEntity::getAreaType, String.valueOf(maxLevel))
                .orderByAsc(SysAreaEntity::getAreaSort)
                .orderByAsc(SysAreaEntity::getId));
        Map<Long, List<SysAreaEntity>> childrenByParentId = areas.stream()
                .sorted(Comparator.comparing(SysAreaEntity::getAreaSort, Comparator.nullsLast(Integer::compareTo))
                        .thenComparing(SysAreaEntity::getId, Comparator.nullsLast(Long::compareTo)))
                .collect(Collectors.groupingBy(area -> area.getPid() == null ? 0L : area.getPid()));
        return childrenByParentId.getOrDefault(0L, List.of()).stream()
                .map(area -> toTreeNode(area, childrenByParentId)).toList();
    }

    @Override
    public List<SysAreaVO> listByPid(Long parentId) {
        return areaMapper.selectList(new LambdaQueryWrapper<SysAreaEntity>()
                        .eq(SysAreaEntity::getPid, parentId)
                        .eq(SysAreaEntity::getAreaStatus, "1")
                        .orderByAsc(SysAreaEntity::getAreaSort)
                        .orderByAsc(SysAreaEntity::getId))
                .stream().map(this::toVO).toList();
    }

    @Override
    public SysAreaVO getById(Long id) {
        return toVO(requireArea(id));
    }

    @Override
    public SysAreaVO getByAdcode(Long adcode) {
        SysAreaEntity entity = areaMapper.selectOne(new LambdaQueryWrapper<SysAreaEntity>()
                .eq(SysAreaEntity::getAdcode, adcode));
        Require.notNull(entity, AreaCode.AREA_NOT_FOUND);
        return toVO(entity);
    }

    @Override
    public Void create(SaveAreaCommand command) {
        Require.notNull(command, AreaCode.AREA_INVALID);
        SysAreaEntity entity = new SysAreaEntity();
        copy(command, entity);
        Require.isTrue(areaMapper.insert(entity) > 0, AreaCode.AREA_INVALID, "Failed to create area");
        return null;
    }

    @Override
    public Void update(SaveAreaCommand command) {
        Require.notNull(command, AreaCode.AREA_INVALID);
        Require.notNull(command.getId(), AreaCode.AREA_INVALID, "行政区划 ID 不能为空");
        SysAreaEntity existing = requireArea(command.getId());
        if (parseAreaType(existing.getAreaType()) < 5) {
            Require.isTrue(Objects.equals(existing.getAdcode(), command.getAdcode()),
                    AreaCode.AREA_PROTECTED, "Standard administrative area adcode cannot be modified");
        }
        SysAreaEntity entity = new SysAreaEntity();
        entity.setId(command.getId());
        copy(command, entity);
        Require.isTrue(areaMapper.updateById(entity) > 0, AreaCode.AREA_INVALID, "Failed to update area");
        return null;
    }

    @Override
    public Void delete(Long id) {
        SysAreaEntity existing = requireArea(id);
        Require.isTrue(parseAreaType(existing.getAreaType()) >= 5,
                AreaCode.AREA_PROTECTED, "Standard administrative area cannot be deleted");
        Require.isTrue(areaMapper.deleteById(id) > 0, AreaCode.AREA_INVALID, "Failed to delete area");
        return null;
    }

    @Override
    public List<SysAreaVO> listActive() {
        return areaMapper.selectList(new LambdaQueryWrapper<SysAreaEntity>()
                        .eq(SysAreaEntity::getAreaStatus, "1")
                        .orderByAsc(SysAreaEntity::getAreaSort))
                .stream().map(this::toVO).toList();
    }

    private SysAreaEntity requireArea(Long id) {
        SysAreaEntity entity = areaMapper.selectById(id);
        Require.notNull(entity, AreaCode.AREA_NOT_FOUND);
        return entity;
    }

    private int normalizeTreeLevel(Integer type) {
        if (type == null) {
            return DEFAULT_TREE_LEVEL;
        }
        return Math.max(DEFAULT_TREE_LEVEL, Math.min(type, MAX_TREE_LEVEL));
    }

    private SysAreaTreeNodeVO toTreeNode(SysAreaEntity entity, Map<Long, List<SysAreaEntity>> childrenByParentId) {
        List<SysAreaTreeNodeVO> children = childrenByParentId.getOrDefault(entity.getId(), List.of()).stream()
                .map(child -> toTreeNode(child, childrenByParentId)).toList();
        SysAreaTreeNodeVO node = new SysAreaTreeNodeVO();
        node.setId(entity.getId());
        node.setPid(entity.getPid());
        node.setParentId(entity.getPid());
        node.setAdcode(entity.getAdcode());
        node.setName(entity.getName());
        node.setLevel(parseAreaType(entity.getAreaType()));
        node.setHot(entity.getHot());
        node.setChildren(children);
        node.setLeaf(children.isEmpty());
        return node;
    }

    private int parseAreaType(String areaType) {
        if (areaType == null || areaType.isBlank()) {
            return 0;
        }
        try {
            return Integer.parseInt(areaType);
        } catch (NumberFormatException exception) {
            return Require.fail(AreaCode.AREA_INVALID, "Invalid area type: " + areaType, exception);
        }
    }

    private void copy(SaveAreaCommand command, SysAreaEntity entity) {
        entity.setPid(command.getPid());
        entity.setName(command.getName());
        entity.setLetter(command.getLetter());
        entity.setAdcode(command.getAdcode());
        entity.setLocation(command.getLocation());
        entity.setAreaSort(command.getAreaSort() == null ? 0 : command.getAreaSort());
        entity.setAreaStatus(command.getAreaStatus());
        entity.setAreaType(command.getAreaType());
        entity.setHot(command.getHot() == null ? "0" : command.getHot());
        entity.setCityCode(command.getCityCode());
        if (command.getTenantId() != null) {
            entity.setTenantId(String.valueOf(command.getTenantId()));
        }
    }

    private SysAreaVO toVO(SysAreaEntity entity) {
        SysAreaVO vo = new SysAreaVO();
        vo.setId(entity.getId());
        vo.setPid(entity.getPid());
        vo.setName(entity.getName());
        vo.setLetter(entity.getLetter());
        vo.setAdcode(entity.getAdcode());
        vo.setLocation(entity.getLocation());
        vo.setAreaSort(entity.getAreaSort());
        vo.setAreaStatus(entity.getAreaStatus());
        vo.setAreaType(entity.getAreaType());
        vo.setHot(entity.getHot());
        vo.setCityCode(entity.getCityCode());
        if (entity.getTenantId() != null && entity.getTenantId().matches("\\d+")) {
            vo.setTenantId(Long.valueOf(entity.getTenantId()));
        }
        return vo;
    }
}
