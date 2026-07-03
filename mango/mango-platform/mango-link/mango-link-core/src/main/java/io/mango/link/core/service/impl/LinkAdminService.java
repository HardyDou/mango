package io.mango.link.core.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import io.mango.common.result.Require;
import io.mango.common.vo.PageResult;
import io.mango.identity.api.TenantMemberProvider;
import io.mango.link.api.command.CreateLinkCategoryCommand;
import io.mango.link.api.command.CreateLinkItemCommand;
import io.mango.link.api.command.LinkVisibilityTargetCommand;
import io.mango.link.api.command.UpdateLinkCategoryCommand;
import io.mango.link.api.command.UpdateLinkCategoryStatusCommand;
import io.mango.link.api.command.UpdateLinkItemCommand;
import io.mango.link.api.command.UpdateLinkItemStatusCommand;
import io.mango.link.api.enums.LinkStatus;
import io.mango.link.api.enums.LinkCategoryScope;
import io.mango.link.api.enums.LinkVisibilityScope;
import io.mango.link.api.enums.LinkVisibilityTargetType;
import io.mango.link.api.query.LinkCategoryPageQuery;
import io.mango.link.api.query.LinkCategoryQuery;
import io.mango.link.api.query.LinkItemPageQuery;
import io.mango.link.api.vo.LinkCategoryVO;
import io.mango.link.api.vo.LinkItemVO;
import io.mango.link.core.entity.LinkCategoryEntity;
import io.mango.link.core.entity.LinkFavoriteEntity;
import io.mango.link.core.entity.LinkItemEntity;
import io.mango.link.core.entity.LinkVisibilityTargetEntity;
import io.mango.link.core.mapper.LinkCategoryMapper;
import io.mango.link.core.mapper.LinkFavoriteMapper;
import io.mango.link.core.mapper.LinkItemMapper;
import io.mango.link.core.mapper.LinkVisibilityTargetMapper;
import io.mango.link.core.service.ILinkAdminService;
import io.mango.link.core.support.LinkContextSupport;
import io.mango.link.core.support.LinkSupport;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Map;

@Service
public class LinkAdminService extends LinkBaseService implements ILinkAdminService {

    public LinkAdminService(LinkCategoryMapper categoryMapper,
                            LinkItemMapper itemMapper,
                            LinkVisibilityTargetMapper targetMapper,
                            LinkFavoriteMapper favoriteMapper,
                            ObjectProvider<TenantMemberProvider> tenantMemberProvider) {
        super(categoryMapper, itemMapper, targetMapper, favoriteMapper, tenantMemberProvider);
    }

    @Override
    public PageResult<LinkCategoryVO> pageCategories(LinkCategoryPageQuery query) {
        LinkCategoryPageQuery resolved = query == null ? new LinkCategoryPageQuery() : query;
        IPage<LinkCategoryEntity> page = categoryMapper.selectPage(new Page<>(resolved.getPage(), resolved.getSize()),
                categoryWrapper(LinkContextSupport.currentTenantId(), resolved.getKeyword(), resolved.getStatus()));
        return PageResult.of(page.getRecords().stream().map(this::toCategoryVO).toList(),
                page.getTotal(), page.getCurrent(), page.getSize());
    }

