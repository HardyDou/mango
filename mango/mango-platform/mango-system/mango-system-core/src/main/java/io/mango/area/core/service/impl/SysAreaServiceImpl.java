package io.mango.area.core.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import io.mango.area.api.entity.SysArea;
import io.mango.area.core.service.ISysAreaService;
import io.mango.area.core.mapper.SysAreaMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * Area service implementation.
 *
 * @author Mango
 */
@Service
@RequiredArgsConstructor
public class SysAreaServiceImpl implements ISysAreaService {

    private final SysAreaMapper areaMapper;

    private static final int DEFAULT_TREE_LEVEL = 1;
    private static final int MAX_TREE_LEVEL = 4;

    @Override
    public List<Map<String, Object>> tree(Integer type) {
        int maxLevel = normalizeTreeLevel(type);
        LambdaQueryWrapper<SysArea> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(SysArea::getAreaStatus, "1")
                .le(SysArea::getAreaType, String.valueOf(maxLevel))
                .orderByAsc(SysArea::getAreaSort)
                .orderByAsc(SysArea::getId);

        List<SysArea> areas = areaMapper.selectList(wrapper);
        Map<Long, List<SysArea>> childrenByParentId = areas.stream()
                .sorted(Comparator.comparing(SysArea::getAreaSort, Comparator.nullsLast(Integer::compareTo))
                        .thenComparing(SysArea::getId, Comparator.nullsLast(Long::compareTo)))
                .collect(Collectors.groupingBy(area -> area.getPid() == null ? 0L : area.getPid()));

        return childrenByParentId.getOrDefault(0L, new ArrayList<>())
                .stream()
                .map(area -> buildTreeNode(area, childrenByParentId))
                .collect(Collectors.toList());
    }

    private int normalizeTreeLevel(Integer type) {
        if (type == null) {
            return DEFAULT_TREE_LEVEL;
        }
        return Math.max(DEFAULT_TREE_LEVEL, Math.min(type, MAX_TREE_LEVEL));
    }

    private Map<String, Object> buildTreeNode(SysArea area, Map<Long, List<SysArea>> childrenByParentId) {
        List<Map<String, Object>> children = childrenByParentId.getOrDefault(area.getId(), new ArrayList<>())
                .stream()
                .map(child -> buildTreeNode(child, childrenByParentId))
                .collect(Collectors.toList());
        Map<String, Object> node = new HashMap<>();
        node.put("id", area.getId());
        node.put("pid", area.getPid());
        node.put("parentId", area.getPid());
        node.put("adcode", area.getAdcode());
        node.put("name", area.getName());
        node.put("level", parseAreaType(area.getAreaType()));
        node.put("hot", area.getHot());
        node.put("children", children);
        node.put("leaf", children.isEmpty());
        return node;
    }

    private Integer parseAreaType(String areaType) {
        if (Objects.isNull(areaType) || areaType.isBlank()) {
            return null;
        }
        try {
            return Integer.parseInt(areaType);
        } catch (NumberFormatException e) {
            return null;
        }
    }

    @Override
    public List<SysArea> listByPid(Long parentId) {
        LambdaQueryWrapper<SysArea> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(SysArea::getPid, parentId)
                .eq(SysArea::getAreaStatus, "1")
                .orderByAsc(SysArea::getAreaSort)
                .orderByAsc(SysArea::getId);
        return areaMapper.selectList(wrapper);
    }

    @Override
    public SysArea getById(Long id) {
        return areaMapper.selectById(id);
    }

    @Override
    public SysArea getByAdcode(Long adcode) {
        LambdaQueryWrapper<SysArea> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(SysArea::getAdcode, adcode);
        return areaMapper.selectOne(wrapper);
    }

    @Override
    public boolean save(SysArea area) {
        return areaMapper.insert(area) > 0;
    }

    @Override
    public boolean update(SysArea area) {
        // Only allow updating custom area types (area_type >= 5)
        SysArea existing = areaMapper.selectById(area.getId());
        if (existing != null) {
            int existingType = Integer.parseInt(existing.getAreaType());
            if (existingType < 5) {
                // Standard administrative area - check if adcode is being changed
                if (!existing.getAdcode().equals(area.getAdcode())) {
                    throw new UnsupportedOperationException("Standard administrative area adcode cannot be modified");
                }
            }
        }
        return areaMapper.updateById(area) > 0;
    }

    @Override
    public boolean delete(Long id) {
        // Only allow deleting custom area types (area_type >= 5)
        SysArea existing = areaMapper.selectById(id);
        if (existing != null) {
            int existingType = Integer.parseInt(existing.getAreaType());
            if (existingType < 5) {
                throw new UnsupportedOperationException("Standard administrative area cannot be deleted");
            }
        }
        return areaMapper.deleteById(id) > 0;
    }

    @Override
    public List<SysArea> listActive() {
        LambdaQueryWrapper<SysArea> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(SysArea::getAreaStatus, "1")
                .orderByAsc(SysArea::getAreaSort);
        return areaMapper.selectList(wrapper);
    }
}
