package io.mango.link.core.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import io.mango.common.result.Require;
import io.mango.common.result.R;
import io.mango.identity.api.TenantMemberProvider;
import io.mango.link.api.enums.LinkNavigationSource;
import io.mango.link.api.enums.LinkVisibilityScope;
import io.mango.link.api.query.LinkPublicItemQuery;
import io.mango.link.api.vo.LinkPublicItemVO;
import io.mango.link.core.entity.LinkAccessRecordEntity;
import io.mango.link.core.entity.LinkCategoryEntity;
import io.mango.link.core.entity.LinkItemEntity;
import io.mango.link.core.entity.LinkVisibilityTargetEntity;
import io.mango.link.core.mapper.LinkAccessRecordMapper;
import io.mango.link.core.mapper.LinkCategoryMapper;
import io.mango.link.core.mapper.LinkFavoriteMapper;
import io.mango.link.core.mapper.LinkItemMapper;
import io.mango.link.core.mapper.LinkVisibilityTargetMapper;
import io.mango.link.core.service.ILinkOpenService;
import io.mango.link.core.support.LinkContextSupport;
import io.mango.link.core.support.LinkSupport;
import io.mango.system.api.SysConfigApi;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Service
public class LinkOpenService extends LinkBaseService implements ILinkOpenService {

    private static final String JUMP_ENABLED_CONFIG_KEY = "mango.link.open.jump.enabled";
    private static final String LEGACY_JUMP_ENABLED_CONFIG_KEY = "link.open.jump.enabled";
    private static final int USER_AGENT_LIMIT = 512;
    private static final int REFERER_LIMIT = 1024;
    private static final int VISITOR_ID_LIMIT = 128;
    private static final int EXTRA_PARAMS_LIMIT = 1024;
    private static final long ANONYMOUS_TENANT_ID = 0L;

    private final LinkAccessRecordMapper accessRecordMapper;
    private final ObjectProvider<SysConfigApi> sysConfigApi;

    public LinkOpenService(LinkCategoryMapper categoryMapper,
                           LinkItemMapper itemMapper,
                           LinkVisibilityTargetMapper targetMapper,
                           LinkFavoriteMapper favoriteMapper,
                           ObjectProvider<TenantMemberProvider> tenantMemberProvider,
                           LinkAccessRecordMapper accessRecordMapper,
                           ObjectProvider<SysConfigApi> sysConfigApi) {
        super(categoryMapper, itemMapper, targetMapper, favoriteMapper, tenantMemberProvider);
        this.accessRecordMapper = accessRecordMapper;
        this.sysConfigApi = sysConfigApi;
    }

