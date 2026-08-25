package io.mango.ai.core.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.IdWorker;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.mango.ai.api.command.CreateAiModelCommand;
import io.mango.ai.api.command.CreateAiProviderConnectionCommand;
import io.mango.ai.api.command.SetAiCapabilityRouteCommand;
import io.mango.ai.api.command.UpdateAiModelCommand;
import io.mango.ai.api.command.UpdateAiProviderConnectionCommand;
import io.mango.ai.api.enums.AiApiProtocol;
import io.mango.ai.api.enums.AiCapability;
import io.mango.ai.api.enums.AiModality;
import io.mango.ai.api.enums.AiProviderType;
import io.mango.ai.api.vo.AiCapabilityRouteVO;
import io.mango.ai.api.vo.AiServiceModelOptionVO;
import io.mango.ai.api.vo.AiModelVO;
import io.mango.ai.api.vo.AiProviderConnectionVO;
import io.mango.ai.api.vo.AiProviderTypeVO;
import io.mango.ai.api.vo.AiServiceRuntimeOptionsVO;
import io.mango.ai.core.entity.AiCapabilityRouteEntity;
import io.mango.ai.core.entity.AiModelEntity;
import io.mango.ai.core.entity.AiProviderConnectionEntity;
import io.mango.ai.core.mapper.AiCapabilityRouteMapper;
import io.mango.ai.core.mapper.AiModelMapper;
import io.mango.ai.core.mapper.AiProviderConnectionMapper;
import io.mango.ai.core.service.AiModelResolution;
import io.mango.ai.core.service.IAiModelManagementService;
import io.mango.ai.core.service.impl.provider.OpenAiCompatibleEndpoint;
import io.mango.ai.core.service.impl.provider.OpenAiResponsesChatModel;
import io.mango.ai.api.enums.AiCode;
import io.mango.common.result.Require;
import io.mango.infra.crypto.impl.ICryptoService;
import lombok.RequiredArgsConstructor;
import org.springframework.ai.deepseek.DeepSeekChatModel;
import org.springframework.ai.deepseek.DeepSeekChatOptions;
import org.springframework.ai.deepseek.api.DeepSeekApi;
import org.springframework.ai.model.tool.ToolCallingManager;
import org.springframework.ai.openai.OpenAiChatModel;
import org.springframework.ai.openai.OpenAiChatOptions;
import org.springframework.ai.openai.api.OpenAiApi;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.net.URI;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/** AI 厂商、模型目录和能力路由服务。 */
@Service
@RequiredArgsConstructor
public class AiModelManagementService implements IAiModelManagementService {
    private static final int API_KEY_HINT_LENGTH = 4;
    private static final String OLLAMA_CLIENT_PLACEHOLDER_API_KEY = "ollama-local";
    private static final TypeReference<Set<AiCapability>> CAPABILITY_TYPE = new TypeReference<>() { };
    private static final TypeReference<Set<AiModality>> MODALITY_TYPE = new TypeReference<>() { };

    private final AiProviderConnectionMapper providerMapper;
    private final AiModelMapper modelMapper;
    private final AiCapabilityRouteMapper routeMapper;
    private final ICryptoService cryptoService;
    private final ObjectMapper objectMapper;

    @Override
    public List<AiProviderConnectionVO> providers() {
        return providerMapper.selectList(new LambdaQueryWrapper<AiProviderConnectionEntity>()
                        .orderByAsc(AiProviderConnectionEntity::getDisplayName))
                .stream().map(this::toProvider).toList();
    }

    @Override
    public List<AiProviderTypeVO> providerTypes() {
        return Arrays.stream(AiProviderType.values()).map(type -> {
            AiProviderTypeVO vo = new AiProviderTypeVO();
            vo.setCode(type);
            vo.setName(type.getDisplayName());
            vo.setDefaultCode(type.getDefaultCode());
            vo.setDefaultBaseUrl(type.getDefaultBaseUrl());
            vo.setApiKeyRequired(type.isApiKeyRequired());
            return vo;
        }).toList();
    }

