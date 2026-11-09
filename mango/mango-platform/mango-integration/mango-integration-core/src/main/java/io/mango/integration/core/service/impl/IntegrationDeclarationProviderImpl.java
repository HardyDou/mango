package io.mango.integration.core.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import io.mango.common.result.Require;
import io.mango.integration.api.enums.IntegrationCode;
import io.mango.integration.api.vo.IntegrationCredentialFieldVO;
import io.mango.integration.api.spi.IntegrationDeclarationProvider;
import io.mango.integration.api.vo.IntegrationInstanceDeclarationVO;
import io.mango.integration.core.entity.IntegrationConnectorEntity;
import io.mango.integration.core.entity.IntegrationInstanceEntity;
import io.mango.integration.core.mapper.IntegrationConnectorMapper;
import io.mango.integration.core.mapper.IntegrationInstanceMapper;
import io.mango.integration.core.service.IIntegrationCredentialService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * 集成声明读取实现。
 */
@Service
@RequiredArgsConstructor(onConstructor_ = @SuppressFBWarnings(
        value = "EI_EXPOSE_REP2",
        justification = "Spring injects managed mappers and credential service"))
public class IntegrationDeclarationProviderImpl implements IntegrationDeclarationProvider {

    private static final String CALLBACK_PATH_PREFIX = "/integration/callback/";

    private final IntegrationInstanceMapper instanceMapper;
    private final IntegrationConnectorMapper connectorMapper;
    private final IIntegrationCredentialService credentialService;

    @Override
    public IntegrationInstanceDeclarationVO instance(String instanceCode) {
        Require.notBlank(instanceCode, IntegrationCode.INTEGRATION_BUSINESS_ERROR, "实例编码不能为空");
        IntegrationInstanceEntity entity = instanceMapper.selectOne(
                new LambdaQueryWrapper<IntegrationInstanceEntity>()
                        .eq(IntegrationInstanceEntity::getInstanceCode, instanceCode.trim())
                        .last("LIMIT 1"));
        Require.notNull(entity, IntegrationCode.INTEGRATION_NOT_FOUND, "连接实例不存在");
        IntegrationInstanceDeclarationVO declaration = new IntegrationInstanceDeclarationVO();
        declaration.setInstanceCode(entity.getInstanceCode());
        declaration.setConnectorCode(entity.getConnectorCode());
        declaration.setEnvironment(entity.getEnvironment());
        declaration.setConfigValuesJson(entity.getConfigValuesJson());
        declaration.setCredentialCode(entity.getCredentialCode());
        declaration.setCapabilitiesJson(entity.getCapabilitiesJson());
        declaration.setCallbackPath(CALLBACK_PATH_PREFIX + entity.getConnectorCode() + "/" + entity.getInstanceCode());
        declaration.setStatus(entity.getStatus());
        return declaration;
    }

    @Override
    public String connectorAuthSchema(String connectorCode) {
        Require.notBlank(connectorCode, IntegrationCode.INTEGRATION_BUSINESS_ERROR, "连接器编码不能为空");
        IntegrationConnectorEntity connector = connectorMapper.selectOne(
                new LambdaQueryWrapper<IntegrationConnectorEntity>()
                        .eq(IntegrationConnectorEntity::getConnectorCode, connectorCode.trim())
                        .last("LIMIT 1"));
        Require.notNull(connector, IntegrationCode.INTEGRATION_NOT_FOUND, "连接器定义不存在");
        return connector.getAuthSchema();
    }

    @Override
    public List<IntegrationCredentialFieldVO> credentials(String credentialCode) {
        Require.notBlank(credentialCode, IntegrationCode.INTEGRATION_BUSINESS_ERROR, "凭证编码不能为空");
        Map<String, String> values = credentialService.resolve(credentialCode);
        List<IntegrationCredentialFieldVO> fields = new ArrayList<>();
        values.forEach((fieldCode, fieldValue) -> {
            if (!StringUtils.hasText(fieldCode)) {
                return;
            }
            IntegrationCredentialFieldVO field = new IntegrationCredentialFieldVO();
            field.setFieldCode(fieldCode);
            field.setFieldValue(fieldValue);
            fields.add(field);
        });
        return fields;
    }
}
