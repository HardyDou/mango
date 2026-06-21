package io.mango.authorization.core.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import io.mango.authorization.api.command.FrontendModuleRuntimeStrategyCommand;
import io.mango.authorization.api.vo.FrontendModuleRuntimeStrategyVO;
import io.mango.authorization.core.config.FrontendRuntimeProperties;
import io.mango.authorization.core.entity.FrontendModuleRuntimeStrategy;
import io.mango.authorization.core.mapper.FrontendModuleRuntimeStrategyMapper;
import io.mango.authorization.core.service.IFrontendRuntimeStrategyService;
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
public class FrontendRuntimeStrategyServiceImpl implements IFrontendRuntimeStrategyService {

    private final FrontendModuleRuntimeStrategyMapper strategyMapper;
    private final FrontendRuntimeProperties properties;

    @Override
    public String currentDeployProfile() {
        return normalizeProfile(properties.getDeployProfile());
    }

    @Override
    public List<FrontendModuleRuntimeStrategyVO> list(String appCode, String deployProfile, Integer status) {
        LambdaQueryWrapper<FrontendModuleRuntimeStrategy> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(StringUtils.hasText(appCode), FrontendModuleRuntimeStrategy::getAppCode, appCode)
                .eq(StringUtils.hasText(deployProfile), FrontendModuleRuntimeStrategy::getDeployProfile, normalizeProfile(deployProfile))
                .eq(status != null, FrontendModuleRuntimeStrategy::getStatus, status)
                .orderByAsc(FrontendModuleRuntimeStrategy::getSort)
                .orderByAsc(FrontendModuleRuntimeStrategy::getModuleCode);
        return strategyMapper.selectList(wrapper).stream().map(this::toVO).toList();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long save(FrontendModuleRuntimeStrategyCommand command) {
        FrontendModuleRuntimeStrategy strategy = find(
                command.getAppCode(),
                command.getModuleCode(),
                command.getDeployProfile());
        LocalDateTime now = LocalDateTime.now();
        if (strategy == null) {
            strategy = new FrontendModuleRuntimeStrategy();
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
        if (strategyId == null) {
            return false;
        }
        FrontendModuleRuntimeStrategy strategy = strategyMapper.selectById(strategyId);
        if (strategy == null) {
            return false;
        }
        strategy.setStatus(0);
        strategy.setUpdateTime(LocalDateTime.now());
        return strategyMapper.updateById(strategy) > 0;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Boolean disable(String appCode, String moduleCode, String deployProfile) {
        FrontendModuleRuntimeStrategy strategy = find(appCode, moduleCode, deployProfile);
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
        if (strategyId == null) {
            return false;
        }
        return strategyMapper.deleteById(strategyId) > 0;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Boolean delete(String appCode, String moduleCode, String deployProfile) {
        if (!StringUtils.hasText(appCode) || !StringUtils.hasText(moduleCode)) {
            return false;
        }
        return strategyMapper.delete(new LambdaQueryWrapper<FrontendModuleRuntimeStrategy>()
                .eq(FrontendModuleRuntimeStrategy::getAppCode, appCode)
                .eq(FrontendModuleRuntimeStrategy::getModuleCode, moduleCode)
                .eq(FrontendModuleRuntimeStrategy::getDeployProfile, normalizeProfile(deployProfile))) > 0;
    }

    private FrontendModuleRuntimeStrategy find(String appCode, String moduleCode, String deployProfile) {
        if (!StringUtils.hasText(appCode) || !StringUtils.hasText(moduleCode)) {
            return null;
        }
        return strategyMapper.selectOne(new LambdaQueryWrapper<FrontendModuleRuntimeStrategy>()
                .eq(FrontendModuleRuntimeStrategy::getAppCode, appCode)
                .eq(FrontendModuleRuntimeStrategy::getModuleCode, moduleCode)
                .eq(FrontendModuleRuntimeStrategy::getDeployProfile, normalizeProfile(deployProfile))
                .last("LIMIT 1"));
    }

    private FrontendModuleRuntimeStrategyVO toVO(FrontendModuleRuntimeStrategy strategy) {
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
