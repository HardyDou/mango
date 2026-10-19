package io.mango.cms.core.service.impl;

import io.mango.infra.context.api.MangoContextHolder;
import io.mango.infra.context.api.MangoContextSnapshot;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

class CmsSupportTest {

    @AfterEach
    void tearDown() {
        MangoContextHolder.clear();
    }

    @Test
    void currentTenantIdOrNull_无租户上下文_返回空() {
        MangoContextHolder.clear();

        assertThat(CmsSupport.currentTenantIdOrNull()).isNull();
    }

    @Test
    void currentTenantIdOrNull_有租户上下文_返回租户() {
        MangoContextHolder.set(MangoContextSnapshot.empty().withTenantId("tenant-a"));

        assertThat(CmsSupport.currentTenantIdOrNull()).isEqualTo("tenant-a");
    }

    @Test
    void isEffective_开始未到或已过结束_不可见() {
        LocalDateTime now = LocalDateTime.of(2026, 6, 22, 10, 0);

        assertThat(CmsSupport.isEffective(now.plusMinutes(1), null, now)).isFalse();
        assertThat(CmsSupport.isEffective(null, now, now)).isFalse();
        assertThat(CmsSupport.isEffective(now.minusMinutes(1), now.plusMinutes(1), now)).isTrue();
    }

}
