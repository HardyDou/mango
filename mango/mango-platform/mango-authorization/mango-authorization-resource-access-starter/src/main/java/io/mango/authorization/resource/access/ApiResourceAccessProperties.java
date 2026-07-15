package io.mango.authorization.resource.access;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.ArrayList;
import java.util.List;

/** API 资源鉴权所需的入口路径配置。 */
@ConfigurationProperties(prefix = "mango.access")
public class ApiResourceAccessProperties {

    /** 外部网关或前端代理暴露的 API 前缀。 */
    private List<String> externalApiPrefixes = new ArrayList<>(List.of("/api"));

    public List<String> getExternalApiPrefixes() {
        return externalApiPrefixes;
    }

    public void setExternalApiPrefixes(List<String> externalApiPrefixes) {
        this.externalApiPrefixes = externalApiPrefixes;
    }
}
