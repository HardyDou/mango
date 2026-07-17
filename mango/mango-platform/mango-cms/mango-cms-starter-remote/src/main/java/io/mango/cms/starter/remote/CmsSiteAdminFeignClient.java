package io.mango.cms.starter.remote;

import io.mango.cms.api.CmsSiteAdminApi;
import io.mango.cms.api.command.SaveCmsSiteCommand;
import io.mango.cms.api.command.UpdateCmsStatusCommand;
import io.mango.cms.api.query.CmsSitePageQuery;
import io.mango.cms.api.vo.CmsSiteVO;
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
 * CMS 站点管理 Feign 适配器。
 */
@FeignClient(name = "mango-cms", contextId = "cmsSiteAdminFeignClient", path = "/cms")
public interface CmsSiteAdminFeignClient extends CmsSiteAdminApi {

    /**
     * {@inheritDoc}
     *
     * @param query 远程请求参数
     * @return 远程调用结果
     */
    @Override
    @GetMapping("/sites/page")
    R<PageResult<CmsSiteVO>> pageSites(@SpringQueryMap CmsSitePageQuery query);

    /**
     * {@inheritDoc}
     *
     * @param id 远程请求参数
     * @return 远程调用结果
     */
    @Override
    @GetMapping("/sites/detail")
    R<CmsSiteVO> detailSite(@RequestParam("id") Long id);

    /**
     * {@inheritDoc}
     *
     * @param command 远程请求参数
     * @return 远程调用结果
     */
    @Override
    @PostMapping("/sites")
    R<Long> createSite(@RequestBody SaveCmsSiteCommand command);

    /**
     * {@inheritDoc}
     *
     * @param command 远程请求参数
     * @return 远程调用结果
     */
    @Override
    @PutMapping("/sites")
    R<Boolean> updateSite(@RequestBody SaveCmsSiteCommand command);

    /**
     * {@inheritDoc}
     *
     * @param command 远程请求参数
     * @return 远程调用结果
     */
    @Override
    @PutMapping("/sites/status")
    R<Boolean> updateSiteStatus(@RequestBody UpdateCmsStatusCommand command);

    /**
     * {@inheritDoc}
     *
     * @param id 远程请求参数
     * @return 远程调用结果
     */
    @Override
    @DeleteMapping("/sites")
    R<Boolean> deleteSite(@RequestParam("id") Long id);
}
