package io.mango.template.core.service;

import io.mango.common.result.R;
import io.mango.common.vo.PageResult;
import io.mango.template.api.command.*;
import io.mango.template.api.query.TemplatePageQuery;
import io.mango.template.api.query.TemplateRenderRecordPageQuery;
import io.mango.template.api.vo.TemplateDetailVO;
import io.mango.template.api.vo.TemplateRenderRecordVO;
import io.mango.template.api.vo.TemplateRenderResultVO;
import io.mango.template.api.vo.TemplateVO;

import java.util.List;

/**
 * 模板服务。
 */
public interface ITemplateService {

    /**
     * 分页查询模板。
     *
     * @param query 查询条件。
     * @return 模板分页结果。
     */
    R<PageResult<TemplateVO>> page(TemplatePageQuery query);

    /**
     * 查询模板详情。
     *
     * @param id 模板ID。
     * @return 模板详情。
     */
    R<TemplateDetailVO> detail(Long id);

    /**
     * 创建模板。
     *
     * @param command 保存模板命令。
     * @return 模板ID。
     */
    R<Long> create(SaveTemplateCommand command);

    /**
     * 更新模板。
     *
     * @param command 保存模板命令。
     * @return 是否成功。
     */
    R<Boolean> update(SaveTemplateCommand command);

    /**
     * 删除模板。
     *
     * @param id 模板ID。
     * @return 是否成功。
     */
    R<Boolean> delete(Long id);

    /**
     * 更新模板状态。
     *
     * @param command 更新状态命令。
     * @return 是否成功。
     */
    R<Boolean> updateStatus(UpdateTemplateStatusCommand command);

    /**
     * 发布模板版本。
     *
     * @param command 发布版本命令。
     * @return 模板版本ID。
     */
    R<Long> publishVersion(PublishTemplateVersionCommand command);

    /**
     * 启用模板历史版本。
     *
     * @param command 启用版本命令。
     * @return 是否成功。
     */
    R<Boolean> activateVersion(ActivateTemplateVersionCommand command);

    /**
     * 提取模板变量。
     *
     * @param command 提取变量命令。
     * @return 变量名列表。
     */
    R<List<String>> extractVariables(ExtractTemplateVariablesCommand command);

    /**
     * 同步渲染模板。
     *
     * @param command 渲染命令。
     * @return 渲染结果。
     */
    R<TemplateRenderResultVO> render(TemplateRenderCommand command);

    /**
     * 异步渲染模板。
     *
     * @param command 渲染命令。
     * @return 渲染任务结果。
     */
    R<TemplateRenderResultVO> renderAsync(TemplateRenderCommand command);

    /**
     * 查询渲染记录。
     *
     * @param id 渲染记录ID。
     * @return 渲染记录。
     */
    R<TemplateRenderRecordVO> renderRecord(Long id);

    /**
     * 分页查询渲染记录。
     *
     * @param query 查询条件。
     * @return 渲染记录分页结果。
     */
    R<PageResult<TemplateRenderRecordVO>> renderRecordPage(TemplateRenderRecordPageQuery query);
}
