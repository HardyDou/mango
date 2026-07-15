package io.mango.authorization.core.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import io.mango.authorization.api.enums.AuthorizationCode;
import io.mango.authorization.api.command.FrontendModuleRuntimeStrategyCommand;
import io.mango.authorization.api.query.FrontendModuleRuntimeStrategyQuery;
import io.mango.authorization.api.vo.FrontendModuleRuntimeStrategyVO;
import io.mango.authorization.core.config.FrontendRuntimeProperties;
import io.mango.authorization.core.entity.FrontendModuleRuntimeStrategyEntity;
import io.mango.authorization.core.mapper.FrontendModuleRuntimeStrategyMapper;
import io.mango.authorization.core.service.IFrontendRuntimeStrategyService;
import io.mango.common.result.Require;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Locale;

/**
 * 前端模块运行策略服务实现。
 */
@Service
@RequiredArgsConstructor
public class FrontendRuntimeStrategyService implements IFrontendRuntimeStrategyService {

    private static final String PLATFORM_TENANT_ID = "default";

    private final FrontendModuleRuntimeStrategyMapper strategyMapper;
    private final FrontendRuntimeProperties properties;

    @Override
    public String currentDeployProfile() {
        return normalizeProfile(properties.getDeployProfile());
    }

    @Override
    public List<FrontendModuleRuntimeStrategyVO> list(FrontendModuleRuntimeStrategyQuery query) {
        LambdaQueryWrapper<FrontendModuleRuntimeStrategyEntity> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(StringUtils.hasText(query.getAppCode()), FrontendModuleRuntimeStrategyEntity::getAppCode, query.getAppCode())
                .eq(StringUtils.hasText(query.getModuleCode()), FrontendModuleRuntimeStrategyEntity::getModuleCode, query.getModuleCode())
                .eq(StringUtils.hasText(query.getDeployProfile()), FrontendModuleRuntimeStrategyEntity::getDeployProfile,
                        normalizeProfile(query.getDeployProfile()))
                .eq(query.getStatus() != null, FrontendModuleRuntimeStrategyEntity::getStatus, query.getStatus())
                .orderByAsc(FrontendModuleRuntimeStrategyEntity::getSort)
                .orderByAsc(FrontendModuleRuntimeStrategyEntity::getModuleCode);
        return strategyMapper.selectList(wrapper).stream().map(this::toVO).toList();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long save(FrontendModuleRuntimeStrategyCommand command) {
        Require.notNull(command, AuthorizationCode.AUTHORIZATION_BUSINESS_ERROR, "前端运行策略命令不能为空");
        FrontendModuleRuntimeStrategyEntity strategy = find(
                command.getAppCode(),
                command.getModuleCode(),
                command.getDeployProfile());
        LocalDateTime now = LocalDateTime.now();
        if (strategy == null) {
            strategy = new FrontendModuleRuntimeStrategyEntity();
            strategy.setTenantId(PLATFORM_TENANT_ID);
            strategy.setAppCode(command.getAppCode());
            strategy.setModuleCode(command.getModuleCode());
            strategy.setDeployProfile(normalizeProfile(command.getDeployProfile()));
            strategy.setCreateTime(now);
        }
        strategy.setPageType(defaultString(command.getPageType(), "LOCAL_ROUTE"));
        strategy.setRuntimeCode(defaultString(command.getRuntimeCode(), "mango-admin-local"));
        strategy.setStatus(command.getStatus() == null ? 1 : command.getStatus());
        strategy.setSort(command.getSort() == null ? 0 : command.getSort());
        strategy.setUpdateTime(now);
        if (strategy.getStrategyId() == null) {
            strategyMapper.insert(strategy);
        } else {
            strategyMapper.updateById(strategy);
        }
        return strategy.getStrategyId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Boolean disable(Long strategyId) {
        Require.notNull(strategyId, AuthorizationCode.AUTHORIZATION_BUSINESS_ERROR, "前端运行策略ID不能为空");
        if (strategyId == null) {
            return false;
        }
        FrontendModuleRuntimeStrategyEntity strategy = strategyMapper.selectById(strategyId);
        if (strategy == null) {
            return false;
        }
        strategy.setStatus(0);
        strategy.setUpdateTime(LocalDateTime.now());
        return strategyMapper.updateById(strategy) > 0;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Boolean disable(FrontendModuleRuntimeStrategyQuery query) {
        Require.notNull(query, AuthorizationCode.AUTHORIZATION_BUSINESS_ERROR, "前端运行策略条件不能为空");
        FrontendModuleRuntimeStrategyEntity strategy = find(
                query.getAppCode(), query.getModuleCode(), query.getDeployProfile());
        if (strategy == null) {
            return false;
        }
        strategy.setStatus(0);
        strategy.setUpdateTime(LocalDateTime.now());
        return strategyMapper.updateById(strategy) > 0;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Boolean delete(Long strategyId) {
        Require.notNull(strategyId, AuthorizationCode.AUTHORIZATION_BUSINESS_ERROR, "前端运行策略ID不能为空");
        if (strategyId == null) {
            return false;
        }
        return strategyMapper.deleteById(strategyId) > 0;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Boolean delete(FrontendModuleRuntimeStrategyQuery query) {
        Require.notNull(query, AuthorizationCode.AUTHORIZATION_BUSINESS_ERROR, "前端运行策略条件不能为空");
        if (!StringUtils.hasText(query.getAppCode()) || !StringUtils.hasText(query.getModuleCode())) {
            return false;
        }
        return strategyMapper.delete(new LambdaQueryWrapper<FrontendModuleRuntimeStrategyEntity>()
                .eq(FrontendModuleRuntimeStrategyEntity::getAppCode, query.getAppCode())
                .eq(FrontendModuleRuntimeStrategyEntity::getModuleCode, query.getModuleCode())
                .eq(FrontendModuleRuntimeStrategyEntity::getDeployProfile,
                        normalizeProfile(query.getDeployProfile()))) > 0;
    }

    private FrontendModuleRuntimeStrategyEntity find(String appCode, String moduleCode, String deployProfile) {
        if (!StringUtils.hasText(appCode) || !StringUtils.hasText(moduleCode)) {
            return null;
        }
        return strategyMapper.selectOne(new LambdaQueryWrapper<FrontendModuleRuntimeStrategyEntity>()
                .eq(FrontendModuleRuntimeStrategyEntity::getAppCode, appCode)
                .eq(FrontendModuleRuntimeStrategyEntity::getModuleCode, moduleCode)
                .eq(FrontendModuleRuntimeStrategyEntity::getDeployProfile, normalizeProfile(deployProfile))
                .last("LIMIT 1"));
    }

    private FrontendModuleRuntimeStrategyVO toVO(FrontendModuleRuntimeStrategyEntity strategy) {
        FrontendModuleRuntimeStrategyVO vo = new FrontendModuleRuntimeStrategyVO();
        vo.setStrategyId(strategy.getStrategyId());
        vo.setAppCode(strategy.getAppCode());
        vo.setModuleCode(strategy.getModuleCode());
        vo.setDeployProfile(strategy.getDeployProfile());
        vo.setPageType(strategy.getPageType());
        vo.setRuntimeCode(strategy.getRuntimeCode());
        vo.setStatus(strategy.getStatus());
        vo.setSort(strategy.getSort());
        vo.setCreateTime(strategy.getCreateTime());
        vo.setUpdateTime(strategy.getUpdateTime());
        return vo;
    }

    private String normalizeProfile(String value) {
        return StringUtils.hasText(value) ? value.trim().toLowerCase(Locale.ROOT) : "monolith";
    }

    private String defaultString(String value, String defaultValue) {
        return StringUtils.hasText(value) ? value : defaultValue;
    }
}
