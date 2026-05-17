package io.mango.authorization.core.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import io.mango.authorization.api.command.TenantAppBindingCommand;
import io.mango.authorization.api.vo.TenantAppBindingVO;
import io.mango.authorization.core.entity.TenantAppBinding;
import io.mango.authorization.core.mapper.TenantAppBindingMapper;
import io.mango.authorization.core.service.ITenantAppBindingService;
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
public class TenantAppBindingServiceImpl implements ITenantAppBindingService {

    private final TenantAppBindingMapper bindingMapper;

    @Override
    public List<TenantAppBindingVO> list(Long tenantId, String appCode, Integer status) {
        LambdaQueryWrapper<TenantAppBinding> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(tenantId != null, TenantAppBinding::getTenantId, tenantId)
                .eq(StringUtils.hasText(appCode), TenantAppBinding::getAppCode, appCode)
                .eq(status != null, TenantAppBinding::getStatus, status)
                .orderByAsc(TenantAppBinding::getTenantId)
                .orderByAsc(TenantAppBinding::getAppCode);
        return bindingMapper.selectList(wrapper).stream().map(this::toVO).toList();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long enable(TenantAppBindingCommand command) {
        TenantAppBinding existing = get(command.getTenantId(), command.getAppCode());
        LocalDateTime now = LocalDateTime.now();
        if (existing != null) {
            existing.setStatus(command.getStatus() == null ? 1 : command.getStatus());
            existing.setExpireTime(command.getExpireTime());
            existing.setUpdateTime(now);
            bindingMapper.updateById(existing);
            return existing.getBindingId();
        }
        TenantAppBinding binding = new TenantAppBinding();
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
        TenantAppBinding existing = get(tenantId, appCode);
        if (existing == null) {
            return false;
        }
        existing.setStatus(0);
        existing.setUpdateTime(LocalDateTime.now());
        return bindingMapper.updateById(existing) > 0;
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
        TenantAppBinding binding = get(tenantId, appCode);
        if (binding == null || !Integer.valueOf(1).equals(binding.getStatus())) {
            return false;
        }
        return binding.getExpireTime() == null || binding.getExpireTime().isAfter(LocalDateTime.now());
    }

    private TenantAppBinding get(Long tenantId, String appCode) {
        if (tenantId == null || !StringUtils.hasText(appCode)) {
            return null;
        }
        return bindingMapper.selectOne(new LambdaQueryWrapper<TenantAppBinding>()
                .eq(TenantAppBinding::getTenantId, tenantId)
                .eq(TenantAppBinding::getAppCode, appCode)
                .last("LIMIT 1"));
    }

    private TenantAppBindingVO toVO(TenantAppBinding binding) {
        TenantAppBindingVO vo = new TenantAppBindingVO();
        vo.setBindingId(binding.getBindingId());
        vo.setTenantId(binding.getTenantId());
        vo.setAppCode(binding.getAppCode());
        vo.setStatus(binding.getStatus());
        vo.setExpireTime(binding.getExpireTime());
        vo.setCreateTime(binding.getCreateTime());
        vo.setUpdateTime(binding.getUpdateTime());
        return vo;
    }
}
