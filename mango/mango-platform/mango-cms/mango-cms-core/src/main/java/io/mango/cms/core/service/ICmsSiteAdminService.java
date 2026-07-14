package io.mango.cms.core.service;

import io.mango.cms.api.command.SaveCmsSiteCommand;
import io.mango.cms.api.command.UpdateCmsStatusCommand;
import io.mango.cms.api.query.CmsSitePageQuery;
import io.mango.cms.api.vo.CmsSiteVO;
import io.mango.common.vo.PageResult;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;

/** CMS SiteAdmin aggregate contract. */
public interface ICmsSiteAdminService {

    /**
     * Executes the CMS pageSites domain operation.
     *
     * @param query operation input
     * @return PageResult<CmsSiteVO> operation result
     */
    PageResult<CmsSiteVO> pageSites(@Valid CmsSitePageQuery query);

    /**
     * Executes the CMS detailSite domain operation.
     *
     * @param id operation input
     * @return CmsSiteVO operation result
     */
    CmsSiteVO detailSite(@NotNull(message = "站点 ID 不能为空") Long id);

    /**
     * Executes the CMS createSite domain operation.
     *
     * @param command operation input
     * @return Long operation result
     */
    Long createSite(@Valid SaveCmsSiteCommand command);

    /**
     * Executes the CMS updateSite domain operation.
     *
     * @param command operation input
     * @return Boolean operation result
     */
    Boolean updateSite(@Valid SaveCmsSiteCommand command);

    /**
     * Executes the CMS updateSiteStatus domain operation.
     *
     * @param command operation input
     * @return Boolean operation result
     */
    Boolean updateSiteStatus(@Valid UpdateCmsStatusCommand command);

    /**
     * Executes the CMS deleteSite domain operation.
     *
     * @param id operation input
     * @return Boolean operation result
     */
    Boolean deleteSite(@NotNull(message = "站点 ID 不能为空") Long id);

}
