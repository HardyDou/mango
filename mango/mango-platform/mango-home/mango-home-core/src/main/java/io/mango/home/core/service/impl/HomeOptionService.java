package io.mango.home.core.service.impl;

import io.mango.home.api.query.HomeUserOptionQuery;
import io.mango.home.api.vo.HomeUserOptionVO;
import io.mango.home.core.service.IHomeOptionService;
import io.mango.home.core.service.IHomeUserOptionProvider;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

/** 首页管理候选项服务实现。 */
@Service
@RequiredArgsConstructor
public class HomeOptionService implements IHomeOptionService {

    private static final long DEFAULT_OPTION_SIZE = 50L;

    private final IHomeUserOptionProvider userOptionProvider;

    @Override
    public List<HomeUserOptionVO> listPageUserOptions(HomeUserOptionQuery query) {
        return userOptionProvider.list(resolve(query));
    }

    @Override
    public List<HomeUserOptionVO> listVisibleUserOptions(HomeUserOptionQuery query) {
        return userOptionProvider.list(resolve(query));
    }

    private HomeUserOptionQuery resolve(HomeUserOptionQuery query) {
        HomeUserOptionQuery resolved = query == null ? new HomeUserOptionQuery() : query;
        if (resolved.getSize() == null) {
            resolved.setSize(DEFAULT_OPTION_SIZE);
        }
        return resolved;
    }
}
