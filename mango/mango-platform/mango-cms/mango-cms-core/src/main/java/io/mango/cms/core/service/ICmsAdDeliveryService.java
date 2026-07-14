package io.mango.cms.core.service;

import io.mango.cms.api.command.SaveCmsAdDeliveryCommand;
import io.mango.cms.api.command.UpdateCmsStatusCommand;
import io.mango.cms.api.query.CmsAdDeliveryPageQuery;
import io.mango.cms.api.vo.CmsAdDeliveryVO;
import io.mango.common.vo.PageResult;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;

/** CMS AdDelivery aggregate contract. */
public interface ICmsAdDeliveryService {

    /**
     * Executes the CMS pageAdDeliveries domain operation.
     *
     * @param query operation input
     * @return PageResult<CmsAdDeliveryVO> operation result
     */
    PageResult<CmsAdDeliveryVO> pageAdDeliveries(@Valid CmsAdDeliveryPageQuery query);

    /**
     * Executes the CMS detailAdDelivery domain operation.
     *
     * @param id operation input
     * @return CmsAdDeliveryVO operation result
     */
    CmsAdDeliveryVO detailAdDelivery(@NotNull(message = "广告投放 ID 不能为空") Long id);

    /**
     * Executes the CMS createAdDelivery domain operation.
     *
     * @param command operation input
     * @return Long operation result
     */
    Long createAdDelivery(@Valid SaveCmsAdDeliveryCommand command);

    /**
     * Executes the CMS updateAdDelivery domain operation.
     *
     * @param command operation input
     * @return Boolean operation result
     */
    Boolean updateAdDelivery(@Valid SaveCmsAdDeliveryCommand command);

    /**
     * Executes the CMS updateAdDeliveryStatus domain operation.
     *
     * @param command operation input
     * @return Boolean operation result
     */
    Boolean updateAdDeliveryStatus(@Valid UpdateCmsStatusCommand command);

    /**
     * Executes the CMS deleteAdDelivery domain operation.
     *
     * @param id operation input
     * @return Boolean operation result
     */
    Boolean deleteAdDelivery(@NotNull(message = "广告投放 ID 不能为空") Long id);

}
