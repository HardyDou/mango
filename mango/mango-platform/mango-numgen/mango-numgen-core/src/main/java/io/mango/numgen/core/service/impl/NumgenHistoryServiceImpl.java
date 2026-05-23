package io.mango.numgen.core.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import io.mango.common.result.R;
import io.mango.common.vo.PageResult;
import io.mango.numgen.api.query.NumgenHistoryPageQuery;
import io.mango.numgen.api.vo.NumgenHistoryVO;
import io.mango.numgen.core.entity.NumgenHistory;
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
public class NumgenHistoryServiceImpl implements INumgenHistoryService {

    private final NumgenHistoryMapper historyMapper;

    @Override
    public R<PageResult<NumgenHistoryVO>> pageHistories(NumgenHistoryPageQuery query) {
        NumgenHistoryPageQuery resolved = query == null ? new NumgenHistoryPageQuery() : query;
        IPage<NumgenHistory> page = historyMapper.selectPage(new Page<>(resolved.getPage(), resolved.getSize()), historyWrapper(resolved));
        List<NumgenHistoryVO> records = page.getRecords().stream().map(this::toHistoryVO).collect(Collectors.toList());
        return R.ok(PageResult.of(records, page.getTotal(), page.getCurrent(), page.getSize()));
    }

    private LambdaQueryWrapper<NumgenHistory> historyWrapper(NumgenHistoryPageQuery query) {
        return new LambdaQueryWrapper<NumgenHistory>()
                .eq(StringUtils.hasText(query.getGenKey()), NumgenHistory::getGenKey, query.getGenKey())
                .like(StringUtils.hasText(query.getResultNo()), NumgenHistory::getResultNo, query.getResultNo())
                .eq(query.getStatus() != null, NumgenHistory::getStatus, query.getStatus())
                .eq(query.getRuleVersion() != null, NumgenHistory::getRuleVersion, query.getRuleVersion())
                .eq(StringUtils.hasText(query.getBizKey()), NumgenHistory::getBizKey, query.getBizKey())
                .eq(NumgenHistory::getTenantId, currentTenantId())
                .orderByDesc(NumgenHistory::getCreateTime);
    }

    private NumgenHistoryVO toHistoryVO(NumgenHistory entity) {
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
        vo.setCreateTime(entity.getCreateTime());
        return vo;
    }

    private Long currentTenantId() {
        return NumgenContextSupport.currentTenantId();
    }
}
