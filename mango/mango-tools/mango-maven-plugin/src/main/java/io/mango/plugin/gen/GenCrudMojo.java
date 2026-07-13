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
 * 生成 CRUD 代码。 技术栈: Mango CRUD 基线 + Entity/Query/Command/VO 分离
 *
 * <p>mvn mango:gen-crud -Dmodule=user -Dentity=User -DentityDisplayName=用户 -Dtable=sys_user
 *
 * @author hardy
 */
@Mojo(name = "gen-crud", defaultPhase = LifecyclePhase.GENERATE_SOURCES)
public class GenCrudMojo extends AbstractMojo {

    private static final String MODULE_PATTERN = "[a-z][a-z0-9]*";
    private static final String ENTITY_PATTERN = "[A-Za-z][A-Za-z0-9_-]*";
    private static final String TABLE_PATTERN = "[a-z][a-z0-9_]*";
    private static final String CONTROLLER_TEMPLATE =
            """
            package io.mango.%1$s.starter.controller;

            import io.mango.%1$s.api.%2$sApi;
            import io.mango.%1$s.api.command.Create%2$sCommand;
            import io.mango.%1$s.api.command.Update%2$sCommand;
            import io.mango.%1$s.api.query.%2$sPageQuery;
            import io.mango.%1$s.api.vo.%2$sVO;
            import io.mango.%1$s.core.service.I%2$sService;
            import io.mango.common.result.R;
            import io.mango.infra.persistence.api.crud.DeleteCommand;
            import io.mango.infra.persistence.api.query.PersistencePageResult;
            import io.swagger.v3.oas.annotations.Operation;
            import io.swagger.v3.oas.annotations.Parameter;
            import io.swagger.v3.oas.annotations.tags.Tag;
            import jakarta.validation.Valid;
            import jakarta.validation.constraints.NotNull;
            import org.springdoc.core.annotations.ParameterObject;
            import org.springframework.validation.annotation.Validated;
            import org.springframework.web.bind.annotation.GetMapping;
            import org.springframework.web.bind.annotation.PostMapping;
            import org.springframework.web.bind.annotation.RequestBody;
            import org.springframework.web.bind.annotation.RequestMapping;
            import org.springframework.web.bind.annotation.RequestParam;
            import org.springframework.web.bind.annotation.RestController;

            /**
             * %2$s 管理接口。
             *
             * @author %5$s
             */
            @RestController
            @Validated
            @Tag(name = "%3$s", description = "%3$s管理接口")
            @RequestMapping("%4$s")
            public class %2$sController implements %2$sApi {

                private final I%2$sService service;

                public %2$sController(I%2$sService service) {
                    this.service = service;
                }

                @Override
                @Operation(summary = "分页查询%3$s", description = "按查询条件分页获取%3$s")
                @GetMapping("/page")
                public R<PersistencePageResult<%2$sVO>> page(
                        @ParameterObject @Valid %2$sPageQuery query) {
                    return R.ok(service.page(query));
                }

                @Override
                @Operation(summary = "查询%3$s详情", description = "按业务标识获取%3$s详情")
                @GetMapping("/detail")
                public R<%2$sVO> detail(
                        @Parameter(description = "业务标识") @RequestParam("id") @NotNull Long id) {
                    return R.ok(service.detail(id));
                }

                @Override
                @Operation(summary = "创建%3$s", description = "创建一条%3$s业务记录")
                @PostMapping("/create")
                public R<Long> create(@RequestBody @Valid Create%2$sCommand command) {
                    return R.ok(service.create(command));
                }

                @Override
                @Operation(summary = "修改%3$s", description = "按业务标识修改%3$s业务记录")
                @PostMapping("/update")
                public R<Boolean> update(@RequestBody @Valid Update%2$sCommand command) {
                    return R.ok(service.update(command));
                }

                @Override
                @Operation(summary = "删除%3$s", description = "按业务标识删除%3$s业务记录")
                @PostMapping("/delete")
                public R<Boolean> delete(@RequestBody @Valid DeleteCommand command) {
                    return R.ok(service.delete(command));
                }
            }
            """;
    private static final String SERVICE_INTERFACE_TEMPLATE =
            """
            package io.mango.%1$s.core.service;

            import io.mango.%1$s.api.command.Create%2$sCommand;
            import io.mango.%1$s.api.command.Update%2$sCommand;
            import io.mango.%1$s.api.query.%2$sPageQuery;
            import io.mango.%1$s.api.vo.%2$sVO;
            import io.mango.%1$s.core.entity.%2$sEntity;
            import io.mango.infra.persistence.api.crud.MangoTypedCrudService;

            /**
             * %2$s 业务服务。
             *
             * @author %3$s
             */
            public interface I%2$sService extends MangoTypedCrudService<
                    %2$sEntity, Create%2$sCommand, Update%2$sCommand, %2$sPageQuery, %2$sVO, Long> {
            }
            """;
    private static final String SERVICE_IMPLEMENTATION_TEMPLATE =
            """
            package io.mango.%1$s.core.service.impl;

            %3$simport io.mango.%1$s.api.command.Create%2$sCommand;
            import io.mango.%1$s.api.command.Update%2$sCommand;
            import io.mango.%1$s.api.enums.%2$sCode;
            import io.mango.%1$s.api.query.%2$sPageQuery;
            import io.mango.%1$s.api.vo.%2$sVO;
            import io.mango.%1$s.core.entity.%2$sEntity;
            import io.mango.%1$s.core.mapper.%2$sMapper;
            import io.mango.%1$s.core.service.I%2$sService;
            %4$simport io.mango.common.result.Require;
            import io.mango.infra.persistence.api.crud.DeleteCommand;
            import io.mango.infra.persistence.api.crud.MangoCrudServiceImpl;
            import io.mango.infra.persistence.api.query.PersistencePageResult;
            import org.springframework.stereotype.Service;
            import org.springframework.transaction.annotation.Transactional;

            /**
             * %2$s 业务服务实现。
             *
             * @author %5$s
             */
            @Service
            public class %2$sService extends MangoCrudServiceImpl<%2$sMapper, %2$sEntity>
                    implements I%2$sService {

            %6$s    @Override
                @Transactional(rollbackFor = Exception.class)
                public Long create(Create%2$sCommand command) {
                    Require.notNull(command, %2$sCode.VALIDATION_ERROR);
                    Object id = createByCommand(command);
                    Require.isTrue(id instanceof Long, %2$sCode.VALIDATION_ERROR);
                    return (Long) id;
                }

                @Override
                @Transactional(rollbackFor = Exception.class)
                public boolean update(Update%2$sCommand command) {
                    Require.notNull(command, %2$sCode.VALIDATION_ERROR);
                    Require.notNull(getById(command.getId()), %2$sCode.NOT_FOUND);
                    return updateByCommand(command);
                }

                @Override
                @Transactional(rollbackFor = Exception.class)
                public boolean delete(DeleteCommand command) {
                    Require.notNull(command, %2$sCode.VALIDATION_ERROR);
                    Require.notNull(command.getId(), %2$sCode.VALIDATION_ERROR);
                    Require.notNull(getById(command.getId()), %2$sCode.NOT_FOUND);
                    return deleteById(command.getId());
                }

                @Override
                @SuppressWarnings("unchecked")
                public PersistencePageResult<%2$sVO> page(%2$sPageQuery query) {
                    Require.notNull(query, %2$sCode.VALIDATION_ERROR);
                    return (PersistencePageResult<%2$sVO>) (PersistencePageResult<?>)
                            pageByQuery(query);
                }

                @Override
                public %2$sVO detail(Long id) {
                    Require.notNull(id, %2$sCode.VALIDATION_ERROR);
                    %2$sEntity entity = getById(id);
                    Require.notNull(entity, %2$sCode.NOT_FOUND);
                    return toVO(entity);
                }

                @Override
                protected Class<%2$sEntity> entityType() {
                    return %2$sEntity.class;
                }

            %7$s    @Override
                protected %2$sVO toVO(%2$sEntity entity) {
                    if (entity == null) {
                        return null;
                    }
                    %2$sVO vo = new %2$sVO();
                    vo.setId(entity.getId());
                    return vo;
                }
            }
            """;
    private static final String FEIGN_CLIENT_TEMPLATE =
            """
            package io.mango.%1$s.starter.remote;

            import io.mango.%1$s.api.%2$sApi;
            import io.mango.%1$s.api.command.Create%2$sCommand;
            import io.mango.%1$s.api.command.Update%2$sCommand;
            import io.mango.%1$s.api.query.%2$sPageQuery;
            import io.mango.%1$s.api.vo.%2$sVO;
            import io.mango.common.result.R;
            import io.mango.infra.persistence.api.crud.DeleteCommand;
            import io.mango.infra.persistence.api.query.PersistencePageResult;
            import jakarta.validation.Valid;
            import jakarta.validation.constraints.NotNull;
            import org.springframework.cloud.openfeign.FeignClient;
            import org.springframework.cloud.openfeign.SpringQueryMap;
            import org.springframework.web.bind.annotation.GetMapping;
            import org.springframework.web.bind.annotation.PostMapping;
            import org.springframework.web.bind.annotation.RequestBody;
            import org.springframework.web.bind.annotation.RequestParam;

            /**
             * %2$s 远程调用客户端。
             *
             * @author %4$s
             */
            @FeignClient(name = "mango-%1$s", contextId = "%5$s", path = "%3$s")
            public interface %2$sFeignClient extends %2$sApi {

                @Override
                @GetMapping("/page")
                R<PersistencePageResult<%2$sVO>> page(
                        @SpringQueryMap @Valid %2$sPageQuery query);

                @Override
                @GetMapping("/detail")
                R<%2$sVO> detail(@RequestParam("id") @NotNull Long id);

                @Override
                @PostMapping("/create")
                R<Long> create(@RequestBody @Valid Create%2$sCommand command);

                @Override
                @PostMapping("/update")
                R<Boolean> update(@RequestBody @Valid Update%2$sCommand command);

                @Override
                @PostMapping("/delete")
                R<Boolean> delete(@RequestBody @Valid DeleteCommand command);
            }
            """;

