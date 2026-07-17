package io.mango.cms.api;

import io.mango.cms.api.command.SaveCmsNavigationCommand;
import io.mango.cms.api.command.UpdateCmsStatusCommand;
import io.mango.cms.api.query.CmsNavigationPageQuery;
import io.mango.cms.api.vo.CmsNavigationVO;
import io.mango.common.result.R;
import io.mango.common.vo.PageResult;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import org.springframework.validation.annotation.Validated;

/**
 * CMS 导航管理能力契约。
 */
@Validated
public interface CmsNavigationApi {

    /**
     * 分页查询导航。
     *
     * @param query 请求参数
     * @return 调用结果
     */
    R<PageResult<CmsNavigationVO>> pageNavigations(@Valid CmsNavigationPageQuery query);

    /**
     * 查询导航详情。
     *
     * @param id 请求参数
     * @return 调用结果
     */
    R<CmsNavigationVO> detailNavigation(@NotNull(message = "导航 ID 不能为空") Long id);

    /**
     * 创建导航。
     *
     * @param command 请求参数
     * @return 调用结果
     */
    R<Long> createNavigation(@Valid SaveCmsNavigationCommand command);

    /**
     * 更新导航。
     *
     * @param command 请求参数
     * @return 调用结果
     */
    R<Boolean> updateNavigation(@Valid SaveCmsNavigationCommand command);

    /**
     * 更新导航状态。
     *
     * @param command 请求参数
     * @return 调用结果
     */
    R<Boolean> updateNavigationStatus(@Valid UpdateCmsStatusCommand command);

    /**
     * 删除导航。
     *
     * @param id 请求参数
     * @return 调用结果
     */
    R<Boolean> deleteNavigation(@NotNull(message = "导航 ID 不能为空") Long id);
}
