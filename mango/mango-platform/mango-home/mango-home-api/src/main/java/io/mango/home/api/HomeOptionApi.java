package io.mango.home.api;

import io.mango.common.result.R;
import io.mango.home.api.query.HomeUserOptionQuery;
import io.mango.home.api.vo.HomeUserOptionVO;
import jakarta.validation.Valid;

import java.util.List;

/** 首页管理页面所需的窄候选项 API。 */
public interface HomeOptionApi {

    /**
     * 查询首页列表筛选可用的当前租户用户候选项。
     *
     * @param query 候选项查询条件
     * @return 当前租户用户候选项
     */
    R<List<HomeUserOptionVO>> listPageUserOptions(@Valid HomeUserOptionQuery query);

    /**
     * 查询用户首页预览可用的当前租户用户候选项。
     *
     * @param query 候选项查询条件
     * @return 当前租户可见用户候选项
     */
    R<List<HomeUserOptionVO>> listVisibleUserOptions(@Valid HomeUserOptionQuery query);
}
