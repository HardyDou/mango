package io.mango.workflow.core.support;

import io.mango.common.result.R;
import io.mango.common.result.Require;
import io.mango.domain.api.DomainApi;
import io.mango.domain.api.vo.DomainVO;
import io.mango.workflow.api.enums.WorkflowCode;

/**
 * Workflow business-domain contract adapter.
 */
public final class WorkflowDomainSupport {

    private WorkflowDomainSupport() {
    }

    public static void requireEnabled(DomainApi domainApi, String domainCode) {
        Require.notBlank(domainCode, WorkflowCode.DEFINITION_INVALID, "业务域不能为空");
        R<DomainVO> response = domainApi.detailByCode(domainCode.trim());
        Require.isTrue(response != null && response.isSuccess() && response.getData() != null,
                WorkflowCode.DEFINITION_INVALID, "业务域不存在");
        Require.isTrue(Integer.valueOf(1).equals(response.getData().getStatus()),
                WorkflowCode.DEFINITION_INVALID, "业务域已停用");
    }
}
