package io.mango.workflow.core.support;

import io.mango.workflow.api.vo.WorkflowNodeActionConfigVO;
import io.mango.workflow.core.model.WorkflowApprovalNodeConfig;
import io.mango.workflow.core.model.WorkflowNodeActionConfig;
import org.springframework.util.StringUtils;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Resolves node action configuration into the runtime contract returned by task detail APIs.
 */
public final class WorkflowNodeActionConfigResolver {

    private WorkflowNodeActionConfigResolver() {
    }

    public static Map<String, WorkflowNodeActionConfigVO> resolve(WorkflowApprovalNodeConfig config) {
        Map<String, WorkflowNodeActionConfig> configured = config == null || config.getActions() == null
                ? Map.of()
                : config.getActions();
        Map<String, WorkflowNodeActionConfigVO> result = new LinkedHashMap<>();
        result.put("save", nodeAction("暂存", false, false, false, 10, configured.get("save"), true,
                "当前后端未提供暂存接口"));
        result.put("transfer", nodeAction("转办", false, false, false, 20, configured.get("transfer"), true,
                "当前后端未提供转办接口"));
        result.put("addSign", nodeAction("加签", false, false, false, 30, configured.get("addSign"), true,
                "当前后端未提供加签接口"));
        result.put("reject", nodeAction("驳回", true, true, true, 40, configured.get("reject"), false,
                null));
        result.put("complete", nodeAction("通过", true, false, false, 50, configured.get("complete"), false,
                null));
        return result;
    }

    private static WorkflowNodeActionConfigVO nodeAction(String defaultLabel, boolean defaultEnabled,
                                                        boolean defaultRequireComment, boolean defaultDanger,
                                                        int defaultOrder, WorkflowNodeActionConfig configured,
                                                        boolean unsupported, String unsupportedTooltip) {
        WorkflowNodeActionConfigVO vo = new WorkflowNodeActionConfigVO();
        boolean enabled = configured == null || configured.getEnabled() == null ? defaultEnabled : configured.getEnabled();
        vo.setEnabled(enabled);
        vo.setLabel(StringUtils.hasText(configured == null ? null : configured.getLabel()) ? configured.getLabel() : defaultLabel);
        vo.setRequireComment(configured == null || configured.getRequireComment() == null
                ? defaultRequireComment
                : configured.getRequireComment());
        vo.setConfirmText(StringUtils.hasText(configured == null ? null : configured.getConfirmText())
                ? configured.getConfirmText()
                : "确认" + vo.getLabel() + "当前任务？");
        vo.setDanger(configured == null || configured.getDanger() == null ? defaultDanger : configured.getDanger());
        vo.setOrder(configured == null || configured.getOrder() == null ? defaultOrder : configured.getOrder());
        vo.setDisabled(unsupported && enabled);
        vo.setTooltip(unsupported && enabled ? unsupportedTooltip : null);
        return vo;
    }
}