    @Parameter(property = "module", required = true)
    private String module;

    @Parameter(property = "entity", required = true)
    private String entity;

    @Parameter(property = "entityDisplayName", required = true)
    private String entityDisplayName;

    @Parameter(property = "table", required = true)
    private String table;

    @Parameter(property = "dataScopeResource")
    private String dataScopeResource;

    @Parameter(property = "baseDir", defaultValue = "${project.basedir}")
    private String baseDir;

    @Override
    public void execute() throws MojoExecutionException {
        validateInputs();
        getLog().info(
                        "Generating CRUD for module="
                                + module
                                + ", entity="
                                + entity
                                + ", table="
                                + table);

        try {
            Path moduleRoot = Paths.get(baseDir, "mango-" + module);
            Path apiJavaRoot =
                    moduleRoot.resolve(
                            "mango-" + module + "-api/src/main/java/io/mango/" + module + "/api");
            Path coreJavaRoot =
                    moduleRoot.resolve(
                            "mango-" + module + "-core/src/main/java/io/mango/" + module + "/core");
            Path starterJavaRoot =
                    moduleRoot.resolve(
                            "mango-"
                                    + module
                                    + "-starter/src/main/java/io/mango/"
                                    + module
                                    + "/starter");
            Path remoteJavaRoot =
                    moduleRoot.resolve(
                            "mango-"
                                    + module
                                    + "-starter-remote/src/main/java/io/mango/"
                                    + module
                                    + "/starter/remote");

            generateAt(apiJavaRoot, this::generateApi);
            generateAt(apiJavaRoot.resolve("query"), this::generatePageQuery);
            generateAt(apiJavaRoot.resolve("command"), this::generateCreateCommand);
            generateAt(apiJavaRoot.resolve("command"), this::generateUpdateCommand);
            generateAt(apiJavaRoot.resolve("vo"), this::generateVO);
            generateAt(apiJavaRoot.resolve("enums"), this::generateBizCode);
            generateAt(coreJavaRoot.resolve("entity"), this::generateEntity);
            generateAt(coreJavaRoot.resolve("mapper"), this::generateMapper);
            generateAt(
                    moduleRoot.resolve(
                            "mango-" + module + "-core/src/main/resources/db/migration/" + module),
                    this::generateMigration);

            Path serviceDir = createDirectory(coreJavaRoot.resolve("service"));
            Path serviceImplDir = createDirectory(serviceDir.resolve("impl"));
            generateService(serviceDir, serviceImplDir);
            generateAt(starterJavaRoot.resolve("controller"), this::generateController);
            generateAt(remoteJavaRoot, this::generateFeignClient);
            generateAt(remoteJavaRoot, this::generateRemoteAutoConfiguration);

            getLog().info("CRUD generated successfully to: mango-" + module);
        } catch (IOException e) {
            throw new MojoExecutionException("Failed to generate CRUD", e);
        }
    }

