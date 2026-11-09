package io.mango.integration.core.service;

import io.mango.integration.api.command.IntegrationInstanceCommand;
import io.mango.integration.api.command.IntegrationInstanceStatusCommand;
import io.mango.integration.api.query.IntegrationInstancePageQuery;
import io.mango.integration.api.vo.IntegrationInstanceVO;
import io.mango.common.vo.PageResult;

/**
 * 集成连接实例服务。
 */
public interface IIntegrationInstanceService {

    PageResult<IntegrationInstanceVO> page(IntegrationInstancePageQuery query);

    IntegrationInstanceVO detail(String instanceCode);

    Boolean save(IntegrationInstanceCommand command);

    Boolean changeStatus(IntegrationInstanceStatusCommand command);

    java.util.List<io.mango.integration.api.vo.IntegrationCallLogVO> callLogs(String instanceCode, Integer limit);

    io.mango.integration.api.vo.IntegrationConnectionTestVO testConnection(
            io.mango.integration.api.command.IntegrationConnectionTestCommand command);
}
