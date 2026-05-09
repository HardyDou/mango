package io.mango.org.core.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("org_post")
public class PostEntity {

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    private String postName;

    private String postCode;

    private Integer postSort;

    private String postStatus;

    private String remark;

    private Long tenantId;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;

    private Long createdBy;

    private LocalDateTime createdAt;

    private Long updatedBy;

    private LocalDateTime updatedAt;
}
