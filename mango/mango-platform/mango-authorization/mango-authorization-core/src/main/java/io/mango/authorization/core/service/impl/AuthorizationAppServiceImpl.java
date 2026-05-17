package io.mango.authorization.core.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import io.mango.authorization.api.AuthorizationQuery;
import io.mango.authorization.api.command.AppCommand;
import io.mango.authorization.api.command.AppLoginContextCommand;
import io.mango.authorization.api.vo.AppRuntimeDescriptorVO;
import io.mango.authorization.api.vo.AppLoginContextVO;
import io.mango.authorization.api.vo.AppVO;
import io.mango.authorization.core.entity.AuthorizationApp;
import io.mango.authorization.core.entity.AuthorizationAppLoginContext;
import io.mango.authorization.core.entity.FrontendAppRegistry;
import io.mango.authorization.core.mapper.AuthorizationAppLoginContextMapper;
import io.mango.authorization.core.mapper.AuthorizationAppMapper;
import io.mango.authorization.core.mapper.FrontendAppRegistryMapper;
import io.mango.authorization.core.service.IAuthorizationAppService;
import io.mango.authorization.core.service.IFrontendRuntimeStrategyService;
import io.mango.authorization.core.service.ISubjectAuthorityService;
import io.mango.authorization.core.service.ITenantAppBindingService;
import io.mango.infra.persistence.starter.crud.MangoCrudServiceImpl;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * 授权应用服务实现。
 * <p>
 * 授权应用基础信息仍写入 authorization_app；前端入口运行配置独立写入 frontend_app_registry。
 */
