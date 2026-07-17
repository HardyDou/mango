package io.mango.cms.starter.remote;

import io.mango.cms.api.CmsSiteCategoryApi;
import io.mango.cms.api.command.SaveCmsSiteCategoryCommand;
import io.mango.cms.api.command.UpdateCmsStatusCommand;
import io.mango.cms.api.query.CmsSiteCategoryTreeQuery;
import io.mango.cms.api.vo.CmsSiteCategoryVO;
import io.mango.common.result.R;
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
 * CMS 站点栏目 Feign 适配器。
 */
@FeignClient(name = "mango-cms", contextId = "cmsSiteCategoryFeignClient", path = "/cms")
public interface CmsSiteCategoryFeignClient extends CmsSiteCategoryApi {

    /**
     * {@inheritDoc}
     *
     * @param query 远程请求参数
     * @return 远程调用结果
     */
    @Override
    @GetMapping("/site-categories/tree")
    R<List<CmsSiteCategoryVO>> treeSiteCategories(@SpringQueryMap CmsSiteCategoryTreeQuery query);

    /**
     * {@inheritDoc}
     *
     * @param id 远程请求参数
     * @return 远程调用结果
     */
    @Override
    @GetMapping("/site-categories/detail")
    R<CmsSiteCategoryVO> detailSiteCategory(@RequestParam("id") Long id);

    /**
     * {@inheritDoc}
     *
     * @param command 远程请求参数
     * @return 远程调用结果
     */
    @Override
    @PostMapping("/site-categories")
    R<Long> createSiteCategory(@RequestBody SaveCmsSiteCategoryCommand command);

    /**
     * {@inheritDoc}
     *
     * @param command 远程请求参数
     * @return 远程调用结果
     */
    @Override
    @PutMapping("/site-categories")
    R<Boolean> updateSiteCategory(@RequestBody SaveCmsSiteCategoryCommand command);

    /**
     * {@inheritDoc}
     *
     * @param command 远程请求参数
     * @return 远程调用结果
     */
    @Override
    @PutMapping("/site-categories/status")
    R<Boolean> updateSiteCategoryStatus(@RequestBody UpdateCmsStatusCommand command);

    /**
     * {@inheritDoc}
     *
     * @param id 远程请求参数
     * @return 远程调用结果
     */
    @Override
    @DeleteMapping("/site-categories")
    R<Boolean> deleteSiteCategory(@RequestParam("id") Long id);
}
