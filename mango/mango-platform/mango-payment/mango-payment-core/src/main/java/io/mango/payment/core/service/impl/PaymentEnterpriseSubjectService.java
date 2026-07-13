package io.mango.payment.core.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import io.mango.common.result.Require;
import io.mango.common.vo.PageResult;
import io.mango.payment.api.enums.PaymentCode;
import io.mango.payment.api.command.SavePaymentEnterpriseSubjectCommand;
import io.mango.payment.api.query.PaymentConfigPageQuery;
import io.mango.payment.api.vo.PaymentEnterpriseSubjectVO;
import io.mango.payment.core.entity.PaymentEnterpriseSubjectEntity;
import io.mango.payment.core.entity.PaymentSubjectBankAccountEntity;
import io.mango.payment.core.mapper.PaymentEnterpriseSubjectMapper;
import io.mango.payment.core.mapper.PaymentSubjectBankAccountMapper;
import io.mango.payment.core.service.IPaymentEnterpriseSubjectService;
import io.mango.payment.core.service.PaymentContextSupport;
import io.mango.payment.core.service.PaymentOperationAuditService;
import io.mango.payment.core.service.PaymentSensitiveValueCodec;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class PaymentEnterpriseSubjectService implements IPaymentEnterpriseSubjectService {

    private final PaymentEnterpriseSubjectMapper subjectMapper;
    private final PaymentSubjectBankAccountMapper bankAccountMapper;
    private final PaymentOperationAuditService auditService;
    private final PaymentSensitiveValueCodec sensitiveValueService;

    @Override
    public PageResult<PaymentEnterpriseSubjectVO> pageEnterpriseSubjects(PaymentConfigPageQuery query) {
        PaymentConfigPageQuery resolved = query == null ? new PaymentConfigPageQuery() : query;
        IPage<PaymentEnterpriseSubjectEntity> page = subjectMapper.selectPage(new Page<>(resolved.getPage(), resolved.getSize()), wrapper(resolved));
        List<PaymentEnterpriseSubjectVO> records = page.getRecords().stream().map(this::toVO).collect(Collectors.toList());
        return PageResult.of(records, page.getTotal(), page.getCurrent(), page.getSize());
    }

    @Override
    public PaymentEnterpriseSubjectVO detailEnterpriseSubject(Long id) {
        return toVO(selectRequired(id));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long createEnterpriseSubject(SavePaymentEnterpriseSubjectCommand command) {
        Require.notNull(command, PaymentCode.PAYMENT_ENTERPRISE_SUBJECT_INVALID);
        validate(command, false);
        PaymentEnterpriseSubjectEntity entity = new PaymentEnterpriseSubjectEntity();
        copy(command, entity);
        entity.setTenantId(PaymentContextSupport.currentTenantId());
        subjectMapper.insert(entity);
        syncDefaultBankAccount(entity);
        auditService.record(new PaymentOperationAuditService.AuditEntry(
                PaymentOperationAuditService.ACTION_CREATE_ENTERPRISE_SUBJECT,
                PaymentOperationAuditService.RESOURCE_PAYMENT_ENTERPRISE_SUBJECT,
                String.valueOf(entity.getId()),
                PaymentOperationAuditService.RESULT_SUCCESS));
        return entity.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Boolean updateEnterpriseSubject(SavePaymentEnterpriseSubjectCommand command) {
        Require.notNull(command, PaymentCode.PAYMENT_ENTERPRISE_SUBJECT_INVALID);
        validate(command, true);
        PaymentEnterpriseSubjectEntity entity = selectRequired(command.getId());
        copy(command, entity);
        boolean updated = subjectMapper.updateById(entity) > 0;
        syncDefaultBankAccount(entity);
        auditService.record(new PaymentOperationAuditService.AuditEntry(
                PaymentOperationAuditService.ACTION_UPDATE_ENTERPRISE_SUBJECT,
                PaymentOperationAuditService.RESOURCE_PAYMENT_ENTERPRISE_SUBJECT,
                String.valueOf(entity.getId()),
                PaymentOperationAuditService.RESULT_SUCCESS));
        return updated;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Boolean deleteEnterpriseSubject(Long id) {
        PaymentEnterpriseSubjectEntity entity = selectRequired(id);
        long relationCount = countDeleteRelations(entity);
        if (relationCount > 0) {
            auditService.record(new PaymentOperationAuditService.AuditEntry(
                    PaymentOperationAuditService.ACTION_DELETE_ENTERPRISE_SUBJECT,
                    PaymentOperationAuditService.RESOURCE_PAYMENT_ENTERPRISE_SUBJECT,
                    String.valueOf(entity.getId()),
                    PaymentOperationAuditService.RESULT_REJECTED));
        }
        Require.isTrue(relationCount == 0, PaymentCode.PAYMENT_ENTERPRISE_SUBJECT_DELETE_HAS_RELATIONS);
        boolean deleted = subjectMapper.deleteById(id) > 0;
        Require.isTrue(deleted, PaymentCode.PAYMENT_ENTERPRISE_SUBJECT_DELETE_FAILED);
        auditService.record(new PaymentOperationAuditService.AuditEntry(
                PaymentOperationAuditService.ACTION_DELETE_ENTERPRISE_SUBJECT,
                PaymentOperationAuditService.RESOURCE_PAYMENT_ENTERPRISE_SUBJECT,
                String.valueOf(entity.getId()),
                PaymentOperationAuditService.RESULT_SUCCESS));
        return true;
    }

    private LambdaQueryWrapper<PaymentEnterpriseSubjectEntity> wrapper(PaymentConfigPageQuery query) {
        String keyword = PaymentContextSupport.trimToNull(query.getKeyword());
        return new LambdaQueryWrapper<PaymentEnterpriseSubjectEntity>()
                .and(StringUtils.hasText(keyword), nested -> nested
                        .like(PaymentEnterpriseSubjectEntity::getSubjectName, keyword)
                        .or()
                        .like(PaymentEnterpriseSubjectEntity::getCreditCode, keyword))
                .eq(query.getStatus() != null, PaymentEnterpriseSubjectEntity::getStatus, query.getStatus())
                .eq(PaymentEnterpriseSubjectEntity::getTenantId, PaymentContextSupport.currentTenantId())
                .orderByDesc(PaymentEnterpriseSubjectEntity::getUpdatedAt);
    }

    private PaymentEnterpriseSubjectEntity selectRequired(Long id) {
        Require.notNull(id, PaymentCode.PAYMENT_ENTERPRISE_SUBJECT_INVALID, "主体 ID 不能为空");
        PaymentEnterpriseSubjectEntity entity = subjectMapper.selectById(id);
        Require.notNull(entity, PaymentCode.PAYMENT_ENTERPRISE_SUBJECT_NOT_FOUND);
        Require.isTrue(PaymentContextSupport.currentTenantId().equals(entity.getTenantId()), PaymentCode.PAYMENT_ENTERPRISE_SUBJECT_NOT_FOUND);
        return entity;
    }

    private void validate(SavePaymentEnterpriseSubjectCommand command, boolean update) {
        if (update) {
            Require.notNull(command.getId(), PaymentCode.PAYMENT_ENTERPRISE_SUBJECT_INVALID, "主体 ID 不能为空");
        }
        Require.notBlank(command.getSubjectName(), PaymentCode.PAYMENT_ENTERPRISE_SUBJECT_INVALID, "主体名称不能为空");
        Require.notBlank(command.getCreditCode(), PaymentCode.PAYMENT_ENTERPRISE_SUBJECT_INVALID, "统一社会信用代码不能为空");
        Require.notBlank(command.getBankAccountNo(), PaymentCode.PAYMENT_ENTERPRISE_SUBJECT_INVALID, "银行账户不能为空");
        Require.notBlank(command.getBankName(), PaymentCode.PAYMENT_ENTERPRISE_SUBJECT_INVALID, "开户行不能为空");
        Require.notNull(command.getStatus(), PaymentCode.PAYMENT_ENTERPRISE_SUBJECT_INVALID, "状态不能为空");
    }

    private long countDeleteRelations(PaymentEnterpriseSubjectEntity entity) {
        return subjectMapper.countDeleteRelations(entity.getTenantId(), entity.getId());
    }

    private void copy(SavePaymentEnterpriseSubjectCommand command, PaymentEnterpriseSubjectEntity entity) {
        entity.setSubjectName(command.getSubjectName().trim());
        entity.setCreditCodeHash(sensitiveValueService.stableHash(command.getCreditCode()));
        entity.setCreditCode(sensitiveValueService.encrypt(command.getCreditCode()));
        entity.setBankAccountNo(sensitiveValueService.encrypt(command.getBankAccountNo()));
        entity.setBankName(command.getBankName().trim());
        entity.setLicenseFileId(command.getLicenseFileId());
        entity.setStatus(command.getStatus());
    }

    private void syncDefaultBankAccount(PaymentEnterpriseSubjectEntity subject) {
        PaymentSubjectBankAccountEntity account = bankAccountMapper.selectOne(new LambdaQueryWrapper<PaymentSubjectBankAccountEntity>()
                .eq(PaymentSubjectBankAccountEntity::getTenantId, subject.getTenantId())
                .eq(PaymentSubjectBankAccountEntity::getSubjectId, subject.getId())
                .eq(PaymentSubjectBankAccountEntity::getDefaultAccount, 1)
                .eq(PaymentSubjectBankAccountEntity::getDelFlag, 0)
                .last("LIMIT 1"));
        if (account == null) {
            account = new PaymentSubjectBankAccountEntity();
            account.setSubjectId(subject.getId());
            account.setTenantId(subject.getTenantId());
            account.setAccountType("CORPORATE");
            account.setDefaultAccount(1);
            account.setDelFlag(0);
        }
        account.setAccountName(subject.getSubjectName());
        account.setAccountNo(subject.getBankAccountNo());
        account.setBankName(subject.getBankName());
        account.setStatus(subject.getStatus());
        if (account.getId() == null) {
            bankAccountMapper.insert(account);
        } else {
            bankAccountMapper.updateById(account);
        }
    }

    private PaymentEnterpriseSubjectVO toVO(PaymentEnterpriseSubjectEntity entity) {
        PaymentEnterpriseSubjectVO vo = new PaymentEnterpriseSubjectVO();
        vo.setId(entity.getId());
        vo.setSubjectName(entity.getSubjectName());
        vo.setCreditCodeMask(sensitiveValueService.mask(entity.getCreditCode(), 4, 4));
        vo.setBankAccountNoMask(sensitiveValueService.mask(entity.getBankAccountNo(), 4, 4));
        vo.setBankName(entity.getBankName());
        vo.setLicenseFileId(entity.getLicenseFileId());
        vo.setStatus(entity.getStatus());
        vo.setCreateTime(entity.getCreatedAt());
        vo.setUpdateTime(entity.getUpdatedAt());
        return vo;
    }

}
