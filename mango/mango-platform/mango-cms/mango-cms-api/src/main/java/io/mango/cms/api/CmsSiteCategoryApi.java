package io.mango.cms.api;

import io.mango.cms.api.command.SaveCmsSiteCategoryCommand;
import io.mango.cms.api.command.UpdateCmsStatusCommand;
import io.mango.cms.api.query.CmsSiteCategoryTreeQuery;
import io.mango.cms.api.vo.CmsSiteCategoryVO;
import io.mango.common.result.R;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import org.springframework.validation.annotation.Validated;

import java.util.List;

/**
 * CMS 站点栏目能力契约。
 */
@Validated
public interface CmsSiteCategoryApi {

    /**
     * 查询站点栏目树。
     *
     * @param query 请求参数
     * @return 调用结果
     */
    R<List<CmsSiteCategoryVO>> treeSiteCategories(@Valid CmsSiteCategoryTreeQuery query);

    /**
     * 查询站点栏目详情。
     *
     * @param id 请求参数
     * @return 调用结果
     */
    R<CmsSiteCategoryVO> detailSiteCategory(@NotNull(message = "栏目 ID 不能为空") Long id);

    /**
     * 创建站点栏目。
     *
     * @param command 请求参数
     * @return 调用结果
     */
    R<Long> createSiteCategory(@Valid SaveCmsSiteCategoryCommand command);

    /**
     * 更新站点栏目。
     *
     * @param command 请求参数
     * @return 调用结果
     */
    R<Boolean> updateSiteCategory(@Valid SaveCmsSiteCategoryCommand command);

    /**
     * 更新站点栏目状态。
     *
     * @param command 请求参数
     * @return 调用结果
     */
    R<Boolean> updateSiteCategoryStatus(@Valid UpdateCmsStatusCommand command);

    /**
     * 删除站点栏目。
     *
     * @param id 请求参数
     * @return 调用结果
     */
    R<Boolean> deleteSiteCategory(@NotNull(message = "栏目 ID 不能为空") Long id);
}
