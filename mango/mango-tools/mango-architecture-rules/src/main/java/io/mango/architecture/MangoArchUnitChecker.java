package io.mango.architecture;

import com.tngtech.archunit.core.domain.JavaAnnotation;
import com.tngtech.archunit.core.domain.JavaClass;
import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.domain.JavaConstructorCall;
import com.tngtech.archunit.core.domain.JavaField;
import com.tngtech.archunit.core.domain.JavaGenericArrayType;
import com.tngtech.archunit.core.domain.JavaMethod;
import com.tngtech.archunit.core.domain.JavaModifier;
import com.tngtech.archunit.core.domain.JavaParameter;
import com.tngtech.archunit.core.domain.JavaParameterizedType;
import com.tngtech.archunit.core.domain.JavaType;
import com.tngtech.archunit.core.importer.ClassFileImporter;

import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.function.Predicate;

/** Bytecode architecture checks with optional read-only contract context directories. */
public final class MangoArchUnitChecker {

    private static final String REST_CONTROLLER =
            "org.springframework.web.bind.annotation.RestController";
    private static final String CONTROLLER = "org.springframework.stereotype.Controller";
    private static final String SERVICE = "org.springframework.stereotype.Service";
    private static final String COMPONENT = "org.springframework.stereotype.Component";
    private static final String BEAN = "org.springframework.context.annotation.Bean";
    private static final String CONDITIONAL_ON_MISSING_BEAN =
            "org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean";
    private static final Set<String> SPRING_MANAGED_METHOD_ANNOTATIONS =
            Set.of(
                    "org.springframework.transaction.annotation.Transactional",
                    "org.springframework.scheduling.annotation.Async",
                    "org.springframework.scheduling.annotation.Scheduled",
                    "org.springframework.cache.annotation.Cacheable",
                    "org.springframework.cache.annotation.CacheEvict",
                    "org.springframework.cache.annotation.CachePut");
    private static final String MAPPER = "org.apache.ibatis.annotations.Mapper";
    private static final String TABLE_NAME = "com.baomidou.mybatisplus.annotation.TableName";
    private static final String FEIGN_CLIENT = "org.springframework.cloud.openfeign.FeignClient";
    private static final String LOCAL_CAPABILITY_CONTRACT =
            "io.mango.common.contract.LocalCapabilityContract";
    private static final String BINARY_HTTP_ADAPTER =
            "io.mango.common.contract.BinaryHttpAdapter";
    private static final String NATIVE_HTTP_ADAPTER =
            "io.mango.common.contract.NativeHttpAdapter";
    private static final String FILE_PREVIEW_VENDOR_PACKAGE_PREFIX = "cn.keking.";
    private static final String MODEL_AND_VIEW = "org.springframework.web.servlet.ModelAndView";
    private static final String SSE_EMITTER =
            "org.springframework.web.servlet.mvc.method.annotation.SseEmitter";
    private static final String RESPONSE_ENTITY = "org.springframework.http.ResponseEntity";
    private static final Set<String> NATIVE_HTTP_BODY_TYPES =
            Set.of(
                    "org.springframework.core.io.InputStreamResource",
                    "org.springframework.core.io.Resource");
    private static final String MANGO_INFRA_PACKAGE_PREFIX = "io.mango.infra.";
    private static final String REQUEST_MAPPING =
            "org.springframework.web.bind.annotation.RequestMapping";
    private static final String REQUEST_BODY =
            "org.springframework.web.bind.annotation.RequestBody";
    private static final String MODEL_ATTRIBUTE =
            "org.springframework.web.bind.annotation.ModelAttribute";
    private static final String PARAMETER_OBJECT = "org.springdoc.core.annotations.ParameterObject";
    private static final String SPRING_QUERY_MAP =
            "org.springframework.cloud.openfeign.SpringQueryMap";
    private static final String GET_MAPPING = "org.springframework.web.bind.annotation.GetMapping";
    private static final Set<String> WRITE_MAPPINGS =
            Set.of(
                    "org.springframework.web.bind.annotation.PostMapping",
                    "org.springframework.web.bind.annotation.PutMapping",
                    "org.springframework.web.bind.annotation.PatchMapping",
                    "org.springframework.web.bind.annotation.DeleteMapping");
    private static final Set<String> HTTP_MAPPINGS =
            Set.of(
                    "org.springframework.web.bind.annotation.RequestMapping",
                    "org.springframework.web.bind.annotation.GetMapping",
                    "org.springframework.web.bind.annotation.PostMapping",
                    "org.springframework.web.bind.annotation.PutMapping",
                    "org.springframework.web.bind.annotation.PatchMapping",
                    "org.springframework.web.bind.annotation.DeleteMapping");
    private static final String BODY_BINDING = "BODY";
    private static final String QUERY_BINDING = "QUERY";
    private static final String HEADER_BINDING = "HEADER";
    private static final String QUERY_OBJECT_BINDING = "QUERY_OBJECT";
    private static final Map<String, String> PARAMETER_BINDINGS =
            Map.of(
                    REQUEST_BODY,
                    BODY_BINDING,
                    "org.springframework.web.bind.annotation.RequestParam",
                    QUERY_BINDING,
                    "org.springframework.web.bind.annotation.RequestHeader",
                    HEADER_BINDING,
                    MODEL_ATTRIBUTE,
                    QUERY_OBJECT_BINDING,
                    PARAMETER_OBJECT,
                    QUERY_OBJECT_BINDING,
                    SPRING_QUERY_MAP,
                    QUERY_OBJECT_BINDING);
    private static final String PERSISTENCE_API_PACKAGE = "io.mango.infra.persistence.api.";
    private static final String PERSISTENCE_BASE_PACKAGE = PERSISTENCE_API_PACKAGE + "entity.";
    private static final String TENANT_ENTITY = PERSISTENCE_BASE_PACKAGE + "TenantEntity";
    private static final String BASE_ENTITY = PERSISTENCE_BASE_PACKAGE + "BaseEntity";
    private static final String BASE_MAPPER = "com.baomidou.mybatisplus.core.mapper.BaseMapper";
    private static final String MANGO_CRUD_SERVICE =
            "io.mango.infra.persistence.api.crud.MangoCrudService";
    private static final String MANGO_TYPED_CRUD_SERVICE =
            "io.mango.infra.persistence.api.crud.MangoTypedCrudService";
    private static final String MANGO_CRUD_SERVICE_IMPL =
            "io.mango.infra.persistence.api.crud.MangoCrudServiceImpl";
    private static final String MYBATIS_SERVICE_IMPL =
            "com.baomidou.mybatisplus.extension.service.impl.ServiceImpl";
    private static final String CONTROLLER_KIND = "Controller";
    private static final String FEIGN_KIND = "FeignClient";
    private static final String ROOT_PATH = "/";
    private static final String REQUEST_MAPPING_SIMPLE_NAME = "RequestMapping";
    private static final String MAPPING_SUFFIX = "Mapping";
    private static final String AMBIGUOUS_HTTP_METHOD = "AMBIGUOUS";
    private static final String COMMAND_SUFFIX = "Command";
    private static final String REQUEST_SUFFIX = "Request";
    private static final String QUERY_SUFFIX = "Query";
    private static final String RESPONSE_SUFFIX = "Response";
    private static final String VIEW_SUFFIX = "VO";
    private static final String SERVICE_SUFFIX = "Service";
    private static final String SERVICE_IMPL_SUFFIX = "ServiceImpl";
    private static final String ENTITY_SUFFIX = "Entity";
    private static final String MAPPER_SUFFIX = "Mapper";
    private static final String JAVA_LONG = "java.lang.Long";
    private static final String MANGO_CRUD_SERVICE_SIMPLE = "MangoCrudService";
    private static final String MANGO_TYPED_CRUD_SERVICE_SIMPLE = "MangoTypedCrudService";
    private static final String CREATE_PREFIX = "Create";
    private static final String UPDATE_PREFIX = "Update";
    private static final int TYPED_CRUD_ARGUMENT_COUNT = 6;
    private static final int CRUD_BASE_ARGUMENT_COUNT = 2;
    private static final int QUERY_ARGUMENT_INDEX = 3;
    private static final int VIEW_ARGUMENT_INDEX = 4;
    private static final int IDENTIFIER_ARGUMENT_INDEX = 5;
    private static final List<String> MAPPING_CONDITION_ATTRIBUTES =
            List.of("params", "headers", "consumes", "produces");
    private static final Set<String> STANDARD_CRUD_METHODS =
            Set.of("create", "update", "delete", "page", "detail");
    private final Set<String> allowedReverseControllers;
    private final Map<String, String> globalEntityTables;

    public MangoArchUnitChecker() {
        this(Set.of(), Map.of());
    }

    public MangoArchUnitChecker(Set<String> allowedReverseControllers) {
        this(allowedReverseControllers, Map.of());
    }

    public MangoArchUnitChecker(
            Set<String> allowedReverseControllers, Map<String, String> globalEntityTables) {
        this.allowedReverseControllers = Set.copyOf(allowedReverseControllers);
        this.globalEntityTables = Map.copyOf(globalEntityTables);
    }

    public List<ArchitectureIssue> check(Map<Path, ModuleRole> classDirectories) {
        Map<Path, ModuleRole> normalized = normalizeAndValidate(classDirectories);
        return importAndCheck(normalized, Map.of(), Set.of(), false);
    }

    public List<ArchitectureIssue> check(
            Map<Path, ModuleRole> classDirectories,
            Collection<Path> contractContextDirectories) {
        Map<Path, ModuleRole> normalized = normalizeAndValidate(classDirectories);
        return importAndCheck(
                normalized,
                Map.of(),
                normalizeAndValidateContext(contractContextDirectories),
                false);
    }

    public List<ArchitectureIssue> check(
            Map<Path, ModuleRole> classDirectories, Map<Path, ModuleContract> moduleContracts) {
        Map<Path, ModuleRole> normalized = normalizeAndValidate(classDirectories);
        Map<Path, ModuleContract> normalizedContracts = new LinkedHashMap<>();
        moduleContracts.forEach(
                (path, contract) ->
                        normalizedContracts.put(path.toAbsolutePath().normalize(), contract));
        return importAndCheck(normalized, normalizedContracts, Set.of(), true);
    }

