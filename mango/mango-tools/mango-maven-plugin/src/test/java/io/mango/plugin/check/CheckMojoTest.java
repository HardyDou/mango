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

    private void createStarterModule(String artifactId, String moduleName,
                                     String modulePath, String controllerPath) throws Exception {
        Path starterDir = tempDir.resolve(artifactId);
        Files.createDirectories(starterDir.resolve("src/main/resources/META-INF/mango"));
        Files.createDirectories(starterDir.resolve("src/main/java/io/mango/demo/starter"));
        Files.writeString(starterDir.resolve("pom.xml"), """
                <?xml version="1.0" encoding="UTF-8"?>
                <project>
                    <groupId>io.mango</groupId>
                    <artifactId>%s</artifactId>
                    <version>1.0.0</version>
                </project>
                """.formatted(artifactId));
        Files.writeString(starterDir.resolve("src/main/resources/META-INF/mango/module.properties"), """
                module-name=%s
                module-path=%s
                """.formatted(moduleName, modulePath));
        Files.writeString(starterDir.resolve("src/main/java/io/mango/demo/starter/DemoController.java"), """
                package io.mango.demo.starter;

                import org.springframework.web.bind.annotation.RequestMapping;
                import org.springframework.web.bind.annotation.RestController;

                @RestController
                @RequestMapping("%s")
                public class DemoController {
                }
                """.formatted(controllerPath));
    }

    @Test
    void checkNaming_ruleProvided_executesSuccessfully() throws Exception {
        // given
        CheckMojo mojo = new CheckMojo();
        setField(mojo, "rule", "naming");
        setField(mojo, "baseDir", tempDir.toString());
        setField(mojo, "session", null);

        // when & then
        assertDoesNotThrow(() -> mojo.execute());
    }

    @Test
    void checkNaming_withNonKebabArtifactId_reportsIssue() throws Exception {
        // given
        Path pomFile = tempDir.resolve("mango-demo/pom.xml");
        Files.createDirectories(pomFile.getParent());
        Files.writeString(pomFile, """
                <?xml version="1.0" encoding="UTF-8"?>
                <project>
                    <parent>
                        <groupId>io.mango</groupId>
                        <artifactId>mango-parent</artifactId>
                        <version>1.0.0</version>
                    </parent>
                    <artifactId>mangoDemoCore</artifactId>
                </project>
                """);

        // when
        CheckMojo mojo = new CheckMojo();
        setField(mojo, "rule", "naming");
        setField(mojo, "baseDir", tempDir.toString());
        setField(mojo, "session", null);

        // then
        assertThrows(org.apache.maven.plugin.MojoExecutionException.class, () -> mojo.execute());
    }

    @Test
    void checkMethodLength_genericRule_reportsUnsupported() throws Exception {
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

        // then - generic code quality belongs to PMD/P3C/Checkstyle
        assertThrows(org.apache.maven.plugin.MojoExecutionException.class, () -> mojo.execute());
    }

    @Test
    void checkClassLength_genericRule_reportsUnsupported() throws Exception {
        // given - public class declarations must not be counted as methods
        Path javaFile = tempDir.resolve("Response.java");
        StringBuilder source = new StringBuilder();
        source.append("public class Response {\n");
        for (int i = 0; i < 70; i++) {
            source.append("    private String field").append(i).append(";\n");
        }
        source.append("    public static Response ok() {\n");
        source.append("        return new Response();\n");
        source.append("    }\n");
        source.append("    public boolean success()\n");
        source.append("    {\n");
        source.append("        return true;\n");
        source.append("    }\n");
        source.append("}\n");
        Files.writeString(javaFile, source.toString());

        // when
        CheckMojo mojo = new CheckMojo();
        setField(mojo, "rule", "class-length");
        setField(mojo, "baseDir", tempDir.toString());
        setField(mojo, "session", null);
        setField(mojo, "maxClassLength", 50);

        // then
        assertThrows(org.apache.maven.plugin.MojoExecutionException.class, () -> mojo.execute());
    }

    @Test
    void checkDuplicate_genericRule_reportsUnsupported() throws Exception {
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
        assertThrows(org.apache.maven.plugin.MojoExecutionException.class, () -> mojo.execute());
    }

    @Test
    void checkAll_withGenericQualityIssue_runsOnlyMangoSpecificRules() throws Exception {
        // given - generic code quality is delegated to PMD/P3C/Checkstyle, not mango:check
        Path javaFile = tempDir.resolve("src/main/java/io/mango/demo/TestService.java");
        Files.createDirectories(javaFile.getParent());
        StringBuilder source = new StringBuilder();
        source.append("package io.mango.demo;\n");
        source.append("public class TestService {\n");
        source.append("    public void longMethod() {\n");
        for (int i = 0; i < 60; i++) {
            source.append("        System.out.println(\"line " + i + "\");\n");
        }
        source.append("    }\n");
        source.append("}\n");
        Files.writeString(javaFile, source.toString());

        // when
        CheckMojo mojo = new CheckMojo();
        setField(mojo, "rule", "all");
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
    void checkDependency_withStarterRemoteDependingOnCore_reportsIssue() throws Exception {
        // given
        Path projectDir = tempDir.resolve("mango-demo-starter-remote");
        Files.createDirectories(projectDir);
        Files.writeString(projectDir.resolve("pom.xml"), """
                <?xml version="1.0" encoding="UTF-8"?>
                <project>
                    <groupId>io.mango</groupId>
                    <artifactId>mango-demo-starter-remote</artifactId>
                    <version>1.0.0</version>
                    <dependencies>
                        <dependency>
                            <groupId>io.mango</groupId>
                            <artifactId>mango-demo-core</artifactId>
                        </dependency>
                    </dependencies>
                </project>
                """);

        // when
        CheckMojo mojo = new CheckMojo();
        setField(mojo, "rule", "dependency");
        setField(mojo, "baseDir", tempDir.toString());
        setField(mojo, "session", null);

        // then
        assertThrows(org.apache.maven.plugin.MojoExecutionException.class, () -> mojo.execute());
    }

    @Test
    void checkDependency_withStarterRemoteDependingOnSupport_passes() throws Exception {
        Path projectDir = tempDir.resolve("mango-demo-starter-remote");
        Files.createDirectories(projectDir);
        Files.writeString(projectDir.resolve("pom.xml"), """
                <?xml version="1.0" encoding="UTF-8"?>
                <project>
                    <groupId>io.mango</groupId>
                    <artifactId>mango-demo-starter-remote</artifactId>
                    <version>1.0.0</version>
                    <dependencies>
                        <dependency>
                            <groupId>io.mango</groupId>
                            <artifactId>mango-demo-api</artifactId>
                        </dependency>
                        <dependency>
                            <groupId>io.mango</groupId>
                            <artifactId>mango-demo-support</artifactId>
                        </dependency>
                        <dependency>
                            <groupId>io.mango</groupId>
                            <artifactId>mango-infra-feign-starter</artifactId>
                        </dependency>
                    </dependencies>
                </project>
                """);

        CheckMojo mojo = new CheckMojo();
        setField(mojo, "rule", "dependency");
        setField(mojo, "baseDir", tempDir.toString());
        setField(mojo, "session", null);

        assertDoesNotThrow(() -> mojo.execute());
    }

    @Test
    void checkDependency_withStarterRemoteDependingOnSpringCloudOpenFeign_reportsIssue() throws Exception {
        Path projectDir = tempDir.resolve("mango-demo-starter-remote");
        Files.createDirectories(projectDir);
        Files.writeString(projectDir.resolve("pom.xml"), """
                <?xml version="1.0" encoding="UTF-8"?>
                <project>
                    <groupId>io.mango</groupId>
                    <artifactId>mango-demo-starter-remote</artifactId>
                    <version>1.0.0</version>
                    <dependencies>
                        <dependency>
                            <groupId>io.mango</groupId>
                            <artifactId>mango-demo-api</artifactId>
                        </dependency>
                        <dependency>
                            <groupId>org.springframework.cloud</groupId>
                            <artifactId>spring-cloud-starter-openfeign</artifactId>
                        </dependency>
                    </dependencies>
                </project>
                """);

        CheckMojo mojo = new CheckMojo();
        setField(mojo, "rule", "dependency");
        setField(mojo, "baseDir", tempDir.toString());
        setField(mojo, "session", null);

        assertThrows(org.apache.maven.plugin.MojoExecutionException.class, () -> mojo.execute());
    }

    @Test
    void checkDependency_withAuthorizationRemoteSupport_passes() throws Exception {
        Path projectDir = tempDir.resolve("mango-authorization-starter-remote");
        Files.createDirectories(projectDir);
        Files.writeString(projectDir.resolve("pom.xml"), """
                <?xml version="1.0" encoding="UTF-8"?>
                <project>
                    <groupId>io.mango</groupId>
                    <artifactId>mango-authorization-starter-remote</artifactId>
                    <version>1.0.0</version>
                    <dependencies>
                        <dependency>
                            <groupId>io.mango</groupId>
                            <artifactId>mango-authorization-api</artifactId>
                        </dependency>
                        <dependency>
                            <groupId>io.mango</groupId>
                            <artifactId>mango-authorization-support</artifactId>
                        </dependency>
                    </dependencies>
                </project>
                """);

        CheckMojo mojo = new CheckMojo();
        setField(mojo, "rule", "dependency");
        setField(mojo, "baseDir", tempDir.toString());
        setField(mojo, "session", null);

        assertDoesNotThrow(() -> mojo.execute());
    }

    @Test
    void checkWebBoundary_withApiDependingOnWebApi_passes() throws Exception {
        Path projectDir = tempDir.resolve("mango-demo-api");
        Files.createDirectories(projectDir);
        Files.writeString(projectDir.resolve("pom.xml"), """
                <?xml version="1.0" encoding="UTF-8"?>
                <project>
                    <groupId>io.mango</groupId>
                    <artifactId>mango-demo-api</artifactId>
                    <version>1.0.0</version>
                    <dependencies>
                        <dependency>
                            <groupId>io.mango</groupId>
                            <artifactId>mango-infra-web-api</artifactId>
                        </dependency>
                    </dependencies>
                </project>
                """);

        CheckMojo mojo = new CheckMojo();
        setField(mojo, "rule", "web-boundary");
        setField(mojo, "baseDir", tempDir.toString());
        setField(mojo, "session", null);

        assertDoesNotThrow(() -> mojo.execute());
    }

    @Test
    void checkWebBoundary_withApiDependingOnWebStarter_reportsIssue() throws Exception {
        Path projectDir = tempDir.resolve("mango-demo-api");
        Files.createDirectories(projectDir);
        Files.writeString(projectDir.resolve("pom.xml"), """
                <?xml version="1.0" encoding="UTF-8"?>
                <project>
                    <groupId>io.mango</groupId>
                    <artifactId>mango-demo-api</artifactId>
                    <version>1.0.0</version>
                    <dependencies>
                        <dependency>
                            <groupId>io.mango</groupId>
                            <artifactId>mango-infra-web-starter</artifactId>
                        </dependency>
                    </dependencies>
                </project>
                """);

        CheckMojo mojo = new CheckMojo();
        setField(mojo, "rule", "web-boundary");
        setField(mojo, "baseDir", tempDir.toString());
        setField(mojo, "session", null);

        assertThrows(org.apache.maven.plugin.MojoExecutionException.class, () -> mojo.execute());
    }

    @Test
    void checkWebBoundary_withDuplicateSpringWebStarter_reportsIssue() throws Exception {
        Path projectDir = tempDir.resolve("mango-demo-starter");
        Files.createDirectories(projectDir);
        Files.writeString(projectDir.resolve("pom.xml"), """
                <?xml version="1.0" encoding="UTF-8"?>
                <project>
                    <groupId>io.mango</groupId>
                    <artifactId>mango-demo-starter</artifactId>
                    <version>1.0.0</version>
                    <dependencies>
                        <dependency>
                            <groupId>io.mango</groupId>
                            <artifactId>mango-infra-web-starter</artifactId>
                        </dependency>
                        <dependency>
                            <groupId>org.springframework.boot</groupId>
                            <artifactId>spring-boot-starter-web</artifactId>
                        </dependency>
                    </dependencies>
                </project>
                """);

        CheckMojo mojo = new CheckMojo();
        setField(mojo, "rule", "web-boundary");
        setField(mojo, "baseDir", tempDir.toString());
        setField(mojo, "session", null);

        assertThrows(org.apache.maven.plugin.MojoExecutionException.class, () -> mojo.execute());
    }

    @Test
    void checkModuleInfo_withStarterModuleProperties_passes() throws Exception {
        // given
        Path starterDir = tempDir.resolve("mango-rbac-starter");
        Files.createDirectories(starterDir.resolve("src/main/resources/META-INF/mango"));
        Files.createDirectories(starterDir.resolve("src/main/java/io/mango/rbac/starter"));
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
        Files.writeString(starterDir.resolve("src/main/java/io/mango/rbac/starter/RbacController.java"), """
                package io.mango.rbac.starter;

                import org.springframework.web.bind.annotation.RequestMapping;
                import org.springframework.web.bind.annotation.RestController;

                @RestController
                @RequestMapping("/rbac/user")
                public class RbacController {
                }
                """);
        Files.writeString(starterDir.resolve("src/main/resources/META-INF/mango/module.properties"),
                """
                module-name=mango-rbac
                module-path=/rbac
                """);

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
        Path starterDir = tempDir.resolve("mango-rbac-starter");
        Files.createDirectories(starterDir.resolve("src/main/resources/META-INF/mango"));
        Files.writeString(starterDir.resolve("pom.xml"), """
                <?xml version="1.0" encoding="UTF-8"?>
                <project>
                    <groupId>io.mango</groupId>
                    <artifactId>mango-rbac-starter</artifactId>
                    <version>1.0.0</version>
                </project>
                """);
        Files.writeString(starterDir.resolve("src/main/resources/META-INF/mango/module.properties"), """
                module-name=mango-rbac
                module-path=/rbac
                """);
        Path sourceDir = tempDir.resolve("mango-rbac-starter-remote/src/main/java/io/mango/rbac/starter/remote");
        Files.createDirectories(sourceDir);
        Files.writeString(sourceDir.resolve("SysUserFeignClient.java"), """
                package io.mango.rbac.starter.remote;

                import org.springframework.cloud.openfeign.FeignClient;

                @FeignClient(name = "mango-rbac", path = "/rbac/user")
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
        Path starterDir = tempDir.resolve("mango-rbac-starter");
        Files.createDirectories(starterDir.resolve("src/main/resources/META-INF/mango"));
        Files.writeString(starterDir.resolve("pom.xml"), """
                <?xml version="1.0" encoding="UTF-8"?>
                <project>
                    <groupId>io.mango</groupId>
                    <artifactId>mango-rbac-starter</artifactId>
                    <version>1.0.0</version>
                </project>
                """);
        Files.writeString(starterDir.resolve("src/main/resources/META-INF/mango/module.properties"), """
                module-name=mango-rbac
                module-path=/rbac
                """);
        Path sourceDir = tempDir.resolve("mango-rbac-starter-remote/src/main/java/io/mango/rbac/starter/remote");
        Files.createDirectories(sourceDir);
        Files.writeString(sourceDir.resolve("SysUserFeignClient.java"), """
                package io.mango.rbac.starter.remote;

                import org.springframework.cloud.openfeign.FeignClient;

                @FeignClient(name = "permission-service", path = "/rbac/user")
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
    void checkModuleInfo_withDuplicateModulePath_reportsIssue() throws Exception {
        createStarterModule("mango-rbac-starter", "mango-rbac", "/shared", "/shared/user");
        createStarterModule("mango-auth-starter", "mango-auth", "/shared", "/shared/login");

        CheckMojo mojo = new CheckMojo();
        setField(mojo, "rule", "module-info");
        setField(mojo, "baseDir", tempDir.toString());
        setField(mojo, "session", null);

        assertThrows(org.apache.maven.plugin.MojoExecutionException.class, () -> mojo.execute());
    }

    @Test
    void checkModuleInfo_withNonStarterModuleProperties_reportsIssue() throws Exception {
        Path coreDir = tempDir.resolve("mango-rbac-core");
        Files.createDirectories(coreDir.resolve("src/main/resources/META-INF/mango"));
        Files.writeString(coreDir.resolve("pom.xml"), """
                <?xml version="1.0" encoding="UTF-8"?>
                <project>
                    <groupId>io.mango</groupId>
                    <artifactId>mango-rbac-core</artifactId>
                    <version>1.0.0</version>
                </project>
                """);
        Files.writeString(coreDir.resolve("src/main/resources/META-INF/mango/module.properties"), """
                module-name=mango-rbac
                module-path=/rbac
                """);

        CheckMojo mojo = new CheckMojo();
        setField(mojo, "rule", "module-info");
        setField(mojo, "baseDir", tempDir.toString());
        setField(mojo, "session", null);

        assertThrows(org.apache.maven.plugin.MojoExecutionException.class, () -> mojo.execute());
    }

    @Test
    void checkRemoteAdapter_withWrongPath_reportsIssue() throws Exception {
        createStarterModule("mango-rbac-starter", "mango-rbac", "/rbac", "/rbac/user");
        Path sourceDir = tempDir.resolve("mango-rbac-starter-remote/src/main/java/io/mango/rbac/starter/remote");
        Files.createDirectories(sourceDir);
        Files.writeString(sourceDir.resolve("SysUserFeignClient.java"), """
                package io.mango.rbac.starter.remote;

                import org.springframework.cloud.openfeign.FeignClient;

                @FeignClient(name = "mango-rbac", path = "/internal/rbac/user")
                public interface SysUserFeignClient {
                }
                """);

        CheckMojo mojo = new CheckMojo();
        setField(mojo, "rule", "remote-adapter");
        setField(mojo, "baseDir", tempDir.toString());
        setField(mojo, "session", null);

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
    void checkApiContract_withLocalCollaborationType_reportsIssue() throws Exception {
        Path sourceDir = tempDir.resolve("mango-realtime-api/src/main/java/io/mango/realtime/api");
        Files.createDirectories(sourceDir);
        Files.writeString(sourceDir.resolve("RealtimePollingService.java"), """
                package io.mango.realtime.api;

                public interface RealtimePollingService {
                }
                """);

        CheckMojo mojo = new CheckMojo();
        setField(mojo, "rule", "api-contract");
        setField(mojo, "baseDir", tempDir.toString());
        setField(mojo, "session", null);

        assertThrows(org.apache.maven.plugin.MojoExecutionException.class, () -> mojo.execute());
    }

    @Test
    void checkApiContract_withSessionType_reportsIssue() throws Exception {
        Path sourceDir = tempDir.resolve("mango-realtime-api/src/main/java/io/mango/realtime/api");
        Files.createDirectories(sourceDir);
        Files.writeString(sourceDir.resolve("RealtimeSession.java"), """
                package io.mango.realtime.api;

                public interface RealtimeSession {
                }
                """);

        CheckMojo mojo = new CheckMojo();
        setField(mojo, "rule", "api-contract");
        setField(mojo, "baseDir", tempDir.toString());
        setField(mojo, "session", null);

        assertThrows(org.apache.maven.plugin.MojoExecutionException.class, () -> mojo.execute());
    }

    @Test
    void checkApiContract_withHttpContractNotEndingApi_reportsIssue() throws Exception {
        Path sourceDir = tempDir.resolve("mango-demo-api/src/main/java/io/mango/demo/api");
        Files.createDirectories(sourceDir);
        Files.writeString(sourceDir.resolve("AuthContract.java"), """
                package io.mango.demo.api;

                import org.springframework.web.bind.annotation.PostMapping;

                public interface AuthContract {
                    @PostMapping("/login")
                    void login();
                }
                """);

        CheckMojo mojo = new CheckMojo();
        setField(mojo, "rule", "api-contract");
        setField(mojo, "baseDir", tempDir.toString());
        setField(mojo, "session", null);

        assertThrows(org.apache.maven.plugin.MojoExecutionException.class, () -> mojo.execute());
    }

    @Test
    void checkApiContract_withRegistryApiName_reportsIssue() throws Exception {
        Path sourceDir = tempDir.resolve("mango-demo-api/src/main/java/io/mango/demo/api");
        Files.createDirectories(sourceDir);
        Files.writeString(sourceDir.resolve("DemoRegistryApi.java"), """
                package io.mango.demo.api;

                public interface DemoRegistryApi {
                }
                """);

        CheckMojo mojo = new CheckMojo();
        setField(mojo, "rule", "api-contract");
        setField(mojo, "baseDir", tempDir.toString());
        setField(mojo, "session", null);

        assertThrows(org.apache.maven.plugin.MojoExecutionException.class, () -> mojo.execute());
    }

    @Test
    void checkApiContract_withControllerHoldingApi_reportsIssue() throws Exception {
        Path sourceDir = tempDir.resolve("mango-demo-starter/src/main/java/io/mango/demo/starter");
        Files.createDirectories(sourceDir);
        Files.writeString(sourceDir.resolve("DemoController.java"), """
                package io.mango.demo.starter;

                import io.mango.demo.api.DemoApi;
                import org.springframework.web.bind.annotation.RestController;

                @RestController
                public class DemoController {
                    private final DemoApi demoApi;
                }
                """);

        CheckMojo mojo = new CheckMojo();
        setField(mojo, "rule", "api-contract");
        setField(mojo, "baseDir", tempDir.toString());
        setField(mojo, "session", null);

        assertThrows(org.apache.maven.plugin.MojoExecutionException.class, () -> mojo.execute());
    }

    @Test
    void checkApiContract_withServiceImplementingApi_reportsIssue() throws Exception {
        Path sourceDir = tempDir.resolve("mango-demo-core/src/main/java/io/mango/demo/core");
        Files.createDirectories(sourceDir);
        Files.writeString(sourceDir.resolve("DemoService.java"), """
                package io.mango.demo.core;

                import io.mango.demo.api.DemoApi;

                public class DemoService implements DemoApi {
                }
                """);

        CheckMojo mojo = new CheckMojo();
        setField(mojo, "rule", "api-contract");
        setField(mojo, "baseDir", tempDir.toString());
        setField(mojo, "session", null);

        assertThrows(org.apache.maven.plugin.MojoExecutionException.class, () -> mojo.execute());
    }

    @Test
    void checkApiContract_withFeignClientNotExtendingApi_reportsIssue() throws Exception {
        Path sourceDir = tempDir.resolve("mango-demo-starter-remote/src/main/java/io/mango/demo/starter/remote");
        Files.createDirectories(sourceDir);
        Files.writeString(sourceDir.resolve("DemoFeignClient.java"), """
                package io.mango.demo.starter.remote;

                import org.springframework.cloud.openfeign.FeignClient;

                @FeignClient(name = "mango-demo", path = "/demo")
                public interface DemoFeignClient {
                }
                """);

        CheckMojo mojo = new CheckMojo();
        setField(mojo, "rule", "api-contract");
        setField(mojo, "baseDir", tempDir.toString());
        setField(mojo, "session", null);

        assertThrows(org.apache.maven.plugin.MojoExecutionException.class, () -> mojo.execute());
    }

    @Test
    void checkApiContract_withMultipleMethodParameters_reportsIssue() throws Exception {
        Path sourceDir = tempDir.resolve("mango-demo-api/src/main/java/io/mango/demo/api");
        Files.createDirectories(sourceDir);
        Files.writeString(sourceDir.resolve("DemoApi.java"), """
                package io.mango.demo.api;

                import io.mango.common.result.R;

                public interface DemoApi {
                    R<Boolean> updateValue(Long id, String value);
                }
                """);

        CheckMojo mojo = new CheckMojo();
        setField(mojo, "rule", "api-contract");
        setField(mojo, "baseDir", tempDir.toString());
        setField(mojo, "session", null);

        assertThrows(org.apache.maven.plugin.MojoExecutionException.class, () -> mojo.execute());
    }

    @Test
    void checkPathParam_withPathVariable_reportsIssue() throws Exception {
        Path sourceDir = tempDir.resolve("mango-demo-starter/src/main/java/io/mango/demo/starter");
        Files.createDirectories(sourceDir);
        Files.writeString(sourceDir.resolve("DemoController.java"), """
                package io.mango.demo.starter;

                import org.springframework.web.bind.annotation.GetMapping;
                import org.springframework.web.bind.annotation.PathVariable;
                import org.springframework.web.bind.annotation.RestController;

                @RestController
                public class DemoController {
                    @GetMapping("/demo/{id}")
                    String detail(@PathVariable Long id) {
                        return "ok";
                    }
                }
                """);

        CheckMojo mojo = new CheckMojo();
        setField(mojo, "rule", "path-param");
        setField(mojo, "baseDir", tempDir.toString());
        setField(mojo, "session", null);

        assertThrows(org.apache.maven.plugin.MojoExecutionException.class, () -> mojo.execute());
    }

    @Test
    void checkPathParam_withRequestParam_passes() throws Exception {
        Path sourceDir = tempDir.resolve("mango-demo-starter/src/main/java/io/mango/demo/starter");
        Files.createDirectories(sourceDir);
        Files.writeString(sourceDir.resolve("DemoController.java"), """
                package io.mango.demo.starter;

                import org.springframework.web.bind.annotation.GetMapping;
                import org.springframework.web.bind.annotation.RequestParam;
                import org.springframework.web.bind.annotation.RestController;

                @RestController
                public class DemoController {
                    @GetMapping("/demo/detail")
                    String detail(@RequestParam Long id) {
                        return "ok";
                    }
                }
                """);

        CheckMojo mojo = new CheckMojo();
        setField(mojo, "rule", "path-param");
        setField(mojo, "baseDir", tempDir.toString());
        setField(mojo, "session", null);

        assertDoesNotThrow(() -> mojo.execute());
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
    void checkKvKey_withKvPrefix_reportsIssue() throws Exception {
        // given
        Path sourceDir = tempDir.resolve("mango-demo-core/src/main/java/io/mango/demo/core");
        Files.createDirectories(sourceDir);
        Files.writeString(sourceDir.resolve("DemoService.java"), """
                package io.mango.demo.core;

                import io.mango.infra.kv.api.annotation.Locker;

                public class DemoService {
                    @Locker(key = "mango:kv:prod:lock:order:1")
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
    void checkPersistenceSchema_withRequiredColumns_passes() throws Exception {
        // given
        Path migrationFile = tempDir.resolve("mango-demo-core/src/main/resources/db/migration/demo/V1__init_demo.sql");
        Files.createDirectories(migrationFile.getParent());
        Files.writeString(migrationFile, """
                CREATE TABLE demo_user (
                    `id` bigint NOT NULL,
                    `created_by` bigint DEFAULT NULL,
                    `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
                    `updated_by` bigint DEFAULT NULL,
                    `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
                    `tenant_id` varchar(64) NOT NULL DEFAULT 'default',
                    PRIMARY KEY (`id`)
                );
                """);

        CheckMojo mojo = new CheckMojo();
        setField(mojo, "rule", "persistence-schema");
        setField(mojo, "baseDir", tempDir.toString());
        setField(mojo, "session", null);

        assertDoesNotThrow(() -> mojo.execute());
    }

    @Test
    void checkPersistenceSchema_withMissingRequiredColumns_reportsIssue() throws Exception {
        // given
        Path migrationFile = tempDir.resolve("mango-demo-core/src/main/resources/db/migration/demo/V1__init_demo.sql");
        Files.createDirectories(migrationFile.getParent());
        Files.writeString(migrationFile, """
                CREATE TABLE demo_user (
                    `id` bigint NOT NULL,
                    `name` varchar(64) NOT NULL,
                    PRIMARY KEY (`id`)
                );
                """);

        CheckMojo mojo = new CheckMojo();
        setField(mojo, "rule", "persistence-schema");
        setField(mojo, "baseDir", tempDir.toString());
        setField(mojo, "session", null);

        assertThrows(org.apache.maven.plugin.MojoExecutionException.class, () -> mojo.execute());
    }

    @Test
    void checkPersistenceSchema_withDisabledTable_passes() throws Exception {
        // given
        Path migrationFile = tempDir.resolve("mango-demo-core/src/main/resources/db/migration/demo/V1__init_external.sql");
        Files.createDirectories(migrationFile.getParent());
        Files.writeString(migrationFile, """
                -- mango-check: disable persistence-audit-fields reason=外部系统同步表
                CREATE TABLE external_event (
                    `id` bigint NOT NULL,
                    `payload` text,
                    PRIMARY KEY (`id`)
                );
                """);

        CheckMojo mojo = new CheckMojo();
        setField(mojo, "rule", "persistence-schema");
        setField(mojo, "baseDir", tempDir.toString());
        setField(mojo, "session", null);

        assertDoesNotThrow(() -> mojo.execute());
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
    void checkTestFixture_withReportFile_writesJsonReport() throws Exception {
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
        Path reportFile = tempDir.resolve("target/check-report.json");

        // when
        CheckMojo mojo = new CheckMojo();
        setField(mojo, "rule", "test-fixture");
        setField(mojo, "baseDir", tempDir.toString());
        setField(mojo, "reportFile", reportFile.toString());
        setField(mojo, "session", null);

        // then
        assertDoesNotThrow(() -> mojo.execute());
        String report = Files.readString(reportFile);
        assertTrue(report.contains("\"passed\" : true"));
        assertTrue(report.contains("\"issues\" : [ ]"));
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
