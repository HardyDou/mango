package io.mango.numgen.core.service;

import io.mango.common.vo.PageResult;
import io.mango.numgen.api.query.NumgenSequencePageQuery;
import io.mango.numgen.api.vo.NumgenSequenceVO;

/**
 * 编号序列服务。
 */
public interface INumgenSequenceService {

    PageResult<NumgenSequenceVO> pageSequences(NumgenSequencePageQuery query);
}
