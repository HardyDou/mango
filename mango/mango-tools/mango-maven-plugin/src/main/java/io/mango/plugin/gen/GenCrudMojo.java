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
 * 生成 CRUD 代码。
 * 技术栈: MyBatis-Plus + Entity/Query/Command/VO 分离
 *
 * mvn mango:gen-crud -Dmodule=user -Dentity=User -Dtable=sys_user
 *
 * @author Mango
 */
@Mojo(name = "gen-crud", defaultPhase = LifecyclePhase.GENERATE_SOURCES)
public class GenCrudMojo extends AbstractMojo {

    @Parameter(property = "module", required = true)
    private String module;

    @Parameter(property = "entity", required = true)
    private String entity;

    @Parameter(property = "table", required = true)
    private String table;

    @Parameter(property = "baseDir", defaultValue = "${project.basedir}")
    private String baseDir;

    @Override
    public void execute() throws MojoExecutionException {
        getLog().info("Generating CRUD for module=" + module + ", entity=" + entity + ", table=" + table);

        try {
            Path apiDir = Paths.get(baseDir,
                    "mango-" + module + "/mango-" + module + "-api/src/main/java/io/mango/" + module + "/api");
            Files.createDirectories(apiDir);
            generateApi(apiDir);

            Path entityDir = Paths.get(baseDir,
                    "mango-" + module + "/mango-" + module + "-core/src/main/java/io/mango/" + module + "/core/entity");
            Files.createDirectories(entityDir);
            generateEntity(entityDir);

            Path queryDir = Paths.get(baseDir,
                    "mango-" + module + "/mango-" + module + "-api/src/main/java/io/mango/" + module + "/api/query");
            Files.createDirectories(queryDir);
            generatePageQuery(queryDir);

            Path commandDir = Paths.get(baseDir,
                    "mango-" + module + "/mango-" + module + "-api/src/main/java/io/mango/" + module + "/api/command");
            Files.createDirectories(commandDir);
            generateCreateCommand(commandDir);
            generateUpdateCommand(commandDir);

            Path voDir = Paths.get(baseDir,
                    "mango-" + module + "/mango-" + module + "-api/src/main/java/io/mango/" + module + "/api/vo");
            Files.createDirectories(voDir);
            generateVO(voDir);

            Path mapperDir = Paths.get(baseDir,
                    "mango-" + module + "/mango-" + module + "-core/src/main/java/io/mango/" + module + "/core/mapper");
            Files.createDirectories(mapperDir);
            generateMapper(mapperDir);

            Path serviceDir = Paths.get(baseDir,
                    "mango-" + module + "/mango-" + module + "-core/src/main/java/io/mango/" + module + "/core/service");
            Files.createDirectories(serviceDir);
            Path serviceImplDir = Paths.get(baseDir,
                    "mango-" + module + "/mango-" + module + "-core/src/main/java/io/mango/" + module + "/core/service/impl");
            Files.createDirectories(serviceImplDir);
            generateService(serviceDir, serviceImplDir);

            Path controllerDir = Paths.get(baseDir,
                    "mango-" + module + "/mango-" + module + "-starter/src/main/java/io/mango/" + module + "/starter/controller");
            Files.createDirectories(controllerDir);
            generateController(controllerDir);

            Path remoteDir = Paths.get(baseDir,
                    "mango-" + module + "/mango-" + module + "-starter-remote/src/main/java/io/mango/" + module + "/starter/remote");
            Files.createDirectories(remoteDir);
            generateFeignClient(remoteDir);
            generateRemoteAutoConfiguration(remoteDir);

            Path bizCodeDir = Paths.get(baseDir,
                    "mango-" + module + "/mango-" + module + "-api/src/main/java/io/mango/" + module + "/api/enums");
            Files.createDirectories(bizCodeDir);
            generateBizCode(bizCodeDir);

            getLog().info("CRUD generated successfully to: mango-" + module);
        } catch (IOException e) {
            throw new MojoExecutionException("Failed to generate CRUD", e);
        }
    }

