package io.mango.numgen.core.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import io.mango.common.vo.PageResult;
import io.mango.numgen.api.query.NumgenSequencePageQuery;
import io.mango.numgen.api.vo.NumgenSequenceVO;
import io.mango.numgen.core.entity.NumgenSequenceEntity;
import io.mango.numgen.core.mapper.NumgenSequenceMapper;
import io.mango.numgen.core.service.INumgenSequenceService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

/**
 * 编号序列服务实现。
 */
@Service
@RequiredArgsConstructor
public class NumgenSequenceService implements INumgenSequenceService {

    private final NumgenSequenceMapper sequenceMapper;

    @Override
    public PageResult<NumgenSequenceVO> pageSequences(NumgenSequencePageQuery query) {
        NumgenSequencePageQuery resolved = query == null ? new NumgenSequencePageQuery() : query;
        IPage<NumgenSequenceEntity> page = sequenceMapper.selectPage(new Page<>(resolved.getPage(), resolved.getSize()), sequenceWrapper(resolved));
        List<NumgenSequenceVO> records = page.getRecords().stream().map(this::toSequenceVO).collect(Collectors.toList());
        return PageResult.of(records, page.getTotal(), page.getCurrent(), page.getSize());
    }

    private LambdaQueryWrapper<NumgenSequenceEntity> sequenceWrapper(NumgenSequencePageQuery query) {
        return new LambdaQueryWrapper<NumgenSequenceEntity>()
                .eq(query.getGenKey() != null && !query.getGenKey().isBlank(), NumgenSequenceEntity::getGenKey, query.getGenKey())
                .eq(query.getRuleVersion() != null, NumgenSequenceEntity::getRuleVersion, query.getRuleVersion())
                .eq(query.getScopeKey() != null && !query.getScopeKey().isBlank(), NumgenSequenceEntity::getScopeKey, query.getScopeKey())
                .eq(NumgenSequenceEntity::getTenantId, currentTenantId())
                .orderByDesc(NumgenSequenceEntity::getUpdatedAt);
    }

    private NumgenSequenceVO toSequenceVO(NumgenSequenceEntity entity) {
        NumgenSequenceVO vo = new NumgenSequenceVO();
        vo.setId(entity.getId());
        vo.setGenKey(entity.getGenKey());
        vo.setRuleVersion(entity.getRuleVersion());
        vo.setScopeKey(entity.getScopeKey());
        vo.setCurrentValue(entity.getCurrentValue());
        vo.setVersion(entity.getVersion());
        vo.setCreateTime(entity.getCreatedAt());
        vo.setUpdateTime(entity.getUpdatedAt());
        return vo;
    }

    private String currentTenantId() {
        return NumgenContextSupport.currentTenantId();
    }
}
