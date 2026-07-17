package io.mango.job.core.service.engine;

import io.mango.job.core.entity.MangoJobDefinitionEntity;
import lombok.Getter;
import lombok.Setter;

/**
 * Mango Job 引擎同步请求。
 */
@Getter
@Setter
public class MangoJobEngineDefinitionContext {

    private MangoJobDefinitionEntity definition;

    private String action;

    public static MangoJobEngineDefinitionContext of(MangoJobDefinitionEntity definition, String action) {
        MangoJobEngineDefinitionContext context = new MangoJobEngineDefinitionContext();
        context.setDefinition(definition);
        context.setAction(action);
        return context;
    }
}
