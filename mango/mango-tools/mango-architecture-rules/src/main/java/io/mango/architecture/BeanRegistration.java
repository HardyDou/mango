package io.mango.architecture;

import java.util.LinkedHashSet;
import java.util.Set;

record BeanRegistration(boolean conditional, Set<String> factories) {
    BeanRegistration merge(BeanRegistration other) {
        Set<String> mergedFactories = new LinkedHashSet<>(factories);
        mergedFactories.addAll(other.factories);
        return new BeanRegistration(conditional || other.conditional, Set.copyOf(mergedFactories));
    }
}
