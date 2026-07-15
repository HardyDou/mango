package io.mango.authorization.core.service;

import io.mango.authorization.api.command.FrontendModuleRuntimeStrategyCommand;
import io.mango.authorization.api.query.FrontendModuleRuntimeStrategyQuery;
import io.mango.authorization.api.vo.FrontendModuleRuntimeStrategyVO;

import java.util.List;

/**
 * 前端模块运行策略服务。
 */
public interface IFrontendRuntimeStrategyService {

    String currentDeployProfile();

    List<FrontendModuleRuntimeStrategyVO> list(FrontendModuleRuntimeStrategyQuery query);

    Long save(FrontendModuleRuntimeStrategyCommand command);

    Boolean disable(Long strategyId);

    Boolean disable(FrontendModuleRuntimeStrategyQuery query);

    Boolean delete(Long strategyId);

    Boolean delete(FrontendModuleRuntimeStrategyQuery query);
}
