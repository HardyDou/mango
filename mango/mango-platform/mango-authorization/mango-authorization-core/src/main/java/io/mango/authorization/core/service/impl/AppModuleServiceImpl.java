package io.mango.authorization.core.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import io.mango.authorization.api.command.AppModuleCommand;
import io.mango.authorization.api.vo.AppModuleVO;
import io.mango.authorization.core.entity.AuthorizationAppModule;
import io.mango.authorization.core.entity.FrontendMenuRuntimeConfig;
import io.mango.authorization.core.entity.Menu;
import io.mango.authorization.core.mapper.AuthorizationAppModuleMapper;
import io.mango.authorization.core.mapper.FrontendMenuRuntimeConfigMapper;
import io.mango.authorization.core.mapper.MenuMapper;
import io.mango.authorization.core.service.IAppModuleService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 逻辑应用集成模块服务实现。
 */
@Service
@RequiredArgsConstructor
public class AppModuleServiceImpl implements IAppModuleService {

    private final AuthorizationAppModuleMapper appModuleMapper;
    private final MenuMapper menuMapper;
    private final FrontendMenuRuntimeConfigMapper menuRuntimeConfigMapper;

    @Override
    public List<AppModuleVO> list(String appCode, Integer status) {
        LambdaQueryWrapper<AuthorizationAppModule> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(StringUtils.hasText(appCode), AuthorizationAppModule::getAppCode, appCode)
                .eq(status != null, AuthorizationAppModule::getStatus, status)
                .orderByAsc(AuthorizationAppModule::getSort)
                .orderByAsc(AuthorizationAppModule::getModuleCode);
        return appModuleMapper.selectList(wrapper).stream().map(this::toVO).toList();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long save(AppModuleCommand command) {
        AuthorizationAppModule binding = find(command.getAppCode(), command.getModuleCode());
        LocalDateTime now = LocalDateTime.now();
        if (binding == null) {
            binding = new AuthorizationAppModule();
            binding.setAppCode(command.getAppCode());
            binding.setModuleCode(command.getModuleCode());
            binding.setCreateTime(now);
        }
        binding.setModuleName(resolveModuleName(command));
        binding.setStatus(command.getStatus() == null ? 1 : command.getStatus());
        binding.setSort(command.getSort() == null ? 0 : command.getSort());
        binding.setUpdateTime(now);
        if (binding.getBindingId() == null) {
            appModuleMapper.insert(binding);
        } else {
            appModuleMapper.updateById(binding);
        }
        return binding.getBindingId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Boolean disable(String appCode, String moduleCode) {
        AuthorizationAppModule binding = find(appCode, moduleCode);
        if (binding == null) {
            return false;
        }
        binding.setStatus(0);
        binding.setUpdateTime(LocalDateTime.now());
        return appModuleMapper.updateById(binding) > 0;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Integer syncMenus(String appCode, String moduleCode) {
        if (!StringUtils.hasText(appCode) || !StringUtils.hasText(moduleCode)) {
            return 0;
        }
        List<Menu> menus = menuMapper.selectList(new LambdaQueryWrapper<Menu>()
                .eq(Menu::getAppCode, appCode)
                .eq(Menu::getModuleCode, moduleCode)
                .eq(Menu::getDelFlag, 0));
        menus.forEach(this::ensureMenuRuntimeConfig);
        return menus.size();
    }

    private AuthorizationAppModule find(String appCode, String moduleCode) {
        if (!StringUtils.hasText(appCode) || !StringUtils.hasText(moduleCode)) {
            return null;
        }
        return appModuleMapper.selectOne(new LambdaQueryWrapper<AuthorizationAppModule>()
                .eq(AuthorizationAppModule::getAppCode, appCode)
                .eq(AuthorizationAppModule::getModuleCode, moduleCode)
                .last("LIMIT 1"));
    }

    private String resolveModuleName(AppModuleCommand command) {
        if (StringUtils.hasText(command.getModuleName())) {
            return command.getModuleName();
        }
        return command.getModuleCode();
    }

    private void ensureMenuRuntimeConfig(Menu menu) {
        FrontendMenuRuntimeConfig config = menuRuntimeConfigMapper.selectOne(
                new LambdaQueryWrapper<FrontendMenuRuntimeConfig>()
                        .eq(FrontendMenuRuntimeConfig::getMenuId, menu.getMenuId())
                        .last("LIMIT 1"));
        LocalDateTime now = LocalDateTime.now();
        if (config == null) {
            config = new FrontendMenuRuntimeConfig();
            config.setMenuId(menu.getMenuId());
            config.setCreateTime(now);
        }
        config.setAppCode(menu.getAppCode());
        config.setPageType(defaultPageType(menu));
        config.setExternalUrl(null);
        config.setUpdateTime(now);
        if (config.getConfigId() == null) {
            menuRuntimeConfigMapper.insert(config);
        } else {
            menuRuntimeConfigMapper.updateById(config);
        }
    }

    private String defaultPageType(Menu menu) {
        if (Integer.valueOf(3).equals(menu.getMenuType())) {
            return "BUTTON";
        }
        if (Integer.valueOf(1).equals(menu.getEmbedded())) {
            return "IFRAME";
        }
        return "LOCAL_ROUTE";
    }

    private AppModuleVO toVO(AuthorizationAppModule binding) {
        AppModuleVO vo = new AppModuleVO();
        vo.setBindingId(binding.getBindingId());
        vo.setAppCode(binding.getAppCode());
        vo.setModuleCode(binding.getModuleCode());
        vo.setModuleName(binding.getModuleName());
        vo.setStatus(binding.getStatus());
        vo.setSort(binding.getSort());
        vo.setCreateTime(binding.getCreateTime());
        vo.setUpdateTime(binding.getUpdateTime());
        return vo;
    }
}
