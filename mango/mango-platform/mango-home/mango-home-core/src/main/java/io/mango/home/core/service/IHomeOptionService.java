package io.mango.home.core.service;

import io.mango.home.api.query.HomeUserOptionQuery;
import io.mango.home.api.vo.HomeUserOptionVO;

import java.util.List;

/** 首页管理候选项服务。 */
public interface IHomeOptionService {

    List<HomeUserOptionVO> listPageUserOptions(HomeUserOptionQuery query);

    List<HomeUserOptionVO> listVisibleUserOptions(HomeUserOptionQuery query);
}
