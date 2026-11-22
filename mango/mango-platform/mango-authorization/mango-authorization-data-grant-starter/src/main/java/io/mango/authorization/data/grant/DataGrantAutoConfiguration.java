package io.mango.authorization.data.grant;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.mango.infra.persistence.api.scope.DataScopeProvider;
import io.mango.infra.web.starter.MangoWebProperties;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;

/**
 * Grant 自动配置。
 * <p>
 * 默认开启（{@code mango.data-grant.enabled=true}）。所有组件都使用 {@link ConditionalOnMissingBean}，
 * 业务方可以整体替换编码器、签发器、校验器、拦截器或响应通知。
 * <p>
 * 启动时把三个 Grant 响应头追加进 {@code mango.web.cors.exposed-headers}、把 {@code X-Mango-Data-Grant}
 * 追加进 {@code mango.web.cors.allowed-headers}（均幂等）。前者缺失时跨域前端读不到响应头，后者缺失时
 * 浏览器不允许回传，两种情况下 Grant 机制在浏览器端都等于不可用。
 */
@AutoConfiguration
@AutoConfigureAfter(name = {
        "io.mango.infra.web.starter.WebAutoConfiguration",
        "org.springframework.boot.autoconfigure.jackson.JacksonAutoConfiguration"
})
@ConditionalOnProperty(prefix = DataGrantProperties.PREFIX, name = "enabled", matchIfMissing = true)
@EnableConfigurationProperties(DataGrantProperties.class)
public class DataGrantAutoConfiguration {

    private final DataGrantProperties properties;

    /**
     * 构造自动配置。
     *
     * @param properties    Grant 配置。
     * @param webProperties Web 配置，用于追加 CORS 暴露头；容器未装配时为 null。
     */
    public DataGrantAutoConfiguration(
            DataGrantProperties properties,
            ObjectProvider<MangoWebProperties> webProperties) {
        this.properties = properties;
        MangoWebProperties web = webProperties.getIfAvailable();
        if (web != null && web.getCors() != null) {
            DataGrantHeaders.expose(web.getCors().getExposedHeaders());
            DataGrantHeaders.allowRequestHeader(web.getCors().getAllowedHeaders());
        }
    }

    /**
     * Grant 编解码器。
     *
     * @param objectMapper JSON 序列化器。
     * @return 编解码器。
     */
    @Bean
    @ConditionalOnMissingBean
    public DataGrantCodec dataGrantCodec(ObjectMapper objectMapper) {
        return new DataGrantCodec(properties, objectMapper);
    }

    /**
     * Grant 签发器。
     *
     * @param codec             编解码器。
     * @param dataScopeProvider 数据范围提供者，可选。
     * @return 签发器。
     */
    @Bean
    @ConditionalOnMissingBean
    public DataGrantIssuer dataGrantIssuer(
            DataGrantCodec codec,
            ObjectProvider<DataScopeProvider> dataScopeProvider) {
        return new DataGrantIssuer(properties, codec, dataScopeProvider);
    }

    /**
     * Grant 校验器。
     *
     * @param codec             编解码器。
     * @param dataScopeProvider 数据范围提供者，可选。
     * @return 校验器。
     */
    @Bean
    @ConditionalOnMissingBean
    public DataGrantVerifier dataGrantVerifier(
            DataGrantCodec codec,
            ObjectProvider<DataScopeProvider> dataScopeProvider) {
        return new DataGrantVerifier(codec, dataScopeProvider);
    }

    /**
     * Grant 校验拦截器。
     *
     * @param verifier 校验器。
     * @return 拦截器。
     */
    @Bean
    @ConditionalOnMissingBean
    public DataGrantInterceptor dataGrantInterceptor(DataGrantVerifier verifier) {
        return new DataGrantInterceptor(properties, verifier);
    }

    /**
     * Grant 签发响应通知。
     *
     * @param issuer 签发器。
     * @return 响应通知。
     */
    @Bean
    @ConditionalOnMissingBean
    public DataGrantResponseAdvice dataGrantResponseAdvice(DataGrantIssuer issuer) {
        return new DataGrantResponseAdvice(properties, issuer);
    }

    /**
     * 拦截器注册。
     *
     * @param interceptor Grant 拦截器。
     * @return Web MVC 配置。
     */
    @Bean
    @ConditionalOnMissingBean(name = "dataGrantWebMvcConfiguration")
    public DataGrantWebMvcConfiguration dataGrantWebMvcConfiguration(DataGrantInterceptor interceptor) {
        return new DataGrantWebMvcConfiguration(interceptor);
    }
}
