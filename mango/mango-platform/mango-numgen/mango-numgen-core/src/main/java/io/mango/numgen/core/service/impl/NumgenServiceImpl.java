package io.mango.numgen.core.service.impl;

import io.mango.common.result.Require;
import io.mango.infra.kv.api.ICache;
import io.mango.numgen.api.command.NumgenBatchCommand;
import io.mango.numgen.api.command.NumgenNextCommand;
import io.mango.numgen.api.command.NumgenValidateRuleCommand;
import io.mango.numgen.api.vo.NumgenRuleValidationVO;
import io.mango.numgen.core.config.NumgenKvProperties;
import io.mango.numgen.core.entity.NumgenHistory;
import io.mango.numgen.core.entity.NumgenRule;
import io.mango.numgen.core.entity.NumgenRuleSegment;
import io.mango.numgen.core.mapper.NumgenHistoryMapper;
import io.mango.numgen.core.mapper.NumgenRuleMapper;
import io.mango.numgen.core.mapper.NumgenRuleSegmentMapper;
import io.mango.numgen.core.service.INumgenService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@EnableConfigurationProperties(NumgenKvProperties.class)
public class NumgenServiceImpl implements INumgenService {

    private final NumgenRuleMapper ruleMapper;
    private final NumgenRuleSegmentMapper segmentMapper;
    private final NumgenSequenceAllocator sequenceAllocator;
    private final NumgenRuleRenderer ruleRenderer;
    private final NumgenHistoryMapper historyMapper;
    private final ObjectProvider<ICache> cacheProvider;
    private final NumgenKvProperties kvProperties;

    @Override
    @Transactional
    public String nextValue(NumgenNextCommand command) {
        NumgenBatchCommand batchCommand = new NumgenBatchCommand();
        batchCommand.setGenKey(command.getGenKey());
        batchCommand.setCount(1);
        batchCommand.setParams(command.getParams());
        return batchValue(batchCommand).get(0);
    }

    @Override
    @Transactional
    public List<String> batchValue(NumgenBatchCommand command) {
        Require.notNull(command, "生成编号命令不能为空");
        Require.notBlank(command.getGenKey(), "编号规则键不能为空");
        Require.isTrue(command.getCount() > 0, "生成数量必须大于0");
        long startMillis = System.currentTimeMillis();
        Long tenantId = NumgenContextSupport.currentTenantId();
        NumgenRule rule = activeRule(command.getGenKey(), tenantId);
        List<NumgenRuleSegment> segments = segmentMapper.selectByRuleId(rule.getId(), tenantId);
        NumgenRuleValidationVO validation = ruleRenderer.validate(rule, segments);
        Require.isTrue(validation.isValid(), String.join("；", validation.getErrors()));
        Map<String, Object> params = command.getParams();
        NumgenSequenceAllocator.Segment sequenceSegment = sequenceAllocator.allocate(
                rule.getGenKey(),
                rule.getVersion(),
                tenantId,
                command.getCount());

        List<String> values = new ArrayList<>(command.getCount());
        for (long value = sequenceSegment.start(); value <= sequenceSegment.end(); value++) {
            String result = ruleRenderer.render(segments, params, value);
            values.add(result);
            try {
                insertHistory(rule, result, params, System.currentTimeMillis() - startMillis, 1, null);
            } catch (Exception ex) {
                insertHistoryFailure(rule, result, params, ex);
            }
        }
        return values;
    }

    @Override
    public NumgenRuleValidationVO validateRule(NumgenValidateRuleCommand command) {
        Require.notNull(command, "规则校验命令不能为空");
        NumgenRule rule = new NumgenRule();
        rule.setGenKey(command.getGenKey());
        rule.setRuleName(command.getRuleName());
        List<NumgenRuleSegment> segments = command.getSegments().stream().map(segmentCommand -> {
            NumgenRuleSegment segment = new NumgenRuleSegment();
            segment.setId(segmentCommand.getId());
            segment.setRuleId(segmentCommand.getRuleId());
            segment.setSortOrder(segmentCommand.getSortOrder());
            segment.setSegmentType(segmentCommand.getSegmentType());
            segment.setSegmentName(segmentCommand.getSegmentName());
            segment.setLiteralValue(segmentCommand.getLiteralValue());
            segment.setVariableKey(segmentCommand.getVariableKey());
            segment.setDateFormat(segmentCommand.getDateFormat());
            segment.setSeqWidth(segmentCommand.getSeqWidth());
            segment.setPadChar(segmentCommand.getPadChar());
            return segment;
        }).collect(java.util.stream.Collectors.toList());
        return ruleRenderer.validate(rule, segments);
    }

    private NumgenRule activeRule(String genKey, Long tenantId) {
        String cacheKey = "numgen:rule:" + tenantId + ":" + genKey;
        ICache cache = cacheProvider.getIfAvailable();
        if (cache != null) {
            String cached = cache.get(cacheKey);
            if (cached != null && !cached.isBlank()) {
                NumgenRule cachedRule = ruleMapper.selectById(Long.valueOf(cached));
                if (cachedRule != null) {
                    return cachedRule;
                }
            }
        }
        NumgenRule rule = ruleMapper.selectActiveByGenKey(genKey, tenantId);
        Require.notNull(rule, "编号规则不存在或未发布：" + genKey);
        if (cache != null) {
            cache.set(cacheKey, String.valueOf(rule.getId()), kvProperties.getRuleCacheTtlSeconds());
        }
        return rule;
    }

    private void insertHistory(NumgenRule rule, String result, Map<String, Object> params, long costMillis, int status, String errorMessage) {
        NumgenHistory history = new NumgenHistory();
        history.setGenKey(rule.getGenKey());
        history.setRuleId(rule.getId());
        history.setResultNo(result);
        history.setRuleVersion(rule.getVersion());
        history.setBizKey(params == null ? null : String.valueOf(params.getOrDefault("bizKey", "")));
        history.setInputDigest(params == null ? null : Integer.toHexString(params.hashCode()));
        history.setCostMillis(costMillis);
        history.setStatus(status);
        history.setErrorMessage(errorMessage);
        history.setTenantId(rule.getTenantId());
        historyMapper.insert(history);
    }

    private void insertHistoryFailure(NumgenRule rule, String result, Map<String, Object> params, Exception ex) {
        try {
            NumgenHistory history = new NumgenHistory();
            history.setGenKey(rule.getGenKey());
            history.setRuleId(rule.getId());
            history.setResultNo(result);
            history.setRuleVersion(rule.getVersion());
            history.setBizKey(params == null ? null : String.valueOf(params.getOrDefault("bizKey", "")));
            history.setInputDigest(params == null ? null : Integer.toHexString(params.hashCode()));
            history.setStatus(0);
            history.setErrorMessage(ex.getMessage());
            history.setTenantId(rule.getTenantId());
            historyMapper.insert(history);
        } catch (Exception ignore) {
            // 历史不应阻塞主链路
        }
    }
}