    public List<ArchitectureIssue> check(
            Map<Path, ModuleRole> classDirectories,
            Map<Path, ModuleContract> moduleContracts,
            Collection<Path> contractContextDirectories) {
        Map<Path, ModuleRole> normalized = normalizeAndValidate(classDirectories);
        Map<Path, ModuleContract> normalizedContracts = new LinkedHashMap<>();
        moduleContracts.forEach(
                (path, contract) ->
                        normalizedContracts.put(path.toAbsolutePath().normalize(), contract));
        return importAndCheck(
                normalized,
                normalizedContracts,
                normalizeAndValidateContext(contractContextDirectories),
                true);
    }

    private List<ArchitectureIssue> importAndCheck(
            Map<Path, ModuleRole> normalized,
            Map<Path, ModuleContract> moduleContracts,
            Set<Path> contractContextDirectories,
            boolean requireFeignContract) {
        JavaClasses classes;
        try {
            Set<Path> importDirectories = new LinkedHashSet<>(normalized.keySet());
            importDirectories.addAll(contractContextDirectories);
            classes = new ClassFileImporter().importPaths(importDirectories);
        } catch (RuntimeException exception) {
            throw new IllegalStateException(
                    "MANGO-ARCH-ENGINE-002 ArchUnit failed to import Reactor bytecode", exception);
        }
        if (classes.isEmpty()) {
            throw new IllegalStateException("MANGO-ARCH-ENGINE-003 ArchUnit imported zero classes");
        }
        return check(
                classes,
                javaClass -> roleOf(javaClass, normalized),
                javaClass -> valueOf(javaClass, moduleContracts),
                javaClass -> valueOf(javaClass, normalized) != null,
                requireFeignContract);
    }

    public List<ArchitectureIssue> check(
            JavaClasses classes, Function<JavaClass, ModuleRole> roleResolver) {
        return check(classes, roleResolver, ignored -> null, ignored -> true, false);
    }

    public List<ArchitectureIssue> check(
            JavaClasses classes,
            Function<JavaClass, ModuleRole> roleResolver,
            Function<JavaClass, ModuleContract> contractResolver) {
        return check(classes, roleResolver, contractResolver, ignored -> true, true);
    }

    private List<ArchitectureIssue> check(
            JavaClasses classes,
            Function<JavaClass, ModuleRole> roleResolver,
            Function<JavaClass, ModuleContract> contractResolver,
            Predicate<JavaClass> governedClass,
            boolean requireFeignContract) {
        List<ArchitectureIssue> issues = new ArrayList<>();
        Map<String, JavaClass> feignContexts = new LinkedHashMap<>();
        List<JavaClass> orderedClasses = orderedClasses(classes).stream()
                .filter(this::isMangoOwnedType)
                .filter(governedClass)
                .toList();
        Map<String, BeanRegistration> beanRegistrations = beanRegistrations(orderedClasses);
        for (JavaClass javaClass : orderedClasses) {
            ModuleRole role = roleResolver.apply(javaClass);
            checkModuleContent(javaClass, role, issues);
            checkApiContractType(javaClass, role, issues);
            checkServiceContractType(javaClass, role, issues);
            checkControllerType(javaClass, role, contractResolver, requireFeignContract, issues);
            checkServiceType(javaClass, role, beanRegistrations, issues);
            checkSpringManagedAnnotations(javaClass, beanRegistrations, issues);
            checkManualServiceConstruction(javaClass, beanRegistrations, issues);
            checkStaticServiceLocator(javaClass, issues);
            checkMapperType(javaClass, role, issues);
            checkEntityType(javaClass, role, issues);
            checkFeignType(
                    javaClass, role, contractResolver, requireFeignContract, feignContexts, issues);
        }
        checkAdapterEndpointParity(orderedClasses, issues);
        issues.sort(
                Comparator.comparing(ArchitectureIssue::ruleId)
                        .thenComparing(ArchitectureIssue::subject));
        return List.copyOf(issues);
    }

    private List<JavaClass> orderedClasses(JavaClasses classes) {
        List<JavaClass> ordered = new ArrayList<>();
        classes.forEach(ordered::add);
        ordered.sort(Comparator.comparing(JavaClass::getName));
        return ordered;
    }

    private Map<String, BeanRegistration> beanRegistrations(List<JavaClass> classes) {
        Map<String, BeanRegistration> registrations = new LinkedHashMap<>();
        for (JavaClass javaClass : classes) {
            for (JavaMethod method : javaClass.getMethods()) {
                if (!method.isAnnotatedWith(BEAN)) {
                    continue;
                }
                boolean conditional = method.isAnnotatedWith(CONDITIONAL_ON_MISSING_BEAN);
                String factory = method.getFullName();
                registerBean(registrations, method.getRawReturnType(), conditional, factory);
                for (JavaConstructorCall call : method.getConstructorCallsFromSelf()) {
                    registerBean(registrations, call.getTargetOwner(), conditional, factory);
                }
            }
        }
        return Map.copyOf(registrations);
    }

    private void registerBean(
            Map<String, BeanRegistration> registrations,
            JavaClass type,
            boolean conditional,
            String factory) {
        if (type.isPrimitive() || Void.TYPE.getName().equals(type.getName())) {
            return;
        }
        registrations.merge(
                type.getName(),
                new BeanRegistration(conditional, Set.of(factory)),
                (left, right) -> left.merge(right));
    }

    private void checkApiContractType(
            JavaClass javaClass, ModuleRole role, List<ArchitectureIssue> issues) {
        if (!isApiContract(javaClass)) {
            return;
        }
        if (role != ModuleRole.API) {
            add(issues, "MANGO-ARCH-TYPE-009", javaClass, "XxxApi contract must be located in api");
        }
        if (!javaClass.getRawInterfaces().isEmpty()) {
            add(
                    issues,
                    "MANGO-ARCH-API-007",
                    javaClass,
                    "XxxApi must not inherit another interface; declare the complete contract"
                            + " directly");
        }
    }

    private void checkServiceContractType(
            JavaClass javaClass, ModuleRole role, List<ArchitectureIssue> issues) {
        if (!isServiceContract(javaClass)) {
            return;
        }
        if (role != ModuleRole.CORE) {
            add(
                    issues,
                    "MANGO-ARCH-TYPE-011",
                    javaClass,
                    "IXxxService contract must be located in core");
        }
        if (hasUnsupportedServiceParent(javaClass)) {
            add(
                    issues,
                    "MANGO-ARCH-SVC-016",
                    javaClass,
                    "IXxxService may inherit only canonical Mango CRUD contracts");
        }
    }

    private boolean hasUnsupportedServiceParent(JavaClass javaClass) {
        return javaClass.getRawInterfaces().stream()
                .anyMatch(
                        parent ->
                                !MANGO_CRUD_SERVICE.equals(parent.getName())
                                        && !MANGO_TYPED_CRUD_SERVICE.equals(parent.getName()));
    }

    private void checkControllerType(
            JavaClass javaClass,
            ModuleRole role,
            Function<JavaClass, ModuleContract> contractResolver,
            boolean requireContract,
            List<ArchitectureIssue> issues) {
        if (!isController(javaClass)) {
            return;
        }
        checkController(
                javaClass, role, contractResolver.apply(javaClass), requireContract, issues);
    }

    private void checkServiceType(
            JavaClass javaClass,
            ModuleRole role,
            Map<String, BeanRegistration> beanRegistrations,
            List<ArchitectureIssue> issues) {
        if (isServiceImplementation(javaClass)) {
            checkService(javaClass, role, beanRegistrations, issues);
        }
    }

    private void checkMapperType(
            JavaClass javaClass, ModuleRole role, List<ArchitectureIssue> issues) {
        if (!isMapper(javaClass)) {
            return;
        }
        if (role != ModuleRole.CORE) {
            add(issues, "MANGO-ARCH-TYPE-006", javaClass, "Mapper must be located in core");
        }
        checkMapper(javaClass, issues);
    }

    private void checkEntityType(
            JavaClass javaClass, ModuleRole role, List<ArchitectureIssue> issues) {
        if (!isEntity(javaClass)) {
            return;
        }
        if (role != ModuleRole.CORE && !javaClass.getName().startsWith(PERSISTENCE_BASE_PACKAGE)) {
            add(issues, "MANGO-ARCH-TYPE-007", javaClass, "Entity must be located in core");
        }
        checkEntity(javaClass, issues);
    }

    private void checkFeignType(
            JavaClass javaClass,
            ModuleRole role,
            Function<JavaClass, ModuleContract> contractResolver,
            boolean requireContract,
            Map<String, JavaClass> feignContexts,
            List<ArchitectureIssue> issues) {
        if (!javaClass.isAnnotatedWith(FEIGN_CLIENT)) {
            return;
        }
        if (role != ModuleRole.STARTER_REMOTE) {
            add(
                    issues,
                    "MANGO-ARCH-FEIGN-001",
                    javaClass,
                    "FeignClient must be located in starter-remote");
        }
        checkFeign(
                javaClass,
                contractResolver.apply(javaClass),
                requireContract,
                feignContexts,
                issues);
    }

    private Map<Path, ModuleRole> normalizeAndValidate(Map<Path, ModuleRole> classDirectories) {
        if (classDirectories == null || classDirectories.isEmpty()) {
            throw new IllegalStateException(
                    "MANGO-ARCH-ENGINE-003 no Reactor class directories configured");
        }
        Map<Path, ModuleRole> result = new LinkedHashMap<>();
        classDirectories.forEach(
                (path, role) -> {
                    Path normalized = path.toAbsolutePath().normalize();
                    if (!Files.isDirectory(normalized)) {
                        throw new IllegalStateException(
                                "MANGO-ARCH-ENGINE-003 missing compiled class directory: "
                                        + normalized);
                    }
                    result.put(normalized, role);
                });
        return result;
    }

