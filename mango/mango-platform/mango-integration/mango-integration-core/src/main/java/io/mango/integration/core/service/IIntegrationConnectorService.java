package io.mango.integration.core.service;

import io.mango.integration.api.command.IntegrationConnectorCommand;
import io.mango.integration.api.query.IntegrationConnectorPageQuery;
import io.mango.integration.api.vo.IntegrationConnectorVO;
import io.mango.integration.core.entity.IntegrationConnectorEntity;
import io.mango.common.vo.PageResult;

/**
 * 集成连接器定义服务。
 */
public interface IIntegrationConnectorService {

    PageResult<IntegrationConnectorVO> page(IntegrationConnectorPageQuery query);

    IntegrationConnectorVO detail(String connectorCode);

    IntegrationConnectorEntity require(String connectorCode);

    Boolean save(IntegrationConnectorCommand command);

    Boolean saveDeclaration(IntegrationConnectorCommand command);

    boolean isEnabled(IntegrationConnectorEntity connector);

    Long findIdByCode(String connectorCode);

    Boolean updateStatusByCode(String connectorCode, Integer status);
}