    private String pkg(String path, Object... args) {
        return String.format(path, args);
    }

    private void generateController(Path dir) throws IOException {
        String entityName = toPascalCase(entity);
        String camelEntity = toCamelCase(entity);
        String resourcePath = camelEntity.toLowerCase();

        String content =
            "package io.mango." + module + ".starter.controller;\n\n" +
            "import io.mango." + module + ".api." + entityName + "Api;\n" +
            "import io.mango.common.result.R;\n" +
            "import io.mango.common.vo.PageResult;\n" +
            "import io.mango." + module + ".api.command.Create" + entityName + "Command;\n" +
            "import io.mango." + module + ".api.command.Update" + entityName + "Command;\n" +
            "import io.mango." + module + ".api.query." + entityName + "PageQuery;\n" +
            "import io.mango." + module + ".api.vo." + entityName + "VO;\n" +
            "import io.mango." + module + ".core.service.I" + entityName + "Service;\n" +
            "import lombok.RequiredArgsConstructor;\n" +
            "import org.springframework.web.bind.annotation.*;\n\n" +
            "@RestController\n" +
            "@RequestMapping(\"/" + resourcePath + "\")\n" +
            "@RequiredArgsConstructor\n" +
            "public class " + entityName + "Controller implements " + entityName + "Api {\n\n" +
            "    private final I" + entityName + "Service " + camelEntity + "Service;\n\n" +
            "    @Override\n" +
            "    @GetMapping(\"/page\")\n" +
            "    public R<PageResult<" + entityName + "VO>> page(" + entityName + "PageQuery query) {\n" +
            "        return R.ok(" + camelEntity + "Service.page(query));\n" +
            "    }\n\n" +
            "    @Override\n" +
            "    @GetMapping(\"/{id}\")\n" +
            "    public R<" + entityName + "VO> get(@PathVariable Long id) {\n" +
            "        return R.ok(" + camelEntity + "Service.getById(id));\n" +
            "    }\n\n" +
            "    @Override\n" +
            "    @PostMapping\n" +
            "    public R<Void> save(@RequestBody Create" + entityName + "Command command) {\n" +
            "        " + camelEntity + "Service.save(command);\n" +
            "        return R.ok();\n" +
            "    }\n\n" +
            "    @Override\n" +
            "    @PutMapping(\"/{id}\")\n" +
            "    public R<Void> update(@PathVariable Long id, @RequestBody Update" + entityName + "Command command) {\n" +
            "        command.setId(id);\n" +
            "        " + camelEntity + "Service.update(command);\n" +
            "        return R.ok();\n" +
            "    }\n\n" +
            "    @Override\n" +
            "    @DeleteMapping(\"/{id}\")\n" +
            "    public R<Void> delete(@PathVariable Long id) {\n" +
            "        " + camelEntity + "Service.delete(id);\n" +
            "        return R.ok();\n" +
            "    }\n" +
            "}\n";
        Files.writeString(dir.resolve(entityName + "Controller.java"), content);
    }

    private void generateApi(Path dir) throws IOException {
        String entityName = toPascalCase(entity);

        String content =
            "package io.mango." + module + ".api;\n\n" +
            "import io.mango.common.result.R;\n" +
            "import io.mango.common.vo.PageResult;\n" +
            "import io.mango." + module + ".api.command.Create" + entityName + "Command;\n" +
            "import io.mango." + module + ".api.command.Update" + entityName + "Command;\n" +
            "import io.mango." + module + ".api.query." + entityName + "PageQuery;\n" +
            "import io.mango." + module + ".api.vo." + entityName + "VO;\n" +
            "import org.springframework.web.bind.annotation.PathVariable;\n" +
            "import org.springframework.web.bind.annotation.RequestBody;\n\n" +
            "public interface " + entityName + "Api {\n\n" +
            "    R<PageResult<" + entityName + "VO>> page(" + entityName + "PageQuery query);\n\n" +
            "    R<" + entityName + "VO> get(@PathVariable Long id);\n\n" +
            "    R<Void> save(@RequestBody Create" + entityName + "Command command);\n\n" +
            "    R<Void> update(@PathVariable Long id, @RequestBody Update" + entityName + "Command command);\n\n" +
            "    R<Void> delete(@PathVariable Long id);\n" +
            "}\n";
        Files.writeString(dir.resolve(entityName + "Api.java"), content);
    }

