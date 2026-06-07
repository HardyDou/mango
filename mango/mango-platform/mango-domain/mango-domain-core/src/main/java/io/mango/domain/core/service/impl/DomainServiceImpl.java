package io.mango.domain.core.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import io.mango.common.result.R;
import io.mango.common.result.Require;
import io.mango.common.vo.PageResult;
import io.mango.domain.api.command.CreateDomainCommand;
import io.mango.domain.api.command.UpdateDomainCommand;
import io.mango.domain.api.command.UpdateDomainStatusCommand;
import io.mango.domain.api.query.DomainPageQuery;
import io.mango.domain.api.vo.DomainVO;
import io.mango.domain.core.entity.DomainEntity;
import io.mango.domain.core.mapper.DomainMapper;
import io.mango.domain.core.service.IDomainService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * 业务域服务实现。
 */
@Service
@RequiredArgsConstructor
public class DomainServiceImpl implements IDomainService {

    private static final long ROOT_PARENT_ID = 0L;
    private static final int STATUS_DISABLED = 0;
    private static final int STATUS_ENABLED = 1;

    private final DomainMapper domainMapper;

    @Override
    public R<PageResult<DomainVO>> page(DomainPageQuery query) {
        DomainPageQuery resolved = query == null ? new DomainPageQuery() : query;
        IPage<DomainEntity> page = domainMapper.selectPage(
                new Page<>(resolved.getPage(), resolved.getSize()),
                wrapper(resolved));
        Map<Long, DomainEntity> parentMap = parentMap(page.getRecords());
        return R.ok(PageResult.of(page.getRecords().stream().map(item -> toVO(item, parentMap)).toList(),
                page.getTotal(), page.getCurrent(), page.getSize()));
    }

    @Override
    public R<List<DomainVO>> tree(DomainPageQuery query) {
        DomainPageQuery resolved = query == null ? new DomainPageQuery() : query;
        List<DomainEntity> domains = domainMapper.selectList(wrapper(resolved));
        return R.ok(buildTree(domains));
    }

    @Override
    public R<List<DomainVO>> enabledTree() {
        DomainPageQuery query = new DomainPageQuery();
        query.setStatus(STATUS_ENABLED);
        return tree(query);
    }

    @Override
    public R<DomainVO> detail(Long id) {
        DomainEntity entity = selectDomain(id);
        return R.ok(toVO(entity, parentMap(List.of(entity))));
    }

