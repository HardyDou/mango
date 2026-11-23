package io.mango.authorization.data.grant;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * Grant 保护姿态日志。
 *
 * <p>三种姿态在运行期的可观测性完全不同：启用且配置密钥时每个请求都带 Grant 头；启用但缺密钥时列表不签发、
 * 响应没有 {@code X-Mango-Data-Grant}（请求侧只能看到 {@code absent}）；关闭时连 Header 都没有，
 * 运维无法从流量上分辨「保护关闭」与「没有接入」。因此启动时打一行确定的日志，把当前姿态写进日志。</p>
 *
 * <p>本配置不受 {@code mango.data-grant.enabled} 约束，否则「关闭」这一最需要被看见的姿态反而没有日志。</p>
 */
@Slf4j
@Configuration(proxyBeanMethods = false)
@EnableConfigurationProperties(DataGrantProperties.class)
public class DataGrantPostureConfiguration {

    /**
     * 构造并输出当前保护姿态。
     *
     * @param properties Grant 配置。
     */
    public DataGrantPostureConfiguration(DataGrantProperties properties) {
        if (!properties.isEnabled()) {
            log.warn("数据权限 Grant 已关闭（默认状态）。该能力当前为半成品（未完成项见 #1420），禁止在生产启用");
            return;
        }
        if (!properties.hasSecret()) {
            log.warn("数据权限 Grant 已启用但未配置 mango.data-grant.secret，列表响应不会签发 Grant，详情接口按未携带处理");
            return;
        }
        log.info("数据权限 Grant 已启用, mode={}, ttlSeconds={}, maxIds={}",
                properties.getMode(), properties.getTtlSeconds(), properties.getMaxIds());
    }
}
