package io.mango.job.core.service.engine;

import io.mango.job.core.entity.MangoJobDefinitionEntity;
import lombok.Getter;
import lombok.Setter;

/**
 * Mango Job 引擎同步请求。
 */
@Getter
@Setter
public class MangoJobEngineRequest {

    private MangoJobDefinitionEntity definition;

    private String action;
}
