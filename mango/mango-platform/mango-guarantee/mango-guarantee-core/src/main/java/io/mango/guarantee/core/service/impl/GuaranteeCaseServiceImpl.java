package io.mango.guarantee.core.service.impl;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import io.mango.common.result.Require;
import io.mango.guarantee.api.GuaranteeCode;
import io.mango.guarantee.api.command.GuaranteeCaseCommand;
import io.mango.guarantee.api.query.GuaranteeCaseQuery;
import io.mango.guarantee.api.vo.GuaranteeCaseVO;
import io.mango.guarantee.core.entity.GuaranteeCase;
import io.mango.guarantee.core.mapper.GuaranteeCaseMapper;
import io.mango.guarantee.core.service.IGuaranteeCaseService;
import io.mango.infra.context.core.MangoContextHolder;
import io.mango.infra.persistence.api.query.PersistencePageResult;
import io.mango.workflow.api.WorkflowBusinessProcessApi;
import io.mango.workflow.api.vo.WorkflowBusinessProcessVO;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 保函业务单服务实现。
 */
@Service
@RequiredArgsConstructor
public class GuaranteeCaseServiceImpl implements IGuaranteeCaseService {

    private static final int DEFAULT_STATUS = 0;
    private static final String DEFAULT_CURRENCY = "CNY";

    private final GuaranteeCaseMapper caseMapper;
    private final ObjectProvider<WorkflowBusinessProcessApi> workflowBusinessProcessApiProvider;

    @Override
    public PersistencePageResult<GuaranteeCaseVO> page(GuaranteeCaseQuery query) {
        GuaranteeCaseQuery actualQuery = query == null ? new GuaranteeCaseQuery() : query;
        Long tenantId = currentTenantId();
        Page<GuaranteeCaseVO> page = new Page<>(positive(actualQuery.getPage(), 1), positive(actualQuery.getSize(), 10));
        page.setRecords(caseMapper.selectVisiblePage(page, tenantId, actualQuery));
        fillWorkflowProgress(page.getRecords());
        return PersistencePageResult.of(page.getRecords(), page.getTotal(), page.getCurrent(), page.getSize());
    }

    @Override
    public GuaranteeCaseVO get(Long caseId) {
        Require.notNull(caseId, "业务单ID不能为空");
        GuaranteeCaseVO detail = caseMapper.selectVisibleById(caseId, currentTenantId());
        Require.notNull(detail, GuaranteeCode.CASE_NOT_FOUND);
        fillWorkflowProgress(List.of(detail));
        return detail;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long create(GuaranteeCaseCommand command) {
        Long tenantId = currentTenantId();
        GuaranteeCase entity = new GuaranteeCase();
        entity.setCaseNo(generateCaseNo(tenantId));
        entity.setSourceTenantId(tenantId);
        entity.setTenantId(tenantId);
        applyCommand(entity, command, true);
        caseMapper.insert(entity);
        return entity.getCaseId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Boolean update(GuaranteeCaseCommand command) {
        Require.notNull(command, "保函业务单不能为空");
        Require.notNull(command.getCaseId(), "业务单ID不能为空");
        Long tenantId = currentTenantId();
        GuaranteeCase existing = caseMapper.selectById(command.getCaseId());
        Require.notNull(existing, GuaranteeCode.CASE_NOT_FOUND);
        Require.isTrue(tenantId.equals(existing.getSourceTenantId()), GuaranteeCode.CASE_NOT_FOUND);
        existing.setCaseId(command.getCaseId());
        applyCommand(existing, command, false);
        return caseMapper.updateById(existing) > 0;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Boolean delete(Long caseId) {
        Require.notNull(caseId, "业务单ID不能为空");
        return caseMapper.softDeleteSourceCase(caseId, currentTenantId()) > 0;
    }

    private void applyCommand(GuaranteeCase entity, GuaranteeCaseCommand command, boolean create) {
        Require.notNull(command, "保函业务单不能为空");
        entity.setTitle(command.getTitle());
        entity.setApplicantName(command.getApplicantName());
        entity.setBeneficiaryName(command.getBeneficiaryName());
        entity.setGuaranteeType(normalizeCode(command.getGuaranteeType()));
        entity.setAmount(command.getAmount());
        entity.setCurrency(firstText(normalizeCode(command.getCurrency()), DEFAULT_CURRENCY));
        entity.setExpectedIssueDate(command.getExpectedIssueDate());
        entity.setStatus(command.getStatus() == null ? DEFAULT_STATUS : command.getStatus());
        entity.setRemark(command.getRemark());
        if (create) {
            entity.setCreateTime(LocalDateTime.now());
            entity.setUpdateTime(LocalDateTime.now());
            entity.setDelFlag(0);
        } else {
            entity.setUpdateTime(LocalDateTime.now());
        }
    }

    private Long currentTenantId() {
        String tenantId = MangoContextHolder.tenantId();
        Require.notBlank(tenantId, GuaranteeCode.TENANT_CONTEXT_REQUIRED);
        try {
            return Long.valueOf(tenantId);
        } catch (NumberFormatException e) {
            return Require.fail(GuaranteeCode.TENANT_CONTEXT_REQUIRED.getCode(), "当前机构上下文不是有效数字: " + tenantId);
        }
    }

    private void fillWorkflowProgress(List<GuaranteeCaseVO> records) {
        if (records == null || records.isEmpty()) {
            return;
        }
        WorkflowBusinessProcessApi workflowApi = workflowBusinessProcessApiProvider.getIfAvailable();
        if (workflowApi == null) {
            return;
        }
        List<String> businessKeys = records.stream()
                .map(GuaranteeCaseVO::getCaseNo)
                .filter(StringUtils::hasText)
                .distinct()
                .toList();
        if (businessKeys.isEmpty()) {
            return;
        }
        Map<String, WorkflowBusinessProcessVO> processByBusinessKey = workflowApi.latestByBusinessKeys(businessKeys)
                .stream()
                .collect(Collectors.toMap(WorkflowBusinessProcessVO::getBusinessKey, Function.identity(), (left, right) -> left));
        for (GuaranteeCaseVO record : records) {
            WorkflowBusinessProcessVO process = processByBusinessKey.get(record.getCaseNo());
            if (process == null) {
                continue;
            }
            record.setProcessInstanceId(process.getProcessInstanceId());
            record.setProcessName(process.getProcessName());
            record.setProcessStatus(process.getStatus());
            record.setCurrentTaskName(process.getCurrentTaskName());
            record.setCurrentTaskDefinitionKey(process.getCurrentTaskDefinitionKey());
        }
    }

    private String generateCaseNo(Long tenantId) {
        return "GH" + LocalDate.now().format(DateTimeFormatter.BASIC_ISO_DATE) + tenantId + System.nanoTime();
    }

    private long positive(Integer value, long defaultValue) {
        return value == null || value < 1 ? defaultValue : value;
    }

    private String normalizeCode(String value) {
        return StringUtils.hasText(value) ? value.trim().toUpperCase(Locale.ROOT) : null;
    }

    private String firstText(String first, String second) {
        return StringUtils.hasText(first) ? first : second;
    }
}
