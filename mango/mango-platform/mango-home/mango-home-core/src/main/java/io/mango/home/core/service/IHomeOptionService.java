package io.mango.home.core.service;

import io.mango.home.api.query.HomeUserOptionQuery;
import io.mango.home.api.vo.HomeUserOptionVO;

import java.util.List;

/** 首页管理候选项服务。 */
public interface IHomeOptionService {

    /**
     * 查询首页列表筛选可用的用户候选项。
     *
     * @param query 候选项查询条件
     * @return 当前租户用户候选项
     */
    List<HomeUserOptionVO> listPageUserOptions(HomeUserOptionQuery query);

    /**
     * 查询用户首页预览可用的用户候选项。
     *
     * @param query 候选项查询条件
     * @return 当前租户可见用户候选项
     */
    List<HomeUserOptionVO> listVisibleUserOptions(HomeUserOptionQuery query);
}