    private void generateAt(Path directory, SourceGenerator generator) throws IOException {
        generator.generate(createDirectory(directory));
    }

    private Path createDirectory(Path directory) throws IOException {
        Files.createDirectories(directory);
        return directory;
    }

    @FunctionalInterface
    private interface SourceGenerator {
        /**
         * Generates one source group in the prepared directory.
         *
         * @param directory prepared output directory
         * @throws IOException when generated source cannot be written
         */
        void generate(Path directory) throws IOException;
    }

    private void generateController(Path dir) throws IOException {
        String entityName = toPascalCase(entity);
        String displayName = entityDisplayName.trim();
        String resourcePath = toCamelCase(entity).toLowerCase();
        String modulePath = module.toLowerCase();
        String controllerPath = resourceEndpointPath(modulePath, resourcePath);
        String author = currentAuthor();
        String content =
                CONTROLLER_TEMPLATE.formatted(
                        module, entityName, displayName, controllerPath, author);
        Files.writeString(dir.resolve(entityName + "Controller.java"), content);
    }

    private void generateApi(Path dir) throws IOException {
        String entityName = toPascalCase(entity);
        String author = currentAuthor();

        String content =
                "package io.mango."
                        + module
                        + ".api;\n\n"
                        + "import io.mango.common.result.R;\n"
                        + "import io.mango.infra.persistence.api.crud.DeleteCommand;\n"
                        + "import io.mango.infra.persistence.api.query.PersistencePageResult;\n"
                        + "import io.mango."
                        + module
                        + ".api.command.Create"
                        + entityName
                        + "Command;\n"
                        + "import io.mango."
                        + module
                        + ".api.command.Update"
                        + entityName
                        + "Command;\n"
                        + "import io.mango."
                        + module
                        + ".api.query."
                        + entityName
                        + "PageQuery;\n"
                        + "import io.mango."
                        + module
                        + ".api.vo."
                        + entityName
                        + "VO;\n"
                        + "import jakarta.validation.Valid;\n\n"
                        + javaDoc(entityName + " 跨模块接口契约。", author)
                        + "public interface "
                        + entityName
                        + "Api {\n\n"
                        + "    R<PersistencePageResult<"
                        + entityName
                        + "VO>> page(@Valid "
                        + entityName
                        + "PageQuery query);\n\n"
                        + "    R<"
                        + entityName
                        + "VO> detail(@jakarta.validation.constraints.NotNull Long id);\n\n"
                        + "    R<Long> create(@Valid Create"
                        + entityName
                        + "Command command);\n\n"
                        + "    R<Boolean> update(@Valid Update"
                        + entityName
                        + "Command command);\n\n"
                        + "    R<Boolean> delete(@Valid DeleteCommand command);\n"
                        + "}\n";
        Files.writeString(dir.resolve(entityName + "Api.java"), content);
    }

