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

    /**
     * 保存连接器定义（页面维护）。
     *
     * <p>新建时主键由持久层生成；{@code command.id} 在手工保存时被忽略。</p>
     *
     * @param command 连接器保存命令
     * @return 是否保存成功
     */
    Boolean save(IntegrationConnectorCommand command);

    /**
     * 保存连接器定义（资源声明维护）。
     *
     * <p>新建时若 {@code command.id} 已给出，则直接作为主键写入，保证声明回放与冷基线生成可重复；
     * 已存在的声明行（含历史雪花主键）保持原有主键不变。</p>
     *
     * @param command 连接器保存命令
     * @return 是否保存成功
     */
    Boolean saveDeclaration(IntegrationConnectorCommand command);

    boolean isEnabled(IntegrationConnectorEntity connector);

    Long findIdByCode(String connectorCode);

    Boolean updateStatusByCode(String connectorCode, Integer status);
}
