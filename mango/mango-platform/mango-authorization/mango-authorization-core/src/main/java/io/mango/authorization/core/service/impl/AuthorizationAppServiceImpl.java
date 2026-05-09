package io.mango.authorization.core.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import io.mango.authorization.api.command.AppCommand;
import io.mango.authorization.api.command.AppLoginContextCommand;
import io.mango.authorization.api.vo.AppLoginContextVO;
import io.mango.authorization.api.vo.AppVO;
import io.mango.authorization.core.entity.AuthorizationApp;
import io.mango.authorization.core.entity.AuthorizationAppLoginContext;
import io.mango.authorization.core.mapper.AuthorizationAppLoginContextMapper;
import io.mango.authorization.core.mapper.AuthorizationAppMapper;
import io.mango.authorization.core.service.IAuthorizationAppService;
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
 * 授权应用入口服务实现。
 */
@Service
@RequiredArgsConstructor
public class AuthorizationAppServiceImpl
        extends MangoCrudServiceImpl<AuthorizationAppMapper, AuthorizationApp>
        implements IAuthorizationAppService {

    private final AuthorizationAppLoginContextMapper loginContextMapper;

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
        return apps.stream().map(app -> toVO(app, contextsByAppCode.get(app.getAppCode()))).toList();
    }

    @Override
    public AppVO get(Long appId) {
        AuthorizationApp app = getById(appId);
        if (app == null) {
            return null;
        }
        return toVO(app, listContexts(app.getAppCode()));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long create(AppCommand command) {
        AuthorizationApp app = toEntity(command);
        beforeCreate(command, app);
        save(app);
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
        vo.setLoginContexts(loginContexts == null ? new ArrayList<>() : loginContexts);
        vo.setIcon(app.getIcon());
        vo.setSort(app.getSort());
        vo.setStatus(app.getStatus());
        vo.setRemark(app.getRemark());
        vo.setCreateTime(app.getCreateTime());
        vo.setUpdateTime(app.getUpdateTime());
        return vo;
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
