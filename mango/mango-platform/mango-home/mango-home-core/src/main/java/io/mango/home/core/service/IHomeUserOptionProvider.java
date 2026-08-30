package io.mango.home.core.service;

import io.mango.home.api.query.HomeUserOptionQuery;
import io.mango.home.api.vo.HomeUserOptionVO;

import java.util.List;

/** 首页领域读取当前租户用户候选项所需的最小能力。 */
public interface IHomeUserOptionProvider {

    List<HomeUserOptionVO> list(HomeUserOptionQuery query);
}
