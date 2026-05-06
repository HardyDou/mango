package io.mango.plugin.gen;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

/**
 * GenCrudMojo 单元测试
 */
class GenCrudMojoTest {

    @TempDir
    Path tempDir;

    private void setField(Object target, String fieldName, Object value) throws Exception {
        Field field = target.getClass().getDeclaredField(fieldName);
        field.setAccessible(true);
        field.set(target, value);
    }

    private Object invokeMethod(Object target, String methodName, Class<?>[] paramTypes, Object... args) throws Exception {
        Method method = target.getClass().getDeclaredMethod(methodName, paramTypes);
        method.setAccessible(true);
        return method.invoke(target, args);
    }

    @Test
    void execute_withValidParams_createsCrudFiles() throws Exception {
        // given
        createModuleStructure();

        GenCrudMojo mojo = new GenCrudMojo();
        setField(mojo, "module", "user");
        setField(mojo, "entity", "User");
        setField(mojo, "table", "sys_user");
        setField(mojo, "baseDir", tempDir.toString());

        // when
        mojo.execute();

        // then
        assertTrue(Files.exists(tempDir.resolve("mango-user/mango-user-core/src/main/java/io/mango/user/core/entity/UserEntity.java")));
        assertTrue(Files.exists(tempDir.resolve("mango-user/mango-user-api/src/main/java/io/mango/user/api/vo/UserVO.java")));
        assertTrue(Files.exists(tempDir.resolve("mango-user/mango-user-core/src/main/java/io/mango/user/core/mapper/UserMapper.java")));
        assertTrue(Files.exists(tempDir.resolve("mango-user/mango-user-core/src/main/java/io/mango/user/core/service/IUserService.java")));
        assertTrue(Files.exists(tempDir.resolve("mango-user/mango-user-core/src/main/java/io/mango/user/core/service/impl/UserServiceImpl.java")));
        assertTrue(Files.exists(tempDir.resolve("mango-user/mango-user-starter/src/main/java/io/mango/user/starter/controller/UserController.java")));
        assertTrue(Files.exists(tempDir.resolve("mango-user/mango-user-api/src/main/java/io/mango/user/api/enums/UserCode.java")));
        assertTrue(Files.exists(tempDir.resolve("mango-user/mango-user-core/src/main/resources/db/migration/user/V1__init_sys_user.sql")));
    }

    @Test
    void execute_withSnakeCaseEntity_normalizesToPascalCase() throws Exception {
        // given
        createModuleStructure();

        GenCrudMojo mojo = new GenCrudMojo();
        setField(mojo, "module", "user");
        setField(mojo, "entity", "user_profile");
        setField(mojo, "table", "sys_user_profile");
        setField(mojo, "baseDir", tempDir.toString());

        // when
        mojo.execute();

        // then
        assertTrue(Files.exists(tempDir.resolve("mango-user/mango-user-core/src/main/java/io/mango/user/core/entity/UserProfileEntity.java")));
        assertTrue(Files.exists(tempDir.resolve("mango-user/mango-user-api/src/main/java/io/mango/user/api/vo/UserProfileVO.java")));
    }

    @Test
    void toPascalCase_withSnakeCase_returnsPascalCase() throws Exception {
        // given
        GenCrudMojo mojo = new GenCrudMojo();

        // when & then
        assertEquals("UserProfile", invokeMethod(mojo, "toPascalCase", new Class[]{String.class}, "user_profile"));
        assertEquals("User", invokeMethod(mojo, "toPascalCase", new Class[]{String.class}, "user"));
        assertEquals("Username", invokeMethod(mojo, "toPascalCase", new Class[]{String.class}, "UserName"));
        assertEquals("XmlParser", invokeMethod(mojo, "toPascalCase", new Class[]{String.class}, "xml_parser"));
    }

    @Test
    void toCamelCase_withPascalCase_returnsCamelCase() throws Exception {
        // given
        GenCrudMojo mojo = new GenCrudMojo();

        // when & then
        assertEquals("userprofile", invokeMethod(mojo, "toCamelCase", new Class[]{String.class}, "UserProfile"));
        assertEquals("user", invokeMethod(mojo, "toCamelCase", new Class[]{String.class}, "user"));
        assertEquals("username", invokeMethod(mojo, "toCamelCase", new Class[]{String.class}, "UserName"));
    }

    @Test
    void generatePO_containsTableAnnotation() throws Exception {
        // given
        createModuleStructure();

        GenCrudMojo mojo = new GenCrudMojo();
        setField(mojo, "module", "user");
        setField(mojo, "entity", "User");
        setField(mojo, "table", "sys_user");
        setField(mojo, "baseDir", tempDir.toString());

        // when
        mojo.execute();

        // then
        Path poFile = tempDir.resolve("mango-user/mango-user-core/src/main/java/io/mango/user/core/entity/UserEntity.java");
        String content = Files.readString(poFile);
        assertTrue(content.contains("@TableName(\"sys_user\")"), "PO should contain @TableName annotation");
        assertTrue(content.contains("class UserEntity"), "Entity should have correct class name");
        assertTrue(content.contains("extends TenantEntity"), "Entity should inherit standard tenant entity");
        assertFalse(content.contains("@Data"), "Entity should not use @Data");
    }

