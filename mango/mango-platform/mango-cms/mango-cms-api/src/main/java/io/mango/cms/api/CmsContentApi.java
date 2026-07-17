package io.mango.cms.api;

import io.mango.cms.api.command.CmsOfflineCommand;
import io.mango.cms.api.command.SaveCmsContentCommand;
import io.mango.cms.api.command.UpdateCmsContentReviewCommand;
import io.mango.cms.api.query.CmsContentPageQuery;
import io.mango.cms.api.vo.CmsContentVO;
import io.mango.common.result.R;
import io.mango.common.vo.PageResult;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import org.springframework.validation.annotation.Validated;

/**
 * CMS 内容管理能力契约。
 */
@Validated
public interface CmsContentApi {

    /**
     * 分页查询内容。
     *
     * @param query 请求参数
     * @return 调用结果
     */
    R<PageResult<CmsContentVO>> pageContents(@Valid CmsContentPageQuery query);

    /**
     * 查询内容详情。
     *
     * @param id 请求参数
     * @return 调用结果
     */
    R<CmsContentVO> detailContent(@NotNull(message = "内容 ID 不能为空") Long id);

    /**
     * 创建内容。
     *
     * @param command 请求参数
     * @return 调用结果
     */
    R<Long> createContent(@Valid SaveCmsContentCommand command);

    /**
     * 更新内容。
     *
     * @param command 请求参数
     * @return 调用结果
     */
    R<Boolean> updateContent(@Valid SaveCmsContentCommand command);

    /**
     * 提交内容审核。
     *
     * @param command 请求参数
     * @return 调用结果
     */
    R<Boolean> submitContent(@Valid CmsOfflineCommand command);

    /**
     * 审核通过内容。
     *
     * @param command 请求参数
     * @return 调用结果
     */
    R<Boolean> approveContent(@Valid UpdateCmsContentReviewCommand command);

    /**
     * 审核拒绝内容。
     *
     * @param command 请求参数
     * @return 调用结果
     */
    R<Boolean> rejectContent(@Valid UpdateCmsContentReviewCommand command);

    /**
     * 下线内容。
     *
     * @param command 请求参数
     * @return 调用结果
     */
    R<Boolean> offlineContent(@Valid CmsOfflineCommand command);

    /**
     * 删除内容。
     *
     * @param id 请求参数
     * @return 调用结果
     */
    R<Boolean> deleteContent(@NotNull(message = "内容 ID 不能为空") Long id);
}
