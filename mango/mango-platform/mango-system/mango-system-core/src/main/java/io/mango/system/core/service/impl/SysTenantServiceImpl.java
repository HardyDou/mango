package io.mango.system.core.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import io.mango.auth.api.spi.LoginTenantProvider;
import io.mango.auth.api.vo.LoginTenantVO;
import io.mango.common.result.Require;
import io.mango.common.result.R;
import io.mango.identity.api.TenantMemberProvider;
import io.mango.identity.api.vo.TenantMemberInfo;
import io.mango.system.api.SystemCode;
import io.mango.system.api.enums.InstitutionStatus;
import io.mango.system.api.tenant.TenantDependencyChecker;
import io.mango.system.api.tenant.TenantPackageBindingHandler;
import io.mango.system.api.tenant.TenantProvisionContext;
import io.mango.system.api.tenant.TenantProvisioner;
import io.mango.system.api.po.SysTenantPo;
import io.mango.system.core.entity.SysTenant;
import io.mango.system.core.mapper.SysTenantMapper;
import io.mango.system.core.service.ISysTenantService;
import io.mango.infra.context.core.MangoContextHolder;
import io.mango.infra.context.core.MangoContextSnapshot;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class SysTenantServiceImpl implements ISysTenantService, LoginTenantProvider {

    private final SysTenantMapper sysTenantMapper;
    private final TenantMemberProvider tenantMemberProvider;
    private final ObjectProvider<TenantProvisioner> tenantProvisioners;
    private final ObjectProvider<TenantDependencyChecker> tenantDependencyCheckers;
    private final ObjectProvider<TenantPackageBindingHandler> tenantPackageBindingHandlers;

    @Override
    public R<List<SysTenantPo>> list() {
        List<SysTenant> list = sysTenantMapper.selectList(null);
        List<SysTenantPo> poList = list.stream().map(this::convertToPo).collect(Collectors.toList());
        return R.ok(poList);
    }

    @Override
    public R<SysTenantPo> get(Long id) {
        SysTenant entity = sysTenantMapper.selectById(id);
        if (entity == null) {
            return R.fail(SystemCode.INSTITUTION_NOT_FOUND);
        }
        return R.ok(convertToPo(entity));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public R<Long> create(SysTenantPo po) {
        SysTenant entity = new SysTenant();
        entity.setTenantName(po.getTenantName());
        entity.setTenantCode(po.getTenantCode());
        entity.setInstitutionType(firstText(po.getInstitutionType(), "ENTERPRISE"));
        entity.setCapabilityCodes(normalizeCodes(po.getCapabilityCodes()));
        entity.setPackageId(po.getPackageId());
        entity.setStatus(requireValidStatus(po.getStatus()));
        entity.setContact(po.getContact());
        entity.setMobile(po.getMobile());
        entity.setEmail(po.getEmail());
        entity.setRemark(po.getRemark());
        sysTenantMapper.insert(entity);
        provisionTenant(entity);
        bindTenantPackage(entity.getId(), entity.getPackageId());
        return R.ok(entity.getId());
    }

    @Override
    public R<Boolean> update(SysTenantPo po) {
        if (po.getId() == null) {
            return R.fail("ID不能为空");
        }
        SysTenant entity = new SysTenant();
        entity.setId(po.getId());
        entity.setTenantName(po.getTenantName());
        entity.setTenantCode(po.getTenantCode());
        entity.setInstitutionType(firstText(po.getInstitutionType(), "ENTERPRISE"));
        entity.setCapabilityCodes(normalizeCodes(po.getCapabilityCodes()));
        entity.setPackageId(po.getPackageId());
        entity.setStatus(requireValidStatus(po.getStatus()));
        entity.setContact(po.getContact());
        entity.setMobile(po.getMobile());
        entity.setEmail(po.getEmail());
        entity.setRemark(po.getRemark());
        boolean updated = sysTenantMapper.updateById(entity) > 0;
        if (updated) {
            bindTenantPackage(entity.getId(), entity.getPackageId());
        }
        return R.ok(updated);
    }

    @Override
    public R<Boolean> delete(Long id) {
        SysTenant tenant = sysTenantMapper.selectById(id);
        Require.notNull(tenant, SystemCode.INSTITUTION_NOT_FOUND);
        List<String> blockers = tenantDependencyCheckers.orderedStream()
                .map(checker -> checker.check(id))
                .filter(java.util.Optional::isPresent)
                .map(java.util.Optional::get)
                .toList();
        Require.isTrue(blockers.isEmpty(), SystemCode.INSTITUTION_DELETE_BLOCKED.getCode(),
                String.join("；", blockers));
        return R.ok(sysTenantMapper.deleteById(id) > 0);
    }

    @Override
    public R<Boolean> updateStatus(Long id, Integer status) {
        SysTenant tenant = sysTenantMapper.selectById(id);
        Require.notNull(tenant, SystemCode.INSTITUTION_NOT_FOUND);
        SysTenant entity = new SysTenant();
        entity.setId(id);
        entity.setStatus(requireValidStatus(status));
        return R.ok(sysTenantMapper.updateById(entity) > 0);
    }

    @Override
    public R<List<LoginTenantVO>> listLoginOptions() {
        List<SysTenant> list = sysTenantMapper.selectList(new LambdaQueryWrapper<SysTenant>()
                .eq(SysTenant::getStatus, InstitutionStatus.ENABLED.value())
                .orderByAsc(SysTenant::getId));
        return R.ok(list.stream().map(this::convertToLoginTenantVO).collect(Collectors.toList()));
    }

    @Override
    public LoginTenantVO getEnabledById(String tenantId) {
        if (tenantId == null || tenantId.isBlank()) {
            return null;
        }
        Long id;
        try {
            id = Long.valueOf(tenantId.trim());
        } catch (NumberFormatException e) {
            return null;
        }
        SysTenant tenant = sysTenantMapper.selectOne(new LambdaQueryWrapper<SysTenant>()
                .eq(SysTenant::getId, id)
                .eq(SysTenant::getStatus, InstitutionStatus.ENABLED.value())
                .last("LIMIT 1"));
        return tenant == null ? null : convertToLoginTenantVO(tenant);
    }

    @Override
    public LoginTenantVO getEnabledByUserAndTenantId(Long userId, String tenantId) {
        LoginTenantVO tenant = getEnabledById(tenantId);
        if (tenant == null) {
            return null;
        }
        return attachMember(userId, tenant);
    }

    @Override
    public LoginTenantVO getEnabledByUserAndTenantCode(Long userId, String tenantCode) {
        LoginTenantVO tenant = getEnabledByCode(tenantCode);
        if (tenant == null) {
            return null;
        }
        return attachMember(userId, tenant);
    }

    @Override
    public List<LoginTenantVO> listEnabledByUser(Long userId) {
        return tenantMemberProvider.listEnabledMembers(userId)
                .stream()
                .map(this::convertMemberToLoginTenantVO)
                .filter(vo -> vo != null)
                .toList();
    }

    @Override
    public LoginTenantVO getEnabledByCode(String tenantCode) {
        if (tenantCode == null || tenantCode.isBlank()) {
            return null;
        }
        SysTenant tenant = sysTenantMapper.selectOne(new LambdaQueryWrapper<SysTenant>()
                .eq(SysTenant::getTenantCode, tenantCode.trim())
                .eq(SysTenant::getStatus, InstitutionStatus.ENABLED.value())
                .last("LIMIT 1"));
        return tenant == null ? null : convertToLoginTenantVO(tenant);
    }

    private SysTenantPo convertToPo(SysTenant entity) {
        SysTenantPo po = new SysTenantPo();
        po.setId(entity.getId());
        po.setTenantName(entity.getTenantName());
        po.setTenantCode(entity.getTenantCode());
        po.setInstitutionType(firstText(entity.getInstitutionType(), "ENTERPRISE"));
        po.setCapabilityCodes(entity.getCapabilityCodes());
        po.setPackageId(entity.getPackageId());
        po.setStatus(entity.getStatus());
        po.setContact(entity.getContact());
        po.setMobile(entity.getMobile());
        po.setEmail(entity.getEmail());
        po.setRemark(entity.getRemark());
        return po;
    }

    private LoginTenantVO convertToLoginTenantVO(SysTenant entity) {
        LoginTenantVO vo = new LoginTenantVO();
        vo.setTenantId(String.valueOf(entity.getId()));
        vo.setTenantCode(entity.getTenantCode());
        vo.setTenantName(entity.getTenantName());
        return vo;
    }

    private void provisionTenant(SysTenant tenant) {
        TenantProvisionContext context = new TenantProvisionContext(
                tenant.getId(),
                tenant.getTenantCode(),
                tenant.getTenantName());
        MangoContextSnapshot original = MangoContextHolder.get();
        MangoContextHolder.set(original.withTenantId(String.valueOf(tenant.getId())));
        try {
            tenantProvisioners.orderedStream().forEach(provisioner -> provisioner.provision(context));
        } finally {
            MangoContextHolder.set(original);
        }
    }

    private void bindTenantPackage(Long tenantId, Long packageId) {
        tenantPackageBindingHandlers.orderedStream()
                .forEach(handler -> handler.bindPackage(tenantId, packageId));
    }

    private LoginTenantVO attachMember(Long userId, LoginTenantVO tenant) {
        Long tenantId = parseTenantId(tenant.getTenantId());
        TenantMemberInfo member = tenantMemberProvider.getEnabledMember(userId, tenantId);
        if (member == null) {
            return null;
        }
        tenant.setMemberId(member.getMemberId());
        tenant.setMemberName(member.getDisplayName());
        tenant.setMemberType(member.getMemberType());
        return tenant;
    }

    private LoginTenantVO convertMemberToLoginTenantVO(TenantMemberInfo member) {
        SysTenant tenant = sysTenantMapper.selectOne(new LambdaQueryWrapper<SysTenant>()
                .eq(SysTenant::getId, member.getTenantId())
                .eq(SysTenant::getStatus, InstitutionStatus.ENABLED.value())
                .last("LIMIT 1"));
        if (tenant == null) {
            return null;
        }
        LoginTenantVO vo = convertToLoginTenantVO(tenant);
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
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private String firstText(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value.trim();
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
