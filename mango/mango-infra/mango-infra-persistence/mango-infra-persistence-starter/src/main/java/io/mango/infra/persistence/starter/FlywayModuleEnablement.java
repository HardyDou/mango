package io.mango.infra.persistence.starter;

import io.mango.infra.persistence.api.module.PersistenceModuleEnablement;
import lombok.RequiredArgsConstructor;
import org.springframework.util.StringUtils;

import java.util.Optional;

/**
 * 基于 Flyway 模块配置回答模块是否启用。
 * <p>
 * 与迁移执行使用同一份 {@link PersistenceFlywayProperties}，因此 {@code enabled=false} 在迁移与
 * Resource 装配两侧保持同一语义。
 */
@RequiredArgsConstructor
final class FlywayModuleEnablement implements PersistenceModuleEnablement {

    private final PersistenceFlywayProperties properties;

    @Override
    public boolean isEnabled(String moduleCode) {
        if (!properties.isEnabled()) {
            return false;
        }
        if (!StringUtils.hasText(moduleCode)) {
            return true;
        }
        PersistenceFlywayProperties.ModuleConfig module = properties.getModules().get(moduleCode.trim());
        return module == null || module.isEnabled();
    }

    @Override
    public Optional<String> skipReason(String moduleCode) {
        if (!StringUtils.hasText(moduleCode)) {
            return Optional.empty();
        }
        PersistenceFlywayProperties.ModuleConfig module = properties.getModules().get(moduleCode.trim());
        if (module == null || module.isEnabled()) {
            return Optional.empty();
        }
        return Optional.ofNullable(module.getSkipReason()).filter(StringUtils::hasText);
    }
}
