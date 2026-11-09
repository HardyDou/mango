package io.mango.integration.api;

import io.mango.integration.api.command.IntegrationInstanceCommand;
import io.mango.integration.api.command.IntegrationConnectionTestCommand;
import io.mango.integration.api.command.IntegrationInstanceStatusCommand;
import io.mango.integration.api.query.IntegrationInstancePageQuery;
import io.mango.integration.api.vo.IntegrationCallLogVO;
import io.mango.integration.api.vo.IntegrationConnectionTestVO;
import io.mango.integration.api.vo.IntegrationInstanceVO;
import io.mango.common.result.R;
import io.mango.common.vo.PageResult;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * 集成连接实例 API。
 */
public interface IntegrationInstanceApi {

    R<PageResult<IntegrationInstanceVO>> page(@Valid IntegrationInstancePageQuery query);

    R<IntegrationInstanceVO> detail(
            @NotBlank(message = "实例编码不能为空") @Size(max = 64) String instanceCode);

    R<Boolean> save(@Valid IntegrationInstanceCommand command);

    R<Boolean> changeStatus(@Valid IntegrationInstanceStatusCommand command);

    R<IntegrationConnectionTestVO> testConnection(@Valid IntegrationConnectionTestCommand command);

    R<java.util.List<IntegrationCallLogVO>> callLogs(
            @NotBlank(message = "实例编码不能为空") @Size(max = 64) String instanceCode,
            @jakarta.validation.constraints.Min(1) @jakarta.validation.constraints.Max(200) Integer limit);
}
