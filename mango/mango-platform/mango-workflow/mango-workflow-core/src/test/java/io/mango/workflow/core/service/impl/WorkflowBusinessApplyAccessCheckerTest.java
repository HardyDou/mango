package io.mango.workflow.core.service.impl;

import io.mango.common.exception.BizException;
import io.mango.infra.context.api.MangoContextHolder;
import io.mango.infra.context.api.MangoContextSnapshot;
import io.mango.workflow.api.WorkflowBusinessApplyDataPermissionProvider;
import io.mango.workflow.api.WorkflowBusinessApplyAccessContext;
import io.mango.workflow.api.enums.WorkflowCode;
import io.mango.workflow.core.entity.WorkflowBusinessApplyEntity;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.ObjectProvider;

import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class WorkflowBusinessApplyAccessCheckerTest {

    private final ObjectProvider<WorkflowBusinessApplyDataPermissionProvider> providers = mock();
    private final WorkflowBusinessApplyAccessChecker checker = new WorkflowBusinessApplyAccessChecker(providers);

    @AfterEach
    void tearDown() {
        MangoContextHolder.clear();
    }

    @Test
    void matchingProviderControlsBusinessDataAccess() {
        WorkflowBusinessApplyDataPermissionProvider provider = mock();
        when(providers.orderedStream()).thenReturn(Stream.of(provider));
        when(provider.supports("GUARANTEE")).thenReturn(true);
        when(provider.canRead(org.mockito.ArgumentMatchers.any(WorkflowBusinessApplyAccessContext.class)))
                .thenReturn(false);
        WorkflowBusinessApplyEntity apply = apply("GUARANTEE", "B-1", 1L, 1001L);
        MangoContextHolder.set(MangoContextSnapshot.empty().withSecurity(
                1001L, "1", "user", "default", "USER", "USER", 1L, "app"));

        assertThatThrownBy(() -> checker.check(apply))
                .isInstanceOf(BizException.class)
                .extracting("code").isEqualTo(WorkflowCode.APPLY_ACCESS_DENIED.getCode());
    }

    @Test
    void defaultOwnerCheckRequiresTenantAndApplicant() {
        when(providers.orderedStream()).thenAnswer(invocation -> Stream.empty());
        WorkflowBusinessApplyEntity apply = apply("RESOURCE", "B-2", 1L, 1001L);
        MangoContextHolder.set(MangoContextSnapshot.empty().withSecurity(
                1001L, "1", "user", "default", "USER", "USER", 1L, "app"));

        checker.check(apply);
        MangoContextHolder.clear();
        assertThat(checker.isAllowed(apply)).isFalse();
    }

    private WorkflowBusinessApplyEntity apply(String type, String key, Long tenant, Long applicant) {
        WorkflowBusinessApplyEntity apply = new WorkflowBusinessApplyEntity();
        apply.setId(1L);
        apply.setBusinessType(type);
        apply.setBusinessKey(key);
        apply.setTenantId(String.valueOf(tenant));
        apply.setApplicantId(applicant);
        return apply;
    }
}