    private Set<Path> normalizeAndValidateContext(Collection<Path> classDirectories) {
        if (classDirectories == null || classDirectories.isEmpty()) {
            return Set.of();
        }
        Set<Path> result = new LinkedHashSet<>();
        for (Path path : classDirectories) {
            Path normalized = path.toAbsolutePath().normalize();
            if (!Files.isDirectory(normalized)) {
                throw new IllegalStateException(
                        "MANGO-ARCH-ENGINE-003 missing compiled class directory: " + normalized);
            }
            result.add(normalized);
        }
        return Set.copyOf(result);
    }

    private ModuleRole roleOf(JavaClass javaClass, Map<Path, ModuleRole> roots) {
        ModuleRole role = valueOf(javaClass, roots);
        if (role == null) {
            throw new IllegalStateException(
                    "MANGO-ARCH-ENGINE-004 class is outside Reactor roots: " + javaClass.getName());
        }
        return role;
    }

    private <T> T valueOf(JavaClass javaClass, Map<Path, T> roots) {
        URI uri =
                javaClass
                        .getSource()
                        .orElseThrow(
                                () ->
                                        new IllegalStateException(
                                                "MANGO-ARCH-ENGINE-004 class has no bytecode"
                                                        + " source: "
                                                        + javaClass.getName()))
                        .getUri();
        Path source = Path.of(uri).toAbsolutePath().normalize();
        return roots.entrySet().stream()
                .filter(entry -> source.startsWith(entry.getKey()))
                .map(Map.Entry::getValue)
                .findFirst()
                .orElse(null);
    }

    private void checkController(
            JavaClass javaClass,
            ModuleRole role,
            ModuleContract contract,
            boolean requireContract,
            List<ArchitectureIssue> issues) {
        boolean nativeHttpAdapter = isNativeHttpAdapter(javaClass);
        checkControllerAnnotation(javaClass, issues);
        checkControllerRole(javaClass, role, issues);
        checkControllerRootMapping(javaClass, issues);
        checkControllerInheritance(javaClass, issues);
        if (!isBinaryHttpAdapter(javaClass) && !nativeHttpAdapter) {
            checkControllerApi(javaClass, issues);
        }
        if (requireContract && !nativeHttpAdapter && !hasValidControllerRoot(javaClass, contract)) {
            add(
                    issues,
                    "MANGO-ARCH-CTRL-008",
                    javaClass,
                    "Controller requires an explicit root path matching module-path");
        }
        directApi(javaClass)
                .filter(JavaClass::isFullyImported)
                .ifPresent(
                        api ->
                                checkAdapterMethods(
                                        javaClass,
                                        api,
                                        "MANGO-ARCH-CTRL-005",
                                        CONTROLLER_KIND,
                                        issues));
        if (!nativeHttpAdapter) {
            checkControllerFields(javaClass, issues);
        }
    }

    private void checkControllerAnnotation(JavaClass javaClass, List<ArchitectureIssue> issues) {
        if (!javaClass.isAnnotatedWith(REST_CONTROLLER)) {
            add(
                    issues,
                    "MANGO-ARCH-CTRL-011",
                    javaClass,
                    "Controller must declare canonical @RestController directly; composed or MVC"
                            + " variants are forbidden");
        }
    }

    private void checkControllerRole(
            JavaClass javaClass, ModuleRole role, List<ArchitectureIssue> issues) {
        if (role == ModuleRole.STARTER) {
            return;
        }
        if (role == ModuleRole.STARTER_REMOTE
                && allowedReverseControllers.contains(javaClass.getName())) {
            return;
        }
        add(issues, "MANGO-ARCH-TYPE-001", javaClass, "Controller must be located in starter");
    }

    private void checkControllerRootMapping(JavaClass javaClass, List<ArchitectureIssue> issues) {
        javaClass
                .tryGetAnnotationOfType(REQUEST_MAPPING)
                .filter(annotation -> hasUnsupportedMappingConditions(annotation, true))
                .ifPresent(
                        annotation ->
                                add(
                                        issues,
                                        "MANGO-ARCH-CTRL-012",
                                        javaClass,
                                        "Controller root mapping may declare only one path"));
    }

    private void checkControllerInheritance(JavaClass javaClass, List<ArchitectureIssue> issues) {
        javaClass
                .getRawSuperclass()
                .filter(superclass -> !Object.class.getName().equals(superclass.getName()))
                .ifPresent(
                        superclass ->
                                add(
                                        issues,
                                        "MANGO-ARCH-CTRL-009",
                                        javaClass,
                                        "Controller inheritance is forbidden; compose a service"
                                                + " interface instead of extending "
                                                + superclass.getName()));
    }

    private void checkControllerApi(JavaClass javaClass, List<ArchitectureIssue> issues) {
        long apiCount = javaClass.getRawInterfaces().stream().filter(this::isApiContract).count();
        if (apiCount != 1 || javaClass.getRawInterfaces().size() != 1) {
            add(
                    issues,
                    "MANGO-ARCH-TYPE-002",
                    javaClass,
                    "Controller must directly implement exactly one domain XxxApi interface");
        }
    }

    private boolean hasValidControllerRoot(JavaClass javaClass, ModuleContract contract) {
        if (contract == null || !contract.hasModuleIdentity()) {
            return false;
        }
        List<String> rootPaths =
                javaClass
                        .tryGetAnnotationOfType(REQUEST_MAPPING)
                        .map(this::configuredPaths)
                        .orElse(List.of());
        if (rootPaths.size() != 1) {
            return false;
        }
        String rootPath = normalizeEndpointPath(rootPaths.get(0));
        return contract.modulePathPrefixes().stream()
                .anyMatch(prefix -> matchesControllerRoot(rootPath, prefix));
    }

    private boolean matchesControllerRoot(String rootPath, String modulePrefix) {
        if (matchesPathPrefix(rootPath, modulePrefix)) {
            return true;
        }
        String reversePrefix = "/_" + modulePrefix.substring(1);
        return matchesPathPrefix(rootPath, reversePrefix);
    }

    private boolean matchesPathPrefix(String path, String prefix) {
        return path.equals(prefix) || path.startsWith(prefix + ROOT_PATH);
    }

    private void checkControllerFields(JavaClass javaClass, List<ArchitectureIssue> issues) {
        for (JavaField field : javaClass.getAllFields()) {
            JavaClass type = field.getRawType();
            String name = type.getSimpleName();
            boolean allowedServicePort =
                    !field.getModifiers().contains(JavaModifier.STATIC)
                            && isInterfaceOrExternalStub(type)
                            && (name.matches("I[A-Z].*Service")
                                    || (type.getName().contains(".support.")
                                            && name.endsWith("Executor")));
            if (!allowedServicePort) {
                issues.add(
                        new ArchitectureIssue(
                                "MANGO-ARCH-TYPE-003",
                                field.getFullName(),
                                "Controller field must depend on a service interface, not "
                                        + type.getName()));
            }
        }
    }

    private void checkFeign(
            JavaClass javaClass,
            ModuleContract contract,
            boolean requireContract,
            Map<String, JavaClass> contexts,
            List<ArchitectureIssue> issues) {
        if (!isBinaryHttpAdapter(javaClass) && !isNativeHttpAdapter(javaClass)) {
            checkFeignStructure(javaClass, issues);
            directApi(javaClass)
                    .ifPresent(
                            api ->
                                    checkAdapterMethods(
                                            javaClass,
                                            api,
                                            "MANGO-ARCH-FEIGN-008",
                                            FEIGN_KIND,
                                            issues));
        }
        JavaAnnotation<JavaClass> annotation = javaClass.getAnnotationOfType(FEIGN_CLIENT);
        FeignMetadata metadata =
                new FeignMetadata(
                        annotation.get("name").orElse(null),
                        annotation.get("contextId").orElse(null),
                        annotation.get("path").orElse(null));
        checkFeignRequiredMetadata(javaClass, metadata, issues);
        checkFeignContext(javaClass, contract, metadata.contextId(), contexts, issues);
        checkFeignContract(javaClass, contract, requireContract, metadata, issues);
    }

    private void checkFeignStructure(JavaClass javaClass, List<ArchitectureIssue> issues) {
        if (!javaClass.getFields().isEmpty()) {
            add(
                    issues,
                    "MANGO-ARCH-FEIGN-009",
                    javaClass,
                    "FeignClient must not declare fields or constants");
        }
        long apiCount =
                javaClass.getRawInterfaces().stream()
                        .filter(this::isApiContract)
                        .count();
        if (apiCount != 1 || javaClass.getRawInterfaces().size() != 1) {
            add(
                    issues,
                    "MANGO-ARCH-FEIGN-002",
                    javaClass,
                    "FeignClient must extend exactly one XxxApi");
        }
    }

    private void checkFeignRequiredMetadata(
            JavaClass javaClass, FeignMetadata metadata, List<ArchitectureIssue> issues) {
        if (!hasText(metadata.name()) || !hasText(metadata.contextId())) {
            add(
                    issues,
                    "MANGO-ARCH-FEIGN-003",
                    javaClass,
                    "FeignClient requires non-empty name and contextId");
        }
        if (!isAbsolutePath(metadata.path())) {
            add(
                    issues,
                    "MANGO-ARCH-FEIGN-004",
                    javaClass,
                    "FeignClient path must be an absolute HTTP path");
        }
    }

    private boolean isAbsolutePath(Object path) {
        return path instanceof String stringPath && stringPath.startsWith(ROOT_PATH);
    }

    private void checkFeignContext(
            JavaClass javaClass,
            ModuleContract contract,
            Object contextId,
            Map<String, JavaClass> contexts,
            List<ArchitectureIssue> issues) {
        if (contextId instanceof String contextText && !contextText.isBlank()) {
            String group = feignContextGroup(javaClass, contract);
            String key = group + "|" + contextText;
            JavaClass existing = contexts.putIfAbsent(key, javaClass);
            if (existing != null) {
                issues.add(
                        new ArchitectureIssue(
                                "MANGO-ARCH-FEIGN-005",
                                existing.getName() + "|" + javaClass.getName(),
                                "FeignClient contextId must be unique in starter-remote"));
            }
            String expected = lowerCamel(javaClass.getSimpleName());
            if (!expected.equals(contextText)) {
                add(
                        issues,
                        "MANGO-ARCH-FEIGN-006",
                        javaClass,
                        "FeignClient contextId must be " + expected);
            }
        }
    }

