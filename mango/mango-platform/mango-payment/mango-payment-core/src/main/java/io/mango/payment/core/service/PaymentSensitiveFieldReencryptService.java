package io.mango.payment.core.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import io.mango.common.result.Require;
import io.mango.payment.api.PaymentCode;
import io.mango.payment.api.vo.PaymentSensitiveFieldReencryptResultVO;
import io.mango.payment.core.entity.PaymentApplication;
import io.mango.payment.core.entity.PaymentEnterpriseSubject;
import io.mango.payment.core.entity.PaymentSubjectBankAccountEntity;
import io.mango.payment.core.mapper.PaymentApplicationMapper;
import io.mango.payment.core.mapper.PaymentEnterpriseSubjectMapper;
import io.mango.payment.core.mapper.PaymentSubjectBankAccountMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class PaymentSensitiveFieldReencryptService {

    private static final int DEFAULT_LIMIT = 100;
    private static final int MAX_LIMIT = 1000;

    private final PaymentApplicationMapper applicationMapper;
    private final PaymentEnterpriseSubjectMapper subjectMapper;
    private final PaymentSubjectBankAccountMapper bankAccountMapper;
    private final PaymentSensitiveValueService sensitiveValueService;
    private final PaymentOperationAuditService auditService;

    @Transactional(rollbackFor = Exception.class)
    public PaymentSensitiveFieldReencryptResultVO reencryptCurrentTenant(Integer limit) {
        int resolvedLimit = resolveLimit(limit);
        Long tenantId = PaymentContextSupport.currentTenantId();
        LocalDateTime now = LocalDateTime.now();
        Long operatorId = PaymentContextSupport.currentUserId();

        int applicationSecretCount = reencryptApplicationSecrets(tenantId, resolvedLimit, operatorId, now);
        EnterpriseSubjectReencryptCount enterpriseSubjectCount =
                reencryptEnterpriseSubjects(tenantId, resolvedLimit, operatorId, now);
        int subjectBankAccountCount = reencryptSubjectBankAccounts(tenantId, resolvedLimit, operatorId, now);

        PaymentSensitiveFieldReencryptResultVO result = new PaymentSensitiveFieldReencryptResultVO();
        result.setApplicationSecretCount(applicationSecretCount);
        result.setEnterpriseCreditCodeCount(enterpriseSubjectCount.creditCodeCount());
        result.setEnterpriseBankAccountCount(enterpriseSubjectCount.bankAccountCount());
        result.setSubjectBankAccountCount(subjectBankAccountCount);
        result.setTotalCount(applicationSecretCount + enterpriseSubjectCount.creditCodeCount()
                + enterpriseSubjectCount.bankAccountCount() + subjectBankAccountCount);
        result.setLimit(resolvedLimit);
        auditService.record(
                PaymentOperationAuditService.ACTION_REENCRYPT_SENSITIVE_FIELDS,
                PaymentOperationAuditService.RESOURCE_PAYMENT_SENSITIVE_FIELDS,
                "tenant:" + tenantId + ",count:" + result.getTotalCount(),
                PaymentOperationAuditService.RESULT_SUCCESS);
        return result;
    }

    private int resolveLimit(Integer limit) {
        int resolved = limit == null ? DEFAULT_LIMIT : limit;
        Require.isTrue(resolved > 0 && resolved <= MAX_LIMIT,
                PaymentCode.PAYMENT_SENSITIVE_VALUE_INVALID.getCode(), "重加密批量大小必须在 1-1000 之间");
        return resolved;
    }

    private int reencryptApplicationSecrets(Long tenantId, int limit, Long operatorId, LocalDateTime now) {
        List<PaymentApplication> rows = applicationMapper.selectList(
                new LambdaQueryWrapper<PaymentApplication>()
                        .eq(PaymentApplication::getTenantId, tenantId)
                        .isNotNull(PaymentApplication::getAppSecret)
                        .notLikeRight(PaymentApplication::getAppSecret, "enc:")
                        .last("LIMIT " + limit));
        int updated = 0;
        for (PaymentApplication row : rows) {
            String encrypted = encryptIfNeeded(row.getAppSecret());
            if (!StringUtils.hasText(encrypted) || encrypted.equals(row.getAppSecret())) {
                continue;
            }
            row.setAppSecret(encrypted);
            row.setUpdatedBy(operatorId);
            row.setUpdatedAt(now);
            updated += applicationMapper.updateById(row);
        }
        return updated;
    }

    private EnterpriseSubjectReencryptCount reencryptEnterpriseSubjects(
            Long tenantId,
            int limit,
            Long operatorId,
            LocalDateTime now) {
        List<PaymentEnterpriseSubject> rows = subjectMapper.selectList(
                new LambdaQueryWrapper<PaymentEnterpriseSubject>()
                        .eq(PaymentEnterpriseSubject::getTenantId, tenantId)
                        .and(wrapper -> wrapper
                                .isNotNull(PaymentEnterpriseSubject::getCreditCode)
                                .notLikeRight(PaymentEnterpriseSubject::getCreditCode, "enc:")
                                .or()
                                .isNotNull(PaymentEnterpriseSubject::getBankAccountNo)
                                .notLikeRight(PaymentEnterpriseSubject::getBankAccountNo, "enc:"))
                        .last("LIMIT " + limit));
        int creditCodeCount = 0;
        int bankAccountCount = 0;
        for (PaymentEnterpriseSubject row : rows) {
            boolean changed = false;
            if (shouldEncrypt(row.getCreditCode())) {
                String plaintext = row.getCreditCode();
                row.setCreditCode(sensitiveValueService.encrypt(plaintext));
                row.setCreditCodeHash(sensitiveValueService.stableHash(plaintext));
                creditCodeCount++;
                changed = true;
            }
            if (shouldEncrypt(row.getBankAccountNo())) {
                row.setBankAccountNo(sensitiveValueService.encrypt(row.getBankAccountNo()));
                bankAccountCount++;
                changed = true;
            }
            if (changed) {
                row.setUpdatedBy(operatorId);
                row.setUpdatedAt(now);
                subjectMapper.updateById(row);
            }
        }
        return new EnterpriseSubjectReencryptCount(creditCodeCount, bankAccountCount);
    }

    private int reencryptSubjectBankAccounts(Long tenantId, int limit, Long operatorId, LocalDateTime now) {
        List<PaymentSubjectBankAccountEntity> rows = bankAccountMapper.selectList(
                new LambdaQueryWrapper<PaymentSubjectBankAccountEntity>()
                        .eq(PaymentSubjectBankAccountEntity::getTenantId, tenantId)
                        .isNotNull(PaymentSubjectBankAccountEntity::getAccountNo)
                        .notLikeRight(PaymentSubjectBankAccountEntity::getAccountNo, "enc:")
                        .last("LIMIT " + limit));
        int updated = 0;
        for (PaymentSubjectBankAccountEntity row : rows) {
            String encrypted = encryptIfNeeded(row.getAccountNo());
            if (!StringUtils.hasText(encrypted) || encrypted.equals(row.getAccountNo())) {
                continue;
            }
            row.setAccountNo(encrypted);
            row.setUpdatedBy(operatorId);
            row.setUpdatedAt(now);
            updated += bankAccountMapper.updateById(row);
        }
        return updated;
    }

    private String encryptIfNeeded(String value) {
        if (!shouldEncrypt(value)) {
            return value;
        }
        return sensitiveValueService.encrypt(value);
    }

    private boolean shouldEncrypt(String value) {
        return StringUtils.hasText(value) && !sensitiveValueService.isEncrypted(value);
    }

    private record EnterpriseSubjectReencryptCount(int creditCodeCount, int bankAccountCount) {
    }
}
