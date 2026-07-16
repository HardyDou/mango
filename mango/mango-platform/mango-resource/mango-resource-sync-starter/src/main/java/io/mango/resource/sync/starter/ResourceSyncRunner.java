package io.mango.resource.sync.starter;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.mango.common.result.R;
import io.mango.common.result.Require;
import io.mango.resource.api.ResourceRegistryApi;
import io.mango.resource.api.command.RegisterResourceDeclarationsCommand;
import io.mango.resource.api.enums.ResourceCode;
import io.mango.resource.support.model.ResourceDeclaration;
import io.mango.resource.support.config.ResourceRegistryProperties;
import io.mango.resource.support.declaration.ResourceDeclarationCollector;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.Ordered;
import org.springframework.util.StringUtils;

import java.util.List;

/**
 * 扫描当前应用资源声明并调用资源注册中心 API。
 */
@Slf4j
@RequiredArgsConstructor
public class ResourceSyncRunner implements ApplicationRunner, Ordered {

    private static final int RESOURCE_SYNC_ORDER_OFFSET = 50;

    private final ResourceRegistryProperties properties;
    private final ResourceDeclarationCollector collector;
    private final ResourceRegistryApi resourceRegistryApi;
    private final ObjectMapper objectMapper;
    private final String applicationName;

    @Override
    public void run(ApplicationArguments args) {
        if (!properties.isEnabled() || !properties.getRemote().isEnabled()) {
            log.info("Mango resource declaration sync disabled");
            return;
        }
        List<ResourceDeclaration> declarations = collector.collect();
        List<String> moduleCodes = collector.managedModuleCodes(declarations).stream().sorted().toList();
        if (declarations.isEmpty() && moduleCodes.isEmpty()) {
            log.info("Mango resource declaration sync skipped: no declarations and no managed modules");
            return;
        }
        RegisterResourceDeclarationsCommand command = new RegisterResourceDeclarationsCommand();
        command.setAppCode(resolveAppCode());
        command.setServiceCode(resolveServiceCode());
        command.setModuleCodes(moduleCodes);
        command.setDeclarations(serializeDeclarations(declarations));
        R<Boolean> response = resourceRegistryApi.registerDeclarations(command);
        Require.notNull(response, ResourceCode.RESOURCE_SYNC_FAILED, "资源注册中心无响应");
        Require.isTrue(response.isSuccess(), ResourceCode.RESOURCE_SYNC_FAILED, response.getMsg());
        log.info("Mango resource declaration sync complete: appCode={}, serviceCode={}, declarations={}",
                command.getAppCode(), command.getServiceCode(), declarations.size());
    }

    @Override
    public int getOrder() {
        return Ordered.LOWEST_PRECEDENCE - RESOURCE_SYNC_ORDER_OFFSET;
    }

    private String resolveAppCode() {
        String appCode = properties.getRemote().getAppCode();
        if (StringUtils.hasText(appCode)) {
            return appCode;
        }
        Require.notBlank(applicationName, ResourceCode.RESOURCE_INVALID, "资源注册 appCode 不能为空");
        return applicationName;
    }

    private String resolveServiceCode() {
        String serviceCode = properties.getRemote().getServiceCode();
        if (StringUtils.hasText(serviceCode)) {
            return serviceCode;
        }
        return applicationName;
    }

    private String serializeDeclarations(List<ResourceDeclaration> declarations) {
        try {
            return objectMapper.writeValueAsString(declarations);
        } catch (JsonProcessingException exception) {
            log.warn("Mango resource declarations serialization failed: {}", exception.getOriginalMessage());
            Require.isTrue(false, ResourceCode.RESOURCE_INVALID, "资源声明序列化失败");
            return "[]";
        }
    }
}
