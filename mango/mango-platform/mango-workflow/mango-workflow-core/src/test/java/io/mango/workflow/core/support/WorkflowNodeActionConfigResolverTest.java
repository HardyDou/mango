package io.mango.workflow.core.support;

import io.mango.workflow.api.vo.WorkflowNodeActionConfigVO;
import io.mango.workflow.core.model.WorkflowApprovalNodeConfig;
import io.mango.workflow.core.model.WorkflowNodeActionConfig;
import org.junit.jupiter.api.Test;

import java.util.LinkedHashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class WorkflowNodeActionConfigResolverTest {

    @Test
    void resolve_shouldReturnCompatibleDefaultsWhenNodeHasNoActions() {
        Map<String, WorkflowNodeActionConfigVO> actions = WorkflowNodeActionConfigResolver.resolve(null);

        assertThat(actions.keySet()).containsExactly("save", "transfer", "addSign", "reject", "complete");
        assertThat(actions.get("complete"))
                .returns(true, WorkflowNodeActionConfigVO::getEnabled)
                .returns("通过", WorkflowNodeActionConfigVO::getLabel)
                .returns(false, WorkflowNodeActionConfigVO::getRequireComment)
                .returns(false, WorkflowNodeActionConfigVO::getDisabled);
        assertThat(actions.get("reject"))
                .returns(true, WorkflowNodeActionConfigVO::getEnabled)
                .returns("驳回", WorkflowNodeActionConfigVO::getLabel)
                .returns(true, WorkflowNodeActionConfigVO::getRequireComment)
                .returns(true, WorkflowNodeActionConfigVO::getDanger);
        assertThat(actions.get("save"))
                .returns(false, WorkflowNodeActionConfigVO::getEnabled)
                .returns(false, WorkflowNodeActionConfigVO::getDisabled)
                .returns(null, WorkflowNodeActionConfigVO::getTooltip);
    }

    @Test
    void resolve_shouldApplyCustomLabelsCommentRequirementConfirmAndOrder() {
        WorkflowApprovalNodeConfig config = new WorkflowApprovalNodeConfig();
        WorkflowNodeActionConfig complete = new WorkflowNodeActionConfig();
        complete.setLabel("同意并提交");
        complete.setRequireComment(true);
        complete.setConfirmText("确认提交给下一节点？");
        complete.setOrder(12);

        WorkflowNodeActionConfig reject = new WorkflowNodeActionConfig();
        reject.setEnabled(false);
        reject.setLabel("退回补充");
        reject.setRequireComment(false);

        config.setActions(new LinkedHashMap<>(Map.of(
                "complete", complete,
                "reject", reject
        )));

        Map<String, WorkflowNodeActionConfigVO> actions = WorkflowNodeActionConfigResolver.resolve(config);

        assertThat(actions.get("complete"))
                .returns(true, WorkflowNodeActionConfigVO::getEnabled)
                .returns("同意并提交", WorkflowNodeActionConfigVO::getLabel)
                .returns(true, WorkflowNodeActionConfigVO::getRequireComment)
                .returns("确认提交给下一节点？", WorkflowNodeActionConfigVO::getConfirmText)
                .returns(12, WorkflowNodeActionConfigVO::getOrder);
        assertThat(actions.get("reject"))
                .returns(false, WorkflowNodeActionConfigVO::getEnabled)
                .returns("退回补充", WorkflowNodeActionConfigVO::getLabel)
                .returns(false, WorkflowNodeActionConfigVO::getRequireComment);
    }

    @Test
    void resolve_shouldDisableUnsupportedActionWhenConfiguredEnabled() {
        WorkflowApprovalNodeConfig config = new WorkflowApprovalNodeConfig();
        WorkflowNodeActionConfig transfer = new WorkflowNodeActionConfig();
        transfer.setEnabled(true);
        transfer.setLabel("转交他人处理");
        config.setActions(Map.of("transfer", transfer));

        Map<String, WorkflowNodeActionConfigVO> actions = WorkflowNodeActionConfigResolver.resolve(config);

        assertThat(actions.get("transfer"))
                .returns(true, WorkflowNodeActionConfigVO::getEnabled)
                .returns(true, WorkflowNodeActionConfigVO::getDisabled)
                .returns("转交他人处理", WorkflowNodeActionConfigVO::getLabel)
                .returns("当前后端未提供转办接口", WorkflowNodeActionConfigVO::getTooltip);
    }
}
