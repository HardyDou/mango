package io.mango.cms.starter.remote;

import io.mango.cms.api.CmsSiteSettingApi;
import io.mango.cms.api.command.SaveCmsSiteSettingCommand;
import io.mango.cms.api.vo.CmsSiteSettingVO;
import io.mango.common.result.R;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;

/**
 * CMS 站点设置 Feign 适配器。
 */
@FeignClient(name = "mango-cms", contextId = "cmsSiteSettingFeignClient", path = "/cms")
public interface CmsSiteSettingFeignClient extends CmsSiteSettingApi {

    /**
     * {@inheritDoc}
     *
     * @param siteId 远程请求参数
     * @return 远程调用结果
     */
    @Override
    @GetMapping("/site-settings/detail")
    R<CmsSiteSettingVO> detailSiteSetting(@RequestParam("siteId") Long siteId);

    /**
     * {@inheritDoc}
     *
     * @param command 远程请求参数
     * @return 远程调用结果
     */
    @Override
    @PutMapping("/site-settings")
    R<Boolean> saveSiteSetting(@RequestBody SaveCmsSiteSettingCommand command);
}
