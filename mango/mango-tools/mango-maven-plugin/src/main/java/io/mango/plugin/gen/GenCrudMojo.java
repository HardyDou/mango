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
 * 技术栈: Mango CRUD 基线 + Entity/Query/Command/VO 分离
 *
 * mvn mango:gen-crud -Dmodule=user -Dentity=User -Dtable=sys_user
 *
 * @author hardy
 */
@Mojo(name = "gen-crud", defaultPhase = LifecyclePhase.GENERATE_SOURCES)
public class GenCrudMojo extends AbstractMojo {

    @Parameter(property = "module", required = true)
    private String module;

    @Parameter(property = "entity", required = true)
    private String entity;

    @Parameter(property = "table", required = true)
    private String table;

    @Parameter(property = "dataScopeResource")
    private String dataScopeResource;

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

            Path migrationDir = Paths.get(baseDir,
                    "mango-" + module + "/mango-" + module + "-core/src/main/resources/db/migration/" + module);
            Files.createDirectories(migrationDir);
            generateMigration(migrationDir);

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

    private void generateController(Path dir) throws IOException {
        String entityName = toPascalCase(entity);
        String camelEntity = toCamelCase(entity);
        String resourcePath = camelEntity.toLowerCase();
        String author = currentAuthor();

        String content =
            "package io.mango." + module + ".starter.controller;\n\n" +
            "import io.mango." + module + ".api.command.Create" + entityName + "Command;\n" +
            "import io.mango." + module + ".api.command.Update" + entityName + "Command;\n" +
            "import io.mango." + module + ".api.query." + entityName + "PageQuery;\n" +
            "import io.mango." + module + ".core.service.I" + entityName + "Service;\n" +
            "import io.mango.infra.persistence.web.starter.controller.BaseCrudController;\n" +
            "import org.springframework.web.bind.annotation.RequestMapping;\n" +
            "import org.springframework.web.bind.annotation.RestController;\n\n" +
            javaDoc(entityName + " 管理接口。", author) +
            "@RestController\n" +
            "@RequestMapping(\"/" + resourcePath + "\")\n" +
            "public class " + entityName + "Controller extends BaseCrudController<\n" +
            "        I" + entityName + "Service,\n" +
            "        Create" + entityName + "Command,\n" +
            "        Update" + entityName + "Command,\n" +
            "        " + entityName + "PageQuery> {\n\n" +
            "    public " + entityName + "Controller(I" + entityName + "Service service) {\n" +
            "        super(service);\n" +
            "    }\n\n" +
            "    @Override\n" +
            "    protected Class<" + entityName + "PageQuery> queryType() {\n" +
            "        return " + entityName + "PageQuery.class;\n" +
            "    }\n" +
            "}\n";
        Files.writeString(dir.resolve(entityName + "Controller.java"), content);
    }

    private void generateApi(Path dir) throws IOException {
        String entityName = toPascalCase(entity);
        String author = currentAuthor();

        String content =
            "package io.mango." + module + ".api;\n\n" +
            "import io.mango.common.result.R;\n" +
            "import io.mango.infra.persistence.api.crud.DeleteCommand;\n" +
            "import io.mango.infra.persistence.api.query.PersistencePageResult;\n" +
            "import io.mango." + module + ".api.command.Create" + entityName + "Command;\n" +
            "import io.mango." + module + ".api.command.Update" + entityName + "Command;\n" +
            "import io.mango." + module + ".api.query." + entityName + "PageQuery;\n" +
            "import io.mango." + module + ".api.vo." + entityName + "VO;\n" +
            "import jakarta.validation.Valid;\n\n" +
            javaDoc(entityName + " 跨模块接口契约。", author) +
            "public interface " + entityName + "Api {\n\n" +
            "    R<PersistencePageResult<" + entityName + "VO>> page(" + entityName + "PageQuery query);\n\n" +
            "    R<" + entityName + "VO> detail(Long id);\n\n" +
            "    R<Object> create(@Valid Create" + entityName + "Command command);\n\n" +
            "    R<Boolean> update(@Valid Update" + entityName + "Command command);\n\n" +
            "    R<Boolean> delete(DeleteCommand command);\n" +
            "}\n";
        Files.writeString(dir.resolve(entityName + "Api.java"), content);
    }