    private void generateService(Path serviceDir, Path serviceImplDir) throws IOException {
        String entityName = toPascalCase(entity);
        String author = currentAuthor();
        String content = SERVICE_INTERFACE_TEMPLATE.formatted(module, entityName, author);
        Files.writeString(serviceDir.resolve("I" + entityName + "Service.java"), content);
        String implContent =
                SERVICE_IMPLEMENTATION_TEMPLATE.formatted(
                        module,
                        entityName,
                        dataScopeQueryWrapperImport(),
                        dataScopeImports(),
                        author,
                        dataScopeFieldAndConstructor(entityName),
                        dataScopeMethod(entityName));
        Files.writeString(serviceImplDir.resolve(entityName + "Service.java"), implContent);
    }

    private String dataScopeImports() {
        if (!hasDataScopeResource()) {
            return "";
        }
        return "import io.mango.infra.persistence.api.scope.DataScopeApplier;\n"
                + "import io.mango.infra.persistence.api.scope.DataScopeMapping;\n";
    }

    private String dataScopeQueryWrapperImport() {
        if (hasDataScopeResource()) {
            return "import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;\n";
        }
        return "";
    }

    private String dataScopeFieldAndConstructor(String entityName) {
        if (!hasDataScopeResource()) {
            return "";
        }
        return "    private final DataScopeApplier dataScopeApplier;\n\n"
                + "    public "
                + entityName
                + "Service(DataScopeApplier dataScopeApplier) {\n"
                + "        this.dataScopeApplier = dataScopeApplier;\n"
                + "    }\n\n";
    }

