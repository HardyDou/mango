package io.mango.template.core.service;

import io.mango.common.result.R;
import io.mango.common.vo.PageResult;
import io.mango.template.api.command.SaveTemplateCategoryCommand;
import io.mango.template.api.command.UpdateTemplateCategoryStatusCommand;
import io.mango.template.api.query.TemplateCategoryPageQuery;
import io.mango.template.api.vo.TemplateCategoryVO;

import java.util.List;

/**
 * 模板分类服务。
 */
public interface ITemplateCategoryService {

    R<PageResult<TemplateCategoryVO>> page(TemplateCategoryPageQuery query);

    R<List<TemplateCategoryVO>> list(TemplateCategoryPageQuery query);

    R<TemplateCategoryVO> detail(Long id);

    R<Long> create(SaveTemplateCategoryCommand command);

    R<Boolean> update(SaveTemplateCategoryCommand command);

    R<Boolean> updateStatus(UpdateTemplateCategoryStatusCommand command);

    R<Boolean> delete(Long id);
}
