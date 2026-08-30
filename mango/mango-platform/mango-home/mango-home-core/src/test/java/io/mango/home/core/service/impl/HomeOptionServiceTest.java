package io.mango.home.core.service.impl;

import io.mango.home.api.query.HomeUserOptionQuery;
import io.mango.home.api.vo.HomeUserOptionVO;
import io.mango.home.core.service.IHomeUserOptionProvider;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.same;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class HomeOptionServiceTest {

    private final IHomeUserOptionProvider provider = mock(IHomeUserOptionProvider.class);
    private final HomeOptionService service = new HomeOptionService(provider);

    @Test
    void bothPageContractsDelegateToCurrentTenantProvider() {
        HomeUserOptionQuery query = new HomeUserOptionQuery();
        HomeUserOptionVO option = new HomeUserOptionVO();
        when(provider.list(query)).thenReturn(List.of(option));

        assertThat(service.listPageUserOptions(query)).containsExactly(option);
        assertThat(service.listVisibleUserOptions(query)).containsExactly(option);

        verify(provider, org.mockito.Mockito.times(2)).list(same(query));
    }

    @Test
    void explicitNullSizeUsesBoundedDefault() {
        HomeUserOptionQuery query = new HomeUserOptionQuery();
        query.setSize(null);

        service.listPageUserOptions(query);

        assertThat(query.getSize()).isEqualTo(50L);
        verify(provider).list(same(query));
    }
}
