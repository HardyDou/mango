package io.mango.cms.api;

import io.mango.cms.api.command.SaveCmsBannerCommand;
import io.mango.cms.api.command.UpdateCmsStatusCommand;
import io.mango.cms.api.query.CmsBannerPageQuery;
import io.mango.cms.api.vo.CmsBannerVO;
import io.mango.common.result.R;
import io.mango.common.vo.PageResult;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import org.springframework.validation.annotation.Validated;

/**
 * CMS Banner 管理能力契约。
 */
@Validated
public interface CmsBannerApi {

    /**
     * 分页查询 Banner。
     *
     * @param query 请求参数
     * @return 调用结果
     */
    R<PageResult<CmsBannerVO>> pageBanners(@Valid CmsBannerPageQuery query);

    /**
     * 查询 Banner 详情。
     *
     * @param id 请求参数
     * @return 调用结果
     */
    R<CmsBannerVO> detailBanner(@NotNull(message = "Banner ID 不能为空") Long id);

    /**
     * 创建 Banner。
     *
     * @param command 请求参数
     * @return 调用结果
     */
    R<Long> createBanner(@Valid SaveCmsBannerCommand command);

    /**
     * 更新 Banner。
     *
     * @param command 请求参数
     * @return 调用结果
     */
    R<Boolean> updateBanner(@Valid SaveCmsBannerCommand command);

    /**
     * 更新 Banner 状态。
     *
     * @param command 请求参数
     * @return 调用结果
     */
    R<Boolean> updateBannerStatus(@Valid UpdateCmsStatusCommand command);

    /**
     * 删除 Banner。
     *
     * @param id 请求参数
     * @return 调用结果
     */
    R<Boolean> deleteBanner(@NotNull(message = "Banner ID 不能为空") Long id);
}
