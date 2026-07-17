package io.mango.cms.api;

import io.mango.cms.api.command.SaveCmsSiteSettingCommand;
import io.mango.cms.api.vo.CmsSiteSettingVO;
import io.mango.common.result.R;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import org.springframework.validation.annotation.Validated;

/**
 * CMS 站点设置能力契约。
 */
@Validated
public interface CmsSiteSettingApi {

    /**
     * 查询站点设置详情。
     *
     * @param siteId 请求参数
     * @return 调用结果
     */
    R<CmsSiteSettingVO> detailSiteSetting(@NotNull(message = "站点 ID 不能为空") Long siteId);

    /**
     * 保存站点设置。
     *
     * @param command 请求参数
     * @return 调用结果
     */
    R<Boolean> saveSiteSetting(@Valid SaveCmsSiteSettingCommand command);
}
