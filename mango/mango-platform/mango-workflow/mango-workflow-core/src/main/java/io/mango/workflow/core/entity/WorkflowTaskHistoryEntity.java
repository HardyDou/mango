package io.mango.workflow.core.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

/**
 * Flowable 历史任务的只读投影实体。
 *
 * <p>该实体为任务历史查询提供字段契约，并满足核心模块「所有 Mapper 必须是实体 Mapper」的架构约束。
 * 对应表 {@code ACT_HI_TASKINST} 属于 Flowable 引擎：禁止通过 {@code BaseMapper} 的写操作方法修改，
 * 任务生命周期只能由引擎驱动。启动期数据库结构校验按 {@code act_} 前缀跳过该表。
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("ACT_HI_TASKINST")
public class WorkflowTaskHistoryEntity extends WorkflowBaseEntity {

    @TableField("ID_")
    private String taskId;

    @TableField("NAME_")
    private String taskName;

    @TableField("TASK_DEF_KEY_")
    private String taskDefinitionKey;

    @TableField("PROC_INST_ID_")
    private String processInstanceId;

    @TableField("PROC_DEF_ID_")
    private String processDefinitionId;

    @TableField("ASSIGNEE_")
    private String assigneeKey;

    @TableField("CREATE_TIME_")
    private LocalDateTime createTime;

    @TableField("CLAIM_TIME_")
    private LocalDateTime claimTime;

    @TableField("END_TIME_")
    private LocalDateTime endTime;

    @TableField("DELETE_REASON_")
    private String deleteReason;
}
