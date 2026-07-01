package io.mango.link.core.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("link_item")
public class LinkItemEntity {

    @TableId(value = "id", type = IdType.ASSIGN_ID)
    private Long id;

    private Long tenantId;

    private Long categoryId;

    private String name;

    private String url;

    private String summary;

    private String iconUrl;

    private String tags;

    private String visibilityScope;

    private Long ownerUserId;

    private String openMode;

    private Boolean recommended;

    private Integer sortNo;

    private String status;

    private String remark;

    private Long createdBy;

    private Long updatedBy;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;
}
