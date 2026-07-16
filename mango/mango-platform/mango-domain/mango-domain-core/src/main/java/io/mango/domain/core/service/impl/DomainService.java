package io.mango.domain.core.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import io.mango.common.result.Require;
import io.mango.common.vo.PageResult;
import io.mango.domain.api.command.CreateDomainCommand;
import io.mango.domain.api.command.UpdateDomainCommand;
import io.mango.domain.api.command.UpdateDomainStatusCommand;
import io.mango.domain.api.enums.DomainCode;
import io.mango.domain.api.query.DomainPageQuery;
import io.mango.domain.api.vo.DomainVO;
import io.mango.domain.core.entity.DomainEntity;
import io.mango.domain.core.mapper.DomainMapper;
import io.mango.domain.core.service.IDomainService;
import io.mango.infra.persistence.api.crud.DeleteCommand;
import io.mango.infra.persistence.api.crud.MangoCrudServiceImpl;
import io.mango.infra.persistence.api.query.PersistencePageResult;
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
@SuppressWarnings("PMD.ServiceOrDaoClassShouldEndWithImplRule")
public class DomainService extends MangoCrudServiceImpl<DomainMapper, DomainEntity>
        implements IDomainService {

    private static final long ROOT_PARENT_ID = 0L;
    private static final int STATUS_DISABLED = 0;
    private static final int STATUS_ENABLED = 1;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long create(CreateDomainCommand command) {
        validateCreate(command);
        Object id = createByCommand(command);
        Require.isTrue(id instanceof Long, DomainCode.VALIDATION_ERROR, "业务域ID生成失败");
        return (Long) id;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean update(UpdateDomainCommand command) {
        Require.notNull(command, DomainCode.VALIDATION_ERROR, "业务域修改命令不能为空");
        validateUpdate(command);
        return updateByCommand(command);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean delete(DeleteCommand command) {
        Require.notNull(command, DomainCode.VALIDATION_ERROR, "业务域删除命令不能为空");
        Require.notNull(command.getId(), DomainCode.VALIDATION_ERROR, "业务域ID不能为空");
        return deleteById(command.getId());
    }

    @Override
    public PersistencePageResult<DomainVO> page(DomainPageQuery query) {
        DomainPageQuery resolved = resolveQuery(query);
        PersistencePageResult<?> source = pageByQuery(resolved);
        List<DomainVO> records = source.getRecords().stream().map(DomainVO.class::cast).toList();
        fillParentNames(records);
        return PersistencePageResult.of(records, source.getTotal(), source.getPage(), source.getSize());
    }

    @Override
    public PageResult<DomainVO> pageResult(DomainPageQuery query) {
        PersistencePageResult<DomainVO> result = page(query);
        return PageResult.of(result.getRecords(), result.getTotal(), result.getPage(), result.getSize());
    }

    @Override
    public DomainVO detail(Long id) {
        DomainEntity entity = selectDomain(id);
        DomainVO vo = toVO(entity);
        fillParentNames(List.of(vo));
        return vo;
    }

    @Override
    public List<DomainVO> tree(DomainPageQuery query) {
        DomainPageQuery resolved = resolveQuery(query);
        return buildTree(list(buildQueryWrapper(resolved)));
    }

    @Override
    public List<DomainVO> enabledTree() {
        DomainPageQuery query = new DomainPageQuery();
        query.setStatus(STATUS_ENABLED);
        return tree(query);
    }

    @Override
    public DomainVO detailByCode(String domainCode) {
        Require.notBlank(domainCode, DomainCode.VALIDATION_ERROR, "业务域编码不能为空");
        DomainEntity entity = lambdaQuery()
                .eq(DomainEntity::getDomainCode, normalizeCode(domainCode))
                .last("LIMIT 1")
                .one();
        Require.notNull(entity, DomainCode.NOT_FOUND, "业务域不存在");
        DomainVO vo = toVO(entity);
        fillParentNames(List.of(vo));
        return vo;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean updateStatus(UpdateDomainStatusCommand command) {
        Require.notNull(command, DomainCode.VALIDATION_ERROR, "业务域状态命令不能为空");
        DomainEntity entity = selectDomain(command.getId());
        entity.setStatus(normalizeStatus(command.getStatus()));
        entity.setUpdateTime(LocalDateTime.now());
        return updateById(entity);
    }

    @Override
    protected Class<DomainEntity> entityType() {
        return DomainEntity.class;
    }

    @Override
    protected QueryWrapper<DomainEntity> buildQueryWrapper(Object queryObject) {
        DomainPageQuery query = (DomainPageQuery) queryObject;
        QueryWrapper<DomainEntity> wrapper = new QueryWrapper<>();
        LambdaQueryWrapper<DomainEntity> lambda = wrapper.lambda();
        String domainCode = trimToNull(query.getDomainCode());
        String domainName = trimToNull(query.getDomainName());
        if (StringUtils.hasText(domainCode)) {
            lambda.like(DomainEntity::getDomainCode, normalizeCode(domainCode));
        }
        if (StringUtils.hasText(domainName)) {
            lambda.like(DomainEntity::getDomainName, domainName);
        }
        lambda.eq(query.getStatus() != null, DomainEntity::getStatus, query.getStatus());
        lambda.orderByAsc(DomainEntity::getParentId, DomainEntity::getSort, DomainEntity::getId);
        return wrapper;
    }

    @Override
    protected void beforeCreate(Object commandObject, DomainEntity entity) {
        CreateDomainCommand command = (CreateDomainCommand) commandObject;
        DomainEntity parent = selectParent(command.getParentId());
        String fullCode = resolveFullCode(parent, command.getDomainCode());
        Require.isTrue(selectByCode(fullCode) == null, DomainCode.CONFLICT, "业务域编码已存在");
        Require.isTrue(selectByShortCode(command.getDomainShortCode()) == null,
                DomainCode.CONFLICT, "业务域简写已存在");
        entity.setDomainCode(fullCode);
        if (parent == null) {
            entity.setParentId(ROOT_PARENT_ID);
        } else {
            entity.setParentId(parent.getId());
        }
        entity.setDomainShortCode(normalizeCode(command.getDomainShortCode()));
        entity.setDomainName(command.getDomainName().trim());
        entity.setSort(resolveSort(command.getSort()));
        entity.setStatus(resolveCreateStatus(command.getStatus()));
        entity.setRemark(trimToEmpty(command.getRemark()));
        entity.setDeleted(0);
        LocalDateTime now = LocalDateTime.now();
        entity.setCreateTime(now);
        entity.setUpdateTime(now);
    }

    @Override
    protected void beforeUpdate(Object commandObject, DomainEntity entity) {
        UpdateDomainCommand command = (UpdateDomainCommand) commandObject;
        DomainEntity current = selectDomain(command.getId());
        DomainEntity exists = selectByShortCode(command.getDomainShortCode());
        Require.isTrue(exists == null || exists.getId().equals(current.getId()),
                DomainCode.CONFLICT, "业务域简写已存在");
        entity.setDomainShortCode(normalizeCode(command.getDomainShortCode()));
        entity.setDomainName(command.getDomainName().trim());
        entity.setSort(resolveSort(command.getSort()));
        if (command.getStatus() != null) {
            entity.setStatus(normalizeStatus(command.getStatus()));
        }
        entity.setRemark(trimToEmpty(command.getRemark()));
        entity.setUpdateTime(LocalDateTime.now());
    }

    @Override
    protected void beforeDelete(Object id) {
        DomainEntity entity = selectDomain((Long) id);
        long childCount = lambdaQuery().eq(DomainEntity::getParentId, entity.getId()).count();
        Require.isTrue(childCount == 0, DomainCode.CONFLICT, "存在子业务域，不能删除");
    }

    @Override
    protected DomainVO toVO(DomainEntity entity) {
        if (entity == null) {
            return null;
        }
        DomainVO vo = new DomainVO();
        vo.setId(entity.getId());
        vo.setTenantId(entity.getTenantId());
        vo.setDomainCode(entity.getDomainCode());
        vo.setDomainShortCode(entity.getDomainShortCode());
        vo.setDomainName(entity.getDomainName());
        vo.setParentId(entity.getParentId());
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

    private void validateCreate(CreateDomainCommand command) {
        Require.notNull(command, DomainCode.VALIDATION_ERROR, "业务域新增命令不能为空");
        Require.notBlank(command.getDomainCode(), DomainCode.VALIDATION_ERROR, "业务域编码不能为空");
        Require.notBlank(command.getDomainShortCode(), DomainCode.VALIDATION_ERROR, "业务域简写不能为空");
        Require.notBlank(command.getDomainName(), DomainCode.VALIDATION_ERROR, "业务域名称不能为空");
    }

    private void validateUpdate(UpdateDomainCommand command) {
        Require.notNull(command.getId(), DomainCode.VALIDATION_ERROR, "业务域ID不能为空");
        Require.notBlank(command.getDomainShortCode(), DomainCode.VALIDATION_ERROR, "业务域简写不能为空");
        Require.notBlank(command.getDomainName(), DomainCode.VALIDATION_ERROR, "业务域名称不能为空");
    }

    private DomainEntity selectDomain(Long id) {
        Require.notNull(id, DomainCode.VALIDATION_ERROR, "业务域ID不能为空");
        DomainEntity entity = getById(id);
        Require.notNull(entity, DomainCode.NOT_FOUND, "业务域不存在");
        return entity;
    }

    private DomainEntity selectParent(Long parentId) {
        if (parentId == null || parentId == ROOT_PARENT_ID) {
            return null;
        }
        return selectDomain(parentId);
    }

    private DomainEntity selectByCode(String domainCode) {
        return lambdaQuery()
                .eq(DomainEntity::getDomainCode, normalizeCode(domainCode))
                .last("LIMIT 1")
                .one();
    }

    private DomainEntity selectByShortCode(String shortCode) {
        return lambdaQuery()
                .eq(DomainEntity::getDomainShortCode, normalizeCode(shortCode))
                .last("LIMIT 1")
                .one();
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

    private void fillParentNames(List<DomainVO> domains) {
        List<Long> parentIds = domains.stream()
                .map(DomainVO::getParentId)
                .filter(parentId -> parentId != null && parentId != ROOT_PARENT_ID)
                .distinct()
                .toList();
        if (parentIds.isEmpty()) {
            return;
        }
        Map<Long, String> parentNames = listByIds(parentIds).stream()
                .collect(LinkedHashMap::new,
                        (map, item) -> map.put(item.getId(), item.getDomainName()),
                        Map::putAll);
        domains.forEach(domain -> domain.setParentName(parentNames.get(domain.getParentId())));
    }

    private List<DomainVO> buildTree(List<DomainEntity> domains) {
        Map<Long, DomainVO> nodeMap = new LinkedHashMap<>();
        domains.forEach(entity -> nodeMap.put(entity.getId(), toVO(entity)));
        List<DomainVO> roots = new ArrayList<>();
        nodeMap.values().forEach(node -> {
            if (node.getParentId() == null
                    || node.getParentId() == ROOT_PARENT_ID
                    || !nodeMap.containsKey(node.getParentId())) {
                roots.add(node);
                return;
            }
            nodeMap.get(node.getParentId()).getChildren().add(node);
        });
        return roots;
    }

    private Integer normalizeStatus(Integer status) {
        Require.notNull(status, DomainCode.VALIDATION_ERROR, "业务域状态不能为空");
        Require.isTrue(status == STATUS_DISABLED || status == STATUS_ENABLED,
                DomainCode.VALIDATION_ERROR, "业务域状态非法");
        return status;
    }

    private String normalizeCode(String value) {
        Require.notBlank(value, DomainCode.VALIDATION_ERROR, "编码不能为空");
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
        if (trimmed == null) {
            return "";
        }
        return trimmed;
    }

    private DomainPageQuery resolveQuery(DomainPageQuery query) {
        if (query == null) {
            return new DomainPageQuery();
        }
        return query;
    }

    private Integer resolveSort(Integer sort) {
        if (sort == null) {
            return 0;
        }
        return sort;
    }

    private Integer resolveCreateStatus(Integer status) {
        if (status == null) {
            return STATUS_ENABLED;
        }
        return normalizeStatus(status);
    }
}
