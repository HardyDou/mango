package io.mango.integration.api.spi;

import io.mango.integration.api.vo.IntegrationCredentialFieldVO;
import io.mango.integration.api.vo.IntegrationInstanceDeclarationVO;

import java.util.List;

/**
 * 集成声明读取能力。
 *
 * <p>业务域只依赖本接口读取实例配置与凭证，不直接访问集成模块的持久化实现。</p>
 */
public interface IntegrationDeclarationProvider {

    /**
     * 读取连接实例声明。
     *
     * @param instanceCode 实例编码
     * @return 实例声明
     */
    IntegrationInstanceDeclarationVO instance(String instanceCode);

    /**
     * 读取连接器认证规范。
     *
     * @param connectorCode 连接器编码
     * @return 认证规范JSON
     */
    String connectorAuthSchema(String connectorCode);

    /**
     * 读取解密后的凭证字段。
     *
     * @param credentialCode 凭证编码
     * @return 凭证字段列表
     */
    List<IntegrationCredentialFieldVO> credentials(String credentialCode);
}