    @Override
    public List<LinkPublicItemVO> listPublicItems(LinkPublicItemQuery query) {
        LinkPublicItemQuery resolved = query == null ? new LinkPublicItemQuery() : query;
        Long tenantId = LinkContextSupport.resolveTenantId(resolved.getTenantId());
        Long userId = LinkContextSupport.currentUserIdOrNull();
        if (userId != null) {
            return listVisibleItems(tenantId, userId, resolved);
        }
        List<LinkItemEntity> items = itemMapper.selectList(publicWrapper(tenantId, resolved));
        Map<Long, LinkCategoryEntity> categories = categoriesById(tenantId, items.stream().map(LinkItemEntity::getCategoryId).toList());
        return items.stream()
                .filter(item -> enabledCategory(categories.get(item.getCategoryId())))
                .sorted(Comparator.comparing(LinkItemEntity::getCategoryId, Comparator.nullsLast(Long::compareTo))
                        .thenComparing(LinkItemEntity::getRecommended, Comparator.nullsLast(Comparator.reverseOrder()))
                        .thenComparing(LinkItemEntity::getSortNo, Comparator.nullsLast(Integer::compareTo)))
                .map(item -> toPublicVO(item, categories.get(item.getCategoryId()), LinkNavigationSource.PUBLIC, false))
                .toList();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public String resolveRedirectUrl(Long id, String source, String clientIp, String userAgent, String referer) {
        Require.notNull(id, "网址 ID 不能为空");
        LinkItemEntity item = itemMapper.selectById(id);
        Require.notNull(item, "网址不存在");
        LinkCategoryEntity category = item.getCategoryId() == null
                ? null
                : categoryMapper.selectByTenantAndId(item.getTenantId(), item.getCategoryId());
        Require.isTrue(categoryVisible(item, category), "网址不可见");
        Long userId = LinkContextSupport.currentUserIdOrNull();
        List<LinkVisibilityTargetEntity> targets = targetMapper.selectList(new LambdaQueryWrapper<LinkVisibilityTargetEntity>()
                .eq(LinkVisibilityTargetEntity::getTenantId, item.getTenantId())
                .eq(LinkVisibilityTargetEntity::getLinkId, item.getId()));
        Require.isTrue(isVisibleToUser(item.getTenantId(), userId, item, targets), "网址不可见");
        recordAccess(item, userId, source, clientIp, userAgent, referer);
        return item.getUrl();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public String resolveJumpUrl(String url,
                                 String visitorId,
                                 String source,
                                 String extraParams,
                                 String clientIp,
                                 String userAgent,
                                 String referer) {
        String targetUrl = LinkSupport.normalizeUrl(url);
        Long tenantId = LinkContextSupport.currentTenantIdOrNull();
        Long userId = LinkContextSupport.currentUserIdOrNull();
        LinkItemEntity item = tenantId == null ? null : itemMapper.selectEnabledByTenantAndUrl(tenantId, targetUrl);
        recordAccess(tenantId == null ? ANONYMOUS_TENANT_ID : tenantId, item, targetUrl, userId,
                visitorId, source, extraParams, clientIp, userAgent, referer);
        return targetUrl;
    }

    private List<LinkPublicItemVO> listVisibleItems(Long tenantId, Long userId, LinkPublicItemQuery query) {
        List<LinkItemEntity> items = itemMapper.selectList(visibleWrapper(tenantId, query));
        Map<Long, LinkCategoryEntity> categories = categoriesById(tenantId, items.stream().map(LinkItemEntity::getCategoryId).toList());
        Map<Long, List<LinkVisibilityTargetEntity>> targets = targetsByLinkId(tenantId, items.stream().map(LinkItemEntity::getId).toList());
        Set<Long> favorites = favoriteLinkIds(tenantId, userId, items.stream().map(LinkItemEntity::getId).toList());
        List<LinkItemEntity> visibleItems = items.stream()
                .filter(item -> categoryVisible(item, categories.get(item.getCategoryId())))
                .filter(item -> keywordMatched(item, query.getKeyword()))
                .filter(item -> isVisibleToUser(tenantId, userId, item, targets.get(item.getId())))
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

    private LambdaQueryWrapper<LinkItemEntity> publicWrapper(Long tenantId, LinkPublicItemQuery query) {
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

    private LambdaQueryWrapper<LinkItemEntity> visibleWrapper(Long tenantId, LinkPublicItemQuery query) {
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
        return enabledCategory(category);
    }

    private LinkPublicItemVO toPublicVO(LinkItemEntity item,
                                        LinkCategoryEntity category,
                                        LinkNavigationSource source,
                                        boolean favorited) {
        LinkPublicItemVO vo = toPublicVO(item, category);
        vo.setSource(source);
        vo.setFavorited(favorited);
        vo.setRedirectUrl(jumpEnabled() ? redirectUrl(item, source) : null);
        return vo;
    }

    private boolean jumpEnabled() {
        SysConfigApi api = sysConfigApi.getIfAvailable();
        if (api == null) {
            return false;
        }
        R<String> current = api.getValue(JUMP_ENABLED_CONFIG_KEY);
        if (current != null && current.isSuccess()) {
            return Boolean.parseBoolean(current.getData());
        }
        return configEnabled(api, LEGACY_JUMP_ENABLED_CONFIG_KEY);
    }

    private boolean configEnabled(SysConfigApi api, String configKey) {
        R<Boolean> result = api.getBooleanValue(configKey, false);
        Boolean enabled = result == null ? null : result.getData();
        return Boolean.TRUE.equals(enabled);
    }

    private String redirectUrl(LinkItemEntity item, LinkNavigationSource source) {
        return "/link/open/jump?url=" + URLEncoder.encode(item.getUrl(), StandardCharsets.UTF_8)
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

    private void recordAccess(Long tenantId,
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
        record.setLinkId(item == null ? null : item.getId());
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
