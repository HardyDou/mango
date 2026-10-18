package io.mango.information.core.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.mango.common.exception.BizException;
import io.mango.common.result.Require;
import io.mango.common.vo.PageResult;
import io.mango.information.api.command.SaveInformationFormCommand;
import io.mango.information.api.command.UpdateInformationFormStatusCommand;
import io.mango.information.api.enums.InformationCode;
import io.mango.information.api.enums.InformationFormStatus;
import io.mango.information.api.query.InformationFormPageQuery;
import io.mango.information.api.vo.InformationFormDetailVO;
import io.mango.information.api.vo.InformationFormVO;
import io.mango.information.core.entity.InformationFormEntity;
import io.mango.information.core.entity.InformationSubmissionEntity;
import io.mango.information.core.mapper.InformationFormMapper;
import io.mango.information.core.mapper.InformationSubmissionMapper;
import io.mango.information.core.service.IInformationFormService;
import io.mango.information.core.service.InformationActorNameProvider;
import io.mango.information.core.support.InformationDefinition;
import io.mango.infra.context.api.MangoContextHolder;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.HexFormat;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/** 信息采集表单服务。 */
@Service
@RequiredArgsConstructor
public class InformationFormService implements IInformationFormService {
    private static final SecureRandom RANDOM = new SecureRandom();
    private final InformationFormMapper formMapper;
    private final InformationSubmissionMapper submissionMapper;
    private final ObjectMapper objectMapper;
    private final ObjectProvider<InformationActorNameProvider> actorNameProvider;

    @Override
    public PageResult<InformationFormVO> page(InformationFormPageQuery query) {
        InformationFormPageQuery resolved = query == null ? new InformationFormPageQuery() : query;
        LambdaQueryWrapper<InformationFormEntity> wrapper = tenantWrapper();
        if (StringUtils.hasText(resolved.getKeyword())) {
            wrapper.and(w -> w.like(InformationFormEntity::getFormCode, resolved.getKeyword())
                    .or().like(InformationFormEntity::getFormName, resolved.getKeyword()));
        }
        if (StringUtils.hasText(resolved.getStatus())) {
            wrapper.eq(InformationFormEntity::getStatus, resolved.getStatus());
        }
        wrapper.orderByDesc(InformationFormEntity::getUpdatedAt);
        IPage<InformationFormEntity> page = formMapper.selectPage(new Page<>(resolved.getPage(), resolved.getSize()), wrapper);
        Map<Long, String> names = resolveActorNames(page.getRecords().stream()
                .map(InformationFormEntity::getCreatedBy)
                .filter(java.util.Objects::nonNull)
                .collect(java.util.stream.Collectors.toCollection(LinkedHashSet::new)));
        List<InformationFormVO> list = page.getRecords().stream().map(entity -> toListVO(entity, names)).toList();
        return PageResult.of(list, page.getTotal(), page.getCurrent(), page.getSize());
    }

    @Override
    public InformationFormDetailVO detail(Long id) {
        InformationFormEntity entity = select(id);
        Long createdBy = entity.getCreatedBy();
        Map<Long, String> names = createdBy == null ? Map.of() : resolveActorNames(Set.of(createdBy));
        return toDetailVO(entity, actorName(createdBy, names));
    }

