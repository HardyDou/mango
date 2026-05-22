package io.mango.template.api;

import io.mango.common.result.R;
import io.mango.common.vo.PageResult;
import io.mango.template.api.command.*;
import io.mango.template.api.query.TemplatePageQuery;
import io.mango.template.api.query.TemplateRenderRecordPageQuery;
import io.mango.template.api.vo.TemplateDetailVO;
import io.mango.template.api.vo.TemplateRenderRecordVO;
import io.mango.template.api.vo.TemplateRenderResultVO;
import io.mango.template.api.vo.TemplateVO;
import jakarta.validation.Valid;

import java.util.List;

/**
 * 模板服务 API 契约。
 */
public interface TemplateApi {

    /** 分页查询模板。 */
    R<PageResult<TemplateVO>> page(TemplatePageQuery query);

    /** 查询模板详情。 */
    R<TemplateDetailVO> detail(Long id);

    /** 新增模板。 */
    R<Long> create(@Valid SaveTemplateCommand command);

    /** 修改模板。 */
    R<Boolean> update(@Valid SaveTemplateCommand command);

    /** 删除模板。 */
    R<Boolean> delete(Long id);

    /** 启停模板。 */
    R<Boolean> updateStatus(@Valid UpdateTemplateStatusCommand command);

    /** 发布模板版本。 */
    R<Long> publishVersion(@Valid PublishTemplateVersionCommand command);

    /** 启用模板历史版本。 */
    R<Boolean> activateVersion(@Valid ActivateTemplateVersionCommand command);

    /** 从内容中提取变量建议。 */
    R<List<String>> extractVariables(@Valid ExtractTemplateVariablesCommand command);

    /** 同步渲染模板。 */
    R<TemplateRenderResultVO> render(@Valid TemplateRenderCommand command);

    /** 提交异步渲染任务。 */
    R<TemplateRenderResultVO> renderAsync(@Valid TemplateRenderCommand command);

    /** 查询渲染记录。 */
    R<TemplateRenderRecordVO> renderRecord(Long id);

    /** 分页查询渲染记录。 */
    R<PageResult<TemplateRenderRecordVO>> renderRecordPage(TemplateRenderRecordPageQuery query);
}
