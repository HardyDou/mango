package io.mango.authorization.core.service;

import io.mango.authorization.api.command.FrontendAppHealthCheckCommand;
import io.mango.authorization.api.vo.FrontendAppHealthCheckVO;

/**
 * 前端运行单元健康检查服务。
 */
public interface IFrontendAppHealthService {

    FrontendAppHealthCheckVO check(FrontendAppHealthCheckCommand command);
}