    private void generateService(Path serviceDir, Path serviceImplDir) throws IOException {
        String entityName = toPascalCase(entity);
        String camelEntity = toCamelCase(entity);

        String content =
            "package io.mango." + module + ".core.service;\n\n" +
            "import io.mango.common.vo.PageResult;\n" +
            "import io.mango." + module + ".api.command.Create" + entityName + "Command;\n" +
            "import io.mango." + module + ".api.command.Update" + entityName + "Command;\n" +
            "import io.mango." + module + ".api.query." + entityName + "PageQuery;\n" +
            "import io.mango." + module + ".api.vo." + entityName + "VO;\n\n" +
            "public interface I" + entityName + "Service {\n\n" +
            "    " + entityName + "VO getById(Long id);\n\n" +
            "    PageResult<" + entityName + "VO> page(" + entityName + "PageQuery query);\n\n" +
            "    void save(Create" + entityName + "Command command);\n\n" +
            "    void update(Update" + entityName + "Command command);\n\n" +
            "    void delete(Long id);\n" +
            "}\n";
        Files.writeString(serviceDir.resolve("I" + entityName + "Service.java"), content);

        String implContent =
            "package io.mango." + module + ".core.service.impl;\n\n" +
            "import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;\n" +
            "import com.baomidou.mybatisplus.core.metadata.IPage;\n" +
            "import com.baomidou.mybatisplus.extension.plugins.pagination.Page;\n" +
            "import io.mango.common.result.Require;\n" +
            "import io.mango.common.vo.PageResult;\n" +
            "import io.mango." + module + ".api.command.Create" + entityName + "Command;\n" +
            "import io.mango." + module + ".api.command.Update" + entityName + "Command;\n" +
            "import io.mango." + module + ".api.query." + entityName + "PageQuery;\n" +
            "import io.mango." + module + ".api.vo." + entityName + "VO;\n" +
            "import io.mango." + module + ".core.entity." + entityName + "Entity;\n" +
            "import io.mango." + module + ".core.mapper." + entityName + "Mapper;\n" +
            "import io.mango." + module + ".core.service.I" + entityName + "Service;\n" +
            "import lombok.RequiredArgsConstructor;\n" +
            "import org.springframework.stereotype.Service;\n\n" +
            "@Service\n" +
            "@RequiredArgsConstructor\n" +
            "public class " + entityName + "ServiceImpl implements I" + entityName + "Service {\n\n" +
            "    private final " + entityName + "Mapper " + camelEntity + "Mapper;\n\n" +
            "    @Override\n" +
            "    public " + entityName + "VO getById(Long id) {\n" +
            "        " + entityName + "Entity entity = " + camelEntity + "Mapper.selectById(id);\n" +
            "        Require.notNull(entity, 404, \"记录不存在\");\n" +
            "        return toVO(entity);\n" +
            "    }\n\n" +
            "    @Override\n" +
            "    public PageResult<" + entityName + "VO> page(" + entityName + "PageQuery query) {\n" +
            "        LambdaQueryWrapper<" + entityName + "Entity> wrapper = new LambdaQueryWrapper<>();\n" +
            "        IPage<" + entityName + "Entity> page = " + camelEntity + "Mapper.selectPage(\n" +
            "                new Page<>(query.getPage(), query.getSize()), wrapper);\n" +
            "        return PageResult.of(page.getRecords().stream().map(this::toVO).toList(),\n" +
            "                page.getTotal(), page.getCurrent(), page.getSize());\n" +
            "    }\n\n" +
            "    @Override\n" +
            "    public void save(Create" + entityName + "Command command) {\n" +
            "        " + camelEntity + "Mapper.insert(toEntity(command));\n" +
            "    }\n\n" +
            "    @Override\n" +
            "    public void update(Update" + entityName + "Command command) {\n" +
            "        Require.notNull(command.getId(), 400, \"ID 不能为空\");\n" +
            "        " + camelEntity + "Mapper.updateById(toEntity(command));\n" +
            "    }\n\n" +
            "    @Override\n" +
            "    public void delete(Long id) {\n" +
            "        " + camelEntity + "Mapper.deleteById(id);\n" +
            "    }\n\n" +
            "    private " + entityName + "VO toVO(" + entityName + "Entity entity) {\n" +
            "        if (entity == null) {\n" +
            "            return null;\n" +
            "        }\n" +
            "        " + entityName + "VO vo = new " + entityName + "VO();\n" +
            "        vo.setId(entity.getId());\n" +
            "        return vo;\n" +
            "    }\n\n" +
            "    private " + entityName + "Entity toEntity(Create" + entityName + "Command command) {\n" +
            "        if (command == null) {\n" +
            "            return null;\n" +
            "        }\n" +
            "        " + entityName + "Entity entity = new " + entityName + "Entity();\n" +
            "        return entity;\n" +
            "    }\n\n" +
            "    private " + entityName + "Entity toEntity(Update" + entityName + "Command command) {\n" +
            "        if (command == null) {\n" +
            "            return null;\n" +
            "        }\n" +
            "        " + entityName + "Entity entity = new " + entityName + "Entity();\n" +
            "        entity.setId(command.getId());\n" +
            "        return entity;\n" +
            "    }\n" +
            "}\n";
        Files.writeString(serviceImplDir.resolve(entityName + "ServiceImpl.java"), implContent);
    }

