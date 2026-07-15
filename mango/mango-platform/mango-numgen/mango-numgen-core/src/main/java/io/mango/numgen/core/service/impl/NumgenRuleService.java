package io.mango.numgen.core.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import io.mango.common.result.Require;
import io.mango.common.vo.PageResult;
import io.mango.infra.kv.api.ICache;
import io.mango.numgen.api.command.NumgenPreviewCommand;
import io.mango.numgen.api.command.NumgenPublishCommand;
import io.mango.numgen.api.command.SaveNumgenRuleCommand;
import io.mango.numgen.api.command.UpdateNumgenRuleStatusCommand;
import io.mango.numgen.api.enums.NumgenCode;
import io.mango.numgen.api.query.NumgenRulePageQuery;
import io.mango.numgen.api.vo.NumgenPreviewVO;
import io.mango.numgen.api.vo.NumgenRuleVO;
import io.mango.numgen.core.config.NumgenKvProperties;
import io.mango.numgen.core.entity.NumgenGeneratorEntity;
import io.mango.numgen.core.entity.NumgenRuleEntity;
import io.mango.numgen.core.entity.NumgenRuleSegmentEntity;
import io.mango.numgen.core.mapper.NumgenGeneratorMapper;
import io.mango.numgen.core.mapper.NumgenRuleMapper;
import io.mango.numgen.core.mapper.NumgenRuleSegmentMapper;
import io.mango.numgen.core.service.INumgenRuleService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@EnableConfigurationProperties(NumgenKvProperties.class)
public class NumgenRuleService implements INumgenRuleService {

    private final NumgenRuleMapper ruleMapper;
    private final NumgenGeneratorMapper generatorMapper;
    private final NumgenRuleSegmentMapper segmentMapper;
    private final NumgenRuleRenderer ruleRenderer;
    private final ObjectProvider<ICache> cacheProvider;

    private static final String VERSION_STATE_DRAFT = "DRAFT";
    private static final String VERSION_STATE_ACTIVE = "ACTIVE";
    private static final String VERSION_STATE_HISTORY = "HISTORY";

    @Override
    public PageResult<NumgenRuleVO> pageRules(NumgenRulePageQuery query) {
        NumgenRulePageQuery resolved = query == null ? new NumgenRulePageQuery() : query;
        IPage<NumgenRuleEntity> page = ruleMapper.selectPage(new Page<>(resolved.getPage(), resolved.getSize()), wrapper(resolved));
        List<NumgenRuleVO> records = page.getRecords().stream().map(this::toRuleVO).collect(Collectors.toList());
        return PageResult.of(records, page.getTotal(), page.getCurrent(), page.getSize());
    }

