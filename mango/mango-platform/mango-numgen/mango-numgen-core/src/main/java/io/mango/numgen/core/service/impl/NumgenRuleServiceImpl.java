package io.mango.numgen.core.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import io.mango.common.result.R;
import io.mango.common.result.Require;
import io.mango.common.vo.PageResult;
import io.mango.infra.kv.api.ICache;
import io.mango.numgen.api.command.NumgenPreviewCommand;
import io.mango.numgen.api.command.NumgenPublishCommand;
import io.mango.numgen.api.command.SaveNumgenRuleCommand;
import io.mango.numgen.api.command.UpdateNumgenRuleStatusCommand;
import io.mango.numgen.api.query.NumgenRulePageQuery;
import io.mango.numgen.api.vo.NumgenPreviewVO;
import io.mango.numgen.api.vo.NumgenRuleVO;
import io.mango.numgen.core.config.NumgenKvProperties;
import io.mango.numgen.core.entity.NumgenGenerator;
import io.mango.numgen.core.entity.NumgenRule;
import io.mango.numgen.core.entity.NumgenRuleSegment;
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
public class NumgenRuleServiceImpl implements INumgenRuleService {

    private final NumgenRuleMapper ruleMapper;
    private final NumgenGeneratorMapper generatorMapper;
    private final NumgenRuleSegmentMapper segmentMapper;
    private final NumgenRuleRenderer ruleRenderer;
    private final ObjectProvider<ICache> cacheProvider;

    private static final String VERSION_STATE_DRAFT = "DRAFT";
    private static final String VERSION_STATE_ACTIVE = "ACTIVE";
    private static final String VERSION_STATE_HISTORY = "HISTORY";

    @Override
    public R<PageResult<NumgenRuleVO>> pageRules(NumgenRulePageQuery query) {
        NumgenRulePageQuery resolved = query == null ? new NumgenRulePageQuery() : query;
        IPage<NumgenRule> page = ruleMapper.selectPage(new Page<>(resolved.getPage(), resolved.getSize()), wrapper(resolved));
        List<NumgenRuleVO> records = page.getRecords().stream().map(this::toRuleVO).collect(Collectors.toList());
        return R.ok(PageResult.of(records, page.getTotal(), page.getCurrent(), page.getSize()));
    }

