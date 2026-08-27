package io.mango.resource.support.declaration;

import com.fasterxml.jackson.annotation.JsonAlias;
import io.mango.resource.support.model.ResourceDeclaration;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * classpath 资源声明文件根对象。
 */
@Data
public class ResourceDeclarationFile {

    private Mango mango;

    public Mango getMango() {
        if (mango == null) {
            return null;
        }
        return mango.copy();
    }

    public void setMango(Mango mango) {
        if (mango == null) {
            this.mango = null;
            return;
        }
        this.mango = mango.copy();
    }

    @Data
    public static class Mango {
        private Resource resource;

        public Resource getResource() {
            if (resource == null) {
                return null;
            }
            return resource.copy();
        }

        public void setResource(Resource resource) {
            if (resource == null) {
                this.resource = null;
                return;
            }
            this.resource = resource.copy();
        }

        private Mango copy() {
            Mango copy = new Mango();
            if (resource != null) {
                copy.resource = resource.copy();
            }
            return copy;
        }
    }

    @Data
    @EqualsAndHashCode(doNotUseGetters = true)
    public static class Resource {
        @JsonAlias("schema-version")
        private Integer schemaVersion;
        @JsonAlias("module-code")
        private String moduleCode;
        @JsonAlias("module-name")
        private String moduleName;
        @JsonAlias({"module-dependencies", "moduleDependencies"})
        private List<String> dependencies;
        private Map<String, List<ResourceDeclaration>> declarations;

        public List<String> getDependencies() {
            return dependencies == null ? null : List.copyOf(dependencies);
        }

        public void setDependencies(List<String> dependencies) {
            this.dependencies = dependencies == null ? null : List.copyOf(dependencies);
        }

        public Map<String, List<ResourceDeclaration>> getDeclarations() {
            return copyDeclarations(declarations);
        }

        public void setDeclarations(Map<String, List<ResourceDeclaration>> declarations) {
            this.declarations = copyDeclarations(declarations);
        }

        private Resource copy() {
            Resource copy = new Resource();
            copy.schemaVersion = schemaVersion;
            copy.moduleCode = moduleCode;
            copy.moduleName = moduleName;
            copy.dependencies = dependencies == null ? null : List.copyOf(dependencies);
            copy.declarations = copyDeclarations(declarations);
            return copy;
        }

        private static Map<String, List<ResourceDeclaration>> copyDeclarations(
                Map<String, List<ResourceDeclaration>> source) {
            Map<String, List<ResourceDeclaration>> copy = new LinkedHashMap<>();
            if (source == null) {
                return copy;
            }
            source.forEach((resourceType, declarations) ->
                    copy.put(resourceType, copyDeclarationList(declarations)));
            return copy;
        }

        private static List<ResourceDeclaration> copyDeclarationList(List<ResourceDeclaration> declarations) {
            if (declarations == null) {
                return List.of();
            }
            return declarations.stream()
                    .map(ResourceDeclaration::copy)
                    .toList();
        }
    }
}
