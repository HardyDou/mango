package io.mango.payment.core.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import io.mango.common.result.R;
import io.mango.common.result.Require;
import io.mango.common.vo.PageResult;
import io.mango.payment.api.PaymentCode;
import io.mango.payment.api.command.CreatePaymentApplicationCommand;
import io.mango.payment.api.command.SavePaymentApplicationCommand;
import io.mango.payment.api.command.UpdatePaymentApplicationCommand;
import io.mango.payment.api.query.PaymentConfigPageQuery;
import io.mango.payment.api.vo.PaymentApplicationSaveResultVO;
import io.mango.payment.api.vo.PaymentApplicationVO;
import io.mango.payment.core.entity.PaymentApplication;
import io.mango.payment.core.mapper.PaymentApplicationMapper;
import io.mango.payment.core.service.IPaymentApplicationService;
import io.mango.payment.core.service.PaymentContextSupport;
import io.mango.payment.core.service.PaymentOperationAuditService;
import io.mango.payment.core.service.PaymentSensitiveValueService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class PaymentApplicationServiceImpl implements IPaymentApplicationService {

    private static final SecureRandom SECURE_RANDOM = new SecureRandom();
    private static final int DEFAULT_SECRET_BYTES = 32;
    private static final int DEFAULT_APP_ID_BYTES = 16;

    private final PaymentApplicationMapper applicationMapper;
    private final PaymentOperationAuditService auditService;
    private final PaymentSensitiveValueService sensitiveValueService;

    @Override
    public R<PageResult<PaymentApplicationVO>> pageApplications(PaymentConfigPageQuery query) {
        PaymentConfigPageQuery resolved = query == null ? new PaymentConfigPageQuery() : query;
        IPage<PaymentApplication> page = applicationMapper.selectPage(new Page<>(resolved.getPage(), resolved.getSize()), wrapper(resolved));
        List<PaymentApplicationVO> records = page.getRecords().stream().map(this::toVO).collect(Collectors.toList());
        return R.ok(PageResult.of(records, page.getTotal(), page.getCurrent(), page.getSize()));
    }

    @Override
    public R<PaymentApplicationVO> detailApplication(Long id) {
        return R.ok(toVO(selectRequired(id)));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public R<PaymentApplicationSaveResultVO> createApplication(CreatePaymentApplicationCommand command) {
        Require.notNull(command, PaymentCode.PAYMENT_APPLICATION_INVALID);
        validate(command, false);
        Long tenantId = PaymentContextSupport.currentTenantId();
        PaymentApplication entity = new PaymentApplication();
        entity.setAppId(generateUniqueAppId(tenantId));
        copy(command, entity);
        entity.setTenantId(tenantId);
        String generatedSecret = refreshSecretState(command, entity);
        applicationMapper.insert(entity);
        auditService.record(
                PaymentOperationAuditService.ACTION_CREATE_APPLICATION,
                PaymentOperationAuditService.RESOURCE_PAYMENT_APPLICATION,
                entity.getAppId(),
                PaymentOperationAuditService.RESULT_SUCCESS);
        return R.ok(saveResult(entity, generatedSecret));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public R<PaymentApplicationSaveResultVO> updateApplication(UpdatePaymentApplicationCommand command) {
        Require.notNull(command, PaymentCode.PAYMENT_APPLICATION_INVALID);
        Require.notNull(command.getId(), PaymentCode.PAYMENT_APPLICATION_INVALID.getCode(), "应用 ID 不能为空");
        validate(command, true);
        PaymentApplication entity = selectRequired(command.getId());
        copy(command, entity);
        String generatedSecret = refreshSecretState(command, entity);
        applicationMapper.updateById(entity);
        auditService.record(
                PaymentOperationAuditService.ACTION_UPDATE_APPLICATION,
                PaymentOperationAuditService.RESOURCE_PAYMENT_APPLICATION,
                entity.getAppId(),
                PaymentOperationAuditService.RESULT_SUCCESS);
        return R.ok(saveResult(entity, generatedSecret));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public R<Boolean> deleteApplication(Long id) {
        PaymentApplication entity = selectRequired(id);
        long relationCount = countDeleteRelations(entity);
        if (relationCount > 0) {
            auditService.record(
                    PaymentOperationAuditService.ACTION_DELETE_APPLICATION,
                    PaymentOperationAuditService.RESOURCE_PAYMENT_APPLICATION,
                    entity.getAppId(),
                    PaymentOperationAuditService.RESULT_REJECTED);
        }
        Require.isTrue(relationCount == 0, PaymentCode.PAYMENT_APPLICATION_DELETE_HAS_RELATIONS);
        boolean deleted = applicationMapper.deleteById(id) > 0;
        Require.isTrue(deleted, PaymentCode.PAYMENT_APPLICATION_DELETE_FAILED);
        auditService.record(
                PaymentOperationAuditService.ACTION_DELETE_APPLICATION,
                PaymentOperationAuditService.RESOURCE_PAYMENT_APPLICATION,
                entity.getAppId(),
                PaymentOperationAuditService.RESULT_SUCCESS);
        return R.ok(true);
    }

    private LambdaQueryWrapper<PaymentApplication> wrapper(PaymentConfigPageQuery query) {
        String keyword = PaymentContextSupport.trimToNull(query.getKeyword());
        return new LambdaQueryWrapper<PaymentApplication>()
                .and(StringUtils.hasText(keyword), nested -> nested
                        .like(PaymentApplication::getAppId, keyword)
                        .or()
                        .like(PaymentApplication::getAppName, keyword))
                .eq(query.getStatus() != null, PaymentApplication::getStatus, query.getStatus())
                .eq(PaymentApplication::getTenantId, PaymentContextSupport.currentTenantId())
                .orderByDesc(PaymentApplication::getUpdatedAt);
    }

    private PaymentApplication selectRequired(Long id) {
        Require.notNull(id, PaymentCode.PAYMENT_APPLICATION_INVALID.getCode(), "应用 ID 不能为空");
        PaymentApplication entity = applicationMapper.selectById(id);
        Require.notNull(entity, PaymentCode.PAYMENT_APPLICATION_NOT_FOUND);
        Require.isTrue(PaymentContextSupport.currentTenantId().equals(entity.getTenantId()), PaymentCode.PAYMENT_APPLICATION_NOT_FOUND);
        return entity;
    }

    private long countDeleteRelations(PaymentApplication entity) {
        String appId = entity.getAppId();
        String legacyAppCode = legacyAppCode(appId);
        return applicationMapper.countDeleteRelations(entity.getTenantId(), entity.getId(), appId, legacyAppCode);
    }

    private String legacyAppCode(String appId) {
        if (!StringUtils.hasText(appId)) {
            return "";
        }
        String normalized = appId.startsWith("app_") ? appId.substring(4) : appId;
        return normalized.replace('-', '_').toUpperCase();
    }

    private void validate(SavePaymentApplicationCommand command, boolean update) {
        if (update) {
            Require.notNull(command.getId(), PaymentCode.PAYMENT_APPLICATION_INVALID.getCode(), "应用 ID 不能为空");
        }
        Require.notBlank(command.getAppName(), PaymentCode.PAYMENT_APPLICATION_INVALID.getCode(), "应用名称不能为空");
        Require.notNull(command.getIpWhitelistEnabled(), PaymentCode.PAYMENT_APPLICATION_INVALID.getCode(), "IP 白名单开关不能为空");
        if (Integer.valueOf(1).equals(command.getIpWhitelistEnabled())) {
            Require.notBlank(command.getIpWhitelist(), PaymentCode.PAYMENT_APPLICATION_INVALID.getCode(), "IP 白名单开启时必须配置允许来源");
        }
        Require.notNull(command.getPayloadEncryptEnabled(), PaymentCode.PAYMENT_APPLICATION_INVALID.getCode(), "报文加密开关不能为空");
        if (Integer.valueOf(1).equals(command.getPayloadEncryptEnabled())) {
            Require.notBlank(command.getSignAlgorithm(), PaymentCode.PAYMENT_APPLICATION_INVALID.getCode(), "报文加密开启时必须配置签名算法");
        }
        Require.notNull(command.getDemoApp(), PaymentCode.PAYMENT_APPLICATION_INVALID.getCode(), "示例应用标记不能为空");
        Require.notNull(command.getStatus(), PaymentCode.PAYMENT_APPLICATION_INVALID.getCode(), "状态不能为空");
    }

    private void copy(SavePaymentApplicationCommand command, PaymentApplication entity) {
        entity.setAppName(command.getAppName().trim());
        entity.setIpWhitelistEnabled(command.getIpWhitelistEnabled());
        entity.setIpWhitelist(Integer.valueOf(1).equals(command.getIpWhitelistEnabled()) ? PaymentContextSupport.trimToNull(command.getIpWhitelist()) : null);
        entity.setPayloadEncryptEnabled(command.getPayloadEncryptEnabled());
        entity.setSignAlgorithm(Integer.valueOf(1).equals(command.getPayloadEncryptEnabled()) ? defaultIfBlank(command.getSignAlgorithm(), "HMAC_SHA256") : null);
        entity.setNotifyRetryPolicy(PaymentContextSupport.trimToNull(command.getNotifyRetryPolicy()));
        entity.setDemoApp(command.getDemoApp());
        entity.setStatus(command.getStatus());
    }

    private String refreshSecretState(SavePaymentApplicationCommand command, PaymentApplication entity) {
        if (!Integer.valueOf(1).equals(command.getPayloadEncryptEnabled())) {
            entity.setAppSecret(null);
            entity.setSecretConfigured(0);
            entity.setSecretVersion(0);
            entity.setSecretLastResetTime(null);
            return null;
        }
        if (!StringUtils.hasText(entity.getAppSecret())) {
            String generatedSecret = generateSecret();
            entity.setAppSecret(sensitiveValueService.encrypt(generatedSecret));
            entity.setSecretConfigured(1);
            entity.setSecretVersion(entity.getSecretVersion() == null || entity.getSecretVersion() < 1 ? 1 : entity.getSecretVersion());
            entity.setSecretLastResetTime(LocalDateTime.now());
            return generatedSecret;
        }
        entity.setSecretConfigured(1);
        entity.setSecretVersion(entity.getSecretVersion() == null || entity.getSecretVersion() < 1 ? 1 : entity.getSecretVersion());
        return null;
    }

    private PaymentApplicationVO toVO(PaymentApplication entity) {
        PaymentApplicationVO vo = new PaymentApplicationVO();
        vo.setId(entity.getId());
        vo.setAppId(entity.getAppId());
        vo.setAppName(entity.getAppName());
        vo.setSecretConfigured(entity.getSecretConfigured());
        vo.setSecretVersion(entity.getSecretVersion());
        vo.setSecretLastResetTime(entity.getSecretLastResetTime());
        vo.setSignAlgorithm(entity.getSignAlgorithm());
        vo.setIpWhitelistEnabled(entity.getIpWhitelistEnabled());
        vo.setIpWhitelist(entity.getIpWhitelist());
        vo.setPayloadEncryptEnabled(entity.getPayloadEncryptEnabled());
        vo.setNotifyRetryPolicy(entity.getNotifyRetryPolicy());
        vo.setDemoApp(entity.getDemoApp());
        vo.setStatus(entity.getStatus());
        vo.setCreateTime(entity.getCreatedAt());
        vo.setUpdateTime(entity.getUpdatedAt());
        return vo;
    }

    private PaymentApplicationSaveResultVO saveResult(PaymentApplication entity, String generatedSecret) {
        PaymentApplicationSaveResultVO result = new PaymentApplicationSaveResultVO();
        result.setId(entity.getId());
        result.setAppId(entity.getAppId());
        result.setAppSecret(generatedSecret);
        result.setSecretGenerated(StringUtils.hasText(generatedSecret) ? 1 : 0);
        return result;
    }

    private String generateUniqueAppId(Long tenantId) {
        for (int i = 0; i < 8; i++) {
            String appId = "app_" + randomUrlSafe(DEFAULT_APP_ID_BYTES);
            Long count = applicationMapper.selectCount(new LambdaQueryWrapper<PaymentApplication>()
                    .eq(PaymentApplication::getTenantId, tenantId)
                    .eq(PaymentApplication::getAppId, appId));
            if (count == 0) {
                return appId;
            }
        }
        Require.isTrue(false, PaymentCode.PAYMENT_APPLICATION_APP_ID_GENERATE_FAILED);
        return "";
    }

    private String generateSecret() {
        return randomUrlSafe(DEFAULT_SECRET_BYTES);
    }

    private String randomUrlSafe(int bytesLength) {
        byte[] bytes = new byte[bytesLength];
        SECURE_RANDOM.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    private String defaultIfBlank(String value, String defaultValue) {
        String trimmed = PaymentContextSupport.trimToNull(value);
        return trimmed == null ? defaultValue : trimmed;
    }
}
