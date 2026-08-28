package io.mango.workflow.core.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

/** 工作流参与关系投影。 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("workflow_process_participant")
public class WorkflowProcessParticipantEntity extends WorkflowBaseEntity {
    private String processKey;
    private String businessKey;
    private String processInstanceId;
    private Long userId;
    private Long memberId;
    private String usernameSnapshot;
    private String displayNameSnapshot;
    private String participantType;
    private Boolean active;
    private LocalDateTime firstParticipatedAt;
    private LocalDateTime lastParticipatedAt;
}