    private void generateMapper(Path dir) throws IOException {
        String entityName = toPascalCase(entity);

        String content =
            "package io.mango." + module + ".core.mapper;\n\n" +
            "import com.baomidou.mybatisplus.core.mapper.BaseMapper;\n" +
            "import io.mango." + module + ".core.entity." + entityName + "Entity;\n" +
            "import org.apache.ibatis.annotations.Mapper;\n\n" +
            "@Mapper\n" +
            "public interface " + entityName + "Mapper extends BaseMapper<" + entityName + "Entity> {\n" +
            "}\n";
        Files.writeString(dir.resolve(entityName + "Mapper.java"), content);
    }

    private void generateEntity(Path dir) throws IOException {
        String entityName = toPascalCase(entity);

        String content =
            "package io.mango." + module + ".core.entity;\n\n" +
            "import com.baomidou.mybatisplus.annotation.IdType;\n" +
            "import com.baomidou.mybatisplus.annotation.TableId;\n" +
            "import com.baomidou.mybatisplus.annotation.TableName;\n" +
            "import lombok.Data;\n\n" +
            "@Data\n" +
            "@TableName(\"" + table + "\")\n" +
            "public class " + entityName + "Entity {\n\n" +
            "    @TableId(type = IdType.AUTO)\n" +
            "    private Long id;\n\n" +
            "}\n";
        Files.writeString(dir.resolve(entityName + "Entity.java"), content);
    }

    private void generatePageQuery(Path dir) throws IOException {
        String entityName = toPascalCase(entity);

        String content =
            "package io.mango." + module + ".api.query;\n\n" +
            "import io.mango.common.po.PageQuery;\n" +
            "import lombok.Data;\n" +
            "import lombok.EqualsAndHashCode;\n\n" +
            "@Data\n" +
            "@EqualsAndHashCode(callSuper = true)\n" +
            "public class " + entityName + "PageQuery extends PageQuery {\n" +
            "}\n";
        Files.writeString(dir.resolve(entityName + "PageQuery.java"), content);
    }

