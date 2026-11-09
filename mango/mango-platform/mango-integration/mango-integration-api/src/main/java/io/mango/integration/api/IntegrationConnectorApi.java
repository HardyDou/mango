package io.mango.integration.api;

import io.mango.integration.api.command.IntegrationConnectorCommand;
import io.mango.integration.api.query.IntegrationConnectorPageQuery;
import io.mango.integration.api.vo.IntegrationConnectorVO;
import io.mango.common.result.R;
import io.mango.common.vo.PageResult;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * 集成连接器定义 API。
 *
 * <p>定义由平台随版本发布，本接口只提供只读查询。</p>
 */
public interface IntegrationConnectorApi {

    R<PageResult<IntegrationConnectorVO>> page(@Valid IntegrationConnectorPageQuery query);

    R<IntegrationConnectorVO> detail(
            @NotBlank(message = "连接器编码不能为空") @Size(max = 64) String connectorCode);

    R<Boolean> save(@Valid IntegrationConnectorCommand command);
}