    private String feignContextGroup(JavaClass javaClass, ModuleContract contract) {
        if (contract == null) {
            return javaClass.getPackageName();
        }
        return contract.artifactId();
    }

    private void checkFeignContract(
            JavaClass javaClass,
            ModuleContract contract,
            boolean requireContract,
            FeignMetadata metadata,
            List<ArchitectureIssue> issues) {
        if (requireContract && !hasModuleIdentity(contract)) {
            addFeignContractIssue(
                    issues,
                    javaClass,
                    contract,
                    "FeignClient requires matching starter module.properties");
            return;
        }
        if (contract == null) {
            return;
        }
        checkFeignModuleName(javaClass, contract, metadata.name(), issues);
        checkFeignModulePath(javaClass, contract, metadata.path(), issues);
    }

    private boolean hasModuleIdentity(ModuleContract contract) {
        return contract != null && contract.hasModuleIdentity();
    }

    private void checkFeignModuleName(
            JavaClass javaClass,
            ModuleContract contract,
            Object name,
            List<ArchitectureIssue> issues) {
        if (name instanceof String nameText && contract.moduleName().equals(nameText)) {
            return;
        }
        addFeignContractIssue(
                issues,
                javaClass,
                contract,
                "FeignClient name must match module-name " + contract.moduleName());
    }

    private void checkFeignModulePath(
            JavaClass javaClass,
            ModuleContract contract,
            Object path,
            List<ArchitectureIssue> issues) {
        List<String> requiredPrefixes = contract.modulePathPrefixes();
        if (path instanceof String pathText
                && requiredPrefixes.stream()
                        .anyMatch(prefix -> matchesPathPrefix(pathText, prefix))) {
            return;
        }
        addFeignContractIssue(
                issues,
                javaClass,
                contract,
                "FeignClient path must start with module-path "
                        + String.join(" or ", requiredPrefixes));
    }

    private String lowerCamel(String value) {
        if (value.isEmpty()) {
            return value;
        }
        return Character.toLowerCase(value.charAt(0)) + value.substring(1);
    }

    private boolean hasText(Object value) {
        return value instanceof String text && !text.isBlank();
    }

    private void checkAdapterMethods(
            JavaClass adapter,
            JavaClass api,
            String ruleId,
            String adapterKind,
            List<ArchitectureIssue> issues) {
        Map<String, JavaMethod> apiMethods = methodMap(api.getAllMethods());
        Set<String> apiSignatures = apiMethods.keySet();
        Map<String, JavaMethod> adapterMethods = methodMap(adapter.getMethods());
        for (String signature : apiSignatures) {
            checkApiMethodRedeclaration(
                    adapter,
                    apiMethods.get(signature),
                    adapterMethods.get(signature),
                    signature,
                    ruleId,
                    adapterKind,
                    issues);
        }
        for (JavaMethod method : adapter.getMethods()) {
            checkAdapterMethod(adapter, method, apiSignatures, ruleId, adapterKind, issues);
        }
    }

    private Map<String, JavaMethod> methodMap(Set<JavaMethod> methods) {
        return methods.stream()
                .collect(
                        java.util.stream.Collectors.toMap(
                                this::methodSignature,
                                Function.identity(),
                                (left, right) -> left,
                                LinkedHashMap::new));
    }

    private void checkApiMethodRedeclaration(
            JavaClass adapter,
            JavaMethod apiMethod,
            JavaMethod adapterMethod,
            String signature,
            String ruleId,
            String adapterKind,
            List<ArchitectureIssue> issues) {
        if (adapterMethod == null || !isHttpMapped(adapterMethod)) {
            issues.add(
                    new ArchitectureIssue(
                            ruleId,
                            adapter.getName() + "#" + signature,
                            adapterKind
                                    + " must redeclare every XxxApi method with an HTTP mapping"));
            return;
        }
        if (!genericMethodContract(adapterMethod).equals(genericMethodContract(apiMethod))) {
            issues.add(
                    new ArchitectureIssue(
                            ruleId,
                            adapter.getName() + "#" + signature,
                            adapterKind
                                    + " method must preserve the exact generic API parameter"
                                    + " and return contract"));
        }
    }

    private void checkAdapterMethod(
            JavaClass adapter,
            JavaMethod method,
            Set<String> apiSignatures,
            String ruleId,
            String adapterKind,
            List<ArchitectureIssue> issues) {
        String signature = methodSignature(method);
        if (isMethodOutsideApi(method, signature, apiSignatures, adapterKind)) {
            issues.add(
                    new ArchitectureIssue(
                            ruleId,
                            adapter.getName() + "#" + signature,
                            adapterKind + " must not declare methods outside XxxApi"));
        }
        if (isHttpMapped(method)) {
            checkMappedAdapterMethod(adapter, method, signature, adapterKind, issues);
        }
        if (FEIGN_KIND.equals(adapterKind)
                && !method.getModifiers().contains(JavaModifier.ABSTRACT)) {
            issues.add(
                    new ArchitectureIssue(
                            "MANGO-ARCH-FEIGN-009",
                            adapter.getName() + "#" + signature,
                            "FeignClient methods must be abstract adapter declarations"));
        }
    }

    private boolean isMethodOutsideApi(
            JavaMethod method, String signature, Set<String> apiSignatures, String adapterKind) {
        if (apiSignatures.contains(signature)) {
            return false;
        }
        if (isHttpMapped(method) || FEIGN_KIND.equals(adapterKind)) {
            return true;
        }
        return CONTROLLER_KIND.equals(adapterKind)
                && method.getModifiers().contains(JavaModifier.PUBLIC);
    }

    private void checkMappedAdapterMethod(
            JavaClass adapter,
            JavaMethod method,
            String signature,
            String adapterKind,
            List<ArchitectureIssue> issues) {
        String methodSubject = adapter.getName() + "#" + signature;
        if (method.isAnnotatedWith(REQUEST_MAPPING)) {
            issues.add(
                    new ArchitectureIssue(
                            "MANGO-ARCH-ADAPTER-004",
                            methodSubject,
                            "Adapter methods must use a concrete HTTP verb annotation, not"
                                    + " @RequestMapping"));
        }
        checkAdapterParameters(adapter, method, signature, adapterKind, issues);
        List<JavaAnnotation<JavaMethod>> mappings = mappingAnnotations(method);
        if (mappings.stream()
                .anyMatch(mapping -> hasUnsupportedMappingConditions(mapping, false))) {
            issues.add(
                    new ArchitectureIssue(
                            "MANGO-ARCH-ADAPTER-006",
                            methodSubject,
                            "HTTP adapter mappings may not declare params, headers, consumes or"
                                    + " produces conditions"));
        }
        if (hasInvalidMappingCardinality(method, mappings)) {
            issues.add(
                    new ArchitectureIssue(
                            "MANGO-ARCH-ADAPTER-003",
                            methodSubject,
                            "HTTP adapter method requires one HTTP verb and at most one path"));
        }
    }

    private void checkAdapterParameters(
            JavaClass adapter,
            JavaMethod method,
            String signature,
            String adapterKind,
            List<ArchitectureIssue> issues) {
        for (JavaParameter parameter : method.getParameters()) {
            String subject = parameterSubject(adapter, signature, parameter);
            if (!hasOneExplicitParameterBinding(parameter)) {
                issues.add(
                        new ArchitectureIssue(
                                "MANGO-ARCH-ADAPTER-002",
                                subject,
                                "Every HTTP adapter parameter requires exactly one explicit"
                                        + " transport binding"));
            }
            if (hasOptionalCommandBody(parameter)) {
                issues.add(
                        new ArchitectureIssue(
                                "MANGO-ARCH-ADAPTER-005",
                                subject,
                                "Command/Request @RequestBody must remain required"));
            }
            if (hasInvalidAdapterBinding(method, parameter, adapterKind)) {
                issues.add(
                        new ArchitectureIssue(
                                "MANGO-ARCH-ADAPTER-007",
                                subject,
                                "HTTP adapter binding is incompatible with its verb, model type"
                                        + " or adapter kind"));
            }
        }
    }

    private String parameterSubject(JavaClass adapter, String signature, JavaParameter parameter) {
        return adapter.getName() + "#" + signature + "[" + parameter.getIndex() + "]";
    }

    private List<JavaAnnotation<JavaMethod>> mappingAnnotations(JavaMethod method) {
        return HTTP_MAPPINGS.stream()
                .filter(method::isAnnotatedWith)
                .map(method::getAnnotationOfType)
                .toList();
    }

    private boolean hasInvalidMappingCardinality(
            JavaMethod method, List<JavaAnnotation<JavaMethod>> mappings) {
        if (mappings.size() != 1) {
            return true;
        }
        return !method.isAnnotatedWith(REQUEST_MAPPING) && hasAmbiguousMapping(mappings.get(0));
    }

    private String methodSignature(JavaMethod method) {
        return method.getName()
                + "("
                + method.getRawParameterTypes().stream()
                        .map(JavaClass::getName)
                        .collect(java.util.stream.Collectors.joining(","))
                + ")";
    }

    private String genericMethodContract(JavaMethod method) {
        return genericTypeSignature(method.getReturnType())
                + "("
                + method.getParameterTypes().stream()
                        .map(this::genericTypeSignature)
                        .collect(java.util.stream.Collectors.joining(","))
                + ")";
    }

    private String genericTypeSignature(JavaType type) {
        if (type instanceof JavaParameterizedType parameterized) {
            return parameterized.toErasure().getName()
                    + "<"
                    + parameterized.getActualTypeArguments().stream()
                            .map(this::genericTypeSignature)
                            .collect(java.util.stream.Collectors.joining(","))
                    + ">";
        }
        if (type instanceof JavaGenericArrayType arrayType) {
            return genericTypeSignature(arrayType.getComponentType()) + "[]";
        }
        return type.getName();
    }

    private boolean hasUnsupportedMappingConditions(
            com.tngtech.archunit.core.domain.JavaAnnotation<?> annotation, boolean classLevel) {
        if (MAPPING_CONDITION_ATTRIBUTES.stream()
                .anyMatch(
                        attribute ->
                                !stringValues(annotation.get(attribute).orElse(null)).isEmpty())) {
            return true;
        }
        return classLevel && !stringValues(annotation.get("method").orElse(null)).isEmpty();
    }

