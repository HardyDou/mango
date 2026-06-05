package io.mango.job.core.service.engine;

import io.mango.job.core.entity.MangoJobDefinitionEntity;
import io.mango.job.core.entity.MangoJobInstanceEntity;

/**
 * Mango Job 引擎同步服务。
 */
public interface IMangoJobEngineSyncService {

    void syncDefinition(MangoJobDefinitionEntity definition, String action);

    void deleteDefinition(MangoJobDefinitionEntity definition);

    void trigger(MangoJobDefinitionEntity definition, MangoJobInstanceEntity instance, String batchNo);

    void trigger(MangoJobDefinitionEntity definition,
                 MangoJobInstanceEntity instance,
                 String batchNo,
                 String paramValue);

    void refreshInstance(MangoJobDefinitionEntity definition, MangoJobInstanceEntity instance);
}
