package io.mango.authorization.resource.sync;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import io.mango.authorization.api.command.ApiResourceRegisterCommand;
import io.mango.resource.support.ResourceProvider;
import io.mango.resource.support.ResourceTypes;
import io.mango.resource.support.declaration.ResourceDeclarationLoader;
import io.mango.resource.support.model.ResourceDeclaration;
import org.springframework.util.AntPathMatcher;

import java.util.List;

/**
 * 将 Spring MVC API 访问声明提供给 mango-resource。
 */
public class ApiAccessResourceProvider implements ResourceProvider {

    private static final AntPathMatcher PATH_MATCHER = new AntPathMatcher();

    private final ApiAccessResourceDiscoverer discoverer;
    private final String providerModuleCode;
    private final ApiResourceDeclarationConverter converter;
    private final ResourceDeclarationLoader declarationLoader;

    public ApiAccessResourceProvider(ApiAccessResourceDiscoverer discoverer,
                                     ApiResourceSyncProperties properties,
                                     ApiResourceDeclarationConverter converter) {
        this(discoverer, properties, converter, null);
    }

    @SuppressFBWarnings(value = "EI_EXPOSE_REP2",
            justification = "Spring injects managed declaration loader; copying it is not valid")
    public ApiAccessResourceProvider(ApiAccessResourceDiscoverer discoverer,
                                     ApiResourceSyncProperties properties,
                                     ApiResourceDeclarationConverter converter,
                                     ResourceDeclarationLoader declarationLoader) {
        this.discoverer = discoverer;
        this.providerModuleCode = properties.getProviderModuleCode();
        this.converter = converter;
        this.declarationLoader = declarationLoader;
    }

    @Override
    public boolean participatesInBootstrap() {
        return false;
    }

    @Override
    public List<String> moduleCodes() {
        return List.of(providerModuleCode);
    }

    @Override
    public List<ResourceDeclaration> provide() {
        List<ResourceDeclaration> fixedDeclarations = fixedDeclarations();
        return discoverer.discover().stream()
                .filter(command -> !isCoveredByFixedDeclaration(command, fixedDeclarations))
                .map(command -> converter.toDeclaration(command, providerModuleCode))
                .toList();
    }

    private List<ResourceDeclaration> fixedDeclarations() {
        if (declarationLoader == null) {
            return List.of();
        }
        return declarationLoader.load().stream()
                .filter(declaration -> ResourceTypes.API_RESOURCE.equals(declaration.getResourceType()))
                .toList();
    }

    private boolean isCoveredByFixedDeclaration(
            ApiResourceRegisterCommand command,
            List<ResourceDeclaration> fixedDeclarations) {
        return fixedDeclarations.stream().anyMatch(declaration -> sameModule(declaration, command)
                && methodMatches(field(declaration, "httpMethod"), command.getHttpMethod())
                && pathMatches(field(declaration, "pathPattern"), command.getPathPattern()));
    }

    private boolean sameModule(ResourceDeclaration declaration,
                               ApiResourceRegisterCommand command) {
        String moduleName = field(declaration, "moduleName");
        return moduleName != null && moduleName.equals(command.getModuleName());
    }

    private boolean methodMatches(String fixedMethod, String scannedMethod) {
        return fixedMethod != null && scannedMethod != null
                && ("ALL".equalsIgnoreCase(fixedMethod)
                || "ALL".equalsIgnoreCase(scannedMethod)
                || fixedMethod.equalsIgnoreCase(scannedMethod));
    }

    private boolean pathMatches(String fixedPath, String scannedPath) {
        return fixedPath != null && scannedPath != null
                && (fixedPath.equals(scannedPath)
                || PATH_MATCHER.match(fixedPath, scannedPath)
                || PATH_MATCHER.match(scannedPath, fixedPath));
    }

    private String field(ResourceDeclaration declaration, String fieldName) {
        if (declaration.getFields() == null || declaration.getFields().get(fieldName) == null
                || declaration.getFields().get(fieldName).getValue() == null) {
            return null;
        }
        return String.valueOf(declaration.getFields().get(fieldName).getValue());
    }
}
