package io.mango.numgen.api;

import io.mango.common.result.R;
import io.mango.common.vo.PageResult;
import io.mango.numgen.api.command.SaveNumgenRuleSegmentCommand;
import io.mango.numgen.api.query.NumgenSegmentPageQuery;
import io.mango.numgen.api.vo.NumgenRuleSegmentVO;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import org.springframework.validation.annotation.Validated;

@Validated
public interface NumgenSegmentApi {

    R<PageResult<NumgenRuleSegmentVO>> pageSegments(@Valid NumgenSegmentPageQuery query);

    R<NumgenRuleSegmentVO> detailSegment(@NotNull(message = "编号规则片段 ID 不能为空") Long id);

    R<Long> createSegment(@Valid SaveNumgenRuleSegmentCommand command);

    R<Boolean> updateSegment(@Valid SaveNumgenRuleSegmentCommand command);

    R<Boolean> deleteSegment(@NotNull(message = "编号规则片段 ID 不能为空") Long id);
}
