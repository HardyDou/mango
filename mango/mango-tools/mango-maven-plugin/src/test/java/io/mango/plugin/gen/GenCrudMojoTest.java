package io.mango.plugin.gen;

import static org.junit.jupiter.api.Assertions.*;

import io.mango.architecture.MangoPmdChecker;
import io.mango.common.result.Require;
import io.mango.plugin.check.CheckMojo;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import javax.tools.ToolProvider;

/** GenCrudMojo 单元测试 */
class GenCrudMojoTest {

    @TempDir Path tempDir;

    private GenCrudMojo newMojo() throws Exception {
        GenCrudMojo mojo = new GenCrudMojo();
        setField(mojo, "entityDisplayName", "用户");
        return mojo;
    }

    private void setField(Object target, String fieldName, Object value) throws Exception {
        Field field = target.getClass().getDeclaredField(fieldName);
        field.setAccessible(true);
        field.set(target, value);
    }

    private Object invokeMethod(
            Object target, String methodName, Class<?>[] paramTypes, Object... args)
            throws Exception {
        Method method = target.getClass().getDeclaredMethod(methodName, paramTypes);
        method.setAccessible(true);
        return method.invoke(target, args);
    }

    @Test
    void execute_withValidParams_createsCrudFiles() throws Exception {
        // given
        createModuleStructure();

        GenCrudMojo mojo = newMojo();
        setField(mojo, "module", "user");
        setField(mojo, "entity", "User");
        setField(mojo, "table", "sys_user");
        setField(mojo, "baseDir", tempDir.toString());

        // when
        mojo.execute();

        // then
        assertTrue(
                Files.exists(
                        tempDir.resolve(
                                "mango-user/mango-user-core/src/main/java/io/mango/user/core/entity/UserEntity.java")));
        assertTrue(
                Files.exists(
                        tempDir.resolve(
                                "mango-user/mango-user-api/src/main/java/io/mango/user/api/vo/UserVO.java")));
        assertTrue(
                Files.exists(
                        tempDir.resolve(
                                "mango-user/mango-user-core/src/main/java/io/mango/user/core/mapper/UserMapper.java")));
        assertTrue(
                Files.exists(
                        tempDir.resolve(
                                "mango-user/mango-user-core/src/main/java/io/mango/user/core/service/IUserService.java")));
        assertTrue(
                Files.exists(
                        tempDir.resolve(
                                "mango-user/mango-user-core/src/main/java/io/mango/user/core/service/impl/UserService.java")));
        assertTrue(
                Files.exists(
                        tempDir.resolve(
                                "mango-user/mango-user-starter/src/main/java/io/mango/user/starter/controller/UserController.java")));
        assertTrue(
                Files.exists(
                        tempDir.resolve(
                                "mango-user/mango-user-api/src/main/java/io/mango/user/api/enums/UserCode.java")));
        assertTrue(
                Files.exists(
                        tempDir.resolve(
                                "mango-user/mango-user-core/src/main/resources/db/migration/user/V1__init_sys_user.sql")));
    }

    @Test
    void execute_withSnakeCaseEntity_normalizesToPascalCase() throws Exception {
        // given
        createModuleStructure();

        GenCrudMojo mojo = newMojo();
        setField(mojo, "module", "user");
        setField(mojo, "entity", "user_profile");
        setField(mojo, "table", "sys_user_profile");
        setField(mojo, "baseDir", tempDir.toString());

        // when
        mojo.execute();

        // then
        assertTrue(
                Files.exists(
                        tempDir.resolve(
                                "mango-user/mango-user-core/src/main/java/io/mango/user/core/entity/UserProfileEntity.java")));
        assertTrue(
                Files.exists(
                        tempDir.resolve(
                                "mango-user/mango-user-api/src/main/java/io/mango/user/api/vo/UserProfileVO.java")));
    }

