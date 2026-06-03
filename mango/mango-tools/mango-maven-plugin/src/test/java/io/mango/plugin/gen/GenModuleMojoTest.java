package io.mango.plugin.gen;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

/**
 * GenModuleMojo 单元测试
 */
class GenModuleMojoTest {

    @TempDir
    Path tempDir;

    private void setField(Object target, String fieldName, Object value) throws Exception {
        Field field = target.getClass().getDeclaredField(fieldName);
        field.setAccessible(true);
        field.set(target, value);
    }

    @Test
    void execute_withValidName_createsModuleStructure() throws Exception {
        // given
        GenModuleMojo mojo = new GenModuleMojo();
        setField(mojo, "name", "user");
        setField(mojo, "baseDir", tempDir.toString());

        // when
        mojo.execute();

        // then
        Path moduleDir = tempDir.resolve("mango-user");
        assertTrue(Files.exists(moduleDir), "mango-user directory should exist");
        assertTrue(Files.exists(moduleDir.resolve("pom.xml")), "pom.xml should exist");
        assertTrue(Files.exists(moduleDir.resolve("mango-user-api")), "mango-user-api should exist");
        assertTrue(Files.exists(moduleDir.resolve("mango-user-core")), "mango-user-core should exist");
        assertTrue(Files.exists(moduleDir.resolve("mango-user-starter")), "mango-user-starter should exist");
        assertTrue(Files.exists(moduleDir.resolve("mango-user-starter-remote")), "mango-user-starter-remote should exist");
    }

    @Test
    void execute_withUpperCaseName_normalizesToLowerCase() throws Exception {
        // given
        GenModuleMojo mojo = new GenModuleMojo();
        setField(mojo, "name", "USER");
        setField(mojo, "baseDir", tempDir.toString());

        // when
        mojo.execute();

        // then
        Path moduleDir = tempDir.resolve("mango-user");
        assertTrue(Files.exists(moduleDir), "mango-user directory should exist (normalized to lowercase)");
    }

    @Test
    void execute_createsApiModuleWithEnumsDir() throws Exception {
        // given
        GenModuleMojo mojo = new GenModuleMojo();
        setField(mojo, "name", "role");
        setField(mojo, "baseDir", tempDir.toString());

        // when
        mojo.execute();

        // then
        Path enumsDir = tempDir.resolve("mango-role/mango-role-api/src/main/java/io/mango/role/enums");
        assertTrue(Files.exists(enumsDir), "enums directory should exist in api module");
    }

    @Test
    void execute_createsCoreModuleStructure() throws Exception {
        // given
        GenModuleMojo mojo = new GenModuleMojo();
        setField(mojo, "name", "dept");
        setField(mojo, "baseDir", tempDir.toString());

        // when
        mojo.execute();

        // then
        Path coreDir = tempDir.resolve("mango-dept/mango-dept-core/src/main/java/io/mango/dept");
        assertTrue(Files.exists(coreDir.resolve("po")), "po directory should exist");
        assertTrue(Files.exists(coreDir.resolve("vo")), "vo directory should exist");
        assertTrue(Files.exists(coreDir.resolve("mapper")), "mapper directory should exist");
        assertTrue(Files.exists(coreDir.resolve("service/impl")), "service/impl directory should exist");
    }

    @Test
    void execute_createsStarterModule() throws Exception {
        // given
        GenModuleMojo mojo = new GenModuleMojo();
        setField(mojo, "name", "auth");
        setField(mojo, "baseDir", tempDir.toString());

        // when
        mojo.execute();

        // then
        Path starterDir = tempDir.resolve("mango-auth/mango-auth-starter/src/main/java/io/mango/auth/controller");
        assertTrue(Files.exists(starterDir), "controller directory should exist in starter module");
    }

    @Test
    void capitalize_withLowerCase_returnsCapitalized() throws Exception {
        // given
        GenModuleMojo mojo = new GenModuleMojo();

        // use reflection to invoke private method
        Method method = GenModuleMojo.class.getDeclaredMethod("capitalize", String.class);
        method.setAccessible(true);

        // when & then
        assertEquals("User", method.invoke(mojo, "user"));
        assertEquals("User", method.invoke(mojo, "USER"));
        assertEquals("Test", method.invoke(mojo, "test"));
        assertNull(method.invoke(mojo, (Object) null));
        assertEquals("", method.invoke(mojo, ""));
    }

    @Test
    void execute_withUpperCaseName_normalizesDisplayName() throws Exception {
        // given
        GenModuleMojo mojo = new GenModuleMojo();
        setField(mojo, "name", "USER");
        setField(mojo, "baseDir", tempDir.toString());

        // when
        mojo.execute();

        // then
        String pomContent = Files.readString(tempDir.resolve("mango-user/pom.xml"));
        assertTrue(pomContent.contains("<name>Mango User</name>"));
        assertFalse(pomContent.contains("<name>Mango USER</name>"));
    }
}