    private String dataScopeMethod(String entityName) {
        if (!hasDataScopeResource()) {
            return "";
        }
        String tableName = effectiveTableName();
        return "    @Override\n"
                + "    protected void applyDataScope(QueryWrapper<"
                + entityName
                + "Entity> wrapper, Object query) {\n"
                + "        dataScopeApplier.apply(\n"
                + "                wrapper,\n"
                + "                \""
                + dataScopeResource.trim()
                + "\",\n"
                + "                DataScopeMapping.builder()\n"
                + "                        .tableName(\""
                + tableName
                + "\")\n"
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
                "package io.mango."
                        + module
                        + ".core.mapper;\n\n"
                        + "import com.baomidou.mybatisplus.core.mapper.BaseMapper;\n"
                        + "import io.mango."
                        + module
                        + ".core.entity."
                        + entityName
                        + "Entity;\n"
                        + "import org.apache.ibatis.annotations.Mapper;\n\n"
                        + javaDoc(entityName + " 数据访问接口。", author)
                        + "@Mapper\n"
                        + "public interface "
                        + entityName
                        + "Mapper extends BaseMapper<"
                        + entityName
                        + "Entity> {\n"
                        + "}\n";
        Files.writeString(dir.resolve(entityName + "Mapper.java"), content);
    }

    private void generateEntity(Path dir) throws IOException {
        String entityName = toPascalCase(entity);
        String author = currentAuthor();

        String content =
                "package io.mango."
                        + module
                        + ".core.entity;\n\n"
                        + "import com.baomidou.mybatisplus.annotation.TableName;\n"
                        + "import io.mango.infra.persistence.api.entity.TenantEntity;\n"
                        + "import lombok.Getter;\n"
                        + "import lombok.Setter;\n\n"
                        + javaDoc(entityName + " 持久化实体。", author)
                        + "@Getter\n"
                        + "@Setter\n"
                        + "@TableName(\""
                        + table
                        + "\")\n"
                        + "public class "
                        + entityName
                        + "Entity extends TenantEntity {\n"
                        + "}\n";
        Files.writeString(dir.resolve(entityName + "Entity.java"), content);
    }

    private void generateMigration(Path dir) throws IOException {
        String tableName = effectiveTableName();
        String content =
                "CREATE TABLE IF NOT EXISTS `"
                        + tableName
                        + "` (\n"
                        + "  `id` bigint NOT NULL COMMENT '主键',\n"
                        + "  `created_by` bigint DEFAULT NULL COMMENT '创建人 ID',\n"
                        + "  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT"
                        + " '创建时间',\n"
                        + "  `updated_by` bigint DEFAULT NULL COMMENT '更新人 ID',\n"
                        + "  `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE"
                        + " CURRENT_TIMESTAMP COMMENT '更新时间',\n"
                        + "  `tenant_id` varchar(64) NOT NULL DEFAULT 'default' COMMENT '租户标识',\n"
                        + "  `org_id` bigint DEFAULT NULL COMMENT '所属组织 ID',\n"
                        + "  PRIMARY KEY (`id`),\n"
                        + "  KEY `idx_"
                        + tableName
                        + "_tenant_id` (`tenant_id`),\n"
                        + "  KEY `idx_"
                        + tableName
                        + "_org_id` (`org_id`)\n"
                        + ") ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci"
                        + " COMMENT='"
                        + toPascalCase(entity)
                        + "';\n";
        Files.writeString(dir.resolve("V1__init_" + tableName + ".sql"), content);
    }

