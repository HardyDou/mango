package io.mango.cms.core.service;

import io.mango.cms.api.command.SaveCmsSiteSettingCommand;
import io.mango.cms.api.vo.CmsSiteSettingVO;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;

/** CMS SiteSetting aggregate contract. */
public interface ICmsSiteSettingService {

    /**
     * Executes the CMS detailSiteSetting domain operation.
     *
     * @param siteId operation input
     * @return CmsSiteSettingVO operation result
     */
    CmsSiteSettingVO detailSiteSetting(@NotNull(message = "站点 ID 不能为空") Long siteId);

    /**
     * Executes the CMS saveSiteSetting domain operation.
     *
     * @param command operation input
     * @return Boolean operation result
     */
    Boolean saveSiteSetting(@Valid SaveCmsSiteSettingCommand command);

}
