package io.mango.cms.core.service;

import io.mango.cms.api.command.SaveCmsContentTagCommand;
import io.mango.cms.api.command.UpdateCmsStatusCommand;
import io.mango.cms.api.query.CmsContentTagPageQuery;
import io.mango.cms.api.vo.CmsContentTagVO;
import io.mango.common.vo.PageResult;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import java.util.List;

/** CMS ContentTag aggregate contract. */
public interface ICmsContentTagService {

    /**
     * Executes the CMS pageContentTags domain operation.
     *
     * @param query operation input
     * @return PageResult<CmsContentTagVO> operation result
     */
    PageResult<CmsContentTagVO> pageContentTags(@Valid CmsContentTagPageQuery query);

    /**
     * Executes the CMS listContentTags domain operation.
     *
     * @param query operation input
     * @return List<CmsContentTagVO> operation result
     */
    List<CmsContentTagVO> listContentTags(@Valid CmsContentTagPageQuery query);

    /**
     * Executes the CMS detailContentTag domain operation.
     *
     * @param id operation input
     * @return CmsContentTagVO operation result
     */
    CmsContentTagVO detailContentTag(@NotNull(message = "标签 ID 不能为空") Long id);

    /**
     * Executes the CMS createContentTag domain operation.
     *
     * @param command operation input
     * @return Long operation result
     */
    Long createContentTag(@Valid SaveCmsContentTagCommand command);

    /**
     * Executes the CMS updateContentTag domain operation.
     *
     * @param command operation input
     * @return Boolean operation result
     */
    Boolean updateContentTag(@Valid SaveCmsContentTagCommand command);

    /**
     * Executes the CMS updateContentTagStatus domain operation.
     *
     * @param command operation input
     * @return Boolean operation result
     */
    Boolean updateContentTagStatus(@Valid UpdateCmsStatusCommand command);

    /**
     * Executes the CMS deleteContentTag domain operation.
     *
     * @param id operation input
     * @return Boolean operation result
     */
    Boolean deleteContentTag(@NotNull(message = "标签 ID 不能为空") Long id);

}
