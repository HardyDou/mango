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
 * 生成权限菜单 SQL
 *
 * mvn mango:gen-permission -Dmodule=user
 *
 * 生成权限码格式: {model}:{module}:{action}
 * 例如: system:user:add, system:user:edit
 *
 * @author hardy
 */
@Mojo(name = "gen-permission", defaultPhase = LifecyclePhase.GENERATE_SOURCES)
public class GenPermissionMojo extends AbstractMojo {

    @Parameter(property = "module", required = true)
    private String module;

    @Parameter(property = "model", defaultValue = "system")
    private String model;

    @Parameter(property = "outputDir", defaultValue = "${project.basedir}/generated")
    private String outputDir;

    @Override
    public void execute() throws MojoExecutionException {
        getLog().info("Generating permission SQL for module: " + module);

        try {
            Path outDir = Paths.get(outputDir);
            Files.createDirectories(outDir);

            String sql = generateMenuSQL();
            Files.writeString(outDir.resolve("menu_" + module + ".sql"), sql);

            getLog().info("Permission SQL generated: " + outDir.resolve("menu_" + module + ".sql"));
        } catch (IOException e) {
            throw new MojoExecutionException("Failed to generate permission SQL", e);
        }
    }

    private String generateMenuSQL() {
        String moduleName = module.toLowerCase();
        String permPrefix = model + ":" + moduleName;
        long baseId = 900_000_000_000L + ((long) (moduleName.hashCode() & 0x7fffffff) * 10L);
        long rootMenuId = baseId + 1;

        return """
-- ----------------------------
-- %s 菜单与权限
-- 由 Mango 脚手架生成
-- ----------------------------

-- 1. 菜单
INSERT INTO authorization_menu
(menu_id, parent_id, menu_type, menu_name, menu_code, path, component, sort, status, visible) VALUES
(%d, 0, 2, '%s管理', '%s', '/%s', 'Layout', 10, 1, 1),
(%d, %d, 3, '%s列表', '%s:list', '/%s/list', '%s/%s/index', 1, 1, 1),
(%d, %d, 3, '%s新增', '%s:add', '/%s/add', '%s/%s/add', 2, 1, 1),
(%d, %d, 3, '%s编辑', '%s:edit', '/%s/edit', '%s/%s/edit', 3, 1, 1),
(%d, %d, 3, '%s删除', '%s:delete', '/%s/delete', NULL, 4, 1, 1),
(%d, %d, 3, '%s导出', '%s:export', '/%s/export', NULL, 5, 1, 1),
(%d, %d, 3, '%s导入', '%s:import', '/%s/import', NULL, 6, 1, 1);

-- 2. 权限码
-- %s:add    - 新增
-- %s:edit   - 编辑
-- %s:delete - 删除
-- %s:list   - 列表查询
-- %s:export  - 导出
-- %s:import  - 导入
-- %s:detail  - 详情查看
""".formatted(
                moduleName,
                rootMenuId,
                moduleName,
                permPrefix,
                moduleName,
                baseId + 2,
                rootMenuId,
                moduleName,
                permPrefix,
                moduleName,
                moduleName, moduleName,
                baseId + 3,
                rootMenuId,
                moduleName,
                permPrefix,
                moduleName,
                moduleName, moduleName,
                baseId + 4,
                rootMenuId,
                moduleName,
                permPrefix,
                moduleName,
                moduleName, moduleName,
                baseId + 5,
                rootMenuId,
                moduleName,
                permPrefix,
                moduleName,
                baseId + 6,
                rootMenuId,
                moduleName,
                permPrefix,
                moduleName,
                baseId + 7,
                rootMenuId,
                moduleName,
                permPrefix,
                moduleName,
                permPrefix,
                permPrefix,
                permPrefix,
                permPrefix,
                permPrefix,
                permPrefix,
                permPrefix
        );
    }
}
