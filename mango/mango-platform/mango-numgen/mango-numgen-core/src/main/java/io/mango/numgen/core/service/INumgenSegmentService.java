package io.mango.numgen.core.service;

import io.mango.common.result.R;
import io.mango.common.vo.PageResult;
import io.mango.numgen.api.command.SaveNumgenRuleSegmentCommand;
import io.mango.numgen.api.query.NumgenSegmentPageQuery;
import io.mango.numgen.api.vo.NumgenRuleSegmentVO;

public interface INumgenSegmentService {

    R<PageResult<NumgenRuleSegmentVO>> pageSegments(NumgenSegmentPageQuery query);

    R<NumgenRuleSegmentVO> detailSegment(Long id);

    R<Long> createSegment(SaveNumgenRuleSegmentCommand command);

    R<Boolean> updateSegment(SaveNumgenRuleSegmentCommand command);

    R<Boolean> deleteSegment(Long id);
}
