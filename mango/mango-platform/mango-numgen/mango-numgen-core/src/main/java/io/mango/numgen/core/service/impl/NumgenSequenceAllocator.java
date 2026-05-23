package io.mango.numgen.core.service.impl;

import com.baomidou.mybatisplus.core.toolkit.IdWorker;
import io.mango.common.result.Require;
import io.mango.infra.kv.api.ILocker;
import io.mango.numgen.core.config.NumgenKvProperties;
import io.mango.numgen.core.entity.NumgenSequence;
import io.mango.numgen.core.mapper.NumgenSequenceMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class NumgenSequenceAllocator {

    private static final int MAX_RETRY = 8;

    private final NumgenSequenceMapper sequenceMapper;
    private final ObjectProvider<ILocker> lockerProvider;
    private final NumgenKvProperties kvProperties;

    public Segment allocate(String genKey, Integer ruleVersion, Long tenantId, int count) {
        ILocker locker = lockerProvider.getIfAvailable();
        if (locker == null) {
            return allocateWithOptimisticLock(genKey, ruleVersion, tenantId, count);
        }
        String lockKey = "numgen:sequence:" + tenantId + ":" + genKey + ":" + ruleVersion;
        Require.isTrue(locker.tryLock(lockKey, kvProperties.getAllocationLockTtlSeconds()), 409, "编号序列分配繁忙，请重试");
        try {
            return allocateWithOptimisticLock(genKey, ruleVersion, tenantId, count);
        } finally {
            locker.unlock(lockKey);
        }
    }

    private Segment allocateWithOptimisticLock(String genKey, Integer ruleVersion, Long tenantId, int count) {
        ensureSequence(genKey, ruleVersion, tenantId);
        for (int i = 0; i < MAX_RETRY; i++) {
            NumgenSequence sequence = sequenceMapper.selectByRule(genKey, ruleVersion, tenantId);
            long start = sequence.getCurrentValue() + 1;
            long end = sequence.getCurrentValue() + count;
            int updated = sequenceMapper.allocateSegment(sequence.getId(), sequence.getVersion(), count);
            if (updated == 1) {
                return new Segment(start, end);
            }
        }
        return Require.fail(409, "编号序列分配冲突，请重试");
    }

    private void ensureSequence(String genKey, Integer ruleVersion, Long tenantId) {
        NumgenSequence sequence = new NumgenSequence();
        sequence.setId(IdWorker.getId());
        sequence.setGenKey(genKey);
        sequence.setRuleVersion(ruleVersion);
        sequence.setCurrentValue(0L);
        sequence.setTenantId(tenantId);
        sequenceMapper.insertIgnore(sequence);
    }

    public record Segment(long start, long end) {
    }
}
