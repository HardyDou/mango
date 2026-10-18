package io.mango.information.core.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.mango.common.result.Require;
import io.mango.common.exception.BizException;
import io.mango.common.vo.PageResult;
import io.mango.information.api.command.SaveInformationFormCommand;
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
import io.mango.infra.context.api.MangoContextHolder;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.HexFormat;
import java.util.List;

/** 信息采集表单服务。 */
@Service
@RequiredArgsConstructor
public class InformationFormService implements IInformationFormService {
    private static final SecureRandom RANDOM = new SecureRandom();
    private final InformationFormMapper formMapper;
    private final InformationSubmissionMapper submissionMapper;
    private final ObjectMapper objectMapper;

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
        List<InformationFormVO> list = page.getRecords().stream().map(this::toListVO).toList();
        return PageResult.of(list, page.getTotal(), page.getCurrent(), page.getSize());
    }

    @Override
    public InformationFormDetailVO detail(Long id) {
        return toDetailVO(select(id));
    }

    @Override
    public InformationFormDetailVO publicDetail(String formCode) {
        InformationFormEntity form = formMapper.selectOne(tenantWrapper()
                .eq(InformationFormEntity::getFormCode, formCode).last("LIMIT 1"));
        Require.notNull(form, InformationCode.FORM_NOT_FOUND);
        Require.isTrue(InformationFormStatus.PUBLISHED.name().equals(form.getStatus()), InformationCode.FORM_NOT_PUBLISHED);
        return toDetailVO(form);
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
            entity.setStatus(Boolean.TRUE.equals(command.getPublished()) ? InformationFormStatus.PUBLISHED.name() : InformationFormStatus.DRAFT.name());
        } else {
            entity = select(command.getId());
            entity.setStatus(Boolean.TRUE.equals(command.getPublished()) ? InformationFormStatus.PUBLISHED.name() : InformationFormStatus.DRAFT.name());
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

    private LambdaQueryWrapper<InformationFormEntity> tenantWrapper() {
        return new LambdaQueryWrapper<InformationFormEntity>()
                .eq(StringUtils.hasText(MangoContextHolder.tenantId()), InformationFormEntity::getTenantId, MangoContextHolder.tenantId());
    }

    private InformationFormVO toListVO(InformationFormEntity entity) {
        InformationFormVO vo = new InformationFormVO();
        vo.setId(entity.getId()); vo.setFormCode(entity.getFormCode()); vo.setFormName(entity.getFormName());
        vo.setDescription(entity.getDescription()); vo.setStatus(InformationFormStatus.valueOf(entity.getStatus()));
        vo.setFieldCount(fieldCount(entity.getDefinitionJson()));
        vo.setSubmissionCount(submissionMapper.selectCount(new LambdaQueryWrapper<InformationSubmissionEntity>()
                .eq(StringUtils.hasText(MangoContextHolder.tenantId()), InformationSubmissionEntity::getTenantId, MangoContextHolder.tenantId())
                .eq(InformationSubmissionEntity::getFormId, entity.getId())));
        vo.setHostedPath("/information/hosted?formCode=" + entity.getFormCode());
        vo.setUpdatedAt(entity.getUpdatedAt());
        return vo;
    }

    private InformationFormDetailVO toDetailVO(InformationFormEntity entity) {
        InformationFormDetailVO vo = new InformationFormDetailVO();
        vo.setId(entity.getId()); vo.setFormCode(entity.getFormCode()); vo.setFormName(entity.getFormName());
        vo.setDescription(entity.getDescription()); vo.setDefinitionJson(entity.getDefinitionJson());
        vo.setSuccessMessage(entity.getSuccessMessage()); vo.setStatus(InformationFormStatus.valueOf(entity.getStatus()));
        vo.setServerSecret(entity.getServerSecret()); vo.setHostedPath("/information/hosted?formCode=" + entity.getFormCode());
        vo.setCreatedAt(entity.getCreatedAt()); vo.setUpdatedAt(entity.getUpdatedAt());
        return vo;
    }

    private int fieldCount(String json) {
        try { return objectMapper.readTree(json).size(); } catch (Exception e) { return 0; }
    }

    private void validateDefinition(String json) {
        try {
            JsonNode node = objectMapper.readTree(json);
            Require.isTrue(node.isArray() && !node.isEmpty(), InformationCode.INVALID_FORM_DEFINITION);
            for (JsonNode field : node) {
                Require.isTrue(field.hasNonNull("fieldCode") && field.hasNonNull("fieldName") && field.hasNonNull("fieldType"), InformationCode.INVALID_FORM_DEFINITION);
            }
        } catch (Exception e) {
            throw new BizException(InformationCode.INVALID_FORM_DEFINITION.getCode(), InformationCode.INVALID_FORM_DEFINITION.getMessage(), e);
        }
    }

    private String newSecret() {
        byte[] bytes = new byte[24]; RANDOM.nextBytes(bytes);
        return java.util.Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    public static String digest(String secret) {
        try { return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(secret.getBytes(StandardCharsets.UTF_8))); }
        catch (Exception e) { throw new IllegalStateException(e); }
    }
}
