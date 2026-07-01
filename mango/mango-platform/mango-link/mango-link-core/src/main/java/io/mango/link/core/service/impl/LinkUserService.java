package io.mango.link.core.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import io.mango.common.result.Require;
import io.mango.common.vo.PageResult;
import io.mango.identity.api.TenantMemberProvider;
import io.mango.link.api.command.CreateLinkPersonalCategoryCommand;
import io.mango.link.api.command.CreateLinkFavoriteCommand;
import io.mango.link.api.command.CreateLinkPersonalItemCommand;
import io.mango.link.api.command.DeleteLinkFavoriteCommand;
import io.mango.link.api.command.UpdateLinkPersonalItemCommand;
import io.mango.link.api.enums.LinkVisibilityScope;
import io.mango.link.api.query.LinkCompanyItemQuery;
import io.mango.link.api.query.LinkFavoriteQuery;
import io.mango.link.api.query.LinkPersonalItemPageQuery;
import io.mango.link.api.vo.LinkCategoryVO;
import io.mango.link.api.vo.LinkFavoriteVO;
import io.mango.link.api.vo.LinkNavigationItemVO;
import io.mango.link.api.vo.LinkPersonalItemVO;
import io.mango.link.core.entity.LinkCategoryEntity;
import io.mango.link.core.entity.LinkFavoriteEntity;
import io.mango.link.core.entity.LinkItemEntity;
import io.mango.link.core.entity.LinkVisibilityTargetEntity;
import io.mango.link.core.mapper.LinkCategoryMapper;
import io.mango.link.core.mapper.LinkFavoriteMapper;
import io.mango.link.core.mapper.LinkItemMapper;
import io.mango.link.core.mapper.LinkVisibilityTargetMapper;
import io.mango.link.core.service.ILinkUserService;
import io.mango.link.core.support.LinkContextSupport;
import io.mango.link.core.support.LinkSupport;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Service
public class LinkUserService extends LinkBaseService implements ILinkUserService {

    public LinkUserService(LinkCategoryMapper categoryMapper,
                           LinkItemMapper itemMapper,
                           LinkVisibilityTargetMapper targetMapper,
                           LinkFavoriteMapper favoriteMapper,
                           ObjectProvider<TenantMemberProvider> tenantMemberProvider) {
        super(categoryMapper, itemMapper, targetMapper, favoriteMapper, tenantMemberProvider);
    }

    @Override
    public List<LinkNavigationItemVO> listCompanyItems(LinkCompanyItemQuery query) {
        LinkCompanyItemQuery resolved = query == null ? new LinkCompanyItemQuery() : query;
        Long tenantId = LinkContextSupport.currentTenantId();
        Long userId = LinkContextSupport.currentUserIdOrNull();
        List<LinkItemEntity> items = itemMapper.selectList(companyWrapper(tenantId, resolved.getCategoryId()));
        Map<Long, LinkCategoryEntity> categories = categoriesById(tenantId, items.stream().map(LinkItemEntity::getCategoryId).toList());
        Map<Long, List<LinkVisibilityTargetEntity>> targets = targetsByLinkId(tenantId, items.stream().map(LinkItemEntity::getId).toList());
        Set<Long> favorites = favoriteLinkIds(tenantId, userId, items.stream().map(LinkItemEntity::getId).toList());
        return items.stream()
                .filter(item -> enabledCategory(categories.get(item.getCategoryId())))
                .filter(item -> keywordMatched(item, resolved.getKeyword()))
                .filter(item -> isVisibleToUser(tenantId, userId, item, targets.get(item.getId())))
                .sorted(navigationComparator())
                .map(item -> toNavigationVO(item, categories.get(item.getCategoryId()), favorites.contains(item.getId())))
                .toList();
    }

