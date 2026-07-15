package io.mango.numgen.core.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import io.mango.common.vo.PageResult;
import io.mango.numgen.api.query.NumgenHistoryPageQuery;
import io.mango.numgen.api.vo.NumgenHistoryVO;
import io.mango.numgen.core.entity.NumgenHistoryEntity;
import io.mango.numgen.core.mapper.NumgenHistoryMapper;
import io.mango.numgen.core.service.INumgenHistoryService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.stream.Collectors;

/**
 * 发号历史服务实现。
 */
@Service
@RequiredArgsConstructor
public class NumgenHistoryService implements INumgenHistoryService {

    private final NumgenHistoryMapper historyMapper;

    @Override
    public PageResult<NumgenHistoryVO> pageHistories(NumgenHistoryPageQuery query) {
        NumgenHistoryPageQuery resolved = query == null ? new NumgenHistoryPageQuery() : query;
        IPage<NumgenHistoryEntity> page = historyMapper.selectPage(new Page<>(resolved.getPage(), resolved.getSize()), historyWrapper(resolved));
        List<NumgenHistoryVO> records = page.getRecords().stream().map(this::toHistoryVO).collect(Collectors.toList());
        return PageResult.of(records, page.getTotal(), page.getCurrent(), page.getSize());
    }

    private LambdaQueryWrapper<NumgenHistoryEntity> historyWrapper(NumgenHistoryPageQuery query) {
        return new LambdaQueryWrapper<NumgenHistoryEntity>()
                .eq(StringUtils.hasText(query.getGenKey()), NumgenHistoryEntity::getGenKey, query.getGenKey())
                .like(StringUtils.hasText(query.getResultNo()), NumgenHistoryEntity::getResultNo, query.getResultNo())
                .eq(query.getStatus() != null, NumgenHistoryEntity::getStatus, query.getStatus())
                .eq(query.getRuleVersion() != null, NumgenHistoryEntity::getRuleVersion, query.getRuleVersion())
                .eq(StringUtils.hasText(query.getBizKey()), NumgenHistoryEntity::getBizKey, query.getBizKey())
                .eq(NumgenHistoryEntity::getTenantId, currentTenantId())
                .orderByDesc(NumgenHistoryEntity::getCreatedAt);
    }

    private NumgenHistoryVO toHistoryVO(NumgenHistoryEntity entity) {
        NumgenHistoryVO vo = new NumgenHistoryVO();
        vo.setId(entity.getId());
        vo.setGenKey(entity.getGenKey());
        vo.setRuleId(entity.getRuleId());
        vo.setResultNo(entity.getResultNo());
        vo.setRuleVersion(entity.getRuleVersion());
        vo.setBizKey(entity.getBizKey());
        vo.setInputDigest(entity.getInputDigest());
        vo.setCostMillis(entity.getCostMillis());
        vo.setStatus(entity.getStatus());
        vo.setErrorMessage(entity.getErrorMessage());
        vo.setCreateTime(entity.getCreatedAt());
        return vo;
    }

    private String currentTenantId() {
        return NumgenContextSupport.currentTenantId();
    }
}
