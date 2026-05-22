package io.mango.template.starter.remote;

import io.mango.common.result.R;
import io.mango.common.vo.PageResult;
import io.mango.template.api.TemplateApi;
import io.mango.template.api.command.*;
import io.mango.template.api.query.TemplatePageQuery;
import io.mango.template.api.query.TemplateRenderRecordPageQuery;
import io.mango.template.api.vo.TemplateDetailVO;
import io.mango.template.api.vo.TemplateRenderRecordVO;
import io.mango.template.api.vo.TemplateRenderResultVO;
import io.mango.template.api.vo.TemplateVO;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 模板服务 Feign 适配器。
 */
@FeignClient(name = "mango-template", path = "/template/templates")
public interface TemplateFeignClient extends TemplateApi {

    /**
     * 分页查询模板。
     *
     * @param query 查询条件。
     * @return 模板分页结果。
     */
    @Override
    @GetMapping("/page")
    R<PageResult<TemplateVO>> page(TemplatePageQuery query);

    /**
     * 查询模板详情。
     *
     * @param id 模板ID。
     * @return 模板详情。
     */
    @Override
    @GetMapping("/detail")
    R<TemplateDetailVO> detail(@RequestParam("id") Long id);

    /**
     * 创建模板。
     *
     * @param command 保存模板命令。
     * @return 模板ID。
     */
    @Override
    @PostMapping
    R<Long> create(@RequestBody SaveTemplateCommand command);

    /**
     * 更新模板。
     *
     * @param command 保存模板命令。
     * @return 是否成功。
     */
    @Override
    @PutMapping
    R<Boolean> update(@RequestBody SaveTemplateCommand command);

    /**
     * 删除模板。
     *
     * @param id 模板ID。
     * @return 是否成功。
     */
    @Override
    @DeleteMapping
    R<Boolean> delete(@RequestParam("id") Long id);

    /**
     * 更新模板状态。
     *
     * @param command 更新状态命令。
     * @return 是否成功。
     */
    @Override
    @PutMapping("/status")
    R<Boolean> updateStatus(@RequestBody UpdateTemplateStatusCommand command);

    /**
     * 发布模板版本。
     *
     * @param command 发布版本命令。
     * @return 模板版本ID。
     */
    @Override
    @PostMapping("/versions")
    R<Long> publishVersion(@RequestBody PublishTemplateVersionCommand command);

    /**
     * 启用模板历史版本。
     *
     * @param command 启用版本命令。
     * @return 是否成功。
     */
    @Override
    @PutMapping("/versions/current")
    R<Boolean> activateVersion(@RequestBody ActivateTemplateVersionCommand command);

    /**
     * 提取模板变量。
     *
     * @param command 提取变量命令。
     * @return 变量名列表。
     */
    @Override
    @PostMapping("/variables/extract")
    R<List<String>> extractVariables(@RequestBody ExtractTemplateVariablesCommand command);

    /**
     * 同步渲染模板。
     *
     * @param command 渲染命令。
     * @return 渲染结果。
     */
    @Override
    @PostMapping("/render")
    R<TemplateRenderResultVO> render(@RequestBody TemplateRenderCommand command);

    /**
     * 异步渲染模板。
     *
     * @param command 渲染命令。
     * @return 渲染任务结果。
     */
    @Override
    @PostMapping("/render/async")
    R<TemplateRenderResultVO> renderAsync(@RequestBody TemplateRenderCommand command);

    /**
     * 查询渲染记录。
     *
     * @param id 渲染记录ID。
     * @return 渲染记录。
     */
    @Override
    @GetMapping("/render-records/detail")
    R<TemplateRenderRecordVO> renderRecord(@RequestParam("id") Long id);

    /**
     * 分页查询渲染记录。
     *
     * @param query 查询条件。
     * @return 渲染记录分页结果。
     */
    @Override
    @GetMapping("/render-records/page")
    R<PageResult<TemplateRenderRecordVO>> renderRecordPage(TemplateRenderRecordPageQuery query);
}