    private void generateService(Path serviceDir, Path serviceImplDir) throws IOException {
        String entityName = toPascalCase(entity);
        String author = currentAuthor();

        String content =
            "package io.mango." + module + ".core.service;\n\n" +
            "import io.mango.infra.persistence.api.crud.MangoCrudService;\n\n" +
            javaDoc(entityName + " 业务服务。", author) +
            "public interface I" + entityName + "Service extends MangoCrudService {\n" +
            "}\n";
        Files.writeString(serviceDir.resolve("I" + entityName + "Service.java"), content);

        String implContent =
            "package io.mango." + module + ".core.service.impl;\n\n" +
            dataScopeQueryWrapperImport() +
            "import io.mango." + module + ".api.vo." + entityName + "VO;\n" +
            "import io.mango." + module + ".core.entity." + entityName + "Entity;\n" +
            "import io.mango." + module + ".core.mapper." + entityName + "Mapper;\n" +
            "import io.mango." + module + ".core.service.I" + entityName + "Service;\n" +
            dataScopeImports() +
            "import io.mango.infra.persistence.api.crud.MangoCrudServiceImpl;\n" +
            "import org.springframework.stereotype.Service;\n\n" +
            javaDoc(entityName + " 业务服务实现。", author) +
            "@Service\n" +
            "public class " + entityName + "ServiceImpl extends MangoCrudServiceImpl<" + entityName + "Mapper, "
                    + entityName + "Entity>\n" +
            "        implements I" + entityName + "Service {\n\n" +
            dataScopeFieldAndConstructor(entityName) +
            "    @Override\n" +
            "    protected Class<" + entityName + "Entity> entityType() {\n" +
            "        return " + entityName + "Entity.class;\n" +
            "    }\n\n" +
            dataScopeMethod(entityName) +
            "    @Override\n" +
            "    protected Object toVO(" + entityName + "Entity entity) {\n" +
            "        if (entity == null) {\n" +
            "            return null;\n" +
            "        }\n" +
            "        " + entityName + "VO vo = new " + entityName + "VO();\n" +
            "        vo.setId(entity.getId());\n" +
            "        return vo;\n" +
            "    }\n" +
            "}\n";
        Files.writeString(serviceImplDir.resolve(entityName + "ServiceImpl.java"), implContent);
    }

    private String dataScopeImports() {
        if (!hasDataScopeResource()) {
            return "";
        }
        return "import io.mango.infra.persistence.api.scope.DataScopeApplier;\n"
                + "import io.mango.infra.persistence.api.scope.DataScopeMapping;\n";
    }

    private String dataScopeQueryWrapperImport() {
        return hasDataScopeResource() ? "import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;\n" : "";
    }

    private String dataScopeFieldAndConstructor(String entityName) {
        if (!hasDataScopeResource()) {
            return "";
        }
        return "    private final DataScopeApplier dataScopeApplier;\n\n"
                + "    public " + entityName + "ServiceImpl(DataScopeApplier dataScopeApplier) {\n"
                + "        this.dataScopeApplier = dataScopeApplier;\n"
                + "    }\n\n";
    }

    private String dataScopeMethod(String entityName) {
        if (!hasDataScopeResource()) {
            return "";
        }
        String tableName = table == null || table.isBlank() ? toSnakeCase(entity) : table.trim();
        return "    @Override\n"
                + "    protected void applyDataScope(QueryWrapper<" + entityName + "Entity> wrapper, Object query) {\n"
                + "        dataScopeApplier.apply(\n"
                + "                wrapper,\n"
                + "                \"" + dataScopeResource.trim() + "\",\n"
                + "                DataScopeMapping.builder()\n"
                + "                        .tableName(\"" + tableName + "\")\n"
                + "                        .selfField(\"created_by\")\n"
                + "                        .orgField(\"org_id\")\n"
                + "                        .tenantField(\"tenant_id\")\n"
                + "                        .build()\n"
                + "        );\n"
                + "    }\n\n";
    }

    private boolean hasDataScopeResource() {
        return dataScopeResource != null && !dataScopeResource.isBlank();
    }