    private boolean isHttpMapped(JavaMethod method) {
        return HTTP_MAPPINGS.stream().anyMatch(method::isAnnotatedWith);
    }

    private record FeignMetadata(Object name, Object contextId, Object path) {}

    private void checkAdapterEndpointParity(
            List<JavaClass> classes, List<ArchitectureIssue> issues) {
        Map<String, List<JavaClass>> controllers = new LinkedHashMap<>();
        Map<String, List<JavaClass>> feigns = new LinkedHashMap<>();
        for (JavaClass javaClass : classes) {
            Optional<JavaClass> api = directApi(javaClass);
            if (api.isEmpty()) {
                continue;
            }
            if (isController(javaClass)) {
                controllers
                        .computeIfAbsent(api.get().getName(), ignored -> new ArrayList<>())
                        .add(javaClass);
            }
            if (javaClass.isAnnotatedWith(FEIGN_CLIENT)) {
                feigns.computeIfAbsent(api.get().getName(), ignored -> new ArrayList<>())
                        .add(javaClass);
            }
        }
        controllers.forEach(
                (apiName, controllerAdapters) -> {
                    for (JavaClass controller : controllerAdapters) {
                        for (JavaClass feign : feigns.getOrDefault(apiName, List.of())) {
                            compareEndpoints(apiName, controller, feign, issues);
                        }
                    }
                });
    }

    private Optional<JavaClass> directApi(JavaClass adapter) {
        List<JavaClass> apis =
                adapter.getRawInterfaces().stream().filter(this::isApiContract).toList();
        if (apis.size() == 1) {
            return Optional.of(apis.get(0));
        }
        return Optional.empty();
    }

    private void compareEndpoints(
            String apiName, JavaClass controller, JavaClass feign, List<ArchitectureIssue> issues) {
        Map<String, String> controllerEndpoints =
                endpointContracts(controller, controllerRoot(controller));
        Map<String, String> feignEndpoints = endpointContracts(feign, feignRoot(feign));
        Set<String> signatures = new java.util.LinkedHashSet<>(controllerEndpoints.keySet());
        signatures.addAll(feignEndpoints.keySet());
        for (String signature : signatures) {
            String controllerEndpoint = controllerEndpoints.get(signature);
            String feignEndpoint = feignEndpoints.get(signature);
            if (!java.util.Objects.equals(controllerEndpoint, feignEndpoint)) {
                issues.add(
                        new ArchitectureIssue(
                                "MANGO-ARCH-ADAPTER-001",
                                controller.getName() + "|" + feign.getName() + "#" + signature,
                                "Controller and FeignClient endpoint mappings differ: controller="
                                        + String.valueOf(controllerEndpoint)
                                        + ", feign="
                                        + String.valueOf(feignEndpoint)));
            }
        }
    }

    private Map<String, String> endpointContracts(JavaClass adapter, String root) {
        Map<String, String> endpoints = new LinkedHashMap<>();
        for (JavaMethod method : adapter.getMethods()) {
            Optional<com.tngtech.archunit.core.domain.JavaAnnotation<JavaMethod>> mapping =
                    HTTP_MAPPINGS.stream()
                            .filter(method::isAnnotatedWith)
                            .findFirst()
                            .flatMap(method::tryGetAnnotationOfType);
            mapping.ifPresent(
                    annotation ->
                            endpoints.put(
                                    methodSignature(method),
                                    httpMethod(annotation)
                                            + " "
                                            + joinPath(root, annotationPath(annotation))
                                            + " "
                                            + method.getParameters().stream()
                                                    .map(this::parameterContract)
                                                    .collect(
                                                            java.util.stream.Collectors.joining(
                                                                    ",", "[", "]"))));
        }
        return endpoints;
    }

    private String controllerRoot(JavaClass controller) {
        return controller
                .tryGetAnnotationOfType(REQUEST_MAPPING)
                .map(this::annotationPath)
                .orElse("");
    }

    private String feignRoot(JavaClass feign) {
        return stringValue(feign.getAnnotationOfType(FEIGN_CLIENT).get("path").orElse(""));
    }

    private String annotationPath(com.tngtech.archunit.core.domain.JavaAnnotation<?> annotation) {
        Object path = annotation.get("path").orElse(null);
        String value = stringValue(path);
        if (!value.isEmpty()) {
            return value;
        }
        return stringValue(annotation.get("value").orElse(""));
    }

    private List<String> configuredPaths(
            com.tngtech.archunit.core.domain.JavaAnnotation<?> annotation) {
        List<String> paths = new ArrayList<>(stringValues(annotation.get("path").orElse(null)));
        paths.addAll(stringValues(annotation.get("value").orElse(null)));
        return paths;
    }

    private boolean hasAmbiguousMapping(
            com.tngtech.archunit.core.domain.JavaAnnotation<?> annotation) {
        List<String> paths = stringValues(annotation.get("path").orElse(null));
        if (paths.isEmpty()) {
            paths = stringValues(annotation.get("value").orElse(null));
        }
        if (paths.size() > 1) {
            return true;
        }
        return REQUEST_MAPPING.equals(annotation.getRawType().getName())
                && stringValues(annotation.get("method").orElse(null)).size() != 1;
    }

    private String httpMethod(com.tngtech.archunit.core.domain.JavaAnnotation<?> annotation) {
        String simpleName = annotation.getRawType().getSimpleName();
        if (!REQUEST_MAPPING_SIMPLE_NAME.equals(simpleName)) {
            return simpleName
                    .substring(0, simpleName.length() - MAPPING_SUFFIX.length())
                    .toUpperCase();
        }
        List<String> methods = stringValues(annotation.get("method").orElse(null));
        if (methods.size() == 1) {
            return methods.get(0);
        }
        return AMBIGUOUS_HTTP_METHOD;
    }

    private String stringValue(Object value) {
        if (value instanceof String text) {
            return text;
        }
        if (value instanceof String[] values && values.length > 0) {
            return values[0];
        }
        return "";
    }

    private List<String> stringValues(Object value) {
        if (value instanceof String text) {
            if (text.isBlank()) {
                return List.of();
            }
            return List.of(text);
        }
        if (value instanceof Object[] values) {
            return java.util.Arrays.stream(values)
                    .map(String::valueOf)
                    .filter(text -> !text.isBlank())
                    .toList();
        }
        if (value instanceof Iterable<?> values) {
            List<String> result = new ArrayList<>();
            values.forEach(item -> result.add(String.valueOf(item)));
            return result.stream().filter(text -> !text.isBlank()).toList();
        }
        if (value == null) {
            return List.of();
        }
        return List.of(String.valueOf(value));
    }

    private String joinPath(String root, String relative) {
        String left = normalizeEndpointPath(root);
        String right = normalizeEndpointPath(relative);
        if (left.equals(ROOT_PATH)) {
            return right;
        }
        if (right.equals(ROOT_PATH)) {
            return left;
        }
        return left + right;
    }

    private String normalizeEndpointPath(String value) {
        if (value == null || value.isBlank()) {
            return ROOT_PATH;
        }
        if (ROOT_PATH.equals(value.trim())) {
            return ROOT_PATH;
        }
        String normalized = value.trim();
        if (!normalized.startsWith(ROOT_PATH)) {
            normalized = ROOT_PATH + normalized;
        }
        while (normalized.endsWith(ROOT_PATH) && normalized.length() > 1) {
            normalized = normalized.substring(0, normalized.length() - 1);
        }
        return normalized;
    }

    private Optional<String> parameterBinding(
            com.tngtech.archunit.core.domain.JavaParameter parameter) {
        return PARAMETER_BINDINGS.entrySet().stream()
                .filter(entry -> parameter.isAnnotatedWith(entry.getKey()))
                .map(Map.Entry::getValue)
                .findFirst();
    }

    private boolean hasOneExplicitParameterBinding(
            com.tngtech.archunit.core.domain.JavaParameter parameter) {
        List<Map.Entry<String, String>> bindings =
                PARAMETER_BINDINGS.entrySet().stream()
                        .filter(entry -> parameter.isAnnotatedWith(entry.getKey()))
                        .toList();
        if (bindings.size() != 1) {
            return false;
        }
        Map.Entry<String, String> binding = bindings.get(0);
        if (!requiresNamedBinding(binding.getValue())) {
            return true;
        }
        var annotation = parameter.getAnnotationOfType(binding.getKey());
        return hasText(annotation.get("name").orElse(""))
                || hasText(annotation.get("value").orElse(""));
    }

    private boolean requiresNamedBinding(String binding) {
        return QUERY_BINDING.equals(binding) || HEADER_BINDING.equals(binding);
    }

    private String parameterContract(com.tngtech.archunit.core.domain.JavaParameter parameter) {
        List<Map.Entry<String, String>> bindings =
                PARAMETER_BINDINGS.entrySet().stream()
                        .filter(entry -> parameter.isAnnotatedWith(entry.getKey()))
                        .toList();
        if (bindings.size() != 1) {
            if (bindings.isEmpty()) {
                return "UNBOUND";
            }
            return "MULTI";
        }
        Map.Entry<String, String> binding = bindings.get(0);
        var annotation = parameter.getAnnotationOfType(binding.getKey());
        if (BODY_BINDING.equals(binding.getValue())) {
            return BODY_BINDING
                    + "("
                    + String.valueOf(annotation.get("required").orElse(true))
                    + ")";
        }
        if (!requiresNamedBinding(binding.getValue())) {
            return binding.getValue();
        }
        String name = stringValue(annotation.get("name").orElse(""));
        if (name.isBlank()) {
            name = stringValue(annotation.get("value").orElse(""));
        }
        String required = String.valueOf(annotation.get("required").orElse(""));
        String defaultValue = stringValue(annotation.get("defaultValue").orElse(""));
        return binding.getValue() + "(" + name + "|" + required + "|" + defaultValue + ")";
    }

