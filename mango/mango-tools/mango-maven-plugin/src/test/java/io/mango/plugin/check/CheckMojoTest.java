package io.mango.plugin.check;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

/**
 * CheckMojo 单元测试
 */
class CheckMojoTest {

    @TempDir
    Path tempDir;

    private void setField(Object target, String fieldName, Object value) throws Exception {
        Field field = target.getClass().getDeclaredField(fieldName);
        field.setAccessible(true);
        field.set(target, value);
    }

    @Test
    void checkNaming_ruleProvided_executesSuccessfully() throws Exception {
        // given
        CheckMojo mojo = new CheckMojo();
        setField(mojo, "rule", "naming");
        setField(mojo, "baseDir", tempDir.toString());
        setField(mojo, "session", null);

        // when & then - should not throw
        assertDoesNotThrow(() -> mojo.execute());
    }

    @Test
    void checkMethodLength_withLongMethod_reportsIssue() throws Exception {
        // given - create a file with a long method
        Path javaFile = tempDir.resolve("TestService.java");
        StringBuilder longMethod = new StringBuilder();
        longMethod.append("public class TestService {\n");
        longMethod.append("    public void longMethod() {\n");
        // Add 60 lines (over 50 limit)
        for (int i = 0; i < 60; i++) {
            longMethod.append("        System.out.println(\"line " + i + "\");\n");
        }
        longMethod.append("    }\n");
        longMethod.append("}\n");
        Files.writeString(javaFile, longMethod.toString());

        // when
        CheckMojo mojo = new CheckMojo();
        setField(mojo, "rule", "method-length");
        setField(mojo, "baseDir", tempDir.toString());
        setField(mojo, "session", null);
        setField(mojo, "maxMethodLength", 50);

        // then - it should throw because issues are found
        assertThrows(org.apache.maven.plugin.MojoExecutionException.class, () -> mojo.execute());
    }

    @Test
    void checkClassLength_withShortClass_passes() throws Exception {
        // given
        Path javaFile = tempDir.resolve("ShortClass.java");
        String content = """
                public class ShortClass {
                    public void doSomething() {
                        System.out.println("hello");
                    }
                }
                """;
        Files.writeString(javaFile, content);

        // when
        CheckMojo mojo = new CheckMojo();
        setField(mojo, "rule", "class-length");
        setField(mojo, "baseDir", tempDir.toString());
        setField(mojo, "session", null);
        setField(mojo, "maxClassLength", 500);

        // then
        assertDoesNotThrow(() -> mojo.execute());
    }

    @Test
    void checkDuplicate_withUniqueMethods_passes() throws Exception {
        // given
        Path javaFile = tempDir.resolve("UniqueService.java");
        String content = """
                public class UniqueService {
                    public void methodA() { }
                    public void methodB() { }
                    public void methodC() { }
                }
                """;
        Files.writeString(javaFile, content);

        // when
        CheckMojo mojo = new CheckMojo();
        setField(mojo, "rule", "duplicate");
        setField(mojo, "baseDir", tempDir.toString());
        setField(mojo, "session", null);

        // then
        assertDoesNotThrow(() -> mojo.execute());
    }

    @Test
    void checkDependency_withValidPom_passes() throws Exception {
        // given
        Path projectDir = tempDir.resolve("mango-user");
        Files.createDirectories(projectDir);
        Path pomFile = projectDir.resolve("pom.xml");
        String pomContent = """
                <?xml version="1.0" encoding="UTF-8"?>
                <project>
                    <groupId>io.mango</groupId>
                    <artifactId>mango-user-api</artifactId>
                    <version>1.0.0</version>
                </project>
                """;
        Files.writeString(pomFile, pomContent);

        // when
        CheckMojo mojo = new CheckMojo();
        setField(mojo, "rule", "dependency");
        setField(mojo, "baseDir", tempDir.toString());
        setField(mojo, "session", null);

        // then
        assertDoesNotThrow(() -> mojo.execute());
    }

