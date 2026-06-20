package io.mango.resource.sync.starter;

import io.mango.common.result.R;
import io.mango.common.result.Require;
import io.mango.resource.api.ResourceRegistryApi;
import io.mango.resource.api.command.RegisterResourceDeclarationsCommand;
import io.mango.resource.api.model.ResourceDeclaration;
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

    private final ResourceRegistryProperties properties;
    private final ResourceDeclarationCollector collector;
    private final ResourceRegistryApi resourceRegistryApi;
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
        command.setDeclarations(declarations);
        R<Boolean> response = resourceRegistryApi.registerDeclarations(command);
        Require.notNull(response, "资源注册中心无响应");
        Require.isTrue(response.isSuccess(), response.getMsg());
        log.info("Mango resource declaration sync complete: appCode={}, serviceCode={}, declarations={}",
                command.getAppCode(), command.getServiceCode(), declarations.size());
    }

    @Override
    public int getOrder() {
        return Ordered.LOWEST_PRECEDENCE - 50;
    }

    private String resolveAppCode() {
        String appCode = properties.getRemote().getAppCode();
        if (StringUtils.hasText(appCode)) {
            return appCode;
        }
        Require.notBlank(applicationName, "资源注册 appCode 不能为空");
        return applicationName;
    }

    private String resolveServiceCode() {
        String serviceCode = properties.getRemote().getServiceCode();
        if (StringUtils.hasText(serviceCode)) {
            return serviceCode;
        }
        return applicationName;
    }
}
