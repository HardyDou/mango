package io.mango.cms.core.service;

import io.mango.cms.api.command.SaveCmsContentCategoryCommand;
import io.mango.cms.api.command.UpdateCmsStatusCommand;
import io.mango.cms.api.query.CmsContentCategoryPageQuery;
import io.mango.cms.api.vo.CmsContentCategoryVO;
import io.mango.common.vo.PageResult;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import java.util.List;

/** CMS ContentCategory aggregate contract. */
public interface ICmsContentCategoryService {

    /**
     * Executes the CMS pageContentCategories domain operation.
     *
     * @param query operation input
     * @return PageResult<CmsContentCategoryVO> operation result
     */
    PageResult<CmsContentCategoryVO> pageContentCategories(@Valid CmsContentCategoryPageQuery query);

    /**
     * Executes the CMS listContentCategories domain operation.
     *
     * @param query operation input
     * @return List<CmsContentCategoryVO> operation result
     */
    List<CmsContentCategoryVO> listContentCategories(@Valid CmsContentCategoryPageQuery query);

    /**
     * Executes the CMS treeContentCategories domain operation.
     *
     * @param query operation input
     * @return List<CmsContentCategoryVO> operation result
     */
    List<CmsContentCategoryVO> treeContentCategories(@Valid CmsContentCategoryPageQuery query);

    /**
     * Executes the CMS detailContentCategory domain operation.
     *
     * @param id operation input
     * @return CmsContentCategoryVO operation result
     */
    CmsContentCategoryVO detailContentCategory(@NotNull(message = "分类 ID 不能为空") Long id);

    /**
     * Executes the CMS createContentCategory domain operation.
     *
     * @param command operation input
     * @return Long operation result
     */
    Long createContentCategory(@Valid SaveCmsContentCategoryCommand command);

    /**
     * Executes the CMS updateContentCategory domain operation.
     *
     * @param command operation input
     * @return Boolean operation result
     */
    Boolean updateContentCategory(@Valid SaveCmsContentCategoryCommand command);

    /**
     * Executes the CMS updateContentCategoryStatus domain operation.
     *
     * @param command operation input
     * @return Boolean operation result
     */
    Boolean updateContentCategoryStatus(@Valid UpdateCmsStatusCommand command);

    /**
     * Executes the CMS deleteContentCategory domain operation.
     *
     * @param id operation input
     * @return Boolean operation result
     */
    Boolean deleteContentCategory(@NotNull(message = "分类 ID 不能为空") Long id);

}