    @Test
    void checkModuleInfo_withStarterModuleProperties_passes() throws Exception {
        // given
        Path starterDir = tempDir.resolve("mango-rbac-starter");
        Files.createDirectories(starterDir.resolve("src/main/resources/META-INF/mango"));
        Files.writeString(starterDir.resolve("pom.xml"), """
                <?xml version="1.0" encoding="UTF-8"?>
                <project>
                    <parent>
                        <groupId>io.mango</groupId>
                        <artifactId>mango-parent</artifactId>
                        <version>1.0.0</version>
                    </parent>
                    <groupId>io.mango</groupId>
                    <artifactId>mango-rbac-starter</artifactId>
                    <version>1.0.0</version>
                </project>
                """);
        Files.writeString(starterDir.resolve("src/main/resources/META-INF/mango/module.properties"),
                "module-name=mango-rbac\n");

        // when
        CheckMojo mojo = new CheckMojo();
        setField(mojo, "rule", "module-info");
        setField(mojo, "baseDir", tempDir.toString());
        setField(mojo, "session", null);

        // then
        assertDoesNotThrow(() -> mojo.execute());
    }

    @Test
    void checkModuleInfo_withMissingModuleProperties_reportsIssue() throws Exception {
        // given
        Path starterDir = tempDir.resolve("mango-rbac-starter");
        Files.createDirectories(starterDir);
        Files.writeString(starterDir.resolve("pom.xml"), """
                <?xml version="1.0" encoding="UTF-8"?>
                <project>
                    <groupId>io.mango</groupId>
                    <artifactId>mango-rbac-starter</artifactId>
                    <version>1.0.0</version>
                </project>
                """);

        // when
        CheckMojo mojo = new CheckMojo();
        setField(mojo, "rule", "module-info");
        setField(mojo, "baseDir", tempDir.toString());
        setField(mojo, "session", null);

        // then
        assertThrows(org.apache.maven.plugin.MojoExecutionException.class, () -> mojo.execute());
    }

    @Test
    void checkRemoteAdapter_withModuleName_passes() throws Exception {
        // given
        Path sourceDir = tempDir.resolve("mango-rbac-starter-remote/src/main/java/io/mango/rbac/starter/remote");
        Files.createDirectories(sourceDir);
        Files.writeString(sourceDir.resolve("SysUserFeignClient.java"), """
                package io.mango.rbac.starter.remote;

                import org.springframework.cloud.openfeign.FeignClient;

                @FeignClient(name = "mango-rbac", path = "/user")
                public interface SysUserFeignClient {
                }
                """);

        // when
        CheckMojo mojo = new CheckMojo();
        setField(mojo, "rule", "remote-adapter");
        setField(mojo, "baseDir", tempDir.toString());
        setField(mojo, "session", null);

        // then
        assertDoesNotThrow(() -> mojo.execute());
    }

    @Test
    void checkRemoteAdapter_withServiceName_reportsIssue() throws Exception {
        // given
        Path sourceDir = tempDir.resolve("mango-rbac-starter-remote/src/main/java/io/mango/rbac/starter/remote");
        Files.createDirectories(sourceDir);
        Files.writeString(sourceDir.resolve("SysUserFeignClient.java"), """
                package io.mango.rbac.starter.remote;

                import org.springframework.cloud.openfeign.FeignClient;

                @FeignClient(name = "permission-service", path = "/user")
                public interface SysUserFeignClient {
                }
                """);

        // when
        CheckMojo mojo = new CheckMojo();
        setField(mojo, "rule", "remote-adapter");
        setField(mojo, "baseDir", tempDir.toString());
        setField(mojo, "session", null);

        // then
        assertThrows(org.apache.maven.plugin.MojoExecutionException.class, () -> mojo.execute());
    }

    @Test
    void checkApiContract_withoutFeignClient_passes() throws Exception {
        // given
        Path sourceDir = tempDir.resolve("mango-rbac-api/src/main/java/io/mango/rbac/api");
        Files.createDirectories(sourceDir);
        Files.writeString(sourceDir.resolve("SysUserApi.java"), """
                package io.mango.rbac.api;

                public interface SysUserApi {
                }
                """);

        // when
        CheckMojo mojo = new CheckMojo();
        setField(mojo, "rule", "api-contract");
        setField(mojo, "baseDir", tempDir.toString());
        setField(mojo, "session", null);

        // then
        assertDoesNotThrow(() -> mojo.execute());
    }

