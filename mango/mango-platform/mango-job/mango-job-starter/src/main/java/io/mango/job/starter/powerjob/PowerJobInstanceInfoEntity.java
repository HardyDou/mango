package io.mango.job.starter.powerjob;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * PowerJob 实例记录，只读用于导入调度实例。
 */
@Getter
@Setter
@TableName("instance_info")
public class PowerJobInstanceInfoEntity {

    private Long id;

    private Long instanceId;

    private Long appId;

    private Long jobId;

    private Long expectedTriggerTime;

    private Long actualTriggerTime;

    private Long finishedTime;

    private Integer status;

    private String taskTrackerAddress;

    private String outerKey;

    private String result;

    private LocalDateTime gmtCreate;

    private LocalDateTime gmtModified;
}
