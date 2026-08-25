package io.mango.resource.support.declaration;

import io.mango.resource.support.ResourceProvider;
import io.mango.resource.support.model.ResourceDeclaration;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.ObjectProvider;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

/**
 * 汇总所有资源声明提供者。
 */
@RequiredArgsConstructor
public class ResourceDeclarationCollector {

    private final ObjectProvider<ResourceProvider> providers;

    public List<ResourceDeclaration> collect() {
        return collectProviders(providers);
    }

    public List<ResourceDeclaration> collectBootstrap() {
        return collectProviders(providers.orderedStream().filter(ResourceProvider::participatesInBootstrap).toList());
    }

    private List<ResourceDeclaration> collectProviders(Iterable<ResourceProvider> source) {
        List<ResourceDeclaration> declarations = new ArrayList<>();
        for (ResourceProvider provider : source) {
            List<ResourceDeclaration> provided = provider.provide();
            if (provided != null) {
                declarations.addAll(provided);
            }
        }
        return declarations;
    }

    public Set<String> managedModuleCodes(List<ResourceDeclaration> declarations) {
        return managedModuleCodes(declarations, providers);
    }

    public Set<String> managedBootstrapModuleCodes(List<ResourceDeclaration> declarations) {
        return managedModuleCodes(declarations,
                providers.orderedStream().filter(ResourceProvider::participatesInBootstrap).toList());
    }

    public Map<String, List<String>> managedBootstrapModuleDependencies() {
        Map<String, List<String>> dependencies = new LinkedHashMap<>();
        for (ResourceProvider provider : providers.orderedStream()
                .filter(ResourceProvider::participatesInBootstrap).toList()) {
            Map<String, List<String>> provided = provider.moduleDependencies();
            if (provided == null) {
                continue;
            }
            provided.forEach((moduleCode, requiredModules) -> mergeDependencies(
                    dependencies, moduleCode, requiredModules));
        }
        return dependencies;
    }

    private static void mergeDependencies(Map<String, List<String>> target, String moduleCode,
                                          List<String> requiredModules) {
        if (moduleCode == null || moduleCode.isBlank()) {
            throw new IllegalStateException("Resource module dependency owner is required");
        }
        List<String> normalized = requiredModules == null ? List.of() : requiredModules.stream()
                .filter(value -> value != null && !value.isBlank())
                .map(String::trim)
                .distinct()
                .sorted()
                .toList();
        List<String> previous = target.putIfAbsent(moduleCode.trim(), normalized);
        if (previous != null && !previous.equals(normalized)) {
            throw new IllegalStateException("Conflicting Resource module dependencies: " + moduleCode);
        }
    }

    private Set<String> managedModuleCodes(List<ResourceDeclaration> declarations,
                                           Iterable<ResourceProvider> source) {
        Set<String> moduleCodes = new LinkedHashSet<>();
        for (ResourceDeclaration declaration : declarations) {
            if (declaration.getModuleCode() != null && !declaration.getModuleCode().isBlank()) {
                moduleCodes.add(declaration.getModuleCode());
            }
        }
        for (ResourceProvider provider : source) {
            List<String> providedModuleCodes = provider.moduleCodes();
            if (providedModuleCodes != null) {
                providedModuleCodes.stream()
                        .filter(moduleCode -> moduleCode != null && !moduleCode.isBlank())
                        .forEach(moduleCodes::add);
            }
        }
        return moduleCodes;
    }
}
