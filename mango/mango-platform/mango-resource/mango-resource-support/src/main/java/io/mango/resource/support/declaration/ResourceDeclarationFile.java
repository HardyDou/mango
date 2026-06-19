package io.mango.resource.support.declaration;

import com.fasterxml.jackson.annotation.JsonAlias;
import io.mango.resource.api.model.ResourceDeclaration;
import lombok.Data;

import java.util.List;
import java.util.Map;

/**
 * classpath 资源声明文件根对象。
 */
@Data
public class ResourceDeclarationFile {

    private Mango mango;

    @Data
    public static class Mango {
        private Resource resource;
    }

    @Data
    public static class Resource {
        @JsonAlias("schema-version")
        private Integer schemaVersion;
        @JsonAlias("module-code")
        private String moduleCode;
        @JsonAlias("module-name")
        private String moduleName;
        private Map<String, List<ResourceDeclaration>> declarations;
    }
}
