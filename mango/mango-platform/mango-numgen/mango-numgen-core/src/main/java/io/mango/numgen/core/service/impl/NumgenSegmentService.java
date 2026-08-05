package io.mango.numgen.core.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import io.mango.common.result.Require;
import io.mango.common.vo.PageResult;
import io.mango.numgen.api.command.SaveNumgenRuleSegmentCommand;
import io.mango.numgen.api.enums.NumgenCode;
import io.mango.numgen.api.query.NumgenSegmentPageQuery;
import io.mango.numgen.api.vo.NumgenRuleSegmentVO;
import io.mango.numgen.core.entity.NumgenRuleEntity;
import io.mango.numgen.core.entity.NumgenRuleSegmentEntity;
import io.mango.numgen.core.mapper.NumgenRuleMapper;
import io.mango.numgen.core.mapper.NumgenRuleSegmentMapper;
import io.mango.numgen.core.service.INumgenSegmentService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class NumgenSegmentService implements INumgenSegmentService {

    private static final Set<String> SUPPORTED_TYPES = Set.of("TEXT", "DATE", "PARAM", "SEQ", "EXPR");

    private final NumgenRuleSegmentMapper segmentMapper;
    private final NumgenRuleMapper ruleMapper;

    @Override
    public PageResult<NumgenRuleSegmentVO> pageSegments(NumgenSegmentPageQuery query) {
        NumgenSegmentPageQuery resolved = query == null ? new NumgenSegmentPageQuery() : query;
        LambdaQueryWrapper<NumgenRuleSegmentEntity> wrapper = new LambdaQueryWrapper<NumgenRuleSegmentEntity>()
                .eq(resolved.getRuleId() != null, NumgenRuleSegmentEntity::getRuleId, resolved.getRuleId())
                .eq(NumgenRuleSegmentEntity::getTenantId, NumgenContextSupport.currentTenantId())
                .orderByAsc(NumgenRuleSegmentEntity::getSortOrder)
                .orderByAsc(NumgenRuleSegmentEntity::getId);
        IPage<NumgenRuleSegmentEntity> page = segmentMapper.selectPage(new Page<>(resolved.getPage(), resolved.getSize()), wrapper);
        List<NumgenRuleSegmentVO> records = page.getRecords().stream().map(this::toVO).collect(Collectors.toList());
        return PageResult.of(records, page.getTotal(), page.getCurrent(), page.getSize());
    }

    @Override
    public NumgenRuleSegmentVO detailSegment(Long id) {
        return toVO(selectRequired(id));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long createSegment(SaveNumgenRuleSegmentCommand command) {
        Require.notNull(command, NumgenCode.NUMGEN_SEGMENT_INVALID, "编号规则片段不能为空");
        validate(command, false);
        NumgenRuleEntity rule = selectRuleRequired(command.getRuleId());
        requireEditableRule(rule);
        NumgenRuleSegmentEntity entity = new NumgenRuleSegmentEntity();
        copy(command, entity);
        entity.setTenantId(rule.getTenantId());
        segmentMapper.insert(entity);
        return entity.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Boolean updateSegment(SaveNumgenRuleSegmentCommand command) {
        Require.notNull(command, NumgenCode.NUMGEN_SEGMENT_INVALID, "编号规则片段不能为空");
        Require.notNull(command.getId(), NumgenCode.NUMGEN_SEGMENT_INVALID, "编号规则片段 ID 不能为空");
        validate(command, true);
        NumgenRuleSegmentEntity entity = selectRequired(command.getId());
        NumgenRuleEntity rule = selectRuleRequired(entity.getRuleId());
        requireEditableRule(rule);
        Require.isTrue(entity.getRuleId().equals(command.getRuleId()), NumgenCode.NUMGEN_SEGMENT_RULE_IMMUTABLE,
                "片段所属规则不能修改");
        copy(command, entity);
        return segmentMapper.updateById(entity) > 0;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Boolean deleteSegment(Long id) {
        NumgenRuleSegmentEntity entity = selectRequired(id);
        NumgenRuleEntity rule = selectRuleRequired(entity.getRuleId());
        requireEditableRule(rule);
        int deleted = segmentMapper.deleteById(id);
        Require.isTrue(deleted > 0, NumgenCode.NUMGEN_SEGMENT_NOT_FOUND);
        return Boolean.TRUE;
    }

    private void validate(SaveNumgenRuleSegmentCommand command, boolean update) {
        if (update) {
            Require.notNull(command.getId(), NumgenCode.NUMGEN_SEGMENT_INVALID, "编号规则片段 ID 不能为空");
        }
        Require.notNull(command.getRuleId(), NumgenCode.NUMGEN_SEGMENT_INVALID, "规则 ID 不能为空");
        Require.notNull(command.getSortOrder(), NumgenCode.NUMGEN_SEGMENT_INVALID, "排序不能为空");
        Require.notBlank(command.getSegmentType(), NumgenCode.NUMGEN_SEGMENT_INVALID, "片段类型不能为空");
        Require.isTrue(SUPPORTED_TYPES.contains(command.getSegmentType()), NumgenCode.NUMGEN_SEGMENT_INVALID,
                "不支持的片段类型：" + command.getSegmentType());
        Require.notBlank(command.getSegmentName(), NumgenCode.NUMGEN_SEGMENT_INVALID, "片段名称不能为空");
        Require.isTrue(!"SEQ".equals(command.getSegmentType()) || !Integer.valueOf(1).equals(command.getSequenceScope()),
                NumgenCode.NUMGEN_SEGMENT_INVALID, "流水片段不能参与流水分组");
        switch (command.getSegmentType()) {
            case "TEXT" -> Require.notBlank(command.getLiteralValue(), NumgenCode.NUMGEN_SEGMENT_INVALID, "字符串不能为空");
            case "EXPR" -> Require.notBlank(command.getLiteralValue(), NumgenCode.NUMGEN_SEGMENT_INVALID, "表达式不能为空");
            case "DATE" -> {
                Require.notBlank(command.getDateFormat(), NumgenCode.NUMGEN_SEGMENT_INVALID, "日期格式不能为空");
                try {
                    DateTimeFormatter.ofPattern(command.getDateFormat());
                } catch (IllegalArgumentException exception) {
                    Require.fail(NumgenCode.NUMGEN_SEGMENT_INVALID, "日期格式非法：" + command.getDateFormat());
                }
            }
            case "PARAM" -> Require.notBlank(command.getVariableKey(), NumgenCode.NUMGEN_SEGMENT_INVALID, "参数键不能为空");
            case "SEQ" -> Require.notNull(command.getSeqWidth(), NumgenCode.NUMGEN_SEGMENT_INVALID, "流水位数不能为空");
            default -> Require.fail(NumgenCode.NUMGEN_SEGMENT_INVALID, "不支持的片段类型：" + command.getSegmentType());
        }
    }

    private NumgenRuleEntity selectRuleRequired(Long ruleId) {
        Require.notNull(ruleId, NumgenCode.NUMGEN_SEGMENT_INVALID, "规则 ID 不能为空");
        NumgenRuleEntity rule = ruleMapper.selectById(ruleId);
        Require.notNull(rule, NumgenCode.NUMGEN_RULE_NOT_FOUND);
        Require.isTrue(NumgenContextSupport.currentTenantId().equals(rule.getTenantId()), NumgenCode.NUMGEN_RULE_NOT_FOUND);
        return rule;
    }

    private void requireEditableRule(NumgenRuleEntity rule) {
        Require.isTrue("DRAFT".equals(rule.getVersionState()), NumgenCode.NUMGEN_RULE_NOT_EDITABLE,
                "只有草稿版本可以修改片段");
    }

    private NumgenRuleSegmentEntity selectRequired(Long id) {
        Require.notNull(id, NumgenCode.NUMGEN_SEGMENT_INVALID, "编号规则片段 ID 不能为空");
        NumgenRuleSegmentEntity entity = segmentMapper.selectById(id);
        Require.notNull(entity, NumgenCode.NUMGEN_SEGMENT_NOT_FOUND);
        Require.isTrue(NumgenContextSupport.currentTenantId().equals(entity.getTenantId()), NumgenCode.NUMGEN_SEGMENT_NOT_FOUND);
        return entity;
    }

    private void copy(SaveNumgenRuleSegmentCommand command, NumgenRuleSegmentEntity entity) {
        entity.setRuleId(command.getRuleId());
        entity.setSortOrder(command.getSortOrder());
        entity.setSegmentType(command.getSegmentType());
        entity.setSegmentName(command.getSegmentName().trim());
        entity.setLiteralValue(NumgenContextSupport.trimToNull(command.getLiteralValue()));
        entity.setVariableKey(NumgenContextSupport.trimToNull(command.getVariableKey()));
        entity.setDateFormat(NumgenContextSupport.trimToNull(command.getDateFormat()));
        entity.setSeqWidth(command.getSeqWidth());
        entity.setPadChar(NumgenContextSupport.trimToNull(command.getPadChar()) == null ? "0" : command.getPadChar().trim());
        entity.setSequenceScope("SEQ".equals(command.getSegmentType()) ? 0 : (Integer.valueOf(1).equals(command.getSequenceScope()) ? 1 : 0));
    }

    private NumgenRuleSegmentVO toVO(NumgenRuleSegmentEntity entity) {
        NumgenRuleSegmentVO vo = new NumgenRuleSegmentVO();
        vo.setId(entity.getId());
        vo.setRuleId(entity.getRuleId());
        vo.setSortOrder(entity.getSortOrder());
        vo.setSegmentType(entity.getSegmentType());
        vo.setSegmentName(entity.getSegmentName());
        vo.setLiteralValue(entity.getLiteralValue());
        vo.setVariableKey(entity.getVariableKey());
        vo.setDateFormat(entity.getDateFormat());
        vo.setSeqWidth(entity.getSeqWidth());
        vo.setPadChar(entity.getPadChar());
        vo.setSequenceScope(entity.getSequenceScope());
        vo.setCreateTime(entity.getCreatedAt());
        vo.setUpdateTime(entity.getUpdatedAt());
        return vo;
    }
}
