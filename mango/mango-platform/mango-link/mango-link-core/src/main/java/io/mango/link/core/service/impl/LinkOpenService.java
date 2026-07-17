package io.mango.link.core.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import io.mango.common.result.Require;
import io.mango.link.api.enums.LinkCode;
import io.mango.link.api.enums.LinkNavigationSource;
import io.mango.link.api.enums.LinkVisibilityScope;
import io.mango.link.api.query.LinkPublicItemQuery;
import io.mango.link.api.vo.LinkPublicItemVO;
import io.mango.link.core.entity.LinkAccessRecordEntity;
import io.mango.link.core.entity.LinkCategoryEntity;
import io.mango.link.core.entity.LinkItemEntity;
import io.mango.link.core.entity.LinkVisibilityTargetEntity;
import io.mango.link.core.integration.LinkConfigGateway;
import io.mango.link.core.mapper.LinkAccessRecordMapper;
import io.mango.link.core.mapper.LinkCategoryMapper;
import io.mango.link.core.mapper.LinkItemMapper;
import io.mango.link.core.mapper.LinkVisibilityTargetMapper;
import io.mango.link.core.service.ILinkOpenService;
import io.mango.link.core.service.LinkJumpContext;
import io.mango.link.core.service.LinkRedirectContext;
import io.mango.link.core.support.LinkContextSupport;
import io.mango.link.core.support.LinkSupport;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class LinkOpenService implements ILinkOpenService {

    private static final String JUMP_ENABLED_CONFIG_KEY = "mango.link.open.jump.enabled";
    private static final String LEGACY_JUMP_ENABLED_CONFIG_KEY = "link.open.jump.enabled";
    private static final int USER_AGENT_LIMIT = 512;
    private static final int REFERER_LIMIT = 1024;
    private static final int VISITOR_ID_LIMIT = 128;
    private static final int EXTRA_PARAMS_LIMIT = 1024;
    private static final String ANONYMOUS_TENANT_ID = "0";

    private final LinkCategoryMapper categoryMapper;
    private final LinkItemMapper itemMapper;
    private final LinkVisibilityTargetMapper targetMapper;
    private final LinkAccessRecordMapper accessRecordMapper;
    private final LinkConfigGateway configGateway;
    private final LinkServiceSupport support;

    @Override
    public List<LinkPublicItemVO> listPublicItems(LinkPublicItemQuery query) {
        LinkPublicItemQuery resolved = query;
        if (resolved == null) {
            resolved = new LinkPublicItemQuery();
        }
        String tenantId = LinkContextSupport.resolveTenantId(resolved.getTenantId());
        List<LinkItemEntity> items = itemMapper.selectList(publicWrapper(tenantId, resolved));
        Map<Long, LinkCategoryEntity> categories = support.categoriesById(tenantId,
                items.stream().map(LinkItemEntity::getCategoryId).toList());
        return items.stream()
                .filter(item -> support.enabledCategory(categories.get(item.getCategoryId())))
                .sorted(Comparator.comparing(LinkItemEntity::getCategoryId, Comparator.nullsLast(Long::compareTo))
                        .thenComparing(LinkItemEntity::getRecommended, Comparator.nullsLast(Comparator.reverseOrder()))
                        .thenComparing(LinkItemEntity::getSortNo, Comparator.nullsLast(Integer::compareTo)))
                .map(item -> toPublicVO(item, categories.get(item.getCategoryId()), LinkNavigationSource.PUBLIC, false))
                .toList();
    }

    @Override
    public List<LinkPublicItemVO> listVisibleItems(LinkPublicItemQuery query) {
        LinkPublicItemQuery resolved = query;
        if (resolved == null) {
            resolved = new LinkPublicItemQuery();
        }
        return listVisibleItems(LinkContextSupport.currentTenantId(),
                LinkContextSupport.currentUserId(), resolved);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public String resolveRedirectUrl(LinkRedirectContext context) {
        Require.notNull(context, LinkCode.LINK_BUSINESS_ERROR, "跳转上下文不能为空");
        Require.notNull(context.getId(), LinkCode.LINK_BUSINESS_ERROR, "网址 ID 不能为空");
        LinkItemEntity item = itemMapper.selectById(context.getId());
        Require.notNull(item, LinkCode.LINK_BUSINESS_ERROR, "网址不存在");
        String tenantId = LinkContextSupport.currentTenantIdOrNull();
        Require.isTrue(tenantId == null || tenantId.equals(item.getTenantId()),
                LinkCode.LINK_BUSINESS_ERROR, "网址不存在");
        LinkCategoryEntity category = null;
        if (item.getCategoryId() != null) {
            category = categoryMapper.selectOne(new LambdaQueryWrapper<LinkCategoryEntity>()
                    .eq(LinkCategoryEntity::getTenantId, item.getTenantId())
                    .eq(LinkCategoryEntity::getId, item.getCategoryId())
                    .last("LIMIT 1"));
        }
        Require.isTrue(categoryVisible(item, category), LinkCode.LINK_BUSINESS_ERROR, "网址不可见");
        Long userId = LinkContextSupport.currentUserIdOrNull();
        List<LinkVisibilityTargetEntity> targets = targetMapper.selectList(new LambdaQueryWrapper<LinkVisibilityTargetEntity>()
                .eq(LinkVisibilityTargetEntity::getTenantId, item.getTenantId())
                .eq(LinkVisibilityTargetEntity::getLinkId, item.getId()));
        Require.isTrue(support.isVisibleToUser(item.getTenantId(), userId, item, targets),
                LinkCode.LINK_BUSINESS_ERROR, "网址不可见");
        recordAccess(item, userId, context.getSource(), context.getClientIp(),
                context.getUserAgent(), context.getReferer());
        return item.getUrl();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public String resolveJumpUrl(LinkJumpContext context) {
        Require.notNull(context, LinkCode.LINK_BUSINESS_ERROR, "跳转上下文不能为空");
        String targetUrl = LinkSupport.normalizeUrl(context.getUrl());
        String tenantId = LinkContextSupport.currentTenantIdOrNull();
        Long userId = LinkContextSupport.currentUserIdOrNull();
        LinkItemEntity item = null;
        String accessTenantId = ANONYMOUS_TENANT_ID;
        if (tenantId != null) {
            item = itemMapper.selectOne(new LambdaQueryWrapper<LinkItemEntity>()
                    .eq(LinkItemEntity::getTenantId, tenantId)
                    .eq(LinkItemEntity::getUrl, targetUrl)
                    .eq(LinkItemEntity::getStatus, LinkSupport.enabled())
                    .last("LIMIT 1"));
            accessTenantId = tenantId;
        }
        recordAccess(accessTenantId, item, targetUrl, userId,
                context.getVisitorId(), context.getSource(), context.getExtraParams(), context.getClientIp(),
                context.getUserAgent(), context.getReferer());
        return targetUrl;
    }

    private List<LinkPublicItemVO> listVisibleItems(String tenantId, Long userId, LinkPublicItemQuery query) {
        List<LinkItemEntity> items = itemMapper.selectList(visibleWrapper(tenantId, query));
        Map<Long, LinkCategoryEntity> categories = support.categoriesById(tenantId,
                items.stream().map(LinkItemEntity::getCategoryId).toList());
        Map<Long, List<LinkVisibilityTargetEntity>> targets = support.targetsByLinkId(tenantId,
                items.stream().map(LinkItemEntity::getId).toList());
        Set<Long> favorites = support.favoriteLinkIds(tenantId, userId,
                items.stream().map(LinkItemEntity::getId).toList());
        List<LinkItemEntity> visibleItems = items.stream()
                .filter(item -> categoryVisible(item, categories.get(item.getCategoryId())))
                .filter(item -> support.keywordMatched(item, query.getKeyword()))
                .filter(item -> support.isVisibleToUser(tenantId, userId, item, targets.get(item.getId())))
                .sorted(navigationComparator())
                .toList();

        List<LinkPublicItemVO> result = new ArrayList<>();
        for (LinkItemEntity item : visibleItems) {
            if (!LinkVisibilityScope.PERSONAL.name().equals(item.getVisibilityScope())) {
                result.add(toPublicVO(item, categories.get(item.getCategoryId()),
                        LinkNavigationSource.COMPANY, favorites.contains(item.getId())));
            }
        }
        for (LinkItemEntity item : visibleItems) {
            if (favorites.contains(item.getId())) {
                result.add(toPublicVO(item, categories.get(item.getCategoryId()),
                        LinkNavigationSource.FAVORITE, true));
            }
        }
        for (LinkItemEntity item : visibleItems) {
            if (LinkVisibilityScope.PERSONAL.name().equals(item.getVisibilityScope())) {
                result.add(toPublicVO(item, categories.get(item.getCategoryId()),
                        LinkNavigationSource.PERSONAL, favorites.contains(item.getId())));
            }
        }
        return result;
    }

    private LambdaQueryWrapper<LinkItemEntity> publicWrapper(String tenantId, LinkPublicItemQuery query) {
        LambdaQueryWrapper<LinkItemEntity> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(LinkItemEntity::getTenantId, tenantId)
                .eq(LinkItemEntity::getStatus, LinkSupport.enabled())
                .eq(LinkItemEntity::getVisibilityScope, LinkVisibilityScope.PUBLIC.name());
        if (query.getCategoryId() != null) {
            wrapper.eq(LinkItemEntity::getCategoryId, query.getCategoryId());
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
        return wrapper;
    }

    private LambdaQueryWrapper<LinkItemEntity> visibleWrapper(String tenantId, LinkPublicItemQuery query) {
        LambdaQueryWrapper<LinkItemEntity> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(LinkItemEntity::getTenantId, tenantId)
                .eq(LinkItemEntity::getStatus, LinkSupport.enabled());
        if (query.getCategoryId() != null) {
            wrapper.eq(LinkItemEntity::getCategoryId, query.getCategoryId());
        }
        return wrapper;
    }

    private boolean categoryVisible(LinkItemEntity item, LinkCategoryEntity category) {
        if (LinkVisibilityScope.PERSONAL.name().equals(item.getVisibilityScope()) && item.getCategoryId() == null) {
            return true;
        }
        return support.enabledCategory(category);
    }

    private LinkPublicItemVO toPublicVO(LinkItemEntity item,
                                        LinkCategoryEntity category,
                                        LinkNavigationSource source,
                                        boolean favorited) {
        LinkPublicItemVO vo = support.toPublicVO(item, category);
        vo.setSource(source);
        vo.setFavorited(favorited);
        String resolvedRedirectUrl = null;
        if (jumpEnabled()) {
            resolvedRedirectUrl = redirectUrl(item, source);
        }
        vo.setRedirectUrl(resolvedRedirectUrl);
        return vo;
    }

    private boolean jumpEnabled() {
        return configGateway.booleanValue(JUMP_ENABLED_CONFIG_KEY, LEGACY_JUMP_ENABLED_CONFIG_KEY);
    }

    private String redirectUrl(LinkItemEntity item, LinkNavigationSource source) {
        String path = source == LinkNavigationSource.PUBLIC ? "/link/open/jump" : "/link/visible-links/jump";
        return path + "?url=" + URLEncoder.encode(item.getUrl(), StandardCharsets.UTF_8)
                + "&source=" + source.name();
    }

    private void recordAccess(LinkItemEntity item,
                              Long userId,
                              String source,
                              String clientIp,
                              String userAgent,
                              String referer) {
        recordAccess(item.getTenantId(), item, item.getUrl(), userId, null, source, null, clientIp, userAgent, referer);
    }

    private void recordAccess(String tenantId,
                              LinkItemEntity item,
                              String url,
                              Long userId,
                              String visitorId,
                              String source,
                              String extraParams,
                              String clientIp,
                              String userAgent,
                              String referer) {
        LinkAccessRecordEntity record = new LinkAccessRecordEntity();
        record.setTenantId(tenantId);
        Long linkId = null;
        if (item != null) {
            linkId = item.getId();
        }
        record.setLinkId(linkId);
        record.setUrl(limit(url, REFERER_LIMIT));
        record.setUserId(userId);
        record.setVisitorId(limit(visitorId, VISITOR_ID_LIMIT));
        record.setSource(LinkContextSupport.trimToNull(source));
        record.setExtraParams(limit(extraParams, EXTRA_PARAMS_LIMIT));
        record.setClientIp(LinkContextSupport.trimToNull(clientIp));
        record.setUserAgent(limit(userAgent, USER_AGENT_LIMIT));
        record.setReferer(limit(referer, REFERER_LIMIT));
        record.setAccessTime(LocalDateTime.now());
        accessRecordMapper.insert(record);
    }

    private String limit(String value, int limit) {
        String normalized = LinkContextSupport.trimToNull(value);
        if (normalized == null || normalized.length() <= limit) {
            return normalized;
        }
        return normalized.substring(0, limit);
    }

    private Comparator<LinkItemEntity> navigationComparator() {
        return Comparator.comparing(LinkItemEntity::getCategoryId, Comparator.nullsLast(Long::compareTo))
                .thenComparing(LinkItemEntity::getRecommended, Comparator.nullsLast(Comparator.reverseOrder()))
                .thenComparing(LinkItemEntity::getSortNo, Comparator.nullsLast(Integer::compareTo))
                .thenComparing(LinkItemEntity::getUpdatedAt, Comparator.nullsLast(Comparator.reverseOrder()));
    }
}
