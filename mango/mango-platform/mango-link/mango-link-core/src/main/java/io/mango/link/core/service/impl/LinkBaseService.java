package io.mango.link.core.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import io.mango.common.result.Require;
import io.mango.identity.api.TenantMemberProvider;
import io.mango.identity.api.vo.TenantMemberInfo;
import io.mango.link.api.command.LinkVisibilityTargetCommand;
import io.mango.link.api.enums.LinkNavigationSource;
import io.mango.link.api.enums.LinkVisibilityScope;
import io.mango.link.api.enums.LinkVisibilityTargetType;
import io.mango.link.api.vo.LinkCategoryVO;
import io.mango.link.api.vo.LinkFavoriteVO;
import io.mango.link.api.vo.LinkItemVO;
import io.mango.link.api.vo.LinkNavigationItemVO;
import io.mango.link.api.vo.LinkPersonalItemVO;
import io.mango.link.api.vo.LinkPublicItemVO;
import io.mango.link.api.vo.LinkVisibilityTargetVO;
import io.mango.link.core.entity.LinkCategoryEntity;
import io.mango.link.core.entity.LinkFavoriteEntity;
import io.mango.link.core.entity.LinkItemEntity;
import io.mango.link.core.entity.LinkVisibilityTargetEntity;
import io.mango.link.core.mapper.LinkCategoryMapper;
import io.mango.link.core.mapper.LinkFavoriteMapper;
import io.mango.link.core.mapper.LinkItemMapper;
import io.mango.link.core.mapper.LinkVisibilityTargetMapper;
import io.mango.link.core.support.LinkContextSupport;
import io.mango.link.core.support.LinkSupport;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

@RequiredArgsConstructor
abstract class LinkBaseService {

    protected final LinkCategoryMapper categoryMapper;
    protected final LinkItemMapper itemMapper;
    protected final LinkVisibilityTargetMapper targetMapper;
    protected final LinkFavoriteMapper favoriteMapper;
    protected final ObjectProvider<TenantMemberProvider> tenantMemberProvider;

    protected LinkCategoryEntity selectCategoryRequired(Long tenantId, Long categoryId) {
        LinkCategoryEntity category = categoryMapper.selectByTenantAndId(tenantId, categoryId);
        Require.notNull(category, "网址分类不存在");
        return category;
    }

    protected LinkItemEntity selectItemRequired(Long tenantId, Long id) {
        LinkItemEntity item = itemMapper.selectByTenantAndId(tenantId, id);
        Require.notNull(item, "网址不存在");
        return item;
    }

    protected void requireEnabledCategory(Long tenantId, Long categoryId) {
        LinkCategoryEntity category = selectCategoryRequired(tenantId, categoryId);
        Require.isTrue(LinkSupport.enabled().equals(category.getStatus()), "网址分类已停用");
    }

    protected Map<Long, LinkCategoryEntity> categoriesById(Long tenantId, Collection<Long> categoryIds) {
        if (categoryIds == null || categoryIds.isEmpty()) {
            return Collections.emptyMap();
        }
        List<Long> resolvedIds = categoryIds.stream()
                .filter(java.util.Objects::nonNull)
                .distinct()
                .toList();
        if (resolvedIds.isEmpty()) {
            return Collections.emptyMap();
        }
        List<LinkCategoryEntity> categories = categoryMapper.selectList(new LambdaQueryWrapper<LinkCategoryEntity>()
                .eq(LinkCategoryEntity::getTenantId, tenantId)
                .in(LinkCategoryEntity::getId, resolvedIds));
        Map<Long, LinkCategoryEntity> result = new HashMap<>();
        for (LinkCategoryEntity category : categories) {
            result.put(category.getId(), category);
        }
        return result;
    }

    protected Map<Long, List<LinkVisibilityTargetEntity>> targetsByLinkId(Long tenantId, Collection<Long> linkIds) {
        if (linkIds == null || linkIds.isEmpty()) {
            return Map.of();
        }
        return targetMapper.selectList(new LambdaQueryWrapper<LinkVisibilityTargetEntity>()
                        .eq(LinkVisibilityTargetEntity::getTenantId, tenantId)
                        .in(LinkVisibilityTargetEntity::getLinkId, linkIds))
                .stream()
                .collect(java.util.stream.Collectors.groupingBy(LinkVisibilityTargetEntity::getLinkId));
    }

