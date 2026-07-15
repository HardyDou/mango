package io.mango.numgen.core.service.impl;

import com.baomidou.mybatisplus.core.toolkit.IdWorker;
import io.mango.common.result.Require;
import io.mango.numgen.api.enums.NumgenCode;
import io.mango.numgen.core.entity.NumgenSequenceEntity;
import io.mango.numgen.core.mapper.NumgenSequenceMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
public class NumgenSequenceAllocator {

    private final NumgenSequenceMapper sequenceMapper;

    @Transactional(rollbackFor = Exception.class)
    public Segment allocate(String genKey, Integer ruleVersion, String scopeKey, String tenantId, int count) {
        Require.isTrue(count > 0, NumgenCode.NUMGEN_SEQUENCE_ALLOCATE_FAILED,
                "编号序列分配数量必须大于0");
        String resolvedScopeKey = scopeKey == null || scopeKey.isBlank() ? "GLOBAL" : scopeKey;
        NumgenSequenceEntity sequence = newSequence(genKey, ruleVersion, resolvedScopeKey, tenantId);
        Require.isTrue(sequenceMapper.upsertAndAllocate(sequence, count) > 0,
                NumgenCode.NUMGEN_SEQUENCE_ALLOCATE_FAILED, "编号序列分配冲突，请重试");
        NumgenSequenceEntity allocated = sequenceMapper.selectByScope(genKey, resolvedScopeKey, tenantId);
        Require.notNull(allocated, NumgenCode.NUMGEN_SEQUENCE_ALLOCATE_FAILED,
                "编号序列初始化失败：" + genKey + ":" + resolvedScopeKey);
        long end = allocated.getCurrentValue();
        long start = end - count + 1;
        return new Segment(start, end);
    }

    private NumgenSequenceEntity newSequence(String genKey, Integer ruleVersion, String scopeKey, String tenantId) {
        NumgenSequenceEntity sequence = new NumgenSequenceEntity();
        sequence.setId(IdWorker.getId());
        sequence.setGenKey(genKey);
        sequence.setRuleVersion(ruleVersion);
        sequence.setScopeKey(scopeKey);
        sequence.setCurrentValue(0L);
        sequence.setTenantId(tenantId);
        return sequence;
    }

    public record Segment(long start, long end) {
    }
}