    @Test
    void generateMigration_containsStandardPersistenceColumns() throws Exception {
        // given
        createModuleStructure();

        GenCrudMojo mojo = new GenCrudMojo();
        setField(mojo, "module", "user");
        setField(mojo, "entity", "User");
        setField(mojo, "table", "sys_user");
        setField(mojo, "baseDir", tempDir.toString());

        // when
        mojo.execute();

        // then
        Path migrationFile = tempDir.resolve("mango-user/mango-user-core/src/main/resources/db/migration/user/V1__init_sys_user.sql");
        String content = Files.readString(migrationFile);
        assertTrue(content.contains("CREATE TABLE IF NOT EXISTS `sys_user`"));
        assertTrue(content.contains("`id` bigint NOT NULL COMMENT '主键'"));
        assertFalse(content.toLowerCase().contains("auto_increment"));
        assertTrue(content.contains("`created_by` bigint"));
        assertTrue(content.contains("`created_at` datetime"));
        assertTrue(content.contains("`updated_by` bigint"));
        assertTrue(content.contains("`updated_at` datetime"));
        assertTrue(content.contains("`tenant_id` varchar(64)"));
    }

    @Test
    void generateController_containsRequestMapping() throws Exception {
        // given
        createModuleStructure();

        GenCrudMojo mojo = new GenCrudMojo();
        setField(mojo, "module", "user");
        setField(mojo, "entity", "User");
        setField(mojo, "table", "sys_user");
        setField(mojo, "baseDir", tempDir.toString());

        // when
        mojo.execute();

        // then
        Path controllerFile = tempDir.resolve("mango-user/mango-user-starter/src/main/java/io/mango/user/starter/controller/UserController.java");
        String content = Files.readString(controllerFile);
        assertTrue(content.contains("@RequestMapping(\"/user\")"), "Controller should have @RequestMapping");
        assertTrue(content.contains("@GetMapping(\"/page\")"), "Controller should have page endpoint");
        assertTrue(content.contains("@PostMapping"), "Controller should have POST endpoint");
    }

    @Test
    void generateBizCode_containsSuccessAndErrorCodes() throws Exception {
        // given
        createModuleStructure();

        GenCrudMojo mojo = new GenCrudMojo();
        setField(mojo, "module", "user");
        setField(mojo, "entity", "User");
        setField(mojo, "table", "sys_user");
        setField(mojo, "baseDir", tempDir.toString());

        // when
        mojo.execute();

        // then
        Path bizCodeFile = tempDir.resolve("mango-user/mango-user-api/src/main/java/io/mango/user/api/enums/UserCode.java");
        String content = Files.readString(bizCodeFile);
        assertTrue(content.contains("SUCCESS(200"), "BizCode should contain SUCCESS code");
        assertTrue(content.contains("NOT_FOUND(404"), "BizCode should contain NOT_FOUND code");
    }

    @Test
    void generateApi_doesNotContainWebAnnotations() throws Exception {
        // given
        createModuleStructure();

        GenCrudMojo mojo = new GenCrudMojo();
        setField(mojo, "module", "user");
        setField(mojo, "entity", "User");
        setField(mojo, "table", "sys_user");
        setField(mojo, "baseDir", tempDir.toString());

        // when
        mojo.execute();

        // then
        Path apiFile = tempDir.resolve("mango-user/mango-user-api/src/main/java/io/mango/user/api/UserApi.java");
        String content = Files.readString(apiFile);
        assertFalse(content.contains("org.springframework.web.bind.annotation"));
        assertFalse(content.contains("@PathVariable"));
        assertFalse(content.contains("@RequestBody"));
    }

    /**
     * Helper: Create minimal module directory structure
     */
    private void createModuleStructure() throws Exception {
        Path moduleDir = tempDir.resolve("mango-user");
        Files.createDirectories(moduleDir.resolve("mango-user-api/src/main/java/io/mango/user/api/enums"));
        Files.createDirectories(moduleDir.resolve("mango-user-core/src/main/java/io/mango/user/core/entity"));
        Files.createDirectories(moduleDir.resolve("mango-user-api/src/main/java/io/mango/user/api/vo"));
        Files.createDirectories(moduleDir.resolve("mango-user-core/src/main/java/io/mango/user/core/mapper"));
        Files.createDirectories(moduleDir.resolve("mango-user-core/src/main/java/io/mango/user/core/service/impl"));
        Files.createDirectories(moduleDir.resolve("mango-user-starter/src/main/java/io/mango/user/starter/controller"));
        Files.createDirectories(moduleDir.resolve("mango-user-starter-remote/src/main/java/io/mango/user/starter/remote"));

        // Create minimal parent pom
        Files.writeString(moduleDir.resolve("pom.xml"), """
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
