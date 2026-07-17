package io.mango.cms.starter.remote;

import io.mango.cms.api.CmsContentCategoryApi;
import io.mango.cms.api.command.SaveCmsContentCategoryCommand;
import io.mango.cms.api.command.UpdateCmsStatusCommand;
import io.mango.cms.api.query.CmsContentCategoryPageQuery;
import io.mango.cms.api.vo.CmsContentCategoryVO;
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
 * CMS 内容分类 Feign 适配器。
 */
@FeignClient(name = "mango-cms", contextId = "cmsContentCategoryFeignClient", path = "/cms")
public interface CmsContentCategoryFeignClient extends CmsContentCategoryApi {

    /**
     * {@inheritDoc}
     *
     * @param query 远程请求参数
     * @return 远程调用结果
     */
    @Override
    @GetMapping("/content-categories/page")
    R<PageResult<CmsContentCategoryVO>> pageContentCategories(@SpringQueryMap CmsContentCategoryPageQuery query);

    /**
     * {@inheritDoc}
     *
     * @param query 远程请求参数
     * @return 远程调用结果
     */
    @Override
    @GetMapping("/content-categories/list")
    R<List<CmsContentCategoryVO>> listContentCategories(@SpringQueryMap CmsContentCategoryPageQuery query);

    /**
     * {@inheritDoc}
     *
     * @param query 远程请求参数
     * @return 远程调用结果
     */
    @Override
    @GetMapping("/content-categories/tree")
    R<List<CmsContentCategoryVO>> treeContentCategories(@SpringQueryMap CmsContentCategoryPageQuery query);

    /**
     * {@inheritDoc}
     *
     * @param id 远程请求参数
     * @return 远程调用结果
     */
    @Override
    @GetMapping("/content-categories/detail")
    R<CmsContentCategoryVO> detailContentCategory(@RequestParam("id") Long id);

    /**
     * {@inheritDoc}
     *
     * @param command 远程请求参数
     * @return 远程调用结果
     */
    @Override
    @PostMapping("/content-categories")
    R<Long> createContentCategory(@RequestBody SaveCmsContentCategoryCommand command);

    /**
     * {@inheritDoc}
     *
     * @param command 远程请求参数
     * @return 远程调用结果
     */
    @Override
    @PutMapping("/content-categories")
    R<Boolean> updateContentCategory(@RequestBody SaveCmsContentCategoryCommand command);

    /**
     * {@inheritDoc}
     *
     * @param command 远程请求参数
     * @return 远程调用结果
     */
    @Override
    @PutMapping("/content-categories/status")
    R<Boolean> updateContentCategoryStatus(@RequestBody UpdateCmsStatusCommand command);

    /**
     * {@inheritDoc}
     *
     * @param id 远程请求参数
     * @return 远程调用结果
     */
    @Override
    @DeleteMapping("/content-categories")
    R<Boolean> deleteContentCategory(@RequestParam("id") Long id);
}