    @Override
    public R<DomainVO> detailByCode(String domainCode) {
        Require.notBlank(domainCode, "业务域编码不能为空");
        DomainEntity entity = domainMapper.selectOne(new LambdaQueryWrapper<DomainEntity>()
                .eq(DomainEntity::getDomainCode, normalizeCode(domainCode))
                .last("LIMIT 1"));
        Require.notNull(entity, "业务域不存在");
        return R.ok(toVO(entity, parentMap(List.of(entity))));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public R<Long> create(CreateDomainCommand command) {
        validateCreate(command);
        DomainEntity parent = selectParent(command.getParentId());
        String fullCode = resolveFullCode(parent, command.getDomainCode());
        Require.isNull(selectByCode(fullCode), "业务域编码已存在");
        Require.isNull(selectByShortCode(command.getDomainShortCode()), "业务域简写已存在");
        DomainEntity entity = new DomainEntity();
        entity.setDomainCode(fullCode);
        entity.setParentId(parent == null ? ROOT_PARENT_ID : parent.getId());
        entity.setDomainShortCode(normalizeCode(command.getDomainShortCode()));
        entity.setDomainName(command.getDomainName().trim());
        entity.setSort(command.getSort() == null ? 0 : command.getSort());
        entity.setStatus(command.getStatus() == null ? STATUS_ENABLED : normalizeStatus(command.getStatus()));
        entity.setRemark(trimToEmpty(command.getRemark()));
        entity.setDeleted(0);
        LocalDateTime now = LocalDateTime.now();
        entity.setCreateTime(now);
        entity.setUpdateTime(now);
        domainMapper.insert(entity);
        return R.ok(entity.getId());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public R<Boolean> update(UpdateDomainCommand command) {
        validateUpdate(command);
        DomainEntity entity = selectDomain(command.getId());
        DomainEntity exists = selectByShortCode(command.getDomainShortCode());
        Require.isTrue(exists == null || exists.getId().equals(entity.getId()), "业务域简写已存在");
        entity.setDomainShortCode(normalizeCode(command.getDomainShortCode()));
        entity.setDomainName(command.getDomainName().trim());
        entity.setSort(command.getSort() == null ? 0 : command.getSort());
        if (command.getStatus() != null) {
            entity.setStatus(normalizeStatus(command.getStatus()));
        }
        entity.setRemark(trimToEmpty(command.getRemark()));
        entity.setUpdateTime(LocalDateTime.now());
        return R.ok(domainMapper.updateById(entity) > 0);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public R<Boolean> updateStatus(UpdateDomainStatusCommand command) {
        Require.notNull(command, "业务域状态命令不能为空");
        DomainEntity entity = selectDomain(command.getId());
        entity.setStatus(normalizeStatus(command.getStatus()));
        entity.setUpdateTime(LocalDateTime.now());
        return R.ok(domainMapper.updateById(entity) > 0);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public R<Boolean> delete(Long id) {
        DomainEntity entity = selectDomain(id);
        Long childCount = domainMapper.selectCount(new LambdaQueryWrapper<DomainEntity>()
                .eq(DomainEntity::getParentId, entity.getId()));
        Require.isTrue(childCount == 0, "存在子业务域，不能删除");
        return R.ok(domainMapper.deleteById(entity.getId()) > 0);
    }

    private void validateCreate(CreateDomainCommand command) {
        Require.notNull(command, "业务域新增命令不能为空");
        Require.notBlank(command.getDomainCode(), "业务域编码不能为空");
        Require.notBlank(command.getDomainShortCode(), "业务域简写不能为空");
        Require.notBlank(command.getDomainName(), "业务域名称不能为空");
    }

    private void validateUpdate(UpdateDomainCommand command) {
        Require.notNull(command, "业务域修改命令不能为空");
        Require.notNull(command.getId(), "业务域ID不能为空");
        Require.notBlank(command.getDomainShortCode(), "业务域简写不能为空");
        Require.notBlank(command.getDomainName(), "业务域名称不能为空");
    }

    private LambdaQueryWrapper<DomainEntity> wrapper(DomainPageQuery query) {
        LambdaQueryWrapper<DomainEntity> wrapper = new LambdaQueryWrapper<>();
        String domainCode = trimToNull(query.getDomainCode());
        String domainName = trimToNull(query.getDomainName());
        if (StringUtils.hasText(domainCode)) {
            wrapper.like(DomainEntity::getDomainCode, normalizeCode(domainCode));
        }
        if (StringUtils.hasText(domainName)) {
            wrapper.like(DomainEntity::getDomainName, domainName);
        }
        wrapper.eq(query.getStatus() != null, DomainEntity::getStatus, query.getStatus());
        wrapper.orderByAsc(DomainEntity::getParentId, DomainEntity::getSort, DomainEntity::getId);
        return wrapper;
    }

    private DomainEntity selectDomain(Long id) {
        Require.notNull(id, "业务域ID不能为空");
        DomainEntity entity = domainMapper.selectById(id);
        Require.notNull(entity, "业务域不存在");
        return entity;
    }

    private DomainEntity selectParent(Long parentId) {
        if (parentId == null || parentId == ROOT_PARENT_ID) {
            return null;
        }
        return selectDomain(parentId);
    }

    private DomainEntity selectByCode(String domainCode) {
        return domainMapper.selectOne(new LambdaQueryWrapper<DomainEntity>()
                .eq(DomainEntity::getDomainCode, normalizeCode(domainCode))
                .last("LIMIT 1"));
    }

    private DomainEntity selectByShortCode(String shortCode) {
        return domainMapper.selectOne(new LambdaQueryWrapper<DomainEntity>()
                .eq(DomainEntity::getDomainShortCode, normalizeCode(shortCode))
                .last("LIMIT 1"));
    }

    private String resolveFullCode(DomainEntity parent, String currentCode) {
        String normalized = normalizeCode(currentCode);
        if (parent == null) {
            return normalized;
        }
        String parentCode = parent.getDomainCode();
        String prefix = parentCode + "_";
        if (normalized.startsWith(prefix)) {
            return normalized;
        }
        return prefix + normalized;
    }

    private Map<Long, DomainEntity> parentMap(List<DomainEntity> domains) {
        List<Long> parentIds = domains.stream()
                .map(DomainEntity::getParentId)
                .filter(parentId -> parentId != null && parentId != ROOT_PARENT_ID)
                .distinct()
                .toList();
        if (parentIds.isEmpty()) {
            return Map.of();
        }
        return domainMapper.selectBatchIds(parentIds).stream()
                .collect(LinkedHashMap::new, (map, item) -> map.put(item.getId(), item), Map::putAll);
    }

    private List<DomainVO> buildTree(List<DomainEntity> domains) {
        Map<Long, DomainVO> nodeMap = new LinkedHashMap<>();
        domains.forEach(entity -> nodeMap.put(entity.getId(), toVO(entity, Map.of())));
        List<DomainVO> roots = new ArrayList<>();
        nodeMap.values().forEach(node -> {
            if (node.getParentId() == null || node.getParentId() == ROOT_PARENT_ID || !nodeMap.containsKey(node.getParentId())) {
                roots.add(node);
                return;
            }
            nodeMap.get(node.getParentId()).getChildren().add(node);
        });
        return roots;
    }

    private DomainVO toVO(DomainEntity entity, Map<Long, DomainEntity> parentMap) {
        DomainVO vo = new DomainVO();
        vo.setId(entity.getId());
        vo.setTenantId(entity.getTenantId());
        vo.setDomainCode(entity.getDomainCode());
        vo.setDomainShortCode(entity.getDomainShortCode());
        vo.setDomainName(entity.getDomainName());
        vo.setParentId(entity.getParentId());
        DomainEntity parent = parentMap.get(entity.getParentId());
        vo.setParentName(parent == null ? null : parent.getDomainName());
        vo.setSort(entity.getSort());
        vo.setStatus(entity.getStatus());
        vo.setRemark(entity.getRemark());
        vo.setCreateTime(entity.getCreateTime());
        vo.setUpdateTime(entity.getUpdateTime());
        vo.setCreatedBy(entity.getCreatedBy());
        vo.setCreatedAt(entity.getCreatedAt());
        vo.setUpdatedBy(entity.getUpdatedBy());
        vo.setUpdatedAt(entity.getUpdatedAt());
        return vo;
    }

    private Integer normalizeStatus(Integer status) {
        Require.notNull(status, "业务域状态不能为空");
        Require.isTrue(status == STATUS_DISABLED || status == STATUS_ENABLED, "业务域状态非法");
        return status;
    }

    private String normalizeCode(String value) {
        Require.notBlank(value, "编码不能为空");
        return value.trim().replace('-', '_').toUpperCase(Locale.ROOT);
    }

    private String trimToNull(String value) {
        if (!StringUtils.hasText(value)) {
            return null;
        }
        return value.trim();
    }

    private String trimToEmpty(String value) {
        String trimmed = trimToNull(value);
        return trimmed == null ? "" : trimmed;
    }
}