    private boolean hasOptionalCommandBody(
            com.tngtech.archunit.core.domain.JavaParameter parameter) {
        if (!parameter.isAnnotatedWith(REQUEST_BODY)) {
            return false;
        }
        String simpleName = parameter.getRawType().getSimpleName();
        boolean command = isCommandType(simpleName);
        return command
                && Boolean.FALSE.equals(
                        parameter.getAnnotationOfType(REQUEST_BODY).get("required").orElse(true));
    }

    private boolean hasInvalidAdapterBinding(
            JavaMethod method, JavaParameter parameter, String adapterKind) {
        boolean get = method.isAnnotatedWith(GET_MAPPING);
        boolean write = WRITE_MAPPINGS.stream().anyMatch(method::isAnnotatedWith);
        String simpleName = parameter.getRawType().getSimpleName();
        boolean command = isCommandType(simpleName);
        boolean query = simpleName.endsWith(QUERY_SUFFIX);
        boolean body = parameter.isAnnotatedWith(REQUEST_BODY);
        if (get && body) {
            return true;
        }
        if (get && command) {
            return true;
        }
        if (write && command && !body) {
            return true;
        }
        if (FEIGN_KIND.equals(adapterKind)) {
            return hasInvalidFeignBinding(parameter, query);
        }
        return hasInvalidControllerBinding(parameter, query);
    }

    private boolean isCommandType(String simpleName) {
        return simpleName.endsWith(COMMAND_SUFFIX) || simpleName.endsWith(REQUEST_SUFFIX);
    }

    private boolean hasInvalidFeignBinding(JavaParameter parameter, boolean query) {
        if (parameter.isAnnotatedWith(PARAMETER_OBJECT)
                || parameter.isAnnotatedWith(MODEL_ATTRIBUTE)) {
            return true;
        }
        return query && !parameter.isAnnotatedWith(SPRING_QUERY_MAP);
    }

    private boolean hasInvalidControllerBinding(JavaParameter parameter, boolean query) {
        if (parameter.isAnnotatedWith(SPRING_QUERY_MAP)) {
            return true;
        }
        if (!query) {
            return false;
        }
        if (parameter.isAnnotatedWith(PARAMETER_OBJECT)) {
            return false;
        }
        return !parameter.isAnnotatedWith(MODEL_ATTRIBUTE);
    }

    private void checkService(
            JavaClass javaClass,
            ModuleRole role,
            Map<String, BeanRegistration> beanRegistrations,
            List<ArchitectureIssue> issues) {
        BeanRegistration registration = beanRegistrations.get(javaClass.getName());
        boolean frameworkRegistration = isFrameworkServiceRegistration(javaClass, registration);
        checkServiceRegistration(javaClass, registration, frameworkRegistration, issues);
        if (frameworkRegistration) {
            checkServiceApiBoundary(javaClass, issues);
            return;
        }
        checkServiceLocation(javaClass, role, issues);
        checkServiceNaming(javaClass, issues);
        checkServiceApiBoundary(javaClass, issues);
        CrudFacts facts = crudFacts(javaClass);
        checkServiceInheritance(javaClass, facts, issues);
        checkCrudContract(javaClass, facts, issues);
    }

    private void checkServiceRegistration(
            JavaClass javaClass,
            BeanRegistration registration,
            boolean frameworkRegistration,
            List<ArchitectureIssue> issues) {
        boolean serviceStereotype = javaClass.isAnnotatedWith(SERVICE);
        boolean implementsServiceContract = implementsServiceContract(javaClass);
        if (implementsServiceContract && !frameworkRegistration && !serviceStereotype) {
            add(
                    issues,
                    "MANGO-ARCH-BEAN-001",
                    javaClass,
                    "Business IXxxService implementation requires @Service");
        }
        boolean plainService = !serviceStereotype && !implementsServiceContract;
        boolean missingConditionalRegistration =
                registration == null || !registration.conditional();
        if (plainService && missingConditionalRegistration) {
            add(
                    issues,
                    "MANGO-ARCH-BEAN-002",
                    javaClass,
                    "Plain framework XxxService requires starter @Bean and"
                            + " @ConditionalOnMissingBean registration");
        }
        if (serviceStereotype && registration != null) {
            add(
                    issues,
                    "MANGO-ARCH-BEAN-003",
                    javaClass,
                    "Service must use one registration mechanism; do not combine @Service and"
                            + " @Bean");
        }
    }

    private boolean isFrameworkServiceRegistration(
            JavaClass javaClass, BeanRegistration registration) {
        return !javaClass.isAnnotatedWith(SERVICE)
                && !javaClass.getPackageName().contains(".core.service")
                && (registration != null || !implementsServiceContract(javaClass));
    }

    private boolean implementsServiceContract(JavaClass javaClass) {
        return javaClass.getAllRawInterfaces().stream().anyMatch(this::isServiceContract);
    }

    private void checkSpringManagedAnnotations(
            JavaClass javaClass,
            Map<String, BeanRegistration> beanRegistrations,
            List<ArchitectureIssue> issues) {
        if (javaClass.isInterface()
                || javaClass.isAnnotation()
                || !usesSpringManagedMethodAnnotation(javaClass)
                || isSpringManaged(javaClass, beanRegistrations)) {
            return;
        }
        add(
                issues,
                "MANGO-ARCH-BEAN-005",
                javaClass,
                "@Transactional/@Async/@Scheduled/cache annotations require Spring Bean"
                        + " registration");
    }

    private boolean usesSpringManagedMethodAnnotation(JavaClass javaClass) {
        if (SPRING_MANAGED_METHOD_ANNOTATIONS.stream().anyMatch(javaClass::isAnnotatedWith)) {
            return true;
        }
        return javaClass.getMethods().stream()
                .anyMatch(
                        method ->
                                SPRING_MANAGED_METHOD_ANNOTATIONS.stream()
                                        .anyMatch(method::isAnnotatedWith));
    }

    private boolean isSpringManaged(
            JavaClass javaClass, Map<String, BeanRegistration> beanRegistrations) {
        return beanRegistrations.containsKey(javaClass.getName())
                || isSpringStereotype(javaClass);
    }

    private boolean isSpringStereotype(JavaClass javaClass) {
        if (javaClass.isAnnotatedWith(SERVICE)
                || javaClass.isAnnotatedWith(COMPONENT)
                || javaClass.isAnnotatedWith(CONTROLLER)
                || javaClass.isAnnotatedWith(REST_CONTROLLER)) {
            return true;
        }
        return javaClass.getAnnotations().stream()
                .map(JavaAnnotation::getRawType)
                .anyMatch(
                        annotation ->
                                annotation.isAnnotatedWith(COMPONENT)
                                        || annotation.isAnnotatedWith(SERVICE)
                                        || annotation.isAnnotatedWith(CONTROLLER));
    }

    private void checkManualServiceConstruction(
            JavaClass javaClass,
            Map<String, BeanRegistration> beanRegistrations,
            List<ArchitectureIssue> issues) {
        if (!isController(javaClass) && !isServiceImplementation(javaClass)) {
            return;
        }
        for (JavaConstructorCall call : javaClass.getConstructorCallsFromSelf()) {
            if (call.getOrigin().isAnnotatedWith(BEAN)) {
                continue;
            }
            JavaClass target = call.getTargetOwner();
            if (call.getOrigin().isConstructor()
                    && javaClass.getAllRawSuperclasses().contains(target)) {
                continue;
            }
            if (!isManagedServiceTarget(target, beanRegistrations)) {
                continue;
            }
            issues.add(
                    new ArchitectureIssue(
                            "MANGO-ARCH-BEAN-004",
                            call.getOrigin().getFullName()
                                    + " -> "
                                    + target.getName(),
                            "Controller/business Service must inject Spring-managed Service;"
                                    + " direct construction bypasses proxies and lifecycle"));
        }
    }

    private boolean isManagedServiceTarget(
            JavaClass target, Map<String, BeanRegistration> beanRegistrations) {
        return isServiceImplementation(target) && isSpringManaged(target, beanRegistrations);
    }

    private void checkStaticServiceLocator(
            JavaClass javaClass, List<ArchitectureIssue> issues) {
        if (javaClass.isInterface() || javaClass.isAnnotation()) {
            return;
        }
        for (JavaField field : javaClass.getFields()) {
            if (!field.getModifiers().contains(JavaModifier.STATIC)
                    || field.getModifiers().contains(JavaModifier.FINAL)
                    || !isDomainServiceType(field.getRawType())) {
                continue;
            }
            issues.add(
                    new ArchitectureIssue(
                            "MANGO-ARCH-BEAN-006",
                            field.getFullName(),
                            "Mutable static Service state is a forbidden Service Locator; inject a"
                                    + " Spring Bean instead"));
        }
    }

    private boolean isDomainServiceType(JavaClass type) {
        String simpleName = type.getSimpleName();
        return simpleName.matches("I[A-Z].*Service")
                || (!type.isInterface()
                        && !type.getName().startsWith("java.")
                        && simpleName.endsWith(SERVICE_SUFFIX));
    }

    private void checkServiceLocation(
            JavaClass javaClass, ModuleRole role, List<ArchitectureIssue> issues) {
        if (role != ModuleRole.CORE) {
            add(
                    issues,
                    "MANGO-ARCH-TYPE-004",
                    javaClass,
                    "Service implementation must be located in core");
        }
    }

    private void checkServiceNaming(JavaClass javaClass, List<ArchitectureIssue> issues) {
        boolean implementsServiceContract =
                javaClass.getAllRawInterfaces().stream().anyMatch(this::isServiceContract);
        if (implementsServiceContract
                && !javaClass.getSimpleName().endsWith(SERVICE_SUFFIX)
                && !javaClass.getSimpleName().endsWith(SERVICE_IMPL_SUFFIX)) {
            add(
                    issues,
                    "MANGO-ARCH-SVC-005",
                    javaClass,
                    "IXxxService implementation should be named XxxService; XxxServiceImpl is"
                            + " acceptable");
        }
        if (hasServiceClassName(javaClass) && !implementsNamedService(javaClass)) {
            add(issues, "MANGO-ARCH-TYPE-005", javaClass, "XxxService must implement IXxxService");
        }
    }

