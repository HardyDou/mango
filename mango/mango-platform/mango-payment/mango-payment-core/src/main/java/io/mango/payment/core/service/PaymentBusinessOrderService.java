package io.mango.payment.core.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.IdWorker;
import io.mango.common.result.Require;
import io.mango.common.vo.PageResult;
import io.mango.payment.api.PaymentCode;
import io.mango.payment.api.command.CreatePaymentBusinessOrderCommand;
import io.mango.payment.api.enums.PaymentBusinessOrderStatusEnum;
import io.mango.payment.api.query.PaymentConfigPageQuery;
import io.mango.payment.api.vo.PaymentBusinessOrderStatusVO;
import io.mango.payment.api.vo.PaymentBusinessOrderVO;
import io.mango.payment.core.entity.PaymentApplication;
import io.mango.payment.core.entity.PaymentBusinessOrderEntity;
import io.mango.payment.core.entity.PaymentEnterpriseSubject;
import io.mango.payment.core.mapper.PaymentApplicationMapper;
import io.mango.payment.core.mapper.PaymentBusinessOrderMapper;
import io.mango.payment.core.mapper.PaymentEnterpriseSubjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class PaymentBusinessOrderService {

    private final PaymentApplicationMapper applicationMapper;
    private final PaymentEnterpriseSubjectMapper enterpriseSubjectMapper;
    private final PaymentBusinessOrderMapper businessOrderMapper;
    private final PaymentOrderStatusFlowService statusFlowService;
    private final PaymentNumberService numberService;
    private final PaymentOrderViewSupport viewSupport;

    public PageResult<PaymentBusinessOrderVO> pageBusinessOrders(PaymentConfigPageQuery query) {
        PaymentConfigPageQuery resolved = query == null ? new PaymentConfigPageQuery() : query;
        String keyword = PaymentContextSupport.trimToNull(resolved.getKeyword());
        String statusCode = PaymentContextSupport.trimToNull(resolved.getStatusCode());
        Long tenantId = PaymentContextSupport.currentTenantId();
        Long applicationId = resolved.getApplicationId();
        Long enterpriseSubjectId = resolved.getEnterpriseSubjectId();
        long total = businessOrderMapper.countBusinessOrders(tenantId, keyword, statusCode, applicationId, enterpriseSubjectId);
        long page = resolved.getPage();
        long size = resolved.getSize();
        List<PaymentBusinessOrderVO> rows = businessOrderMapper.selectBusinessOrderPage(
                tenantId, keyword, statusCode, applicationId, enterpriseSubjectId, size, (page - 1) * size);
        rows.forEach(this::fillBusinessOrderStatusName);
        return PageResult.of(rows, total, page, size);
    }

    public PaymentBusinessOrderVO detailBusinessOrder(Long id) {
        Require.notNull(id, PaymentCode.PAYMENT_READONLY_RESOURCE_INVALID.getCode(), "业务订单 ID 不能为空");
        PaymentBusinessOrderVO vo = businessOrderMapper.selectBusinessOrderDetail(PaymentContextSupport.currentTenantId(), id);
        Require.notNull(vo, PaymentCode.PAYMENT_READONLY_RESOURCE_NOT_FOUND);
        fillBusinessOrderStatusName(vo);
        vo.setStatusFlows(viewSupport.listBusinessStatusFlows(id));
        return vo;
    }

    public List<PaymentBusinessOrderStatusVO> listBusinessOrderStatuses() {
        return PaymentBusinessOrderStatusEnum.options().stream().map(status -> {
            PaymentBusinessOrderStatusVO vo = new PaymentBusinessOrderStatusVO();
            vo.setStatusCode(status.getCode());
            vo.setStatusName(status.getLabel());
            return vo;
        }).toList();
    }

    @Transactional(rollbackFor = Exception.class)
    public PaymentBusinessOrderVO createBusinessOrder(CreatePaymentBusinessOrderCommand command) {
        Require.notNull(command, PaymentCode.PAYMENT_BUSINESS_ORDER_INVALID.getCode(), "创建业务订单命令不能为空");
        Require.notBlank(command.getAppId(), PaymentCode.PAYMENT_BUSINESS_ORDER_INVALID.getCode(), "AppId 不能为空");
        Require.notBlank(command.getTitle(), PaymentCode.PAYMENT_BUSINESS_ORDER_INVALID.getCode(), "支付标题不能为空");
        Require.notNull(command.getSubjectId(), PaymentCode.PAYMENT_BUSINESS_ORDER_INVALID.getCode(), "收款主体 ID 不能为空");
        Require.notNull(command.getAmount(), PaymentCode.PAYMENT_BUSINESS_ORDER_INVALID.getCode(), "订单金额不能为空");
        Require.isTrue(command.getAmount() > 0, PaymentCode.PAYMENT_AMOUNT_INVALID);
        String appId = command.getAppId().trim();
        LocalDateTime now = LocalDateTime.now();
        String bizOrderNo = PaymentContextSupport.trimToNull(command.getBizOrderNo());
        if (bizOrderNo == null) {
            bizOrderNo = numberService.next(PaymentNumberService.PAY_BIZ_ORDER_NO);
        }
        Long tenantId = PaymentContextSupport.currentTenantId();
        PaymentApplication application = selectRequiredApplication(tenantId, appId);
        PaymentEnterpriseSubject subject = selectRequiredSubject(tenantId, command.getSubjectId());
        Require.isTrue(businessOrderMapper.selectOne(new LambdaQueryWrapper<PaymentBusinessOrderEntity>()
                        .eq(PaymentBusinessOrderEntity::getTenantId, tenantId)
                        .eq(PaymentBusinessOrderEntity::getAppCode, application.getAppId())
                        .eq(PaymentBusinessOrderEntity::getBizOrderNo, bizOrderNo)) == null,
                PaymentCode.PAYMENT_BUSINESS_ORDER_INVALID.getCode(), "业务订单号已存在");

        PaymentBusinessOrderEntity entity = new PaymentBusinessOrderEntity();
        entity.setId(IdWorker.getId());
        entity.setBizOrderNo(bizOrderNo);
        entity.setAppCode(application.getAppId());
        entity.setTitle(command.getTitle().trim());
        entity.setSubjectId(subject.getId());
        entity.setAmount(command.getAmount());
        entity.setPaidAmount(0L);
        entity.setRefundedAmount(0L);
        entity.setCurrency(PaymentContextSupport.trimToNull(command.getCurrency()) == null ? "CNY" : command.getCurrency().trim());
        entity.setStatus(PaymentBusinessOrderStatusEnum.TO_PAY.getCode());
        entity.setExpireTime(command.getExpireTime());
        entity.setNotifyUrl(PaymentContextSupport.trimToNull(command.getNotifyUrl()));
        entity.setReturnUrl(PaymentContextSupport.trimToNull(command.getReturnUrl()));
        entity.setExtendInfo(PaymentContextSupport.trimToNull(command.getExtendInfo()));
        entity.setTenantId(tenantId);
        entity.setCreatedBy(PaymentContextSupport.currentUserId());
        entity.setCreatedAt(now);
        entity.setUpdatedBy(PaymentContextSupport.currentUserId());
        entity.setUpdatedAt(now);
        entity.setDelFlag(0);
        businessOrderMapper.insert(entity);
        statusFlowService.record(
                tenantId,
                PaymentOrderStatusFlowService.ORDER_TYPE_BUSINESS,
                entity.getId(),
                entity.getBizOrderNo(),
                null,
                PaymentBusinessOrderStatusEnum.TO_PAY.getCode(),
                "MANUAL_CREATE_BUSINESS_ORDER",
                entity.getBizOrderNo(),
                now,
                "后台创建业务订单");
        return detailBusinessOrder(entity.getId());
    }

    private void fillBusinessOrderStatusName(PaymentBusinessOrderVO vo) {
        if (viewSupport.isExpiredOpenBusinessOrder(vo.getStatus(), vo.getExpireTime())) {
            vo.setStatusName("已过期");
            vo.setPayable(false);
            vo.setPayDisabledReason("订单已过期");
            return;
        }
        vo.setStatusName(PaymentBusinessOrderStatusEnum.labelOf(vo.getStatus()));
        if (PaymentBusinessOrderStatusEnum.TO_PAY.getCode().equals(vo.getStatus())
                || PaymentBusinessOrderStatusEnum.PAYING.getCode().equals(vo.getStatus())) {
            vo.setPayable(vo.getCashierConfigId() != null);
            vo.setPayDisabledReason(vo.getCashierConfigId() == null ? "当前业务订单未匹配收银台配置" : null);
            return;
        }
        vo.setPayable(false);
        vo.setPayDisabledReason("当前状态不可发起支付");
    }

    private PaymentApplication selectRequiredApplication(Long tenantId, String appId) {
        PaymentApplication application = applicationMapper.selectOne(new LambdaQueryWrapper<PaymentApplication>()
                .eq(PaymentApplication::getTenantId, tenantId)
                .eq(PaymentApplication::getAppId, appId));
        Require.notNull(application, PaymentCode.PAYMENT_APPLICATION_NOT_FOUND);
        Require.isTrue(Integer.valueOf(1).equals(application.getStatus()),
                PaymentCode.PAYMENT_APPLICATION_INVALID.getCode(), "支付应用未启用");
        return application;
    }

    private PaymentEnterpriseSubject selectRequiredSubject(Long tenantId, Long subjectId) {
        PaymentEnterpriseSubject subject = enterpriseSubjectMapper.selectById(subjectId);
        Require.notNull(subject, PaymentCode.PAYMENT_ENTERPRISE_SUBJECT_NOT_FOUND);
        Require.isTrue(tenantId.equals(subject.getTenantId()), PaymentCode.PAYMENT_ENTERPRISE_SUBJECT_NOT_FOUND);
        Require.isTrue(Integer.valueOf(1).equals(subject.getStatus()),
                PaymentCode.PAYMENT_ENTERPRISE_SUBJECT_INVALID.getCode(), "收款主体未启用");
        return subject;
    }
}