    private void generateMapper(Path dir) throws IOException {
        String entityName = toPascalCase(entity);
        String author = currentAuthor();

        String content =
            "package io.mango." + module + ".core.mapper;\n\n" +
            "import com.baomidou.mybatisplus.core.mapper.BaseMapper;\n" +
            "import io.mango." + module + ".core.entity." + entityName + "Entity;\n" +
            "import org.apache.ibatis.annotations.Mapper;\n\n" +
            javaDoc(entityName + " 数据访问接口。", author) +
            "@Mapper\n" +
            "public interface " + entityName + "Mapper extends BaseMapper<" + entityName + "Entity> {\n" +
            "}\n";
        Files.writeString(dir.resolve(entityName + "Mapper.java"), content);
    }

    private void generateEntity(Path dir) throws IOException {
        String entityName = toPascalCase(entity);
        String author = currentAuthor();

        String content =
            "package io.mango." + module + ".core.entity;\n\n" +
            "import com.baomidou.mybatisplus.annotation.TableName;\n" +
            "import io.mango.infra.persistence.api.entity.TenantEntity;\n" +
            "import lombok.Getter;\n" +
            "import lombok.Setter;\n\n" +
            javaDoc(entityName + " 持久化实体。", author) +
            "@Getter\n" +
            "@Setter\n" +
            "@TableName(\"" + table + "\")\n" +
            "public class " + entityName + "Entity extends TenantEntity {\n" +
            "}\n";
        Files.writeString(dir.resolve(entityName + "Entity.java"), content);
    }

    private void generateMigration(Path dir) throws IOException {
        String tableName = table == null || table.isBlank() ? toSnakeCase(entity) : table.trim();
        String content =
                "CREATE TABLE IF NOT EXISTS `" + tableName + "` (\n"
                        + "  `id` bigint NOT NULL COMMENT '主键',\n"
                        + "  `created_by` bigint DEFAULT NULL COMMENT '创建人 ID',\n"
                        + "  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',\n"
                        + "  `updated_by` bigint DEFAULT NULL COMMENT '更新人 ID',\n"
                        + "  `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',\n"
                        + "  `tenant_id` varchar(64) NOT NULL DEFAULT 'default' COMMENT '租户标识',\n"
                        + "  `org_id` bigint DEFAULT NULL COMMENT '所属组织 ID',\n"
                        + "  PRIMARY KEY (`id`),\n"
                        + "  KEY `idx_" + tableName + "_tenant_id` (`tenant_id`),\n"
                        + "  KEY `idx_" + tableName + "_org_id` (`org_id`)\n"
                        + ") ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='" + toPascalCase(entity) + "';\n";
        Files.writeString(dir.resolve("V1__init_" + tableName + ".sql"), content);
    }

    private void generatePageQuery(Path dir) throws IOException {
        String entityName = toPascalCase(entity);
        String author = currentAuthor();

        String content =
            "package io.mango." + module + ".api.query;\n\n" +
            "import io.mango.common.po.PageQuery;\n" +
            "import lombok.Data;\n" +
            "import lombok.EqualsAndHashCode;\n\n" +
            javaDoc(entityName + " 分页查询参数。", author) +
            "@Data\n" +
            "@EqualsAndHashCode(callSuper = true)\n" +
            "public class " + entityName + "PageQuery extends PageQuery {\n" +
            "}\n";
        Files.writeString(dir.resolve(entityName + "PageQuery.java"), content);
    }

    private void generateCreateCommand(Path dir) throws IOException {
        String entityName = toPascalCase(entity);
        String author = currentAuthor();

        String content =
            "package io.mango." + module + ".api.command;\n\n" +
            "import lombok.Data;\n\n" +
            javaDoc("创建 " + entityName + " 的命令。", author) +
            "@Data\n" +
            "public class Create" + entityName + "Command {\n" +
            "}\n";
        Files.writeString(dir.resolve("Create" + entityName + "Command.java"), content);
    }

    private void generateUpdateCommand(Path dir) throws IOException {
        String entityName = toPascalCase(entity);
        String author = currentAuthor();

        String content =
            "package io.mango." + module + ".api.command;\n\n" +
            "import lombok.Data;\n\n" +
            javaDoc("更新 " + entityName + " 的命令。", author) +
            "@Data\n" +
            "public class Update" + entityName + "Command {\n\n" +
            "    private Long id;\n\n" +
            "}\n";
        Files.writeString(dir.resolve("Update" + entityName + "Command.java"), content);
    }