    @Override
    public InformationFormDetailVO publicDetail(String formCode) {
        InformationFormEntity form = formMapper.selectOne(tenantWrapper()
                .eq(InformationFormEntity::getFormCode, formCode).last("LIMIT 1"));
        Require.notNull(form, InformationCode.FORM_NOT_FOUND);
        Require.isTrue(InformationFormStatus.PUBLISHED.name().equals(form.getStatus()), InformationCode.FORM_NOT_PUBLISHED);
        return toDetailVO(form, null);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long save(SaveInformationFormCommand command) {
        validateDefinition(command.getDefinitionJson());
        InformationFormEntity entity;
        if (command.getId() == null) {
            entity = new InformationFormEntity();
            entity.setServerSecret(newSecret());
            entity.setServerSecretDigest(digest(entity.getServerSecret()));
            entity.setStatus(command.getStatus() == null ? InformationFormStatus.DRAFT.name() : command.getStatus().name());
        } else {
            entity = select(command.getId());
            if (command.getStatus() != null) {
                entity.setStatus(command.getStatus().name());
            }
        }
        InformationFormEntity duplicate = formMapper.selectOne(tenantWrapper()
                .eq(InformationFormEntity::getFormCode, command.getFormCode())
                .ne(command.getId() != null, InformationFormEntity::getId, command.getId())
                .last("LIMIT 1"));
        Require.isNull(duplicate, InformationCode.FORM_CODE_DUPLICATED);
        entity.setFormCode(command.getFormCode().trim());
        entity.setFormName(command.getFormName().trim());
        entity.setDescription(command.getDescription());
        entity.setDefinitionJson(command.getDefinitionJson());
        entity.setSuccessMessage(StringUtils.hasText(command.getSuccessMessage()) ? command.getSuccessMessage() : "提交成功");
        if (command.getId() == null) {
            formMapper.insert(entity);
        } else {
            formMapper.updateById(entity);
        }
        return entity.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean updateStatus(UpdateInformationFormStatusCommand command) {
        InformationFormEntity entity = select(command.getId());
        entity.setStatus(command.getStatus().name());
        return formMapper.updateById(entity) > 0;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public String regenerateSecret(Long id) {
        InformationFormEntity entity = select(id);
        entity.setServerSecret(newSecret());
        entity.setServerSecretDigest(digest(entity.getServerSecret()));
        formMapper.updateById(entity);
        return entity.getServerSecret();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean delete(Long id) {
        InformationFormEntity form = select(id);
        Require.isTrue(submissionMapper.selectCount(new LambdaQueryWrapper<InformationSubmissionEntity>()
                .eq(StringUtils.hasText(MangoContextHolder.tenantId()), InformationSubmissionEntity::getTenantId, MangoContextHolder.tenantId())
                .eq(InformationSubmissionEntity::getFormId, form.getId())) == 0, InformationCode.INVALID_SUBMISSION, "已有提交记录的表单不可删除");
        return formMapper.deleteById(form.getId()) > 0;
    }

    public InformationFormEntity select(Long id) {
        Require.notNull(id, InformationCode.FORM_NOT_FOUND);
        InformationFormEntity form = formMapper.selectOne(tenantWrapper().eq(InformationFormEntity::getId, id).last("LIMIT 1"));
        return Require.nonNull(form, InformationCode.FORM_NOT_FOUND);
    }

    public InformationFormEntity selectPublished(String formCode) {
        InformationFormEntity form = formMapper.selectOne(tenantWrapper()
                .eq(InformationFormEntity::getFormCode, formCode)
                .eq(InformationFormEntity::getStatus, InformationFormStatus.PUBLISHED.name())
                .last("LIMIT 1"));
        return Require.nonNull(form, InformationCode.FORM_NOT_PUBLISHED);
    }

    private String actorName(Long userId, Map<Long, String> names) {
        return userId == null || names == null ? null : names.get(userId);
    }

    private Map<Long, String> resolveActorNames(Set<Long> userIds) {
        Set<Long> resolvedIds = userIds.stream().filter(java.util.Objects::nonNull).collect(java.util.stream.Collectors.toCollection(LinkedHashSet::new));
        if (resolvedIds.isEmpty()) {
            return Map.of();
        }
        InformationActorNameProvider provider = actorNameProvider.getIfAvailable();
        if (provider == null) {
            return Map.of();
        }
        Map<Long, String> names = provider.resolveNames(resolvedIds);
        return names == null ? Map.of() : names;
    }

    private LambdaQueryWrapper<InformationFormEntity> tenantWrapper() {
        return new LambdaQueryWrapper<InformationFormEntity>()
                .eq(StringUtils.hasText(MangoContextHolder.tenantId()), InformationFormEntity::getTenantId, MangoContextHolder.tenantId());
    }

    private InformationFormVO toListVO(InformationFormEntity entity, Map<Long, String> actorNames) {
        InformationFormVO vo = new InformationFormVO();
        vo.setId(entity.getId());
        vo.setFormCode(entity.getFormCode());
        vo.setFormName(entity.getFormName());
        vo.setDescription(entity.getDescription());
        vo.setStatus(InformationFormStatus.valueOf(entity.getStatus()));
        vo.setFieldCount(fieldCount(entity.getDefinitionJson()));
        vo.setSubmissionCount(submissionCount(entity.getId()));
        vo.setHostedPath("/information/hosted?formCode=" + entity.getFormCode());
        vo.setCreatedByName(actorName(entity.getCreatedBy(), actorNames));
        vo.setCreatedAt(entity.getCreatedAt());
        vo.setUpdatedAt(entity.getUpdatedAt());
        return vo;
    }

    private InformationFormDetailVO toDetailVO(InformationFormEntity entity, String createdByName) {
        InformationFormDetailVO vo = new InformationFormDetailVO();
        vo.setId(entity.getId());
        vo.setFormCode(entity.getFormCode());
        vo.setFormName(entity.getFormName());
        vo.setDescription(entity.getDescription());
        vo.setDefinitionJson(entity.getDefinitionJson());
        vo.setSuccessMessage(entity.getSuccessMessage());
        vo.setStatus(InformationFormStatus.valueOf(entity.getStatus()));
        vo.setServerSecret(entity.getServerSecret());
        vo.setHostedPath("/information/hosted?formCode=" + entity.getFormCode());
        vo.setSubmissionCount(submissionCount(entity.getId()));
        vo.setCreatedByName(createdByName);
        vo.setCreatedAt(entity.getCreatedAt());
        vo.setUpdatedAt(entity.getUpdatedAt());
        return vo;
    }

    private long submissionCount(Long formId) {
        return submissionMapper.selectCount(new LambdaQueryWrapper<InformationSubmissionEntity>()
                .eq(StringUtils.hasText(MangoContextHolder.tenantId()), InformationSubmissionEntity::getTenantId, MangoContextHolder.tenantId())
                .eq(InformationSubmissionEntity::getFormId, formId));
    }

    private int fieldCount(String json) {
        try {
            return InformationDefinition.fields(objectMapper.readTree(json)).size();
        } catch (Exception e) {
            return 0;
        }
    }

    /** 定义必须是 JSON 数组，且至少包含一个可提交采集项，字段编码不可重复。 */
    private void validateDefinition(String json) {
        try {
            JsonNode node = objectMapper.readTree(json);
            Require.isTrue(node.isArray(), InformationCode.INVALID_FORM_DEFINITION);
            List<InformationDefinition.Field> fields = InformationDefinition.fields(node);
            Require.isTrue(!fields.isEmpty(), InformationCode.INVALID_FORM_DEFINITION);
            Set<String> codes = new LinkedHashSet<>();
            for (InformationDefinition.Field field : fields) {
                Require.isTrue(codes.add(field.code()), InformationCode.INVALID_FORM_DEFINITION, "字段编码重复：" + field.code());
            }
        } catch (BizException e) {
            throw e;
        } catch (Exception e) {
            throw new BizException(InformationCode.INVALID_FORM_DEFINITION.getCode(), InformationCode.INVALID_FORM_DEFINITION.getMessage(), e);
        }
    }

    private String newSecret() {
        byte[] bytes = new byte[24];
        RANDOM.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    public static String digest(String secret) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(secret.getBytes(StandardCharsets.UTF_8)));
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }

    static LocalDateTime now() {
        return LocalDateTime.now();
    }
}
