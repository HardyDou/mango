package io.mango.area.core.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import io.mango.area.api.entity.SysArea;
import io.mango.area.core.service.ISysAreaService;
import io.mango.area.core.mapper.SysAreaMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Area service implementation with lazy loading tree support
 *
 * @author Mango
 */
@Service
@RequiredArgsConstructor
public class SysAreaServiceImpl implements ISysAreaService {

    private final SysAreaMapper areaMapper;

    /**
     * Maximum tree depth levels
     * 1=省, 2=市, 3=区/县, 4=街道
     */
    private static final int MAX_LEVEL = 4;

    @Override
    public List<Map<String, Object>> tree(Integer type) {
        // Default to root (parentId=0) with max level
        return treeByParentId(0L, type, MAX_LEVEL);
    }

    /**
     * Get tree by parent ID with lazy loading
     *
     * @param parentId parent ID (0 for root)
     * @param type     area type filter (1-4), null for all
     * @param maxLevel maximum depth to load
     * @return tree structure
     */
    private List<Map<String, Object>> treeByParentId(Long parentId, Integer type, int maxLevel) {
        if (maxLevel <= 0) {
            return new ArrayList<>();
        }

        LambdaQueryWrapper<SysArea> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(SysArea::getPid, parentId)
                .eq(SysArea::getAreaStatus, "1")
                .orderByAsc(SysArea::getAreaSort)
                .orderByAsc(SysArea::getId);

        if (type != null) {
            wrapper.le(SysArea::getAreaType, type.toString());
        }

        List<SysArea> children = areaMapper.selectList(wrapper);

        return children.stream().map(area -> {
            Map<String, Object> node = new HashMap<>();
            node.put("id", area.getId());
            node.put("pid", area.getPid());
            node.put("adcode", area.getAdcode());
            node.put("name", area.getName());
            node.put("level", area.getAreaType());
            node.put("hot", "1".equals(area.getHot()));

            // Lazy load children if within max level
            if (maxLevel > 1) {
                int childLevel = maxLevel - 1;
                // Only load children if we haven't exceeded the type filter
                if (type == null || Integer.parseInt(area.getAreaType()) < type) {
                    List<Map<String, Object>> childNodes = treeByParentId(area.getId(), type, childLevel);
                    node.put("children", childNodes);
                    node.put("leaf", childNodes.isEmpty());
                } else {
                    node.put("children", new ArrayList<>());
                    node.put("leaf", true);
                }
            } else {
                node.put("children", new ArrayList<>());
                node.put("leaf", true);
            }

            return node;
        }).collect(Collectors.toList());
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
