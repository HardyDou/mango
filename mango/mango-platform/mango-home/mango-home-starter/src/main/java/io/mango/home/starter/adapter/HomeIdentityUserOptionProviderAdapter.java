package io.mango.home.starter.adapter;

import io.mango.common.result.R;
import io.mango.common.result.Require;
import io.mango.common.vo.PageResult;
import io.mango.home.api.enums.HomeCode;
import io.mango.home.api.query.HomeUserOptionQuery;
import io.mango.home.api.vo.HomeUserOptionVO;
import io.mango.home.core.service.IHomeUserOptionProvider;
import io.mango.identity.api.IdentityUserApi;
import io.mango.identity.api.query.IdentityUserPageQuery;
import io.mango.identity.api.vo.IdentityUserVO;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.Objects;

/** 将 Identity 当前租户成员查询适配为 Home 最小用户候选能力。 */
@Component
@RequiredArgsConstructor
public class HomeIdentityUserOptionProviderAdapter implements IHomeUserOptionProvider {

    private final ObjectProvider<IdentityUserApi> identityUserApiProvider;

    @Override
    public List<HomeUserOptionVO> list(HomeUserOptionQuery query) {
        IdentityUserApi identityUserApi = Require.nonNull(identityUserApiProvider.getIfAvailable(),
                HomeCode.HOME_USER_OPTION_PROVIDER_MISSING);
        IdentityUserPageQuery identityQuery = new IdentityUserPageQuery();
        identityQuery.setPage(1L);
        identityQuery.setSize(query.getSize());
        identityQuery.setKeyword(trimToNull(query.getKeyword()));
        identityQuery.setStatus(1);
        R<PageResult<IdentityUserVO>> response = identityUserApi.page(identityQuery);
        Require.isTrue(response != null && response.isSuccess() && response.getData() != null,
                HomeCode.HOME_USER_OPTION_LOAD_FAILED);
        return Objects.requireNonNull(response).getData().getList().stream()
                .filter(user -> user.getUserId() != null)
                .map(this::toOption)
                .toList();
    }

    private HomeUserOptionVO toOption(IdentityUserVO user) {
        HomeUserOptionVO option = new HomeUserOptionVO();
        option.setUserId(user.getUserId());
        option.setMemberId(user.getMemberId());
        option.setDisplayName(firstText(user.getNickname(), user.getMemberName(), user.getUsername(),
                String.valueOf(user.getUserId())));
        option.setUsername(user.getUsername());
        return option;
    }

    private String trimToNull(String value) {
        return StringUtils.hasText(value) ? value.trim() : null;
    }

    private String firstText(String... values) {
        for (String value : values) {
            if (StringUtils.hasText(value)) {
                return value.trim();
            }
        }
        return null;
    }
}
