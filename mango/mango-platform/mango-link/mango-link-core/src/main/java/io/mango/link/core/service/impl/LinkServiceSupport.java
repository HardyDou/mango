package io.mango.link.core.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import io.mango.common.result.Require;
import io.mango.identity.api.TenantMemberProvider;
import io.mango.identity.api.vo.TenantMemberVO;
import io.mango.link.api.command.LinkVisibilityTargetCommand;
import io.mango.link.api.enums.LinkNavigationSource;
import io.mango.link.api.enums.LinkCode;
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
import org.springframework.stereotype.Component;
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
@Component
public class LinkServiceSupport {

    private final LinkCategoryMapper categoryMapper;
    private final LinkItemMapper itemMapper;
    private final LinkVisibilityTargetMapper targetMapper;
    private final LinkFavoriteMapper favoriteMapper;
    private final ObjectProvider<TenantMemberProvider> tenantMemberProvider;

    public LinkCategoryEntity selectCategoryRequired(String tenantId, Long categoryId) {
        LinkCategoryEntity category = categoryMapper.selectOne(new LambdaQueryWrapper<LinkCategoryEntity>()
                .eq(LinkCategoryEntity::getTenantId, tenantId)
                .eq(LinkCategoryEntity::getId, categoryId)
                .last("LIMIT 1"));
        Require.notNull(category, LinkCode.LINK_BUSINESS_ERROR, "网址分类不存在");
        return category;
    }

    public LinkItemEntity selectItemRequired(String tenantId, Long id) {
        LinkItemEntity item = itemMapper.selectOne(new LambdaQueryWrapper<LinkItemEntity>()
                .eq(LinkItemEntity::getTenantId, tenantId)
                .eq(LinkItemEntity::getId, id)
                .last("LIMIT 1"));
        Require.notNull(item, LinkCode.LINK_BUSINESS_ERROR, "网址不存在");
        return item;
    }

    public void requireEnabledCategory(String tenantId, Long categoryId) {
        LinkCategoryEntity category = selectCategoryRequired(tenantId, categoryId);
        Require.isTrue(LinkSupport.enabled().equals(category.getStatus()),
                LinkCode.LINK_BUSINESS_ERROR, "网址分类已停用");
    }

    public Map<Long, LinkCategoryEntity> categoriesById(String tenantId, Collection<Long> categoryIds) {
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
        Map<Long, LinkCategoryEntity> result = new HashMap<>(categories.size());
        for (LinkCategoryEntity category : categories) {
            result.put(category.getId(), category);
        }
        return result;
    }

    public Map<Long, List<LinkVisibilityTargetEntity>> targetsByLinkId(String tenantId, Collection<Long> linkIds) {
        if (linkIds == null || linkIds.isEmpty()) {
            return Map.of();
        }
        return targetMapper.selectList(new LambdaQueryWrapper<LinkVisibilityTargetEntity>()
                        .eq(LinkVisibilityTargetEntity::getTenantId, tenantId)
                        .in(LinkVisibilityTargetEntity::getLinkId, linkIds))
                .stream()
                .collect(java.util.stream.Collectors.groupingBy(LinkVisibilityTargetEntity::getLinkId));
    }

