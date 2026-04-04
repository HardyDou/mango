package io.mango.system.core.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("sys_route_conf")
public class SysRoute {
    @TableId(type = IdType.ASSIGN_ID)
    private Long id;
    private String routeName;
    private Integer routeType;
    private String routePath;
    private String routeDesc;
    private Integer sort;
    private Integer status;
    @TableField(fill = FieldFill.INSERT)
    private String createBy;
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private String updateBy;
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;
}
