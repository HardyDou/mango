package io.mango.common.credential;

import java.util.List;

/**
 * 详情访问票据的解析结果。
 *
 * @param kid 签名密钥版本
 * @param issuedAt 签发时间（epoch 秒）
 * @param endpoints 允许访问的入口标识
 * @param dataId 该票据绑定的数据标识
 * @author Mango
 */
public record DetailTicket(String kid, long issuedAt, List<String> endpoints, String dataId) {

    /**
     * 宽松校验入口集合。
     *
     * @param endpoint 当前请求的入口标识
     * @return 是否允许
     */
    public boolean allows(String endpoint) {
        return endpoints != null && endpoints.contains(endpoint);
    }
}
