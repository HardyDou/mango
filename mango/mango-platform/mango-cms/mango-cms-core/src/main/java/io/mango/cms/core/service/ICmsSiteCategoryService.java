package io.mango.cms.core.service;

import io.mango.cms.api.command.SaveCmsSiteCategoryCommand;
import io.mango.cms.api.command.UpdateCmsStatusCommand;
import io.mango.cms.api.query.CmsSiteCategoryTreeQuery;
import io.mango.cms.api.vo.CmsSiteCategoryVO;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import java.util.List;

/** CMS SiteCategory aggregate contract. */
public interface ICmsSiteCategoryService {

    /**
     * Executes the CMS treeSiteCategories domain operation.
     *
     * @param query operation input
     * @return List<CmsSiteCategoryVO> operation result
     */
    List<CmsSiteCategoryVO> treeSiteCategories(@Valid CmsSiteCategoryTreeQuery query);

    /**
     * Executes the CMS detailSiteCategory domain operation.
     *
     * @param id operation input
     * @return CmsSiteCategoryVO operation result
     */
    CmsSiteCategoryVO detailSiteCategory(@NotNull(message = "栏目 ID 不能为空") Long id);

    /**
     * Executes the CMS createSiteCategory domain operation.
     *
     * @param command operation input
     * @return Long operation result
     */
    Long createSiteCategory(@Valid SaveCmsSiteCategoryCommand command);

    /**
     * Executes the CMS updateSiteCategory domain operation.
     *
     * @param command operation input
     * @return Boolean operation result
     */
    Boolean updateSiteCategory(@Valid SaveCmsSiteCategoryCommand command);

    /**
     * Executes the CMS updateSiteCategoryStatus domain operation.
     *
     * @param command operation input
     * @return Boolean operation result
     */
    Boolean updateSiteCategoryStatus(@Valid UpdateCmsStatusCommand command);

    /**
     * Executes the CMS deleteSiteCategory domain operation.
     *
     * @param id operation input
     * @return Boolean operation result
     */
    Boolean deleteSiteCategory(@NotNull(message = "栏目 ID 不能为空") Long id);

}
