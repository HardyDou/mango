package io.mango.admin.it;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.mango.common.result.R;
import io.mango.file.core.config.FileProperties;
import io.mango.file.core.entity.FileObjectEntity;
import io.mango.file.core.entity.FileRecordEntity;
import io.mango.file.core.entity.FileStorageConfigEntity;
import io.mango.file.core.mapper.FileObjectMapper;
import io.mango.file.core.mapper.FileRecordMapper;
import io.mango.file.core.mapper.FileStorageConfigMapper;
import io.mango.file.core.resource.FileAssetResourceHandler;
import io.mango.file.core.storage.FileObject;
import io.mango.file.core.storage.FileStorage;
import io.mango.file.core.storage.FileStorageRouter;
import io.mango.infra.bootstrap.api.BootstrapExecutionContext;
import io.mango.infra.bootstrap.api.BootstrapStep;
import io.mango.resource.api.ResourceDeclarationApi;
import io.mango.resource.support.ResourceProvider;
import io.mango.resource.support.ResourceTypes;
import io.mango.resource.support.config.ResourceRegistryProperties;
import io.mango.resource.support.declaration.ResourceDeclarationCanonicalizer;
import io.mango.resource.support.declaration.ResourceDeclarationCollector;
import io.mango.resource.support.model.ResourceDeclaration;
import io.mango.resource.sync.starter.ResourceBootstrapStepContributor;
import io.mango.resource.sync.starter.ResourceManifestArtifactLoader;
import io.mango.resource.sync.starter.ResourceManifestSerializer;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;
import org.springframework.core.io.DefaultResourceLoader;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

/** Executable consumer fixture for the packaged Resource manifest contract. */
public final class ResourceArtifactBootstrapConsumerApplication {

    private static final String VERIFY_ARGUMENT = "--verify-file-asset-bootstrap";
    private static final String OBJECT_NAME = "mango-assets/package-fixture/document.txt";
    private static final byte[] EXPECTED_CONTENT =
            "issue-877 packaged bootstrap fixture\n".getBytes(StandardCharsets.UTF_8);

    private ResourceArtifactBootstrapConsumerApplication() {
    }

    public static void main(String[] args) {
        if (args.length != 1 || !VERIFY_ARGUMENT.equals(args[0])) {
            throw new IllegalArgumentException("Expected " + VERIFY_ARGUMENT);
        }
        new ResourceArtifactBootstrapConsumerApplicationVerifier().verify();
        System.out.println("FILE_ASSET_BOOTSTRAP_VERIFIED");
    }

    private static final class ResourceArtifactBootstrapConsumerApplicationVerifier {

        private final AtomicReference<FileObjectEntity> objectState = new AtomicReference<>();
        private final AtomicReference<FileRecordEntity> recordState = new AtomicReference<>();
        private final InMemoryFileStorage storage = new InMemoryFileStorage();
        private final ObjectMapper objectMapper = new ObjectMapper();

        private void verify() {
            ClassLoader classLoader = ResourceArtifactBootstrapConsumerApplication.class.getClassLoader();
            DefaultResourceLoader resourceLoader = new DefaultResourceLoader(classLoader);
            FileAssetResourceHandler handler = fileAssetHandler(resourceLoader);
            ResourceDeclarationApi declarationApi = command -> {
                command.getModuleManifests().forEach(module -> declarations(module.getDeclarations()).stream()
                        .filter(declaration -> ResourceTypes.FILE_ASSET.equals(declaration.getResourceType()))
                        .forEach(handler::upsert));
                return R.ok(true);
            };
            DefaultListableBeanFactory beanFactory = new DefaultListableBeanFactory();
            ResourceDeclarationCollector collector = new ResourceDeclarationCollector(
                    beanFactory.getBeanProvider(ResourceProvider.class));
            ResourceManifestArtifactLoader artifactLoader =
                    new ResourceManifestArtifactLoader(objectMapper, resourceLoader);
            ResourceBootstrapStepContributor contributor = new ResourceBootstrapStepContributor(
                    new ResourceRegistryProperties(), collector, declarationApi,
                    new ResourceManifestSerializer(), new ResourceDeclarationCanonicalizer(objectMapper),
                    artifactLoader, "resource-artifact-bootstrap-consumer");

            List<BootstrapStep> steps = contributor.contributeSteps();
            require(steps.size() == 2, "Packaged manifest must contribute EXPAND and FINALIZE steps");
            for (BootstrapStep step : steps) {
                step.execute(new BootstrapExecutionContext(
                        "issue-877-verification", "issue-877", "issue-877-release", "issue-877-build",
                        1L, "1".repeat(64), 1L, step.phase()));
            }

            require(recordState.get() != null, "FILE_ASSET file record was not persisted");
            require(objectState.get() != null, "FILE_ASSET object metadata was not persisted");
            require(java.util.Arrays.equals(EXPECTED_CONTENT, storage.objects.get(OBJECT_NAME)),
                    "FILE_ASSET stable object content does not match the packaged object");
            require(storage.objects.keySet().stream().noneMatch(key -> key.startsWith(".mango-staging/")),
                    "FILE_ASSET staging object was not cleaned after publication");
        }

