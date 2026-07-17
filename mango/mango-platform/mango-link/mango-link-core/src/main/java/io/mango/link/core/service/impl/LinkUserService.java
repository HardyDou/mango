package io.mango.link.core.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import io.mango.common.result.Require;
import io.mango.common.vo.PageResult;
import io.mango.link.api.command.CreateLinkPersonalCategoryCommand;
import io.mango.link.api.command.CreateLinkFavoriteCommand;
import io.mango.link.api.command.CreateLinkPersonalItemCommand;
import io.mango.link.api.command.DeleteLinkFavoriteCommand;
import io.mango.link.api.command.UpdateLinkPersonalCategoryCommand;
import io.mango.link.api.command.UpdateLinkPersonalItemCommand;
import io.mango.link.api.enums.LinkCode;
import io.mango.link.api.enums.LinkVisibilityScope;
import io.mango.link.api.query.LinkCompanyItemQuery;
import io.mango.link.api.query.LinkFavoriteQuery;
import io.mango.link.api.query.LinkPersonalItemPageQuery;
import io.mango.link.api.vo.LinkCategoryVO;
import io.mango.link.api.vo.LinkFavoriteVO;
import io.mango.link.api.vo.LinkNavigationItemVO;
import io.mango.link.api.vo.LinkNavigationWidgetDataVO;
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
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class LinkUserService implements ILinkUserService {

    private static final int NAVIGATION_WIDGET_PAGE_SIZE = 200;

    private final LinkCategoryMapper categoryMapper;
    private final LinkItemMapper itemMapper;
    private final LinkVisibilityTargetMapper targetMapper;
    private final LinkFavoriteMapper favoriteMapper;
    private final LinkServiceSupport support;

    @Override
    public List<LinkNavigationItemVO> listCompanyItems(LinkCompanyItemQuery query) {
        LinkCompanyItemQuery resolved = query;
        if (resolved == null) {
            resolved = new LinkCompanyItemQuery();
        }
        LinkCompanyItemQuery resolvedQuery = resolved;
        String tenantId = LinkContextSupport.currentTenantId();
        Long userId = LinkContextSupport.currentUserIdOrNull();
        List<LinkItemEntity> items = itemMapper.selectList(companyWrapper(tenantId, resolved.getCategoryId()));
        Map<Long, LinkCategoryEntity> categories = support.categoriesById(tenantId,
                items.stream().map(LinkItemEntity::getCategoryId).toList());
        Map<Long, List<LinkVisibilityTargetEntity>> targets = support.targetsByLinkId(tenantId,
                items.stream().map(LinkItemEntity::getId).toList());
        Set<Long> favorites = support.favoriteLinkIds(tenantId, userId,
                items.stream().map(LinkItemEntity::getId).toList());
        return items.stream()
                .filter(item -> support.enabledCategory(categories.get(item.getCategoryId())))
                .filter(item -> support.keywordMatched(item, resolvedQuery.getKeyword()))
                .filter(item -> support.isVisibleToUser(tenantId, userId, item, targets.get(item.getId())))
                .sorted(navigationComparator())
                .map(item -> support.toNavigationVO(item, categories.get(item.getCategoryId()),
                        favorites.contains(item.getId())))
                .toList();
    }

    @Override
    public LinkNavigationWidgetDataVO getNavigationWidgetData() {
        LinkPersonalItemPageQuery personalQuery = new LinkPersonalItemPageQuery();
        personalQuery.setPage(1);
        personalQuery.setSize(NAVIGATION_WIDGET_PAGE_SIZE);
        LinkNavigationWidgetDataVO data = new LinkNavigationWidgetDataVO();
        data.setCompanyItems(listCompanyItems(new LinkCompanyItemQuery()));
        data.setPersonalItems(pagePersonalItems(personalQuery).getList());
        data.setFavoriteItems(listFavorites(new LinkFavoriteQuery()));
        data.setCategories(listPersonalCategories());
        return data;
    }

    @Override
    public List<LinkCategoryVO> listPersonalCategories() {
        String tenantId = LinkContextSupport.currentTenantId();
        Long userId = LinkContextSupport.currentUserId();
        return categoryMapper.selectList(new LambdaQueryWrapper<LinkCategoryEntity>()
                        .eq(LinkCategoryEntity::getTenantId, tenantId)
                        .eq(LinkCategoryEntity::getScope, LinkSupport.personalCategory())
                        .eq(LinkCategoryEntity::getOwnerUserId, userId)
                        .eq(LinkCategoryEntity::getStatus, LinkSupport.enabled())
                        .orderByAsc(LinkCategoryEntity::getSortNo)
                        .orderByDesc(LinkCategoryEntity::getUpdatedAt))
                .stream()
                .map(support::toCategoryVO)
                .toList();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long createPersonalCategory(CreateLinkPersonalCategoryCommand command) {
        Require.notNull(command, LinkCode.LINK_BUSINESS_ERROR, "新增个人分组命令不能为空");
        String tenantId = LinkContextSupport.currentTenantId();
        Long userId = LinkContextSupport.currentUserId();
        String name = LinkContextSupport.trimRequired(command.getName(), "分组名称不能为空");
        Require.isTrue(selectCategoryByOwnerAndName(tenantId, userId, name) == null,
                LinkCode.LINK_BUSINESS_ERROR, "分组名称已存在");
        LocalDateTime now = LocalDateTime.now();
        LinkCategoryEntity entity = new LinkCategoryEntity();
        entity.setTenantId(tenantId);
        entity.setScope(LinkSupport.personalCategory());
        entity.setOwnerUserId(userId);
        entity.setName(name);
        Integer sortNo = command.getSortNo();
        if (sortNo == null) {
            sortNo = 0;
        }
        entity.setSortNo(sortNo);
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
    public boolean updatePersonalCategory(UpdateLinkPersonalCategoryCommand command) {
        Require.notNull(command, LinkCode.LINK_BUSINESS_ERROR, "更新个人分组命令不能为空");
        String tenantId = LinkContextSupport.currentTenantId();
        Long userId = LinkContextSupport.currentUserId();
        LinkCategoryEntity category = requireOwnedPersonalCategory(tenantId, userId, command.getId());
        String name = LinkContextSupport.trimRequired(command.getName(), "分组名称不能为空");
        LinkCategoryEntity exists = selectCategoryByOwnerAndName(tenantId, userId, name);
        Require.isTrue(exists == null || category.getId().equals(exists.getId()),
                LinkCode.LINK_BUSINESS_ERROR, "分组名称已存在");
        category.setName(name);
        Integer sortNo = command.getSortNo();
        if (sortNo == null) {
            sortNo = category.getSortNo();
        }
        category.setSortNo(sortNo);
        category.setUpdatedBy(userId);
        category.setUpdatedAt(LocalDateTime.now());
        return categoryMapper.updateById(category) > 0;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean deletePersonalCategory(Long id) {
        Require.notNull(id, LinkCode.LINK_BUSINESS_ERROR, "个人分组 ID 不能为空");
        String tenantId = LinkContextSupport.currentTenantId();
        Long userId = LinkContextSupport.currentUserId();
        LinkCategoryEntity category = requireOwnedPersonalCategory(tenantId, userId, id);
        Long itemCount = itemMapper.selectCount(new LambdaQueryWrapper<LinkItemEntity>()
                .eq(LinkItemEntity::getTenantId, tenantId)
                .eq(LinkItemEntity::getCategoryId, category.getId())
                .eq(LinkItemEntity::getVisibilityScope, LinkVisibilityScope.PERSONAL.name())
                .eq(LinkItemEntity::getOwnerUserId, userId));
        Require.isTrue(itemCount == null || itemCount.longValue() == 0L,
                LinkCode.LINK_BUSINESS_ERROR, "分组下存在网址，请先删除或移动网址");
        return categoryMapper.deleteById(category.getId()) > 0;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean createFavorite(CreateLinkFavoriteCommand command) {
        Require.notNull(command, LinkCode.LINK_BUSINESS_ERROR, "新增收藏命令不能为空");
        String tenantId = LinkContextSupport.currentTenantId();
        Long userId = LinkContextSupport.currentUserId();
        LinkItemEntity item = support.selectItemRequired(tenantId, command.getLinkId());
        LinkCategoryEntity category = null;
        if (item.getCategoryId() != null) {
            category = support.categoriesById(tenantId, List.of(item.getCategoryId())).get(item.getCategoryId());
        }
        Require.isTrue(visibleCategoryForItem(userId, item, category), LinkCode.LINK_BUSINESS_ERROR, "网址不可见");
        List<LinkVisibilityTargetEntity> targets = targetMapper.selectList(new LambdaQueryWrapper<LinkVisibilityTargetEntity>()
                .eq(LinkVisibilityTargetEntity::getTenantId, tenantId)
                .eq(LinkVisibilityTargetEntity::getLinkId, item.getId()));
        Require.isTrue(support.isVisibleToUser(tenantId, userId, item, targets),
                LinkCode.LINK_BUSINESS_ERROR, "网址不可见，不能收藏");
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
        Require.notNull(command, LinkCode.LINK_BUSINESS_ERROR, "删除收藏命令不能为空");
        String tenantId = LinkContextSupport.currentTenantId();
        Long userId = LinkContextSupport.currentUserId();
        favoriteMapper.delete(new LambdaQueryWrapper<LinkFavoriteEntity>()
                .eq(LinkFavoriteEntity::getTenantId, tenantId)
                .eq(LinkFavoriteEntity::getUserId, userId)
                .eq(LinkFavoriteEntity::getLinkId, command.getLinkId()));
        return true;
    }

    @Override
    public List<LinkFavoriteVO> listFavorites(LinkFavoriteQuery query) {
        LinkFavoriteQuery resolved = query;
        if (resolved == null) {
            resolved = new LinkFavoriteQuery();
        }
        LinkFavoriteQuery resolvedQuery = resolved;
        String tenantId = LinkContextSupport.currentTenantId();
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
        Map<Long, LinkCategoryEntity> categories = support.categoriesById(tenantId,
                items.values().stream().map(LinkItemEntity::getCategoryId).toList());
        Map<Long, List<LinkVisibilityTargetEntity>> targets = support.targetsByLinkId(tenantId, linkIds);
        return favorites.stream()
                .filter(favorite -> items.containsKey(favorite.getLinkId()))
                .map(favorite -> toVisibleFavorite(tenantId, userId, favorite, items.get(favorite.getLinkId()),
                        categories, targets, resolvedQuery))
                .filter(vo -> vo != null)
                .toList();
    }

    @Override
    public PageResult<LinkPersonalItemVO> pagePersonalItems(LinkPersonalItemPageQuery query) {
        LinkPersonalItemPageQuery resolved = query;
        if (resolved == null) {
            resolved = new LinkPersonalItemPageQuery();
        }
        String tenantId = LinkContextSupport.currentTenantId();
        Long userId = LinkContextSupport.currentUserId();
        IPage<LinkItemEntity> page = itemMapper.selectPage(new Page<>(resolved.getPage(), resolved.getSize()),
                personalWrapper(tenantId, userId, resolved));
        Map<Long, LinkCategoryEntity> categories = support.categoriesById(tenantId,
                page.getRecords().stream().map(LinkItemEntity::getCategoryId).toList());
        Set<Long> favorites = support.favoriteLinkIds(tenantId, userId,
                page.getRecords().stream().map(LinkItemEntity::getId).toList());
        return PageResult.of(page.getRecords().stream()
                        .map(item -> support.toPersonalVO(item, categories.get(item.getCategoryId()),
                                favorites.contains(item.getId())))
                        .toList(),
                page.getTotal(), page.getCurrent(), page.getSize());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long createPersonalItem(CreateLinkPersonalItemCommand command) {
        Require.notNull(command, LinkCode.LINK_BUSINESS_ERROR, "新增个人网址命令不能为空");
        String tenantId = LinkContextSupport.currentTenantId();
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
        Require.notNull(command, LinkCode.LINK_BUSINESS_ERROR, "更新个人网址命令不能为空");
        String tenantId = LinkContextSupport.currentTenantId();
        Long userId = LinkContextSupport.currentUserId();
        LinkItemEntity item = support.selectItemRequired(tenantId, command.getId());
        Require.isTrue(LinkVisibilityScope.PERSONAL.name().equals(item.getVisibilityScope())
                && userId.equals(item.getOwnerUserId()), LinkCode.LINK_BUSINESS_ERROR, "个人网址不存在");
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
        Require.notNull(id, LinkCode.LINK_BUSINESS_ERROR, "个人网址 ID 不能为空");
        String tenantId = LinkContextSupport.currentTenantId();
        Long userId = LinkContextSupport.currentUserId();
        LinkItemEntity item = support.selectItemRequired(tenantId, id);
        Require.isTrue(LinkVisibilityScope.PERSONAL.name().equals(item.getVisibilityScope())
                && userId.equals(item.getOwnerUserId()), LinkCode.LINK_BUSINESS_ERROR, "个人网址不存在");
        favoriteMapper.delete(new LambdaQueryWrapper<LinkFavoriteEntity>()
                .eq(LinkFavoriteEntity::getTenantId, tenantId)
                .eq(LinkFavoriteEntity::getLinkId, item.getId()));
        return itemMapper.deleteById(item.getId()) > 0;
    }

    private LambdaQueryWrapper<LinkItemEntity> companyWrapper(String tenantId, Long categoryId) {
        LambdaQueryWrapper<LinkItemEntity> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(LinkItemEntity::getTenantId, tenantId)
                .eq(LinkItemEntity::getStatus, LinkSupport.enabled())
                .ne(LinkItemEntity::getVisibilityScope, LinkVisibilityScope.PERSONAL.name());
        if (categoryId != null) {
            wrapper.eq(LinkItemEntity::getCategoryId, categoryId);
        }
        return wrapper;
    }

    private LambdaQueryWrapper<LinkItemEntity> personalWrapper(String tenantId,
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

    private LinkFavoriteVO toVisibleFavorite(String tenantId,
                                             Long userId,
                                             LinkFavoriteEntity favorite,
                                             LinkItemEntity item,
                                             Map<Long, LinkCategoryEntity> categories,
                                             Map<Long, List<LinkVisibilityTargetEntity>> targets,
                                             LinkFavoriteQuery query) {
        LinkCategoryEntity category = categories.get(item.getCategoryId());
        if (!visibleCategoryForItem(userId, item, category)) {
            return null;
        }
        if (!support.keywordMatched(item, query.getKeyword())) {
            return null;
        }
        if (query.getCategoryId() != null && !query.getCategoryId().equals(item.getCategoryId())) {
            return null;
        }
        if (!support.isVisibleToUser(tenantId, userId, item, targets.get(item.getId()))) {
            return null;
        }
        return support.toFavoriteVO(favorite, item, category);
    }

    private void validatePersonalCategory(String tenantId, Long categoryId) {
        if (categoryId != null) {
            LinkCategoryEntity category = support.selectCategoryRequired(tenantId, categoryId);
            Long userId = LinkContextSupport.currentUserId();
            boolean personalCategory = LinkSupport.personalCategory().equals(category.getScope())
                    && userId.equals(category.getOwnerUserId());
            Require.isTrue(support.enabledCategory(category) && personalCategory,
                    LinkCode.LINK_BUSINESS_ERROR, "网址分组不存在或已停用");
        }
    }

    private LinkCategoryEntity requireOwnedPersonalCategory(String tenantId, Long userId, Long categoryId) {
        LinkCategoryEntity category = support.selectCategoryRequired(tenantId, categoryId);
        Require.isTrue(LinkSupport.personalCategory().equals(category.getScope())
                && userId.equals(category.getOwnerUserId())
                && LinkSupport.enabled().equals(category.getStatus()),
                LinkCode.LINK_BUSINESS_ERROR, "个人分组不存在或已停用");
        return category;
    }

    private boolean visibleCategoryForItem(Long userId, LinkItemEntity item, LinkCategoryEntity category) {
        return support.enabledCategory(category) || personalUngroupedItemVisibleToOwner(userId, item);
    }

    private LinkCategoryEntity selectCategoryByOwnerAndName(String tenantId, Long userId, String name) {
        return categoryMapper.selectOne(new LambdaQueryWrapper<LinkCategoryEntity>()
                .eq(LinkCategoryEntity::getTenantId, tenantId)
                .eq(LinkCategoryEntity::getScope, LinkSupport.personalCategory())
                .eq(LinkCategoryEntity::getOwnerUserId, userId)
                .eq(LinkCategoryEntity::getName, name)
                .last("LIMIT 1"));
    }

    private boolean personalUngroupedItemVisibleToOwner(Long userId, LinkItemEntity item) {
        return item.getCategoryId() == null
                && LinkVisibilityScope.PERSONAL.name().equals(item.getVisibilityScope())
                && userId.equals(item.getOwnerUserId());
    }

    private Comparator<LinkItemEntity> navigationComparator() {
        return Comparator.comparing(LinkItemEntity::getCategoryId, Comparator.nullsLast(Long::compareTo))
                .thenComparing(LinkItemEntity::getRecommended, Comparator.nullsLast(Comparator.reverseOrder()))
                .thenComparing(LinkItemEntity::getSortNo, Comparator.nullsLast(Integer::compareTo))
                .thenComparing(LinkItemEntity::getUpdatedAt, Comparator.nullsLast(Comparator.reverseOrder()));
    }
}
