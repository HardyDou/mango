package io.mango.information.core.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.mango.common.result.Require;
import io.mango.common.exception.BizException;
import io.mango.common.vo.PageResult;
import io.mango.information.api.command.ProcessInformationSubmissionCommand;
import io.mango.information.api.command.SubmitInformationCommand;
import io.mango.information.api.enums.InformationCode;
import io.mango.information.api.enums.InformationFormStatus;
import io.mango.information.api.enums.InformationSubmissionStatus;
import io.mango.information.api.query.InformationSubmissionPageQuery;
import io.mango.information.api.vo.InformationSubmissionResultVO;
import io.mango.information.api.vo.InformationSubmissionVO;
import io.mango.information.core.entity.InformationFormEntity;
import io.mango.information.core.entity.InformationSubmissionEntity;
import io.mango.information.core.entity.InformationSubmissionSnapshotEntity;
import io.mango.information.core.mapper.InformationSubmissionMapper;
import io.mango.information.core.mapper.InformationSubmissionSnapshotMapper;
import io.mango.information.core.service.IInformationSubmissionService;
import io.mango.information.core.support.InformationDefinition;
import io.mango.infra.context.api.MangoContextHolder;
import io.mango.infra.context.api.MangoContextSnapshot;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.security.MessageDigest;
import java.time.LocalDateTime;
import java.util.List;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import com.fasterxml.jackson.core.JsonProcessingException;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.nio.charset.StandardCharsets;

/** 信息采集提交服务。 */
@Service
@RequiredArgsConstructor(onConstructor_ = @SuppressFBWarnings(value = "EI_EXPOSE_REP2",
        justification = "Spring 容器管理的单例依赖，构造后不对外暴露"))
public class InformationSubmissionService implements IInformationSubmissionService {
    private final InformationSubmissionMapper submissionMapper;
    private final InformationSubmissionSnapshotMapper snapshotMapper;
    private final InformationFormService formService;
    private final ObjectMapper objectMapper;

    @Override
    public PageResult<InformationSubmissionVO> page(InformationSubmissionPageQuery query) {
        InformationSubmissionPageQuery resolved = query == null ? new InformationSubmissionPageQuery() : query;
        LambdaQueryWrapper<InformationSubmissionEntity> wrapper = queryWrapper(resolved);
        wrapper.orderByDesc(InformationSubmissionEntity::getSubmittedAt);
        IPage<InformationSubmissionEntity> page = submissionMapper.selectPage(new Page<>(resolved.getPage(), resolved.getSize()), wrapper);
        return PageResult.of(page.getRecords().stream().map(this::toVO).toList(), page.getTotal(), page.getCurrent(), page.getSize());
    }

