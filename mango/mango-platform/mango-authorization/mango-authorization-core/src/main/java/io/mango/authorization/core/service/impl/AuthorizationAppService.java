package io.mango.authorization.core.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import io.mango.authorization.api.enums.AuthorizationCode;
import io.mango.authorization.api.AuthorizationQuery;
import io.mango.authorization.api.command.AppCommand;
import io.mango.authorization.api.command.AppLoginContextCommand;
import io.mango.authorization.api.query.FrontendModuleRuntimeStrategyQuery;
import io.mango.authorization.api.vo.AppRuntimeDescriptorVO;
import io.mango.authorization.api.vo.AppLoginContextVO;
import io.mango.authorization.api.vo.AppVO;
import io.mango.authorization.core.entity.AuthorizationAppEntity;
import io.mango.authorization.core.entity.AuthorizationAppLoginContextEntity;
import io.mango.authorization.core.entity.FrontendAppRegistryEntity;
import io.mango.authorization.core.mapper.AuthorizationAppLoginContextMapper;
import io.mango.authorization.core.mapper.AuthorizationAppMapper;
import io.mango.authorization.core.mapper.FrontendAppRegistryMapper;
import io.mango.authorization.core.service.IAuthorizationAppService;
import io.mango.authorization.core.service.IFrontendRuntimeStrategyService;
import io.mango.authorization.core.service.ISubjectAuthorityService;
import io.mango.authorization.core.service.ITenantAppBindingService;
import io.mango.common.result.Require;
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
 * 授权应用基础信息仍写入 authorization_app；前端入口运行配置独立写入 authorization_frontend_app_registry。
 */
@Service
@RequiredArgsConstructor
public class AuthorizationAppService implements IAuthorizationAppService {

    private static final String PLATFORM_TENANT_ID = "default";

    private final AuthorizationAppMapper authorizationAppMapper;
    private final AuthorizationAppLoginContextMapper loginContextMapper;
    private final FrontendAppRegistryMapper frontendAppRegistryMapper;
    private final ITenantAppBindingService tenantAppBindingService;
    private final ISubjectAuthorityService subjectAuthorityService;
    private final IFrontendRuntimeStrategyService runtimeStrategyService;

    @Override
    public List<AppVO> listByQuery(Object query) {
        QueryWrapper<AuthorizationAppEntity> wrapper = new QueryWrapper<>();
        wrapper.eq("status", 1)
                .orderByAsc("sort");
        List<AuthorizationAppEntity> apps = authorizationAppMapper.selectList(wrapper);
        Map<String, List<AppLoginContextVO>> contextsByAppCode = listContexts(apps.stream()
                .map(AuthorizationAppEntity::getAppCode)
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
            descriptor.setModuleStrategies(listRuntimeStrategies(appCode, deployProfile));
        }
        return descriptor;
    }

    @Override
    public AppVO get(Long appId) {
        Require.notNull(appId, AuthorizationCode.AUTHORIZATION_BUSINESS_ERROR, "应用ID不能为空");
        AuthorizationAppEntity app = authorizationAppMapper.selectById(appId);
        Require.notNull(app, AuthorizationCode.AUTHORIZATION_NOT_FOUND, "应用不存在");
        applyFrontendRegistry(app);
        return toVO(app, listContexts(app.getAppCode()));
    }

    @Override
    public AppVO getByAppCode(String appCode) {
        if (!StringUtils.hasText(appCode)) {
            return null;
        }
        AuthorizationAppEntity app = authorizationAppMapper.selectOne(new LambdaQueryWrapper<AuthorizationAppEntity>()
                .eq(AuthorizationAppEntity::getAppCode, appCode)
                .last("limit 1"));
        if (app == null) {
            return null;
        }
        applyFrontendRegistry(app);
        return toVO(app, listContexts(app.getAppCode()));
    }

