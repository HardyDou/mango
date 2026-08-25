package io.mango.ai.core.service;

import io.mango.ai.api.command.CreateAiModelCommand;
import io.mango.ai.api.command.CreateAiProviderConnectionCommand;
import io.mango.ai.api.command.SetAiCapabilityRouteCommand;
import io.mango.ai.api.command.UpdateAiModelCommand;
import io.mango.ai.api.command.UpdateAiProviderConnectionCommand;
import io.mango.ai.api.enums.AiCapability;
import io.mango.ai.api.vo.AiCapabilityRouteVO;
import io.mango.ai.api.vo.AiServiceRuntimeOptionsVO;
import io.mango.ai.api.vo.AiModelVO;
import io.mango.ai.api.vo.AiProviderConnectionVO;
import io.mango.ai.api.vo.AiProviderTypeVO;
import java.util.List;

public interface IAiModelManagementService {
    List<AiProviderConnectionVO> providers();
    List<AiProviderTypeVO> providerTypes();
    List<AiModelVO> models(Long providerConnectionId, String keyword, Boolean enabled);
    List<AiCapabilityRouteVO> routes();
    Long createProvider(CreateAiProviderConnectionCommand command);
    Boolean updateProvider(UpdateAiProviderConnectionCommand command);
    Boolean deleteProvider(Long id);
    Long createModel(CreateAiModelCommand command);
    Boolean updateModel(UpdateAiModelCommand command);
    Boolean deleteModel(Long id);
    Boolean setRoute(SetAiCapabilityRouteCommand command);
    AiServiceRuntimeOptionsVO runtimeOptions();
    AiModelResolution resolveChatModel(Long modelId);
}
