package io.mango.domain.api;

import io.mango.common.result.R;
import io.mango.common.vo.PageResult;
import io.mango.domain.api.command.CreateDomainCommand;
import io.mango.domain.api.command.UpdateDomainCommand;
import io.mango.domain.api.command.UpdateDomainStatusCommand;
import io.mango.domain.api.query.DomainPageQuery;
import io.mango.domain.api.vo.DomainVO;
import jakarta.validation.Valid;

import java.util.List;

/**
 * 业务域 API 契约。
 */
public interface DomainApi {

    /** 分页查询业务域。 */
    R<PageResult<DomainVO>> page(DomainPageQuery query);

    /** 查询业务域树。 */
    R<List<DomainVO>> tree(DomainPageQuery query);

    /** 查询启用业务域树。 */
    R<List<DomainVO>> enabledTree();

    /** 查询业务域详情。 */
    R<DomainVO> detail(Long id);

    /** 根据编码查询业务域。 */
    R<DomainVO> detailByCode(String domainCode);

    /** 新增业务域。 */
    R<Long> create(@Valid CreateDomainCommand command);

    /** 修改业务域。 */
    R<Boolean> update(@Valid UpdateDomainCommand command);

    /** 启停业务域。 */
    R<Boolean> updateStatus(@Valid UpdateDomainStatusCommand command);

    /** 逻辑删除业务域。 */
    R<Boolean> delete(Long id);
}
