package io.mango.template.core.service;

import io.mango.common.vo.PageResult;
import io.mango.infra.persistence.api.crud.MangoTypedCrudService;
import io.mango.template.api.command.ActivateTemplateVersionCommand;
import io.mango.template.api.command.ExtractTemplateVariablesCommand;
import io.mango.template.api.command.PublishTemplateVersionCommand;
import io.mango.template.api.command.CreateTemplateCommand;
import io.mango.template.api.command.UpdateTemplateCommand;
import io.mango.template.api.command.TemplateRenderCommand;
import io.mango.template.api.command.UpdateTemplateStatusCommand;
import io.mango.template.api.query.TemplatePageQuery;
import io.mango.template.api.query.TemplateRenderRecordPageQuery;
import io.mango.template.api.vo.TemplateDetailVO;
import io.mango.template.api.vo.TemplateRenderRecordVO;
import io.mango.template.api.vo.TemplateRenderResultVO;
import io.mango.template.api.vo.TemplateVO;
import io.mango.template.core.entity.TemplateEntity;

import java.util.List;

/**
 * 模板服务。
 */
public interface ITemplateService extends MangoTypedCrudService<TemplateEntity,
        CreateTemplateCommand, UpdateTemplateCommand, TemplatePageQuery, TemplateVO, Long> {

    PageResult<TemplateVO> pageResult(TemplatePageQuery query);

    TemplateDetailVO detail(Long id);

    boolean delete(Long id);

    boolean updateStatus(UpdateTemplateStatusCommand command);

    Long publishVersion(PublishTemplateVersionCommand command);

    boolean activateVersion(ActivateTemplateVersionCommand command);

    List<String> extractVariables(ExtractTemplateVariablesCommand command);

    TemplateRenderResultVO render(TemplateRenderCommand command);

    TemplateRenderResultVO renderAsync(TemplateRenderCommand command);

    TemplateRenderRecordVO renderRecord(Long id);

    PageResult<TemplateRenderRecordVO> renderRecordPage(TemplateRenderRecordPageQuery query);
}