    @Override
    public List<LinkCategoryVO> listCategories(LinkCategoryQuery query) {
        LinkCategoryQuery resolved = query == null ? new LinkCategoryQuery() : query;
        LambdaQueryWrapper<LinkCategoryEntity> wrapper = categoryWrapper(LinkContextSupport.currentTenantId(),
                resolved.getKeyword(), null);
        if (!Boolean.TRUE.equals(resolved.getIncludeDisabled())) {
            wrapper.eq(LinkCategoryEntity::getStatus, LinkSupport.enabled());
        }
        return categoryMapper.selectList(wrapper).stream().map(this::toCategoryVO).toList();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long createCategory(CreateLinkCategoryCommand command) {
        Long tenantId = LinkContextSupport.currentTenantId();
        String name = LinkContextSupport.trimRequired(command.getName(), "分类名称不能为空");
        Require.isNull(categoryMapper.selectByScopeOwnerAndName(tenantId, LinkSupport.companyCategory(), 0L, name),
                "分类名称已存在");
        LocalDateTime now = LocalDateTime.now();
        LinkCategoryEntity entity = new LinkCategoryEntity();
        entity.setTenantId(tenantId);
        entity.setScope(LinkSupport.companyCategory());
        entity.setOwnerUserId(0L);
        entity.setName(name);
        entity.setSortNo(command.getSortNo() == null ? 0 : command.getSortNo());
        entity.setStatus(LinkSupport.enabled());
        entity.setRemark(LinkContextSupport.trimToNull(command.getRemark()));
        entity.setCreatedBy(LinkContextSupport.currentUserIdOrNull());
        entity.setUpdatedBy(LinkContextSupport.currentUserIdOrNull());
        entity.setCreatedAt(now);
        entity.setUpdatedAt(now);
        categoryMapper.insert(entity);
        return entity.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean updateCategory(UpdateLinkCategoryCommand command) {
        Long tenantId = LinkContextSupport.currentTenantId();
        LinkCategoryEntity entity = selectCategoryRequired(tenantId, command.getId());
        String name = LinkContextSupport.trimRequired(command.getName(), "分类名称不能为空");
        LinkCategoryEntity exists = categoryMapper.selectByScopeOwnerAndName(tenantId,
                entity.getScope(), entity.getOwnerUserId(), name);
        Require.isTrue(exists == null || exists.getId().equals(entity.getId()), "分类名称已存在");
        entity.setName(name);
        entity.setSortNo(command.getSortNo() == null ? 0 : command.getSortNo());
        entity.setRemark(LinkContextSupport.trimToNull(command.getRemark()));
        entity.setUpdatedBy(LinkContextSupport.currentUserIdOrNull());
        entity.setUpdatedAt(LocalDateTime.now());
        return categoryMapper.updateById(entity) > 0;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean updateCategoryStatus(UpdateLinkCategoryStatusCommand command) {
        LinkCategoryEntity entity = selectCategoryRequired(LinkContextSupport.currentTenantId(), command.getId());
        entity.setStatus(LinkSupport.status(command.getStatus()));
        entity.setUpdatedBy(LinkContextSupport.currentUserIdOrNull());
        entity.setUpdatedAt(LocalDateTime.now());
        return categoryMapper.updateById(entity) > 0;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean deleteCategory(Long id) {
        Long tenantId = LinkContextSupport.currentTenantId();
        LinkCategoryEntity entity = selectCategoryRequired(tenantId, id);
        Long activeCount = itemMapper.selectCount(new LambdaQueryWrapper<LinkItemEntity>()
                .eq(LinkItemEntity::getTenantId, tenantId)
                .eq(LinkItemEntity::getCategoryId, entity.getId())
                .eq(LinkItemEntity::getStatus, LinkSupport.enabled()));
        Require.isTrue(activeCount == 0, "分类下存在启用网址，请先停用或迁移网址");
        return categoryMapper.deleteById(entity.getId()) > 0;
    }

    @Override
    public PageResult<LinkItemVO> pageItems(LinkItemPageQuery query) {
        LinkItemPageQuery resolved = query == null ? new LinkItemPageQuery() : query;
        Long tenantId = LinkContextSupport.currentTenantId();
        IPage<LinkItemEntity> page = itemMapper.selectPage(new Page<>(resolved.getPage(), resolved.getSize()),
                itemWrapper(tenantId, resolved));
        Map<Long, LinkCategoryEntity> categories = categoriesById(tenantId,
                page.getRecords().stream().map(LinkItemEntity::getCategoryId).toList());
        Map<Long, List<LinkVisibilityTargetEntity>> targets = targetsByLinkId(tenantId,
                page.getRecords().stream().map(LinkItemEntity::getId).toList());
        return PageResult.of(page.getRecords().stream()
                        .map(item -> toItemVO(item, categories.get(item.getCategoryId()), targets.get(item.getId())))
                        .toList(),
                page.getTotal(), page.getCurrent(), page.getSize());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long createItem(CreateLinkItemCommand command) {
        Long tenantId = LinkContextSupport.currentTenantId();
        validateAdminItem(tenantId, command.getCategoryId(), command.getVisibilityScope(), command.getVisibilityTargets());
        LocalDateTime now = LocalDateTime.now();
        LinkItemEntity entity = new LinkItemEntity();
        applyItemCommand(entity, command);
        entity.setTenantId(tenantId);
        entity.setStatus(LinkSupport.enabled());
        entity.setOpenMode(LinkSupport.newWindow());
        entity.setOwnerUserId(null);
        entity.setCreatedBy(LinkContextSupport.currentUserIdOrNull());
        entity.setUpdatedBy(LinkContextSupport.currentUserIdOrNull());
        entity.setCreatedAt(now);
        entity.setUpdatedAt(now);
        itemMapper.insert(entity);
        replaceTargets(tenantId, entity.getId(), command.getVisibilityScope(), command.getVisibilityTargets());
        return entity.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean updateItem(UpdateLinkItemCommand command) {
        Long tenantId = LinkContextSupport.currentTenantId();
        LinkItemEntity entity = selectItemRequired(tenantId, command.getId());
        if (LinkVisibilityScope.PERSONAL.name().equals(entity.getVisibilityScope())) {
            validateAdminPersonalItem(tenantId, entity.getOwnerUserId(), command.getCategoryId(),
                    command.getVisibilityScope(), command.getVisibilityTargets());
        } else {
            validateAdminItem(tenantId, command.getCategoryId(), command.getVisibilityScope(), command.getVisibilityTargets());
        }
        applyItemCommand(entity, command);
        entity.setUpdatedBy(LinkContextSupport.currentUserIdOrNull());
        entity.setUpdatedAt(LocalDateTime.now());
        int updated = itemMapper.updateById(entity);
        replaceTargets(tenantId, entity.getId(), command.getVisibilityScope(), command.getVisibilityTargets());
        return updated > 0;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean updateItemStatus(UpdateLinkItemStatusCommand command) {
        LinkItemEntity entity = selectItemRequired(LinkContextSupport.currentTenantId(), command.getId());
        entity.setStatus(LinkSupport.status(command.getStatus()));
        entity.setUpdatedBy(LinkContextSupport.currentUserIdOrNull());
        entity.setUpdatedAt(LocalDateTime.now());
        return itemMapper.updateById(entity) > 0;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean deleteItem(Long id) {
        Long tenantId = LinkContextSupport.currentTenantId();
        LinkItemEntity item = selectItemRequired(tenantId, id);
        targetMapper.delete(new LambdaQueryWrapper<LinkVisibilityTargetEntity>()
                .eq(LinkVisibilityTargetEntity::getTenantId, tenantId)
                .eq(LinkVisibilityTargetEntity::getLinkId, item.getId()));
        favoriteMapper.delete(new LambdaQueryWrapper<LinkFavoriteEntity>()
                .eq(LinkFavoriteEntity::getTenantId, tenantId)
                .eq(LinkFavoriteEntity::getLinkId, item.getId()));
        return itemMapper.deleteById(item.getId()) > 0;
    }

    private LambdaQueryWrapper<LinkCategoryEntity> categoryWrapper(Long tenantId, String keyword, LinkStatus status) {
        LambdaQueryWrapper<LinkCategoryEntity> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(LinkCategoryEntity::getTenantId, tenantId);
        String normalized = LinkContextSupport.trimToNull(keyword);
        wrapper.like(StringUtils.hasText(normalized), LinkCategoryEntity::getName, normalized);
        if (status != null) {
            wrapper.eq(LinkCategoryEntity::getStatus, status.name());
        }
        wrapper.orderByAsc(LinkCategoryEntity::getScope)
                .orderByAsc(LinkCategoryEntity::getOwnerUserId)
                .orderByAsc(LinkCategoryEntity::getSortNo)
                .orderByDesc(LinkCategoryEntity::getUpdatedAt);
        return wrapper;
    }

    private LambdaQueryWrapper<LinkItemEntity> itemWrapper(Long tenantId, LinkItemPageQuery query) {
        LambdaQueryWrapper<LinkItemEntity> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(LinkItemEntity::getTenantId, tenantId);
        if (query.getCategoryId() != null) {
            wrapper.eq(LinkItemEntity::getCategoryId, query.getCategoryId());
        }
        if (query.getVisibilityScope() != null) {
            wrapper.eq(LinkItemEntity::getVisibilityScope, query.getVisibilityScope().name());
        }
        if (query.getStatus() != null) {
            wrapper.eq(LinkItemEntity::getStatus, query.getStatus().name());
        }
        String keyword = LinkContextSupport.trimToNull(query.getKeyword());
        wrapper.and(StringUtils.hasText(keyword), nested -> nested
                .like(LinkItemEntity::getName, keyword)
                .or()
                .like(LinkItemEntity::getUrl, keyword)
                .or()
                .like(LinkItemEntity::getSummary, keyword)
                .or()
                .like(LinkItemEntity::getTags, keyword));
        wrapper.orderByAsc(LinkItemEntity::getVisibilityScope)
                .orderByAsc(LinkItemEntity::getOwnerUserId)
                .orderByAsc(LinkItemEntity::getCategoryId)
                .orderByDesc(LinkItemEntity::getRecommended)
                .orderByAsc(LinkItemEntity::getSortNo)
                .orderByDesc(LinkItemEntity::getUpdatedAt);
        return wrapper;
    }

    private void applyItemCommand(LinkItemEntity entity, CreateLinkItemCommand command) {
        entity.setCategoryId(command.getCategoryId());
        entity.setName(LinkContextSupport.trimRequired(command.getName(), "网址名称不能为空"));
        entity.setUrl(LinkSupport.normalizeUrl(command.getUrl()));
        entity.setSummary(LinkContextSupport.trimToNull(command.getSummary()));
        entity.setIconUrl(LinkContextSupport.trimToNull(command.getIconUrl()));
        entity.setTags(LinkSupport.joinTags(command.getTags()));
        entity.setVisibilityScope(LinkSupport.scope(command.getVisibilityScope()));
        entity.setRecommended(Boolean.TRUE.equals(command.getRecommended()));
        entity.setSortNo(command.getSortNo() == null ? 0 : command.getSortNo());
        entity.setRemark(LinkContextSupport.trimToNull(command.getRemark()));
    }

    private void validateAdminItem(Long tenantId,
                                   Long categoryId,
                                   LinkVisibilityScope scope,
                                   List<LinkVisibilityTargetCommand> targets) {
        LinkCategoryEntity category = selectCategoryRequired(tenantId, categoryId);
        Require.isTrue(enabledCategory(category) && LinkSupport.companyCategory().equals(category.getScope()),
                "网址分类不存在或已停用");
        Require.isTrue(scope != LinkVisibilityScope.PERSONAL, "后台网址列表不允许创建个人网址");
        if (scope == LinkVisibilityScope.DEPARTMENT) {
            requireTargets(targets, LinkVisibilityTargetType.DEPARTMENT, "可见范围为指定部门时必须选择部门");
            return;
        }
        if (scope == LinkVisibilityScope.USER) {
            requireTargets(targets, LinkVisibilityTargetType.USER, "可见范围为指定用户时必须选择用户");
            return;
        }
        Require.isTrue(targets == null || targets.isEmpty(), "公开和公司内网址不允许配置指定目标");
    }

    private void validateAdminPersonalItem(Long tenantId,
                                           Long ownerUserId,
                                           Long categoryId,
                                           LinkVisibilityScope scope,
                                           List<LinkVisibilityTargetCommand> targets) {
        LinkCategoryEntity category = selectCategoryRequired(tenantId, categoryId);
        Require.isTrue(enabledCategory(category)
                        && LinkCategoryScope.PERSONAL.name().equals(category.getScope())
                        && ownerUserId != null
                        && ownerUserId.equals(category.getOwnerUserId()),
                "个人网址分类不存在或已停用");
        Require.isTrue(scope == LinkVisibilityScope.PERSONAL, "个人网址不允许修改可见范围");
        Require.isTrue(targets == null || targets.isEmpty(), "个人网址不允许配置指定目标");
    }

    private void requireTargets(List<LinkVisibilityTargetCommand> targets,
                                LinkVisibilityTargetType targetType,
                                String message) {
        Require.isTrue(targets != null && !targets.isEmpty(), message);
        Require.isTrue(targets.stream().allMatch(target -> targetType == target.getTargetType()), "可见目标类型不正确");
    }

    private void replaceTargets(Long tenantId,
                                Long linkId,
                                LinkVisibilityScope scope,
                                List<LinkVisibilityTargetCommand> targets) {
        targetMapper.delete(new LambdaQueryWrapper<LinkVisibilityTargetEntity>()
                .eq(LinkVisibilityTargetEntity::getTenantId, tenantId)
                .eq(LinkVisibilityTargetEntity::getLinkId, linkId));
        if (scope == LinkVisibilityScope.DEPARTMENT || scope == LinkVisibilityScope.USER) {
            for (LinkVisibilityTargetCommand target : targets) {
                targetMapper.insert(toTargetEntity(tenantId, linkId, target));
            }
        }
    }
}