    private boolean hasServiceClassName(JavaClass javaClass) {
        String simpleName = javaClass.getSimpleName();
        return simpleName.endsWith(SERVICE_SUFFIX) || simpleName.endsWith(SERVICE_IMPL_SUFFIX);
    }

    private boolean implementsNamedService(JavaClass javaClass) {
        return javaClass.getAllRawInterfaces().stream()
                .anyMatch(
                        type ->
                                type.getSimpleName().startsWith("I")
                                        && type.getSimpleName().endsWith(SERVICE_SUFFIX));
    }

    private void checkServiceApiBoundary(JavaClass javaClass, List<ArchitectureIssue> issues) {
        if (javaClass.getAllRawInterfaces().stream()
                .anyMatch(this::isApiContract)) {
            add(
                    issues,
                    "MANGO-ARCH-TYPE-008",
                    javaClass,
                    "Service implementation must not implement an HTTP API");
        }
    }

    private CrudFacts crudFacts(JavaClass javaClass) {
        String directSuperclass =
                javaClass.getRawSuperclass().map(JavaClass::getName).orElse(Object.class.getName());
        Set<String> declaredMethods =
                javaClass.getMethods().stream()
                        .map(JavaMethod::getName)
                        .collect(java.util.stream.Collectors.toSet());
        return new CrudFacts(
                hasRawInterface(javaClass, MANGO_CRUD_SERVICE),
                hasRawInterface(javaClass, MANGO_TYPED_CRUD_SERVICE),
                hasRawSuperclass(javaClass, MANGO_CRUD_SERVICE_IMPL),
                hasRawSuperclass(javaClass, MYBATIS_SERVICE_IMPL),
                directSuperclass,
                declaredMethods.containsAll(STANDARD_CRUD_METHODS),
                hasSpoofedCrudContract(javaClass),
                hasSpoofedCrudBase(javaClass));
    }

    private boolean hasRawInterface(JavaClass javaClass, String typeName) {
        return javaClass.getAllRawInterfaces().stream()
                .anyMatch(type -> typeName.equals(type.getName()));
    }

    private boolean hasRawSuperclass(JavaClass javaClass, String typeName) {
        return javaClass.getAllRawSuperclasses().stream()
                .anyMatch(type -> typeName.equals(type.getName()));
    }

    private boolean hasSpoofedCrudContract(JavaClass javaClass) {
        return javaClass.getAllRawInterfaces().stream().anyMatch(this::isSpoofedCrudContract);
    }

    private boolean isSpoofedCrudContract(JavaClass type) {
        String simpleName = type.getSimpleName();
        if (!MANGO_CRUD_SERVICE_SIMPLE.equals(simpleName)
                && !MANGO_TYPED_CRUD_SERVICE_SIMPLE.equals(simpleName)) {
            return false;
        }
        return !MANGO_CRUD_SERVICE.equals(type.getName())
                && !MANGO_TYPED_CRUD_SERVICE.equals(type.getName());
    }

    private boolean hasSpoofedCrudBase(JavaClass javaClass) {
        return javaClass.getAllRawSuperclasses().stream()
                .anyMatch(
                        type ->
                                "MangoCrudServiceImpl".equals(type.getSimpleName())
                                        && !MANGO_CRUD_SERVICE_IMPL.equals(type.getName()));
    }

    private void checkServiceInheritance(
            JavaClass javaClass, CrudFacts facts, List<ArchitectureIssue> issues) {
        if (!Object.class.getName().equals(facts.directSuperclass())
                && !MANGO_CRUD_SERVICE_IMPL.equals(facts.directSuperclass())) {
            add(
                    issues,
                    "MANGO-ARCH-SVC-015",
                    javaClass,
                    "Business services may only extend canonical MangoCrudServiceImpl directly");
        }
        if (facts.spoofedCrudContract() || facts.spoofedCrudBase()) {
            add(
                    issues,
                    "MANGO-ARCH-SVC-009",
                    javaClass,
                    "CRUD services must use canonical io.mango.infra.persistence contracts");
        }
        if (facts.mybatisCrudBase() && !facts.crudBase()) {
            add(
                    issues,
                    "MANGO-ARCH-SVC-014",
                    javaClass,
                    "Business services must not extend MyBatis ServiceImpl directly; use"
                            + " MangoCrudServiceImpl");
        }
    }

    private void checkCrudContract(
            JavaClass javaClass, CrudFacts facts, List<ArchitectureIssue> issues) {
        if (facts.crudContract() && !facts.crudBase()) {
            add(
                    issues,
                    "MANGO-ARCH-SVC-007",
                    javaClass,
                    "CRUD service must extend MangoCrudServiceImpl<Mapper, Entity>");
        }
        if (requiresTypedCrud(facts) && !facts.typedCrudContract()) {
            add(
                    issues,
                    "MANGO-ARCH-SVC-008",
                    javaClass,
                    "Business CRUD service must implement MangoTypedCrudService<E,C,U,Q,V,ID>");
        }
        if (facts.typedCrudContract() && !validTypedCrudSignature(javaClass)) {
            add(
                    issues,
                    "MANGO-ARCH-SVC-011",
                    javaClass,
                    "Typed CRUD generics must align Entity, Mapper, Command, Query, VO and ID"
                            + " contracts");
        }
    }

    private boolean requiresTypedCrud(CrudFacts facts) {
        return facts.crudContract() || facts.standardCrudSurface();
    }

    private boolean validTypedCrudSignature(JavaClass service) {
        Optional<JavaParameterizedType> typedContract =
                findParameterizedInterface(service, MANGO_TYPED_CRUD_SERVICE, new HashSet<>());
        Optional<JavaParameterizedType> crudBase =
                service.getSuperclass()
                        .filter(JavaParameterizedType.class::isInstance)
                        .map(JavaParameterizedType.class::cast)
                        .filter(type -> MANGO_CRUD_SERVICE_IMPL.equals(type.toErasure().getName()));
        if (typedContract.isEmpty() || crudBase.isEmpty()) {
            return false;
        }
        if (typedContract.get().getActualTypeArguments().size() != TYPED_CRUD_ARGUMENT_COUNT) {
            return false;
        }
        if (crudBase.get().getActualTypeArguments().size() != CRUD_BASE_ARGUMENT_COUNT) {
            return false;
        }
        List<JavaType> contractTypes = typedContract.get().getActualTypeArguments();
        List<JavaType> baseTypes = crudBase.get().getActualTypeArguments();
        TypedCrudTypes types =
                new TypedCrudTypes(
                        contractTypes.get(0),
                        contractTypes.get(1),
                        contractTypes.get(2),
                        contractTypes.get(QUERY_ARGUMENT_INDEX),
                        contractTypes.get(VIEW_ARGUMENT_INDEX),
                        contractTypes.get(IDENTIFIER_ARGUMENT_INDEX),
                        baseTypes.get(0),
                        baseTypes.get(1));
        if (!hasValidTypedCrudNames(types)) {
            return false;
        }
        return mapperMatchesEntity(types.mapper(), types.entity());
    }

    private boolean hasValidTypedCrudNames(TypedCrudTypes types) {
        if (!types.entity()
                .toErasure()
                .getName()
                .equals(types.baseEntity().toErasure().getName())) {
            return false;
        }
        String aggregate = removeSuffix(types.entity().toErasure().getSimpleName(), ENTITY_SUFFIX);
        if (aggregate.isEmpty()) {
            return false;
        }
        if (!(CREATE_PREFIX + aggregate + COMMAND_SUFFIX)
                .equals(types.create().toErasure().getSimpleName())) {
            return false;
        }
        if (!(UPDATE_PREFIX + aggregate + COMMAND_SUFFIX)
                .equals(types.update().toErasure().getSimpleName())) {
            return false;
        }
        String queryName = types.query().toErasure().getSimpleName();
        if (!queryName.startsWith(aggregate) || !queryName.endsWith(QUERY_SUFFIX)) {
            return false;
        }
        if (!(aggregate + VIEW_SUFFIX).equals(types.view().toErasure().getSimpleName())) {
            return false;
        }
        return JAVA_LONG.equals(types.identifier().toErasure().getName());
    }

    private boolean mapperMatchesEntity(JavaType mapper, JavaType entity) {
        Optional<JavaParameterizedType> baseMapper =
                findParameterizedInterface(mapper.toErasure(), BASE_MAPPER, new HashSet<>());
        return baseMapper
                .filter(type -> type.getActualTypeArguments().size() == 1)
                .map(
                        type ->
                                type.getActualTypeArguments()
                                        .get(0)
                                        .toErasure()
                                        .getName()
                                        .equals(entity.toErasure().getName()))
                .orElse(false);
    }

    private Optional<JavaParameterizedType> findParameterizedInterface(
            JavaClass source, String rawTypeName, Set<String> visited) {
        if (!visited.add(source.getName())) {
            return Optional.empty();
        }
        for (JavaType interfaceType : source.getInterfaces()) {
            if (interfaceType instanceof JavaParameterizedType parameterized
                    && rawTypeName.equals(parameterized.toErasure().getName())) {
                return Optional.of(parameterized);
            }
            Optional<JavaParameterizedType> inherited =
                    findParameterizedInterface(interfaceType.toErasure(), rawTypeName, visited);
            if (inherited.isPresent()) {
                return inherited;
            }
        }
        return Optional.empty();
    }

    private void checkModuleContent(
            JavaClass javaClass, ModuleRole role, List<ArchitectureIssue> issues) {
        if (role != ModuleRole.API) {
            return;
        }
        if (!isAllowedApiType(javaClass)) {
            add(
                    issues,
                    "MANGO-ARCH-TYPE-010",
                    javaClass,
                    "api modules must contain contracts, not local implementation types");
        }
    }

    private boolean isAllowedApiType(JavaClass javaClass) {
        if (javaClass.getName().startsWith(PERSISTENCE_API_PACKAGE)) {
            return true;
        }
        if (isLocalCapabilityContract(javaClass)) {
            return true;
        }
        if (javaClass.isInterface() || javaClass.isEnum() || javaClass.isAnnotation()) {
            return true;
        }
        String name = javaClass.getSimpleName();
        return isCommandType(name)
                || name.endsWith(QUERY_SUFFIX)
                || name.endsWith(VIEW_SUFFIX)
                || name.endsWith(RESPONSE_SUFFIX);
    }

