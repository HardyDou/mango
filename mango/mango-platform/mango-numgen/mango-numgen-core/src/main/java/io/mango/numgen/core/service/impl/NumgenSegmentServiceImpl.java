package io.mango.numgen.core.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import io.mango.common.result.R;
import io.mango.common.result.Require;
import io.mango.common.vo.PageResult;
import io.mango.numgen.api.command.SaveNumgenRuleSegmentCommand;
import io.mango.numgen.api.query.NumgenSegmentPageQuery;
import io.mango.numgen.api.vo.NumgenRuleSegmentVO;
import io.mango.numgen.core.entity.NumgenRule;
import io.mango.numgen.core.entity.NumgenRuleSegment;
import io.mango.numgen.core.mapper.NumgenRuleMapper;
import io.mango.numgen.core.mapper.NumgenRuleSegmentMapper;
import io.mango.numgen.core.service.INumgenSegmentService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class NumgenSegmentServiceImpl implements INumgenSegmentService {

    private static final Set<String> SUPPORTED_TYPES = Set.of("TEXT", "DATE", "PARAM", "SEQ", "EXPR");

    private final NumgenRuleSegmentMapper segmentMapper;
    private final NumgenRuleMapper ruleMapper;

    @Override
    public R<PageResult<NumgenRuleSegmentVO>> pageSegments(NumgenSegmentPageQuery query) {
        NumgenSegmentPageQuery resolved = query == null ? new NumgenSegmentPageQuery() : query;
        LambdaQueryWrapper<NumgenRuleSegment> wrapper = new LambdaQueryWrapper<NumgenRuleSegment>()
                .eq(resolved.getRuleId() != null, NumgenRuleSegment::getRuleId, resolved.getRuleId())
                .eq(NumgenRuleSegment::getTenantId, NumgenContextSupport.currentTenantId())
                .orderByAsc(NumgenRuleSegment::getSortOrder)
                .orderByAsc(NumgenRuleSegment::getId);
        IPage<NumgenRuleSegment> page = segmentMapper.selectPage(new Page<>(resolved.getPage(), resolved.getSize()), wrapper);
        List<NumgenRuleSegmentVO> records = page.getRecords().stream().map(this::toVO).collect(Collectors.toList());
        return R.ok(PageResult.of(records, page.getTotal(), page.getCurrent(), page.getSize()));
    }

    @Override
    public R<NumgenRuleSegmentVO> detailSegment(Long id) {
        return R.ok(toVO(selectRequired(id)));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public R<Long> createSegment(SaveNumgenRuleSegmentCommand command) {
        Require.notNull(command, "编号规则片段不能为空");
        validate(command, false);
        NumgenRule rule = selectRuleRequired(command.getRuleId());
        requireEditableRule(rule);
        NumgenRuleSegment entity = new NumgenRuleSegment();
        copy(command, entity);
        entity.setTenantId(rule.getTenantId());
        segmentMapper.insert(entity);
        return R.ok(entity.getId());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public R<Boolean> updateSegment(SaveNumgenRuleSegmentCommand command) {
        Require.notNull(command, "编号规则片段不能为空");
        Require.notNull(command.getId(), "编号规则片段 ID 不能为空");
        validate(command, true);
        NumgenRuleSegment entity = selectRequired(command.getId());
        NumgenRule rule = selectRuleRequired(command.getRuleId());
        requireEditableRule(rule);
        copy(command, entity);
        return R.ok(segmentMapper.updateById(entity) > 0);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public R<Boolean> deleteSegment(Long id) {
        NumgenRuleSegment entity = selectRequired(id);
        NumgenRule rule = selectRuleRequired(entity.getRuleId());
        requireEditableRule(rule);
        return R.ok(segmentMapper.deleteById(id) > 0);
    }

    private void validate(SaveNumgenRuleSegmentCommand command, boolean update) {
        if (update) {
            Require.notNull(command.getId(), "编号规则片段 ID 不能为空");
        }
        Require.notNull(command.getRuleId(), "规则 ID 不能为空");
        Require.notNull(command.getSortOrder(), "排序不能为空");
        Require.notBlank(command.getSegmentType(), "片段类型不能为空");
        Require.isTrue(SUPPORTED_TYPES.contains(command.getSegmentType()), "不支持的片段类型：" + command.getSegmentType());
        Require.notBlank(command.getSegmentName(), "片段名称不能为空");
        switch (command.getSegmentType()) {
            case "TEXT" -> Require.notBlank(command.getLiteralValue(), "字符串不能为空");
            case "EXPR" -> Require.notBlank(command.getLiteralValue(), "表达式不能为空");
            case "DATE" -> Require.notBlank(command.getDateFormat(), "日期格式不能为空");
            case "PARAM" -> Require.notBlank(command.getVariableKey(), "参数键不能为空");
            case "SEQ" -> Require.notNull(command.getSeqWidth(), "流水位数不能为空");
            default -> {
            }
        }
    }

    private NumgenRule selectRuleRequired(Long ruleId) {
        Require.notNull(ruleId, "规则 ID 不能为空");
        NumgenRule rule = ruleMapper.selectById(ruleId);
        Require.notNull(rule, "编号规则不存在");
        Require.isTrue(NumgenContextSupport.currentTenantId().equals(rule.getTenantId()), "编号规则不存在");
        return rule;
    }

    private void requireEditableRule(NumgenRule rule) {
        Require.isTrue("DRAFT".equals(rule.getVersionState()), "只有草稿版本可以修改片段");
    }

    private NumgenRuleSegment selectRequired(Long id) {
        Require.notNull(id, "编号规则片段 ID 不能为空");
        NumgenRuleSegment entity = segmentMapper.selectById(id);
        Require.notNull(entity, "编号规则片段不存在");
        Require.isTrue(NumgenContextSupport.currentTenantId().equals(entity.getTenantId()), "编号规则片段不存在");
        return entity;
    }

    private void copy(SaveNumgenRuleSegmentCommand command, NumgenRuleSegment entity) {
        entity.setRuleId(command.getRuleId());
        entity.setSortOrder(command.getSortOrder());
        entity.setSegmentType(command.getSegmentType());
        entity.setSegmentName(command.getSegmentName().trim());
        entity.setLiteralValue(NumgenContextSupport.trimToNull(command.getLiteralValue()));
        entity.setVariableKey(NumgenContextSupport.trimToNull(command.getVariableKey()));
        entity.setDateFormat(NumgenContextSupport.trimToNull(command.getDateFormat()));
        entity.setSeqWidth(command.getSeqWidth());
        entity.setPadChar(NumgenContextSupport.trimToNull(command.getPadChar()) == null ? "0" : command.getPadChar().trim());
    }

    private NumgenRuleSegmentVO toVO(NumgenRuleSegment entity) {
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
        vo.setCreateTime(entity.getCreateTime());
        vo.setUpdateTime(entity.getUpdateTime());
        return vo;
    }
}
