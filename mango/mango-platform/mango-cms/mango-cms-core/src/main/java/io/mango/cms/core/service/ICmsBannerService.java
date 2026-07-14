package io.mango.cms.core.service;

import io.mango.cms.api.command.SaveCmsBannerCommand;
import io.mango.cms.api.command.UpdateCmsStatusCommand;
import io.mango.cms.api.query.CmsBannerPageQuery;
import io.mango.cms.api.vo.CmsBannerVO;
import io.mango.common.vo.PageResult;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;

/** CMS Banner aggregate contract. */
public interface ICmsBannerService {

    /**
     * Executes the CMS pageBanners domain operation.
     *
     * @param query operation input
     * @return PageResult<CmsBannerVO> operation result
     */
    PageResult<CmsBannerVO> pageBanners(@Valid CmsBannerPageQuery query);

    /**
     * Executes the CMS detailBanner domain operation.
     *
     * @param id operation input
     * @return CmsBannerVO operation result
     */
    CmsBannerVO detailBanner(@NotNull(message = "Banner ID 不能为空") Long id);

    /**
     * Executes the CMS createBanner domain operation.
     *
     * @param command operation input
     * @return Long operation result
     */
    Long createBanner(@Valid SaveCmsBannerCommand command);

    /**
     * Executes the CMS updateBanner domain operation.
     *
     * @param command operation input
     * @return Boolean operation result
     */
    Boolean updateBanner(@Valid SaveCmsBannerCommand command);

    /**
     * Executes the CMS updateBannerStatus domain operation.
     *
     * @param command operation input
     * @return Boolean operation result
     */
    Boolean updateBannerStatus(@Valid UpdateCmsStatusCommand command);

    /**
     * Executes the CMS deleteBanner domain operation.
     *
     * @param id operation input
     * @return Boolean operation result
     */
    Boolean deleteBanner(@NotNull(message = "Banner ID 不能为空") Long id);

}