    private String effectiveTableName() {
        if (table == null || table.isBlank()) {
            return toSnakeCase(entity);
        }
        return table.trim();
    }

    private void generatePageQuery(Path dir) throws IOException {
        String entityName = toPascalCase(entity);
        String author = currentAuthor();

        String content =
                "package io.mango."
                        + module
                        + ".api.query;\n\n"
                        + "import io.mango.common.po.PageQuery;\n"
                        + "import io.swagger.v3.oas.annotations.media.Schema;\n"
                        + "import lombok.Data;\n"
                        + "import lombok.EqualsAndHashCode;\n\n"
                        + javaDoc(entityName + " 分页查询参数。", author)
                        + "@Data\n"
                        + "@EqualsAndHashCode(callSuper = true)\n"
                        + "@Schema(description = \""
                        + entityName
                        + " 分页查询参数\")\n"
                        + "public class "
                        + entityName
                        + "PageQuery extends PageQuery {\n"
                        + "}\n";
        Files.writeString(dir.resolve(entityName + "PageQuery.java"), content);
    }

    private void generateCreateCommand(Path dir) throws IOException {
        String entityName = toPascalCase(entity);
        String author = currentAuthor();

        String content =
                "package io.mango."
                        + module
                        + ".api.command;\n\n"
                        + "import io.swagger.v3.oas.annotations.media.Schema;\n"
                        + "import lombok.Data;\n\n"
                        + javaDoc("创建 " + entityName + " 的命令。", author)
                        + "@Data\n"
                        + "@Schema(description = \"创建 "
                        + entityName
                        + " 的命令\")\n"
                        + "public class Create"
                        + entityName
                        + "Command {\n"
                        + "}\n";
        Files.writeString(dir.resolve("Create" + entityName + "Command.java"), content);
    }

    private void generateUpdateCommand(Path dir) throws IOException {
        String entityName = toPascalCase(entity);
        String author = currentAuthor();

        String content =
                "package io.mango."
                        + module
                        + ".api.command;\n\n"
                        + "import io.swagger.v3.oas.annotations.media.Schema;\n"
                        + "import jakarta.validation.constraints.NotNull;\n"
                        + "import lombok.Data;\n\n"
                        + javaDoc("更新 " + entityName + " 的命令。", author)
                        + "@Data\n"
                        + "@Schema(description = \"更新 "
                        + entityName
                        + " 的命令\")\n"
                        + "public class Update"
                        + entityName
                        + "Command {\n\n"
                        + "    @Schema(description = \"业务标识\")\n"
                        + "    @NotNull(message = \"业务标识不能为空\")\n"
                        + "    private Long id;\n\n"
                        + "}\n";
        Files.writeString(dir.resolve("Update" + entityName + "Command.java"), content);
    }

    private void generateVO(Path dir) throws IOException {
        String entityName = toPascalCase(entity);
        String author = currentAuthor();

        String content =
                "package io.mango."
                        + module
                        + ".api.vo;\n\n"
                        + "import io.swagger.v3.oas.annotations.media.Schema;\n"
                        + "import lombok.Data;\n\n"
                        + javaDoc(entityName + " 视图对象。", author)
                        + "@Data\n"
                        + "@Schema(description = \""
                        + entityName
                        + " 视图对象\")\n"
                        + "public class "
                        + entityName
                        + "VO {\n\n"
                        + "    @Schema(description = \"业务标识\")\n"
                        + "    private Long id;\n\n"
                        + "}\n";
        Files.writeString(dir.resolve(entityName + "VO.java"), content);
    }

    private void generateBizCode(Path dir) throws IOException {
        String entityName = toPascalCase(entity);
        String author = currentAuthor();

        String content =
                "package io.mango."
                        + module
                        + ".api.enums;\n\n"
                        + "import io.mango.common.result.BizCode;\n\n"
                        + javaDoc(entityName + " 业务返回码。", author)
                        + "public enum "
                        + entityName
                        + "Code implements BizCode {\n\n"
                        + "    SUCCESS(200, \"操作成功\"),\n"
                        + "    NOT_FOUND(404, \"资源不存在\"),\n"
                        + "    VALIDATION_ERROR(400, \"参数校验失败\");\n\n"
                        + "    private final int code;\n"
                        + "    private final String message;\n\n"
                        + "    "
                        + entityName
                        + "Code(int code, String message) {\n"
                        + "        this.code = code;\n"
                        + "        this.message = message;\n"
                        + "    }\n\n"
                        + "    @Override\n"
                        + "    public int getCode() {\n"
                        + "        return code;\n"
                        + "    }\n\n"
                        + "    @Override\n"
                        + "    public String getMessage() {\n"
                        + "        return message;\n"
                        + "    }\n"
                        + "}\n";
        Files.writeString(dir.resolve(entityName + "Code.java"), content);
    }