    @Override
    public AppVO getRuntimeApp(AuthorizationQuery query, String appCode) {
        AppVO app = getByAppCode(appCode);
        boolean visible = app != null && listRuntimeApps(query).stream()
                .anyMatch(item -> appCode.equals(item.getAppCode()));
        Require.isTrue(visible, AuthorizationCode.AUTHORIZATION_NOT_FOUND,
                "应用运行配置不存在");
        return app;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long upsertBaseline(AppCommand command) {
        Require.notNull(command, AuthorizationCode.AUTHORIZATION_BUSINESS_ERROR, "应用命令不能为空");
        Require.notBlank(command.getAppCode(), AuthorizationCode.AUTHORIZATION_BUSINESS_ERROR, "应用编码不能为空");
        AuthorizationAppEntity app = authorizationAppMapper.selectOne(new LambdaQueryWrapper<AuthorizationAppEntity>()
                .eq(AuthorizationAppEntity::getAppCode, command.getAppCode())
                .last("LIMIT 1"));
        if (app == null) {
            app = toEntity(command);
            beforeCreate(command, app);
            authorizationAppMapper.insert(app);
        } else {
            applyLogicalAppFields(command, app);
            beforeUpdate(command, app);
            authorizationAppMapper.updateById(app);
        }
        saveLoginContexts(app, command.getLoginContexts());
        return app.getAppId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long create(AppCommand command) {
        Require.notNull(command, AuthorizationCode.AUTHORIZATION_BUSINESS_ERROR, "应用命令不能为空");
        AuthorizationAppEntity app = toEntity(command);
        beforeCreate(command, app);
        authorizationAppMapper.insert(app);
        saveFrontendRegistry(command);
        saveLoginContexts(app, command.getLoginContexts());
        return app.getAppId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Boolean update(AppCommand command) {
        Require.notNull(command, AuthorizationCode.AUTHORIZATION_BUSINESS_ERROR, "应用命令不能为空");
        Require.notNull(command.getAppId(), AuthorizationCode.AUTHORIZATION_BUSINESS_ERROR, "应用ID不能为空");
        AuthorizationAppEntity existing = authorizationAppMapper.selectById(command.getAppId());
        Require.notNull(existing, AuthorizationCode.AUTHORIZATION_NOT_FOUND, "应用不存在");
        applyLogicalAppFields(command, existing);
        beforeUpdate(command, existing);
        boolean updated = authorizationAppMapper.updateById(existing) > 0;
        Require.isTrue(updated, AuthorizationCode.AUTHORIZATION_NOT_FOUND, "应用不存在");
        saveFrontendRegistry(command);
        saveLoginContexts(existing, command.getLoginContexts());
        return true;
    }

    private void applyLogicalAppFields(AppCommand command, AuthorizationAppEntity app) {
        app.setAppCode(command.getAppCode());
        app.setAppName(command.getAppName());
        app.setIcon(command.getIcon());
        app.setSort(command.getSort() == null ? 0 : command.getSort());
        app.setStatus(command.getStatus() == null ? 1 : command.getStatus());
        app.setRemark(command.getRemark());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Boolean delete(Long appId) {
        Require.notNull(appId, AuthorizationCode.AUTHORIZATION_BUSINESS_ERROR, "应用ID不能为空");
        Require.isTrue(appId > 0, AuthorizationCode.AUTHORIZATION_BUSINESS_ERROR, "应用ID必须为正数");
        AuthorizationAppEntity app = authorizationAppMapper.selectById(appId);
        Require.notNull(app, AuthorizationCode.AUTHORIZATION_NOT_FOUND, "应用不存在");
        loginContextMapper.delete(new LambdaQueryWrapper<AuthorizationAppLoginContextEntity>()
                .eq(AuthorizationAppLoginContextEntity::getAppId, appId));
        frontendAppRegistryMapper.delete(new LambdaQueryWrapper<FrontendAppRegistryEntity>()
                .eq(FrontendAppRegistryEntity::getAppCode, app.getAppCode()));
        boolean deleted = authorizationAppMapper.deleteById(appId) > 0;
        Require.isTrue(deleted, AuthorizationCode.AUTHORIZATION_NOT_FOUND, "应用不存在");
        return true;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long saveFrontendAppRegistry(FrontendAppRegistryEntity source) {
        Require.notNull(source, AuthorizationCode.AUTHORIZATION_BUSINESS_ERROR, "前端运行单元不能为空");
        Require.notBlank(source.getAppCode(), AuthorizationCode.AUTHORIZATION_BUSINESS_ERROR,
                "前端运行单元编码不能为空");
        FrontendAppRegistryEntity registry = frontendAppRegistryMapper.selectOne(new LambdaQueryWrapper<FrontendAppRegistryEntity>()
                .eq(FrontendAppRegistryEntity::getAppCode, source.getAppCode())
                .last("LIMIT 1"));
        LocalDateTime now = LocalDateTime.now();
        if (registry == null) {
            registry = new FrontendAppRegistryEntity();
            registry.setAppCode(source.getAppCode());
            registry.setTenantId(PLATFORM_TENANT_ID);
            registry.setCreateTime(now);
        }
        registry.setAppType(defaultString(source.getAppType(), "LOCAL"));
        registry.setDeployMode(defaultString(source.getDeployMode(), "EMBEDDED"));
        registry.setEntryUrl(source.getEntryUrl());
        registry.setMountPath(source.getMountPath());
        registry.setActiveRule(source.getActiveRule());
        registry.setFramework(source.getFramework());
        registry.setVersion(source.getVersion());
        registry.setHealthCheckUrl(source.getHealthCheckUrl());
        registry.setSandboxEnabled(Boolean.TRUE.equals(source.getSandboxEnabled()));
        registry.setStyleIsolation(defaultString(source.getStyleIsolation(), "NONE"));
        registry.setUpdateTime(now);
        if (registry.getRegistryId() == null) {
            frontendAppRegistryMapper.insert(registry);
        } else {
            frontendAppRegistryMapper.updateById(registry);
        }
        return registry.getRegistryId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Boolean deleteFrontendAppRegistry(Long registryId) {
        Require.isTrue(registryId == null || registryId > 0,
                AuthorizationCode.AUTHORIZATION_BUSINESS_ERROR, "前端运行单元ID必须为正数");
        if (registryId == null) {
            return false;
        }
        return frontendAppRegistryMapper.deleteById(registryId) > 0;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Boolean deleteFrontendAppRegistry(String appCode) {
        Require.isTrue(appCode == null || appCode.length() <= 64,
                AuthorizationCode.AUTHORIZATION_BUSINESS_ERROR, "前端运行单元编码最多64个字符");
        if (!StringUtils.hasText(appCode)) {
            return false;
        }
        return frontendAppRegistryMapper.delete(new LambdaQueryWrapper<FrontendAppRegistryEntity>()
                .eq(FrontendAppRegistryEntity::getAppCode, appCode)) > 0;
    }

    private AuthorizationAppEntity toEntity(AppCommand source) {
        AuthorizationAppEntity app = new AuthorizationAppEntity();
        app.setAppId(source.getAppId());
        applyLogicalAppFields(source, app);
        app.setAppType(source.getAppType());
        app.setDeployMode(source.getDeployMode());
        app.setEntryUrl(source.getEntryUrl());
        app.setMountPath(source.getMountPath());
        app.setActiveRule(source.getActiveRule());
        app.setFramework(source.getFramework());
        app.setVersion(source.getVersion());
        app.setHealthCheckUrl(source.getHealthCheckUrl());
        app.setSandboxEnabled(source.getSandboxEnabled());
        app.setStyleIsolation(source.getStyleIsolation());
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

    private AppVO toVO(AuthorizationAppEntity app, List<AppLoginContextVO> loginContexts) {
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
        FrontendAppRegistryEntity registry = frontendAppRegistryMapper.selectOne(new LambdaQueryWrapper<FrontendAppRegistryEntity>()
                .eq(FrontendAppRegistryEntity::getAppCode, command.getAppCode())
                .last("LIMIT 1"));
        LocalDateTime now = LocalDateTime.now();
        if (registry == null) {
            registry = new FrontendAppRegistryEntity();
            registry.setAppCode(command.getAppCode());
            registry.setTenantId(PLATFORM_TENANT_ID);
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
                .flatMap(appCode -> listRuntimeStrategies(
                        appCode, runtimeStrategyService.currentDeployProfile()).stream())
                .map(item -> item.getRuntimeCode())
                .filter(StringUtils::hasText)
                .forEach(runtimeCodes::add);
        List<String> distinctCodes = runtimeCodes.stream().distinct().toList();
        if (distinctCodes.isEmpty()) {
            return List.of();
        }
        List<AuthorizationAppEntity> baseApps = authorizationAppMapper.selectList(new QueryWrapper<AuthorizationAppEntity>()
                .in("app_code", distinctCodes)
                .orderByAsc("sort"));
        Map<String, AuthorizationAppEntity> baseByCode = baseApps.stream()
                .collect(Collectors.toMap(AuthorizationAppEntity::getAppCode, item -> item, (left, right) -> left));
        Map<String, FrontendAppRegistryEntity> registryByCode = frontendAppRegistryMapper.selectList(
                        new LambdaQueryWrapper<FrontendAppRegistryEntity>().in(FrontendAppRegistryEntity::getAppCode, distinctCodes))
                .stream()
                .collect(Collectors.toMap(FrontendAppRegistryEntity::getAppCode, item -> item, (left, right) -> left));
        List<AppVO> result = new ArrayList<>();
        for (String runtimeCode : distinctCodes) {
            AuthorizationAppEntity base = baseByCode.get(runtimeCode);
            if (base != null) {
                applyFrontendRegistry(base, registryByCode.get(runtimeCode));
                result.add(toVO(base, listContexts(base.getAppCode())));
                continue;
            }
            FrontendAppRegistryEntity registry = registryByCode.get(runtimeCode);
            if (registry != null) {
                result.add(toRuntimeUnitVO(registry));
            }
        }
        return result;
    }

    private List<io.mango.authorization.api.vo.FrontendModuleRuntimeStrategyVO> listRuntimeStrategies(
            String appCode, String deployProfile) {
        FrontendModuleRuntimeStrategyQuery query = new FrontendModuleRuntimeStrategyQuery();
        query.setAppCode(appCode);
        query.setDeployProfile(deployProfile);
        query.setStatus(1);
        return runtimeStrategyService.list(query);
    }

    private AppVO toRuntimeUnitVO(FrontendAppRegistryEntity registry) {
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

    private void applyFrontendRegistry(AuthorizationAppEntity app) {
        if (app == null || !StringUtils.hasText(app.getAppCode())) {
            return;
        }
        FrontendAppRegistryEntity registry = frontendAppRegistryMapper.selectOne(new LambdaQueryWrapper<FrontendAppRegistryEntity>()
                .eq(FrontendAppRegistryEntity::getAppCode, app.getAppCode())
                .last("LIMIT 1"));
        applyFrontendRegistry(app, registry);
    }

    private void applyFrontendRegistry(List<AuthorizationAppEntity> apps) {
        List<String> appCodes = apps.stream()
                .map(AuthorizationAppEntity::getAppCode)
                .filter(StringUtils::hasText)
                .toList();
        if (appCodes.isEmpty()) {
            return;
        }
        Map<String, FrontendAppRegistryEntity> registryByAppCode = frontendAppRegistryMapper.selectList(
                        new LambdaQueryWrapper<FrontendAppRegistryEntity>().in(FrontendAppRegistryEntity::getAppCode, appCodes))
                .stream()
                .collect(Collectors.toMap(FrontendAppRegistryEntity::getAppCode, item -> item, (left, right) -> left));
        apps.forEach(app -> applyFrontendRegistry(app, registryByAppCode.get(app.getAppCode())));
    }

    private void applyFrontendRegistry(AuthorizationAppEntity app, FrontendAppRegistryEntity registry) {
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

    private void saveLoginContexts(AuthorizationAppEntity app, List<AppLoginContextCommand> commands) {
        List<AppLoginContextCommand> normalized = normalizeLoginContexts(commands);
        Require.notEmpty(normalized, AuthorizationCode.AUTHORIZATION_BUSINESS_ERROR,
                "应用至少需要一个登录上下文");
        loginContextMapper.delete(new LambdaQueryWrapper<AuthorizationAppLoginContextEntity>()
                .eq(AuthorizationAppLoginContextEntity::getAppId, app.getAppId()));
        LocalDateTime now = LocalDateTime.now();
        for (int i = 0; i < normalized.size(); i++) {
            AppLoginContextCommand command = normalized.get(i);
            AuthorizationAppLoginContextEntity context = new AuthorizationAppLoginContextEntity();
            context.setTenantId(PLATFORM_TENANT_ID);
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
        List<AuthorizationAppLoginContextEntity> contexts = loginContextMapper.selectList(
                new LambdaQueryWrapper<AuthorizationAppLoginContextEntity>()
                        .in(AuthorizationAppLoginContextEntity::getAppCode, appCodes)
                        .orderByAsc(AuthorizationAppLoginContextEntity::getSort));
        return contexts.stream()
                .map(this::toContextVO)
                .collect(Collectors.groupingBy(AppLoginContextVO::getAppCode));
    }

    private List<AppLoginContextVO> listContexts(String appCode) {
        if (!StringUtils.hasText(appCode)) {
            return new ArrayList<>();
        }
        return loginContextMapper.selectList(new LambdaQueryWrapper<AuthorizationAppLoginContextEntity>()
                        .eq(AuthorizationAppLoginContextEntity::getAppCode, appCode)
                        .orderByAsc(AuthorizationAppLoginContextEntity::getSort))
                .stream()
                .map(this::toContextVO)
                .toList();
    }

    private AppLoginContextVO toContextVO(AuthorizationAppLoginContextEntity context) {
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

    private void beforeCreate(AppCommand command, AuthorizationAppEntity entity) {
        LocalDateTime now = LocalDateTime.now();
        entity.setTenantId(PLATFORM_TENANT_ID);
        entity.setCreateTime(now);
        entity.setUpdateTime(now);
    }

    private void beforeUpdate(AppCommand command, AuthorizationAppEntity entity) {
        entity.setUpdateTime(LocalDateTime.now());
    }
}
