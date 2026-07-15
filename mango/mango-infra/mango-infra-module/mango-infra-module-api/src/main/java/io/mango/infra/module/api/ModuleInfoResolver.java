package io.mango.infra.module.api;

import io.mango.common.contract.LocalCapabilityContract;

import java.util.Optional;

/**
 * 按 Mango 模块名解析真实部署服务信息。
 */
@FunctionalInterface
@LocalCapabilityContract
public interface ModuleInfoResolver {

    Optional<ModuleInfo> resolve(String moduleName);
}
