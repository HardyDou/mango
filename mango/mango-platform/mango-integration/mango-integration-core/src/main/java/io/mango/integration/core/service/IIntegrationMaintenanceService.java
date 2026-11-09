package io.mango.integration.core.service;

import io.mango.integration.api.command.IntegrationMaintenanceCleanupCommand;

/**
 * 集成模块维护服务。
 */
public interface IIntegrationMaintenanceService {

    Integer cleanup(IntegrationMaintenanceCleanupCommand command);
}
