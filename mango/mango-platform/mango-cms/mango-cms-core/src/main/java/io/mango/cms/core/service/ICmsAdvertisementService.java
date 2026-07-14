package io.mango.cms.core.service;

import io.mango.cms.api.command.SaveCmsAdvertisementCommand;
import io.mango.cms.api.command.UpdateCmsStatusCommand;
import io.mango.cms.api.query.CmsAdvertisementPageQuery;
import io.mango.cms.api.vo.CmsAdvertisementVO;
import io.mango.common.vo.PageResult;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;

/** CMS Advertisement aggregate contract. */
public interface ICmsAdvertisementService {

    /**
     * Executes the CMS pageAdvertisements domain operation.
     *
     * @param query operation input
     * @return PageResult<CmsAdvertisementVO> operation result
     */
    PageResult<CmsAdvertisementVO> pageAdvertisements(@Valid CmsAdvertisementPageQuery query);

    /**
     * Executes the CMS detailAdvertisement domain operation.
     *
     * @param id operation input
     * @return CmsAdvertisementVO operation result
     */
    CmsAdvertisementVO detailAdvertisement(@NotNull(message = "广告 ID 不能为空") Long id);

    /**
     * Executes the CMS createAdvertisement domain operation.
     *
     * @param command operation input
     * @return Long operation result
     */
    Long createAdvertisement(@Valid SaveCmsAdvertisementCommand command);

    /**
     * Executes the CMS updateAdvertisement domain operation.
     *
     * @param command operation input
     * @return Boolean operation result
     */
    Boolean updateAdvertisement(@Valid SaveCmsAdvertisementCommand command);

    /**
     * Executes the CMS updateAdvertisementStatus domain operation.
     *
     * @param command operation input
     * @return Boolean operation result
     */
    Boolean updateAdvertisementStatus(@Valid UpdateCmsStatusCommand command);

    /**
     * Executes the CMS deleteAdvertisement domain operation.
     *
     * @param id operation input
     * @return Boolean operation result
     */
    Boolean deleteAdvertisement(@NotNull(message = "广告 ID 不能为空") Long id);

}
