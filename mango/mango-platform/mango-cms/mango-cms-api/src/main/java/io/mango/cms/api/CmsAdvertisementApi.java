package io.mango.cms.api;

import io.mango.cms.api.command.SaveCmsAdvertisementCommand;
import io.mango.cms.api.command.UpdateCmsStatusCommand;
import io.mango.cms.api.query.CmsAdvertisementPageQuery;
import io.mango.cms.api.vo.CmsAdvertisementVO;
import io.mango.common.result.R;
import io.mango.common.vo.PageResult;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import org.springframework.validation.annotation.Validated;

/**
 * CMS 广告管理能力契约。
 */
@Validated
public interface CmsAdvertisementApi {

    /**
     * 分页查询广告位。
     *
     * @param query 请求参数
     * @return 调用结果
     */
    R<PageResult<CmsAdvertisementVO>> pageAdvertisements(@Valid CmsAdvertisementPageQuery query);

    /**
     * 查询广告位详情。
     *
     * @param id 请求参数
     * @return 调用结果
     */
    R<CmsAdvertisementVO> detailAdvertisement(@NotNull(message = "广告 ID 不能为空") Long id);

    /**
     * 创建广告位。
     *
     * @param command 请求参数
     * @return 调用结果
     */
    R<Long> createAdvertisement(@Valid SaveCmsAdvertisementCommand command);

    /**
     * 更新广告位。
     *
     * @param command 请求参数
     * @return 调用结果
     */
    R<Boolean> updateAdvertisement(@Valid SaveCmsAdvertisementCommand command);

    /**
     * 更新广告位状态。
     *
     * @param command 请求参数
     * @return 调用结果
     */
    R<Boolean> updateAdvertisementStatus(@Valid UpdateCmsStatusCommand command);

    /**
     * 删除广告位。
     *
     * @param id 请求参数
     * @return 调用结果
     */
    R<Boolean> deleteAdvertisement(@NotNull(message = "广告 ID 不能为空") Long id);
}