    @Test
    void checkApiContract_withFeignClient_reportsIssue() throws Exception {
        // given
        Path sourceDir = tempDir.resolve("mango-rbac-api/src/main/java/io/mango/rbac/api");
        Files.createDirectories(sourceDir);
        Files.writeString(sourceDir.resolve("SysUserApi.java"), """
                package io.mango.rbac.api;

                import org.springframework.cloud.openfeign.FeignClient;

                @FeignClient(name = "mango-rbac")
                public interface SysUserApi {
                }
                """);

        // when
        CheckMojo mojo = new CheckMojo();
        setField(mojo, "rule", "api-contract");
        setField(mojo, "baseDir", tempDir.toString());
        setField(mojo, "session", null);

        // then
        assertThrows(org.apache.maven.plugin.MojoExecutionException.class, () -> mojo.execute());
    }

    @Test
    void checkKvKey_withSpelTemplate_passes() throws Exception {
        // given
        Path sourceDir = tempDir.resolve("mango-demo-core/src/main/java/io/mango/demo/core");
        Files.createDirectories(sourceDir);
        Files.writeString(sourceDir.resolve("DemoService.java"), """
                package io.mango.demo.core;

                import io.mango.infra.kv.api.annotation.Cacheable;

                public class DemoService {
                    @Cacheable(key = "user:#{#userId}")
                    public String find(String userId) {
                        return userId;
                    }
                }
                """);

        // when
        CheckMojo mojo = new CheckMojo();
        setField(mojo, "rule", "kv-key");
        setField(mojo, "baseDir", tempDir.toString());
        setField(mojo, "session", null);

        // then
        assertDoesNotThrow(() -> mojo.execute());
    }

    @Test
    void checkKvKey_withInlinePlaceholder_reportsIssue() throws Exception {
        // given
        Path sourceDir = tempDir.resolve("mango-demo-core/src/main/java/io/mango/demo/core");
        Files.createDirectories(sourceDir);
        Files.writeString(sourceDir.resolve("DemoService.java"), """
                package io.mango.demo.core;

                import io.mango.infra.kv.api.annotation.Cacheable;

                public class DemoService {
                    @Cacheable(key = "user:#userId")
                    public String find(String userId) {
                        return userId;
                    }
                }
                """);

        // when
        CheckMojo mojo = new CheckMojo();
        setField(mojo, "rule", "kv-key");
        setField(mojo, "baseDir", tempDir.toString());
        setField(mojo, "session", null);

        // then
        assertThrows(org.apache.maven.plugin.MojoExecutionException.class, () -> mojo.execute());
    }

    @Test
    void checkKvKey_withMultilineInlinePlaceholder_reportsIssue() throws Exception {
        // given
        Path sourceDir = tempDir.resolve("mango-demo-core/src/main/java/io/mango/demo/core");
        Files.createDirectories(sourceDir);
        Files.writeString(sourceDir.resolve("DemoService.java"), """
                package io.mango.demo.core;

                import io.mango.infra.kv.api.annotation.Cacheable;

                public class DemoService {
                    @Cacheable(
                            key = "user:#userId",
                            ttl = 60
                    )
                    public String find(String userId) {
                        return userId;
                    }
                }
                """);

        // when
        CheckMojo mojo = new CheckMojo();
        setField(mojo, "rule", "kv-key");
        setField(mojo, "baseDir", tempDir.toString());
        setField(mojo, "session", null);

        // then
        assertThrows(org.apache.maven.plugin.MojoExecutionException.class, () -> mojo.execute());
    }

