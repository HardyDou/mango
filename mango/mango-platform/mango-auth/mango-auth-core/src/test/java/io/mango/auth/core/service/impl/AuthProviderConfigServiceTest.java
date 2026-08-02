package io.mango.auth.core.service.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.mango.auth.api.command.SaveProviderConfigCommand;
import io.mango.auth.api.enums.ExternalAuthProvider;
import io.mango.auth.api.vo.ProviderConfigVO;
import io.mango.auth.core.entity.AuthProviderConfigEntity;
import io.mango.auth.core.mapper.AuthProviderConfigMapper;
import io.mango.auth.core.service.AuthProviderSecretCodec;
import io.mango.authorization.api.TenantAppBindingApi;
import io.mango.infra.context.api.MangoContextHolder;
import io.mango.infra.context.api.MangoContextSnapshot;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.support.StaticListableBeanFactory;

import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class AuthProviderConfigServiceTest {

    private AuthProviderConfigMapper mapper;
    private AuthProviderSecretCodec codec;
    private AuthProviderConfigService service;

    @BeforeEach
    void setUp() {
        mapper = mock(AuthProviderConfigMapper.class);
        codec = mock(AuthProviderSecretCodec.class);
        StaticListableBeanFactory beans = new StaticListableBeanFactory();
        service = new AuthProviderConfigService(
                mapper, codec, new ObjectMapper(), beans.getBeanProvider(TenantAppBindingApi.class));
        MangoContextHolder.set(MangoContextSnapshot.empty()
                .withRequest(null, null, "1", "internal-admin", null));
    }

    @AfterEach
    void tearDown() {
        MangoContextHolder.clear();
    }

    @Test
    void encryptsNewSecretAndNeverExposesSecretFieldsInTheView() {
        when(mapper.selectOne(any())).thenReturn(null);
        when(codec.encrypt("provider-secret")).thenReturn("enc:ciphertext");
        SaveProviderConfigCommand command = wecomCommand();
        command.setSecret("provider-secret");

        ProviderConfigVO result = service.save(command);

        verify(codec).encrypt("provider-secret");
        verify(mapper).insert(any(AuthProviderConfigEntity.class));
        assertThat(result.getSecretConfigured()).isTrue();
        assertThat(result.getComplete()).isTrue();
        assertThat(Arrays.stream(ProviderConfigVO.class.getDeclaredFields()).map(Field::getName))
                .noneMatch(name -> name.equals("secret") || name.equals("secretCiphertext"));
    }

    @Test
    void blankSecretOnUpdateKeepsExistingCiphertext() {
        AuthProviderConfigEntity existing = new AuthProviderConfigEntity();
        existing.setId(9L);
        existing.setAppCode("internal-admin");
        existing.setProvider(ExternalAuthProvider.WECOM.name());
        existing.setSecretCiphertext("enc:existing-ciphertext");
        when(mapper.selectById(9L)).thenReturn(existing);
        SaveProviderConfigCommand command = wecomCommand();
        command.setId(9L);
        command.setSecret("  ");

        ProviderConfigVO result = service.save(command);

        verify(codec, never()).encrypt(any());
        verify(mapper).updateById(existing);
        assertThat(existing.getSecretCiphertext()).isEqualTo("enc:existing-ciphertext");
        assertThat(result.getSecretConfigured()).isTrue();
    }

    private SaveProviderConfigCommand wecomCommand() {
        SaveProviderConfigCommand command = new SaveProviderConfigCommand();
        command.setAppCode("internal-admin");
        command.setProvider(ExternalAuthProvider.WECOM);
        command.setProviderTenantId("corp-id");
        command.setAgentId("1000003");
        command.setRedirectUris(List.of("https://admin.example.com/callback"));
        command.setEnabled(true);
        return command;
    }
}
