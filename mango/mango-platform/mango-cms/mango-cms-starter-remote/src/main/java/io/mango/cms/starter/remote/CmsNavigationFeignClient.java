package io.mango.cms.starter.remote;

import io.mango.cms.api.CmsNavigationApi;
import io.mango.cms.api.command.SaveCmsNavigationCommand;
import io.mango.cms.api.command.UpdateCmsStatusCommand;
import io.mango.cms.api.query.CmsNavigationPageQuery;
import io.mango.cms.api.vo.CmsNavigationVO;
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

/**
 * CMS 导航管理 Feign 适配器。
 */
@FeignClient(name = "mango-cms", contextId = "cmsNavigationFeignClient", path = "/cms")
public interface CmsNavigationFeignClient extends CmsNavigationApi {

    /**
     * {@inheritDoc}
     *
     * @param query 远程请求参数
     * @return 远程调用结果
     */
    @Override
    @GetMapping("/navigations/page")
    R<PageResult<CmsNavigationVO>> pageNavigations(@SpringQueryMap CmsNavigationPageQuery query);

    /**
     * {@inheritDoc}
     *
     * @param id 远程请求参数
     * @return 远程调用结果
     */
    @Override
    @GetMapping("/navigations/detail")
    R<CmsNavigationVO> detailNavigation(@RequestParam("id") Long id);

    /**
     * {@inheritDoc}
     *
     * @param command 远程请求参数
     * @return 远程调用结果
     */
    @Override
    @PostMapping("/navigations")
    R<Long> createNavigation(@RequestBody SaveCmsNavigationCommand command);

    /**
     * {@inheritDoc}
     *
     * @param command 远程请求参数
     * @return 远程调用结果
     */
    @Override
    @PutMapping("/navigations")
    R<Boolean> updateNavigation(@RequestBody SaveCmsNavigationCommand command);

    /**
     * {@inheritDoc}
     *
     * @param command 远程请求参数
     * @return 远程调用结果
     */
    @Override
    @PutMapping("/navigations/status")
    R<Boolean> updateNavigationStatus(@RequestBody UpdateCmsStatusCommand command);

    /**
     * {@inheritDoc}
     *
     * @param id 远程请求参数
     * @return 远程调用结果
     */
    @Override
    @DeleteMapping("/navigations")
    R<Boolean> deleteNavigation(@RequestParam("id") Long id);
}
