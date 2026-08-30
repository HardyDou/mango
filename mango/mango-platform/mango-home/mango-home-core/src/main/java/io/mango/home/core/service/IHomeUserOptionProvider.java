package io.mango.home.core.service;

import io.mango.home.api.query.HomeUserOptionQuery;
import io.mango.home.api.vo.HomeUserOptionVO;

import java.util.List;

/** 首页领域读取当前租户用户候选项所需的最小能力。 */
public interface IHomeUserOptionProvider {

    /**
     * 读取当前租户内符合条件的用户候选项。
     *
     * @param query 候选项查询条件
     * @return 当前租户用户候选项
     */
    List<HomeUserOptionVO> list(HomeUserOptionQuery query);
}
