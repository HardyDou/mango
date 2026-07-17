package io.mango.template.api;

import io.mango.common.result.R;
import io.mango.common.vo.PageResult;
import io.mango.template.api.command.CreateTemplateCategoryCommand;
import io.mango.template.api.command.UpdateTemplateCategoryCommand;
import io.mango.template.api.command.UpdateTemplateCategoryStatusCommand;
import io.mango.template.api.query.TemplateCategoryPageQuery;
import io.mango.template.api.vo.TemplateCategoryVO;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;

import java.util.List;

/**
 * 模板分类 API 契约。
 */
public interface TemplateCategoryApi {

    /** 分页查询模板分类。 */
    R<PageResult<TemplateCategoryVO>> page(@Valid TemplateCategoryPageQuery query);

    /** 查询启用模板分类列表。 */
    R<List<TemplateCategoryVO>> list(@Valid TemplateCategoryPageQuery query);

    /** 查询模板分类详情。 */
    R<TemplateCategoryVO> detail(@NotNull Long id);

    /** 新增模板分类。 */
    R<Long> create(@Valid CreateTemplateCategoryCommand command);

    /** 修改模板分类。 */
    R<Boolean> update(@Valid UpdateTemplateCategoryCommand command);

    /** 启停模板分类。 */
    R<Boolean> updateStatus(@Valid UpdateTemplateCategoryStatusCommand command);

    /** 删除模板分类。 */
    R<Boolean> delete(@NotNull Long id);
}