    @Override
    public List<AiModelVO> models(Long providerConnectionId, String keyword, Boolean enabled) {
        Require.notNull(providerConnectionId, AiCode.MODEL_INVALID, "厂商接入不能为空");
        Require.nonNull(providerMapper.selectById(providerConnectionId), AiCode.PROVIDER_NOT_FOUND);
        LambdaQueryWrapper<AiModelEntity> wrapper = new LambdaQueryWrapper<AiModelEntity>()
                .eq(AiModelEntity::getProviderConnectionId, providerConnectionId)
                .eq(enabled != null, AiModelEntity::getEnabled, enabled)
                .orderByDesc(AiModelEntity::getUpdatedAt);
        if (StringUtils.hasText(keyword)) {
            wrapper.and(nested -> nested.like(AiModelEntity::getDisplayName, keyword.trim())
                    .or().like(AiModelEntity::getModelName, keyword.trim())
                    .or().like(AiModelEntity::getPlatformAlias, keyword.trim()));
        }
        List<AiCapabilityRouteEntity> routes = routeMapper.selectList(new LambdaQueryWrapper<AiCapabilityRouteEntity>());
        return modelMapper.selectList(wrapper).stream().map(model -> toModel(model, routes)).toList();
    }

    @Override
    public List<AiCapabilityRouteVO> routes() {
        List<AiModelEntity> models = modelMapper.selectList(new LambdaQueryWrapper<AiModelEntity>());
        List<AiProviderConnectionEntity> providers = providerMapper.selectList(new LambdaQueryWrapper<AiProviderConnectionEntity>());
        return routeMapper.selectList(new LambdaQueryWrapper<AiCapabilityRouteEntity>()).stream().map(route -> {
            AiCapabilityRouteVO vo = new AiCapabilityRouteVO();
            vo.setCapability(route.getCapability());
            vo.setModelId(route.getModelId());
            models.stream().filter(model -> model.getId().equals(route.getModelId())).findFirst()
                    .ifPresent(model -> vo.setModelDisplayName(model.getDisplayName()));
            models.stream().filter(model -> model.getId().equals(route.getModelId())).findFirst()
                    .flatMap(model -> providers.stream().filter(p -> p.getId().equals(model.getProviderConnectionId())).findFirst())
                    .ifPresent(provider -> vo.setProviderDisplayName(provider.getDisplayName()));
            return vo;
        }).toList();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long createProvider(CreateAiProviderConnectionCommand command) {
        Require.notNull(command, AiCode.PROVIDER_INVALID);
        AiProviderConnectionEntity entity = new AiProviderConnectionEntity();
        entity.setId(IdWorker.getId());
        entity.setCode(command.getCode().trim());
        entity.setDisplayName(command.getDisplayName().trim());
        entity.setProviderType(command.getProviderType());
        entity.setBaseUrl(normalizeBaseUrl(command.getBaseUrl()));
        if (!Boolean.TRUE.equals(command.getEnabled()) && !StringUtils.hasText(command.getApiKey())) {
            entity.setApiKeyCiphertext("");
            entity.setApiKeyHint("");
        } else {
            setSecret(entity, command.getApiKey(), command.getProviderType());
        }
        entity.setEnabled(command.getEnabled());
        insertProvider(entity);
        return entity.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Boolean updateProvider(UpdateAiProviderConnectionCommand command) {
        Require.notNull(command, AiCode.PROVIDER_INVALID);
        AiProviderConnectionEntity entity = Require.nonNull(providerMapper.selectById(command.getId()), AiCode.PROVIDER_NOT_FOUND);
        entity.setCode(command.getCode().trim());
        entity.setDisplayName(command.getDisplayName().trim());
        entity.setProviderType(command.getProviderType());
        entity.setBaseUrl(normalizeBaseUrl(command.getBaseUrl()));
        if (StringUtils.hasText(command.getApiKey())) setSecret(entity, command.getApiKey(), command.getProviderType());
        entity.setEnabled(command.getEnabled());
        try {
            Require.isTrue(providerMapper.updateById(entity) > 0, AiCode.PROVIDER_NOT_FOUND);
        } catch (DuplicateKeyException exception) {
            Require.fail(AiCode.PROVIDER_CONFLICT, AiCode.PROVIDER_CONFLICT.getMessage(), exception);
        }
        return true;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Boolean deleteProvider(Long id) {
        AiProviderConnectionEntity entity = Require.nonNull(providerMapper.selectById(id), AiCode.PROVIDER_NOT_FOUND);
        long count = modelMapper.selectCount(new LambdaQueryWrapper<AiModelEntity>()
                .eq(AiModelEntity::getProviderConnectionId, entity.getId()));
        Require.isTrue(count == 0, AiCode.PROVIDER_HAS_MODELS);
        return providerMapper.deleteById(entity.getId()) > 0;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long createModel(CreateAiModelCommand command) {
        Require.notNull(command, AiCode.MODEL_INVALID);
        AiProviderConnectionEntity provider = validateProvider(command.getProviderConnectionId());
        AiModelEntity entity = new AiModelEntity();
        entity.setId(IdWorker.getId());
        applyModel(entity, command);
        validateApiProtocol(entity, provider);
        insertModel(entity);
        return entity.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Boolean updateModel(UpdateAiModelCommand command) {
        Require.notNull(command, AiCode.MODEL_INVALID);
        AiModelEntity entity = Require.nonNull(modelMapper.selectById(command.getId()), AiCode.MODEL_NOT_FOUND);
        applyModel(entity, command);
        validateApiProtocol(entity, validateProvider(entity.getProviderConnectionId()));
        try {
            Require.isTrue(modelMapper.updateById(entity) > 0, AiCode.MODEL_NOT_FOUND);
        } catch (DuplicateKeyException exception) {
            Require.fail(AiCode.MODEL_CONFLICT, AiCode.MODEL_CONFLICT.getMessage(), exception);
        }
        return true;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Boolean deleteModel(Long id) {
        AiModelEntity entity = Require.nonNull(modelMapper.selectById(id), AiCode.MODEL_NOT_FOUND);
        routeMapper.delete(new LambdaQueryWrapper<AiCapabilityRouteEntity>().eq(AiCapabilityRouteEntity::getModelId, entity.getId()));
        return modelMapper.deleteById(entity.getId()) > 0;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Boolean setRoute(SetAiCapabilityRouteCommand command) {
        Require.notNull(command, AiCode.ROUTE_INVALID);
        AiModelEntity model = Require.nonNull(modelMapper.selectById(command.getModelId()), AiCode.MODEL_NOT_FOUND);
        Require.isTrue(Boolean.TRUE.equals(model.getEnabled()), AiCode.ROUTE_INVALID, "路由模型必须启用");
        AiProviderConnectionEntity provider = validateProvider(model.getProviderConnectionId());
        Require.isTrue(Boolean.TRUE.equals(provider.getEnabled()), AiCode.ROUTE_INVALID, "路由供应商必须启用");
        Set<AiCapability> capabilities = read(model.getCapabilitiesJson(), CAPABILITY_TYPE);
        Require.isTrue(capabilities.contains(command.getCapability()), AiCode.ROUTE_INVALID, "模型不支持该能力");
        validateApiProtocol(model, provider);
        AiCapabilityRouteEntity route = routeMapper.selectOne(new LambdaQueryWrapper<AiCapabilityRouteEntity>()
                .eq(AiCapabilityRouteEntity::getCapability, command.getCapability()));
        boolean newRoute = route == null;
        if (route == null) {
            route = new AiCapabilityRouteEntity();
            route.setId(IdWorker.getId());
            route.setCapability(command.getCapability());
        }
        route.setModelId(model.getId());
        if (newRoute) routeMapper.insert(route); else routeMapper.updateById(route);
        return true;
    }

    @Override
    public AiServiceRuntimeOptionsVO runtimeOptions() {
        List<AiProviderConnectionEntity> providers = providerMapper.selectList(
                new LambdaQueryWrapper<AiProviderConnectionEntity>());
        List<AiServiceModelOptionVO> options = modelMapper.selectList(new LambdaQueryWrapper<AiModelEntity>())
                .stream()
                .filter(this::isCallableChat)
                .map(model -> toServiceModelOption(model, providers))
                .toList();
        AiCapabilityRouteEntity route = routeMapper.selectOne(new LambdaQueryWrapper<AiCapabilityRouteEntity>()
                .eq(AiCapabilityRouteEntity::getCapability, AiCapability.CHAT));
        Require.nonNull(route, AiCode.CHAT_ROUTE_REQUIRED);
        Require.isTrue(options.stream().anyMatch(option -> route.getModelId().equals(option.getModelId())),
                AiCode.CHAT_ROUTE_REQUIRED, "默认 CHAT 模型当前不可调用");
        AiServiceRuntimeOptionsVO result = new AiServiceRuntimeOptionsVO();
        result.setDefaultModelId(route.getModelId());
        result.setModels(options);
        return result;
    }

    @Override
    public AiModelResolution resolveChatModel(Long modelId) {
        Require.notNull(modelId, AiCode.MODEL_INVALID, "会话模型不能为空");
        AiModelEntity model = Require.nonNull(modelMapper.selectById(modelId), AiCode.MODEL_NOT_FOUND);
        AiProviderConnectionEntity provider = Require.nonNull(providerMapper.selectById(model.getProviderConnectionId()), AiCode.PROVIDER_NOT_FOUND);
        Require.isTrue(Boolean.TRUE.equals(provider.getEnabled()) && Boolean.TRUE.equals(model.getEnabled()),
                AiCode.CHAT_MODEL_UNAVAILABLE, "会话模型或供应商未启用");
        Set<AiCapability> capabilities = read(model.getCapabilitiesJson(), CAPABILITY_TYPE);
        Require.isTrue(capabilities.contains(AiCapability.CHAT), AiCode.MODEL_INVALID, "会话模型不支持 CHAT 能力");
        validateApiProtocol(model, provider);
        String secret = provider.getProviderType() == AiProviderType.OLLAMA
                ? OLLAMA_CLIENT_PLACEHOLDER_API_KEY : cryptoService.decrypt(provider.getApiKeyCiphertext());
        Require.notBlank(secret, AiCode.MODEL_SECRET_UNAVAILABLE);
        if (model.getApiProtocol() == AiApiProtocol.RESPONSES) {
            return new AiModelResolution(
                    model.getId(),
                    new OpenAiResponsesChatModel(provider.getBaseUrl(), secret, model.getModelName(), objectMapper),
                    provider.getCode(), model.getModelName(), model.getApiProtocol(), true,
                    effectiveInputModalities(model, provider), effectiveOutputModalities(model));
        }
        if (provider.getProviderType() == AiProviderType.DEEPSEEK) {
            DeepSeekApi api = DeepSeekApi.builder().apiKey(secret).baseUrl(provider.getBaseUrl()).build();
            DeepSeekChatOptions options = DeepSeekChatOptions.builder().model(model.getModelName()).build();
            return new AiModelResolution(
                    model.getId(),
                    DeepSeekChatModel.builder().deepSeekApi(api).defaultOptions(options)
                            .toolCallingManager(ToolCallingManager.builder().build()).build(),
                    provider.getCode(), model.getModelName(), model.getApiProtocol(), false,
                    effectiveInputModalities(model, provider), effectiveOutputModalities(model));
        }
        if (supportsOpenAiChat(provider.getProviderType())) {
            OpenAiApi api = OpenAiApi.builder().apiKey(secret)
                    .baseUrl(OpenAiCompatibleEndpoint.apiBaseUrl(provider.getBaseUrl())).build();
            OpenAiChatOptions options = OpenAiChatOptions.builder()
                    .model(model.getModelName())
                    .streamUsage(true)
                    .build();
            return new AiModelResolution(
                    model.getId(),
                    OpenAiChatModel.builder().openAiApi(api).defaultOptions(options)
                            .toolCallingManager(ToolCallingManager.builder().build()).build(),
                    provider.getCode(), model.getModelName(), model.getApiProtocol(), false,
                    effectiveInputModalities(model, provider), effectiveOutputModalities(model));
        }
        return Require.fail(AiCode.CHAT_ADAPTER_UNAVAILABLE, "当前供应商尚未接入 Chat 运行时适配器");
    }

    private void applyModel(AiModelEntity entity, CreateAiModelCommand command) {
        entity.setProviderConnectionId(command.getProviderConnectionId());
        entity.setModelName(command.getModelName().trim());
        entity.setDisplayName(command.getDisplayName().trim());
        entity.setPlatformAlias(trimToNull(command.getPlatformAlias()));
        entity.setApiProtocol(command.getApiProtocol());
        entity.setCapabilitiesJson(write(command.getCapabilities()));
        entity.setInputModalitiesJson(write(command.getInputModalities()));
        entity.setOutputModalitiesJson(write(command.getOutputModalities()));
        entity.setParameterJson(trimToNull(command.getParameterJson()));
        validateParameterJson(entity.getParameterJson());
        entity.setEnabled(command.getEnabled());
    }

    private void applyModel(AiModelEntity entity, UpdateAiModelCommand command) {
        validateProvider(entity.getProviderConnectionId());
        entity.setModelName(command.getModelName().trim());
        entity.setDisplayName(command.getDisplayName().trim());
        entity.setPlatformAlias(trimToNull(command.getPlatformAlias()));
        entity.setApiProtocol(command.getApiProtocol());
        entity.setCapabilitiesJson(write(command.getCapabilities()));
        entity.setInputModalitiesJson(write(command.getInputModalities()));
        entity.setOutputModalitiesJson(write(command.getOutputModalities()));
        entity.setParameterJson(trimToNull(command.getParameterJson()));
        validateParameterJson(entity.getParameterJson());
        entity.setEnabled(command.getEnabled());
    }

    private AiProviderConnectionEntity validateProvider(Long id) {
        return Require.nonNull(providerMapper.selectById(id), AiCode.PROVIDER_NOT_FOUND);
    }

    private void insertProvider(AiProviderConnectionEntity entity) {
        try { providerMapper.insert(entity); }
        catch (DuplicateKeyException exception) { Require.fail(AiCode.PROVIDER_CONFLICT, AiCode.PROVIDER_CONFLICT.getMessage(), exception); }
    }

    private void insertModel(AiModelEntity entity) {
        try { modelMapper.insert(entity); }
        catch (DuplicateKeyException exception) { Require.fail(AiCode.MODEL_CONFLICT, AiCode.MODEL_CONFLICT.getMessage(), exception); }
    }

    private void setSecret(AiProviderConnectionEntity entity, String secret, AiProviderType providerType) {
        if (!StringUtils.hasText(secret) && providerType == AiProviderType.OLLAMA) {
            entity.setApiKeyCiphertext("");
            entity.setApiKeyHint("");
            return;
        }
        Require.notBlank(secret, AiCode.MODEL_SECRET_UNAVAILABLE);
        entity.setApiKeyCiphertext(cryptoService.encrypt(secret));
        entity.setApiKeyHint(secret.substring(Math.max(0, secret.length() - API_KEY_HINT_LENGTH)));
    }

    private String normalizeBaseUrl(String value) {
        Require.notBlank(value, AiCode.PROVIDER_INVALID);
        String normalized = value.trim();
        try {
            URI uri = URI.create(normalized);
            String host = uri.getHost();
            Require.isTrue(host != null && ("https".equalsIgnoreCase(uri.getScheme()) || isLocalHttp(uri.getScheme(), host)), AiCode.PROVIDER_INVALID,
                    "服务地址必须使用 HTTPS；本机联调仅允许 HTTP loopback 地址");
            Require.isTrue(uri.getUserInfo() == null && uri.getQuery() == null && uri.getFragment() == null, AiCode.PROVIDER_INVALID);
            while (normalized.endsWith("/")) normalized = normalized.substring(0, normalized.length() - 1);
            return normalized;
        } catch (IllegalArgumentException exception) {
            return Require.fail(AiCode.PROVIDER_INVALID, "服务地址格式不正确", exception);
        }
    }

    private boolean isLocalHttp(String scheme, String host) {
        String normalized = host.toLowerCase(Locale.ROOT);
        return "http".equalsIgnoreCase(scheme) && ("localhost".equals(normalized) || "127.0.0.1".equals(normalized) || "::1".equals(normalized));
    }

    private AiProviderConnectionVO toProvider(AiProviderConnectionEntity entity) {
        AiProviderConnectionVO vo = new AiProviderConnectionVO();
        vo.setId(entity.getId()); vo.setCode(entity.getCode()); vo.setDisplayName(entity.getDisplayName()); vo.setProviderType(entity.getProviderType());
        vo.setBaseUrl(entity.getBaseUrl()); vo.setApiKeyConfigured(StringUtils.hasText(entity.getApiKeyCiphertext())); vo.setApiKeyHint(entity.getApiKeyHint());
        vo.setEnabled(entity.getEnabled()); vo.setModelCount(Math.toIntExact(modelMapper.selectCount(new LambdaQueryWrapper<AiModelEntity>().eq(AiModelEntity::getProviderConnectionId, entity.getId())))); vo.setUpdatedAt(entity.getUpdatedAt());
        return vo;
    }

    private AiModelVO toModel(AiModelEntity entity, Collection<AiCapabilityRouteEntity> routes) {
        AiModelVO vo = new AiModelVO(); vo.setId(entity.getId()); vo.setProviderConnectionId(entity.getProviderConnectionId()); vo.setModelName(entity.getModelName()); vo.setDisplayName(entity.getDisplayName());
        vo.setPlatformAlias(entity.getPlatformAlias()); vo.setApiProtocol(entity.getApiProtocol()); vo.setCapabilities(read(entity.getCapabilitiesJson(), CAPABILITY_TYPE)); vo.setInputModalities(read(entity.getInputModalitiesJson(), MODALITY_TYPE)); vo.setOutputModalities(read(entity.getOutputModalitiesJson(), MODALITY_TYPE)); vo.setParameterJson(entity.getParameterJson()); vo.setEnabled(entity.getEnabled());
        vo.setCallable(entity.getProviderConnectionId() != null && isCallableChat(entity)); vo.setRoutedCapabilities(routes.stream().filter(route -> route.getModelId().equals(entity.getId())).map(AiCapabilityRouteEntity::getCapability).collect(java.util.stream.Collectors.toSet())); vo.setUpdatedAt(entity.getUpdatedAt()); return vo;
    }

    private AiServiceModelOptionVO toServiceModelOption(
            AiModelEntity model,
            Collection<AiProviderConnectionEntity> providers) {
        AiProviderConnectionEntity provider = providers.stream()
                .filter(candidate -> candidate.getId().equals(model.getProviderConnectionId()))
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("模型供应商不存在"));
        AiServiceModelOptionVO option = new AiServiceModelOptionVO();
        option.setModelId(model.getId());
        option.setModelName(model.getModelName());
        option.setDisplayName(model.getDisplayName());
        option.setProviderCode(provider.getCode());
        option.setProviderDisplayName(provider.getDisplayName());
        option.setApiProtocol(model.getApiProtocol());
        option.setThinkingConfigurable(model.getApiProtocol() == AiApiProtocol.RESPONSES);
        option.setInputModalities(effectiveInputModalities(model, provider));
        option.setOutputModalities(effectiveOutputModalities(model));
        return option;
    }

    private Set<AiModality> effectiveInputModalities(
            AiModelEntity model,
            AiProviderConnectionEntity provider) {
        Set<AiModality> configured = read(model.getInputModalitiesJson(), MODALITY_TYPE);
        Set<AiModality> adapter;
        if (provider.getProviderType() == AiProviderType.DEEPSEEK) {
            adapter = Set.of(AiModality.TEXT);
        } else if (model.getApiProtocol() == AiApiProtocol.RESPONSES) {
            adapter = Set.of(AiModality.TEXT, AiModality.IMAGE, AiModality.FILE);
        } else {
            adapter = Set.of(AiModality.TEXT, AiModality.IMAGE, AiModality.AUDIO, AiModality.FILE);
        }
        return configured.stream().filter(adapter::contains).collect(java.util.stream.Collectors.toUnmodifiableSet());
    }

    private Set<AiModality> effectiveOutputModalities(AiModelEntity model) {
        Set<AiModality> configured = read(model.getOutputModalitiesJson(), MODALITY_TYPE);
        return configured.contains(AiModality.TEXT) ? Set.of(AiModality.TEXT) : Set.of();
    }

    private boolean isCallableChat(AiModelEntity entity) {
        AiProviderConnectionEntity provider = providerMapper.selectById(entity.getProviderConnectionId());
        return provider != null
                && Boolean.TRUE.equals(provider.getEnabled())
                && Boolean.TRUE.equals(entity.getEnabled())
                && supportsApiProtocol(provider.getProviderType(), entity.getApiProtocol())
                && voCapabilities(entity).contains(AiCapability.CHAT);
    }

    private void validateApiProtocol(AiModelEntity model, AiProviderConnectionEntity provider) {
        Require.notNull(model.getApiProtocol(), AiCode.MODEL_INVALID, "模型调用协议不能为空");
        Require.isTrue(supportsApiProtocol(provider.getProviderType(), model.getApiProtocol()),
                AiCode.CHAT_ADAPTER_UNAVAILABLE, "供应商不支持所选模型调用协议");
    }

    private boolean supportsApiProtocol(AiProviderType providerType, AiApiProtocol apiProtocol) {
        if (apiProtocol == AiApiProtocol.RESPONSES) {
            return providerType == AiProviderType.OPENAI_COMPATIBLE;
        }
        return apiProtocol == AiApiProtocol.CHAT_COMPLETIONS && supportsOpenAiChat(providerType);
    }
    private boolean supportsOpenAiChat(AiProviderType providerType) {
        return providerType == AiProviderType.DEEPSEEK || providerType == AiProviderType.VOLCENGINE_ARK
                || providerType == AiProviderType.ALIBABA_DASHSCOPE || providerType == AiProviderType.ZHIPU
                || providerType == AiProviderType.SILICONFLOW || providerType == AiProviderType.KIMI
                || providerType == AiProviderType.OPENAI_COMPATIBLE || providerType == AiProviderType.OLLAMA;
    }
    private Set<AiCapability> voCapabilities(AiModelEntity entity) { return read(entity.getCapabilitiesJson(), CAPABILITY_TYPE); }
    private <T> Set<T> read(String value, TypeReference<Set<T>> type) {
        if (!StringUtils.hasText(value)) {
            return Set.of();
        }
        try {
            return objectMapper.readValue(value, type);
        } catch (JsonProcessingException exception) {
            return Require.fail(AiCode.MODEL_INVALID, "模型能力配置数据损坏", exception);
        }
    }
    private String write(Object value) { try { return objectMapper.writeValueAsString(value); } catch (JsonProcessingException exception) { return Require.fail(AiCode.MODEL_INVALID, "模型参数格式不正确", exception); } }
    private void validateParameterJson(String value) {
        if (!StringUtils.hasText(value)) return;
        try { Require.isTrue(objectMapper.readTree(value).isObject(), AiCode.MODEL_INVALID, "模型参数必须是 JSON 对象"); }
        catch (JsonProcessingException exception) { Require.fail(AiCode.MODEL_INVALID, "模型参数 JSON 格式不正确", exception); }
    }
    private String trimToNull(String value) { return StringUtils.hasText(value) ? value.trim() : null; }
}
