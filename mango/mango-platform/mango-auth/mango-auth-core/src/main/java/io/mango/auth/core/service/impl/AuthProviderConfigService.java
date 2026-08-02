package io.mango.auth.core.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.mango.auth.api.command.SaveProviderConfigCommand;
import io.mango.auth.api.enums.AuthCode;
import io.mango.auth.api.enums.ExternalAuthProvider;
import io.mango.auth.api.vo.AvailableProviderVO;
import io.mango.auth.api.vo.ProviderConfigVO;
import io.mango.auth.core.entity.AuthProviderConfigEntity;
import io.mango.auth.core.mapper.AuthProviderConfigMapper;
import io.mango.auth.core.service.AuthProviderSecretCodec;
import io.mango.auth.core.service.IAuthProviderConfigService;
import io.mango.authorization.api.TenantAppBindingApi;
import io.mango.authorization.api.query.TenantAppBindingQuery;
import io.mango.authorization.api.vo.TenantAppBindingVO;
import io.mango.common.result.R;
import io.mango.common.result.Require;
import io.mango.infra.context.api.MangoContextHolder;
import io.mango.infra.context.api.MangoContextSnapshot;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.net.URI;
import java.net.URISyntaxException;
import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class AuthProviderConfigService implements IAuthProviderConfigService {

    private static final TypeReference<List<String>> STRING_LIST_TYPE = new TypeReference<>() { };

    private final AuthProviderConfigMapper mapper;
    private final AuthProviderSecretCodec secretCodec;
    private final ObjectMapper objectMapper;
    private final ObjectProvider<TenantAppBindingApi> tenantAppBindingApiProvider;

    @Override
    public List<ProviderConfigVO> listCurrentTenant(String appCode) {
        requireCurrentTenant();
        String normalizedAppCode = requireText(appCode, "应用编码不能为空");
        requireAppAvailable(normalizedAppCode);
        return mapper.selectList(new LambdaQueryWrapper<AuthProviderConfigEntity>()
                        .eq(AuthProviderConfigEntity::getAppCode, normalizedAppCode)
                        .orderByAsc(AuthProviderConfigEntity::getProvider))
                .stream()
                .map(this::toVO)
                .toList();
    }

    @Override
    @Transactional
    public ProviderConfigVO save(SaveProviderConfigCommand command) {
        Require.notNull(command, AuthCode.PROVIDER_CONFIG_INVALID, "第三方登录配置不能为空");
        requireCurrentTenant();
        String appCode = requireText(command.getAppCode(), "应用编码不能为空");
        requireAppAvailable(appCode);
        validateRedirectUris(command.getRedirectUris());
        validateProviderFields(command);

        AuthProviderConfigEntity entity;
        if (command.getId() == null) {
            entity = mapper.selectOne(new LambdaQueryWrapper<AuthProviderConfigEntity>()
                    .eq(AuthProviderConfigEntity::getAppCode, appCode)
                    .eq(AuthProviderConfigEntity::getProvider, command.getProvider().name())
                    .last("LIMIT 1"));
            Require.isTrue(entity == null, AuthCode.PROVIDER_CONFIG_CONFLICT);
            entity = new AuthProviderConfigEntity();
        } else {
            entity = Require.nonNull(mapper.selectById(command.getId()), AuthCode.PROVIDER_CONFIG_INVALID);
            Require.isTrue(appCode.equals(entity.getAppCode())
                            && command.getProvider().name().equals(entity.getProvider()),
                    AuthCode.PROVIDER_CONFIG_INVALID, "配置的应用和 Provider 不允许修改");
        }
        entity.setAppCode(appCode);
        entity.setProvider(command.getProvider().name());
        entity.setClientId(resolveClientId(command));
        entity.setProviderTenantId(trimToNull(command.getProviderTenantId()));
        entity.setAgentId(trimToNull(command.getAgentId()));
        entity.setRedirectUrisJson(writeRedirectUris(command.getRedirectUris()));
        if (StringUtils.hasText(command.getSecret())) {
            entity.setSecretCiphertext(secretCodec.encrypt(command.getSecret()));
        }
        Require.isTrue(StringUtils.hasText(entity.getSecretCiphertext()), AuthCode.PROVIDER_CONFIG_INVALID,
                "启用前必须配置 Secret");
        entity.setEnabled(command.getEnabled());
        if (entity.getId() == null) {
            mapper.insert(entity);
        } else {
            mapper.updateById(entity);
        }
        return toVO(entity);
    }

    @Override
    public List<AvailableProviderVO> listAvailable(String tenantId, String appCode) {
        MangoContextSnapshot previous = MangoContextHolder.get();
        try {
            MangoContextHolder.update(current -> current.withTenantId(requireText(tenantId, "租户不能为空")));
            return mapper.selectList(new LambdaQueryWrapper<AuthProviderConfigEntity>()
                            .eq(AuthProviderConfigEntity::getAppCode, requireText(appCode, "应用编码不能为空"))
                            .eq(AuthProviderConfigEntity::getEnabled, true)
                            .orderByAsc(AuthProviderConfigEntity::getProvider))
                    .stream()
                    .filter(this::isComplete)
                    .map(entity -> new AvailableProviderVO(ExternalAuthProvider.valueOf(entity.getProvider()),
                            displayName(entity.getProvider())))
                    .toList();
        } finally {
            MangoContextHolder.set(previous);
        }
    }

    @Override
    public ResolvedProviderConfig requireAvailable(String tenantId, String appCode, ExternalAuthProvider provider) {
        Require.notNull(provider, AuthCode.PROVIDER_CONFIG_INVALID, "Provider 不能为空");
        MangoContextSnapshot previous = MangoContextHolder.get();
        try {
            String normalizedTenant = requireText(tenantId, "租户不能为空");
            String normalizedApp = requireText(appCode, "应用编码不能为空");
            MangoContextHolder.update(current -> current.withTenantId(normalizedTenant));
            AuthProviderConfigEntity entity = mapper.selectOne(new LambdaQueryWrapper<AuthProviderConfigEntity>()
                    .eq(AuthProviderConfigEntity::getAppCode, normalizedApp)
                    .eq(AuthProviderConfigEntity::getProvider, provider.name())
                    .eq(AuthProviderConfigEntity::getEnabled, true)
                    .last("LIMIT 1"));
            Require.isTrue(isComplete(entity), AuthCode.PROVIDER_CONFIG_UNAVAILABLE);
            return new ResolvedProviderConfig(entity.getId(), normalizedTenant, normalizedApp, provider,
                    entity.getClientId(), entity.getProviderTenantId(), entity.getAgentId(),
                    secretCodec.decrypt(entity.getSecretCiphertext()), readRedirectUris(entity.getRedirectUrisJson()));
        } finally {
            MangoContextHolder.set(previous);
        }
    }

    private ProviderConfigVO toVO(AuthProviderConfigEntity entity) {
        ProviderConfigVO vo = new ProviderConfigVO();
        vo.setId(entity.getId());
        vo.setAppCode(entity.getAppCode());
        vo.setProvider(ExternalAuthProvider.valueOf(entity.getProvider()));
        vo.setClientId(entity.getClientId());
        vo.setProviderTenantId(entity.getProviderTenantId());
        vo.setAgentId(entity.getAgentId());
        vo.setRedirectUris(readRedirectUris(entity.getRedirectUrisJson()));
        vo.setEnabled(entity.getEnabled());
        vo.setSecretConfigured(StringUtils.hasText(entity.getSecretCiphertext()));
        vo.setComplete(isComplete(entity));
        vo.setUpdatedAt(entity.getUpdatedAt());
        return vo;
    }

    private boolean isComplete(AuthProviderConfigEntity entity) {
        if (entity == null || !StringUtils.hasText(entity.getClientId())
                || !StringUtils.hasText(entity.getSecretCiphertext()) || readRedirectUris(entity.getRedirectUrisJson()).isEmpty()) {
            return false;
        }
        if (ExternalAuthProvider.WECOM.name().equals(entity.getProvider())) {
            return StringUtils.hasText(entity.getProviderTenantId()) && StringUtils.hasText(entity.getAgentId());
        }
        return ExternalAuthProvider.DINGTALK.name().equals(entity.getProvider());
    }

    private void validateProviderFields(SaveProviderConfigCommand command) {
        if (command.getProvider() == ExternalAuthProvider.WECOM) {
            Require.isTrue(StringUtils.hasText(command.getProviderTenantId()) && StringUtils.hasText(command.getAgentId()),
                    AuthCode.PROVIDER_CONFIG_INVALID, "企业微信必须配置企业 ID 和 AgentId");
            return;
        }
        Require.isTrue(StringUtils.hasText(command.getClientId()), AuthCode.PROVIDER_CONFIG_INVALID,
                "钉钉必须配置 ClientId");
    }

    private String resolveClientId(SaveProviderConfigCommand command) {
        String value = command.getProvider() == ExternalAuthProvider.WECOM
                ? command.getProviderTenantId() : command.getClientId();
        return requireText(value, "ClientId 不能为空");
    }

    private void validateRedirectUris(List<String> redirectUris) {
        Require.notEmpty(redirectUris, AuthCode.PROVIDER_CONFIG_INVALID, "至少配置一个回调地址");
        for (String value : redirectUris) {
            try {
                URI uri = new URI(value.trim());
                boolean https = "https".equalsIgnoreCase(uri.getScheme());
                boolean localHttp = "http".equalsIgnoreCase(uri.getScheme())
                        && ("localhost".equalsIgnoreCase(uri.getHost()) || "127.0.0.1".equals(uri.getHost()));
                Require.isTrue(uri.isAbsolute() && uri.getFragment() == null && (https || localHttp),
                        AuthCode.PROVIDER_CONFIG_INVALID, "回调地址必须使用 HTTPS，本地 localhost 可使用 HTTP");
            } catch (URISyntaxException exception) {
                Require.fail(AuthCode.PROVIDER_CONFIG_INVALID, "回调地址格式无效", exception);
            }
        }
    }

    private void requireAppAvailable(String appCode) {
        if (appCode.equals(MangoContextHolder.appCode())) {
            return;
        }
        TenantAppBindingApi api = Require.nonNull(tenantAppBindingApiProvider.getIfAvailable(),
                AuthCode.PROVIDER_CONFIG_INVALID, "租户应用服务不可用");
        TenantAppBindingQuery query = new TenantAppBindingQuery();
        query.setTenantId(Long.valueOf(requireCurrentTenant()));
        query.setAppCode(appCode);
        query.setStatus(1);
        R<List<TenantAppBindingVO>> response = api.list(query);
        Require.isTrue(response != null && response.isSuccess() && response.getData() != null
                        && response.getData().stream().anyMatch(binding -> appCode.equals(binding.getAppCode())),
                AuthCode.PROVIDER_CONFIG_INVALID, "应用未向当前租户启用");
    }

    private String requireCurrentTenant() {
        return requireText(MangoContextHolder.tenantId(), "当前租户上下文无效");
    }

    private String requireText(String value, String message) {
        Require.isTrue(StringUtils.hasText(value), AuthCode.PROVIDER_CONFIG_INVALID, message);
        return value.trim();
    }

    private String writeRedirectUris(List<String> values) {
        try {
            return objectMapper.writeValueAsString(values.stream().map(String::trim).distinct().toList());
        } catch (JsonProcessingException exception) {
            return Require.fail(AuthCode.PROVIDER_CONFIG_INVALID, "回调地址无法保存", exception);
        }
    }

    private List<String> readRedirectUris(String value) {
        if (!StringUtils.hasText(value)) {
            return List.of();
        }
        try {
            return objectMapper.readValue(value, STRING_LIST_TYPE);
        } catch (JsonProcessingException exception) {
            return Require.fail(AuthCode.PROVIDER_CONFIG_INVALID, "回调地址配置损坏", exception);
        }
    }

    private String trimToNull(String value) {
        return StringUtils.hasText(value) ? value.trim() : null;
    }

    private String displayName(String provider) {
        return ExternalAuthProvider.WECOM.name().equals(provider) ? "企业微信" : "钉钉";
    }
}
