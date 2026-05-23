package io.mango.numgen.core.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import io.mango.common.result.R;
import io.mango.common.vo.PageResult;
import io.mango.numgen.api.query.NumgenSequencePageQuery;
import io.mango.numgen.api.vo.NumgenSequenceVO;
import io.mango.numgen.core.entity.NumgenSequence;
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
public class NumgenSequenceServiceImpl implements INumgenSequenceService {

    private final NumgenSequenceMapper sequenceMapper;

    @Override
    public R<PageResult<NumgenSequenceVO>> pageSequences(NumgenSequencePageQuery query) {
        NumgenSequencePageQuery resolved = query == null ? new NumgenSequencePageQuery() : query;
        IPage<NumgenSequence> page = sequenceMapper.selectPage(new Page<>(resolved.getPage(), resolved.getSize()), sequenceWrapper(resolved));
        List<NumgenSequenceVO> records = page.getRecords().stream().map(this::toSequenceVO).collect(Collectors.toList());
        return R.ok(PageResult.of(records, page.getTotal(), page.getCurrent(), page.getSize()));
    }

    private LambdaQueryWrapper<NumgenSequence> sequenceWrapper(NumgenSequencePageQuery query) {
        return new LambdaQueryWrapper<NumgenSequence>()
                .eq(query.getGenKey() != null && !query.getGenKey().isBlank(), NumgenSequence::getGenKey, query.getGenKey())
                .eq(query.getRuleVersion() != null, NumgenSequence::getRuleVersion, query.getRuleVersion())
                .eq(query.getScopeKey() != null && !query.getScopeKey().isBlank(), NumgenSequence::getScopeKey, query.getScopeKey())
                .eq(NumgenSequence::getTenantId, currentTenantId())
                .orderByDesc(NumgenSequence::getUpdateTime);
    }

    private NumgenSequenceVO toSequenceVO(NumgenSequence entity) {
        NumgenSequenceVO vo = new NumgenSequenceVO();
        vo.setId(entity.getId());
        vo.setGenKey(entity.getGenKey());
        vo.setRuleVersion(entity.getRuleVersion());
        vo.setScopeKey(entity.getScopeKey());
        vo.setCurrentValue(entity.getCurrentValue());
        vo.setVersion(entity.getVersion());
        vo.setCreateTime(entity.getCreateTime());
        vo.setUpdateTime(entity.getUpdateTime());
        return vo;
    }

    private Long currentTenantId() {
        return NumgenContextSupport.currentTenantId();
    }
}
