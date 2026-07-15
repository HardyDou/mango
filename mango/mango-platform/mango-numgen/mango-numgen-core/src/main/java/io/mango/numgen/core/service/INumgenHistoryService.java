package io.mango.numgen.core.service;

import io.mango.common.vo.PageResult;
import io.mango.numgen.api.query.NumgenHistoryPageQuery;
import io.mango.numgen.api.vo.NumgenHistoryVO;

/**
 * 发号历史服务。
 */
public interface INumgenHistoryService {

    PageResult<NumgenHistoryVO> pageHistories(NumgenHistoryPageQuery query);
}
