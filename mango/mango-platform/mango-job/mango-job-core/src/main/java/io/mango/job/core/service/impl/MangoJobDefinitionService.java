package io.mango.job.core.service.impl;

import io.mango.job.core.entity.MangoJobDefinitionEntity;
import io.mango.job.core.mapper.MangoJobDefinitionMapper;
import io.mango.job.core.service.IMangoJobDefinitionService;
import org.springframework.stereotype.Service;

/**
 * Job 任务定义内部服务实现。
 */
@Service
public class MangoJobDefinitionService implements IMangoJobDefinitionService {

    private final MangoJobDefinitionMapper mapper;

    private final MangoJobDataSourceRouter dataSourceRouter;

    public MangoJobDefinitionService(MangoJobDefinitionMapper mapper, MangoJobDataSourceRouter dataSourceRouter) {
        this.mapper = mapper;
        this.dataSourceRouter = dataSourceRouter;
    }

    @Override
    public MangoJobDefinitionEntity saveDefinition(MangoJobDefinitionEntity entity) {
        return dataSourceRouter.route(() -> {
            mapper.insert(entity);
            return entity;
        });
    }

    @Override
    public MangoJobDefinitionEntity findById(Long id) {
        return dataSourceRouter.route(() -> mapper.selectById(id));
    }
}
