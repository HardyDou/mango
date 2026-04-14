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
 * 生成 CRUD 代码
 * 技术栈: MyBatis-Plus + PO/VO 分离
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
            Path poDir = Paths.get(baseDir, "mango-" + module + "/mango-" + module + "-core/src/main/java/io/mango/" + module + "/po");
            Files.createDirectories(poDir);
            generatePO(poDir);

            Path voDir = Paths.get(baseDir, "mango-" + module + "/mango-" + module + "-core/src/main/java/io/mango/" + module + "/vo");
            Files.createDirectories(voDir);
            generateVO(voDir);

            Path mapperDir = Paths.get(baseDir, "mango-" + module + "/mango-" + module + "-core/src/main/java/io/mango/" + module + "/mapper");
            Files.createDirectories(mapperDir);
            generateMapper(mapperDir);

            Path serviceDir = Paths.get(baseDir, "mango-" + module + "/mango-" + module + "-core/src/main/java/io/mango/" + module + "/service");
            Files.createDirectories(serviceDir);
            Path serviceImplDir = Paths.get(baseDir, "mango-" + module + "/mango-" + module + "-core/src/main/java/io/mango/" + module + "/service/impl");
            Files.createDirectories(serviceImplDir);
            generateService(serviceDir, serviceImplDir);

            Path controllerDir = Paths.get(baseDir, "mango-" + module + "/mango-" + module + "-starter/src/main/java/io/mango/" + module + "/controller");
            Files.createDirectories(controllerDir);
            generateController(controllerDir);

            Path bizCodeDir = Paths.get(baseDir, "mango-" + module + "/mango-" + module + "-api/src/main/java/io/mango/" + module + "/enums");
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
        String moduleName = module.replace("mango-", "");
        String resourcePath = moduleName + "/" + camelEntity.toLowerCase();

        String content = pkg(
            "package io.mango.%s.controller;\n\n" +
            "import io.mango.common.result.R;\n" +
            "import io.mango.%s.service.I%sService;\n" +
            "import io.mango.%s.po.%sPO;\n" +
            "import io.mango.%s.vo.%sVO;\n" +
            "import lombok.RequiredArgsConstructor;\n" +
            "import org.springframework.web.bind.annotation.*;\n\n" +
            "@RestController\n" +
            "@RequestMapping(\"/%s\")\n" +
            "@RequiredArgsConstructor\n" +
            "public class %sController {\n\n" +
            "    private final I%sService<%sVO> %sService;\n\n" +
            "    @GetMapping(\"/page\")\n" +
            "    public R<%sVO> page(%sPO po) {\n" +
            "        return R.ok(%sService.page(po));\n" +
            "    }\n\n" +
            "    @GetMapping(\"/{id}\")\n" +
            "    public R<%sVO> get(@PathVariable Long id) {\n" +
            "        return R.ok(%sService.getById(id));\n" +
            "    }\n\n" +
            "    @PostMapping\n" +
            "    public R<Void> save(@RequestBody %sPO po) {\n" +
            "        %sService.save(po);\n" +
            "        return R.ok();\n" +
            "    }\n\n" +
            "    @PutMapping(\"/{id}\")\n" +
            "    public R<Void> update(@PathVariable Long id, @RequestBody %sPO po) {\n" +
            "        po.setId(id);\n" +
            "        %sService.update(po);\n" +
            "        return R.ok();\n" +
            "    }\n\n" +
            "    @DeleteMapping(\"/{id}\")\n" +
            "    public R<Void> delete(@PathVariable Long id) {\n" +
            "        %sService.delete(id);\n" +
            "        return R.ok();\n" +
            "    }\n" +
            "}\n",
            module,
            module, entityName,
            module, entityName,
            module, entityName,
            resourcePath,
            entityName,
            entityName, entityName,
            entityName,
            entityName,
            entityName,
            entityName,
            entityName,
            entityName,
            entityName,
            entityName,
            entityName,
            entityName,
            entityName,
            entityName
        );
        Files.writeString(dir.resolve(entityName + "Controller.java"), content);
    }

    private void generateService(Path serviceDir, Path serviceImplDir) throws IOException {
        String entityName = toPascalCase(entity);
        String camelEntity = toCamelCase(entity);

        String content = pkg(
            "package io.mango.%s.service;\n\n" +
            "import io.mango.%s.po.%sPO;\n" +
            "import io.mango.%s.vo.%sVO;\n" +
            "import io.mango.common.result.PageResult;\n\n" +
            "public interface I%sService<T> {\n\n" +
            "    %sVO getById(Long id);\n\n" +
            "    PageResult<%sVO> page(%sPO po);\n\n" +
            "    void save(%sPO po);\n\n" +
            "    void update(%sPO po);\n\n" +
            "    void delete(Long id);\n" +
            "}\n",
            module,
            module, entityName,
            module, entityName,
            entityName,
            entityName,
            entityName,
            entityName,
            entityName,
            entityName,
            entityName
        );
        Files.writeString(serviceDir.resolve("I" + entityName + "Service.java"), content);

        String implContent = pkg(
            "package io.mango.%s.service.impl;\n\n" +
            "import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;\n" +
            "import com.baomidou.mybatisplus.core.mapper.BaseMapper;\n" +
            "import com.baomidou.mybatisplus.core.metadata.IPage;\n" +
            "import com.baomidou.mybatisplus.extension.plugins.pagination.Page;\n" +
            "import io.mango.%s.mapper.%sMapper;\n" +
            "import io.mango.%s.po.%sPO;\n" +
            "import io.mango.%s.service.I%sService;\n" +
            "import io.mango.%s.vo.%sVO;\n" +
            "import io.mango.common.result.PageResult;\n" +
            "import io.mango.common.result.Require;\n" +
            "import lombok.RequiredArgsConstructor;\n" +
            "import org.springframework.stereotype.Service;\n\n" +
            "@Service\n" +
            "@RequiredArgsConstructor\n" +
            "public class %sServiceImpl implements I%sService<%sVO> {\n\n" +
            "    private final %sMapper %sMapper;\n\n" +
            "    @Override\n" +
            "    public %sVO getById(Long id) {\n" +
            "        %sPO po = %sMapper.selectById(id);\n" +
            "        Require.notNull(po, 404, \"记录不存在\");\n" +
            "        return toVO(po);\n" +
            "    }\n\n" +
            "    @Override\n" +
            "    public PageResult<%sVO> page(%sPO po) {\n" +
            "        LambdaQueryWrapper<%sPO> wrapper = new LambdaQueryWrapper<>();\n" +
            "        IPage<%sPO> page = %sMapper.selectPage(new Page<>(po.getPage(), po.getSize()), wrapper);\n" +
            "        return PageResult.of(page.getRecords().stream().map(this::toVO).toList(),\n" +
            "                page.getTotal(), page.getCurrent(), page.getSize());\n" +
            "    }\n\n" +
            "    @Override\n" +
            "    public void save(%sPO po) {\n" +
            "        %sMapper.insert(po);\n" +
            "    }\n\n" +
            "    @Override\n" +
            "    public void update(%sPO po) {\n" +
            "        Require.notNull(po.getId(), 400, \"ID 不能为空\");\n" +
            "        %sMapper.updateById(po);\n" +
            "    }\n\n" +
            "    @Override\n" +
            "    public void delete(Long id) {\n" +
            "        %sMapper.deleteById(id);\n" +
            "    }\n\n" +
            "    private %sVO toVO(%sPO po) {\n" +
            "        if (po == null) return null;\n" +
            "        %sVO vo = new %sVO();\n" +
            "        return vo;\n" +
            "    }\n" +
            "}\n",
            module,
            module, entityName,
            module, entityName,
            module, entityName,
            module, entityName,
            entityName,
            camelEntity,
            entityName, entityName,
            entityName,
            entityName,
            entityName,
            entityName,
            entityName,
            entityName,
            entityName,
            entityName,
            entityName,
            entityName,
            entityName,
            entityName,
            entityName,
            entityName,
            entityName,
            entityName,
            entityName,
            entityName
        );
        Files.writeString(serviceImplDir.resolve(entityName + "ServiceImpl.java"), implContent);
    }

    private void generateMapper(Path dir) throws IOException {
        String entityName = toPascalCase(entity);

        String content = pkg(
            "package io.mango.%s.mapper;\n\n" +
            "import com.baomidou.mybatisplus.core.mapper.BaseMapper;\n" +
            "import io.mango.%s.po.%sPO;\n" +
            "import org.apache.ibatis.annotations.Mapper;\n\n" +
            "@Mapper\n" +
            "public interface %sMapper extends BaseMapper<%sPO> {\n" +
            "}\n",
            module,
            module, entityName,
            entityName,
            entityName,
            entityName
        );
        Files.writeString(dir.resolve(entityName + "Mapper.java"), content);
    }

    private void generatePO(Path dir) throws IOException {
        String entityName = toPascalCase(entity);

        String content = pkg(
            "package io.mango.%s.po;\n\n" +
            "import com.baomidou.mybatisplus.annotation.IdType;\n" +
            "import com.baomidou.mybatisplus.annotation.TableId;\n" +
            "import com.baomidou.mybatisplus.annotation.TableName;\n" +
            "import lombok.Data;\n\n" +
            "@Data\n" +
            "@TableName(\"%s\")\n" +
            "public class %sPO {\n\n" +
            "    @TableId(type = IdType.AUTO)\n" +
            "    private Long id;\n\n" +
            "}\n",
            module,
            table,
            entityName
        );
        Files.writeString(dir.resolve(entityName + "PO.java"), content);
    }

    private void generateVO(Path dir) throws IOException {
        String entityName = toPascalCase(entity);

        String content = pkg(
            "package io.mango.%s.vo;\n\n" +
            "import lombok.Data;\n\n" +
            "@Data\n" +
            "public class %sVO {\n\n" +
            "    private Long id;\n\n" +
            "}\n",
            module,
            entityName,
            entityName
        );
        Files.writeString(dir.resolve(entityName + "VO.java"), content);
    }

    private void generateBizCode(Path dir) throws IOException {
        String entityName = toPascalCase(entity);

        String content = pkg(
            "package io.mango.%s.enums;\n\n" +
            "import io.mango.common.result.BizCode;\n" +
            "import lombok.AllArgsConstructor;\n" +
            "import lombok.Getter;\n\n" +
            "@Getter\n" +
            "@AllArgsConstructor\n" +
            "public enum %sCode implements BizCode {\n\n" +
            "    SUCCESS(200, \"操作成功\"),\n" +
            "    NOT_FOUND(404, \"资源不存在\"),\n" +
            "    VALIDATION_ERROR(400, \"参数校验失败\");\n\n" +
            "    private final int code;\n" +
            "    private final String message;\n" +
            "}\n",
            module,
            entityName,
            entityName
        );
        Files.writeString(dir.resolve(entityName + "Code.java"), content);
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
