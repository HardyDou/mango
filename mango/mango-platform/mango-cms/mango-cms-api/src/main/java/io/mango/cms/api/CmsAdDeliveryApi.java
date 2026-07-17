package io.mango.cms.api;

import io.mango.cms.api.command.SaveCmsAdDeliveryCommand;
import io.mango.cms.api.command.UpdateCmsStatusCommand;
import io.mango.cms.api.query.CmsAdDeliveryPageQuery;
import io.mango.cms.api.vo.CmsAdDeliveryVO;
import io.mango.common.result.R;
import io.mango.common.vo.PageResult;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import org.springframework.validation.annotation.Validated;

/**
 * CMS 广告投放能力契约。
 */
@Validated
public interface CmsAdDeliveryApi {

    /**
     * 分页查询广告投放。
     *
     * @param query 请求参数
     * @return 调用结果
     */
    R<PageResult<CmsAdDeliveryVO>> pageAdDeliveries(@Valid CmsAdDeliveryPageQuery query);

    /**
     * 查询广告投放详情。
     *
     * @param id 请求参数
     * @return 调用结果
     */
    R<CmsAdDeliveryVO> detailAdDelivery(@NotNull(message = "广告投放 ID 不能为空") Long id);

    /**
     * 创建广告投放。
     *
     * @param command 请求参数
     * @return 调用结果
     */
    R<Long> createAdDelivery(@Valid SaveCmsAdDeliveryCommand command);

    /**
     * 更新广告投放。
     *
     * @param command 请求参数
     * @return 调用结果
     */
    R<Boolean> updateAdDelivery(@Valid SaveCmsAdDeliveryCommand command);

    /**
     * 更新广告投放状态。
     *
     * @param command 请求参数
     * @return 调用结果
     */
    R<Boolean> updateAdDeliveryStatus(@Valid UpdateCmsStatusCommand command);

    /**
     * 删除广告投放。
     *
     * @param id 请求参数
     * @return 调用结果
     */
    R<Boolean> deleteAdDelivery(@NotNull(message = "广告投放 ID 不能为空") Long id);
}