    private void generateCreateCommand(Path dir) throws IOException {
        String entityName = toPascalCase(entity);

        String content =
            "package io.mango." + module + ".api.command;\n\n" +
            "import lombok.Data;\n\n" +
            "@Data\n" +
            "public class Create" + entityName + "Command {\n" +
            "}\n";
        Files.writeString(dir.resolve("Create" + entityName + "Command.java"), content);
    }

    private void generateUpdateCommand(Path dir) throws IOException {
        String entityName = toPascalCase(entity);

        String content =
            "package io.mango." + module + ".api.command;\n\n" +
            "import lombok.Data;\n\n" +
            "@Data\n" +
            "public class Update" + entityName + "Command {\n\n" +
            "    private Long id;\n\n" +
            "}\n";
        Files.writeString(dir.resolve("Update" + entityName + "Command.java"), content);
    }

    private void generateVO(Path dir) throws IOException {
        String entityName = toPascalCase(entity);

        String content =
            "package io.mango." + module + ".api.vo;\n\n" +
            "import lombok.Data;\n\n" +
            "@Data\n" +
            "public class " + entityName + "VO {\n\n" +
            "    private Long id;\n\n" +
            "}\n";
        Files.writeString(dir.resolve(entityName + "VO.java"), content);
    }

    private void generateBizCode(Path dir) throws IOException {
        String entityName = toPascalCase(entity);

        String content =
            "package io.mango." + module + ".api.enums;\n\n" +
            "import io.mango.common.result.BizCode;\n" +
            "import lombok.AllArgsConstructor;\n" +
            "import lombok.Getter;\n\n" +
            "@Getter\n" +
            "@AllArgsConstructor\n" +
            "public enum " + entityName + "Code implements BizCode {\n\n" +
            "    SUCCESS(200, \"操作成功\"),\n" +
            "    NOT_FOUND(404, \"资源不存在\"),\n" +
            "    VALIDATION_ERROR(400, \"参数校验失败\");\n\n" +
            "    private final int code;\n" +
            "    private final String message;\n" +
            "}\n";
        Files.writeString(dir.resolve(entityName + "Code.java"), content);
    }

    private void generateFeignClient(Path dir) throws IOException {
        String entityName = toPascalCase(entity);
        String resourcePath = toCamelCase(entity).toLowerCase();

        String content =
            "package io.mango." + module + ".starter.remote;\n\n" +
            "import io.mango." + module + ".api." + entityName + "Api;\n" +
            "import org.springframework.cloud.openfeign.FeignClient;\n\n" +
            "@FeignClient(name = \"" + module + "-service\", path = \"/" + resourcePath + "\")\n" +
            "public interface " + entityName + "FeignClient extends " + entityName + "Api {\n" +
            "}\n";
        Files.writeString(dir.resolve(entityName + "FeignClient.java"), content);
    }

    private void generateRemoteAutoConfiguration(Path dir) throws IOException {
        String entityName = toPascalCase(entity);

        String content =
            "package io.mango." + module + ".starter.remote;\n\n" +
            "import org.springframework.cloud.openfeign.EnableFeignClients;\n" +
            "import org.springframework.context.annotation.Configuration;\n\n" +
            "@Configuration\n" +
            "@EnableFeignClients(basePackageClasses = " + entityName + "FeignClient.class)\n" +
            "public class " + entityName + "RemoteAutoConfiguration {\n" +
            "}\n";
        Files.writeString(dir.resolve(entityName + "RemoteAutoConfiguration.java"), content);
    }

    private String toPascalCase(String input) {
        if (input == null || input.isEmpty()) return input;
        String[] parts = input.toLowerCase().replace("-", "_").split("_");
        StringBuilder sb = new StringBuilder();
        for (String part : parts) {
            if (part.length() > 0) {
                sb.append(part.substring(0, 1).toUpperCase()).append(part.substring(1));
            }
        }
        return sb.toString();
    }

    private String toCamelCase(String input) {
        if (input == null || input.isEmpty()) return input;
        return toPascalCase(input).substring(0, 1).toLowerCase() + toPascalCase(input).substring(1);
    }
}