    @Test
    void toPascalCase_withSnakeCase_returnsPascalCase() throws Exception {
        // given
        GenCrudMojo mojo = newMojo();

        // when & then
        assertEquals(
                "UserProfile",
                invokeMethod(mojo, "toPascalCase", new Class[] {String.class}, "user_profile"));
        assertEquals(
                "User", invokeMethod(mojo, "toPascalCase", new Class[] {String.class}, "user"));
        assertEquals(
                "UserName",
                invokeMethod(mojo, "toPascalCase", new Class[] {String.class}, "UserName"));
        assertEquals(
                "XmlParser",
                invokeMethod(mojo, "toPascalCase", new Class[] {String.class}, "xml_parser"));
    }

    @Test
    void toCamelCase_withPascalCase_returnsCamelCase() throws Exception {
        // given
        GenCrudMojo mojo = newMojo();

        // when & then
        assertEquals(
                "userProfile",
                invokeMethod(mojo, "toCamelCase", new Class[] {String.class}, "UserProfile"));
        assertEquals("user", invokeMethod(mojo, "toCamelCase", new Class[] {String.class}, "user"));
        assertEquals(
                "userName",
                invokeMethod(mojo, "toCamelCase", new Class[] {String.class}, "UserName"));
    }

    @Test
    void execute_requiresChineseDisplayNameAndValidModulePackage() throws Exception {
        GenCrudMojo missingDisplayName = new GenCrudMojo();
        setField(missingDisplayName, "module", "user");
        setField(missingDisplayName, "entity", "User");
        setField(missingDisplayName, "table", "sys_user");
        setField(missingDisplayName, "baseDir", tempDir.toString());
        var missingName =
                assertThrows(
                        org.apache.maven.plugin.MojoExecutionException.class,
                        missingDisplayName::execute);
        assertTrue(missingName.getMessage().contains("entityDisplayName"));

        GenCrudMojo invalidModule = newMojo();
        setField(invalidModule, "module", "user-profile");
        setField(invalidModule, "entity", "User");
        setField(invalidModule, "table", "sys_user");
        setField(invalidModule, "baseDir", tempDir.toString());
        var invalidPackage =
                assertThrows(
                        org.apache.maven.plugin.MojoExecutionException.class,
                        invalidModule::execute);
        assertTrue(invalidPackage.getMessage().contains("module"));
    }

    @Test
    void generatePO_containsTableAnnotation() throws Exception {
        // given
        createModuleStructure();

        GenCrudMojo mojo = newMojo();
        setField(mojo, "module", "user");
        setField(mojo, "entity", "User");
        setField(mojo, "table", "sys_user");
        setField(mojo, "baseDir", tempDir.toString());

        // when
        mojo.execute();

        // then
        Path poFile =
                tempDir.resolve(
                        "mango-user/mango-user-core/src/main/java/io/mango/user/core/entity/UserEntity.java");
        String content = Files.readString(poFile);
        assertTrue(
                content.contains("@TableName(\"sys_user\")"),
                "PO should contain @TableName annotation");
        assertTrue(content.contains("class UserEntity"), "Entity should have correct class name");
        assertTrue(
                content.contains("extends TenantEntity"),
                "Entity should inherit standard tenant entity");
        assertFalse(content.contains("@Data"), "Entity should not use @Data");
    }

    @Test
    void generateMigration_containsStandardPersistenceColumns() throws Exception {
        // given
        createModuleStructure();

        GenCrudMojo mojo = newMojo();
        setField(mojo, "module", "user");
        setField(mojo, "entity", "User");
        setField(mojo, "table", "sys_user");
        setField(mojo, "baseDir", tempDir.toString());

        // when
        mojo.execute();

        // then
        Path migrationFile =
                tempDir.resolve(
                        "mango-user/mango-user-core/src/main/resources/db/migration/user/V1__init_sys_user.sql");
        String content = Files.readString(migrationFile);
        assertTrue(content.contains("CREATE TABLE IF NOT EXISTS `sys_user`"));
        assertTrue(content.contains("`id` bigint NOT NULL COMMENT '主键'"));
        assertFalse(content.toLowerCase().contains("auto_increment"));
        assertTrue(content.contains("`created_by` bigint"));
        assertTrue(content.contains("`created_at` datetime"));
        assertTrue(content.contains("`updated_by` bigint"));
        assertTrue(content.contains("`updated_at` datetime"));
        assertTrue(content.contains("`tenant_id` varchar(64)"));
        assertTrue(content.contains("`org_id` bigint"));
    }

