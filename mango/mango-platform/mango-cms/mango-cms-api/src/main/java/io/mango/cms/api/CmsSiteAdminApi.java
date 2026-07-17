package io.mango.cms.api;

import io.mango.cms.api.command.SaveCmsSiteCommand;
import io.mango.cms.api.command.UpdateCmsStatusCommand;
import io.mango.cms.api.query.CmsSitePageQuery;
import io.mango.cms.api.vo.CmsSiteVO;
import io.mango.common.result.R;
import io.mango.common.vo.PageResult;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import org.springframework.validation.annotation.Validated;

/**
 * CMS 站点管理能力契约。
 */
@Validated
public interface CmsSiteAdminApi {

    /**
     * 分页查询站点。
     *
     * @param query 请求参数
     * @return 调用结果
     */
    R<PageResult<CmsSiteVO>> pageSites(@Valid CmsSitePageQuery query);

    /**
     * 查询站点详情。
     *
     * @param id 请求参数
     * @return 调用结果
     */
    R<CmsSiteVO> detailSite(@NotNull(message = "站点 ID 不能为空") Long id);

    /**
     * 创建站点。
     *
     * @param command 请求参数
     * @return 调用结果
     */
    R<Long> createSite(@Valid SaveCmsSiteCommand command);

    /**
     * 更新站点。
     *
     * @param command 请求参数
     * @return 调用结果
     */
    R<Boolean> updateSite(@Valid SaveCmsSiteCommand command);

    /**
     * 更新站点状态。
     *
     * @param command 请求参数
     * @return 调用结果
     */
    R<Boolean> updateSiteStatus(@Valid UpdateCmsStatusCommand command);

    /**
     * 删除站点。
     *
     * @param id 请求参数
     * @return 调用结果
     */
    R<Boolean> deleteSite(@NotNull(message = "站点 ID 不能为空") Long id);
}
