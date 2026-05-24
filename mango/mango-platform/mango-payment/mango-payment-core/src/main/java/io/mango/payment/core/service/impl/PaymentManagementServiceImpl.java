package io.mango.payment.core.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import io.mango.common.result.Require;
import io.mango.infra.context.core.MangoContextHolder;
import io.mango.payment.api.vo.PaymentManageDomainVO;
import io.mango.payment.api.vo.PaymentManageItemVO;
import io.mango.payment.api.vo.PaymentMethodVO;
import io.mango.payment.api.vo.PaymentTenantCashierVO;
import io.mango.payment.core.entity.PaymentManageDomain;
import io.mango.payment.core.entity.PaymentManageItem;
import io.mango.payment.core.entity.PaymentMethodConfig;
import io.mango.payment.core.entity.PaymentTenantCashier;
import io.mango.payment.core.mapper.PaymentManageDomainMapper;
import io.mango.payment.core.mapper.PaymentManageItemMapper;
import io.mango.payment.core.mapper.PaymentMethodConfigMapper;
import io.mango.payment.core.mapper.PaymentTenantCashierMapper;
import io.mango.payment.core.service.IPaymentManagementService;
import java.util.Arrays;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
@RequiredArgsConstructor
public class PaymentManagementServiceImpl implements IPaymentManagementService {

    private static final String ENABLED = "ENABLED";
    private static final String SANDBOX_CHANNEL = "SANDBOX";

    private final PaymentManageDomainMapper domainMapper;
    private final PaymentManageItemMapper itemMapper;
    private final PaymentTenantCashierMapper cashierMapper;
    private final PaymentMethodConfigMapper methodMapper;

    @Override
    public List<PaymentManageDomainVO> listDomains() {
        return domainMapper.selectList(new LambdaQueryWrapper<PaymentManageDomain>()
                        .orderByAsc(PaymentManageDomain::getSortOrder))
                .stream()
                .map(this::toDomainVO)
                .toList();
    }

    @Override
    public List<PaymentManageItemVO> listItems(String domain) {
        Require.notBlank(domain, "管理域不能为空");
        return itemMapper.selectList(new LambdaQueryWrapper<PaymentManageItem>()
                        .eq(PaymentManageItem::getDomain, domain)
                        .orderByAsc(PaymentManageItem::getSortOrder)
                        .orderByAsc(PaymentManageItem::getId))
                .stream()
                .map(this::toItemVO)
                .toList();
    }

    @Override
    public List<PaymentTenantCashierVO> listTenantCashiers() {
        Long tenantId = currentTenantIdOrNull();
        LambdaQueryWrapper<PaymentTenantCashier> wrapper = new LambdaQueryWrapper<PaymentTenantCashier>()
                .orderByAsc(PaymentTenantCashier::getTenantId)
                .orderByAsc(PaymentTenantCashier::getId);
        if (tenantId != null) {
            wrapper.eq(PaymentTenantCashier::getTenantId, tenantId);
        }
        List<PaymentTenantCashierVO> cashiers = cashierMapper.selectList(wrapper)
                .stream()
                .map(this::toCashierVO)
                .toList();
        if (!cashiers.isEmpty() || tenantId == null) {
            return cashiers;
        }
        return cashierMapper.selectList(new LambdaQueryWrapper<PaymentTenantCashier>()
                        .orderByAsc(PaymentTenantCashier::getTenantId)
                        .orderByAsc(PaymentTenantCashier::getId))
                .stream()
                .map(this::toCashierVO)
                .toList();
    }

    @Override
    public List<PaymentMethodVO> listSandboxMethods() {
        return methodMapper.selectList(new LambdaQueryWrapper<PaymentMethodConfig>()
                        .eq(PaymentMethodConfig::getChannelCode, SANDBOX_CHANNEL)
                        .eq(PaymentMethodConfig::getStatus, ENABLED)
                        .orderByAsc(PaymentMethodConfig::getSortOrder))
                .stream()
                .map(this::toMethodVO)
                .toList();
    }

    private PaymentManageDomainVO toDomainVO(PaymentManageDomain entity) {
        PaymentManageDomainVO vo = new PaymentManageDomainVO();
        vo.setCode(entity.getCode());
        vo.setTitle(entity.getTitle());
        vo.setDescription(entity.getDescription());
        vo.setBadge(entity.getBadge());
        vo.setSortOrder(entity.getSortOrder());
        return vo;
    }

    private PaymentManageItemVO toItemVO(PaymentManageItem entity) {
        PaymentManageItemVO vo = new PaymentManageItemVO();
        vo.setId(entity.getId());
        vo.setDomain(entity.getDomain());
        vo.setCode(entity.getCode());
        vo.setName(entity.getName());
        vo.setOwner(entity.getOwner());
        vo.setStatus(entity.getStatus());
        vo.setPrimaryText(entity.getPrimaryText());
        vo.setSecondaryText(entity.getSecondaryText());
        vo.setUpdatedAt(entity.getUpdatedAt());
        return vo;
    }

    private PaymentTenantCashierVO toCashierVO(PaymentTenantCashier entity) {
        PaymentTenantCashierVO vo = new PaymentTenantCashierVO();
        vo.setTenantId(entity.getTenantId());
        vo.setTenantName(entity.getTenantName());
        vo.setAppCode(entity.getAppCode());
        vo.setCashierCode(entity.getCashierCode());
        vo.setCashierName(entity.getCashierName());
        vo.setEnabledMethods(parseMethods(entity.getEnabledMethods()));
        vo.setDefaultMethod(entity.getDefaultMethod());
        vo.setExpireMinutes(entity.getExpireMinutes());
        vo.setDailyLimit(entity.getDailyLimit());
        return vo;
    }

    private PaymentMethodVO toMethodVO(PaymentMethodConfig entity) {
        PaymentMethodVO vo = new PaymentMethodVO();
        vo.setCode(entity.getMethodCode());
        vo.setLabel(entity.getMethodName());
        vo.setChannelCode(entity.getChannelCode());
        vo.setStatus(entity.getStatus());
        vo.setSingleLimit(entity.getSingleLimit());
        vo.setSortOrder(entity.getSortOrder());
        return vo;
    }

    private List<String> parseMethods(String methods) {
        if (!StringUtils.hasText(methods)) {
            return List.of();
        }
        return Arrays.stream(methods.split(","))
                .map(String::trim)
                .filter(StringUtils::hasText)
                .toList();
    }

    private Long currentTenantIdOrNull() {
        String tenantId = MangoContextHolder.tenantId();
        if (!StringUtils.hasText(tenantId)) {
            return null;
        }
        try {
            return Long.valueOf(tenantId);
        } catch (NumberFormatException e) {
            return Require.fail(400, "当前机构上下文不是有效数字: " + tenantId);
        }
    }
}