    @Test
    void generateController_containsRequestMapping() throws Exception {
        // given
        createModuleStructure();

        GenCrudMojo mojo = newMojo();
        setField(mojo, "module", "user");
        setField(mojo, "entity", "User");
        setField(mojo, "table", "sys_user");
        setField(mojo, "baseDir", tempDir.toString());

        // when
        mojo.execute();

        // then
        Path controllerFile =
                tempDir.resolve(
                        "mango-user/mango-user-starter/src/main/java/io/mango/user/starter/controller/UserController.java");
        String content = Files.readString(controllerFile);
        assertTrue(
                content.contains("@RequestMapping(\"/user\")"),
                "Controller should have @RequestMapping");
        assertTrue(
                content.contains("implements UserApi"),
                "Controller should implement the API contract");
        assertTrue(content.contains("@Validated"), "Controller should enable Bean Validation");
        assertTrue(
                content.contains("private final IUserService service;"),
                "Controller should only depend on the service interface");
        assertTrue(content.contains("R<PersistencePageResult<UserVO>> page("));
        assertTrue(content.contains("@ParameterObject @Valid UserPageQuery query"));
        assertFalse(content.contains("selectPage("), "Controller should not hand-roll pagination");
    }

    @Test
    void generateBizCode_containsSuccessAndErrorCodes() throws Exception {
        // given
        createModuleStructure();

        GenCrudMojo mojo = newMojo();
        setField(mojo, "module", "user");
        setField(mojo, "entity", "User");
        setField(mojo, "table", "sys_user");
        setField(mojo, "baseDir", tempDir.toString());

        // when
        mojo.execute();

        // then
        Path bizCodeFile =
                tempDir.resolve(
                        "mango-user/mango-user-api/src/main/java/io/mango/user/api/enums/UserCode.java");
        String content = Files.readString(bizCodeFile);
        assertTrue(content.contains("SUCCESS(200"), "BizCode should contain SUCCESS code");
        assertTrue(content.contains("NOT_FOUND(404"), "BizCode should contain NOT_FOUND code");
    }

    @Test
    void generateApi_doesNotContainWebAnnotations() throws Exception {
        // given
        createModuleStructure();

        GenCrudMojo mojo = newMojo();
        setField(mojo, "module", "user");
        setField(mojo, "entity", "User");
        setField(mojo, "table", "sys_user");
        setField(mojo, "baseDir", tempDir.toString());

        // when
        mojo.execute();

        // then
        Path apiFile =
                tempDir.resolve(
                        "mango-user/mango-user-api/src/main/java/io/mango/user/api/UserApi.java");
        String content = Files.readString(apiFile);
        assertFalse(content.contains("org.springframework.web.bind.annotation"));
        assertFalse(content.contains("@PathVariable"));
        assertFalse(content.contains("@RequestBody"));
    }

    @Test
    void generateApi_updateSignature_matchesControllerImplementation() throws Exception {
        // given
        createModuleStructure();

        GenCrudMojo mojo = newMojo();
        setField(mojo, "module", "user");
        setField(mojo, "entity", "User");
        setField(mojo, "table", "sys_user");
        setField(mojo, "baseDir", tempDir.toString());

        // when
        mojo.execute();

        // then
        String apiContent =
                Files.readString(
                        tempDir.resolve(
                                "mango-user/mango-user-api/src/main/java/io/mango/user/api/UserApi.java"));
        String controllerContent =
                Files.readString(
                        tempDir.resolve(
                                "mango-user/mango-user-starter/src/main/java/io/mango/user/starter/controller/UserController.java"));
        String feignContent =
                Files.readString(
                        tempDir.resolve(
                                "mango-user/mango-user-starter-remote/src/main/java/io/mango/user/starter/remote/UserFeignClient.java"));
        assertTrue(apiContent.contains("R<Boolean> update(@Valid UpdateUserCommand command);"));
        assertFalse(apiContent.contains("R<Void> update(Long id, UpdateUserCommand command);"));
        assertTrue(controllerContent.contains("implements UserApi"));
        assertTrue(controllerContent.contains("@Validated"));
        assertTrue(feignContent.contains("@PostMapping(\"/update\")"));
        assertTrue(
                feignContent.contains(
                        "R<Boolean> update(@RequestBody @Valid UpdateUserCommand command);"));
        assertTrue(
                feignContent.contains(
                        "@FeignClient(name = \"mango-user\", contextId = \"userFeignClient\", path"
                            + " = \"/user\")"));
    }

