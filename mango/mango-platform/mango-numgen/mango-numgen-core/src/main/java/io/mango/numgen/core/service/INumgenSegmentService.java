package io.mango.numgen.core.service;

import io.mango.common.vo.PageResult;
import io.mango.numgen.api.command.SaveNumgenRuleSegmentCommand;
import io.mango.numgen.api.query.NumgenSegmentPageQuery;
import io.mango.numgen.api.vo.NumgenRuleSegmentVO;

public interface INumgenSegmentService {

    PageResult<NumgenRuleSegmentVO> pageSegments(NumgenSegmentPageQuery query);

    NumgenRuleSegmentVO detailSegment(Long id);

    Long createSegment(SaveNumgenRuleSegmentCommand command);

    Boolean updateSegment(SaveNumgenRuleSegmentCommand command);

    Boolean deleteSegment(Long id);
}
