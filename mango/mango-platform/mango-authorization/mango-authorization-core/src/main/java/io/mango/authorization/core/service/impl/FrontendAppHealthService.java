package io.mango.authorization.core.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import io.mango.authorization.api.command.FrontendAppHealthCheckCommand;
import io.mango.authorization.api.enums.AuthorizationCode;
import io.mango.authorization.api.vo.FrontendAppHealthCheckVO;
import io.mango.authorization.core.config.OutboundNetworkProperties;
import io.mango.authorization.core.entity.FrontendAppRegistryEntity;
import io.mango.authorization.core.mapper.FrontendAppRegistryMapper;
import io.mango.authorization.core.service.IFrontendAppHealthService;
import io.mango.authorization.core.support.integration.OutboundCall;
import io.mango.authorization.core.support.integration.OutboundHttpInvoker;
import io.mango.authorization.core.support.integration.OutboundResult;
import io.mango.common.result.Require;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;

/**
 * 前端运行单元健康检查服务。
 *
 * <p>目标地址只能来自注册表，避免被用作任意地址探测入口；默认限制私网目标。</p>
 */
@Service
@RequiredArgsConstructor(onConstructor_ = @SuppressFBWarnings(
        value = "EI_EXPOSE_REP2",
        justification = "Spring injects managed mapper and configuration collaborators"))
public class FrontendAppHealthService implements IFrontendAppHealthService {

    private static final int DEFAULT_TIMEOUT_MS = 5000;
    private static final String MICRO_APP = "MICRO_APP";

    private final FrontendAppRegistryMapper registryMapper;
    private final OutboundNetworkProperties integrationProperties;

    @Override
    public FrontendAppHealthCheckVO check(FrontendAppHealthCheckCommand command) {
        FrontendAppHealthCheckCommand validated = Require.nonNull(
                command, AuthorizationCode.AUTHORIZATION_BUSINESS_ERROR, "健康检查命令不能为空");
        FrontendAppRegistryEntity registry = registryMapper.selectOne(
                new LambdaQueryWrapper<FrontendAppRegistryEntity>()
                        .eq(FrontendAppRegistryEntity::getAppCode, validated.getRuntimeCode().trim())
                        .last("LIMIT 1"));
        Require.notNull(registry, AuthorizationCode.AUTHORIZATION_NOT_FOUND, "前端运行单元不存在");
        Require.isTrue(StringUtils.hasText(registry.getHealthCheckUrl()) || StringUtils.hasText(registry.getEntryUrl()),
                AuthorizationCode.AUTHORIZATION_BUSINESS_ERROR, "运行单元未配置入口或健康检查地址");
        String targetUrl = StringUtils.hasText(registry.getHealthCheckUrl())
                ? registry.getHealthCheckUrl().trim() : registry.getEntryUrl().trim();
        OutboundResult result = new OutboundHttpInvoker(integrationProperties.isAllowPrivateNetwork())
                .invoke(OutboundCall.builder()
                        .baseUrl(targetUrl)
                        .path("/")
                        .timeoutMs(DEFAULT_TIMEOUT_MS)
                        .build());
        FrontendAppHealthCheckVO vo = new FrontendAppHealthCheckVO();
        vo.setRuntimeCode(registry.getAppCode());
        vo.setTargetUrl(targetUrl);
        vo.setSuccess(result.isSuccess());
        vo.setStatusCode(result.getStatusCode());
        vo.setElapsedMs(result.getElapsedMs());
        vo.setMessage(result.getMessage());
        vo.setCheckedAt(LocalDateTime.now());
        return vo;
    }
}