    @Test
    void execute_generatesMangoPersistenceBaseline() throws Exception {
        // given
        createModuleStructure();

        GenCrudMojo mojo = newMojo();
        setField(mojo, "module", "user");
        setField(mojo, "entity", "User");
        setField(mojo, "table", "sys_user");
        setField(mojo, "baseDir", tempDir.toString());

        // when
        mojo.execute();

        // then
        String serviceContent =
                Files.readString(
                        tempDir.resolve(
                                "mango-user/mango-user-core/src/main/java/io/mango/user/core/service/IUserService.java"));
        String implContent =
                Files.readString(
                        tempDir.resolve(
                                "mango-user/mango-user-core/src/main/java/io/mango/user/core/service/impl/UserService.java"));
        String controllerContent =
                Files.readString(
                        tempDir.resolve(
                                "mango-user/mango-user-starter/src/main/java/io/mango/user/starter/controller/UserController.java"));
        assertTrue(serviceContent.contains("extends MangoTypedCrudService<"));
        assertTrue(
                serviceContent.contains(
                        "UserEntity, CreateUserCommand, UpdateUserCommand, UserPageQuery, UserVO,"
                            + " Long"));
        assertTrue(implContent.contains("extends MangoCrudServiceImpl<UserMapper, UserEntity>"));
        assertTrue(controllerContent.contains("implements UserApi"));
        assertTrue(controllerContent.contains("@RequestBody @Valid CreateUserCommand command"));
        assertFalse(implContent.contains("extends ServiceImpl"));
        assertFalse(implContent.contains("selectPage("));
        assertFalse(implContent.contains("new Page<"));
        assertFalse(implContent.contains("setTenantId("));
    }

    @Test
    void execute_withDataScopeResource_generatesDataScopeHook() throws Exception {
        // given
        createModuleStructure();

        GenCrudMojo mojo = newMojo();
        setField(mojo, "module", "user");
        setField(mojo, "entity", "User");
        setField(mojo, "table", "sys_user");
        setField(mojo, "dataScopeResource", "user:list");
        setField(mojo, "baseDir", tempDir.toString());

        // when
        mojo.execute();

        // then
        String implContent =
                Files.readString(
                        tempDir.resolve(
                                "mango-user/mango-user-core/src/main/java/io/mango/user/core/service/impl/UserService.java"));
        assertTrue(implContent.contains("private final DataScopeApplier dataScopeApplier;"));
        assertTrue(
                implContent.contains(
                        "protected void applyDataScope(QueryWrapper<UserEntity> wrapper, Object"
                            + " query)"));
        assertTrue(implContent.contains("dataScopeApplier.apply("));
        assertTrue(implContent.contains("\"user:list\""));
        assertTrue(implContent.contains(".tableName(\"sys_user\")"));
        assertTrue(implContent.contains(".selfField(\"created_by\")"));
        assertTrue(implContent.contains(".orgField(\"org_id\")"));
        assertTrue(implContent.contains(".tenantField(\"tenant_id\")"));
    }