    public Set<Long> favoriteLinkIds(String tenantId, Long userId, Collection<Long> linkIds) {
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

    public boolean isVisibleToUser(String tenantId,
                                      Long userId,
                                      LinkItemEntity item,
                                      List<LinkVisibilityTargetEntity> targets) {
        if (!LinkSupport.enabled().equals(item.getStatus())) {
            return false;
        }
        LinkVisibilityScope scope = LinkSupport.toScope(item.getVisibilityScope());
        return visibleForScope(tenantId, userId, item, targets, scope);
    }

    private boolean visibleForScope(String tenantId,
                                    Long userId,
                                    LinkItemEntity item,
                                    List<LinkVisibilityTargetEntity> targets,
                                    LinkVisibilityScope scope) {
        if (scope == LinkVisibilityScope.PUBLIC) {
            return true;
        }
        if (userId == null) {
            return false;
        }
        if (scope == LinkVisibilityScope.PERSONAL) {
            return userId.equals(item.getOwnerUserId());
        }
        return visibleForMemberScope(tenantId, userId, targets, scope);
    }

    private boolean visibleForMemberScope(String tenantId,
                                          Long userId,
                                          List<LinkVisibilityTargetEntity> targets,
                                          LinkVisibilityScope scope) {
        TenantMemberProvider provider = tenantMemberProvider.getIfAvailable();
        if (provider == null) {
            return false;
        }
        Long numericTenantId = numericTenantId(tenantId);
        TenantMemberVO member = provider.getEnabledMember(userId, numericTenantId);
        if (member == null) {
            return false;
        }
        if (scope == LinkVisibilityScope.COMPANY) {
            return true;
        }
        return switch (scope) {
            case USER -> visibleToUserTarget(userId, targets);
            case DEPARTMENT -> visibleToDepartmentTarget(numericTenantId, member.getMemberId(), targets, provider);
            default -> false;
        };
    }

    public boolean keywordMatched(LinkItemEntity item, String keyword) {
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

    public boolean enabledCategory(LinkCategoryEntity category) {
        return category != null && LinkSupport.enabled().equals(category.getStatus());
    }

    public LinkCategoryVO toCategoryVO(LinkCategoryEntity entity) {
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

    public LinkItemVO toItemVO(LinkItemEntity item,
                                  LinkCategoryEntity category,
                                  List<LinkVisibilityTargetEntity> targets) {
        LinkItemVO vo = new LinkItemVO();
        fillNavigationFields(vo, item, category);
        vo.setVisibilityScope(LinkSupport.toScope(item.getVisibilityScope()));
        vo.setOwnerUserId(item.getOwnerUserId());
        vo.setOwnerDisplayName(ownerDisplayName(item.getTenantId(), item.getOwnerUserId()));
        vo.setVisibilityTargets(toTargetVos(targets));
        vo.setStatus(LinkSupport.toStatus(item.getStatus()));
        vo.setRemark(item.getRemark());
        vo.setCreateTime(item.getCreatedAt());
        vo.setUpdateTime(item.getUpdatedAt());
        return vo;
    }

    public LinkNavigationItemVO toNavigationVO(LinkItemEntity item,
                                                 LinkCategoryEntity category,
                                                 boolean favorited) {
        LinkNavigationItemVO vo = new LinkNavigationItemVO();
        fillNavigationFields(vo, item, category);
        vo.setFavorited(favorited);
        return vo;
    }

    public LinkFavoriteVO toFavoriteVO(LinkFavoriteEntity favorite,
                                          LinkItemEntity item,
                                          LinkCategoryEntity category) {
        LinkFavoriteVO vo = new LinkFavoriteVO();
        fillNavigationFields(vo, item, category);
        vo.setFavorited(true);
        vo.setFavoriteId(favorite.getId());
        vo.setFavoriteTime(favorite.getCreatedAt());
        return vo;
    }

    public LinkPersonalItemVO toPersonalVO(LinkItemEntity item,
                                             LinkCategoryEntity category,
                                             boolean favorited) {
        LinkPersonalItemVO vo = new LinkPersonalItemVO();
        vo.setId(item.getId());
        vo.setCategoryId(item.getCategoryId());
        vo.setCategoryName(categoryName(category));
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

    public LinkPublicItemVO toPublicVO(LinkItemEntity item, LinkCategoryEntity category) {
        LinkPublicItemVO vo = new LinkPublicItemVO();
        vo.setId(item.getId());
        vo.setCategoryId(item.getCategoryId());
        vo.setCategoryName(categoryName(category));
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

    public List<LinkVisibilityTargetVO> toTargetVos(List<LinkVisibilityTargetEntity> targets) {
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

    public LinkVisibilityTargetEntity toTargetEntity(String tenantId, Long linkId, LinkVisibilityTargetCommand command) {
        LinkVisibilityTargetEntity target = new LinkVisibilityTargetEntity();
        target.setTenantId(tenantId);
        target.setLinkId(linkId);
        target.setTargetType(command.getTargetType().name());
        target.setTargetId(command.getTargetId());
        target.setTargetName(LinkContextSupport.trimToNull(command.getTargetName()));
        target.setCreatedAt(LocalDateTime.now());
        return target;
    }

    public void fillNavigationFields(LinkNavigationItemVO vo, LinkItemEntity item, LinkCategoryEntity category) {
        vo.setId(item.getId());
        vo.setCategoryId(item.getCategoryId());
        vo.setCategoryName(categoryName(category));
        vo.setName(item.getName());
        vo.setUrl(item.getUrl());
        vo.setSummary(item.getSummary());
        vo.setIconUrl(item.getIconUrl());
        vo.setTags(LinkSupport.splitTags(item.getTags()));
        vo.setOpenMode(LinkSupport.toOpenMode(item.getOpenMode()));
        vo.setRecommended(item.getRecommended());
        vo.setSortNo(item.getSortNo());
    }

    public String ownerDisplayName(String tenantId, Long ownerUserId) {
        if (ownerUserId == null || ownerUserId == 0L) {
            return "企业";
        }
        TenantMemberProvider provider = tenantMemberProvider.getIfAvailable();
        if (provider == null) {
            return String.valueOf(ownerUserId);
        }
        TenantMemberVO member = provider.getEnabledMember(ownerUserId, numericTenantId(tenantId));
        if (member == null || !StringUtils.hasText(member.getDisplayName())) {
            return String.valueOf(ownerUserId);
        }
        return member.getDisplayName();
    }

    private boolean visibleToUserTarget(Long userId, List<LinkVisibilityTargetEntity> targets) {
        return targets != null && targets.stream()
                .anyMatch(target -> LinkVisibilityTargetType.USER.name().equals(target.getTargetType())
                        && userId.equals(target.getTargetId()));
    }

    private boolean visibleToDepartmentTarget(Long tenantId,
                                              Long memberId,
                                              List<LinkVisibilityTargetEntity> targets,
                                              TenantMemberProvider provider) {
        return targets != null && targets.stream()
                .anyMatch(target -> LinkVisibilityTargetType.DEPARTMENT.name().equals(target.getTargetType())
                        && provider.existsOrgRelation(tenantId, memberId, target.getTargetId()));
    }

    private String categoryName(LinkCategoryEntity category) {
        if (category == null) {
            return null;
        }
        return category.getName();
    }

    private boolean contains(String value, String keyword) {
        return value != null && value.toLowerCase().contains(keyword);
    }

    private Long numericTenantId(String tenantId) {
        try {
            return Long.valueOf(tenantId);
        } catch (NumberFormatException exception) {
            return Require.fail(LinkCode.LINK_BUSINESS_ERROR.getCode(), "当前机构上下文不是有效数字: " + tenantId);
        }
    }
}
