package io.mango.ai.api;

import io.mango.ai.api.command.CreateAiModelCommand;
import io.mango.ai.api.command.CreateAiProviderConnectionCommand;
import io.mango.ai.api.command.SetAiCapabilityRouteCommand;
import io.mango.ai.api.command.UpdateAiModelCommand;
import io.mango.ai.api.command.UpdateAiProviderConnectionCommand;
import io.mango.ai.api.query.AiModelQuery;
import io.mango.ai.api.vo.AiCapabilityRouteVO;
import io.mango.ai.api.vo.AiModelVO;
import io.mango.ai.api.vo.AiProviderConnectionVO;
import io.mango.ai.api.vo.AiProviderTypeVO;
import io.mango.common.result.R;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.util.List;

/** AI 模型管理对外契约。 */
public interface AiModelManagementApi {
    R<List<AiProviderConnectionVO>> providers();
    R<List<AiProviderTypeVO>> providerTypes();
    R<List<AiModelVO>> models(@Valid AiModelQuery query);
    R<List<AiCapabilityRouteVO>> routes();
    R<Long> createProvider(@Valid CreateAiProviderConnectionCommand command);
    R<Boolean> updateProvider(@Valid UpdateAiProviderConnectionCommand command);
    R<Boolean> deleteProvider(@NotNull @Positive Long id);
    R<Long> createModel(@Valid CreateAiModelCommand command);
    R<Boolean> updateModel(@Valid UpdateAiModelCommand command);
    R<Boolean> deleteModel(@NotNull @Positive Long id);
    R<Boolean> setRoute(@Valid SetAiCapabilityRouteCommand command);
}
