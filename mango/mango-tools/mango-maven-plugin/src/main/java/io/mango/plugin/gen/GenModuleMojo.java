package io.mango.plugin.gen;

import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * 生成模块脚手架
 *
 * mvn mango:gen-module -Dname=user
 *
 * 生成结构:
 * mango-user/
 * ├── mango-user-api/
 * │   └── src/main/java/io/mango/user/
 * │       └── enums/
 * ├── mango-user-core/
 * │   └── src/main/java/io/mango/user/
 * │       ├── po/, vo/, mapper/, service/
 * ├── mango-user-starter/
 * │   └── src/main/java/io/mango/user/
 * │       └── controller/
 * └── mango-user-starter-remote/
 *
 * @author hardy
 */
@Mojo(name = "gen-module", defaultPhase = LifecyclePhase.GENERATE_SOURCES)
public class GenModuleMojo extends AbstractMojo {

    @Parameter(property = "name", required = true)
    private String name;

    @Parameter(property = "baseDir", defaultValue = "${project.basedir}/../")
    private String baseDir;

    @Override
    public void execute() throws MojoExecutionException {
        getLog().info("Generating module: " + name);

        try {
            String moduleName = "mango-" + name.toLowerCase();
            Path moduleDir = Paths.get(baseDir, moduleName);
            Files.createDirectories(moduleDir);

            // 创建各子模块目录
            createApiModule(moduleDir, name);
            createCoreModule(moduleDir, name);
            createStarterModule(moduleDir, name);
            createStarterRemoteModule(moduleDir, name);
            createPom(moduleDir, name);

            getLog().info("Module generated: " + moduleDir);
        } catch (IOException e) {
            throw new MojoExecutionException("Failed to generate module", e);
        }
    }

    private void createApiModule(Path moduleDir, String name) throws IOException {
        Path apiDir = moduleDir.resolve("mango-" + name.toLowerCase() + "-api/src/main/java/io/mango/" + name.toLowerCase() + "/enums");
        Files.createDirectories(apiDir);

        // 生成模块 README
        Path readmeDir = moduleDir.resolve("mango-" + name.toLowerCase() + "-api");
        Files.writeString(readmeDir.resolve("README.md"),
                "# " + name + " API Module\n\n接口定义层，仅定义接口和枚举。\n\n作者：" + currentAuthor() + "\n");
    }

    private void createCoreModule(Path moduleDir, String name) throws IOException {
        Path coreDir = moduleDir.resolve("mango-" + name.toLowerCase() + "-core/src/main/java/io/mango/" + name.toLowerCase());
        Files.createDirectories(coreDir);
        Files.createDirectories(coreDir.resolve("po"));
        Files.createDirectories(coreDir.resolve("vo"));
        Files.createDirectories(coreDir.resolve("mapper"));
        Files.createDirectories(coreDir.resolve("service/impl"));
    }

    private void createStarterModule(Path moduleDir, String name) throws IOException {
        Path starterDir = moduleDir.resolve("mango-" + name.toLowerCase() + "-starter/src/main/java/io/mango/" + name.toLowerCase() + "/controller");
        Files.createDirectories(starterDir);
    }

    private void createStarterRemoteModule(Path moduleDir, String name) throws IOException {
        Path remoteDir = moduleDir.resolve("mango-" + name.toLowerCase() + "-starter-remote/src/main/java/io/mango/" + name.toLowerCase());
        Files.createDirectories(remoteDir);
    }

    private void createPom(Path moduleDir, String name) throws IOException {
        String lowerName = name.toLowerCase();
        String capName = capitalize(name);
        String groupId = "io.mango." + lowerName.replace('-', '.');
        String content = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
                "<project xmlns=\"http://maven.apache.org/POM/4.0.0\"\n" +
                "         xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\"\n" +
                "         xsi:schemaLocation=\"http://maven.apache.org/POM/4.0.0 http://maven.apache.org/xsd/maven-4.0.0.xsd\">\n" +
                "    <modelVersion>4.0.0</modelVersion>\n" +
                "\n" +
                "    <groupId>" + groupId + "</groupId>\n" +
                "    <artifactId>mango-" + lowerName + "</artifactId>\n" +
                "    <version>1.0.0-SNAPSHOT</version>\n" +
                "    <packaging>pom</packaging>\n" +
                "\n" +
                "    <name>Mango " + capName + "</name>\n" +
                "\n" +
                "    <modules>\n" +
                "        <module>mango-" + lowerName + "-api</module>\n" +
                "        <module>mango-" + lowerName + "-core</module>\n" +
                "        <module>mango-" + lowerName + "-starter</module>\n" +
                "        <module>mango-" + lowerName + "-starter-remote</module>\n" +
                "    </modules>\n" +
                "</project>\n";
        Files.writeString(moduleDir.resolve("pom.xml"), content);
    }

    private String capitalize(String s) {
        if (s == null || s.isEmpty()) return s;
        return s.substring(0, 1).toUpperCase() + s.substring(1).toLowerCase();
    }

    private String currentAuthor() {
        String userName = System.getProperty("user.name");
        if (userName == null || userName.isBlank()) {
            userName = System.getenv("USER");
        }
        if (userName == null || userName.isBlank()) {
            userName = System.getenv("USERNAME");
        }
        return userName == null || userName.isBlank() ? "unknown" : userName.trim();
    }
}
