package io.mango.home.starter.adapter;

import io.mango.common.exception.BizException;
import io.mango.common.result.R;
import io.mango.common.vo.PageResult;
import io.mango.home.api.enums.HomeCode;
import io.mango.home.api.query.HomeUserOptionQuery;
import io.mango.home.api.vo.HomeUserOptionVO;
import io.mango.identity.api.IdentityUserApi;
import io.mango.identity.api.query.IdentityUserPageQuery;
import io.mango.identity.api.vo.IdentityUserVO;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.ObjectProvider;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class HomeIdentityUserOptionProviderAdapterTest {

    @Test
    void mapsOnlyMinimalEnabledCurrentTenantUserFields() {
        IdentityUserApi identityUserApi = mock(IdentityUserApi.class);
        IdentityUserVO user = new IdentityUserVO();
        user.setUserId(11L);
        user.setMemberId(21L);
        user.setNickname("张三");
        user.setUsername("zhangsan");
        when(identityUserApi.page(org.mockito.ArgumentMatchers.any()))
                .thenReturn(R.ok(PageResult.of(List.of(user), 1, 1, 20)));
        HomeUserOptionQuery query = new HomeUserOptionQuery();
        query.setKeyword(" 张 ");
        query.setSize(20L);

        List<HomeUserOptionVO> options = adapter(identityUserApi).list(query);

        assertThat(options).singleElement().satisfies(option -> assertThat(option)
                .extracting("userId", "memberId", "displayName", "username")
                .containsExactly(11L, 21L, "张三", "zhangsan"));
        ArgumentCaptor<IdentityUserPageQuery> captor = ArgumentCaptor.forClass(IdentityUserPageQuery.class);
        verify(identityUserApi).page(captor.capture());
        assertThat(captor.getValue()).satisfies(identityQuery -> {
            assertThat(identityQuery.getPage()).isEqualTo(1L);
            assertThat(identityQuery.getSize()).isEqualTo(20L);
            assertThat(identityQuery.getKeyword()).isEqualTo("张");
            assertThat(identityQuery.getStatus()).isEqualTo(1);
        });
    }

    @Test
    void failsExplicitlyWhenIdentityApiIsMissingOrFails() {
        assertThatThrownBy(() -> adapter(null).list(new HomeUserOptionQuery()))
                .isInstanceOf(BizException.class)
                .extracting("code")
                .isEqualTo(HomeCode.HOME_USER_OPTION_PROVIDER_MISSING.getCode());

        IdentityUserApi identityUserApi = mock(IdentityUserApi.class);
        when(identityUserApi.page(org.mockito.ArgumentMatchers.any())).thenReturn(R.fail(500, "identity unavailable"));
        assertThatThrownBy(() -> adapter(identityUserApi).list(new HomeUserOptionQuery()))
                .isInstanceOf(BizException.class)
                .extracting("code")
                .isEqualTo(HomeCode.HOME_USER_OPTION_LOAD_FAILED.getCode());
    }

    private HomeIdentityUserOptionProviderAdapter adapter(IdentityUserApi api) {
        @SuppressWarnings("unchecked")
        ObjectProvider<IdentityUserApi> provider = mock(ObjectProvider.class);
        when(provider.getIfAvailable()).thenReturn(api);
        return new HomeIdentityUserOptionProviderAdapter(provider);
    }
}