    @Override
    public NumgenRuleVO detailRule(Long id) {
        return toRuleVO(selectRuleRequired(id));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long createRule(SaveNumgenRuleCommand command) {
        Require.notNull(command, NumgenCode.NUMGEN_INVALID, "编号规则不能为空");
        validate(command, false);
        String tenantId = NumgenContextSupport.currentTenantId();
        NumgenGeneratorEntity generator = selectGeneratorRequired(command.getGenKey(), tenantId);
        NumgenRuleEntity entity = new NumgenRuleEntity();
        copy(command, entity);
        entity.setGenKey(generator.getGenKey());
        entity.setTenantId(tenantId);
        entity.setPublishStatus(0);
        entity.setVersionState(VERSION_STATE_DRAFT);
        ruleMapper.insert(entity);
        return entity.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Boolean updateRule(SaveNumgenRuleCommand command) {
        Require.notNull(command, NumgenCode.NUMGEN_INVALID, "编号规则不能为空");
        Require.notNull(command.getId(), NumgenCode.NUMGEN_INVALID, "编号规则 ID 不能为空");
        validate(command, true);
        NumgenRuleEntity entity = selectRuleRequired(command.getId());
        Require.isTrue(VERSION_STATE_DRAFT.equals(entity.getVersionState()), NumgenCode.NUMGEN_RULE_NOT_EDITABLE,
                "只有草稿规则可以编辑");
        selectGeneratorRequired(command.getGenKey(), entity.getTenantId());
        copy(command, entity);
        entity.setPublishStatus(0);
        entity.setVersionState(VERSION_STATE_DRAFT);
        return ruleMapper.updateById(entity) > 0;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Boolean updateRuleStatus(UpdateNumgenRuleStatusCommand command) {
        Require.notNull(command, NumgenCode.NUMGEN_INVALID, "状态命令不能为空");
        Require.notNull(command.getId(), NumgenCode.NUMGEN_INVALID, "编号规则 ID 不能为空");
        Require.notNull(command.getStatus(), NumgenCode.NUMGEN_INVALID, "状态不能为空");
        NumgenRuleEntity entity = selectRuleRequired(command.getId());
        entity.setStatus(command.getStatus());
        evictRuleCache(entity.getTenantId(), entity.getGenKey());
        return ruleMapper.updateById(entity) > 0;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Boolean deleteRule(Long id) {
        NumgenRuleEntity entity = selectRuleRequired(id);
        Require.isTrue(VERSION_STATE_DRAFT.equals(entity.getVersionState()), NumgenCode.NUMGEN_RULE_NOT_EDITABLE,
                "只有草稿规则可以删除");
        int deleted = ruleMapper.deleteById(id);
        Require.isTrue(deleted > 0, NumgenCode.NUMGEN_RULE_NOT_FOUND);
        return Boolean.TRUE;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Boolean publishRule(NumgenPublishCommand command) {
        Require.notNull(command, NumgenCode.NUMGEN_INVALID, "发布命令不能为空");
        NumgenRuleEntity selectedRule = selectPublishRule(command);
        List<NumgenRuleSegmentEntity> selectedSegments = segmentMapper.selectByRuleId(selectedRule.getId(), selectedRule.getTenantId());
        Require.isTrue(ruleRenderer.validate(selectedRule, selectedSegments).isValid(),
                NumgenCode.NUMGEN_RULE_NOT_PUBLISHABLE, "规则片段配置不完整，不能发布");
        Require.isTrue(selectedRule.getStatus() == null || selectedRule.getStatus() == 1,
                NumgenCode.NUMGEN_RULE_NOT_PUBLISHABLE, "停用规则不能发布");

        NumgenRuleEntity rule = selectedRule;
        if (VERSION_STATE_HISTORY.equals(selectedRule.getVersionState())) {
            rule = cloneHistoricalRuleAsNextVersion(selectedRule, selectedSegments);
        }

        List<NumgenRuleEntity> versions = ruleMapper.selectVersionsByGenKey(rule.getGenKey(), rule.getTenantId());
        for (NumgenRuleEntity version : versions) {
            if (!version.getId().equals(rule.getId())) {
                version.setPublishStatus(0);
                version.setVersionState(VERSION_STATE_HISTORY);
                ruleMapper.updateById(version);
            }
        }
        rule.setPublishStatus(1);
        rule.setVersionState(VERSION_STATE_ACTIVE);
        ruleMapper.updateById(rule);

        NumgenGeneratorEntity generator = selectGeneratorRequired(rule.getGenKey(), rule.getTenantId());
        generator.setCurrentRuleVersion(rule.getVersion());
        generator.setCurrentPublishStatus(rule.getPublishStatus());
        generatorMapper.updateById(generator);
        evictRuleCache(rule.getTenantId(), rule.getGenKey());
        return Boolean.TRUE;
    }

    @Override
    public NumgenPreviewVO previewRule(NumgenPreviewCommand command) {
        Require.notNull(command, NumgenCode.NUMGEN_INVALID, "预览命令不能为空");
        Require.notBlank(command.getGenKey(), NumgenCode.NUMGEN_INVALID, "规则键不能为空");
        NumgenRuleEntity rule = ruleMapper.selectActiveByGenKey(command.getGenKey(), NumgenContextSupport.currentTenantId());
        Require.notNull(rule, NumgenCode.NUMGEN_ACTIVE_RULE_NOT_FOUND,
                "编号规则不存在或未发布：" + command.getGenKey());
        List<NumgenRuleSegmentEntity> segments = segmentMapper.selectByRuleId(rule.getId(), rule.getTenantId());
        return ruleRenderer.preview(rule, segments, command.getParams(), command.getCount());
    }

    private LambdaQueryWrapper<NumgenRuleEntity> wrapper(NumgenRulePageQuery query) {
        String keyword = NumgenContextSupport.trimToNull(query.getKeyword());
        return new LambdaQueryWrapper<NumgenRuleEntity>()
                .eq(StringUtils.hasText(query.getGenKey()), NumgenRuleEntity::getGenKey, query.getGenKey())
                .and(StringUtils.hasText(keyword), nested -> nested
                        .like(NumgenRuleEntity::getRuleName, keyword)
                        .or()
                        .like(NumgenRuleEntity::getGenKey, keyword))
                .eq(query.getStatus() != null, NumgenRuleEntity::getStatus, query.getStatus())
                .eq(query.getPublishStatus() != null, NumgenRuleEntity::getPublishStatus, query.getPublishStatus())
                .eq(NumgenRuleEntity::getTenantId, NumgenContextSupport.currentTenantId())
                .orderByDesc(NumgenRuleEntity::getUpdatedAt);
    }

    private void validate(SaveNumgenRuleCommand command, boolean update) {
        if (update) {
            Require.notNull(command.getId(), NumgenCode.NUMGEN_INVALID, "编号规则 ID 不能为空");
        }
        Require.notBlank(command.getGenKey(), NumgenCode.NUMGEN_INVALID, "规则键不能为空");
        Require.notBlank(command.getRuleName(), NumgenCode.NUMGEN_INVALID, "规则名称不能为空");
    }

    private NumgenRuleEntity selectRuleRequired(Long id) {
        Require.notNull(id, NumgenCode.NUMGEN_INVALID, "编号规则 ID 不能为空");
        NumgenRuleEntity entity = ruleMapper.selectById(id);
        Require.notNull(entity, NumgenCode.NUMGEN_RULE_NOT_FOUND);
        Require.isTrue(NumgenContextSupport.currentTenantId().equals(entity.getTenantId()),
                NumgenCode.NUMGEN_RULE_NOT_FOUND);
        return entity;
    }

    private NumgenGeneratorEntity selectGeneratorRequired(String genKey, String tenantId) {
        Require.notBlank(genKey, NumgenCode.NUMGEN_INVALID, "业务 Key 不能为空");
        NumgenGeneratorEntity generator = selectGenerator(genKey.trim(), tenantId);
        Require.notNull(generator, NumgenCode.NUMGEN_GENERATOR_NOT_FOUND, "编号生成器不存在：" + genKey);
        Require.isTrue(generator.getStatus() == null || generator.getStatus() == 1,
                NumgenCode.NUMGEN_GENERATOR_NOT_FOUND, "编号生成器已停用：" + genKey);
        return generator;
    }

    private void copy(SaveNumgenRuleCommand command, NumgenRuleEntity entity) {
        entity.setGenKey(command.getGenKey().trim());
        entity.setRuleName(command.getRuleName().trim());
        entity.setVersion(command.getVersion() == null ? 1 : command.getVersion());
        entity.setStatus(command.getStatus() == null ? 1 : command.getStatus());
    }

    private NumgenRuleVO toRuleVO(NumgenRuleEntity entity) {
        NumgenRuleVO vo = new NumgenRuleVO();
        vo.setId(entity.getId());
        vo.setGenKey(entity.getGenKey());
        NumgenGeneratorEntity generator = selectGenerator(entity.getGenKey(), entity.getTenantId());
        vo.setGenName(generator == null ? null : generator.getGenName());
        vo.setRuleName(entity.getRuleName());
        vo.setVersion(entity.getVersion());
        vo.setStatus(entity.getStatus());
        vo.setPublishStatus(entity.getPublishStatus());
        vo.setVersionState(entity.getVersionState());
        vo.setCreateTime(entity.getCreatedAt());
        vo.setUpdateTime(entity.getUpdatedAt());
        return vo;
    }

    private void evictRuleCache(String tenantId, String genKey) {
        ICache cache = cacheProvider.getIfAvailable();
        if (cache != null) {
            cache.delete("numgen:rule:" + tenantId + ":" + genKey);
        }
    }

    private NumgenRuleEntity selectPublishRule(NumgenPublishCommand command) {
        if (command.getRuleId() != null) {
            return selectRuleRequired(command.getRuleId());
        }
        Require.notBlank(command.getGenKey(), NumgenCode.NUMGEN_INVALID, "业务 Key 不能为空");
        String tenantId = NumgenContextSupport.currentTenantId();
        NumgenGeneratorEntity generator = selectGeneratorRequired(command.getGenKey(), tenantId);
        NumgenRuleEntity rule = ruleMapper.selectLatestDraftByGenKey(generator.getGenKey(), tenantId);
        Require.notNull(rule, NumgenCode.NUMGEN_RULE_NO_DRAFT, "没有可发布的规则，请先保存规则配置");
        return rule;
    }

    private NumgenRuleEntity cloneHistoricalRuleAsNextVersion(NumgenRuleEntity source, List<NumgenRuleSegmentEntity> sourceSegments) {
        LocalDateTime now = LocalDateTime.now();
        NumgenRuleEntity clonedRule = new NumgenRuleEntity();
        clonedRule.setGenKey(source.getGenKey());
        clonedRule.setRuleName(source.getRuleName());
        clonedRule.setVersion(nextVersion(source.getGenKey(), source.getTenantId()));
        clonedRule.setStatus(1);
        clonedRule.setPublishStatus(0);
        clonedRule.setVersionState(VERSION_STATE_DRAFT);
        clonedRule.setTenantId(source.getTenantId());
        clonedRule.setCreatedAt(now);
        clonedRule.setUpdatedAt(now);
        ruleMapper.insert(clonedRule);

        for (NumgenRuleSegmentEntity sourceSegment : sourceSegments) {
            NumgenRuleSegmentEntity clonedSegment = new NumgenRuleSegmentEntity();
            clonedSegment.setRuleId(clonedRule.getId());
            clonedSegment.setSortOrder(sourceSegment.getSortOrder());
            clonedSegment.setSegmentType(sourceSegment.getSegmentType());
            clonedSegment.setSegmentName(sourceSegment.getSegmentName());
            clonedSegment.setLiteralValue(sourceSegment.getLiteralValue());
            clonedSegment.setVariableKey(sourceSegment.getVariableKey());
            clonedSegment.setDateFormat(sourceSegment.getDateFormat());
            clonedSegment.setSeqWidth(sourceSegment.getSeqWidth());
            clonedSegment.setPadChar(sourceSegment.getPadChar());
            clonedSegment.setSequenceScope(sourceSegment.getSequenceScope());
            clonedSegment.setTenantId(source.getTenantId());
            clonedSegment.setCreatedAt(now);
            clonedSegment.setUpdatedAt(now);
            segmentMapper.insert(clonedSegment);
        }
        return clonedRule;
    }

    private Integer nextVersion(String genKey, String tenantId) {
        return ruleMapper.selectVersionsByGenKey(genKey, tenantId).stream()
                .map(NumgenRuleEntity::getVersion)
                .filter(version -> version != null)
                .max(Integer::compareTo)
                .orElse(0) + 1;
    }

    private NumgenGeneratorEntity selectGenerator(String genKey, String tenantId) {
        return generatorMapper.selectOne(new LambdaQueryWrapper<NumgenGeneratorEntity>()
                .eq(NumgenGeneratorEntity::getGenKey, genKey)
                .eq(NumgenGeneratorEntity::getTenantId, tenantId));
    }
}
