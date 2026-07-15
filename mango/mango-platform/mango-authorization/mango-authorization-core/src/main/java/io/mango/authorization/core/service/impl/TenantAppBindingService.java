package io.mango.authorization.core.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import io.mango.authorization.api.enums.AuthorizationCode;
import io.mango.authorization.api.command.TenantAppBindingCommand;
import io.mango.authorization.api.query.TenantAppBindingQuery;
import io.mango.authorization.api.vo.TenantAppBindingVO;
import io.mango.authorization.core.entity.TenantAppBindingEntity;
import io.mango.authorization.core.mapper.TenantAppBindingMapper;
import io.mango.authorization.core.service.ITenantAppBindingService;
import io.mango.common.result.Require;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 租户应用开通服务实现。
 */
@Service
@RequiredArgsConstructor
public class TenantAppBindingService implements ITenantAppBindingService {

    private final TenantAppBindingMapper bindingMapper;

    @Override
    public List<TenantAppBindingVO> list(TenantAppBindingQuery query) {
        LambdaQueryWrapper<TenantAppBindingEntity> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(query.getTenantId() != null, TenantAppBindingEntity::getTenantId, query.getTenantId())
                .eq(StringUtils.hasText(query.getAppCode()), TenantAppBindingEntity::getAppCode, query.getAppCode())
                .eq(query.getStatus() != null, TenantAppBindingEntity::getStatus, query.getStatus())
                .orderByAsc(TenantAppBindingEntity::getTenantId)
                .orderByAsc(TenantAppBindingEntity::getAppCode);
        return bindingMapper.selectList(wrapper).stream().map(this::toVO).toList();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long enable(TenantAppBindingCommand command) {
        Require.notNull(command, AuthorizationCode.AUTHORIZATION_BUSINESS_ERROR, "租户应用开通命令不能为空");
        TenantAppBindingEntity existing = get(command.getTenantId(), command.getAppCode());
        LocalDateTime now = LocalDateTime.now();
        if (existing != null) {
            existing.setStatus(command.getStatus() == null ? 1 : command.getStatus());
            existing.setExpireTime(command.getExpireTime());
            existing.setUpdateTime(now);
            bindingMapper.updateById(existing);
            return existing.getBindingId();
        }
        TenantAppBindingEntity binding = new TenantAppBindingEntity();
        binding.setTenantId(command.getTenantId());
        binding.setAppCode(command.getAppCode());
        binding.setStatus(command.getStatus() == null ? 1 : command.getStatus());
        binding.setExpireTime(command.getExpireTime());
        binding.setCreateTime(now);
        binding.setUpdateTime(now);
        bindingMapper.insert(binding);
        return binding.getBindingId();
    }

    @Override
    public Boolean disable(Long tenantId, String appCode) {
        Require.notNull(tenantId, AuthorizationCode.AUTHORIZATION_BUSINESS_ERROR, "租户ID不能为空");
        TenantAppBindingEntity existing = get(tenantId, appCode);
        if (existing == null) {
            return false;
        }
        existing.setStatus(0);
        existing.setUpdateTime(LocalDateTime.now());
        return bindingMapper.updateById(existing) > 0;
    }

    @Override
    public Boolean disableRequired(Long tenantId, String appCode) {
        boolean disabled = Boolean.TRUE.equals(disable(tenantId, appCode));
        Require.isTrue(disabled, AuthorizationCode.AUTHORIZATION_NOT_FOUND, "租户应用开通关系不存在");
        return true;
    }

    @Override
    public void ensureEnabled(Long tenantId, String appCode) {
        if (tenantId == null || !StringUtils.hasText(appCode)) {
            return;
        }
        TenantAppBindingCommand command = new TenantAppBindingCommand();
        command.setTenantId(tenantId);
        command.setAppCode(appCode);
        command.setStatus(1);
        enable(command);
    }

    @Override
    public boolean isEnabled(Long tenantId, String appCode) {
        if (tenantId == null || !StringUtils.hasText(appCode)) {
            return false;
        }
        TenantAppBindingEntity binding = get(tenantId, appCode);
        if (binding == null || !Integer.valueOf(1).equals(binding.getStatus())) {
            return false;
        }
        return binding.getExpireTime() == null || binding.getExpireTime().isAfter(LocalDateTime.now());
    }

    private TenantAppBindingEntity get(Long tenantId, String appCode) {
        if (tenantId == null || !StringUtils.hasText(appCode)) {
            return null;
        }
        return bindingMapper.selectOne(new LambdaQueryWrapper<TenantAppBindingEntity>()
                .eq(TenantAppBindingEntity::getTenantId, tenantId)
                .eq(TenantAppBindingEntity::getAppCode, appCode)
                .last("LIMIT 1"));
    }

    private TenantAppBindingVO toVO(TenantAppBindingEntity binding) {
        TenantAppBindingVO vo = new TenantAppBindingVO();
        vo.setBindingId(binding.getBindingId());
        vo.setTenantId(binding.getTenantIdAsLong());
        vo.setAppCode(binding.getAppCode());
        vo.setStatus(binding.getStatus());
        vo.setExpireTime(binding.getExpireTime());
        vo.setCreateTime(binding.getCreateTime());
        vo.setUpdateTime(binding.getUpdateTime());
        return vo;
    }
}
