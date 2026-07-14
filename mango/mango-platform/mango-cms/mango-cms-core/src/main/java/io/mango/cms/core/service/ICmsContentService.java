package io.mango.cms.core.service;

import io.mango.cms.api.command.CmsOfflineCommand;
import io.mango.cms.api.command.SaveCmsContentCommand;
import io.mango.cms.api.command.UpdateCmsContentReviewCommand;
import io.mango.cms.api.query.CmsContentPageQuery;
import io.mango.cms.api.vo.CmsContentVO;
import io.mango.common.vo.PageResult;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;

/** CMS Content aggregate contract. */
public interface ICmsContentService {

    /**
     * Executes the CMS pageContents domain operation.
     *
     * @param query operation input
     * @return PageResult<CmsContentVO> operation result
     */
    PageResult<CmsContentVO> pageContents(@Valid CmsContentPageQuery query);

    /**
     * Executes the CMS detailContent domain operation.
     *
     * @param id operation input
     * @return CmsContentVO operation result
     */
    CmsContentVO detailContent(@NotNull(message = "内容 ID 不能为空") Long id);

    /**
     * Executes the CMS createContent domain operation.
     *
     * @param command operation input
     * @return Long operation result
     */
    Long createContent(@Valid SaveCmsContentCommand command);

    /**
     * Executes the CMS updateContent domain operation.
     *
     * @param command operation input
     * @return Boolean operation result
     */
    Boolean updateContent(@Valid SaveCmsContentCommand command);

    /**
     * Executes the CMS submitContent domain operation.
     *
     * @param command operation input
     * @return Boolean operation result
     */
    Boolean submitContent(@Valid CmsOfflineCommand command);

    /**
     * Executes the CMS approveContent domain operation.
     *
     * @param command operation input
     * @return Boolean operation result
     */
    Boolean approveContent(@Valid UpdateCmsContentReviewCommand command);

    /**
     * Executes the CMS rejectContent domain operation.
     *
     * @param command operation input
     * @return Boolean operation result
     */
    Boolean rejectContent(@Valid UpdateCmsContentReviewCommand command);

    /**
     * Executes the CMS offlineContent domain operation.
     *
     * @param command operation input
     * @return Boolean operation result
     */
    Boolean offlineContent(@Valid CmsOfflineCommand command);

    /**
     * Executes the CMS deleteContent domain operation.
     *
     * @param id operation input
     * @return Boolean operation result
     */
    Boolean deleteContent(@NotNull(message = "内容 ID 不能为空") Long id);

}
