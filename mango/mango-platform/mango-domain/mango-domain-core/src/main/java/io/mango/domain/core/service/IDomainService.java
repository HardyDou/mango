package io.mango.domain.core.service;

import io.mango.common.result.R;
import io.mango.common.vo.PageResult;
import io.mango.domain.api.command.CreateDomainCommand;
import io.mango.domain.api.command.UpdateDomainCommand;
import io.mango.domain.api.command.UpdateDomainStatusCommand;
import io.mango.domain.api.query.DomainPageQuery;
import io.mango.domain.api.vo.DomainVO;

import java.util.List;

/**
 * 业务域服务。
 */
public interface IDomainService {

    R<PageResult<DomainVO>> page(DomainPageQuery query);

    R<List<DomainVO>> tree(DomainPageQuery query);

    R<List<DomainVO>> enabledTree();

    R<DomainVO> detail(Long id);

    R<DomainVO> detailByCode(String domainCode);

    R<Long> create(CreateDomainCommand command);

    R<Boolean> update(UpdateDomainCommand command);

    R<Boolean> updateStatus(UpdateDomainStatusCommand command);

    R<Boolean> delete(Long id);
}
