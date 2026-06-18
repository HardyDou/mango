package io.mango.resource.support.declaration;

import io.mango.resource.api.ResourceProvider;
import io.mango.resource.api.model.ResourceDeclaration;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.ObjectProvider;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * 汇总所有资源声明提供者。
 */
@RequiredArgsConstructor
public class ResourceDeclarationCollector {

    private final ObjectProvider<ResourceProvider> providers;

    public List<ResourceDeclaration> collect() {
        List<ResourceDeclaration> declarations = new ArrayList<>();
        for (ResourceProvider provider : providers) {
            List<ResourceDeclaration> provided = provider.provide();
            if (provided != null) {
                declarations.addAll(provided);
            }
        }
        return declarations;
    }

    public Set<String> managedModuleCodes(List<ResourceDeclaration> declarations) {
        Set<String> moduleCodes = new LinkedHashSet<>();
        for (ResourceDeclaration declaration : declarations) {
            if (declaration.getModuleCode() != null && !declaration.getModuleCode().isBlank()) {
                moduleCodes.add(declaration.getModuleCode());
            }
        }
        for (ResourceProvider provider : providers) {
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