    @Test
    void execute_generatedLayersPassApiContractGate() throws Exception {
        createModuleStructure();

        GenCrudMojo generator = newMojo();
        setField(generator, "module", "user");
        setField(generator, "entity", "User");
        setField(generator, "table", "sys_user");
        setField(generator, "baseDir", tempDir.toString());
        generator.execute();

        CheckMojo checker = new CheckMojo();
        setField(checker, "rule", "api-contract");
        setField(checker, "baseDir", tempDir.toString());
        setField(checker, "session", null);

        assertDoesNotThrow(checker::execute);
    }

    @Test
    void execute_generatedLayersPassPmd7ArchitectureGate() throws Exception {
        createModuleStructure();

        GenCrudMojo generator = newMojo();
        setField(generator, "module", "user");
        setField(generator, "entity", "User");
        setField(generator, "table", "sys_user");
        setField(generator, "baseDir", tempDir.toString());
        generator.execute();

        Path module = tempDir.resolve("mango-user");
        List<Path> sourceDirectories =
                List.of(
                        module.resolve("mango-user-api/src/main/java"),
                        module.resolve("mango-user-core/src/main/java"),
                        module.resolve("mango-user-starter/src/main/java"),
                        module.resolve("mango-user-starter-remote/src/main/java"));

        Path commonClasspath = classpathEntry(Require.class);
        Path generatedClasses = compileGeneratedBizCode(module, commonClasspath);
        List<Path> auxiliaryClasspath = List.of(commonClasspath, generatedClasses);
        var issues = new MangoPmdChecker().check(sourceDirectories, "21", auxiliaryClasspath);
        assertTrue(
                issues.isEmpty(),
                () -> "Generated CRUD must pass PMD architecture gate: " + issues);
    }

    private Path compileGeneratedBizCode(Path module, Path commonClasspath) throws Exception {
        Path output = tempDir.resolve("generated-contract-classes");
        Files.createDirectories(output);
        Path source =
                module.resolve(
                        "mango-user-api/src/main/java/io/mango/user/api/enums/UserCode.java");
        int exitCode =
                ToolProvider.getSystemJavaCompiler()
                        .run(
                                null,
                                null,
                                null,
                                "-classpath",
                                commonClasspath.toString(),
                                "-d",
                                output.toString(),
                                source.toString());
        assertEquals(0, exitCode, "Generated BizCode contract must compile against mango-common");
        return output;
    }

    private Path classpathEntry(Class<?> type) throws Exception {
        return Path.of(type.getProtectionDomain().getCodeSource().getLocation().toURI());
    }

    /** Helper: Create minimal module directory structure */
    private void createModuleStructure() throws Exception {
        Path moduleDir = tempDir.resolve("mango-user");
        Files.createDirectories(
                moduleDir.resolve("mango-user-api/src/main/java/io/mango/user/api/enums"));
        Files.createDirectories(
                moduleDir.resolve("mango-user-core/src/main/java/io/mango/user/core/entity"));
        Files.createDirectories(
                moduleDir.resolve("mango-user-api/src/main/java/io/mango/user/api/vo"));
        Files.createDirectories(
                moduleDir.resolve("mango-user-core/src/main/java/io/mango/user/core/mapper"));
        Files.createDirectories(
                moduleDir.resolve("mango-user-core/src/main/java/io/mango/user/core/service/impl"));
        Files.createDirectories(
                moduleDir.resolve(
                        "mango-user-starter/src/main/java/io/mango/user/starter/controller"));
        Files.createDirectories(
                moduleDir.resolve(
                        "mango-user-starter-remote/src/main/java/io/mango/user/starter/remote"));

        // Create minimal parent pom
        Files.writeString(
                moduleDir.resolve("pom.xml"),
                """
                <?xml version="1.0" encoding="UTF-8"?>
                <project>
                    <groupId>io.mango</groupId>
                    <artifactId>mango-user</artifactId>
                    <version>1.0.0-SNAPSHOT</version>
                    <packaging>pom</packaging>
                    <modules>
                        <module>mango-user-api</module>
                        <module>mango-user-core</module>
                        <module>mango-user-starter</module>
                        <module>mango-user-starter-remote</module>
                    </modules>
                </project>
                """);
    }
}
