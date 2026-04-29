package io.mango.plugin.gen;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.lang.reflect.Field;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

/**
 * GenPermissionMojo 单元测试
 */
class GenPermissionMojoTest {

    @TempDir
    Path tempDir;

    private void setField(Object target, String fieldName, Object value) throws Exception {
        Field field = target.getClass().getDeclaredField(fieldName);
        field.setAccessible(true);
        field.set(target, value);
    }

    @Test
    void execute_withValidModule_createsMenuSql() throws Exception {
        // given
        GenPermissionMojo mojo = new GenPermissionMojo();
        setField(mojo, "module", "user");
        setField(mojo, "outputDir", tempDir.resolve("generated").toString());

        // when
        mojo.execute();

        // then
        Path sqlFile = tempDir.resolve("generated/menu_user.sql");
        assertTrue(Files.exists(sqlFile), "menu_user.sql should be created");

        String content = Files.readString(sqlFile);
        assertTrue(content.contains("INSERT INTO authorization_menu"), "SQL should contain INSERT INTO authorization_menu");
        assertTrue(content.contains("user管理"), "SQL should contain menu name");
        assertTrue(content.contains("user:list"), "SQL should contain list permission");
        assertTrue(content.contains("user:add"), "SQL should contain add permission");
        assertTrue(content.contains("user:edit"), "SQL should contain edit permission");
        assertTrue(content.contains("user:delete"), "SQL should contain delete permission");
    }

    @Test
    void execute_withCustomModel_usesCustomPrefix() throws Exception {
        // given
        GenPermissionMojo mojo = new GenPermissionMojo();
        setField(mojo, "module", "role");
        setField(mojo, "model", "system");
        setField(mojo, "outputDir", tempDir.resolve("generated").toString());

        // when
        mojo.execute();

        // then
        Path sqlFile = tempDir.resolve("generated/menu_role.sql");
        String content = Files.readString(sqlFile);
        assertTrue(content.contains("system:role:list"), "SQL should use custom model prefix");
        assertTrue(content.contains("system:role:add"), "SQL should use custom model prefix");
    }

    @Test
    void generateMenuSQL_containsAllCrudOperations() throws Exception {
        // given
        GenPermissionMojo mojo = new GenPermissionMojo();
        setField(mojo, "module", "product");
        setField(mojo, "outputDir", tempDir.toString());

        // when
        mojo.execute();

        // then
        Path sqlFile = tempDir.resolve("menu_product.sql");
        String content = Files.readString(sqlFile);

        // Verify all CRUD operations are present
        assertTrue(content.contains("product:list"), "Should contain list permission");
        assertTrue(content.contains("product:add"), "Should contain add permission");
        assertTrue(content.contains("product:edit"), "Should contain edit permission");
        assertTrue(content.contains("product:delete"), "Should contain delete permission");
        assertTrue(content.contains("product:export"), "Should contain export permission");
        assertTrue(content.contains("product:import"), "Should contain import permission");

        // Verify comment explains permissions
        assertTrue(content.contains("detail"), "Should document detail permission in comments");
    }

    @Test
    void execute_withModuleUpperCase_normalizesToLowercase() throws Exception {
        // given
        GenPermissionMojo mojo = new GenPermissionMojo();
        setField(mojo, "module", "PERMISSION");
        setField(mojo, "outputDir", tempDir.resolve("generated").toString());

        // when
        mojo.execute();

        // then
        Path sqlFile = tempDir.resolve("generated/menu_permission.sql");
        String content = Files.readString(sqlFile);
        assertTrue(content.contains("permission管理"), "Should use lowercase module name");
    }

    @Test
    void execute_createsOutputDirectory() throws Exception {
        // given
        GenPermissionMojo mojo = new GenPermissionMojo();
        setField(mojo, "module", "dept");
        setField(mojo, "outputDir", tempDir.resolve("sql/output").toString());

        // when
        mojo.execute();

        // then
        assertTrue(Files.exists(tempDir.resolve("sql/output/menu_dept.sql")));
    }
}
