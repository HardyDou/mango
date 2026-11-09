package io.mango.integration.core.service;

import io.mango.integration.api.command.IntegrationCredentialCommand;
import io.mango.integration.api.command.IntegrationCredentialRotateCommand;
import io.mango.integration.api.vo.IntegrationCredentialVO;

import java.util.List;
import java.util.Map;

/**
 * 集成凭证服务。
 */
public interface IIntegrationCredentialService {

    List<IntegrationCredentialVO> list(String connectorCode);

    Boolean save(IntegrationCredentialCommand command);

    Boolean rotate(IntegrationCredentialRotateCommand command);

    /**
     * 供适配器读取解密后的字段值，禁止对外暴露。
     *
     * @param credentialCode 凭证编码
     * @return 字段值
     */
    Map<String, String> resolve(String credentialCode);

    java.util.List<io.mango.integration.api.vo.IntegrationCredentialRotationVO> rotations(String credentialCode);
}