    private void checkEntity(JavaClass javaClass, List<ArchitectureIssue> issues) {
        if (javaClass.getName().startsWith(PERSISTENCE_BASE_PACKAGE)
                || javaClass.getModifiers().contains(JavaModifier.ABSTRACT)) {
            return;
        }
        if (!javaClass.getSimpleName().endsWith(ENTITY_SUFFIX)) {
            add(
                    issues,
                    "MANGO-ARCH-ENTITY-001",
                    javaClass,
                    "Persistent class must be named XxxEntity");
        }
        String tableName =
                javaClass
                        .tryGetAnnotationOfType(TABLE_NAME)
                        .map(annotation -> stringValue(annotation.get("value").orElse("")))
                        .orElse("");
        boolean hasTableName = !tableName.isBlank();
        if (!hasTableName) {
            add(
                    issues,
                    "MANGO-ARCH-ENTITY-002",
                    javaClass,
                    "Business Entity requires non-blank @TableName");
        }
        String approvedGlobalTable = globalEntityTables.get(javaClass.getName());
        if (approvedGlobalTable != null) {
            if (!approvedGlobalTable.equals(tableName)) {
                add(
                        issues,
                        "MANGO-ARCH-ENTITY-004",
                        javaClass,
                        "Global entity exception table must match @TableName "
                                + approvedGlobalTable);
            }
            return;
        }
        if (javaClass.getAllRawSuperclasses().stream()
                .noneMatch(type -> TENANT_ENTITY.equals(type.getName()))) {
            add(
                    issues,
                    "MANGO-ARCH-ENTITY-003",
                    javaClass,
                    "Business Entity must extend canonical Mango TenantEntity");
        }
    }

    private void checkMapper(JavaClass javaClass, List<ArchitectureIssue> issues) {
        if (!javaClass.isInterface() || !javaClass.isAnnotatedWith(MAPPER)) {
            add(
                    issues,
                    "MANGO-ARCH-MAPPER-004",
                    javaClass,
                    "XxxMapper must be an interface annotated with @Mapper");
        }
        Optional<JavaParameterizedType> baseMapper =
                javaClass.getInterfaces().stream()
                        .filter(JavaParameterizedType.class::isInstance)
                        .map(JavaParameterizedType.class::cast)
                        .filter(type -> BASE_MAPPER.equals(type.toErasure().getName()))
                        .findFirst();
        if (baseMapper.isEmpty() || baseMapper.get().getActualTypeArguments().size() != 1) {
            add(
                    issues,
                    "MANGO-ARCH-MAPPER-005",
                    javaClass,
                    "XxxMapper must directly extend BaseMapper<XxxEntity>");
            return;
        }
        JavaType entityType = baseMapper.get().getActualTypeArguments().get(0);
        String mapperAggregate = removeSuffix(javaClass.getSimpleName(), MAPPER_SUFFIX);
        String entityAggregate =
                removeSuffix(entityType.toErasure().getSimpleName(), ENTITY_SUFFIX);
        if (mapperAggregate.isEmpty() || !mapperAggregate.equals(entityAggregate)) {
            add(
                    issues,
                    "MANGO-ARCH-MAPPER-006",
                    javaClass,
                    "XxxMapper aggregate must match BaseMapper<XxxEntity>");
        }
    }

    private String removeSuffix(String value, String suffix) {
        if (!value.endsWith(suffix)) {
            return "";
        }
        return value.substring(0, value.length() - suffix.length());
    }

    private boolean isController(JavaClass javaClass) {
        if (javaClass.isInterface() || javaClass.isAnnotation()) {
            return false;
        }
        if (javaClass.isAnnotatedWith(REST_CONTROLLER) || javaClass.isAnnotatedWith(CONTROLLER)) {
            return true;
        }
        boolean hasControllerStereotype = javaClass.getAnnotations().stream()
                .map(JavaAnnotation::getRawType)
                .anyMatch(this::isControllerStereotype);
        if (hasControllerStereotype) {
            return true;
        }
        return !javaClass.getModifiers().contains(JavaModifier.ABSTRACT)
                && javaClass.getSimpleName().endsWith(CONTROLLER_KIND);
    }

    private boolean isControllerStereotype(JavaClass annotationType) {
        return annotationType.isAnnotatedWith(REST_CONTROLLER)
                || annotationType.isAnnotatedWith(CONTROLLER);
    }

    private boolean isServiceImplementation(JavaClass javaClass) {
        if (javaClass.getModifiers().contains(JavaModifier.ABSTRACT)
                || isLocalCapabilityContract(javaClass)) {
            return false;
        }
        if (javaClass.isAnnotatedWith(SERVICE)
                || javaClass.getSimpleName().endsWith(SERVICE_IMPL_SUFFIX)) {
            return true;
        }
        if (javaClass.isInterface()) {
            return false;
        }
        if (javaClass.getSimpleName().endsWith(SERVICE_SUFFIX)) {
            return true;
        }
        return javaClass.getAllRawInterfaces().stream().anyMatch(this::isServiceContract);
    }

    private boolean isServiceContract(JavaClass javaClass) {
        return javaClass.isInterface()
                && javaClass.getSimpleName().matches("I[A-Z].*Service")
                && !isLocalCapabilityContract(javaClass);
    }

    private boolean isApiContract(JavaClass javaClass) {
        return isInterfaceOrExternalStub(javaClass)
                && javaClass.getSimpleName().endsWith("Api")
                && !isLocalCapabilityContract(javaClass);
    }

    private boolean isLocalCapabilityContract(JavaClass javaClass) {
        return javaClass.getName().startsWith(MANGO_INFRA_PACKAGE_PREFIX)
                && javaClass.isAnnotatedWith(LOCAL_CAPABILITY_CONTRACT);
    }

    private boolean isBinaryHttpAdapter(JavaClass javaClass) {
        return javaClass.isAnnotatedWith(BINARY_HTTP_ADAPTER);
    }

    private boolean isMangoOwnedType(JavaClass javaClass) {
        return !javaClass.getName().startsWith(FILE_PREVIEW_VENDOR_PACKAGE_PREFIX);
    }

    private boolean isNativeHttpAdapter(JavaClass javaClass) {
        if (javaClass.isAnnotatedWith(NATIVE_HTTP_ADAPTER)) {
            return true;
        }
        List<JavaMethod> httpMethods = javaClass.getMethods().stream()
                .filter(this::isHttpMethod)
                .toList();
        return !httpMethods.isEmpty()
                && httpMethods.stream().allMatch(this::isNativeHttpReturn);
    }

    private boolean isNativeHttpReturn(JavaMethod method) {
        JavaType returnType = method.getReturnType();
        if (MODEL_AND_VIEW.equals(returnType.toErasure().getName())
                || SSE_EMITTER.equals(returnType.toErasure().getName())) {
            return true;
        }
        if (!RESPONSE_ENTITY.equals(returnType.toErasure().getName())
                || !(returnType instanceof JavaParameterizedType parameterized)
                || parameterized.getActualTypeArguments().size() != 1) {
            return false;
        }
        return NATIVE_HTTP_BODY_TYPES.contains(
                parameterized.getActualTypeArguments().get(0).toErasure().getName());
    }

    private boolean isHttpMethod(JavaMethod method) {
        return HTTP_MAPPINGS.stream().anyMatch(method::isAnnotatedWith);
    }

    private boolean isInterfaceOrExternalStub(JavaClass javaClass) {
        return javaClass.isInterface() || !javaClass.isFullyImported();
    }

    private boolean isMapper(JavaClass javaClass) {
        if (javaClass.isAnnotatedWith(MAPPER)
                || javaClass.getSimpleName().endsWith(MAPPER_SUFFIX)) {
            return true;
        }
        return javaClass.getAllRawInterfaces().stream()
                .anyMatch(type -> BASE_MAPPER.equals(type.getName()));
    }

    private boolean isEntity(JavaClass javaClass) {
        if (javaClass.isAnnotatedWith(TABLE_NAME)
                || javaClass.getSimpleName().endsWith(ENTITY_SUFFIX)) {
            return true;
        }
        return javaClass.getAllRawSuperclasses().stream().anyMatch(this::isEntityBase);
    }

    private boolean isEntityBase(JavaClass type) {
        return TENANT_ENTITY.equals(type.getName()) || BASE_ENTITY.equals(type.getName());
    }

    private void add(
            List<ArchitectureIssue> issues, String ruleId, JavaClass javaClass, String message) {
        issues.add(new ArchitectureIssue(ruleId, javaClass.getName(), message));
    }

    private void addFeignContractIssue(
            List<ArchitectureIssue> issues,
            JavaClass javaClass,
            ModuleContract contract,
            String message) {
        String subject = javaClass.getName();
        if (contract != null) {
            subject = contract.artifactId() + " -> " + javaClass.getName();
        }
        issues.add(new ArchitectureIssue("MANGO-ARCH-FEIGN-007", subject, message));
    }

    public record ModuleContract(String artifactId, String moduleName, String modulePath) {
        public ModuleContract {
            if (!hasValue(artifactId)) {
                throw new IllegalArgumentException("ModuleContract artifactId must be non-blank");
            }
            moduleName = normalizeContractValue(moduleName);
            modulePath = normalizeContractValue(modulePath);
        }

        private static boolean hasValue(String value) {
            return value != null && !value.isBlank();
        }

        private static String normalizeContractValue(String value) {
            if (value == null) {
                return "";
            }
            return value.trim();
        }

        public List<String> modulePathPrefixes() {
            return java.util.Arrays.stream(modulePath.split(","))
                    .map(String::trim)
                    .filter(value -> !value.isEmpty())
                    .map(ModuleContract::normalizeModulePrefix)
                    .toList();
        }

        private static String normalizeModulePrefix(String value) {
            if (value.startsWith(ROOT_PATH)) {
                return value;
            }
            return ROOT_PATH + value;
        }

        public boolean hasModuleIdentity() {
            return hasValue(moduleName) && hasValue(modulePath);
        }
    }

}