    private void generateFeignClient(Path dir) throws IOException {
        String entityName = toPascalCase(entity);
        String resourcePath = toCamelCase(entity).toLowerCase();
        String modulePath = module.toLowerCase();
        String remotePath = resourceEndpointPath(modulePath, resourcePath);
        String author = currentAuthor();
        String contextId = lowerCamelCase(entityName + "FeignClient");
        String content =
                FEIGN_CLIENT_TEMPLATE.formatted(module, entityName, remotePath, author, contextId);
        Files.writeString(dir.resolve(entityName + "FeignClient.java"), content);
    }

    private String lowerCamelCase(String value) {
        if (value == null || value.isBlank()) {
            return "";
        }
        return Character.toLowerCase(value.charAt(0)) + value.substring(1);
    }

    private void generateRemoteAutoConfiguration(Path dir) throws IOException {
        String entityName = toPascalCase(entity);
        String author = currentAuthor();

        String content =
                "package io.mango."
                        + module
                        + ".starter.remote;\n\n"
                        + "import org.springframework.cloud.openfeign.EnableFeignClients;\n"
                        + "import org.springframework.context.annotation.Configuration;\n\n"
                        + javaDoc(entityName + " 远程调用自动配置。", author)
                        + "@Configuration\n"
                        + "@EnableFeignClients(basePackageClasses = "
                        + entityName
                        + "FeignClient.class)\n"
                        + "public class "
                        + entityName
                        + "RemoteAutoConfiguration {\n"
                        + "}\n";
        Files.writeString(dir.resolve(entityName + "RemoteAutoConfiguration.java"), content);
    }

    private void validateInputs() throws MojoExecutionException {
        if (module == null || !module.matches(MODULE_PATTERN)) {
            throw new MojoExecutionException(
                    "module must be a lowercase Java package segment, for example order");
        }
        if (entity == null || !entity.matches(ENTITY_PATTERN)) {
            throw new MojoExecutionException(
                    "entity must be a Java aggregate name using letters, digits, '_' or '-'");
        }
        if (table == null || !table.matches(TABLE_PATTERN)) {
            throw new MojoExecutionException("table must be a lowercase snake_case identifier");
        }
        if (entityDisplayName == null
                || entityDisplayName.isBlank()
                || entityDisplayName
                        .codePoints()
                        .noneMatch(
                                codePoint ->
                                        Character.UnicodeScript.of(codePoint)
                                                == Character.UnicodeScript.HAN)) {
            throw new MojoExecutionException(
                    "entityDisplayName must be a non-blank Chinese business name");
        }
    }

    private String toPascalCase(String input) {
        if (input == null || input.isEmpty()) {
            return input;
        }
        String[] parts = input.trim().replace("-", "_").split("_");
        StringBuilder sb = new StringBuilder();
        for (String part : parts) {
            if (part.length() > 0) {
                sb.append(Character.toUpperCase(part.charAt(0))).append(part.substring(1));
            }
        }
        return sb.toString();
    }

    private String resourceEndpointPath(String modulePath, String resourcePath) {
        if (modulePath.equals(resourcePath)) {
            return "/" + modulePath;
        }
        return "/" + modulePath + "/" + resourcePath;
    }

    private String toCamelCase(String input) {
        if (input == null || input.isEmpty()) {
            return input;
        }
        String pascal = toPascalCase(input);
        return Character.toLowerCase(pascal.charAt(0)) + pascal.substring(1);
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
                + " * "
                + description
                + "\n"
                + " *\n"
                + " * @author "
                + author
                + "\n"
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
        if (userName == null || userName.isBlank()) {
            return "unknown";
        }
        return userName.trim();
    }
}