    @Override
    public R<NumgenRuleVO> detailRule(Long id) {
        return R.ok(toRuleVO(selectRuleRequired(id)));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public R<Long> createRule(SaveNumgenRuleCommand command) {
        Require.notNull(command, "编号规则不能为空");
        validate(command, false);
        Long tenantId = NumgenContextSupport.currentTenantId();
        NumgenGenerator generator = selectGeneratorRequired(command.getGenKey(), tenantId);
        NumgenRule entity = new NumgenRule();
        copy(command, entity);
        entity.setGenKey(generator.getGenKey());
        entity.setTenantId(tenantId);
        entity.setPublishStatus(0);
        entity.setVersionState(VERSION_STATE_DRAFT);
        ruleMapper.insert(entity);
        return R.ok(entity.getId());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public R<Boolean> updateRule(SaveNumgenRuleCommand command) {
        Require.notNull(command, "编号规则不能为空");
        Require.notNull(command.getId(), "编号规则 ID 不能为空");
        validate(command, true);
        NumgenRule entity = selectRuleRequired(command.getId());
        Require.isTrue(VERSION_STATE_DRAFT.equals(entity.getVersionState()), "只有草稿规则可以编辑");
        selectGeneratorRequired(command.getGenKey(), entity.getTenantId());
        copy(command, entity);
        entity.setPublishStatus(0);
        entity.setVersionState(VERSION_STATE_DRAFT);
        return R.ok(ruleMapper.updateById(entity) > 0);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public R<Boolean> updateRuleStatus(UpdateNumgenRuleStatusCommand command) {
        Require.notNull(command, "状态命令不能为空");
        Require.notNull(command.getId(), "编号规则 ID 不能为空");
        Require.notNull(command.getStatus(), "状态不能为空");
        NumgenRule entity = selectRuleRequired(command.getId());
        entity.setStatus(command.getStatus());
        evictRuleCache(entity.getTenantId(), entity.getGenKey());
        return R.ok(ruleMapper.updateById(entity) > 0);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public R<Boolean> deleteRule(Long id) {
        NumgenRule entity = selectRuleRequired(id);
        Require.isTrue(VERSION_STATE_DRAFT.equals(entity.getVersionState()), "只有草稿规则可以删除");
        return R.ok(ruleMapper.deleteById(id) > 0);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public R<Boolean> publishRule(NumgenPublishCommand command) {
        Require.notNull(command, "发布命令不能为空");
        NumgenRule selectedRule = selectPublishRule(command);
        List<NumgenRuleSegment> selectedSegments = segmentMapper.selectByRuleId(selectedRule.getId(), selectedRule.getTenantId());
        Require.isTrue(ruleRenderer.validate(selectedRule, selectedSegments).isValid(), "规则片段配置不完整，不能发布");
        Require.isTrue(selectedRule.getStatus() == null || selectedRule.getStatus() == 1, "停用规则不能发布");

        NumgenRule rule = selectedRule;
        if (VERSION_STATE_HISTORY.equals(selectedRule.getVersionState())) {
            rule = cloneHistoricalRuleAsNextVersion(selectedRule, selectedSegments);
        }

        List<NumgenRule> versions = ruleMapper.selectVersionsByGenKey(rule.getGenKey(), rule.getTenantId());
        for (NumgenRule version : versions) {
            if (!version.getId().equals(rule.getId())) {
                version.setPublishStatus(0);
                version.setVersionState(VERSION_STATE_HISTORY);
                ruleMapper.updateById(version);
            }
        }
        rule.setPublishStatus(1);
        rule.setVersionState(VERSION_STATE_ACTIVE);
        ruleMapper.updateById(rule);

        NumgenGenerator generator = selectGeneratorRequired(rule.getGenKey(), rule.getTenantId());
        generator.setCurrentRuleVersion(rule.getVersion());
        generator.setCurrentPublishStatus(rule.getPublishStatus());
        generatorMapper.updateById(generator);
        evictRuleCache(rule.getTenantId(), rule.getGenKey());
        return R.ok(Boolean.TRUE);
    }

    @Override
    public R<NumgenPreviewVO> previewRule(NumgenPreviewCommand command) {
        Require.notNull(command, "预览命令不能为空");
        Require.notBlank(command.getGenKey(), "规则键不能为空");
        NumgenRule rule = ruleMapper.selectActiveByGenKey(command.getGenKey(), NumgenContextSupport.currentTenantId());
        Require.notNull(rule, "编号规则不存在或未发布：" + command.getGenKey());
        List<NumgenRuleSegment> segments = segmentMapper.selectByRuleId(rule.getId(), rule.getTenantId());
        return R.ok(ruleRenderer.preview(rule, segments, command.getParams(), command.getCount()));
    }

    private LambdaQueryWrapper<NumgenRule> wrapper(NumgenRulePageQuery query) {
        String keyword = NumgenContextSupport.trimToNull(query.getKeyword());
        return new LambdaQueryWrapper<NumgenRule>()
                .eq(StringUtils.hasText(query.getGenKey()), NumgenRule::getGenKey, query.getGenKey())
                .and(StringUtils.hasText(keyword), nested -> nested
                        .like(NumgenRule::getRuleName, keyword)
                        .or()
                        .like(NumgenRule::getGenKey, keyword))
                .eq(query.getStatus() != null, NumgenRule::getStatus, query.getStatus())
                .eq(query.getPublishStatus() != null, NumgenRule::getPublishStatus, query.getPublishStatus())
                .eq(NumgenRule::getTenantId, NumgenContextSupport.currentTenantId())
                .orderByDesc(NumgenRule::getUpdateTime);
    }

    private void validate(SaveNumgenRuleCommand command, boolean update) {
        if (update) {
            Require.notNull(command.getId(), "编号规则 ID 不能为空");
        }
        Require.notBlank(command.getGenKey(), "规则键不能为空");
        Require.notBlank(command.getRuleName(), "规则名称不能为空");
    }

    private NumgenRule selectRuleRequired(Long id) {
        Require.notNull(id, "编号规则 ID 不能为空");
        NumgenRule entity = ruleMapper.selectById(id);
        Require.notNull(entity, "编号规则不存在");
        Require.isTrue(NumgenContextSupport.currentTenantId().equals(entity.getTenantId()), "编号规则不存在");
        return entity;
    }

    private NumgenGenerator selectGeneratorRequired(String genKey, Long tenantId) {
        Require.notBlank(genKey, "业务 Key 不能为空");
        NumgenGenerator generator = selectGenerator(genKey.trim(), tenantId);
        Require.notNull(generator, "编号生成器不存在：" + genKey);
        Require.isTrue(generator.getStatus() == null || generator.getStatus() == 1, "编号生成器已停用：" + genKey);
        return generator;
    }

    private void copy(SaveNumgenRuleCommand command, NumgenRule entity) {
        entity.setGenKey(command.getGenKey().trim());
        entity.setRuleName(command.getRuleName().trim());
        entity.setVersion(command.getVersion() == null ? 1 : command.getVersion());
        entity.setStatus(command.getStatus() == null ? 1 : command.getStatus());
    }

    private NumgenRuleVO toRuleVO(NumgenRule entity) {
        NumgenRuleVO vo = new NumgenRuleVO();
        vo.setId(entity.getId());
        vo.setGenKey(entity.getGenKey());
        NumgenGenerator generator = selectGenerator(entity.getGenKey(), entity.getTenantId());
        vo.setGenName(generator == null ? null : generator.getGenName());
        vo.setRuleName(entity.getRuleName());
        vo.setVersion(entity.getVersion());
        vo.setStatus(entity.getStatus());
        vo.setPublishStatus(entity.getPublishStatus());
        vo.setVersionState(entity.getVersionState());
        vo.setCreateTime(entity.getCreateTime());
        vo.setUpdateTime(entity.getUpdateTime());
        return vo;
    }

    private void evictRuleCache(Long tenantId, String genKey) {
        ICache cache = cacheProvider.getIfAvailable();
        if (cache != null) {
            cache.delete("numgen:rule:" + tenantId + ":" + genKey);
        }
    }

    private NumgenRule selectPublishRule(NumgenPublishCommand command) {
        if (command.getRuleId() != null) {
            return selectRuleRequired(command.getRuleId());
        }
        Require.notBlank(command.getGenKey(), "业务 Key 不能为空");
        Long tenantId = NumgenContextSupport.currentTenantId();
        NumgenGenerator generator = selectGeneratorRequired(command.getGenKey(), tenantId);
        NumgenRule rule = ruleMapper.selectLatestDraftByGenKey(generator.getGenKey(), tenantId);
        Require.notNull(rule, "没有可发布的规则，请先保存规则配置");
        return rule;
    }

    private NumgenRule cloneHistoricalRuleAsNextVersion(NumgenRule source, List<NumgenRuleSegment> sourceSegments) {
        LocalDateTime now = LocalDateTime.now();
        NumgenRule clonedRule = new NumgenRule();
        clonedRule.setGenKey(source.getGenKey());
        clonedRule.setRuleName(source.getRuleName());
        clonedRule.setVersion(nextVersion(source.getGenKey(), source.getTenantId()));
        clonedRule.setStatus(1);
        clonedRule.setPublishStatus(0);
        clonedRule.setVersionState(VERSION_STATE_DRAFT);
        clonedRule.setTenantId(source.getTenantId());
        clonedRule.setCreateTime(now);
        clonedRule.setUpdateTime(now);
        ruleMapper.insert(clonedRule);

        for (NumgenRuleSegment sourceSegment : sourceSegments) {
            NumgenRuleSegment clonedSegment = new NumgenRuleSegment();
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
            clonedSegment.setCreateTime(now);
            clonedSegment.setUpdateTime(now);
            segmentMapper.insert(clonedSegment);
        }
        return clonedRule;
    }

    private Integer nextVersion(String genKey, Long tenantId) {
        return ruleMapper.selectVersionsByGenKey(genKey, tenantId).stream()
                .map(NumgenRule::getVersion)
                .filter(version -> version != null)
                .max(Integer::compareTo)
                .orElse(0) + 1;
    }

    private NumgenGenerator selectGenerator(String genKey, Long tenantId) {
        return generatorMapper.selectOne(new LambdaQueryWrapper<NumgenGenerator>()
                .eq(NumgenGenerator::getGenKey, genKey)
                .eq(NumgenGenerator::getTenantId, tenantId));
    }
}
