package io.mango.system.core.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import io.mango.auth.api.spi.LoginTenantProvider;
import io.mango.auth.api.vo.LoginTenantVO;
import io.mango.common.result.Require;
import io.mango.identity.api.TenantMemberProvider;
import io.mango.identity.api.vo.TenantMemberVO;
import io.mango.infra.context.api.MangoContextHolder;
import io.mango.infra.context.api.MangoContextSnapshot;
import io.mango.system.api.enums.SystemCode;
import io.mango.system.api.command.SaveSysTenantCommand;
import io.mango.system.api.enums.InstitutionStatus;
import io.mango.system.api.tenant.TenantDependencyChecker;
import io.mango.system.api.tenant.TenantPackageBindingHandler;
import io.mango.system.api.tenant.TenantPackageBindingProvider;
import io.mango.system.api.tenant.TenantProvisionCommand;
import io.mango.system.api.tenant.TenantProvisioner;
import io.mango.system.api.vo.LoginTenantOptionVO;
import io.mango.system.api.vo.SysTenantVO;
import io.mango.system.core.entity.SysTenantEntity;
import io.mango.system.core.mapper.SysTenantMapper;
import io.mango.system.core.service.ISysTenantService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class SysTenantService implements ISysTenantService, LoginTenantProvider, TenantPackageBindingProvider {

    private final SysTenantMapper sysTenantMapper;
    private final TenantMemberProvider tenantMemberProvider;
    private final ObjectProvider<TenantProvisioner> tenantProvisioners;
    private final ObjectProvider<TenantDependencyChecker> tenantDependencyCheckers;
    private final ObjectProvider<TenantPackageBindingHandler> tenantPackageBindingHandlers;

    @Override
    public List<SysTenantVO> list() {
        return sysTenantMapper.selectList(null).stream().map(this::toVO).toList();
    }

    @Override
    public SysTenantVO get(Long id) {
        return toVO(requireTenant(id));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long create(SaveSysTenantCommand command) {
        Require.notNull(command, SystemCode.SYSTEM_INVALID);
        SysTenantEntity entity = new SysTenantEntity();
        copy(command, entity);
        sysTenantMapper.insert(entity);
        provisionTenant(entity);
        bindTenantPackage(entity.getId(), entity.getPackageId());
        return entity.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Boolean update(SaveSysTenantCommand command) {
        Require.notNull(command, SystemCode.SYSTEM_INVALID);
        Require.notNull(command.getId(), SystemCode.SYSTEM_INVALID, "ID不能为空");
        requireTenant(command.getId());
        SysTenantEntity entity = new SysTenantEntity();
        entity.setId(command.getId());
        copy(command, entity);
        boolean updated = sysTenantMapper.updateById(entity) > 0;
        if (updated) {
            bindTenantPackage(entity.getId(), entity.getPackageId());
        }
        return updated;
    }

    @Override
    public Boolean delete(Long id) {
        requireTenant(id);
        List<String> blockers = tenantDependencyCheckers.orderedStream()
                .map(checker -> checker.check(id))
                .flatMap(java.util.Optional::stream)
                .toList();
        Require.isTrue(blockers.isEmpty(), SystemCode.INSTITUTION_DELETE_BLOCKED, String.join("；", blockers));
        return sysTenantMapper.deleteById(id) > 0;
    }

    @Override
    public Boolean updateStatus(Long id, Integer status) {
        Require.notNull(id, SystemCode.SYSTEM_INVALID, "机构 ID 不能为空");
        Require.notNull(status, SystemCode.INSTITUTION_STATUS_INVALID);
        requireTenant(id);
        SysTenantEntity entity = new SysTenantEntity();
        entity.setId(id);
        entity.setStatus(requireValidStatus(status));
        return sysTenantMapper.updateById(entity) > 0;
    }

    @Override
    public List<LoginTenantOptionVO> listLoginOptions() {
        return enabledTenants().stream()
                .map(tenant -> new LoginTenantOptionVO(String.valueOf(tenant.getId()), tenant.getTenantCode(), tenant.getTenantName()))
                .toList();
    }

    @Override
    public Long findPackageIdByTenantId(Long tenantId) {
        if (tenantId == null) {
            return null;
        }
        SysTenantEntity tenant = sysTenantMapper.selectById(tenantId);
        if (tenant == null) {
            return null;
        }
        return tenant.getPackageId();
    }

    @Override
    public List<Long> listTenantIdsByPackage(Long packageId) {
        if (packageId == null) {
            return List.of();
        }
        return sysTenantMapper.selectList(new LambdaQueryWrapper<SysTenantEntity>()
                        .eq(SysTenantEntity::getPackageId, packageId)
                        .eq(SysTenantEntity::getStatus, InstitutionStatus.ENABLED.value())
                        .orderByAsc(SysTenantEntity::getId))
                .stream().map(SysTenantEntity::getId).toList();
    }

    @Override
    public LoginTenantVO getEnabledById(String tenantId) {
        Long id = parseTenantId(tenantId);
        if (id == null) {
            return null;
        }
        SysTenantEntity tenant = sysTenantMapper.selectOne(new LambdaQueryWrapper<SysTenantEntity>()
                .eq(SysTenantEntity::getId, id)
                .eq(SysTenantEntity::getStatus, InstitutionStatus.ENABLED.value())
                .last("LIMIT 1"));
        if (tenant == null) {
            return null;
        }
        return toLoginTenantVO(tenant);
    }

    @Override
    public LoginTenantVO getEnabledByUserAndTenantId(Long userId, String tenantId) {
        return attachMember(userId, getEnabledById(tenantId));
    }

    @Override
    public LoginTenantVO getEnabledByUserAndTenantCode(Long userId, String tenantCode) {
        return attachMember(userId, getEnabledByCode(tenantCode));
    }

    @Override
    public List<LoginTenantVO> listEnabledByUser(Long userId) {
        return tenantMemberProvider.listEnabledMembers(userId).stream()
                .map(this::fromMember)
                .filter(java.util.Objects::nonNull)
                .toList();
    }

    @Override
    public LoginTenantVO getEnabledByCode(String tenantCode) {
        if (tenantCode == null || tenantCode.isBlank()) {
            return null;
        }
        SysTenantEntity tenant = sysTenantMapper.selectOne(new LambdaQueryWrapper<SysTenantEntity>()
                .eq(SysTenantEntity::getTenantCode, tenantCode.trim())
                .eq(SysTenantEntity::getStatus, InstitutionStatus.ENABLED.value())
                .last("LIMIT 1"));
        if (tenant == null) {
            return null;
        }
        return toLoginTenantVO(tenant);
    }

    private List<SysTenantEntity> enabledTenants() {
        return sysTenantMapper.selectList(new LambdaQueryWrapper<SysTenantEntity>()
                .eq(SysTenantEntity::getStatus, InstitutionStatus.ENABLED.value())
                .orderByAsc(SysTenantEntity::getId));
    }

    private SysTenantEntity requireTenant(Long id) {
        SysTenantEntity tenant = sysTenantMapper.selectById(id);
        Require.notNull(tenant, SystemCode.INSTITUTION_NOT_FOUND);
        return tenant;
    }

    private void copy(SaveSysTenantCommand command, SysTenantEntity entity) {
        entity.setTenantName(command.getTenantName());
        entity.setTenantCode(command.getTenantCode());
        entity.setInstitutionType(firstText(command.getInstitutionType(), "ENTERPRISE"));
        entity.setCapabilityCodes(normalizeCodes(command.getCapabilityCodes()));
        entity.setPackageId(command.getPackageId());
        entity.setStatus(requireValidStatus(command.getStatus()));
        entity.setContact(command.getContact());
        entity.setMobile(command.getMobile());
        entity.setEmail(command.getEmail());
        entity.setRemark(command.getRemark());
    }

    private SysTenantVO toVO(SysTenantEntity entity) {
        SysTenantVO vo = new SysTenantVO();
        vo.setId(entity.getId());
        vo.setTenantName(entity.getTenantName());
        vo.setTenantCode(entity.getTenantCode());
        vo.setInstitutionType(firstText(entity.getInstitutionType(), "ENTERPRISE"));
        vo.setCapabilityCodes(entity.getCapabilityCodes());
        vo.setPackageId(entity.getPackageId());
        vo.setStatus(entity.getStatus());
        vo.setContact(entity.getContact());
        vo.setMobile(entity.getMobile());
        vo.setEmail(entity.getEmail());
        vo.setRemark(entity.getRemark());
        return vo;
    }

    private LoginTenantVO toLoginTenantVO(SysTenantEntity entity) {
        LoginTenantVO vo = new LoginTenantVO();
        vo.setTenantId(String.valueOf(entity.getId()));
        vo.setTenantCode(entity.getTenantCode());
        vo.setTenantName(entity.getTenantName());
        return vo;
    }

    private void provisionTenant(SysTenantEntity tenant) {
        TenantProvisionCommand context = new TenantProvisionCommand(tenant.getId(), tenant.getTenantCode(), tenant.getTenantName());
        MangoContextSnapshot original = MangoContextHolder.get();
        MangoContextHolder.set(original.withTenantId(String.valueOf(tenant.getId())));
        try {
            tenantProvisioners.orderedStream().forEach(provisioner -> provisioner.provision(context));
        } finally {
            MangoContextHolder.set(original);
        }
    }

    private void bindTenantPackage(Long tenantId, Long packageId) {
        tenantPackageBindingHandlers.orderedStream().forEach(handler -> handler.bindPackage(tenantId, packageId));
    }

    private LoginTenantVO attachMember(Long userId, LoginTenantVO tenant) {
        if (tenant == null) {
            return null;
        }
        TenantMemberVO member = tenantMemberProvider.getEnabledMember(userId, parseTenantId(tenant.getTenantId()));
        if (member == null) {
            return null;
        }
        tenant.setMemberId(member.getMemberId());
        tenant.setMemberName(member.getDisplayName());
        tenant.setMemberType(member.getMemberType());
        return tenant;
    }

    private LoginTenantVO fromMember(TenantMemberVO member) {
        SysTenantEntity tenant = sysTenantMapper.selectOne(new LambdaQueryWrapper<SysTenantEntity>()
                .eq(SysTenantEntity::getId, member.getTenantId())
                .eq(SysTenantEntity::getStatus, InstitutionStatus.ENABLED.value())
                .last("LIMIT 1"));
        if (tenant == null) {
            return null;
        }
        LoginTenantVO vo = toLoginTenantVO(tenant);
        vo.setMemberId(member.getMemberId());
        vo.setMemberName(member.getDisplayName());
        vo.setMemberType(member.getMemberType());
        return vo;
    }

    private Long parseTenantId(String tenantId) {
        if (tenantId == null || tenantId.isBlank()) {
            return null;
        }
        try {
            return Long.valueOf(tenantId.trim());
        } catch (NumberFormatException exception) {
            return null;
        }
    }

    private String firstText(String value, String fallback) {
        if (value == null || value.isBlank()) {
            return fallback;
        }
        return value.trim();
    }

    private String normalizeCodes(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return java.util.Arrays.stream(value.split(","))
                .map(String::trim)
                .filter(item -> !item.isEmpty())
                .distinct()
                .collect(Collectors.joining(","));
    }

    private Integer requireValidStatus(Integer status) {
        Require.isTrue(InstitutionStatus.valid(status), SystemCode.INSTITUTION_STATUS_INVALID);
        return status;
    }
}
