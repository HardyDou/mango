package io.mango.workflow.api.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 工作流任务承办人来源。
 *
 * <p>记录任务当前 assignee 是如何落上去的，用于两件事：向业务区分「本人认领」与「平台派单」，
 * 以及判断当前承办人能否把任务释放回节点候选池。判断依据是来源事实，不是 Flowable 的
 * {@code claim_time_}——引擎侧 {@code setAssignee} 不会写领取时间，用领取时间为空推断「非认领获得」
 * 会把平台派单任务锁死（Issue #1275）。
 *
 * <p>不可释放的来源必须给出可读原因和后续动作，不能只返回「不支持」：承办人看到失败提示后要知道
 * 为什么失败、应该改用转办还是找管理员改派。
 */
@Getter
@AllArgsConstructor
public enum WorkflowTaskAssigneeSource {

    /** 当前用户在候选池中主动认领。 */
    SELF_CLAIM("本人认领", true, null),

    /** 平台按自动派单策略在候选集中选择承办人。 */
    AUTO_ASSIGN("平台派单", true, null),

    /** 角色、岗位、组织或静态多人审批人按人展开，每人一条任务。 */
    CANDIDATE_EXPAND("候选按人展开", false,
            "任务由角色、岗位或组织审批人按人展开生成，每个办理人需要本人署名"),

    /** 流程定义直接指定的单人办理人。 */
    DEFINITION("流程定义指定", false, "办理人由流程定义直接指定"),

    /** 发起人在发起时选择办理人。 */
    INITIATOR_SELECT("发起人选择", false, "办理人由发起人在发起时指定"),

    /** 表单字段指定办理人。 */
    FORM_USER("表单指定", false, "办理人由表单字段指定"),

    /** 承办人自助转办后接手。 */
    TRANSFER("转办接手", false, "任务由他人转办给你，交回会丢失转办意图"),

    /** 管理员改派后接手。 */
    REASSIGN("管理员改派", false, "任务由管理员改派给你，交回会破坏管理处置结果"),

    /** 退回时由上游指定目标办理人。 */
    RETURN_ASSIGN("退回指定", false, "任务由退回操作指定你复核，交回会丢失上游的指定意图"),

    /** 节点审批人为空，按兜底策略转管理员或指定用户。 */
    ADMIN_FALLBACK("空办理人兜底", false, "节点没有可用审批人，任务已按兜底策略转给管理员");

    /** 承办人来源无法解析时对外给出的阻断原因。 */
    public static final String UNKNOWN_SOURCE_REASON = "无法确认承办人来源";

    private final String label;

    /** 当前承办人是否可以把任务释放回候选池。 */
    private final boolean releasable;

    /** 不可释放原因；可释放来源为空。 */
    private final String releaseBlockedReason;

    public static WorkflowTaskAssigneeSource fromCode(String code) {
        if (code == null || code.isBlank()) {
            return null;
        }
        String normalized = code.trim().toUpperCase();
        for (WorkflowTaskAssigneeSource source : values()) {
            if (source.name().equals(normalized)) {
                return source;
            }
        }
        return null;
    }

    /** 不可释放时返回给调用方的提示：只说明当前任务的来源与不可释放原因。 */
    public String releaseBlockedMessage() {
        return "当前任务不支持释放：" + releaseBlockedReason + "。";
    }
}
