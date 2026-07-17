package io.mango.template.core.service;

import io.mango.common.vo.PageResult;
import io.mango.infra.persistence.api.crud.MangoTypedCrudService;
import io.mango.template.api.command.CreateTemplateCategoryCommand;
import io.mango.template.api.command.UpdateTemplateCategoryCommand;
import io.mango.template.api.command.UpdateTemplateCategoryStatusCommand;
import io.mango.template.api.query.TemplateCategoryPageQuery;
import io.mango.template.api.vo.TemplateCategoryVO;
import io.mango.template.core.entity.TemplateCategoryEntity;

import java.util.List;

/**
 * 模板分类服务。
 */
public interface ITemplateCategoryService extends MangoTypedCrudService<TemplateCategoryEntity,
        CreateTemplateCategoryCommand, UpdateTemplateCategoryCommand, TemplateCategoryPageQuery, TemplateCategoryVO, Long> {

    PageResult<TemplateCategoryVO> pageResult(TemplateCategoryPageQuery query);

    List<TemplateCategoryVO> list(TemplateCategoryPageQuery query);

    boolean updateStatus(UpdateTemplateCategoryStatusCommand command);

    boolean delete(Long id);
}
