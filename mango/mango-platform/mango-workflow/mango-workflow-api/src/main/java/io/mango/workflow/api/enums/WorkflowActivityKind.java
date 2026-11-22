package io.mango.workflow.api.enums;

/**
 * 活动时间轴中一行活动的类别，用于调用方按类别筛选取用。
 *
 * <p>判定依据 Flowable 的 {@code ACT_TYPE_} 原值：{@code exclusiveGateway}、
 * {@code inclusiveGateway}、{@code parallelGateway}、{@code eventBasedGateway}、
 * {@code complexGateway} 归为 {@link #GATEWAY}；{@code userTask}、{@code manualTask}、
 * {@code receiveTask} 归为 {@link #TASK}；{@code startEvent}、{@code endEvent}、
 * {@code boundaryEvent}、{@code intermediateCatchEvent}、{@code intermediateThrowEvent}
 * 归为 {@link #EVENT}；其余（如 {@code serviceTask}、{@code scriptTask}、{@code callActivity}、
 * {@code subProcess}）归为 {@link #OTHER}。
 */
public enum WorkflowActivityKind {

    /** 用户任务及其它人工任务活动。 */
    TASK,

    /** 网关活动，不含处理人。 */
    GATEWAY,

    /** 开始、结束、边界与中间事件。 */
    EVENT,

    /** 其它活动类型（服务任务、脚本任务、调用活动、子流程等）。 */
    OTHER
}
