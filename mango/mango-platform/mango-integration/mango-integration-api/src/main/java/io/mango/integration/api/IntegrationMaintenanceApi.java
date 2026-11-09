package io.mango.integration.api;

import io.mango.common.result.R;
import io.mango.integration.api.command.IntegrationMaintenanceCleanupCommand;
import jakarta.validation.Valid;

/**
 * 集成模块维护 API。
 */
public interface IntegrationMaintenanceApi {

    /**
     * 手动清理历史日志。
     *
     * @param command 清理命令
     * @return 删除条数
     */
    R<Integer> cleanup(@Valid IntegrationMaintenanceCleanupCommand command);
}