    @Override
    public List<LinkCategoryVO> listPersonalCategories() {
        Long tenantId = LinkContextSupport.currentTenantId();
        Long userId = LinkContextSupport.currentUserId();
        return categoryMapper.selectList(new LambdaQueryWrapper<LinkCategoryEntity>()
                        .eq(LinkCategoryEntity::getTenantId, tenantId)
                        .eq(LinkCategoryEntity::getScope, LinkSupport.personalCategory())
                        .eq(LinkCategoryEntity::getOwnerUserId, userId)
                        .eq(LinkCategoryEntity::getStatus, LinkSupport.enabled())
                        .orderByAsc(LinkCategoryEntity::getSortNo)
                        .orderByDesc(LinkCategoryEntity::getUpdatedAt))
                .stream()
                .map(this::toCategoryVO)
                .toList();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long createPersonalCategory(CreateLinkPersonalCategoryCommand command) {
        Long tenantId = LinkContextSupport.currentTenantId();
        Long userId = LinkContextSupport.currentUserId();
        String name = LinkContextSupport.trimRequired(command.getName(), "分组名称不能为空");
        Require.isNull(categoryMapper.selectByScopeOwnerAndName(tenantId, LinkSupport.personalCategory(), userId, name),
                "分组名称已存在");
        LocalDateTime now = LocalDateTime.now();
        LinkCategoryEntity entity = new LinkCategoryEntity();
        entity.setTenantId(tenantId);
        entity.setScope(LinkSupport.personalCategory());
        entity.setOwnerUserId(userId);
        entity.setName(name);
        entity.setSortNo(command.getSortNo() == null ? 0 : command.getSortNo());
        entity.setStatus(LinkSupport.enabled());
        entity.setCreatedBy(userId);
        entity.setUpdatedBy(userId);
        entity.setCreatedAt(now);
        entity.setUpdatedAt(now);
        categoryMapper.insert(entity);
        return entity.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean createFavorite(CreateLinkFavoriteCommand command) {
        Long tenantId = LinkContextSupport.currentTenantId();
        Long userId = LinkContextSupport.currentUserId();
        LinkItemEntity item = selectItemRequired(tenantId, command.getLinkId());
        LinkCategoryEntity category = categoryMapper.selectByTenantAndId(tenantId, item.getCategoryId());
        Require.isTrue(enabledCategory(category), "网址不可见");
        List<LinkVisibilityTargetEntity> targets = targetMapper.selectList(new LambdaQueryWrapper<LinkVisibilityTargetEntity>()
                .eq(LinkVisibilityTargetEntity::getTenantId, tenantId)
                .eq(LinkVisibilityTargetEntity::getLinkId, item.getId()));
        Require.isTrue(isVisibleToUser(tenantId, userId, item, targets), "网址不可见，不能收藏");
        LinkFavoriteEntity exists = favoriteMapper.selectOne(new LambdaQueryWrapper<LinkFavoriteEntity>()
                .eq(LinkFavoriteEntity::getTenantId, tenantId)
                .eq(LinkFavoriteEntity::getUserId, userId)
                .eq(LinkFavoriteEntity::getLinkId, item.getId())
                .last("LIMIT 1"));
        if (exists != null) {
            return true;
        }
        LinkFavoriteEntity favorite = new LinkFavoriteEntity();
        favorite.setTenantId(tenantId);
        favorite.setUserId(userId);
        favorite.setLinkId(item.getId());
        favorite.setCreatedAt(LocalDateTime.now());
        favoriteMapper.insert(favorite);
        return true;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean deleteFavorite(DeleteLinkFavoriteCommand command) {
        Long tenantId = LinkContextSupport.currentTenantId();
        Long userId = LinkContextSupport.currentUserId();
        favoriteMapper.delete(new LambdaQueryWrapper<LinkFavoriteEntity>()
                .eq(LinkFavoriteEntity::getTenantId, tenantId)
                .eq(LinkFavoriteEntity::getUserId, userId)
                .eq(LinkFavoriteEntity::getLinkId, command.getLinkId()));
        return true;
    }

    @Override
    public List<LinkFavoriteVO> listFavorites(LinkFavoriteQuery query) {
        LinkFavoriteQuery resolved = query == null ? new LinkFavoriteQuery() : query;
        Long tenantId = LinkContextSupport.currentTenantId();
        Long userId = LinkContextSupport.currentUserId();
        List<LinkFavoriteEntity> favorites = favoriteMapper.selectList(new LambdaQueryWrapper<LinkFavoriteEntity>()
                .eq(LinkFavoriteEntity::getTenantId, tenantId)
                .eq(LinkFavoriteEntity::getUserId, userId)
                .orderByDesc(LinkFavoriteEntity::getCreatedAt));
        if (favorites.isEmpty()) {
            return List.of();
        }
        List<Long> linkIds = favorites.stream().map(LinkFavoriteEntity::getLinkId).toList();
        Map<Long, LinkItemEntity> items = itemMapper.selectList(new LambdaQueryWrapper<LinkItemEntity>()
                        .eq(LinkItemEntity::getTenantId, tenantId)
                        .in(LinkItemEntity::getId, linkIds))
                .stream()
                .collect(java.util.stream.Collectors.toMap(LinkItemEntity::getId, item -> item));
        Map<Long, LinkCategoryEntity> categories = categoriesById(tenantId,
                items.values().stream().map(LinkItemEntity::getCategoryId).toList());
        Map<Long, List<LinkVisibilityTargetEntity>> targets = targetsByLinkId(tenantId, linkIds);
        return favorites.stream()
                .filter(favorite -> items.containsKey(favorite.getLinkId()))
                .map(favorite -> toVisibleFavorite(tenantId, userId, favorite, items.get(favorite.getLinkId()),
                        categories, targets, resolved))
                .filter(vo -> vo != null)
                .toList();
    }

    @Override
    public PageResult<LinkPersonalItemVO> pagePersonalItems(LinkPersonalItemPageQuery query) {
        LinkPersonalItemPageQuery resolved = query == null ? new LinkPersonalItemPageQuery() : query;
        Long tenantId = LinkContextSupport.currentTenantId();
        Long userId = LinkContextSupport.currentUserId();
        IPage<LinkItemEntity> page = itemMapper.selectPage(new Page<>(resolved.getPage(), resolved.getSize()),
                personalWrapper(tenantId, userId, resolved));
        Map<Long, LinkCategoryEntity> categories = categoriesById(tenantId,
                page.getRecords().stream().map(LinkItemEntity::getCategoryId).toList());
        return PageResult.of(page.getRecords().stream()
                        .map(item -> toPersonalVO(item, categories.get(item.getCategoryId())))
                        .toList(),
                page.getTotal(), page.getCurrent(), page.getSize());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long createPersonalItem(CreateLinkPersonalItemCommand command) {
        Long tenantId = LinkContextSupport.currentTenantId();
        Long userId = LinkContextSupport.currentUserId();
        validatePersonalCategory(tenantId, command.getCategoryId());
        LocalDateTime now = LocalDateTime.now();
        LinkItemEntity item = new LinkItemEntity();
        item.setTenantId(tenantId);
        item.setCategoryId(command.getCategoryId());
        item.setName(LinkContextSupport.trimRequired(command.getName(), "网址名称不能为空"));
        item.setUrl(LinkSupport.normalizeUrl(command.getUrl()));
        item.setSummary(LinkContextSupport.trimToNull(command.getSummary()));
        item.setIconUrl(LinkContextSupport.trimToNull(command.getIconUrl()));
        item.setTags(LinkSupport.joinTags(command.getTags()));
        item.setVisibilityScope(LinkVisibilityScope.PERSONAL.name());
        item.setOwnerUserId(userId);
        item.setOpenMode(LinkSupport.newWindow());
        item.setRecommended(false);
        item.setSortNo(0);
        item.setStatus(LinkSupport.enabled());
        item.setRemark(LinkContextSupport.trimToNull(command.getRemark()));
        item.setCreatedBy(userId);
        item.setUpdatedBy(userId);
        item.setCreatedAt(now);
        item.setUpdatedAt(now);
        itemMapper.insert(item);
        return item.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean updatePersonalItem(UpdateLinkPersonalItemCommand command) {
        Long tenantId = LinkContextSupport.currentTenantId();
        Long userId = LinkContextSupport.currentUserId();
        LinkItemEntity item = selectItemRequired(tenantId, command.getId());
        Require.isTrue(LinkVisibilityScope.PERSONAL.name().equals(item.getVisibilityScope())
                && userId.equals(item.getOwnerUserId()), "个人网址不存在");
        validatePersonalCategory(tenantId, command.getCategoryId());
        item.setCategoryId(command.getCategoryId());
        item.setName(LinkContextSupport.trimRequired(command.getName(), "网址名称不能为空"));
        item.setUrl(LinkSupport.normalizeUrl(command.getUrl()));
        item.setSummary(LinkContextSupport.trimToNull(command.getSummary()));
        item.setIconUrl(LinkContextSupport.trimToNull(command.getIconUrl()));
        item.setTags(LinkSupport.joinTags(command.getTags()));
        item.setRemark(LinkContextSupport.trimToNull(command.getRemark()));
        item.setUpdatedBy(userId);
        item.setUpdatedAt(LocalDateTime.now());
        return itemMapper.updateById(item) > 0;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean deletePersonalItem(Long id) {
        Long tenantId = LinkContextSupport.currentTenantId();
        Long userId = LinkContextSupport.currentUserId();
        LinkItemEntity item = selectItemRequired(tenantId, id);
        Require.isTrue(LinkVisibilityScope.PERSONAL.name().equals(item.getVisibilityScope())
                && userId.equals(item.getOwnerUserId()), "个人网址不存在");
        favoriteMapper.delete(new LambdaQueryWrapper<LinkFavoriteEntity>()
                .eq(LinkFavoriteEntity::getTenantId, tenantId)
                .eq(LinkFavoriteEntity::getLinkId, item.getId()));
        return itemMapper.deleteById(item.getId()) > 0;
    }

    private LambdaQueryWrapper<LinkItemEntity> companyWrapper(Long tenantId, Long categoryId) {
        LambdaQueryWrapper<LinkItemEntity> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(LinkItemEntity::getTenantId, tenantId)
                .eq(LinkItemEntity::getStatus, LinkSupport.enabled())
                .ne(LinkItemEntity::getVisibilityScope, LinkVisibilityScope.PERSONAL.name());
        if (categoryId != null) {
            wrapper.eq(LinkItemEntity::getCategoryId, categoryId);
        }
        return wrapper;
    }

    private LambdaQueryWrapper<LinkItemEntity> personalWrapper(Long tenantId,
                                                              Long userId,
                                                              LinkPersonalItemPageQuery query) {
        LambdaQueryWrapper<LinkItemEntity> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(LinkItemEntity::getTenantId, tenantId)
                .eq(LinkItemEntity::getOwnerUserId, userId)
                .eq(LinkItemEntity::getVisibilityScope, LinkVisibilityScope.PERSONAL.name());
        if (query.getCategoryId() != null) {
            wrapper.eq(LinkItemEntity::getCategoryId, query.getCategoryId());
        }
        String keyword = LinkContextSupport.trimToNull(query.getKeyword());
        wrapper.and(StringUtils.hasText(keyword), nested -> nested
                .like(LinkItemEntity::getName, keyword)
                .or()
                .like(LinkItemEntity::getUrl, keyword)
                .or()
                .like(LinkItemEntity::getRemark, keyword));
        wrapper.orderByDesc(LinkItemEntity::getUpdatedAt);
        return wrapper;
    }

    private LinkFavoriteVO toVisibleFavorite(Long tenantId,
                                             Long userId,
                                             LinkFavoriteEntity favorite,
                                             LinkItemEntity item,
                                             Map<Long, LinkCategoryEntity> categories,
                                             Map<Long, List<LinkVisibilityTargetEntity>> targets,
                                             LinkFavoriteQuery query) {
        LinkCategoryEntity category = categories.get(item.getCategoryId());
        if (!enabledCategory(category)
                || !keywordMatched(item, query.getKeyword())
                || (query.getCategoryId() != null && !query.getCategoryId().equals(item.getCategoryId()))
                || !isVisibleToUser(tenantId, userId, item, targets.get(item.getId()))) {
            return null;
        }
        return toFavoriteVO(favorite, item, category);
    }

    private void validatePersonalCategory(Long tenantId, Long categoryId) {
        if (categoryId != null) {
            LinkCategoryEntity category = selectCategoryRequired(tenantId, categoryId);
            Long userId = LinkContextSupport.currentUserId();
            boolean companyCategory = LinkSupport.companyCategory().equals(category.getScope())
                    && Long.valueOf(0L).equals(category.getOwnerUserId());
            boolean personalCategory = LinkSupport.personalCategory().equals(category.getScope())
                    && userId.equals(category.getOwnerUserId());
            Require.isTrue(enabledCategory(category) && (companyCategory || personalCategory), "网址分组不存在或已停用");
        }
    }

    private Comparator<LinkItemEntity> navigationComparator() {
        return Comparator.comparing(LinkItemEntity::getCategoryId, Comparator.nullsLast(Long::compareTo))
                .thenComparing(LinkItemEntity::getRecommended, Comparator.nullsLast(Comparator.reverseOrder()))
                .thenComparing(LinkItemEntity::getSortNo, Comparator.nullsLast(Integer::compareTo))
                .thenComparing(LinkItemEntity::getUpdatedAt, Comparator.nullsLast(Comparator.reverseOrder()));
    }
}