@Service
@RequiredArgsConstructor
public class AuthorizationAppServiceImpl
        extends MangoCrudServiceImpl<AuthorizationAppMapper, AuthorizationApp>
        implements IAuthorizationAppService {

    private final AuthorizationAppLoginContextMapper loginContextMapper;
    private final FrontendAppRegistryMapper frontendAppRegistryMapper;
    private final ITenantAppBindingService tenantAppBindingService;
    private final ISubjectAuthorityService subjectAuthorityService;
    private final IFrontendRuntimeStrategyService runtimeStrategyService;

    @Override
    public List<AppVO> listByQuery(Object query) {
        QueryWrapper<AuthorizationApp> wrapper = new QueryWrapper<>();
        wrapper.eq("status", 1)
                .orderByAsc("sort");
        List<AuthorizationApp> apps = super.list(wrapper);
        Map<String, List<AppLoginContextVO>> contextsByAppCode = listContexts(apps.stream()
                .map(AuthorizationApp::getAppCode)
                .filter(StringUtils::hasText)
                .toList());
        applyFrontendRegistry(apps);
        return apps.stream().map(app -> toVO(app, contextsByAppCode.get(app.getAppCode()))).toList();
    }

    @Override
    public List<AppVO> listRuntimeApps(AuthorizationQuery query) {
        if (query == null) {
            return List.of();
        }
        List<AppVO> logicalApps = listByQuery(null).stream()
                .filter(app -> isTenantOpened(query, app))
                .filter(app -> matchesLoginContext(query, app))
                .filter(app -> hasRuntimeAuthority(query, app))
                .toList();
        List<AppVO> runtimeUnits = listRuntimeUnitsFor(logicalApps);
        if (runtimeUnits.isEmpty()) {
            return logicalApps;
        }
        return runtimeUnits;
    }

    @Override
    public AppRuntimeDescriptorVO runtimeDescriptor(AuthorizationQuery query, String appCode) {
        AppRuntimeDescriptorVO descriptor = new AppRuntimeDescriptorVO();
        String deployProfile = runtimeStrategyService.currentDeployProfile();
        descriptor.setDeployProfile(deployProfile);
        descriptor.setApps(listRuntimeApps(query));
        if (StringUtils.hasText(appCode)) {
            descriptor.setModuleStrategies(runtimeStrategyService.list(appCode, deployProfile, 1));
        }
        return descriptor;
    }

    @Override
    public AppVO get(Long appId) {
        AuthorizationApp app = getById(appId);
        if (app == null) {
            return null;
        }
        applyFrontendRegistry(app);
        return toVO(app, listContexts(app.getAppCode()));
    }

    @Override
    public AppVO getByAppCode(String appCode) {
        if (!StringUtils.hasText(appCode)) {
            return null;
        }
        AuthorizationApp app = getOne(new LambdaQueryWrapper<AuthorizationApp>()
                .eq(AuthorizationApp::getAppCode, appCode)
                .last("limit 1"));
        if (app == null) {
            return null;
        }
        applyFrontendRegistry(app);
        return toVO(app, listContexts(app.getAppCode()));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long create(AppCommand command) {
        AuthorizationApp app = toEntity(command);
        beforeCreate(command, app);
        save(app);
        saveFrontendRegistry(command);
        saveLoginContexts(app, command.getLoginContexts());
        return app.getAppId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Boolean update(AppCommand command) {
        if (command == null || command.getAppId() == null) {
            return false;
        }
        AuthorizationApp existing = getById(command.getAppId());
        if (existing == null) {
            return false;
        }
        existing.setAppCode(command.getAppCode());
        existing.setAppName(command.getAppName());
        existing.setIcon(command.getIcon());
        existing.setSort(command.getSort() == null ? 0 : command.getSort());
        existing.setStatus(command.getStatus() == null ? 1 : command.getStatus());
        existing.setRemark(command.getRemark());
        beforeUpdate(command, existing);
        boolean updated = updateById(existing);
        if (updated) {
            saveFrontendRegistry(command);
            saveLoginContexts(existing, command.getLoginContexts());
        }
        return updated;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Boolean delete(Long appId) {
        if (appId == null) {
            return false;
        }
        AuthorizationApp app = getById(appId);
        if (app == null) {
            return false;
        }
        loginContextMapper.delete(new LambdaQueryWrapper<AuthorizationAppLoginContext>()
                .eq(AuthorizationAppLoginContext::getAppId, appId));
        frontendAppRegistryMapper.delete(new LambdaQueryWrapper<FrontendAppRegistry>()
                .eq(FrontendAppRegistry::getAppCode, app.getAppCode()));
        return super.deleteById(appId);
    }

    @Override
    protected Class<AuthorizationApp> entityType() {
        return AuthorizationApp.class;
    }

    @Override
    protected AuthorizationApp toEntity(Object source) {
        AuthorizationApp app = super.toEntity(source);
        if (app.getSort() == null) {
            app.setSort(0);
        }
        if (app.getStatus() == null) {
            app.setStatus(1);
        }
        if (!StringUtils.hasText(app.getAppType())) {
            app.setAppType("LOCAL");
        }
        if (!StringUtils.hasText(app.getDeployMode())) {
            app.setDeployMode("EMBEDDED");
        }
        if (app.getSandboxEnabled() == null) {
            app.setSandboxEnabled(false);
        }
        if (!StringUtils.hasText(app.getStyleIsolation())) {
            app.setStyleIsolation("NONE");
        }
        return app;
    }

    @Override
    protected Object toVO(AuthorizationApp app) {
        return toVO(app, listContexts(app.getAppCode()));
    }

    private AppVO toVO(AuthorizationApp app, List<AppLoginContextVO> loginContexts) {
        AppVO vo = new AppVO();
        vo.setAppId(app.getAppId());
        vo.setAppCode(app.getAppCode());
        vo.setAppName(app.getAppName());
        vo.setAppType(defaultString(app.getAppType(), "LOCAL"));
        vo.setDeployMode(defaultString(app.getDeployMode(), "EMBEDDED"));
        vo.setEntryUrl(app.getEntryUrl());
        vo.setMountPath(app.getMountPath());
        vo.setActiveRule(app.getActiveRule());
        vo.setFramework(app.getFramework());
        vo.setVersion(app.getVersion());
        vo.setHealthCheckUrl(app.getHealthCheckUrl());
        vo.setSandboxEnabled(Boolean.TRUE.equals(app.getSandboxEnabled()));
        vo.setStyleIsolation(defaultString(app.getStyleIsolation(), "NONE"));
        vo.setLoginContexts(loginContexts == null ? new ArrayList<>() : loginContexts);
        vo.setIcon(app.getIcon());
        vo.setSort(app.getSort());
        vo.setStatus(app.getStatus());
        vo.setRemark(app.getRemark());
        vo.setCreateTime(app.getCreateTime());
        vo.setUpdateTime(app.getUpdateTime());
        return vo;
    }

    private String defaultString(String value, String defaultValue) {
        return StringUtils.hasText(value) ? value : defaultValue;
    }

    private boolean isTenantOpened(AuthorizationQuery query, AppVO app) {
        Long tenantId = parseTenantId(query.tenantId());
        if (tenantId == null) {
            return false;
        }
        if (tenantId == 1L) {
            return true;
        }
        return tenantAppBindingService.isEnabled(tenantId, app.getAppCode());
    }

    private boolean matchesLoginContext(AuthorizationQuery query, AppVO app) {
        if (!StringUtils.hasText(query.realm()) && !StringUtils.hasText(query.actorType())) {
            return true;
        }
        return app.getLoginContexts().stream()
                .filter(context -> Integer.valueOf(1).equals(context.getStatus()))
                .anyMatch(context -> equalsCode(query.realm(), context.getRealm())
                        && equalsCode(query.actorType(), context.getActorType()));
    }

    private boolean hasRuntimeAuthority(AuthorizationQuery query, AppVO app) {
        AuthorizationQuery scopedQuery = query.withSystemCode(app.getAppCode());
        return !subjectAuthorityService.listSubjectRoles(scopedQuery).isEmpty()
                || !subjectAuthorityService.listSubjectPermissions(scopedQuery).isEmpty();
    }

    private boolean equalsCode(String left, String right) {
        if (!StringUtils.hasText(left)) {
            return true;
        }
        return left.equalsIgnoreCase(defaultString(right, ""));
    }

    private Long parseTenantId(String tenantId) {
        if (!StringUtils.hasText(tenantId)) {
            return null;
        }
        try {
            return Long.parseLong(tenantId);
        } catch (NumberFormatException ex) {
            return null;
        }
    }

    private void saveFrontendRegistry(AppCommand command) {
        FrontendAppRegistry registry = frontendAppRegistryMapper.selectOne(new LambdaQueryWrapper<FrontendAppRegistry>()
                .eq(FrontendAppRegistry::getAppCode, command.getAppCode())
                .last("LIMIT 1"));
        LocalDateTime now = LocalDateTime.now();
        if (registry == null) {
            registry = new FrontendAppRegistry();
            registry.setAppCode(command.getAppCode());
            registry.setCreateTime(now);
        }
        registry.setAppType(defaultString(command.getAppType(), "LOCAL"));
        registry.setDeployMode(defaultString(command.getDeployMode(), "EMBEDDED"));
        registry.setEntryUrl(command.getEntryUrl());
        registry.setMountPath(command.getMountPath());
        registry.setActiveRule(command.getActiveRule());
        registry.setFramework(command.getFramework());
        registry.setVersion(command.getVersion());
        registry.setHealthCheckUrl(command.getHealthCheckUrl());
        registry.setSandboxEnabled(Boolean.TRUE.equals(command.getSandboxEnabled()));
        registry.setStyleIsolation(defaultString(command.getStyleIsolation(), "NONE"));
        registry.setUpdateTime(now);
        if (registry.getRegistryId() == null) {
            frontendAppRegistryMapper.insert(registry);
        } else {
            frontendAppRegistryMapper.updateById(registry);
        }
    }

    private List<AppVO> listRuntimeUnitsFor(List<AppVO> logicalApps) {
        if (logicalApps.isEmpty()) {
            return List.of();
        }
        List<String> runtimeCodes = new ArrayList<>();
        logicalApps.stream()
                .map(AppVO::getAppCode)
                .filter(StringUtils::hasText)
                .forEach(runtimeCodes::add);
        logicalApps.stream()
                .map(AppVO::getAppCode)
                .filter(StringUtils::hasText)
                .flatMap(appCode -> runtimeStrategyService
                        .list(appCode, runtimeStrategyService.currentDeployProfile(), 1)
                        .stream())
                .map(item -> item.getRuntimeCode())
                .filter(StringUtils::hasText)
                .forEach(runtimeCodes::add);
        List<String> distinctCodes = runtimeCodes.stream().distinct().toList();
        if (distinctCodes.isEmpty()) {
            return List.of();
        }
        List<AuthorizationApp> baseApps = super.list(new QueryWrapper<AuthorizationApp>()
                .in("app_code", distinctCodes)
                .orderByAsc("sort"));
        Map<String, AuthorizationApp> baseByCode = baseApps.stream()
                .collect(Collectors.toMap(AuthorizationApp::getAppCode, item -> item, (left, right) -> left));
        Map<String, FrontendAppRegistry> registryByCode = frontendAppRegistryMapper.selectList(
                        new LambdaQueryWrapper<FrontendAppRegistry>().in(FrontendAppRegistry::getAppCode, distinctCodes))
                .stream()
                .collect(Collectors.toMap(FrontendAppRegistry::getAppCode, item -> item, (left, right) -> left));
        List<AppVO> result = new ArrayList<>();
        for (String runtimeCode : distinctCodes) {
            AuthorizationApp base = baseByCode.get(runtimeCode);
            if (base != null) {
                applyFrontendRegistry(base, registryByCode.get(runtimeCode));
                result.add(toVO(base, listContexts(base.getAppCode())));
                continue;
            }
            FrontendAppRegistry registry = registryByCode.get(runtimeCode);
            if (registry != null) {
                result.add(toRuntimeUnitVO(registry));
            }
        }
        return result;
    }

    private AppVO toRuntimeUnitVO(FrontendAppRegistry registry) {
        AppVO vo = new AppVO();
        vo.setAppCode(registry.getAppCode());
        vo.setAppName(registry.getAppCode());
        vo.setAppType(defaultString(registry.getAppType(), "LOCAL"));
        vo.setDeployMode(defaultString(registry.getDeployMode(), "EMBEDDED"));
        vo.setEntryUrl(registry.getEntryUrl());
        vo.setMountPath(registry.getMountPath());
        vo.setActiveRule(registry.getActiveRule());
        vo.setFramework(registry.getFramework());
        vo.setVersion(registry.getVersion());
        vo.setHealthCheckUrl(registry.getHealthCheckUrl());
        vo.setSandboxEnabled(Boolean.TRUE.equals(registry.getSandboxEnabled()));
        vo.setStyleIsolation(defaultString(registry.getStyleIsolation(), "NONE"));
        vo.setStatus(1);
        vo.setCreateTime(registry.getCreateTime());
        vo.setUpdateTime(registry.getUpdateTime());
        return vo;
    }

    private void applyFrontendRegistry(AuthorizationApp app) {
        if (app == null || !StringUtils.hasText(app.getAppCode())) {
            return;
        }
        FrontendAppRegistry registry = frontendAppRegistryMapper.selectOne(new LambdaQueryWrapper<FrontendAppRegistry>()
                .eq(FrontendAppRegistry::getAppCode, app.getAppCode())
                .last("LIMIT 1"));
        applyFrontendRegistry(app, registry);
    }

    private void applyFrontendRegistry(List<AuthorizationApp> apps) {
        List<String> appCodes = apps.stream()
                .map(AuthorizationApp::getAppCode)
                .filter(StringUtils::hasText)
                .toList();
        if (appCodes.isEmpty()) {
            return;
        }
        Map<String, FrontendAppRegistry> registryByAppCode = frontendAppRegistryMapper.selectList(
                        new LambdaQueryWrapper<FrontendAppRegistry>().in(FrontendAppRegistry::getAppCode, appCodes))
                .stream()
                .collect(Collectors.toMap(FrontendAppRegistry::getAppCode, item -> item, (left, right) -> left));
        apps.forEach(app -> applyFrontendRegistry(app, registryByAppCode.get(app.getAppCode())));
    }

    private void applyFrontendRegistry(AuthorizationApp app, FrontendAppRegistry registry) {
        app.setAppType(defaultString(registry == null ? null : registry.getAppType(), "LOCAL"));
        app.setDeployMode(defaultString(registry == null ? null : registry.getDeployMode(), "EMBEDDED"));
        app.setEntryUrl(registry == null ? null : registry.getEntryUrl());
        app.setMountPath(registry == null ? null : registry.getMountPath());
        app.setActiveRule(registry == null ? null : registry.getActiveRule());
        app.setFramework(registry == null ? null : registry.getFramework());
        app.setVersion(registry == null ? null : registry.getVersion());
        app.setHealthCheckUrl(registry == null ? null : registry.getHealthCheckUrl());
        app.setSandboxEnabled(registry != null && Boolean.TRUE.equals(registry.getSandboxEnabled()));
        app.setStyleIsolation(defaultString(registry == null ? null : registry.getStyleIsolation(), "NONE"));
    }

    private void saveLoginContexts(AuthorizationApp app, List<AppLoginContextCommand> commands) {
        List<AppLoginContextCommand> normalized = normalizeLoginContexts(commands);
        if (normalized.isEmpty()) {
            throw new IllegalArgumentException("应用至少需要一个登录上下文");
        }
        loginContextMapper.delete(new LambdaQueryWrapper<AuthorizationAppLoginContext>()
                .eq(AuthorizationAppLoginContext::getAppId, app.getAppId()));
        LocalDateTime now = LocalDateTime.now();
        for (int i = 0; i < normalized.size(); i++) {
            AppLoginContextCommand command = normalized.get(i);
            AuthorizationAppLoginContext context = new AuthorizationAppLoginContext();
            context.setAppId(app.getAppId());
            context.setAppCode(app.getAppCode());
            context.setRealm(command.getRealm());
            context.setActorType(command.getActorType());
            context.setDefaultFlag(command.getDefaultFlag());
            context.setStatus(command.getStatus());
            context.setSort(command.getSort() == null ? i : command.getSort());
            context.setCreateTime(now);
            context.setUpdateTime(now);
            loginContextMapper.insert(context);
        }
    }

    private List<AppLoginContextCommand> normalizeLoginContexts(List<AppLoginContextCommand> commands) {
        if (commands == null) {
            return new ArrayList<>();
        }
        List<AppLoginContextCommand> normalized = commands.stream()
                .filter(Objects::nonNull)
                .filter(item -> StringUtils.hasText(item.getRealm()) && StringUtils.hasText(item.getActorType()))
                .collect(Collectors.collectingAndThen(
                        Collectors.toMap(
                                item -> normalizeCode(item.getRealm()) + ":" + normalizeCode(item.getActorType()),
                                item -> item,
                                (left, right) -> left),
                        map -> new ArrayList<>(map.values())));
        if (normalized.isEmpty()) {
            return normalized;
        }
        boolean hasDefault = normalized.stream().anyMatch(item -> Integer.valueOf(1).equals(item.getDefaultFlag()));
        for (int i = 0; i < normalized.size(); i++) {
            AppLoginContextCommand item = normalized.get(i);
            item.setRealm(normalizeCode(item.getRealm()));
            item.setActorType(normalizeCode(item.getActorType()));
            item.setStatus(item.getStatus() == null ? 1 : item.getStatus());
            item.setSort(item.getSort() == null ? i : item.getSort());
            item.setDefaultFlag(hasDefault ? (Integer.valueOf(1).equals(item.getDefaultFlag()) ? 1 : 0) : (i == 0 ? 1 : 0));
            if (item.getDefaultFlag() == 1) {
                hasDefault = true;
            }
        }
        boolean defaultAssigned = false;
        for (AppLoginContextCommand item : normalized) {
            if (item.getDefaultFlag() == 1 && !defaultAssigned) {
                defaultAssigned = true;
                continue;
            }
            item.setDefaultFlag(0);
        }
        return normalized;
    }

    private String normalizeCode(String value) {
        return value == null ? null : value.trim().toUpperCase(Locale.ROOT);
    }

    private Map<String, List<AppLoginContextVO>> listContexts(List<String> appCodes) {
        if (appCodes == null || appCodes.isEmpty()) {
            return Map.of();
        }
        List<AuthorizationAppLoginContext> contexts = loginContextMapper.selectList(
                new LambdaQueryWrapper<AuthorizationAppLoginContext>()
                        .in(AuthorizationAppLoginContext::getAppCode, appCodes)
                        .orderByAsc(AuthorizationAppLoginContext::getSort));
        return contexts.stream()
                .map(this::toContextVO)
                .collect(Collectors.groupingBy(AppLoginContextVO::getAppCode));
    }

    private List<AppLoginContextVO> listContexts(String appCode) {
        if (!StringUtils.hasText(appCode)) {
            return new ArrayList<>();
        }
        return loginContextMapper.selectList(new LambdaQueryWrapper<AuthorizationAppLoginContext>()
                        .eq(AuthorizationAppLoginContext::getAppCode, appCode)
                        .orderByAsc(AuthorizationAppLoginContext::getSort))
                .stream()
                .map(this::toContextVO)
                .toList();
    }

    private AppLoginContextVO toContextVO(AuthorizationAppLoginContext context) {
        AppLoginContextVO vo = new AppLoginContextVO();
        vo.setContextId(context.getContextId());
        vo.setAppId(context.getAppId());
        vo.setAppCode(context.getAppCode());
        vo.setRealm(context.getRealm());
        vo.setActorType(context.getActorType());
        vo.setDefaultFlag(context.getDefaultFlag());
        vo.setStatus(context.getStatus());
        vo.setSort(context.getSort());
        vo.setCreateTime(context.getCreateTime());
        vo.setUpdateTime(context.getUpdateTime());
        return vo;
    }

    @Override
    protected void beforeCreate(Object command, AuthorizationApp entity) {
        LocalDateTime now = LocalDateTime.now();
        entity.setCreateTime(now);
        entity.setUpdateTime(now);
    }

    @Override
    protected void beforeUpdate(Object command, AuthorizationApp entity) {
        entity.setUpdateTime(LocalDateTime.now());
    }
}
