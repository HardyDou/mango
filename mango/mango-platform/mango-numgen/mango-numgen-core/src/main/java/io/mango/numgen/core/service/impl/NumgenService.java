package io.mango.numgen.core.service.impl;

import io.mango.common.result.Require;
import io.mango.infra.kv.api.ICache;
import io.mango.numgen.api.command.NumgenBatchCommand;
import io.mango.numgen.api.command.NumgenNextCommand;
import io.mango.numgen.api.command.NumgenValidateRuleCommand;
import io.mango.numgen.api.enums.NumgenCode;
import io.mango.numgen.api.vo.NumgenRuleValidationVO;
import io.mango.numgen.core.config.NumgenKvProperties;
import io.mango.numgen.core.entity.NumgenHistoryEntity;
import io.mango.numgen.core.entity.NumgenRuleEntity;
import io.mango.numgen.core.entity.NumgenRuleSegmentEntity;
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
import java.util.logging.Level;
import java.util.logging.Logger;

@Service
@RequiredArgsConstructor
@EnableConfigurationProperties(NumgenKvProperties.class)
public class NumgenService implements INumgenService {

    private static final int ERROR_MESSAGE_MAX_LENGTH = 512;

    private static final Logger LOGGER = Logger.getLogger(NumgenService.class.getName());

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
        Require.notNull(command, NumgenCode.NUMGEN_INVALID, "生成编号命令不能为空");
        NumgenBatchCommand batchCommand = new NumgenBatchCommand();
        batchCommand.setGenKey(command.getGenKey());
        batchCommand.setCount(1);
        batchCommand.setParams(command.getParams());
        return batchValue(batchCommand).get(0);
    }

    @Override
    @Transactional
    public List<String> batchValue(NumgenBatchCommand command) {
        Require.notNull(command, NumgenCode.NUMGEN_INVALID, "生成编号命令不能为空");
        Require.notBlank(command.getGenKey(), NumgenCode.NUMGEN_INVALID, "编号规则键不能为空");
        Require.notNull(command.getCount(), NumgenCode.NUMGEN_INVALID, "生成数量不能为空");
        Require.isTrue(command.getCount() > 0, NumgenCode.NUMGEN_INVALID, "生成数量必须大于0");
        long startMillis = System.currentTimeMillis();
        String tenantId = NumgenContextSupport.currentTenantId();
        NumgenRuleEntity rule = activeRule(command.getGenKey(), tenantId);
        List<NumgenRuleSegmentEntity> segments = segmentMapper.selectByRuleId(rule.getId(), tenantId);
        NumgenRuleValidationVO validation = ruleRenderer.validate(rule, segments);
        Require.isTrue(validation.isValid(), NumgenCode.NUMGEN_SEGMENT_INVALID,
                String.join("；", validation.getErrors()));
        Map<String, Object> params = command.getParams();
        String scopeKey = ruleRenderer.sequenceScopeKey(segments, params);
        NumgenSequenceAllocator.Segment sequenceSegment = sequenceAllocator.allocate(
                rule.getGenKey(),
                rule.getVersion(),
                scopeKey,
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
        Require.notNull(command, NumgenCode.NUMGEN_INVALID, "规则校验命令不能为空");
        Require.notNull(command.getSegments(), NumgenCode.NUMGEN_INVALID, "规则片段不能为空");
        NumgenRuleEntity rule = new NumgenRuleEntity();
        rule.setGenKey(command.getGenKey());
        rule.setRuleName(command.getRuleName());
        List<NumgenRuleSegmentEntity> segments = command.getSegments().stream().map(segmentCommand -> {
            NumgenRuleSegmentEntity segment = new NumgenRuleSegmentEntity();
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
            segment.setSequenceScope(segmentCommand.getSequenceScope());
            return segment;
        }).collect(java.util.stream.Collectors.toList());
        return ruleRenderer.validate(rule, segments);
    }

    private NumgenRuleEntity activeRule(String genKey, String tenantId) {
        String cacheKey = "numgen:rule:" + tenantId + ":" + genKey;
        ICache cache = cacheProvider.getIfAvailable();
        if (cache != null) {
            String cached = cache.get(cacheKey);
            if (cached != null && !cached.isBlank()) {
                NumgenRuleEntity cachedRule = ruleMapper.selectById(Long.valueOf(cached));
                if (cachedRule != null) {
                    return cachedRule;
                }
            }
        }
        NumgenRuleEntity rule = ruleMapper.selectActiveByGenKey(genKey, tenantId);
        Require.notNull(rule, NumgenCode.NUMGEN_ACTIVE_RULE_NOT_FOUND, "编号规则不存在或未发布：" + genKey);
        if (cache != null) {
            cache.set(cacheKey, String.valueOf(rule.getId()), kvProperties.getRuleCacheTtlSeconds());
        }
        return rule;
    }

    private void insertHistory(NumgenRuleEntity rule, String result, Map<String, Object> params, long costMillis,
                               int status, String errorMessage) {
        NumgenHistoryEntity history = new NumgenHistoryEntity();
        history.setGenKey(rule.getGenKey());
        history.setRuleId(rule.getId());
        history.setResultNo(result);
        history.setRuleVersion(rule.getVersion());
        history.setBizKey(params == null ? null : String.valueOf(params.getOrDefault("bizKey", "")));
        history.setInputDigest(params == null ? null : Integer.toHexString(params.hashCode()));
        history.setCostMillis(costMillis);
        history.setStatus(status);
        history.setErrorMessage(limitErrorMessage(errorMessage));
        history.setTenantId(rule.getTenantId());
        historyMapper.insert(history);
    }

    private void insertHistoryFailure(NumgenRuleEntity rule, String result, Map<String, Object> params, Exception ex) {
        try {
            NumgenHistoryEntity history = new NumgenHistoryEntity();
            history.setGenKey(rule.getGenKey());
            history.setRuleId(rule.getId());
            history.setResultNo(result);
            history.setRuleVersion(rule.getVersion());
            history.setBizKey(params == null ? null : String.valueOf(params.getOrDefault("bizKey", "")));
            history.setInputDigest(params == null ? null : Integer.toHexString(params.hashCode()));
            history.setStatus(0);
            history.setErrorMessage(limitErrorMessage(ex.getMessage()));
            history.setTenantId(rule.getTenantId());
            historyMapper.insert(history);
        } catch (Exception historyEx) {
            LOGGER.log(Level.WARNING,
                    "编号生成成功但失败历史写入失败，genKey=" + rule.getGenKey() + ", ruleId=" + rule.getId(),
                    historyEx);
        }
    }

    private String limitErrorMessage(String errorMessage) {
        if (errorMessage == null || errorMessage.length() <= ERROR_MESSAGE_MAX_LENGTH) {
            return errorMessage;
        }
        return errorMessage.substring(0, ERROR_MESSAGE_MAX_LENGTH);
    }
}