        private FileAssetResourceHandler fileAssetHandler(DefaultResourceLoader resourceLoader) {
            FileStorageConfigEntity config = new FileStorageConfigEntity();
            config.setId(1L);
            config.setTenantId(1L);
            config.setStorageType("MEMORY");
            config.setBucketName("issue-877");
            config.setStatus(1);

            FileStorageConfigMapper storageConfigMapper = mapper(FileStorageConfigMapper.class,
                    (method, arguments) -> {
                        require("selectById".equals(method.getName()), method);
                        return config;
                    });
            FileObjectMapper fileObjectMapper = mapper(FileObjectMapper.class, (method, arguments) -> {
                if ("selectById".equals(method.getName()) || "selectOne".equals(method.getName())) {
                    return objectState.get();
                }
                if ("insert".equals(method.getName()) || "updateById".equals(method.getName())) {
                    FileObjectEntity entity = (FileObjectEntity) arguments[0];
                    entity.setId(8771L);
                    objectState.set(entity);
                    return 1;
                }
                throw unexpected(method);
            });
            FileRecordMapper fileRecordMapper = mapper(FileRecordMapper.class, (method, arguments) -> {
                if ("selectById".equals(method.getName())) {
                    return recordState.get();
                }
                if ("insert".equals(method.getName()) || "updateById".equals(method.getName())) {
                    recordState.set((FileRecordEntity) arguments[0]);
                    return 1;
                }
                throw unexpected(method);
            });
            return new FileAssetResourceHandler(storageConfigMapper, fileObjectMapper, fileRecordMapper,
                    new FileStorageRouter(List.of(storage)), resourceLoader, new FileProperties());
        }

        private List<ResourceDeclaration> declarations(String json) {
            try {
                return objectMapper.readValue(json, new TypeReference<>() {
                });
            } catch (Exception exception) {
                throw new IllegalStateException("Read packaged Resource declarations failed", exception);
            }
        }
    }

    private static final class InMemoryFileStorage implements FileStorage {

        private final Map<String, byte[]> objects = new HashMap<>();

        @Override
        public boolean supports(String storageType) {
            return "MEMORY".equals(storageType);
        }

        @Override
        public void putObject(FileStorageConfigEntity config, String objectName, InputStream inputStream,
                              long contentLength, String contentType) throws Exception {
            ByteArrayOutputStream output = new ByteArrayOutputStream();
            inputStream.transferTo(output);
            require(output.size() == contentLength, "FILE_ASSET staging content length mismatch");
            objects.put(objectName, output.toByteArray());
        }

        @Override
        public FileObject getObject(FileStorageConfigEntity config, String objectName) {
            byte[] content = objects.get(objectName);
            if (content == null) {
                throw new IllegalStateException("Object not found: " + objectName);
            }
            return new FileObject(new ByteArrayInputStream(content), content.length, "text/plain");
        }

        @Override
        public void removeObject(FileStorageConfigEntity config, String objectName) {
            objects.remove(objectName);
        }

        @Override
        public void publishObject(FileStorageConfigEntity config, String stagingObjectName,
                                  String targetObjectName) {
            byte[] content = objects.remove(stagingObjectName);
            require(content != null, "FILE_ASSET staging object is missing");
            objects.put(targetObjectName, content);
        }

        @Override
        public void test(FileStorageConfigEntity config) {
            require(supports(config.getStorageType()), "Unsupported storage type: " + config.getStorageType());
        }
    }

    private static <T> T mapper(Class<T> type, MapperInvocation invocation) {
        Object proxy = Proxy.newProxyInstance(type.getClassLoader(), new Class<?>[]{type},
                (target, method, arguments) -> {
                    if (method.getDeclaringClass() == Object.class) {
                        return objectMethod(target, method, arguments);
                    }
                    return invocation.invoke(method, arguments == null ? new Object[0] : arguments);
                });
        return type.cast(proxy);
    }

    private static Object objectMethod(Object target, Method method, Object[] arguments) {
        return switch (method.getName()) {
            case "equals" -> target == arguments[0];
            case "hashCode" -> System.identityHashCode(target);
            case "toString" -> target.getClass().getName();
            default -> throw unexpected(method);
        };
    }

    private static void require(boolean condition, String message) {
        if (!condition) {
            throw new IllegalStateException(message);
        }
    }

    private static void require(boolean condition, Method method) {
        if (!condition) {
            throw unexpected(method);
        }
    }

    private static IllegalStateException unexpected(Method method) {
        return new IllegalStateException("Unexpected mapper method: " + method);
    }

    @FunctionalInterface
    private interface MapperInvocation {
        Object invoke(Method method, Object[] arguments);
    }
}
