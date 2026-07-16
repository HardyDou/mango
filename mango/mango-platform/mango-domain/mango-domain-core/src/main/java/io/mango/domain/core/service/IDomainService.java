package io.mango.domain.core.service;

import io.mango.common.vo.PageResult;
import io.mango.domain.api.command.CreateDomainCommand;
import io.mango.domain.api.command.UpdateDomainCommand;
import io.mango.domain.api.command.UpdateDomainStatusCommand;
import io.mango.domain.api.query.DomainPageQuery;
import io.mango.domain.api.vo.DomainVO;
import io.mango.domain.core.entity.DomainEntity;
import io.mango.infra.persistence.api.crud.MangoTypedCrudService;

import java.util.List;

/**
 * 业务域服务。
 */
public interface IDomainService extends MangoTypedCrudService<
        DomainEntity, CreateDomainCommand, UpdateDomainCommand, DomainPageQuery, DomainVO, Long> {

    /**
     * 分页查询业务域并适配公共 HTTP 分页契约。
     *
     * @param query 查询条件。
     * @return 业务域分页结果。
     */
    PageResult<DomainVO> pageResult(DomainPageQuery query);

    /**
     * 查询业务域树。
     *
     * @param query 查询条件。
     * @return 业务域树。
     */
    List<DomainVO> tree(DomainPageQuery query);

    /**
     * 查询启用的业务域树。
     *
     * @return 启用的业务域树。
     */
    List<DomainVO> enabledTree();

    /**
     * 按编码查询业务域详情。
     *
     * @param domainCode 业务域编码。
     * @return 业务域详情。
     */
    DomainVO detailByCode(String domainCode);

    /**
     * 更新业务域状态。
     *
     * @param command 状态更新命令。
     * @return 是否更新成功。
     */
    boolean updateStatus(UpdateDomainStatusCommand command);
}