    protected Set<Long> favoriteLinkIds(Long tenantId, Long userId, Collection<Long> linkIds) {
        if (userId == null || linkIds == null || linkIds.isEmpty()) {
            return Set.of();
        }
        List<LinkFavoriteEntity> favorites = favoriteMapper.selectList(new LambdaQueryWrapper<LinkFavoriteEntity>()
                .eq(LinkFavoriteEntity::getTenantId, tenantId)
                .eq(LinkFavoriteEntity::getUserId, userId)
                .in(LinkFavoriteEntity::getLinkId, linkIds));
        Set<Long> result = new HashSet<>();
        for (LinkFavoriteEntity favorite : favorites) {
            result.add(favorite.getLinkId());
        }
        return result;
    }

    protected boolean isVisibleToUser(Long tenantId,
                                      Long userId,
                                      LinkItemEntity item,
                                      List<LinkVisibilityTargetEntity> targets) {
        if (!LinkSupport.enabled().equals(item.getStatus())) {
            return false;
        }
        LinkVisibilityScope scope = LinkSupport.toScope(item.getVisibilityScope());
        if (scope == LinkVisibilityScope.PUBLIC) {
            return true;
        }
        if (userId == null) {
            return false;
        }
        if (scope == LinkVisibilityScope.PERSONAL) {
            return userId.equals(item.getOwnerUserId());
        }
        TenantMemberProvider provider = tenantMemberProvider.getIfAvailable();
        if (provider == null) {
            return false;
        }
        TenantMemberInfo member = provider.getEnabledMember(userId, tenantId);
        if (member == null) {
            return false;
        }
        if (scope == LinkVisibilityScope.COMPANY) {
            return true;
        }
        if (scope == LinkVisibilityScope.USER) {
            return targets != null && targets.stream()
                    .anyMatch(target -> LinkVisibilityTargetType.USER.name().equals(target.getTargetType())
                            && userId.equals(target.getTargetId()));
        }
        if (scope == LinkVisibilityScope.DEPARTMENT) {
            return targets != null && targets.stream()
                    .anyMatch(target -> LinkVisibilityTargetType.DEPARTMENT.name().equals(target.getTargetType())
                            && provider.existsOrgRelation(tenantId, member.getMemberId(), target.getTargetId()));
        }
        return false;
    }

    protected boolean keywordMatched(LinkItemEntity item, String keyword) {
        String normalized = LinkContextSupport.trimToNull(keyword);
        if (!StringUtils.hasText(normalized)) {
            return true;
        }
        String lowered = normalized.toLowerCase();
        return contains(item.getName(), lowered)
                || contains(item.getUrl(), lowered)
                || contains(item.getSummary(), lowered)
                || contains(item.getTags(), lowered);
    }

    protected boolean enabledCategory(LinkCategoryEntity category) {
        return category != null && LinkSupport.enabled().equals(category.getStatus());
    }

    protected LinkCategoryVO toCategoryVO(LinkCategoryEntity entity) {
        LinkCategoryVO vo = new LinkCategoryVO();
        vo.setId(entity.getId());
        vo.setName(entity.getName());
        vo.setScope(LinkSupport.toCategoryScope(entity.getScope()));
        vo.setOwnerUserId(entity.getOwnerUserId());
        vo.setOwnerDisplayName(ownerDisplayName(entity.getTenantId(), entity.getOwnerUserId()));
        vo.setSortNo(entity.getSortNo());
        vo.setStatus(LinkSupport.toStatus(entity.getStatus()));
        vo.setRemark(entity.getRemark());
        vo.setCreateTime(entity.getCreatedAt());
        vo.setUpdateTime(entity.getUpdatedAt());
        return vo;
    }

    protected LinkItemVO toItemVO(LinkItemEntity item,
                                  LinkCategoryEntity category,
                                  List<LinkVisibilityTargetEntity> targets) {
        LinkItemVO vo = new LinkItemVO();
        fillNavigationFields(vo, item, category);
        vo.setVisibilityScope(LinkSupport.toScope(item.getVisibilityScope()));
        vo.setOwnerUserId(item.getOwnerUserId());
        vo.setOwnerDisplayName(ownerDisplayName(item.getTenantId(), item.getOwnerUserId()));
        vo.setVisibilityTargets(toTargetVOs(targets));
        vo.setStatus(LinkSupport.toStatus(item.getStatus()));
        vo.setRemark(item.getRemark());
        vo.setCreateTime(item.getCreatedAt());
        vo.setUpdateTime(item.getUpdatedAt());
        return vo;
    }

    protected LinkNavigationItemVO toNavigationVO(LinkItemEntity item,
                                                 LinkCategoryEntity category,
                                                 boolean favorited) {
        LinkNavigationItemVO vo = new LinkNavigationItemVO();
        fillNavigationFields(vo, item, category);
        vo.setFavorited(favorited);
        return vo;
    }

