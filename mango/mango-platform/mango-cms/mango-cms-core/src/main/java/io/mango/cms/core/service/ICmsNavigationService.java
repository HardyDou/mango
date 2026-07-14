package io.mango.cms.core.service;

import io.mango.cms.api.command.SaveCmsNavigationCommand;
import io.mango.cms.api.command.UpdateCmsStatusCommand;
import io.mango.cms.api.query.CmsNavigationPageQuery;
import io.mango.cms.api.vo.CmsNavigationVO;
import io.mango.common.vo.PageResult;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;

/** CMS Navigation aggregate contract. */
public interface ICmsNavigationService {

    /**
     * Executes the CMS pageNavigations domain operation.
     *
     * @param query operation input
     * @return PageResult<CmsNavigationVO> operation result
     */
    PageResult<CmsNavigationVO> pageNavigations(@Valid CmsNavigationPageQuery query);

    /**
     * Executes the CMS detailNavigation domain operation.
     *
     * @param id operation input
     * @return CmsNavigationVO operation result
     */
    CmsNavigationVO detailNavigation(@NotNull(message = "导航 ID 不能为空") Long id);

    /**
     * Executes the CMS createNavigation domain operation.
     *
     * @param command operation input
     * @return Long operation result
     */
    Long createNavigation(@Valid SaveCmsNavigationCommand command);

    /**
     * Executes the CMS updateNavigation domain operation.
     *
     * @param command operation input
     * @return Boolean operation result
     */
    Boolean updateNavigation(@Valid SaveCmsNavigationCommand command);

    /**
     * Executes the CMS updateNavigationStatus domain operation.
     *
     * @param command operation input
     * @return Boolean operation result
     */
    Boolean updateNavigationStatus(@Valid UpdateCmsStatusCommand command);

    /**
     * Executes the CMS deleteNavigation domain operation.
     *
     * @param id operation input
     * @return Boolean operation result
     */
    Boolean deleteNavigation(@NotNull(message = "导航 ID 不能为空") Long id);

}
