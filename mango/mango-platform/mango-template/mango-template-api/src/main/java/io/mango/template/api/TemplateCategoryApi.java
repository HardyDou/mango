package io.mango.template.api;

import io.mango.common.result.R;
import io.mango.common.vo.PageResult;
import io.mango.template.api.command.SaveTemplateCategoryCommand;
import io.mango.template.api.command.UpdateTemplateCategoryStatusCommand;
import io.mango.template.api.query.TemplateCategoryPageQuery;
import io.mango.template.api.vo.TemplateCategoryVO;
import jakarta.validation.Valid;

import java.util.List;

/**
 * 模板分类 API 契约。
 */
public interface TemplateCategoryApi {

    /** 分页查询模板分类。 */
    R<PageResult<TemplateCategoryVO>> page(TemplateCategoryPageQuery query);

    /** 查询启用模板分类列表。 */
    R<List<TemplateCategoryVO>> list(TemplateCategoryPageQuery query);

    /** 查询模板分类详情。 */
    R<TemplateCategoryVO> detail(Long id);

    /** 新增模板分类。 */
    R<Long> create(@Valid SaveTemplateCategoryCommand command);

    /** 修改模板分类。 */
    R<Boolean> update(@Valid SaveTemplateCategoryCommand command);

    /** 启停模板分类。 */
    R<Boolean> updateStatus(@Valid UpdateTemplateCategoryStatusCommand command);

    /** 删除模板分类。 */
    R<Boolean> delete(Long id);
}
