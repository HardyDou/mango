package io.mango.workflow.api.enums;

/**
 * 流程模板状态。
 */
public enum WorkflowTemplateStatus {

    /**
     * 草稿。
     */
    DRAFT,

    /**
     * 启用，可用于派生流程定义。
     */
    ENABLED,

    /**
     * 停用。
     */
    DISABLED,

    /**
     * 归档，不再维护。
     */
    ARCHIVED
}
