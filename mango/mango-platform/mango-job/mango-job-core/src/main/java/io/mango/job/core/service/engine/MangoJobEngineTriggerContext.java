package io.mango.job.core.service.engine;

import io.mango.job.core.entity.MangoJobDefinitionEntity;
import io.mango.job.core.entity.MangoJobInstanceEntity;
import lombok.Getter;
import lombok.Setter;

/**
 * Mango Job 引擎触发请求。
 */
@Getter
@Setter
public class MangoJobEngineTriggerContext {

    private MangoJobDefinitionEntity definition;

    private MangoJobInstanceEntity instance;

    private String batchNo;

    private String paramValue;

    public static MangoJobEngineTriggerContext of(MangoJobDefinitionEntity definition,
                                                   MangoJobInstanceEntity instance,
                                                   String batchNo,
                                                   String paramValue) {
        MangoJobEngineTriggerContext context = new MangoJobEngineTriggerContext();
        context.setDefinition(definition);
        context.setInstance(instance);
        context.setBatchNo(batchNo);
        context.setParamValue(paramValue);
        return context;
    }
}
