package io.mango.cms.api;

import io.mango.cms.api.command.SaveCmsContentCategoryCommand;
import io.mango.cms.api.command.UpdateCmsStatusCommand;
import io.mango.cms.api.query.CmsContentCategoryPageQuery;
import io.mango.cms.api.vo.CmsContentCategoryVO;
import io.mango.common.result.R;
import io.mango.common.vo.PageResult;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import org.springframework.validation.annotation.Validated;

import java.util.List;

/**
 * CMS 内容分类能力契约。
 */
@Validated
public interface CmsContentCategoryApi {

    /**
     * 分页查询内容分类。
     *
     * @param query 请求参数
     * @return 调用结果
     */
    R<PageResult<CmsContentCategoryVO>> pageContentCategories(@Valid CmsContentCategoryPageQuery query);

    /**
     * 查询内容分类列表。
     *
     * @param query 请求参数
     * @return 调用结果
     */
    R<List<CmsContentCategoryVO>> listContentCategories(@Valid CmsContentCategoryPageQuery query);

    /**
     * 查询内容分类树。
     *
     * @param query 请求参数
     * @return 调用结果
     */
    R<List<CmsContentCategoryVO>> treeContentCategories(@Valid CmsContentCategoryPageQuery query);

    /**
     * 查询内容分类详情。
     *
     * @param id 请求参数
     * @return 调用结果
     */
    R<CmsContentCategoryVO> detailContentCategory(@NotNull(message = "分类 ID 不能为空") Long id);

    /**
     * 创建内容分类。
     *
     * @param command 请求参数
     * @return 调用结果
     */
    R<Long> createContentCategory(@Valid SaveCmsContentCategoryCommand command);

    /**
     * 更新内容分类。
     *
     * @param command 请求参数
     * @return 调用结果
     */
    R<Boolean> updateContentCategory(@Valid SaveCmsContentCategoryCommand command);

    /**
     * 更新内容分类状态。
     *
     * @param command 请求参数
     * @return 调用结果
     */
    R<Boolean> updateContentCategoryStatus(@Valid UpdateCmsStatusCommand command);

    /**
     * 删除内容分类。
     *
     * @param id 请求参数
     * @return 调用结果
     */
    R<Boolean> deleteContentCategory(@NotNull(message = "分类 ID 不能为空") Long id);
}
