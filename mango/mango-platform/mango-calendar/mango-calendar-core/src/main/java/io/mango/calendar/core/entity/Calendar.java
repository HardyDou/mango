package io.mango.calendar.core.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("calendar")
public class Calendar {

    @TableId(value = "id", type = IdType.ASSIGN_ID)
    private Long id;

    private Long tenantId;

    private String calendarCode;

    private String calendarName;

    private Integer status;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;
}