    protected LinkFavoriteVO toFavoriteVO(LinkFavoriteEntity favorite,
                                          LinkItemEntity item,
                                          LinkCategoryEntity category) {
        LinkFavoriteVO vo = new LinkFavoriteVO();
        fillNavigationFields(vo, item, category);
        vo.setFavorited(true);
        vo.setFavoriteId(favorite.getId());
        vo.setFavoriteTime(favorite.getCreatedAt());
        return vo;
    }

    protected LinkPersonalItemVO toPersonalVO(LinkItemEntity item,
                                             LinkCategoryEntity category,
                                             boolean favorited) {
        LinkPersonalItemVO vo = new LinkPersonalItemVO();
        vo.setId(item.getId());
        vo.setCategoryId(item.getCategoryId());
        vo.setCategoryName(category == null ? null : category.getName());
        vo.setName(item.getName());
        vo.setUrl(item.getUrl());
        vo.setSummary(item.getSummary());
        vo.setIconUrl(item.getIconUrl());
        vo.setTags(LinkSupport.splitTags(item.getTags()));
        vo.setRemark(item.getRemark());
        vo.setOpenMode(LinkSupport.toOpenMode(item.getOpenMode()));
        vo.setFavorited(favorited);
        vo.setCreateTime(item.getCreatedAt());
        vo.setUpdateTime(item.getUpdatedAt());
        return vo;
    }

    protected LinkPublicItemVO toPublicVO(LinkItemEntity item, LinkCategoryEntity category) {
        LinkPublicItemVO vo = new LinkPublicItemVO();
        vo.setId(item.getId());
        vo.setCategoryId(item.getCategoryId());
        vo.setCategoryName(category == null ? null : category.getName());
        vo.setName(item.getName());
        vo.setUrl(item.getUrl());
        vo.setSummary(item.getSummary());
        vo.setIconUrl(item.getIconUrl());
        vo.setTags(LinkSupport.splitTags(item.getTags()));
        vo.setOpenMode(LinkSupport.toOpenMode(item.getOpenMode()));
        vo.setRecommended(item.getRecommended());
        vo.setSortNo(item.getSortNo());
        vo.setFavorited(false);
        vo.setSource(LinkNavigationSource.PUBLIC);
        return vo;
    }

    protected List<LinkVisibilityTargetVO> toTargetVOs(List<LinkVisibilityTargetEntity> targets) {
        if (targets == null || targets.isEmpty()) {
            return List.of();
        }
        return targets.stream().map(target -> {
            LinkVisibilityTargetVO vo = new LinkVisibilityTargetVO();
            vo.setId(target.getId());
            vo.setTargetType(LinkSupport.toTargetType(target.getTargetType()));
            vo.setTargetId(target.getTargetId());
            vo.setTargetName(target.getTargetName());
            return vo;
        }).toList();
    }

    protected LinkVisibilityTargetEntity toTargetEntity(Long tenantId, Long linkId, LinkVisibilityTargetCommand command) {
        LinkVisibilityTargetEntity target = new LinkVisibilityTargetEntity();
        target.setTenantId(tenantId);
        target.setLinkId(linkId);
        target.setTargetType(command.getTargetType().name());
        target.setTargetId(command.getTargetId());
        target.setTargetName(LinkContextSupport.trimToNull(command.getTargetName()));
        target.setCreatedAt(LocalDateTime.now());
        return target;
    }

    protected void fillNavigationFields(LinkNavigationItemVO vo, LinkItemEntity item, LinkCategoryEntity category) {
        vo.setId(item.getId());
        vo.setCategoryId(item.getCategoryId());
        vo.setCategoryName(category == null ? null : category.getName());
        vo.setName(item.getName());
        vo.setUrl(item.getUrl());
        vo.setSummary(item.getSummary());
        vo.setIconUrl(item.getIconUrl());
        vo.setTags(LinkSupport.splitTags(item.getTags()));
        vo.setOpenMode(LinkSupport.toOpenMode(item.getOpenMode()));
        vo.setRecommended(item.getRecommended());
        vo.setSortNo(item.getSortNo());
    }

    protected String ownerDisplayName(Long tenantId, Long ownerUserId) {
        if (ownerUserId == null || ownerUserId == 0L) {
            return "企业";
        }
        TenantMemberProvider provider = tenantMemberProvider.getIfAvailable();
        if (provider == null) {
            return String.valueOf(ownerUserId);
        }
        TenantMemberInfo member = provider.getEnabledMember(ownerUserId, tenantId);
        return member == null || !StringUtils.hasText(member.getDisplayName())
                ? String.valueOf(ownerUserId)
                : member.getDisplayName();
    }

    private boolean contains(String value, String keyword) {
        return value != null && value.toLowerCase().contains(keyword);
    }
}