    @Override
    public InformationSubmissionVO detail(Long id) {
        return toVO(select(id));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean process(ProcessInformationSubmissionCommand command) {
        InformationSubmissionEntity entity = select(command.getId());
        Require.isTrue(!InformationSubmissionStatus.CLOSED.name().equals(entity.getStatus())
                || InformationSubmissionStatus.CLOSED == command.getStatus(), InformationCode.SUBMISSION_CLOSED);
        entity.setStatus(command.getStatus().name());
        entity.setProcessRemark(command.getProcessRemark());
        entity.setUpdatedAt(LocalDateTime.now());
        entity.setUpdatedBy(MangoContextHolder.userId());
        return submissionMapper.updateById(entity) > 0;
    }

    @Override
    public String exportCsv(InformationSubmissionPageQuery query) {
        LambdaQueryWrapper<InformationSubmissionEntity> wrapper = queryWrapper(query);
        wrapper.orderByDesc(InformationSubmissionEntity::getSubmittedAt);
        List<InformationSubmissionEntity> records = submissionMapper.selectList(wrapper);
        Map<String, InformationDefinition.Field> columns = new LinkedHashMap<>();
        try {
            // 列集合取当前筛选结果涉及表单的字段并集，展示型规则不导出
            for (InformationSubmissionEntity record : records) {
                InformationFormEntity form = formService.select(record.getFormId());
                for (InformationDefinition.Field field : InformationDefinition.fields(objectMapper.readTree(form.getDefinitionJson()))) {
                    if (!InformationDefinition.displayOnly(field)) {
                        columns.putIfAbsent(field.code(), field);
                    }
                }
            }
            StringBuilder csv = new StringBuilder("记录ID,提交时间,状态,来源页面");
            for (InformationDefinition.Field field : columns.values()) {
                csv.append(',').append(csvCell(field.name()));
            }
            csv.append('\n');
            for (InformationSubmissionEntity record : records) {
                Map<String, Object> values = objectMapper.readValue(record.getValuesJson(), Map.class);
                csv.append(csvCell(String.valueOf(record.getId()))).append(',').append(csvCell(String.valueOf(record.getSubmittedAt()))).append(',')
                        .append(csvCell(statusLabel(record.getStatus()))).append(',').append(csvCell(record.getSourcePage()));
                for (InformationDefinition.Field field : columns.values()) {
                    csv.append(',').append(csvCell(readable(field.type(), values.get(field.code()))));
                }
                csv.append('\n');
            }
            return csv.toString();
        } catch (JsonProcessingException e) {
            throw new IllegalArgumentException("导出采集记录失败", e);
        }
    }

    private String statusLabel(String status) {
        return switch (status) {
            case "PENDING" -> "待处理";
            case "PROCESSING" -> "处理中";
            case "COMPLETED" -> "已完成";
            case "CLOSED" -> "已关闭";
            default -> status;
        };
    }

    /**
     * 复杂值（多选、子表单、表格表单等）序列化为可读 JSON，避免导出出现 [object Object]。
     *
     * <p>导出按原值输出：采集记录仅对有查看权限的角色开放，手机号等字段不再脱敏，
     * 便于处理人直接回访。</p>
     */
    private String readable(String fieldType, Object value) {
        if (value == null) {
            return "";
        }
        if (value instanceof java.util.Collection<?> || value instanceof java.util.Map<?, ?>) {
            return writeQuietly(value);
        }
        return String.valueOf(value);
    }

    private String writeQuietly(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (Exception e) {
            return String.valueOf(value);
        }
    }

    private String csvCell(Object value) {
        String text = value == null ? "" : String.valueOf(value);
        if (text.startsWith("=") || text.startsWith("+") || text.startsWith("-") || text.startsWith("@")) {
            text = "'" + text;
        }
        return '"' + text.replace("\"", "\"\"") + '"';
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public InformationSubmissionResultVO submit(SubmitInformationCommand command, boolean serverRequest) {
        InformationFormEntity form = formService.selectPublished(command.getFormCode());
        if (serverRequest) {
            Require.isTrue(StringUtils.hasText(command.getServerSecret()), InformationCode.SERVER_SECRET_INVALID);
            String actual = InformationFormService.digest(command.getServerSecret());
            Require.isTrue(MessageDigest.isEqual(actual.getBytes(StandardCharsets.UTF_8), form.getServerSecretDigest().getBytes(StandardCharsets.UTF_8)), InformationCode.SERVER_SECRET_INVALID);
        }
        validateValues(form.getDefinitionJson(), command.getValues());
        return withTenantContext(form.getTenantId(), () -> doSubmit(form, command));
    }

    private InformationSubmissionResultVO doSubmit(InformationFormEntity form, SubmitInformationCommand command) {
        if (StringUtils.hasText(command.getIdempotencyKey())) {
            InformationSubmissionEntity old = submissionMapper.selectOne(tenantWrapper()
                    .eq(InformationSubmissionEntity::getFormId, form.getId())
                    .eq(InformationSubmissionEntity::getIdempotencyKey, command.getIdempotencyKey())
                    .last("LIMIT 1"));
            if (old != null) {
                return result(old.getId(), form.getSuccessMessage());
            }
        }
        LocalDateTime now = LocalDateTime.now();
        InformationSubmissionEntity entity = new InformationSubmissionEntity();
        entity.setFormId(form.getId());
        entity.setFormCode(form.getFormCode());
        entity.setValuesJson(write(command.getValues()));
        entity.setSourcePage(command.getSourcePage());
        entity.setIdempotencyKey(command.getIdempotencyKey());
        entity.setStatus(InformationSubmissionStatus.PENDING.name());
        entity.setSubmittedAt(now);
        submissionMapper.insert(entity);
        InformationSubmissionSnapshotEntity snapshot = new InformationSubmissionSnapshotEntity();
        snapshot.setSubmissionId(entity.getId());
        snapshot.setFormId(form.getId());
        snapshot.setDefinitionJson(form.getDefinitionJson());
        snapshotMapper.insert(snapshot);
        return result(entity.getId(), form.getSuccessMessage());
    }

    private InformationSubmissionResultVO withTenantContext(String tenantId, java.util.function.Supplier<InformationSubmissionResultVO> supplier) {
        MangoContextSnapshot previous = MangoContextHolder.get();
        if (StringUtils.hasText(tenantId) && !tenantId.equals(previous.tenantId())) {
            MangoContextHolder.set(previous.withTenantId(tenantId));
        }
        try {
            return supplier.get();
        } finally {
            MangoContextHolder.set(previous);
        }
    }

    /** 列表、导出和待处理计数共用同一套筛选条件，避免口径不一致。 */
    private LambdaQueryWrapper<InformationSubmissionEntity> queryWrapper(InformationSubmissionPageQuery query) {
        InformationSubmissionPageQuery resolved = query == null ? new InformationSubmissionPageQuery() : query;
        LambdaQueryWrapper<InformationSubmissionEntity> wrapper = tenantWrapper();
        if (resolved.getFormId() != null) {
            wrapper.eq(InformationSubmissionEntity::getFormId, resolved.getFormId());
        }
        if (resolved.getStatus() != null) {
            wrapper.eq(InformationSubmissionEntity::getStatus, resolved.getStatus().name());
        }
        if (resolved.getStartTime() != null) {
            wrapper.ge(InformationSubmissionEntity::getSubmittedAt, resolved.getStartTime());
        }
        if (resolved.getEndTime() != null) {
            wrapper.le(InformationSubmissionEntity::getSubmittedAt, resolved.getEndTime());
        }
        if (StringUtils.hasText(resolved.getKeyword())) {
            wrapper.and(w -> w.like(InformationSubmissionEntity::getSourcePage, resolved.getKeyword())
                    .or().like(InformationSubmissionEntity::getValuesJson, resolved.getKeyword()));
        }
        return wrapper;
    }

    private InformationSubmissionEntity select(Long id) {
        InformationSubmissionEntity entity = submissionMapper.selectOne(tenantWrapper().eq(InformationSubmissionEntity::getId, id).last("LIMIT 1"));
        return Require.nonNull(entity, InformationCode.SUBMISSION_NOT_FOUND);
    }

    private LambdaQueryWrapper<InformationSubmissionEntity> tenantWrapper() {
        return new LambdaQueryWrapper<InformationSubmissionEntity>()
                .eq(StringUtils.hasText(MangoContextHolder.tenantId()), InformationSubmissionEntity::getTenantId, MangoContextHolder.tenantId());
    }

    private InformationSubmissionVO toVO(InformationSubmissionEntity entity) {
        InformationSubmissionVO vo = new InformationSubmissionVO();
        vo.setId(entity.getId());
        vo.setFormId(entity.getFormId());
        vo.setFormCode(entity.getFormCode());
        InformationFormEntity form = formService.select(entity.getFormId());
        vo.setFormName(form.getFormName());
        vo.setDefinitionJson(form.getDefinitionJson());
        vo.setValuesJson(entity.getValuesJson());
        vo.setSourcePage(entity.getSourcePage());
        vo.setStatus(InformationSubmissionStatus.valueOf(entity.getStatus()));
        vo.setProcessRemark(entity.getProcessRemark());
        vo.setSubmittedAt(entity.getSubmittedAt());
        vo.setUpdatedAt(entity.getUpdatedAt());
        return vo;
    }

    /** 按表单定义校验提交值：展示型规则不参与，未知字段与缺失必填都拒绝。 */
    private void validateValues(String definitionJson, Map<String, Object> values) {
        try {
            List<InformationDefinition.Field> fields = InformationDefinition.fields(objectMapper.readTree(definitionJson));
            Set<String> definedCodes = new HashSet<>();
            for (InformationDefinition.Field field : fields) {
                definedCodes.add(field.code());
                if (InformationDefinition.displayOnly(field)) {
                    continue;
                }
                if (field.required() && isEmptyValue(values.get(field.code()))) {
                    Require.isTrue(false, InformationCode.INVALID_SUBMISSION, "字段 " + field.code() + " 不能为空");
                }
            }
            values.keySet().forEach(code -> Require.isTrue(definedCodes.contains(code),
                    InformationCode.INVALID_SUBMISSION, "未知字段：" + code));
        } catch (BizException bizException) {
            throw bizException;
        } catch (Exception e) {
            throw new IllegalArgumentException(InformationCode.INVALID_SUBMISSION.getMessage(), e);
        }
    }

    private boolean isEmptyValue(Object value) {
        if (value == null) {
            return true;
        }
        if (value instanceof java.util.Collection<?> collection) {
            return collection.isEmpty();
        }
        return !StringUtils.hasText(String.valueOf(value));
    }

    private String write(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (JsonProcessingException e) {
            throw new IllegalArgumentException(InformationCode.INVALID_SUBMISSION.getMessage(), e);
        }
    }

    private InformationSubmissionResultVO result(Long id, String message) {
        InformationSubmissionResultVO result = new InformationSubmissionResultVO();
        result.setId(id);
        result.setMessage(message);
        return result;
    }
}