    @Test
    void checkKvKey_withInfraPrefix_reportsIssue() throws Exception {
        // given
        Path sourceDir = tempDir.resolve("mango-demo-core/src/main/java/io/mango/demo/core");
        Files.createDirectories(sourceDir);
        Files.writeString(sourceDir.resolve("DemoService.java"), """
                package io.mango.demo.core;

                import io.mango.infra.kv.api.annotation.Locker;

                public class DemoService {
                    @Locker(key = "mango:infra:kv:prod:lock:order:1")
                    public void lock() {
                    }
                }
                """);

        // when
        CheckMojo mojo = new CheckMojo();
        setField(mojo, "rule", "kv-key");
        setField(mojo, "baseDir", tempDir.toString());
        setField(mojo, "session", null);

        // then
        assertThrows(org.apache.maven.plugin.MojoExecutionException.class, () -> mojo.execute());
    }

    @Test
    void checkTestFixture_withMatchingRedisFixture_passes() throws Exception {
        // given
        Path sourceDir = tempDir.resolve("mango-infra-test/src/test/java/io/mango/infra/kv/core");
        Files.createDirectories(sourceDir);
        Files.writeString(sourceDir.resolve("RedisKvStoreTest.java"), """
                package io.mango.infra.kv.core;

                import io.mango.infra.kv.core.redis.RedisKvStore;

                class RedisKvStoreTest {
                    private RedisKvStore kvStore;
                }
                """);

        // when
        CheckMojo mojo = new CheckMojo();
        setField(mojo, "rule", "test-fixture");
        setField(mojo, "baseDir", tempDir.toString());
        setField(mojo, "session", null);

        // then
        assertDoesNotThrow(() -> mojo.execute());
    }

    @Test
    void checkTestFixture_withRedisTestUsingMemoryFixture_reportsIssue() throws Exception {
        // given
        Path sourceDir = tempDir.resolve("mango-infra-test/src/test/java/io/mango/infra/kv/core");
        Files.createDirectories(sourceDir);
        Files.writeString(sourceDir.resolve("RedisCacheTest.java"), """
                package io.mango.infra.kv.core;

                import io.mango.infra.kv.core.memory.MemoryKvStore;

                class RedisCacheTest {
                    private MemoryKvStore kvStore;
                }
                """);

        // when
        CheckMojo mojo = new CheckMojo();
        setField(mojo, "rule", "test-fixture");
        setField(mojo, "baseDir", tempDir.toString());
        setField(mojo, "session", null);

        // then
        assertThrows(org.apache.maven.plugin.MojoExecutionException.class, () -> mojo.execute());
    }

    @Test
    void checkTestFixture_withCapabilityParameterizedFixture_passes() throws Exception {
        // given
        Path sourceDir = tempDir.resolve("mango-infra-test/src/test/java/io/mango/infra/kv/core/capability");
        Files.createDirectories(sourceDir);
        Files.writeString(sourceDir.resolve("CacheTest.java"), """
                package io.mango.infra.kv.core.capability;

                import io.mango.infra.kv.core.jdbc.JdbcKvStore;
                import io.mango.infra.kv.core.memory.MemoryKvStore;
                import io.mango.infra.kv.core.redis.RedisKvStore;

                class CacheTest {
                    private MemoryKvStore memory;
                    private JdbcKvStore jdbc;
                    private RedisKvStore redis;
                }
                """);

        // when
        CheckMojo mojo = new CheckMojo();
        setField(mojo, "rule", "test-fixture");
        setField(mojo, "baseDir", tempDir.toString());
        setField(mojo, "session", null);

        // then
        assertDoesNotThrow(() -> mojo.execute());
    }

    @Test
    void extractSignature_withValidMethod_returnsSignature() throws Exception {
        // given
        Path javaFile = tempDir.resolve("Test.java");
        Files.writeString(javaFile, "public void doSomething(String arg1, int arg2) { }");

        // when
        CheckMojo mojo = new CheckMojo();
        setField(mojo, "rule", "naming");
        setField(mojo, "baseDir", tempDir.toString());
        setField(mojo, "session", null);

        // then - invoke private method via reflection
        Method method = CheckMojo.class.getDeclaredMethod("extractSignature", String.class);
        method.setAccessible(true);
        String signature = (String) method.invoke(mojo, "public void doSomething(String arg1, int arg2) { }");

        assertNotNull(signature);
        assertTrue(signature.contains("doSomething"));
    }
}
