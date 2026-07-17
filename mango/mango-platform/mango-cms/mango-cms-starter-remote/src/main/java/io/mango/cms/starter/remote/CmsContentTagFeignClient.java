package io.mango.cms.starter.remote;

import io.mango.cms.api.CmsContentTagApi;
import io.mango.cms.api.command.SaveCmsContentTagCommand;
import io.mango.cms.api.command.UpdateCmsStatusCommand;
import io.mango.cms.api.query.CmsContentTagPageQuery;
import io.mango.cms.api.vo.CmsContentTagVO;
import io.mango.common.result.R;
import io.mango.common.vo.PageResult;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.cloud.openfeign.SpringQueryMap;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;

/**
 * CMS 内容标签 Feign 适配器。
 */
@FeignClient(name = "mango-cms", contextId = "cmsContentTagFeignClient", path = "/cms")
public interface CmsContentTagFeignClient extends CmsContentTagApi {

    /**
     * {@inheritDoc}
     *
     * @param query 远程请求参数
     * @return 远程调用结果
     */
    @Override
    @GetMapping("/content-tags/page")
    R<PageResult<CmsContentTagVO>> pageContentTags(@SpringQueryMap CmsContentTagPageQuery query);

    /**
     * {@inheritDoc}
     *
     * @param query 远程请求参数
     * @return 远程调用结果
     */
    @Override
    @GetMapping("/content-tags/list")
    R<List<CmsContentTagVO>> listContentTags(@SpringQueryMap CmsContentTagPageQuery query);

    /**
     * {@inheritDoc}
     *
     * @param id 远程请求参数
     * @return 远程调用结果
     */
    @Override
    @GetMapping("/content-tags/detail")
    R<CmsContentTagVO> detailContentTag(@RequestParam("id") Long id);

    /**
     * {@inheritDoc}
     *
     * @param command 远程请求参数
     * @return 远程调用结果
     */
    @Override
    @PostMapping("/content-tags")
    R<Long> createContentTag(@RequestBody SaveCmsContentTagCommand command);

    /**
     * {@inheritDoc}
     *
     * @param command 远程请求参数
     * @return 远程调用结果
     */
    @Override
    @PutMapping("/content-tags")
    R<Boolean> updateContentTag(@RequestBody SaveCmsContentTagCommand command);

    /**
     * {@inheritDoc}
     *
     * @param command 远程请求参数
     * @return 远程调用结果
     */
    @Override
    @PutMapping("/content-tags/status")
    R<Boolean> updateContentTagStatus(@RequestBody UpdateCmsStatusCommand command);

    /**
     * {@inheritDoc}
     *
     * @param id 远程请求参数
     * @return 远程调用结果
     */
    @Override
    @DeleteMapping("/content-tags")
    R<Boolean> deleteContentTag(@RequestParam("id") Long id);
}
