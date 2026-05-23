package io.mango.numgen.api;

import io.mango.common.result.R;
import io.mango.common.vo.PageResult;
import io.mango.numgen.api.query.NumgenHistoryPageQuery;
import io.mango.numgen.api.vo.NumgenHistoryVO;
import jakarta.validation.Valid;
import org.springframework.validation.annotation.Validated;

/**
 * 发号历史 API 契约。
 */
@Validated
public interface NumgenHistoryApi {

    R<PageResult<NumgenHistoryVO>> pageHistories(@Valid NumgenHistoryPageQuery query);
}