    private void generateVO(Path dir) throws IOException {
        String entityName = toPascalCase(entity);
        String author = currentAuthor();

        String content =
            "package io.mango." + module + ".api.vo;\n\n" +
            "import lombok.Data;\n\n" +
            javaDoc(entityName + " 视图对象。", author) +
            "@Data\n" +
            "public class " + entityName + "VO {\n\n" +
            "    private Long id;\n\n" +
            "}\n";
        Files.writeString(dir.resolve(entityName + "VO.java"), content);
    }

    private void generateBizCode(Path dir) throws IOException {
        String entityName = toPascalCase(entity);
        String author = currentAuthor();

        String content =
            "package io.mango." + module + ".api.enums;\n\n" +
            "import io.mango.common.result.BizCode;\n" +
            "import lombok.AllArgsConstructor;\n" +
            "import lombok.Getter;\n\n" +
            javaDoc(entityName + " 业务返回码。", author) +
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
        String author = currentAuthor();

        String content =
            "package io.mango." + module + ".starter.remote;\n\n" +
            "import io.mango." + module + ".api." + entityName + "Api;\n" +
            "import io.mango." + module + ".api.command.Create" + entityName + "Command;\n" +
            "import io.mango." + module + ".api.command.Update" + entityName + "Command;\n" +
            "import io.mango." + module + ".api.query." + entityName + "PageQuery;\n" +
            "import io.mango." + module + ".api.vo." + entityName + "VO;\n" +
            "import io.mango.common.result.R;\n" +
            "import io.mango.infra.persistence.api.crud.DeleteCommand;\n" +
            "import io.mango.infra.persistence.api.query.PersistencePageResult;\n" +
            "import org.springframework.cloud.openfeign.FeignClient;\n" +
            "import org.springframework.cloud.openfeign.SpringQueryMap;\n" +
            "import org.springframework.web.bind.annotation.GetMapping;\n" +
            "import org.springframework.web.bind.annotation.PostMapping;\n" +
            "import org.springframework.web.bind.annotation.RequestBody;\n" +
            "import org.springframework.web.bind.annotation.RequestParam;\n\n" +
            javaDoc(entityName + " 远程调用客户端。", author) +
            "@FeignClient(name = \"" + module + "-service\", path = \"/" + resourcePath + "\")\n" +
            "public interface " + entityName + "FeignClient extends " + entityName + "Api {\n\n" +
            "    @Override\n" +
            "    @GetMapping(\"/page\")\n" +
            "    R<PersistencePageResult<" + entityName + "VO>> page(@SpringQueryMap " + entityName
                    + "PageQuery query);\n\n" +
            "    @Override\n" +
            "    @GetMapping(\"/detail\")\n" +
            "    R<" + entityName + "VO> detail(@RequestParam(\"id\") Long id);\n\n" +
            "    @Override\n" +
            "    @PostMapping(\"/create\")\n" +
            "    R<Object> create(@RequestBody Create" + entityName + "Command command);\n\n" +
            "    @Override\n" +
            "    @PostMapping(\"/update\")\n" +
            "    R<Boolean> update(@RequestBody Update" + entityName + "Command command);\n\n" +
            "    @Override\n" +
            "    @PostMapping(\"/delete\")\n" +
            "    R<Boolean> delete(@RequestBody DeleteCommand command);\n" +
            "}\n";
        Files.writeString(dir.resolve(entityName + "FeignClient.java"), content);
    }

    private void generateRemoteAutoConfiguration(Path dir) throws IOException {
        String entityName = toPascalCase(entity);
        String author = currentAuthor();

        String content =
            "package io.mango." + module + ".starter.remote;\n\n" +
            "import org.springframework.cloud.openfeign.EnableFeignClients;\n" +
            "import org.springframework.context.annotation.Configuration;\n\n" +
            javaDoc(entityName + " 远程调用自动配置。", author) +
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

    private String toSnakeCase(String input) {
        if (input == null || input.isBlank()) {
            return input;
        }
        String normalized = input.trim().replace('-', '_');
        StringBuilder builder = new StringBuilder();
        for (int i = 0; i < normalized.length(); i++) {
            char c = normalized.charAt(i);
            if (Character.isUpperCase(c) && i > 0 && builder.charAt(builder.length() - 1) != '_') {
                builder.append('_');
            }
            builder.append(Character.toLowerCase(c));
        }
        return builder.toString();
    }

    private String javaDoc(String description, String author) {
        return "/**\n"
                + " * " + description + "\n"
                + " *\n"
                + " * @author " + author + "\n"
                + " */\n";
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
