package io.mango.guarantee.core.service;

import io.mango.guarantee.api.command.GuaranteeCaseCommand;
import io.mango.guarantee.api.query.GuaranteeCaseQuery;
import io.mango.guarantee.api.vo.GuaranteeCaseVO;
import io.mango.infra.persistence.api.query.PersistencePageResult;

/**
 * 保函业务单服务。
 */
public interface IGuaranteeCaseService {

    PersistencePageResult<GuaranteeCaseVO> page(GuaranteeCaseQuery query);

    GuaranteeCaseVO get(Long caseId);

    Long create(GuaranteeCaseCommand command);

    Boolean update(GuaranteeCaseCommand command);

    Boolean delete(Long caseId);
}
