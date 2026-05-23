package io.mango.numgen.api;

import io.mango.common.result.R;
import io.mango.common.vo.PageResult;
import io.mango.numgen.api.query.NumgenSequencePageQuery;
import io.mango.numgen.api.vo.NumgenSequenceVO;
import jakarta.validation.Valid;
import org.springframework.validation.annotation.Validated;

/**
 * 编号序列 API 契约。
 */
@Validated
public interface NumgenSequenceApi {

    R<PageResult<NumgenSequenceVO>> pageSequences(@Valid NumgenSequencePageQuery query);
}
