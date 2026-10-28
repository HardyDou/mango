package io.mango.authorization.core.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import io.mango.authorization.api.enums.AuthorizationCode;
import io.mango.authorization.api.command.ApiResourceManualCommand;
import io.mango.authorization.api.query.ApiResourceManualQuery;
import io.mango.authorization.api.query.ApiResourcePageQuery;
import io.mango.authorization.api.vo.ApiResourceAccessDecisionVO;
import io.mango.authorization.api.vo.ApiResourceFilterOptionsVO;
import io.mango.authorization.api.vo.ApiResourceVO;
import io.mango.authorization.api.command.ApiResourceRegisterCommand;
import io.mango.authorization.api.vo.ApiResourceRegisterResultVO;
import io.mango.authorization.api.enums.ApiResourceAccessMode;
import io.mango.authorization.api.enums.ApiResourceSource;
import io.mango.authorization.core.entity.ApiResourceEntity;
import io.mango.authorization.core.mapper.ApiResourceMapper;
import io.mango.authorization.core.service.IApiResourceService;
import io.mango.common.result.Require;
import io.mango.common.vo.PageResult;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.AntPathMatcher;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * API 资源服务实现。
 *
 * @author hardy
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ApiResourceService implements IApiResourceService {

    private static final AntPathMatcher PATH_MATCHER = new AntPathMatcher();
    private static final int MAX_DECISION_CACHE_SIZE = 10_000;
    private static final int MAX_REGISTER_BATCH_SIZE = 10_000;
    private static final String CONFIGURED_HANDLER_CLASS = "configuration";
    private static final int MANUAL_OVERRIDE_OFF = 0;
    private static final int MANUAL_OVERRIDE_ON = 1;
    private static final String ROOT_DOMAIN = "root";
    /** 人工登记是运行时最高控制来源，兼容既有 source=MANUAL 数据。 */
    private static final int SOURCE_RANK_MANUAL = 3;
    private static final int SOURCE_RANK_FIXED = 2;
    private static final int SOURCE_RANK_SCAN = 1;
    private static final int HANDLER_RANK_CONCRETE = 2;
    private static final int HANDLER_RANK_CONFIGURED = 1;
    private static final int HANDLER_RANK_NONE = 0;
    private static final Comparator<ApiResourceEntity> ROUTE_PRECEDENCE_ENTITY = Comparator
            .comparingInt((ApiResourceEntity resource) -> -sourceRank(resource.getSource()))
            .thenComparingLong(ApiResourceEntity::getId);

    private static final Comparator<RouteCandidate> ROUTE_PRECEDENCE = Comparator
            .comparingInt((RouteCandidate candidate) -> -sourceRank(candidate.source()))
            .thenComparingInt(candidate -> -handlerRank(candidate.handlerClass()))
            .thenComparingInt(candidate -> candidate.inBatch() ? 0 : 1)
            .thenComparing(candidate -> candidate.moduleName() == null ? "" : candidate.moduleName())
            .thenComparingLong(candidate -> candidate.id() == null ? Long.MAX_VALUE : candidate.id());

    private final ApiResourceMapper apiResourceMapper;

    private final Map<String, ApiResourceAccessDecisionVO> decisionCache = new ConcurrentHashMap<>();
    private final ConcurrentLinkedQueue<String> decisionCacheKeys = new ConcurrentLinkedQueue<>();
    private volatile List<ApiResourceEntity> activeResourceCache;
    @Override
    @Transactional(rollbackFor = Exception.class)
    public ApiResourceRegisterResultVO registerApiResources(List<ApiResourceRegisterCommand> resources) {
        Require.isTrue(resources == null || resources.size() <= MAX_REGISTER_BATCH_SIZE,
                AuthorizationCode.AUTHORIZATION_BUSINESS_ERROR, "API 资源批量注册数量不能超过10000条");
        return registerApiResources(resources, true);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public ApiResourceRegisterResultVO upsertApiResources(List<ApiResourceRegisterCommand> resources) {
        Require.isTrue(resources == null || resources.size() <= MAX_REGISTER_BATCH_SIZE,
                AuthorizationCode.AUTHORIZATION_BUSINESS_ERROR, "API 资源批量注册数量不能超过10000条");
        return registerApiResources(resources, false);
    }

    private ApiResourceRegisterResultVO registerApiResources(
            List<ApiResourceRegisterCommand> resources, boolean disableStaleResources) {
        if (resources == null || resources.isEmpty()) {
            return ApiResourceRegisterResultVO.empty();
        }

        List<ApiResourceRegisterCommand> validResources = resources.stream()
                .filter(resource -> {
                    boolean valid = isValid(resource);
                    if (!valid) {
                        log.warn("Skip invalid API resource: {}", resource);
                    }
                    return valid;
                })
                .toList();
        if (validResources.isEmpty()) {
            return new ApiResourceRegisterResultVO(0, 0, 0);
        }
        int scannedCount = validResources.size();
        validResources = deduplicate(validResources);
        validResources = excludeCoveredByActiveFixed(validResources);

        Set<String> currentResourceKeys = validResources.stream()
                .map(resource -> resourceKey(
                        resource.getModuleName(),
                        resource.getHttpMethod(),
                        resource.getPathPattern()))
                .collect(Collectors.toSet());
        Map<String, ApiResourceEntity> existingIndex = loadExistingIndex(validResources);
        List<ApiResourceEntity> creates = new ArrayList<>();
        List<ApiResourceEntity> updates = new ArrayList<>();
        for (ApiResourceRegisterCommand resource : validResources) {
            ApiResourceEntity existing = existingIndex.get(resourceKey(
                    resource.getModuleName(),
                    resource.getHttpMethod(),
                    resource.getPathPattern()));
            if (existing == null) {
                creates.add(toEntity(resource));
            } else if (isSoftDeleted(existing)) {
                reviveSoftDeleted(existing, resource);
            } else if (isManualOverride(existing)) {
                log.info("API resource registration skipped by manual override: httpMethod={}, path={}, module={}",
                        resource.getHttpMethod(), resource.getPathPattern(), existing.getModuleName());
            } else {
                merge(existing, resource);
                updates.add(existing);
            }
        }
        List<ApiResourceEntity> staleResources = disableStaleResources
                ? loadStaleAutoScannedResources(validResources, currentResourceKeys) : List.of();
        List<ApiResourceEntity> duplicateRouteResources =
                resolveRouteOwnershipConflicts(validResources, creates, updates);
        staleResources.forEach(resource -> resource.setStatus(0));
        duplicateRouteResources.forEach(resource -> resource.setStatus(0));
        updates.addAll(staleResources);
        updates.addAll(duplicateRouteResources);
        updates = deduplicateEntities(updates);
        if (!creates.isEmpty()) {
            creates.forEach(apiResourceMapper::insert);
        }
        if (!updates.isEmpty()) {
            updates.forEach(apiResourceMapper::updateById);
        }
        if (!staleResources.isEmpty()) {
            log.info("API resource stale entries disabled: count={}", staleResources.size());
        }
        if (!duplicateRouteResources.isEmpty()) {
            log.info("API resource shadowed route entries disabled: count={}", duplicateRouteResources.size());
        }
        clearRuntimeCache();
        return new ApiResourceRegisterResultVO(scannedCount, creates.size(), updates.size());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean deleteApiResource(Long resourceId) {
        Require.notNull(resourceId, AuthorizationCode.AUTHORIZATION_BUSINESS_ERROR, "API资源ID不能为空");
        boolean deleted = apiResourceMapper.deleteById(resourceId) > 0;
        clearRuntimeCache();
        return deleted;
    }

    @Override
    public ApiResourceAccessDecisionVO resolveAccessDecision(String httpMethod, String path) {
        if (!StringUtils.hasText(httpMethod) || !StringUtils.hasText(path)) {
            return ApiResourceAccessDecisionVO.unmatched(ApiResourceAccessMode.LOGIN);
        }
        String method = httpMethod.toUpperCase();
        String cacheKey = method + "\n" + path;
        ApiResourceAccessDecisionVO cached = decisionCache.get(cacheKey);
        if (cached != null) {
            return cached;
        }
        ApiResourceAccessDecisionVO decision = activeResources().stream()
                .filter(resource -> methodMatches(resource.getHttpMethod(), method))
                .sorted(resourceMatchComparator(path))
                .filter(resource -> pathMatches(resource.getPathPattern(), path))
                .findFirst()
                .map(this::toDecision)
                .orElseGet(() -> ApiResourceAccessDecisionVO.unmatched(ApiResourceAccessMode.LOGIN));
        cacheDecision(cacheKey, decision);
        return decision;
    }

    @Override
    public void refreshRuntimeCache() {
        clearRuntimeCache();
    }

    @Override
    public PageResult<ApiResourceVO> pageApiResources(ApiResourcePageQuery query) {
        ApiResourcePageQuery validated = Require.nonNull(
                query, AuthorizationCode.AUTHORIZATION_BUSINESS_ERROR, "API 资源分页查询条件不能为空");
        Page<ApiResourceEntity> page = apiResourceMapper.selectPage(
                Page.of(validated.getPage(), validated.getSize()),
                pageWrapper(validated));
        return PageResult.of(page.getRecords().stream().map(this::toVo).toList(),
                page.getTotal(), page.getCurrent(), page.getSize());
    }

    @Override
    public ApiResourceFilterOptionsVO listApiResourceFilterOptions() {
        ApiResourceFilterOptionsVO options = new ApiResourceFilterOptionsVO();
        options.setModules(distinctColumn(ApiResourceEntity::getModuleName));
        options.setDomains(distinctColumn(ApiResourceEntity::getDomain));
        return options;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void saveManualApiResource(ApiResourceManualCommand command) {
        ApiResourceManualCommand validated = Require.nonNull(
                command, AuthorizationCode.AUTHORIZATION_BUSINESS_ERROR, "人工接管 API 资源命令不能为空");
        String httpMethod = validated.getHttpMethod().trim().toUpperCase();
        ApiResourceEntity entity = requireOverridableResource(validated, httpMethod);
        if (validated.getAccessMode() == ApiResourceAccessMode.PERMISSION) {
            boolean hasPermissionCode = StringUtils.hasText(validated.getPermissionCode())
                    || StringUtils.hasText(entity.getPermissionCode())
                    || StringUtils.hasText(entity.getOriginPermissionCode());
            Require.isTrue(hasPermissionCode,
                    AuthorizationCode.AUTHORIZATION_BUSINESS_ERROR, "访问模式 PERMISSION 必须声明权限码");
        }
        if (!isManualOverride(entity)) {
            entity.setOriginSource(entity.getSource());
            entity.setOriginAccessMode(entity.getAccessMode());
            entity.setOriginPermissionCode(entity.getPermissionCode());
            entity.setOriginDescription(entity.getDescription());
        }
        entity.setManualOverride(MANUAL_OVERRIDE_ON);
        entity.setAccessMode(validated.getAccessMode().name());
        if (StringUtils.hasText(validated.getPermissionCode())) {
            entity.setPermissionCode(validated.getPermissionCode());
        }
        if (StringUtils.hasText(validated.getDescription())) {
            entity.setDescription(validated.getDescription());
        }
        entity.setStatus(1);
        entity.setDeleted(0);
        apiResourceMapper.updateById(entity);
        clearRuntimeCache();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void releaseManualApiResource(ApiResourceManualQuery query) {
        ApiResourceManualQuery validated = Require.nonNull(
                query, AuthorizationCode.AUTHORIZATION_BUSINESS_ERROR, "释放人工接管查询条件不能为空");
        String httpMethod = validated.getHttpMethod().trim().toUpperCase();
        ApiResourceEntity entity = requireManualOverride(validated, httpMethod);
        if (!StringUtils.hasText(entity.getOriginSource())) {
            // 没有原始声明的人工资源，解除后不再保留记录。
            apiResourceMapper.deleteById(entity.getId());
            clearRuntimeCache();
            return;
        }
        apiResourceMapper.update(null, new LambdaUpdateWrapper<ApiResourceEntity>()
                .eq(ApiResourceEntity::getId, entity.getId())
                .set(ApiResourceEntity::getSource, entity.getOriginSource())
                .set(ApiResourceEntity::getAccessMode, entity.getOriginAccessMode())
                .set(ApiResourceEntity::getPermissionCode, entity.getOriginPermissionCode())
                .set(ApiResourceEntity::getDescription, entity.getOriginDescription())
                .set(ApiResourceEntity::getManualOverride, MANUAL_OVERRIDE_OFF)
                .set(ApiResourceEntity::getOriginSource, null)
                .set(ApiResourceEntity::getOriginAccessMode, null)
                .set(ApiResourceEntity::getOriginPermissionCode, null)
                .set(ApiResourceEntity::getOriginDescription, null));
        clearRuntimeCache();
    }

    /**
     * 人工接管只覆盖访问模式与权限标识，不改变模块归属、资源编码和处理入口。
     */
    private ApiResourceEntity requireOverridableResource(ApiResourceManualCommand command, String httpMethod) {
        List<ApiResourceEntity> candidates = apiResourceMapper.selectList(
                new LambdaQueryWrapper<ApiResourceEntity>()
                        .eq(ApiResourceEntity::getHttpMethod, httpMethod)
                        .eq(ApiResourceEntity::getPathPattern, command.getPathPattern())
                        .eq(ApiResourceEntity::getStatus, 1));
        return candidates.stream()
                .filter(resource -> command.getModuleName().equals(resource.getModuleName()))
                .findFirst()
                .or(() -> candidates.stream().filter(this::isManualOverride).findFirst())
                .or(() -> candidates.stream().min(ROUTE_PRECEDENCE_ENTITY))
                .orElseThrow(() -> new IllegalStateException("API 资源不存在，请先完成资源同步后再接管: "
                        + httpMethod + " " + command.getPathPattern()));
    }

    private ApiResourceEntity requireManualOverride(ApiResourceManualQuery query, String httpMethod) {
        List<ApiResourceEntity> candidates = apiResourceMapper.selectList(
                new LambdaQueryWrapper<ApiResourceEntity>()
                        .eq(ApiResourceEntity::getHttpMethod, httpMethod)
                        .eq(ApiResourceEntity::getPathPattern, query.getPathPattern())
                        .eq(ApiResourceEntity::getManualOverride, MANUAL_OVERRIDE_ON));
        return candidates.stream()
                .filter(resource -> query.getModuleName().equals(resource.getModuleName()))
                .findFirst()
                .or(() -> candidates.stream().findFirst())
                .orElseThrow(() -> new IllegalStateException("未找到可解除的人工接管资源: "
                        + httpMethod + " " + query.getPathPattern()));
    }

    private boolean isManualOverride(ApiResourceEntity resource) {
        return resource.getManualOverride() != null && resource.getManualOverride() == MANUAL_OVERRIDE_ON;
    }

    private List<String> distinctColumn(
            com.baomidou.mybatisplus.core.toolkit.support.SFunction<ApiResourceEntity, String> column) {
        return apiResourceMapper.selectList(new LambdaQueryWrapper<ApiResourceEntity>()
                        .select(column)
                        .groupBy(column))
                .stream()
                .map(column)
                .filter(StringUtils::hasText)
                .sorted()
                .toList();
    }

    private LambdaQueryWrapper<ApiResourceEntity> pageWrapper(ApiResourcePageQuery query) {
        LambdaQueryWrapper<ApiResourceEntity> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(StringUtils.hasText(query.getModuleName()),
                ApiResourceEntity::getModuleName, query.getModuleName());
        wrapper.eq(StringUtils.hasText(query.getDomain()), ApiResourceEntity::getDomain, query.getDomain());
        wrapper.eq(query.getSource() != null, ApiResourceEntity::getSource, query.getSource() == null
                ? null : query.getSource().name());
        wrapper.eq(StringUtils.hasText(query.getAccessMode()),
                ApiResourceEntity::getAccessMode, query.getAccessMode());
        wrapper.eq(query.getStatus() != null, ApiResourceEntity::getStatus, query.getStatus());
        if (StringUtils.hasText(query.getKeyword())) {
            wrapper.and(nested -> nested
                    .like(ApiResourceEntity::getPathPattern, query.getKeyword())
                    .or().like(ApiResourceEntity::getResourceCode, query.getKeyword())
                    .or().like(ApiResourceEntity::getPermissionCode, query.getKeyword())
                    .or().like(ApiResourceEntity::getHandlerClass, query.getKeyword()));
        }
        wrapper.orderByAsc(ApiResourceEntity::getDomain)
                .orderByAsc(ApiResourceEntity::getPathPattern)
                .orderByAsc(ApiResourceEntity::getHttpMethod);
        return wrapper;
    }

    private ApiResourceVO toVo(ApiResourceEntity entity) {
        ApiResourceVO vo = new ApiResourceVO();
        vo.setId(entity.getId());
        vo.setModuleName(entity.getModuleName());
        vo.setDomain(entity.getDomain());
        vo.setSource(parseSource(entity.getSource()));
        vo.setManualOverride(isManualOverride(entity));
        vo.setOriginAccessMode(entity.getOriginAccessMode());
        vo.setHttpMethod(entity.getHttpMethod());
        vo.setPathPattern(entity.getPathPattern());
        vo.setResourceCode(entity.getResourceCode());
        vo.setPermissionCode(entity.getPermissionCode());
        vo.setAccessMode(entity.getAccessMode());
        vo.setHandlerClass(entity.getHandlerClass());
        vo.setHandlerMethod(entity.getHandlerMethod());
        vo.setDescription(entity.getDescription());
        vo.setStatus(entity.getStatus());
        vo.setCreateTime(entity.getCreateTime());
        vo.setUpdateTime(entity.getUpdateTime());
        return vo;
    }

    private ApiResourceSource parseSource(String value) {
        if (!StringUtils.hasText(value)) {
            return ApiResourceSource.FIXED_DECLARATION;
        }
        try {
            return ApiResourceSource.valueOf(value);
        } catch (IllegalArgumentException exception) {
            log.warn("Unknown API resource source: {}", value);
            return ApiResourceSource.FIXED_DECLARATION;
        }
    }

    /**
     * 判断资源行是否已被逻辑删除。
     *
     * <p>唯一键不含 {@code deleted}，软删除行仍占用路由，重新声明时必须复活而不是插入新行。
     */
    private boolean isSoftDeleted(ApiResourceEntity entity) {
        return entity.getDeleted() != null && entity.getDeleted() != 0;
    }

    /**
     * 复活历史软删除行，复用原主键，避免与唯一键冲突。
     *
     * <p>{@code deleted} 是 {@code @TableLogic} 字段，{@code updateById} 不会把它写回 0，
     * 因此必须用显式 update 恢复。
     */
    private void reviveSoftDeleted(ApiResourceEntity entity, ApiResourceRegisterCommand resource) {
        log.info("Revive soft-deleted API resource: httpMethod={}, path={}, module={}",
                resource.getHttpMethod(), resource.getPathPattern(), resource.getModuleName());
        merge(entity, resource);
        apiResourceMapper.reviveSoftDeleted(entity);
    }

    private Map<String, ApiResourceEntity> loadExistingIndex(List<ApiResourceRegisterCommand> resources) {
        List<String> moduleNames = resources.stream()
                .map(ApiResourceRegisterCommand::getModuleName)
                .filter(StringUtils::hasText)
                .distinct()
                .toList();
        if (moduleNames.isEmpty()) {
            return Map.of();
        }
        List<ApiResourceEntity> existingResources = apiResourceMapper.selectIncludingDeletedByModuleNames(moduleNames);
        Map<String, ApiResourceEntity> index = new HashMap<>(existingResources.size());
        for (ApiResourceEntity resource : existingResources) {
            String key = resourceKey(resource.getModuleName(), resource.getHttpMethod(), resource.getPathPattern());
            ApiResourceEntity current = index.get(key);
            if (current == null || isSoftDeleted(current)) {
                index.put(key, resource);
            }
        }
        return index;
    }

    private List<ApiResourceEntity> loadStaleAutoScannedResources(
            List<ApiResourceRegisterCommand> resources,
            Set<String> currentResourceKeys) {
        List<String> handlerClasses = resources.stream()
                .map(ApiResourceRegisterCommand::getHandlerClass)
                .filter(StringUtils::hasText)
                .filter(handlerClass -> !"configuration".equals(handlerClass))
                .distinct()
                .toList();
        if (handlerClasses.isEmpty()) {
            return List.of();
        }
        return apiResourceMapper.selectList(new LambdaQueryWrapper<ApiResourceEntity>()
                .in(ApiResourceEntity::getHandlerClass, handlerClasses)
                .eq(ApiResourceEntity::getStatus, 1))
                .stream()
                .filter(resource -> !isManualOverride(resource))
                .filter(resource -> !currentResourceKeys.contains(resourceKey(
                        resource.getModuleName(),
                        resource.getHttpMethod(),
                        resource.getPathPattern())))
                .collect(Collectors.toList());
    }

    /**
     * 按 HTTP 方法和路径确定唯一有效登记行，并禁用被固定声明或当前批次覆盖的历史行。
     *
     * <p>同一路由只能有一个有效 API 资源。优先级顺序为：人工登记高于固定声明，固定声明高于 Controller 扫描；
     * 同为固定声明时，带具体 handler 的声明高于仅配置路径的声明；仍相等时当前批次优先，再按模块名和记录主键升序，
     * 保证同步顺序不影响结果。人工登记还会阻止非人工来源更新同模块、同方法和同路径的记录。
     */
    private List<ApiResourceEntity> resolveRouteOwnershipConflicts(
            List<ApiResourceRegisterCommand> resources,
            List<ApiResourceEntity> creates,
            List<ApiResourceEntity> updates) {
        List<String> pathPatterns = resources.stream()
                .map(ApiResourceRegisterCommand::getPathPattern)
                .filter(StringUtils::hasText)
                .distinct()
                .toList();
        if (pathPatterns.isEmpty()) {
            return List.of();
        }
        Map<String, ApiResourceEntity> batchEntities = new HashMap<>();
        creates.forEach(entity -> batchEntities.put(
                resourceKey(entity.getModuleName(), entity.getHttpMethod(), entity.getPathPattern()), entity));
        updates.forEach(entity -> batchEntities.put(
                resourceKey(entity.getModuleName(), entity.getHttpMethod(), entity.getPathPattern()), entity));
        Map<String, List<RouteCandidate>> candidatesByRoute = new LinkedHashMap<>();
        for (ApiResourceEntity persisted : apiResourceMapper.selectList(new LambdaQueryWrapper<ApiResourceEntity>()
                .in(ApiResourceEntity::getPathPattern, pathPatterns)
                .eq(ApiResourceEntity::getStatus, 1))) {
            String uniqueKey = resourceKey(
                    persisted.getModuleName(), persisted.getHttpMethod(), persisted.getPathPattern());
            if (batchEntities.containsKey(uniqueKey)) {
                continue;
            }
            candidatesByRoute.computeIfAbsent(
                    routeKey(persisted.getHttpMethod(), persisted.getPathPattern()), key -> new ArrayList<>())
                    .add(new RouteCandidate(persisted.getSource(), persisted.getHandlerClass(),
                            persisted.getModuleName(), persisted.getId(), false, persisted));
        }
        for (ApiResourceRegisterCommand resource : resources) {
            ApiResourceEntity entity = batchEntities.get(resourceKey(
                    resource.getModuleName(), resource.getHttpMethod(), resource.getPathPattern()));
            if (entity == null) {
                continue;
            }
            candidatesByRoute.computeIfAbsent(
                    routeKey(resource.getHttpMethod(), resource.getPathPattern()), key -> new ArrayList<>())
                    .add(new RouteCandidate(resource.getSource() == null ? null : resource.getSource().name(),
                            resource.getHandlerClass(), resource.getModuleName(), entity.getId(), true, entity));
        }
        List<ApiResourceEntity> shadowed = new ArrayList<>();
        for (List<RouteCandidate> candidates : candidatesByRoute.values()) {
            if (candidates.size() < 2) {
                continue;
            }
            RouteCandidate winner = candidates.stream().min(ROUTE_PRECEDENCE).orElseThrow();
            if (isManualOverride(winner.entity())) {
                // 人工接管只在运行时分流，不停用其它声明；解除接管后可立即恢复代码声明。
                continue;
            }
            for (RouteCandidate candidate : candidates) {
                if (isManualOverride(candidate.entity())) {
                    continue;
                }
                if (candidate != winner && candidate.entity() != null) {
                    log.info("API resource route shadowed: httpMethod={}, path={}, winner={}, shadowed={}",
                            candidate.entity().getHttpMethod(), candidate.entity().getPathPattern(),
                            winner.moduleName(), candidate.moduleName());
                    if (candidate.entity().getId() == null) {
                        candidate.entity().setStatus(0);
                    } else {
                        shadowed.add(candidate.entity());
                    }
                }
            }
        }
        return shadowed;
    }

    private static int sourceRank(String source) {
        if (ApiResourceSource.FIXED_DECLARATION.name().equals(source)) {
            return SOURCE_RANK_FIXED;
        }
        if (ApiResourceSource.MANUAL.name().equals(source)) {
            return SOURCE_RANK_MANUAL;
        }
        return SOURCE_RANK_SCAN;
    }

    private static int handlerRank(String handlerClass) {
        if (!StringUtils.hasText(handlerClass)) {
            return HANDLER_RANK_NONE;
        }
        if (CONFIGURED_HANDLER_CLASS.equals(handlerClass)) {
            return HANDLER_RANK_CONFIGURED;
        }
        return HANDLER_RANK_CONCRETE;
    }

    private List<ApiResourceRegisterCommand> excludeCoveredByActiveFixed(
            List<ApiResourceRegisterCommand> resources) {
        List<String> pathPatterns = resources.stream()
                .map(ApiResourceRegisterCommand::getPathPattern)
                .filter(StringUtils::hasText)
                .distinct()
                .toList();
        if (pathPatterns.isEmpty()) {
            return resources;
        }
        List<ApiResourceEntity> activeFixed = apiResourceMapper.selectList(new LambdaQueryWrapper<ApiResourceEntity>()
                .in(ApiResourceEntity::getPathPattern, pathPatterns)
                .eq(ApiResourceEntity::getSource, ApiResourceSource.FIXED_DECLARATION.name())
                .eq(ApiResourceEntity::getStatus, 1));
        if (activeFixed.isEmpty()) {
            return resources;
        }
        return resources.stream()
                .filter(resource -> ApiResourceSource.FIXED_DECLARATION.name().equals(sourceName(resource))
                        || activeFixed.stream().noneMatch(fixed -> routeMatches(
                        fixed.getHttpMethod(), fixed.getPathPattern(),
                        resource.getHttpMethod(), resource.getPathPattern())))
                .toList();
    }

    private String sourceName(ApiResourceRegisterCommand resource) {
        return resource.getSource() == null
                ? ApiResourceSource.FIXED_DECLARATION.name() : resource.getSource().name();
    }

    private boolean routeMatches(String leftMethod, String leftPath, String rightMethod, String rightPath) {
        return methodMatches(leftMethod, rightMethod)
                && (leftPath.equals(rightPath)
                || PATH_MATCHER.match(leftPath, rightPath)
                || PATH_MATCHER.match(rightPath, leftPath));
    }

    private List<ApiResourceRegisterCommand> deduplicate(List<ApiResourceRegisterCommand> resources) {
        Map<String, ApiResourceRegisterCommand> index = new LinkedHashMap<>();
        for (ApiResourceRegisterCommand resource : resources) {
            String key = routeKey(resource.getHttpMethod(), resource.getPathPattern());
            ApiResourceRegisterCommand previous = index.get(key);
            if (previous == null || routeCandidate(resource, true).compareTo(routeCandidate(previous, true)) < 0) {
                index.put(key, resource);
            }
        }
        return new ArrayList<>(index.values());
    }

    private RouteCandidate routeCandidate(ApiResourceRegisterCommand resource, boolean inBatch) {
        return new RouteCandidate(resource.getSource() == null ? null : resource.getSource().name(),
                resource.getHandlerClass(), resource.getModuleName(), resource.getResourceId(), inBatch, null);
    }

    private List<ApiResourceEntity> deduplicateEntities(List<ApiResourceEntity> resources) {
        Map<Long, ApiResourceEntity> index = new LinkedHashMap<>();
        resources.forEach(resource -> index.put(resource.getId(), resource));
        return new ArrayList<>(index.values());
    }

    /** 同一 HTTP 方法和路径的登记行优先级。 */
    private record RouteCandidate(
            String source,
            String handlerClass,
            String moduleName,
            Long id,
            boolean inBatch,
            ApiResourceEntity entity) implements Comparable<RouteCandidate> {

        @Override
        public int compareTo(RouteCandidate other) {
            return ROUTE_PRECEDENCE.compare(this, other);
        }
    }

    private List<ApiResourceEntity> activeResources() {
        List<ApiResourceEntity> cached = activeResourceCache;
        if (cached != null) {
            return cached;
        }
        synchronized (this) {
            if (activeResourceCache == null) {
                activeResourceCache = apiResourceMapper.selectList(new LambdaQueryWrapper<ApiResourceEntity>()
                        .eq(ApiResourceEntity::getStatus, 1))
                        .stream()
                        .filter(resource -> StringUtils.hasText(resource.getPathPattern()))
                        .collect(Collectors.toList());
            }
            return activeResourceCache;
        }
    }

    private void clearRuntimeCache() {
        activeResourceCache = null;
        decisionCache.clear();
        decisionCacheKeys.clear();
    }

    private void cacheDecision(String cacheKey, ApiResourceAccessDecisionVO decision) {
        ApiResourceAccessDecisionVO existing = decisionCache.putIfAbsent(cacheKey, decision);
        if (existing != null) {
            return;
        }
        decisionCacheKeys.add(cacheKey);
        while (decisionCache.size() > MAX_DECISION_CACHE_SIZE) {
            String eldest = decisionCacheKeys.poll();
            if (eldest == null) {
                return;
            }
            decisionCache.remove(eldest);
        }
    }

    private boolean methodMatches(String registeredMethod, String requestMethod) {
        return "ALL".equalsIgnoreCase(registeredMethod)
                || requestMethod.equalsIgnoreCase(registeredMethod);
    }

    private String resourceKey(String moduleName, String httpMethod, String pathPattern) {
        return moduleName + "\n" + httpMethod + "\n" + pathPattern;
    }

    private String routeKey(String httpMethod, String pathPattern) {
        return httpMethod + "\n" + pathPattern;
    }

    private boolean isValid(ApiResourceRegisterCommand resource) {
        return resource != null
                && StringUtils.hasText(resource.getModuleName())
                && StringUtils.hasText(resource.getHttpMethod())
                && StringUtils.hasText(resource.getPathPattern());
    }

    private ApiResourceEntity toEntity(ApiResourceRegisterCommand resource) {
        ApiResourceEntity entity = new ApiResourceEntity();
        entity.setId(resource.getResourceId());
        entity.setTenantId("default");
        entity.setStatus(1);
        entity.setDeleted(0);
        merge(entity, resource);
        return entity;
    }

    private void merge(ApiResourceEntity entity, ApiResourceRegisterCommand resource) {
        entity.setModuleName(resource.getModuleName());
        entity.setDomain(resolveDomain(resource.getPathPattern()));
        entity.setSource(resource.getSource() == null
                ? ApiResourceSource.FIXED_DECLARATION.name() : resource.getSource().name());
        entity.setHttpMethod(resource.getHttpMethod());
        entity.setPathPattern(resource.getPathPattern());
        entity.setResourceCode(defaultResourceCode(resource));
        entity.setPermissionCode(resource.getPermissionCode());
        entity.setAccessMode(defaultAccessMode(resource).name());
        entity.setHandlerClass(resource.getHandlerClass());
        entity.setHandlerMethod(resource.getHandlerMethod());
        entity.setDescription(resource.getDescription());
        entity.setStatus(1);
    }

    private String resolveDomain(String pathPattern) {
        if (!StringUtils.hasText(pathPattern) || !pathPattern.startsWith("/")) {
            return ROOT_DOMAIN;
        }
        String remaining = pathPattern.substring(1);
        int separator = remaining.indexOf('/');
        String segment = separator < 0 ? remaining : remaining.substring(0, separator);
        if (!StringUtils.hasText(segment) || segment.contains("*") || segment.contains("{")
                || segment.contains(".")) {
            return ROOT_DOMAIN;
        }
        return segment;
    }

    private String defaultResourceCode(ApiResourceRegisterCommand resource) {
        if (StringUtils.hasText(resource.getResourceCode())) {
            return resource.getResourceCode();
        }
        return resource.getHttpMethod() + ":" + resource.getPathPattern();
    }

    private ApiResourceAccessMode defaultAccessMode(ApiResourceRegisterCommand resource) {
        return resource.getAccessMode() == null ? ApiResourceAccessMode.LOGIN : resource.getAccessMode();
    }

    private ApiResourceAccessDecisionVO toDecision(ApiResourceEntity resource) {
        ApiResourceAccessMode mode = parseAccessMode(resource.getAccessMode());
        ApiResourceAccessDecisionVO decision = new ApiResourceAccessDecisionVO(true, mode,
                resource.getPermissionCode(), parseSource(resource.getSource()));
        decision.setManualOverride(isManualOverride(resource));
        return decision;
    }

    private ApiResourceAccessMode parseAccessMode(String value) {
        if (!StringUtils.hasText(value)) {
            return ApiResourceAccessMode.LOGIN;
        }
        try {
            return ApiResourceAccessMode.valueOf(value);
        } catch (IllegalArgumentException e) {
            log.warn("Unknown API resource access mode: {}", value);
            return ApiResourceAccessMode.LOGIN;
        }
    }

    private boolean pathMatches(String pattern, String path) {
        if (!StringUtils.hasText(pattern) || !StringUtils.hasText(path)) {
            return false;
        }
        if (pattern.equals(path)) {
            return true;
        }
        return PATH_MATCHER.match(pattern, path);
    }

    private Comparator<ApiResourceEntity> resourceMatchComparator(String path) {
        return Comparator
                .comparingInt(this::manualControlScore)
                .reversed()
                .thenComparing(Comparator.comparingInt((ApiResourceEntity resource) -> exactPathScore(resource.getPathPattern(), path))
                        .reversed())
                .thenComparing(Comparator.comparingInt((ApiResourceEntity resource) -> wildcardScore(resource.getPathPattern()))
                        .reversed())
                .thenComparing(Comparator.comparingInt((ApiResourceEntity resource) -> resource.getPathPattern().length())
                        .reversed())
                .thenComparing(ApiResourceEntity::getId, Comparator.nullsLast(Comparator.naturalOrder()));
    }

    private int manualControlScore(ApiResourceEntity resource) {
        return isManualOverride(resource) || ApiResourceSource.MANUAL.name().equals(resource.getSource()) ? 1 : 0;
    }

    private int exactPathScore(String pattern, String path) {
        return pattern != null && pattern.equals(path) ? 1 : 0;
    }

    private int wildcardScore(String pattern) {
        return pattern != null && !pattern.contains("*") && !pattern.contains("{") ? 1 : 0;
    }
}
